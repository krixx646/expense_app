package com.chelumore.xpenze.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

data class Transaction(
    val id: String,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val money: Money,
    val type: TransactionType,
    val title: String,
    val notes: String? = null,
    val dateTime: Long,
    val receiptUri: String? = null,
    val isRecurring: Boolean = false,
    val recurringRuleId: String? = null,
    val tags: List<String> = emptyList(),
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)


