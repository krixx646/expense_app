package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.matchesBudgetScope
import javax.inject.Inject
import kotlin.math.max

data class BudgetProgress(
    val budgetId: String,
    val limitCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val progress: Float
)

class GetBudgetProgressUseCase @Inject constructor() {
    /**
     * Pure calculation use-case: no repository I/O.
     *
     * Filters transactions to EXPENSE within [range] and matching budget category/account if set.
     */
    operator fun invoke(
        budget: Budget,
        transactions: List<Transaction>,
        range: DateRange? = null,
        categories: List<Category> = emptyList()
    ): BudgetProgress {
        val limit = budget.amountLimitCents
        val spent = transactions.asSequence()
            .filter { tx ->
                val inRange = range?.contains(tx.dateTime) ?: true
                val scopeOk = if (categories.isEmpty()) {
                    tx.type == TransactionType.EXPENSE &&
                        (budget.categoryId?.let { it == tx.categoryId } ?: true) &&
                        (budget.accountId?.let { it == tx.accountId } ?: true)
                } else {
                    tx.matchesBudgetScope(budget, categories)
                }
                inRange && scopeOk
            }
            .sumOf { it.money.amountCents }

        val remaining = limit - spent
        val progress = if (limit > 0) (spent.toFloat() / limit.toFloat()) else 0f

        return BudgetProgress(
            budgetId = budget.id,
            limitCents = limit,
            spentCents = spent,
            remainingCents = remaining,
            progress = max(0f, progress)
        )
    }
}


