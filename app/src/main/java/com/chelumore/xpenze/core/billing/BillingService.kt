package com.chelumore.xpenze.core.billing

import android.app.Activity

interface BillingService {
    suspend fun querySubscriptionPlans(
        productIds: List<String>
    ): BillingCatalogResult

    suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        obfuscatedAccountId: String?
    ): BillingPurchaseResult

    suspend fun queryActiveSubscriptions(): BillingRestoreResult
}

