package com.chelumore.xpenze.domain.usecase.analytics

data class IncomeVsExpense(
    val incomeCents: Long,
    val expenseCents: Long,
    val netCents: Long
)

data class CashFlowSummary(
    val incomeCents: Long,
    val expenseCents: Long,
    val netCents: Long
)

data class CategorySpending(
    val categoryId: String?, // null = Uncategorized
    val categoryName: String,
    val amountCents: Long,
    val percent: Float
)

data class CashFlowTrendPoint(
    val dayStartMillis: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val netCents: Long
)


