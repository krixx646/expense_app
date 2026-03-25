package com.chelumore.xpenze.feature.transactions.addedit

import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.MerchantMemorySuggestion
import com.chelumore.xpenze.domain.model.RecurringFrequency
import com.chelumore.xpenze.domain.model.TransactionType

data class AddEditTransactionUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,

    val isEdit: Boolean = false,
    val transactionId: String? = null,
    val draftId: String? = null,

    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val currency: String = Constants.DEFAULT_CURRENCY,
    val title: String = "",
    val notes: String? = null,
    val dateTime: Long = System.currentTimeMillis(),
    val receiptUri: String? = null,
    val tagsText: String = "",
    val isRecurring: Boolean = false,
    val recurringRuleId: String? = null,
    val recurringFrequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val recurringIntervalText: String = "1",

    // Selections by ID (stable across list refresh)
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val selectedToAccountId: String? = null,
    val merchantSuggestion: MerchantMemorySuggestion? = null,

    // Data lists
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
) {
    val selectedCategory: Category?
        get() = categories.firstOrNull { it.id == selectedCategoryId }

    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

    val selectedToAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedToAccountId }

    val canSave: Boolean
        get() {
            val validAmount = (amountText.toDoubleOrNull() ?: 0.0) > 0
            val accountId = selectedAccount?.id
            val validAccount = !accountId.isNullOrBlank()
            val validToAccount = if (type == TransactionType.TRANSFER) {
                val toId = selectedToAccount?.id
                !toId.isNullOrBlank() && toId != accountId
            } else true

            return validAmount && validAccount && validToAccount && !isSaving
        }
}


