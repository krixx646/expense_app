package com.chelumore.xpenze.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_subscriptions")
data class UserSubscriptionEntity(
    @PrimaryKey
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

