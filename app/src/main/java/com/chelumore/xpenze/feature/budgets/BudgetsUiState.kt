package com.chelumore.xpenze.feature.budgets

import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasPremiumAccess: Boolean = false,
    
    // Filters
    val selectedFilter: BudgetFilter = BudgetFilter.ALL,
    val selectedPeriod: BudgetPeriod? = null, // null = All
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,

    // Options for filters
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    
    // Data
    val budgets: List<BudgetUiModel> = emptyList(),
    
    // Summary Counts
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
    val limitAmount: Long,
    val spentAmount: Long,
    val period: BudgetPeriod,
    val currentPeriodLabel: String,
    val daysLeft: Int,
    val dailySpendLimit: Double,
    val progress: Float, // 0.0 to 1.0+
    val status: BudgetStatus
) {
    val remainingAmount: Long
        get() = limitAmount - spentAmount
}

enum class BudgetStatus {
    ON_TRACK, WARNING, EXCEEDED
}

enum class BudgetFilter {
    ALL, ON_TRACK, WARNING, EXCEEDED
}

enum class BudgetSortOption {
    NAME, USAGE_DESC, REMAINING_ASC
}

