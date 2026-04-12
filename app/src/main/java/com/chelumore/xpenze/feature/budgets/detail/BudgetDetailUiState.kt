package com.chelumore.xpenze.feature.budgets.detail

import com.chelumore.xpenze.domain.model.Budget

data class BudgetDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val budget: Budget? = null,
    val isPast: Boolean = false,
    val categoryName: String? = null,
    val accountName: String? = null,
    val timeframeLabel: String = "",
    val statusLabel: String = "",
    val nextTimeframeLabel: String? = null,
    val canCreateNextBudget: Boolean = false,
    val transactionCount: Int = 0,
    val spentCents: Long = 0L,
    val remainingCents: Long = 0L,
    val progress: Float = 0f,
    val averageSpendCents: Long = 0L,
    val largestExpenseTitle: String? = null,
    val largestExpenseCents: Long = 0L,
    val breakdown: List<BudgetBreakdownItemUi> = emptyList(),
    val transactions: List<BudgetDetailTransactionUi> = emptyList()
)

data class BudgetBreakdownItemUi(
    val title: String,
    val subtitle: String,
    val totalAmountCents: Long,
    val transactionCount: Int,
    val sharePercent: Float,
    val colorHex: String,
    val iconName: String
)

data class BudgetDetailTransactionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountCents: Long,
    val currency: String,
    val dateLabel: String,
    val categoryColorHex: String,
    val categoryIconName: String,
    val isRecurring: Boolean,
    val hasReceipt: Boolean
)
