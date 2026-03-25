package com.chelumore.xpenze.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.core.di.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class PlayBillingService @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : BillingService {

    private data class PurchaseUpdateEvent(
        val result: BillingResult,
        val purchases: List<Purchase>,
        val emittedAt: Long
    )

    private val purchaseUpdates = MutableSharedFlow<PurchaseUpdateEvent>(extraBufferCapacity = 8)
    private val connectionMutex = Mutex()
    private val purchaseFlowMutex = Mutex()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        purchaseUpdates.tryEmit(
            PurchaseUpdateEvent(
                result = billingResult,
                purchases = purchases.orEmpty(),
                emittedAt = System.currentTimeMillis()
            )
        )
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override suspend fun querySubscriptionPlans(
        productIds: List<String>
    ): BillingCatalogResult = withContext(ioDispatcher) {
        val distinctIds = productIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (distinctIds.isEmpty()) {
            return@withContext BillingCatalogResult.Success(emptyList())
        }

        val ready = ensureConnected()
        if (ready.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext BillingCatalogResult.Failure(
                ready.debugMessage.ifBlank { "Unable to connect to Google Play Billing." }
            )
        }

        val details = queryProductDetails(distinctIds)
        if (details == null) {
            return@withContext BillingCatalogResult.Failure(
                "Unable to load live Google Play pricing right now."
            )
        }

        val order = distinctIds.withIndex().associate { it.value to it.index }
        val plans = details.mapNotNull { productDetails ->
            val offer = selectOffer(productDetails) ?: return@mapNotNull null
            val recurringPhase = offer.recurringPhase
            BillingSubscriptionPlan(
                productId = productDetails.productId,
                offerToken = offer.offerToken,
                formattedPrice = recurringPhase.formattedPrice,
                priceAmountMicros = recurringPhase.priceAmountMicros,
                priceCurrencyCode = recurringPhase.priceCurrencyCode,
                billingPeriod = recurringPhase.billingPeriod,
                hasIntroductoryPhase = offer.hasIntroductoryPhase
            )
        }.sortedBy { order[it.productId] ?: Int.MAX_VALUE }

        BillingCatalogResult.Success(plans)
    }

    override suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        obfuscatedAccountId: String?
    ): BillingPurchaseResult = withContext(ioDispatcher) {
        purchaseFlowMutex.withLock {
            val ready = ensureConnected()
            if (ready.responseCode != BillingClient.BillingResponseCode.OK) {
                return@withLock BillingPurchaseResult.Failure(
                    ready.debugMessage.ifBlank { "Unable to connect to Google Play Billing." }
                )
            }

            val productDetails = queryProductDetails(productId) ?: return@withLock BillingPurchaseResult.ProductUnavailable
            val offer = selectOffer(productDetails) ?: return@withLock BillingPurchaseResult.ProductUnavailable

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offer.offerToken)
                .build()

            val flowParamsBuilder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
            if (!obfuscatedAccountId.isNullOrBlank()) {
                flowParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId.take(MAX_OBFUSCATED_ID_LENGTH))
            }

            val launchStartedAt = System.currentTimeMillis()
            val launchResult = withContext(mainDispatcher) {
                billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
            }

            when (launchResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Continue and wait for purchase update callback.
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    return@withLock BillingPurchaseResult.UserCanceled
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    return@withLock purchaseFromStoreOrFailure(productId)
                }
                else -> {
                    return@withLock BillingPurchaseResult.Failure(
                        launchResult.debugMessage.ifBlank { "Unable to launch checkout." }
                    )
                }
            }

            val updateEvent = withTimeoutOrNull(PURCHASE_UPDATE_TIMEOUT_MS) {
                purchaseUpdates.first { it.emittedAt >= launchStartedAt }
            }

            if (updateEvent != null) {
                when (updateEvent.result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        val purchase = selectPurchase(updateEvent.purchases, productId)
                        if (purchase != null) {
                            return@withLock mapPurchaseResult(productId, purchase)
                        }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        return@withLock BillingPurchaseResult.UserCanceled
                    }
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        return@withLock purchaseFromStoreOrFailure(productId)
                    }
                    else -> {
                        return@withLock BillingPurchaseResult.Failure(
                            updateEvent.result.debugMessage.ifBlank { "Checkout failed." }
                        )
                    }
                }
            }

            purchaseFromStoreOrFailure(productId)
        }
    }

    override suspend fun queryActiveSubscriptions(): BillingRestoreResult = withContext(ioDispatcher) {
        val ready = ensureConnected()
        if (ready.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext BillingRestoreResult.Failure(
                ready.debugMessage.ifBlank { "Unable to connect to Google Play Billing." }
            )
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val queryResult = suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                continuation.resume(billingResult to purchases)
            }
        }

        val billingResult = queryResult.first
        val purchases = queryResult.second

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext BillingRestoreResult.Failure(
                billingResult.debugMessage.ifBlank { "Unable to query purchases." }
            )
        }

        val active = purchases.flatMap { purchase ->
            purchase.products.map { productId ->
                toSubscriptionPurchase(productId = productId, purchase = purchase)
            }
        }.sortedByDescending { it.purchaseTime }

        BillingRestoreResult.Success(active)
    }

    private suspend fun purchaseFromStoreOrFailure(productId: String): BillingPurchaseResult {
        return when (val restored = queryActiveSubscriptions()) {
            is BillingRestoreResult.Failure -> BillingPurchaseResult.Failure(restored.message)
            is BillingRestoreResult.Success -> {
                val purchase = restored.purchases.firstOrNull { it.productId == productId }
                if (purchase == null) {
                    BillingPurchaseResult.Failure("No active purchase found after checkout.")
                } else {
                    mapPurchaseResult(productId = productId, purchase = purchase)
                }
            }
        }
    }

    private suspend fun mapPurchaseResult(
        productId: String,
        purchase: BillingSubscriptionPurchase
    ): BillingPurchaseResult {
        return when (purchase.state) {
            BillingPurchaseState.PENDING -> BillingPurchaseResult.Pending
            BillingPurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val ack = acknowledge(purchase.purchaseToken)
                    if (ack.responseCode != BillingClient.BillingResponseCode.OK) {
                        return BillingPurchaseResult.Failure(
                            ack.debugMessage.ifBlank { "Unable to acknowledge purchase." }
                        )
                    }
                }
                BillingPurchaseResult.Success(
                    purchase.copy(
                        productId = productId,
                        isAcknowledged = true
                    )
                )
            }
            BillingPurchaseState.UNKNOWN -> BillingPurchaseResult.Failure("Purchase state is not valid.")
        }
    }

    private fun selectPurchase(
        purchases: List<Purchase>,
        productId: String
    ): BillingSubscriptionPurchase? {
        return purchases
            .firstOrNull { purchase -> purchase.products.any { it == productId } }
            ?.let { toSubscriptionPurchase(productId = productId, purchase = it) }
    }

    private data class SelectedSubscriptionOffer(
        val offerToken: String,
        val recurringPhase: ProductDetails.PricingPhase,
        val hasIntroductoryPhase: Boolean
    )

    private fun selectOffer(productDetails: ProductDetails): SelectedSubscriptionOffer? {
        return productDetails.subscriptionOfferDetails
            .orEmpty()
            .mapNotNull { offerDetails ->
                val phases = offerDetails.pricingPhases.pricingPhaseList
                val recurringPhase = phases.lastOrNull { phase ->
                    phase.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
                } ?: phases.lastOrNull() ?: return@mapNotNull null

                val offerToken = offerDetails.offerToken.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                SelectedSubscriptionOffer(
                    offerToken = offerToken,
                    recurringPhase = recurringPhase,
                    hasIntroductoryPhase = phases.size > 1
                )
            }
            .firstOrNull()
    }

    private suspend fun queryProductDetails(productId: String): ProductDetails? {
        return queryProductDetails(listOf(productId))?.firstOrNull()
    }

    private suspend fun queryProductDetails(productIds: List<String>): List<ProductDetails>? {
        if (productIds.isEmpty()) return emptyList()
        val product = QueryProductDetailsParams.Product.newBuilder()
        val products = productIds.map { productId ->
            product
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        val result = suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                continuation.resume(billingResult to productDetailsList)
            }
        }

        val billingResult = result.first
        val details = result.second
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        return details
    }

    private suspend fun acknowledge(purchaseToken: String): BillingResult {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { billingResult ->
                continuation.resume(billingResult)
            }
        }
    }

    private suspend fun ensureConnected(): BillingResult {
        if (billingClient.isReady) return okResult("already_connected")
        return connectionMutex.withLock {
            if (billingClient.isReady) return@withLock okResult("already_connected")
            suspendCancellableCoroutine { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        continuation.resume(billingResult)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) {
                            continuation.resume(
                                errorResult("Billing service disconnected.")
                            )
                        }
                    }
                })
            }
        }
    }

    private fun toSubscriptionPurchase(
        productId: String,
        purchase: Purchase
    ): BillingSubscriptionPurchase {
        val state = when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> BillingPurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> BillingPurchaseState.PENDING
            else -> BillingPurchaseState.UNKNOWN
        }
        return BillingSubscriptionPurchase(
            productId = productId,
            purchaseToken = purchase.purchaseToken,
            purchaseTime = purchase.purchaseTime,
            isAutoRenewing = purchase.isAutoRenewing,
            isAcknowledged = purchase.isAcknowledged,
            state = state,
            orderId = purchase.orderId
        )
    }

    private fun okResult(message: String): BillingResult {
        return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage(message)
            .build()
    }

    private fun errorResult(message: String): BillingResult {
        return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .setDebugMessage(message)
            .build()
    }

    private companion object {
        const val PURCHASE_UPDATE_TIMEOUT_MS = 25_000L
        const val MAX_OBFUSCATED_ID_LENGTH = 64
    }
}

