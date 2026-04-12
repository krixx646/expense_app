package com.chelumore.xpenze.core.billing

enum class BillingPurchaseState {
    PURCHASED,
    PENDING,
    UNKNOWN
}

data class BillingSubscriptionPurchase(
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val isAutoRenewing: Boolean,
    val isAcknowledged: Boolean,
    val state: BillingPurchaseState,
    val orderId: String?
)

data class BillingOneTimePurchase(
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val isAcknowledged: Boolean,
    val state: BillingPurchaseState,
    val orderId: String?
)

data class BillingSubscriptionPlan(
    val productId: String,
    val offerToken: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriod: String,
    val hasIntroductoryPhase: Boolean
)

sealed interface BillingPurchaseResult {
    data class Success(val purchase: BillingSubscriptionPurchase) : BillingPurchaseResult
    data object UserCanceled : BillingPurchaseResult
    data object Pending : BillingPurchaseResult
    data object ProductUnavailable : BillingPurchaseResult
    data class Failure(val message: String) : BillingPurchaseResult
}

sealed interface BillingRestoreResult {
    data class Success(val purchases: List<BillingSubscriptionPurchase>) : BillingRestoreResult
    data class Failure(val message: String) : BillingRestoreResult
}

sealed interface BillingOneTimeRestoreResult {
    data class Success(val purchases: List<BillingOneTimePurchase>) : BillingOneTimeRestoreResult
    data class Failure(val message: String) : BillingOneTimeRestoreResult
}

sealed interface BillingCatalogResult {
    data class Success(
        val plans: List<BillingSubscriptionPlan>
    ) : BillingCatalogResult

    data class Failure(
        val message: String
    ) : BillingCatalogResult
}

