package com.chelumore.xpenze.domain.model

/**
 * Budget augmented with computed spending/progress.
 *
 * This is derived data (computed from transactions) and is NOT stored in the database.
 */
data class BudgetWithProgress(
    val budget: Budget,
    val spentCents: Long,
    val progress: Float
) {
    val remainingCents: Long
        get() = budget.amountLimitCents - spentCents
}


