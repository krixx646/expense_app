package com.chelumore.xpenze.feature.budgets.addedit

import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Category

data class AddEditBudgetUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,

    val isEdit: Boolean = false,
    val budgetId: String? = null,

    // Form
    val name: String = "",
    val amountText: String = "",
    val currencyCode: String = Constants.DEFAULT_CURRENCY,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val alertThreshold: Float = 0.8f,
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,

    val selectedCategoryId: String? = null, // null = all
    val selectedAccountId: String? = null, // null = all

    // Data
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
) {
    val selectedCategory: Category?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedAccountId }

    val canSave: Boolean
        get() = (amountText.toDoubleOrNull() ?: 0.0) > 0.0 && !isSaving
}


