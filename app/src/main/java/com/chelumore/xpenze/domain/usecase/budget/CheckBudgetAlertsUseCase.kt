package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.Transaction
import javax.inject.Inject

data class BudgetAlert(
    val budgetId: String,
    val budgetName: String,
    val percentUsed: Int,
    val overspentCents: Long
)

class CheckBudgetAlertsUseCase @Inject constructor(
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase
) {
    /**
     * Returns budgets that crossed their alert threshold (or exceeded).
     */
    operator fun invoke(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        range: DateRange? = null
    ): List<BudgetAlert> {
        return budgets
            .asSequence()
            .filter { it.isActive }
            .map { budget ->
                val progress = getBudgetProgressUseCase(budget, transactions, range)
                val percentUsed = if (progress.limitCents > 0) {
                    ((progress.spentCents.toDouble() / progress.limitCents.toDouble()) * 100.0).toInt()
                } else 0
                Triple(budget, percentUsed, progress)
            }
            .filter { (budget, percentUsed, _) -> percentUsed >= budget.alertThresholdPercent }
            .map { (budget, percentUsed, progress) ->
                BudgetAlert(
                    budgetId = budget.id,
                    budgetName = budget.name,
                    percentUsed = percentUsed,
                    overspentCents = (progress.spentCents - progress.limitCents).coerceAtLeast(0L)
                )
            }
            .toList()
    }
}


