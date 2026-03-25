package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.repository.BudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(id: String) {
        require(id.isNotBlank()) { "id is required" }
        budgetRepository.delete(id)
    }
}


