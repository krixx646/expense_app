package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.BudgetEntity
import com.chelumore.xpenze.core.database.entity.BudgetPeriod as EntityBudgetPeriod
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod as DomainBudgetPeriod

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = id,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        amountLimitCents = amountLimitCents,
        period = DomainBudgetPeriod.valueOf(period.name),
        startDate = startDate,
        endDate = endDate,
        alertThresholdPercent = alertThresholdPercent,
        isActive = isActive,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isRecurring = isRecurring,
        seriesId = seriesId
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        amountLimitCents = amountLimitCents,
        period = EntityBudgetPeriod.valueOf(period.name),
        startDate = startDate,
        endDate = endDate,
        alertThresholdPercent = alertThresholdPercent,
        isActive = isActive,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isRecurring = isRecurring,
        seriesId = seriesId
    )
}
