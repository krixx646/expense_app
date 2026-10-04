package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class BudgetLifecycleQuery {
    ALL,
    ACTIVE,
    CURRENT,
    PAST
}

class GetBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(activeOnly: Boolean = false): Flow<List<Budget>> {
        return if (activeOnly) budgetRepository.getActive() else budgetRepository.getAll()
    }

    operator fun invoke(query: BudgetLifecycleQuery): Flow<List<Budget>> {
        return when (query) {
            BudgetLifecycleQuery.ALL -> budgetRepository.getAll()
            BudgetLifecycleQuery.ACTIVE -> budgetRepository.getActive()
            BudgetLifecycleQuery.CURRENT -> budgetRepository.getCurrentBudgets()
            BudgetLifecycleQuery.PAST -> budgetRepository.getPastBudgets()
        }
    }
}


