package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.RecurringFrequency as EntityRecurringFrequency
import com.chelumore.xpenze.core.database.entity.RecurringRuleEntity
import com.chelumore.xpenze.core.database.entity.TransactionType as EntityTransactionType
import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.RecurringFrequency as DomainRecurringFrequency
import com.chelumore.xpenze.domain.model.RecurringRule
import com.chelumore.xpenze.domain.model.TransactionType as DomainTransactionType

fun RecurringRuleEntity.toDomain(): RecurringRule {
    return RecurringRule(
        id = id,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        money = Money(amountCents = amount, currency = currency),
        type = DomainTransactionType.valueOf(type.name),
        title = title,
        notes = notes,
        frequency = DomainRecurringFrequency.valueOf(frequency.name),
        interval = interval,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        lastProcessed = lastProcessed,
        isActive = isActive,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RecurringRule.toEntity(): RecurringRuleEntity {
    return RecurringRuleEntity(
        id = id,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        amount = money.amountCents,
        currency = money.currency,
        type = EntityTransactionType.valueOf(type.name),
        title = title,
        notes = notes,
        frequency = EntityRecurringFrequency.valueOf(frequency.name),
        interval = interval,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        lastProcessed = lastProcessed,
        isActive = isActive,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}


