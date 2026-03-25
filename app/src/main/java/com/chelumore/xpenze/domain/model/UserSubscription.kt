package com.chelumore.xpenze.domain.model

data class UserSubscription(
    val id: String,
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val expiryTime: Long,
    val isAutoRenewing: Boolean,
    val isActive: Boolean,
    val acknowledgedAt: Long? = null,
    val updatedAt: Long
)


