package com.chelumore.xpenze.feature.budgets.add

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Category

data class AddBudgetUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,

    // Form Data
    val name: String = "",
    val amountText: String = "",
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val selectedCategory: Category? = null, // null = All Categories
    val selectedAccount: Account? = null, // null = All Accounts
    val alertThreshold: Float = 0.8f, // 80%

    // Selection Data
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
) {
    val canSave: Boolean
        get() = name.isNotBlank() && 
                amountText.toDoubleOrNull() != null && 
                amountText.toDouble() > 0
}

