package com.chelumore.xpenze.feature.transactions.detail

import com.chelumore.xpenze.domain.model.Transaction

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val transaction: Transaction? = null,
    val accountName: String? = null,
    val toAccountName: String? = null,
    val categoryName: String? = null
)


