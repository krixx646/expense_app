package com.chelumore.xpenze.feature.budgets

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Category

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasPremiumAccess: Boolean = false,

    // Filters
    val selectedFilter: BudgetFilter = BudgetFilter.ALL,
    val selectedPeriod: BudgetPeriod? = null,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,

    // Options for filters
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),

    // Data
    val currentBudgets: List<BudgetUiModel> = emptyList(),
    val pastBudgets: List<BudgetUiModel> = emptyList(),
    val expiredPrompt: ExpiredBudgetPromptUi? = null,

    // Summary Counts
    val currentCount: Int = 0,
    val pastCount: Int = 0,
    val activeCount: Int = 0,
    val warningCount: Int = 0,
    val exceededCount: Int = 0
)

data class BudgetUiModel(
    val id: String,
    val name: String,
    val categoryId: String?,
    val accountId: String?,
    val categoryName: String,
    val accountName: String,
    val categoryColorHex: String,
    val categoryIconName: String,
    val limitAmount: Long,
    val spentAmount: Long,
    val period: BudgetPeriod,
    val timeframeLabel: String,
    val metaLabel: String,
    val daysLeft: Int,
    val dailySpendLimit: Double,
    val progress: Float,
    val status: BudgetStatus,
    val isPast: Boolean,
    val isRecurring: Boolean,
    val endDateMillis: Long?,
    val transactionCount: Int
) {
    val remainingAmount: Long
        get() = limitAmount - spentAmount
}

data class ExpiredBudgetPromptUi(
    val budgetId: String,
    val budgetName: String,
    val timeframeLabel: String,
    val spentAmount: Long,
    val limitAmount: Long
)

enum class BudgetStatus {
    ON_TRACK,
    WARNING,
    EXCEEDED
}

enum class BudgetFilter {
    ALL,
    ON_TRACK,
    WARNING,
    EXCEEDED
}

enum class BudgetSortOption {
    NAME,
    USAGE_DESC,
    REMAINING_ASC
}
