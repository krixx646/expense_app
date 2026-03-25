package com.chelumore.xpenze.feature.accounts.list

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.repository.AccountCardStyle

data class AccountsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val showArchived: Boolean = false,
    val totalBalanceCents: Long = 0L,
    val accountCardStyle: AccountCardStyle = AccountCardStyle.MATTE,
    val accountCardStyleOverrides: Map<String, AccountCardStyle> = emptyMap(),
    val accounts: List<Account> = emptyList()
) {
    val visibleAccounts: List<Account>
        get() = if (showArchived) accounts else accounts.filter { !it.isArchived }
}


