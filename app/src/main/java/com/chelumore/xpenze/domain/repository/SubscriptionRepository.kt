package com.chelumore.xpenze.domain.repository

import android.app.Activity
import com.chelumore.xpenze.domain.model.UserSubscription
import kotlinx.coroutines.flow.Flow

sealed interface TrialActivationResult {
    data class Started(
        val startedAt: Long,
        val expiresAt: Long
    ) : TrialActivationResult

    data class AlreadyConsumed(
        val startedAt: Long,
        val expiresAt: Long
    ) : TrialActivationResult

    data object NotSignedIn : TrialActivationResult
    data object BackendUnavailable : TrialActivationResult
    data class Failure(val message: String) : TrialActivationResult
}

sealed interface PurchaseSubscriptionResult {
    data class Success(val subscription: UserSubscription) : PurchaseSubscriptionResult
    data object NotSignedIn : PurchaseSubscriptionResult
    data object UserCanceled : PurchaseSubscriptionResult
    data object Pending : PurchaseSubscriptionResult
    data object ProductUnavailable : PurchaseSubscriptionResult
    data class Failure(val message: String) : PurchaseSubscriptionResult
}

sealed interface RestorePurchasesResult {
    data class Restored(val activeCount: Int) : RestorePurchasesResult
    data object NoActivePurchases : RestorePurchasesResult
    data class Failure(val message: String) : RestorePurchasesResult
}

interface SubscriptionRepository {
    fun getActiveSubscription(): Flow<UserSubscription?>
    fun isPremium(): Flow<Boolean>

    suspend fun saveSubscription(subscription: UserSubscription)
    suspend fun updateSubscription(subscription: UserSubscription)
    suspend fun deactivateExpiredSubscriptions()
    suspend fun markAcknowledged(id: String)
    suspend fun clearSubscriptions()
    suspend fun syncTrialStateFromBackend()
    suspend fun syncPremiumStateFromBackend()
    suspend fun claimTrial(days: Int = 7): TrialActivationResult
    suspend fun purchaseSubscription(
        activity: Activity,
        productId: String
    ): PurchaseSubscriptionResult
    suspend fun restorePurchases(): RestorePurchasesResult
}

