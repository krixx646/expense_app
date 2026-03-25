package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.UserSubscriptionEntity
import com.chelumore.xpenze.domain.model.UserSubscription

fun UserSubscriptionEntity.toDomain(): UserSubscription {
    return UserSubscription(
        id = id,
        productId = productId,
        purchaseToken = purchaseToken,
        purchaseTime = purchaseTime,
        expiryTime = expiryTime,
        isAutoRenewing = isAutoRenewing,
        isActive = isActive,
        acknowledgedAt = acknowledgedAt,
        updatedAt = updatedAt
    )
}

fun UserSubscription.toEntity(): UserSubscriptionEntity {
    return UserSubscriptionEntity(
        id = id,
        productId = productId,
        purchaseToken = purchaseToken,
        purchaseTime = purchaseTime,
        expiryTime = expiryTime,
        isAutoRenewing = isAutoRenewing,
        isActive = isActive,
        acknowledgedAt = acknowledgedAt,
        updatedAt = updatedAt
    )
}


