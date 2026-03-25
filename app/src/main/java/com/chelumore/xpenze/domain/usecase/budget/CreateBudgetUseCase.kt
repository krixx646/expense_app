package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.repository.BudgetRepository
import java.util.UUID
import javax.inject.Inject

class CreateBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    /**
     * Creates a budget and returns the generated budget id.
     */
    suspend operator fun invoke(
        name: String,
        amountLimitCents: Long,
        period: BudgetPeriod,
        categoryId: String? = null,
        accountId: String? = null,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null,
        alertThresholdPercent: Int = 80,
        isActive: Boolean = true
    ): String {
        require(name.isNotBlank()) { "name is required" }
        require(amountLimitCents > 0) { "amountLimitCents must be > 0" }
        require(alertThresholdPercent in 1..100) { "alertThresholdPercent must be 1..100" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val budget = Budget(
            id = id,
            name = name,
            categoryId = categoryId,
            accountId = accountId,
            amountLimitCents = amountLimitCents,
            period = period,
            startDate = startDate,
            endDate = endDate,
            alertThresholdPercent = alertThresholdPercent,
            isActive = isActive,
            synced = false,
            deleted = false,
            createdAt = now,
            updatedAt = now
        )

        budgetRepository.insert(budget)
        return id
    }
}


