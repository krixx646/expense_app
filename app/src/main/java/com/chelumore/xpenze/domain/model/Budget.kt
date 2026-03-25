package com.chelumore.xpenze.domain.model

enum class BudgetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

data class Budget(
    val id: String,
    val name: String,
    val categoryId: String?, // null = all categories
    val accountId: String?, // null = all accounts
    val amountLimitCents: Long,
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long?,
    val alertThresholdPercent: Int,
    val isActive: Boolean = true,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)


