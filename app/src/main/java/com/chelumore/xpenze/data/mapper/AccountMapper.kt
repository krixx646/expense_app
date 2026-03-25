package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.AccountEntity
import com.chelumore.xpenze.core.database.entity.AccountType as EntityAccountType
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType as DomainAccountType

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = DomainAccountType.valueOf(type.name),
        currency = currency,
        initialBalanceCents = initialBalance,
        currentBalanceCents = currentBalance,
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault,
        isArchived = isArchived,
        sortOrder = sortOrder,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        type = EntityAccountType.valueOf(type.name),
        currency = currency,
        initialBalance = initialBalanceCents,
        currentBalance = currentBalanceCents,
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault,
        isArchived = isArchived,
        sortOrder = sortOrder,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}


