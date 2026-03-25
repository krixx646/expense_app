package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.TransactionEntity
import com.chelumore.xpenze.core.database.entity.TransactionType as EntityTransactionType
import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType as DomainTransactionType

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        money = Money(amountCents = amount, currency = currency),
        type = DomainTransactionType.valueOf(type.name),
        title = title,
        notes = notes,
        dateTime = dateTime,
        receiptUri = receiptUri,
        isRecurring = isRecurring,
        recurringRuleId = recurringRuleId,
        tags = tags,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        amount = money.amountCents,
        currency = money.currency,
        type = EntityTransactionType.valueOf(type.name),
        title = title,
        notes = notes,
        dateTime = dateTime,
        receiptUri = receiptUri,
        isRecurring = isRecurring,
        recurringRuleId = recurringRuleId,
        tags = tags,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}


