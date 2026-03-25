package com.chelumore.xpenze.domain.usecase.analytics

import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetIncomeVsExpenseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(range: DateRange): Flow<IncomeVsExpense> {
        return combine(
            transactionRepository.getIncomeByDateRange(range.startMillis, range.endMillis),
            transactionRepository.getExpensesByDateRange(range.startMillis, range.endMillis)
        ) { income, expense ->
            IncomeVsExpense(
                incomeCents = income,
                expenseCents = expense,
                netCents = income - expense
            )
        }
    }
}


