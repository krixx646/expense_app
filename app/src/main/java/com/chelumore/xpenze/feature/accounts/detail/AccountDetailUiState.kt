package com.chelumore.xpenze.feature.accounts.detail

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.repository.AccountCardStyle

data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val globalCardStyle: AccountCardStyle = AccountCardStyle.MATTE,
    val accountCardStyleOverride: AccountCardStyle? = null
)


