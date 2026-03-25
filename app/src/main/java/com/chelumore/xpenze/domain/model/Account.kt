package com.chelumore.xpenze.domain.model

enum class AccountType {
    CASH,
    BANK,
    CREDIT_CARD,
    SAVINGS,
    INVESTMENT,
    OTHER
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalanceCents: Long,
    val currentBalanceCents: Long,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)


