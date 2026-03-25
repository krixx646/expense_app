package com.chelumore.xpenze.feature.accounts.transfer

import com.chelumore.xpenze.domain.model.Account

data class TransferUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,

    val accounts: List<Account> = emptyList(),
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val amountText: String = "",
    val notes: String = ""
) {
    val canSave: Boolean
        get() {
            val amountOk = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            val fromOk = fromAccount != null
            val toOk = toAccount != null && toAccount?.id != fromAccount?.id
            return amountOk && fromOk && toOk && !isSaving
        }
}


