package com.chelumore.xpenze.feature.transactions.add

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.TransactionType

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val title: String = "",
    val notes: String? = null,
    
    // Selections
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val selectedToAccount: Account? = null, // For Transfer
    
    // Data Lists
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() {
            val validAmount = (amountText.toDoubleOrNull() ?: 0.0) > 0
            val validTitle = title.isNotBlank()
            val validAccount = selectedAccount != null
            val validCategory = if (type != TransactionType.TRANSFER) selectedCategory != null else true
            val validToAccount = if (type == TransactionType.TRANSFER) selectedToAccount != null && selectedToAccount != selectedAccount else true
            
            return validAmount && validTitle && validAccount && validCategory && validToAccount
        }
}
