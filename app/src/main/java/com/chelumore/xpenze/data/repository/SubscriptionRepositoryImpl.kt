package com.chelumore.xpenze.data.repository

import android.app.Activity
import android.content.Context
import com.chelumore.xpenze.BuildConfig
import com.chelumore.xpenze.core.billing.BillingPurchaseResult
import com.chelumore.xpenze.core.billing.BillingPurchaseState
import com.chelumore.xpenze.core.billing.BillingRestoreResult
import com.chelumore.xpenze.core.billing.BillingService
import com.chelumore.xpenze.core.billing.BillingSubscriptionPurchase
import com.chelumore.xpenze.core.billing.SupabaseBillingApi
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.common.await
import com.chelumore.xpenze.core.common.currentTimeMillis
import com.chelumore.xpenze.core.database.dao.SubscriptionDao
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.domain.model.UserSubscription
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.PurchaseSubscriptionResult
import com.chelumore.xpenze.domain.repository.RestorePurchasesResult
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.TrialActivationResult
import com.chelumore.xpenze.domain.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val billingService: BillingService,
    private val supabaseBillingApi: SupabaseBillingApi,
    private val subscriptionDao: SubscriptionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SubscriptionRepository {

    override fun getActiveSubscription(): Flow<UserSubscription?> =
        subscriptionDao.getActive().map { it?.toDomain() }

    override fun isPremium(): Flow<Boolean> =
        subscriptionDao.getActive().map { subscription ->
            subscription != null &&
                subscription.isActive &&
                subscription.expiryTime > currentTimeMillis() &&
                !subscription.purchaseToken.startsWith("trial_")
        }

    override suspend fun saveSubscription(subscription: UserSubscription) =
        withContext(ioDispatcher) {
            subscriptionDao.insert(subscription.toEntity())
        }

    override suspend fun updateSubscription(subscription: UserSubscription) =
        withContext(ioDispatcher) {
            subscriptionDao.update(subscription.toEntity())
        }

    override suspend fun deactivateExpiredSubscriptions() = withContext(ioDispatcher) {
        subscriptionDao.deactivateExpired(currentTimeMillis())
    }

    override suspend fun markAcknowledged(id: String) = withContext(ioDispatcher) {
        val now = currentTimeMillis()
        subscriptionDao.markAcknowledged(id, now, now)
    }

    override suspend fun clearSubscriptions() = withContext(ioDispatcher) {
        subscriptionDao.deleteAll()
    }

    override suspend fun syncTrialStateFromBackend() {
        withContext(ioDispatcher) {
            val user = authRepository.getCurrentUser() ?: return@withContext
            val firestore = firestoreOrNull() ?: return@withContext
            val trialDoc = trialDocument(firestore, user.uid)

            runCatching {
                val remote = trialDoc.get().await()
                if (remote.exists()) {
                    val state = remote.toTrialSnapshot()
                    if (state.consumed) {
                        userRepository.setTrialState(
                            startedAt = state.startedAt,
                            expiresAt = state.expiresAt,
                            consumed = true
                        )
                    }
                    return@runCatching
                }

                // Migration path: if trial was consumed before backend enforcement, upload local state once.
                val local = userRepository.userPreferences.first()
                if (local.trialConsumed) {
                    val now = currentTimeMillis()
                    trialDoc.set(
                        mapOf(
                            FIELD_CONSUMED to true,
                            FIELD_STARTED_AT to local.trialStartedAt,
                            FIELD_EXPIRES_AT to local.trialExpiresAt,
                            FIELD_UPDATED_AT to now
                        ),
                        SetOptions.merge()
                    ).await()
                }
            }
        }
    }

    override suspend fun claimTrial(days: Int): TrialActivationResult = withContext(ioDispatcher) {
        val user = authRepository.getCurrentUser() ?: return@withContext TrialActivationResult.NotSignedIn
        val firestore = firestoreOrNull() ?: return@withContext TrialActivationResult.BackendUnavailable
        val now = currentTimeMillis()
        val expiresAt = now + days.coerceAtLeast(1).toLong() * DAY_MS
        val trialDoc = trialDocument(firestore, user.uid)

        val state = runCatching {
            firestore.runTransaction { transaction ->
                val current = transaction.get(trialDoc).toTrialSnapshot()
                if (current.consumed) {
                    current.copy(justStarted = false)
                } else {
                    transaction.set(
                        trialDoc,
                        mapOf(
                            FIELD_CONSUMED to true,
                            FIELD_STARTED_AT to now,
                            FIELD_EXPIRES_AT to expiresAt,
                            FIELD_UPDATED_AT to now
                        ),
                        SetOptions.merge()
                    )
                    TrialSnapshot(
                        consumed = true,
                        startedAt = now,
                        expiresAt = expiresAt,
                        justStarted = true
                    )
                }
            }.await()
        }.getOrElse { error ->
            return@withContext TrialActivationResult.Failure(
                error.message ?: "Unable to verify trial eligibility right now."
            )
        }

        if (state.consumed) {
            userRepository.setTrialState(
                startedAt = state.startedAt,
                expiresAt = state.expiresAt,
                consumed = true
            )
        }

        if (state.justStarted) {
            TrialActivationResult.Started(
                startedAt = state.startedAt,
                expiresAt = state.expiresAt
            )
        } else {
            TrialActivationResult.AlreadyConsumed(
                startedAt = state.startedAt,
                expiresAt = state.expiresAt
            )
        }
    }

    override suspend fun syncPremiumStateFromBackend() {
        withContext(ioDispatcher) {
            val now = currentTimeMillis()
            if (supabaseBillingApi.isConfigured()) {
                val verification = when (val result = supabaseBillingApi.refreshPurchase()) {
                    is SupabaseBillingApi.ApiCallResult.Success -> result.payload.toVerificationResult()
                    is SupabaseBillingApi.ApiCallResult.Failure -> null
                    SupabaseBillingApi.ApiCallResult.NotConfigured -> null
                } ?: return@withContext
                applyRemotePremiumState(
                    verification = verification,
                    now = now
                )
                return@withContext
            }

            val user = authRepository.getCurrentUser() ?: return@withContext
            val firestore = firestoreOrNull() ?: return@withContext
            val premiumDoc = subscriptionDocument(firestore, user.uid)
            runCatching {
                functionsOrNull()
                    ?.getHttpsCallable(FUNCTION_REFRESH_PLAY_SUBSCRIPTION)
                    ?.call(emptyMap<String, Any>())
                    ?.await()
            }

            runCatching {
                val remote = premiumDoc.get().await()
                if (!remote.exists()) {
                    subscriptionDao.deleteAll()
                    userRepository.setPremium(false)
                    return@runCatching
                }

                val isPremium = remote.getBoolean(FIELD_IS_PREMIUM) ?: false
                if (!isPremium) {
                    subscriptionDao.deleteAll()
                    userRepository.setPremium(false)
                    return@runCatching
                }

                val productId = remote.getString(FIELD_PRODUCT_ID)
                    ?.takeIf { it.isNotBlank() }
                    ?: Constants.SUBSCRIPTION_MONTHLY
                val purchaseTime = remote.getLong(FIELD_PURCHASE_TIME) ?: now
                val purchaseHash = remote.getString(FIELD_PURCHASE_TOKEN_HASH).orEmpty()
                val expiryTime = remote.getLong(FIELD_EXPIRY_TIME)
                    ?: estimateExpiryTime(productId = productId, purchaseTime = purchaseTime)
                if (expiryTime <= now) {
                    subscriptionDao.deleteAll()
                    userRepository.setPremium(false)
                    return@runCatching
                }

                val remoteSubscription = UserSubscription(
                    id = "remote_${purchaseHash.ifBlank { "${productId}_$purchaseTime" }}",
                    productId = productId,
                    purchaseToken = "remote_$purchaseHash",
                    purchaseTime = purchaseTime,
                    expiryTime = expiryTime,
                    isAutoRenewing = remote.getBoolean(FIELD_IS_AUTO_RENEWING) ?: true,
                    isActive = true,
                    acknowledgedAt = remote.getLong(FIELD_ACKNOWLEDGED_AT),
                    updatedAt = remote.getLong(FIELD_UPDATED_AT) ?: now
                )

                subscriptionDao.insert(remoteSubscription.toEntity())
                userRepository.setPremium(true)
            }
        }
    }

    override suspend fun purchaseSubscription(
        activity: Activity,
        productId: String
    ): PurchaseSubscriptionResult = withContext(ioDispatcher) {
        val user = authRepository.getCurrentUser() ?: return@withContext PurchaseSubscriptionResult.NotSignedIn
        val obfuscatedAccountId = user.uid.take(MAX_OBFUSCATED_ID_LENGTH)

        when (
            val billingResult = billingService.launchSubscriptionPurchase(
                activity = activity,
                productId = productId,
                obfuscatedAccountId = obfuscatedAccountId
            )
        ) {
            is BillingPurchaseResult.Success -> {
                val verificationResult = verifyPurchaseWithBackend(
                    purchase = billingResult.purchase
                )
                val verification = when (verificationResult) {
                    is BackendVerificationResult.Failure -> {
                        return@withContext PurchaseSubscriptionResult.Failure(
                            sanitizePurchaseFailureMessage(verificationResult.message)
                        )
                    }
                    is BackendVerificationResult.Success -> verificationResult.verification
                }
                if (!verification.isPremium) {
                    return@withContext PurchaseSubscriptionResult.Failure(
                        sanitizePurchaseFailureMessage(verification.message)
                    )
                }
                val subscription = upsertPaidSubscription(
                    purchase = billingResult.purchase,
                    verification = verification
                )
                PurchaseSubscriptionResult.Success(subscription)
            }
            BillingPurchaseResult.UserCanceled -> PurchaseSubscriptionResult.UserCanceled
            BillingPurchaseResult.Pending -> PurchaseSubscriptionResult.Pending
            BillingPurchaseResult.ProductUnavailable -> PurchaseSubscriptionResult.ProductUnavailable
            is BillingPurchaseResult.Failure -> PurchaseSubscriptionResult.Failure(billingResult.message)
        }
    }

    override suspend fun restorePurchases(): RestorePurchasesResult = withContext(ioDispatcher) {
        authRepository.getCurrentUser()
            ?: return@withContext RestorePurchasesResult.Failure("Sign in to restore purchases.")

        when (val restore = billingService.queryActiveSubscriptions()) {
            is BillingRestoreResult.Failure -> {
                RestorePurchasesResult.Failure(restore.message)
            }
            is BillingRestoreResult.Success -> {
                val active = restore.purchases
                    .filter { it.state == BillingPurchaseState.PURCHASED }
                    .sortedByDescending { it.purchaseTime }

                if (active.isEmpty()) {
                    subscriptionDao.deleteAll()
                    userRepository.setPremium(false)
                    return@withContext RestorePurchasesResult.NoActivePurchases
                }

                var verificationFailureMessage: String? = null
                val verified = active.mapNotNull { purchase ->
                    val verification = when (val result = verifyPurchaseWithBackend(purchase)) {
                        is BackendVerificationResult.Failure -> {
                            verificationFailureMessage = verificationFailureMessage ?: result.message
                            return@mapNotNull null
                        }
                        is BackendVerificationResult.Success -> result.verification
                    }
                    if (!verification.isPremium) return@mapNotNull null
                    VerifiedPurchase(
                        purchase = purchase,
                        verification = verification
                    )
                }
                if (verified.isEmpty()) {
                    if (!verificationFailureMessage.isNullOrBlank()) {
                        return@withContext RestorePurchasesResult.Failure(
                            sanitizeRestoreFailureMessage(verificationFailureMessage)
                        )
                    }
                    subscriptionDao.deleteAll()
                    userRepository.setPremium(false)
                    return@withContext RestorePurchasesResult.NoActivePurchases
                }

                val selected = verified.maxWithOrNull(
                    compareBy<VerifiedPurchase> { productPriority(it.verification.productId) }
                        .thenBy { it.verification.expiryTime }
                ) ?: verified.first()

                upsertPaidSubscription(
                    purchase = selected.purchase,
                    verification = selected.verification
                )

                RestorePurchasesResult.Restored(activeCount = verified.size)
            }
        }
    }

    private fun firebaseAppOrNull(): FirebaseApp? {
        return runCatching { FirebaseApp.getInstance() }.getOrNull()
            ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
    }

    private fun firestoreOrNull(): FirebaseFirestore? {
        val app = firebaseAppOrNull() ?: return null
        return runCatching { FirebaseFirestore.getInstance(app) }.getOrNull()
    }

    private fun functionsOrNull(): FirebaseFunctions? {
        val app = firebaseAppOrNull() ?: return null
        val region = BuildConfig.FIREBASE_FUNCTIONS_REGION.ifBlank { DEFAULT_FUNCTIONS_REGION }
        return runCatching { FirebaseFunctions.getInstance(app, region) }.getOrNull()
    }

    private fun trialDocument(
        firestore: FirebaseFirestore,
        userId: String
    ) = firestore.collection(COLLECTION_USERS)
        .document(userId)
        .collection(COLLECTION_ENTITLEMENTS)
        .document(DOCUMENT_TRIAL)

    private fun subscriptionDocument(
        firestore: FirebaseFirestore,
        userId: String
    ) = firestore.collection(COLLECTION_USERS)
        .document(userId)
        .collection(COLLECTION_ENTITLEMENTS)
        .document(DOCUMENT_SUBSCRIPTION)

    private suspend fun upsertPaidSubscription(
        purchase: BillingSubscriptionPurchase,
        verification: VerificationResult
    ): UserSubscription {
        val now = currentTimeMillis()
        val verifiedProductId = verification.productId.ifBlank { purchase.productId }
        val verifiedPurchaseTime = if (verification.purchaseTime > 0L) {
            verification.purchaseTime
        } else {
            purchase.purchaseTime
        }
        val subscription = UserSubscription(
            id = purchase.purchaseToken,
            productId = verifiedProductId,
            purchaseToken = purchase.purchaseToken,
            purchaseTime = verifiedPurchaseTime,
            expiryTime = verification.expiryTime,
            isAutoRenewing = verification.isAutoRenewing,
            isActive = true,
            acknowledgedAt = verification.acknowledgedAt ?: if (purchase.isAcknowledged) now else null,
            updatedAt = now
        )

        subscriptionDao.insert(subscription.toEntity())
        userRepository.setPremium(true)
        userRepository.clearTrialState()
        return subscription
    }

    private suspend fun applyRemotePremiumState(
        verification: VerificationResult,
        now: Long
    ) {
        if (!verification.isPremium) {
            subscriptionDao.deleteAll()
            userRepository.setPremium(false)
            return
        }

        val productId = verification.productId
            .takeIf { it.isNotBlank() }
            ?: Constants.SUBSCRIPTION_MONTHLY
        val purchaseTime = verification.purchaseTime
            .takeIf { it > 0L }
            ?: now
        val purchaseKey = verification.purchaseTokenHash
            .ifBlank { "${productId}_$purchaseTime" }
        val expiryTime = verification.expiryTime
            .takeIf { it > 0L }
            ?: estimateExpiryTime(productId = productId, purchaseTime = purchaseTime)
        if (expiryTime <= now) {
            subscriptionDao.deleteAll()
            userRepository.setPremium(false)
            return
        }

        val remoteSubscription = UserSubscription(
            id = "remote_$purchaseKey",
            productId = productId,
            purchaseToken = "remote_$purchaseKey",
            purchaseTime = purchaseTime,
            expiryTime = expiryTime,
            isAutoRenewing = verification.isAutoRenewing,
            isActive = true,
            acknowledgedAt = verification.acknowledgedAt,
            updatedAt = now
        )

        subscriptionDao.insert(remoteSubscription.toEntity())
        userRepository.setPremium(true)
        userRepository.clearTrialState()
    }

    private suspend fun verifyPurchaseWithBackend(
        purchase: BillingSubscriptionPurchase
    ): BackendVerificationResult {
        if (supabaseBillingApi.isConfigured()) {
            return when (
                val result = supabaseBillingApi.verifyPurchase(
                    productId = purchase.productId,
                    purchaseToken = purchase.purchaseToken,
                    packageName = context.packageName
                )
            ) {
                is SupabaseBillingApi.ApiCallResult.Failure -> {
                    BackendVerificationResult.Failure(result.message)
                }
                SupabaseBillingApi.ApiCallResult.NotConfigured -> {
                    BackendVerificationResult.Failure("Premium backend is not configured.")
                }
                is SupabaseBillingApi.ApiCallResult.Success -> {
                    BackendVerificationResult.Success(result.payload.toVerificationResult())
                }
            }
        }

        return verifyPurchaseWithFirebaseFunctions(purchase)
    }

    private suspend fun verifyPurchaseWithFirebaseFunctions(
        purchase: BillingSubscriptionPurchase
    ): BackendVerificationResult {
        val functions = functionsOrNull() ?: return BackendVerificationResult.Failure(
            "Premium backend is unavailable."
        )
        val resultData = runCatching {
            functions
                .getHttpsCallable(FUNCTION_VERIFY_PLAY_SUBSCRIPTION)
                .call(
                    mapOf(
                        FIELD_PRODUCT_ID to purchase.productId,
                        FIELD_PURCHASE_TOKEN to purchase.purchaseToken,
                        FIELD_PACKAGE_NAME to context.packageName
                    )
                )
                .await()
                .getData()
        }.getOrNull() as? Map<*, *> ?: return BackendVerificationResult.Failure(
            "Unable to verify purchase on server."
        )

        val isPremium = resultData[FIELD_IS_PREMIUM].asBoolean() ?: false
        val productId = resultData[FIELD_PRODUCT_ID].asString().orEmpty()
        val expiryTime = resultData[FIELD_EXPIRY_TIME].asLong() ?: 0L
        val purchaseTime = resultData[FIELD_PURCHASE_TIME].asLong() ?: purchase.purchaseTime
        val isAutoRenewing = resultData[FIELD_IS_AUTO_RENEWING].asBoolean() ?: purchase.isAutoRenewing
        val acknowledgedAt = resultData[FIELD_ACKNOWLEDGED_AT].asLong()
        val purchaseTokenHash = resultData[FIELD_PURCHASE_TOKEN_HASH].asString().orEmpty()
        val message = resultData[FIELD_MESSAGE].asString().orEmpty()

        return BackendVerificationResult.Success(
            VerificationResult(
                isPremium = isPremium,
                productId = productId,
                purchaseTime = purchaseTime,
                expiryTime = expiryTime,
                isAutoRenewing = isAutoRenewing,
                acknowledgedAt = acknowledgedAt,
                purchaseTokenHash = purchaseTokenHash,
                message = message
            )
        )
    }

    private fun SupabaseBillingApi.BillingVerificationPayload.toVerificationResult(): VerificationResult {
        return VerificationResult(
            isPremium = isPremium,
            productId = productId,
            purchaseTime = purchaseTime,
            expiryTime = expiryTime,
            isAutoRenewing = isAutoRenewing,
            acknowledgedAt = acknowledgedAt,
            purchaseTokenHash = purchaseTokenHash,
            message = message
        )
    }

    private fun sanitizePurchaseFailureMessage(raw: String?): String {
        val message = raw?.trim().orEmpty()
        if (message.isBlank()) {
            return "We couldn't verify your Google Play purchase yet. If you were charged, tap Restore Purchases in a moment."
        }
        if (message.contains("Sign in is required", ignoreCase = true)) {
            return "Sign in before purchasing Premium."
        }
        if (message.contains("already linked to another account", ignoreCase = true)) {
            return "This subscription is already linked to another Xpenze account."
        }
        if (looksInternalBillingError(message)) {
            return "We couldn't verify your Google Play purchase yet. If you were charged, tap Restore Purchases in a moment."
        }
        return message
    }

    private fun sanitizeRestoreFailureMessage(raw: String?): String {
        val message = raw?.trim().orEmpty()
        if (message.isBlank()) {
            return "We couldn't restore your Google Play purchase right now. Please try again in a moment."
        }
        if (message.contains("Sign in is required", ignoreCase = true)) {
            return "Sign in to restore purchases."
        }
        if (message.contains("already linked to another account", ignoreCase = true)) {
            return "This subscription is already linked to another Xpenze account."
        }
        if (looksInternalBillingError(message)) {
            return "We couldn't restore your Google Play purchase right now. Please try again in a moment."
        }
        return message
    }

    private fun looksInternalBillingError(message: String): Boolean {
        return message.contains("Google Play verification failed", ignoreCase = true) ||
            message.contains("androidpublisher", ignoreCase = true) ||
            message.contains("permissionDenied", ignoreCase = true) ||
            message.contains("permission denied", ignoreCase = true) ||
            message.contains("\"error\"", ignoreCase = true) ||
            message.contains("{") ||
            message.contains("premium backend", ignoreCase = true) ||
            message.contains("verify purchase on server", ignoreCase = true) ||
            message.contains("temporarily unavailable", ignoreCase = true)
    }

    private fun estimateExpiryTime(productId: String, purchaseTime: Long): Long {
        val duration = when (productId) {
            Constants.SUBSCRIPTION_YEARLY -> YEAR_MS
            Constants.SUBSCRIPTION_MONTHLY -> MONTH_MS
            else -> MONTH_MS
        }
        return purchaseTime + duration
    }

    private fun productPriority(productId: String): Int {
        return when (productId) {
            Constants.SUBSCRIPTION_YEARLY -> 2
            Constants.SUBSCRIPTION_MONTHLY -> 1
            else -> 0
        }
    }

    private fun Any?.asLong(): Long? = when (this) {
        is Long -> this
        is Int -> this.toLong()
        is Double -> this.toLong()
        is Float -> this.toLong()
        is Number -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }

    private fun Any?.asBoolean(): Boolean? = when (this) {
        is Boolean -> this
        is Number -> this.toInt() != 0
        is String -> this.equals("true", ignoreCase = true) || this == "1"
        else -> null
    }

    private fun Any?.asString(): String? {
        return (this as? String)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun DocumentSnapshot.toTrialSnapshot(): TrialSnapshot {
        return TrialSnapshot(
            consumed = getBoolean(FIELD_CONSUMED) ?: false,
            startedAt = getLong(FIELD_STARTED_AT) ?: 0L,
            expiresAt = getLong(FIELD_EXPIRES_AT) ?: 0L
        )
    }

    private data class TrialSnapshot(
        val consumed: Boolean = false,
        val startedAt: Long = 0L,
        val expiresAt: Long = 0L,
        val justStarted: Boolean = false
    )

    private data class VerificationResult(
        val isPremium: Boolean,
        val productId: String,
        val purchaseTime: Long,
        val expiryTime: Long,
        val isAutoRenewing: Boolean,
        val acknowledgedAt: Long?,
        val purchaseTokenHash: String,
        val message: String
    )

    private sealed interface BackendVerificationResult {
        data class Success(
            val verification: VerificationResult
        ) : BackendVerificationResult

        data class Failure(
            val message: String
        ) : BackendVerificationResult
    }

    private data class VerifiedPurchase(
        val purchase: BillingSubscriptionPurchase,
        val verification: VerificationResult
    )

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L

        const val COLLECTION_USERS = "users"
        const val COLLECTION_ENTITLEMENTS = "entitlements"
        const val DOCUMENT_TRIAL = "trial_state"
        const val DOCUMENT_SUBSCRIPTION = "subscription_state"

        const val FIELD_CONSUMED = "trialConsumed"
        const val FIELD_STARTED_AT = "trialStartedAt"
        const val FIELD_EXPIRES_AT = "trialExpiresAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_IS_PREMIUM = "isPremium"
        const val FIELD_PRODUCT_ID = "productId"
        const val FIELD_PURCHASE_TOKEN = "purchaseToken"
        const val FIELD_PURCHASE_TOKEN_HASH = "purchaseTokenHash"
        const val FIELD_PACKAGE_NAME = "packageName"
        const val FIELD_PURCHASE_TIME = "purchaseTime"
        const val FIELD_EXPIRY_TIME = "expiryTime"
        const val FIELD_IS_AUTO_RENEWING = "isAutoRenewing"
        const val FIELD_ACKNOWLEDGED_AT = "acknowledgedAt"
        const val FIELD_MESSAGE = "message"
        const val FIELD_SOURCE = "source"
        const val SOURCE_PLAY_BILLING = "play_billing"
        const val FUNCTION_VERIFY_PLAY_SUBSCRIPTION = "verifyPlaySubscriptionPurchase"
        const val FUNCTION_REFRESH_PLAY_SUBSCRIPTION = "refreshPlaySubscriptionEntitlement"
        const val DEFAULT_FUNCTIONS_REGION = "us-central1"
        const val MAX_OBFUSCATED_ID_LENGTH = 64

        const val MONTH_MS = 31L * DAY_MS
        const val YEAR_MS = 366L * DAY_MS
    }
}

