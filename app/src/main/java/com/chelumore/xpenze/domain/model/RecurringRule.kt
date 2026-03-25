package com.chelumore.xpenze.domain.model

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurringRule(
    val id: String,
    // Template transaction data
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val money: Money,
    val type: TransactionType,
    val title: String,
    val notes: String? = null,
    // Recurrence settings
    val frequency: RecurringFrequency,
    val interval: Int = 1,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null,
    // Scheduling
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long,
    val lastProcessed: Long? = null,
    val isActive: Boolean = true,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)


