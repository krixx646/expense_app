package com.chelumore.xpenze.feature.budgets.detail

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.Transaction

data class BudgetDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val budget: Budget? = null,
    val categoryName: String? = null,
    val accountName: String? = null,

    val spentCents: Long = 0L,
    val remainingCents: Long = 0L,
    val progress: Float = 0f,
    val transactions: List<Transaction> = emptyList()
)


