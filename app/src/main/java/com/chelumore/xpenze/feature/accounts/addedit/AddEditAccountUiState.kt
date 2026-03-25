package com.chelumore.xpenze.feature.accounts.addedit

import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType

data class AddEditAccountUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,

    val isEdit: Boolean = false,
    val accountId: String? = null,

    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val currency: String = Constants.DEFAULT_CURRENCY,
    val initialBalanceText: String = "0.00",
    val colorHex: String = "#81C784",
    val iconName: String = "wallet",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,

    val existingAccount: Account? = null
) {
    val canSave: Boolean
        get() = name.isNotBlank() && currency.isNotBlank() && !isSaving
}


