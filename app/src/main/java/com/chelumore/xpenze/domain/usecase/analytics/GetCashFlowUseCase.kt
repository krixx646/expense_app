package com.chelumore.xpenze.domain.usecase.analytics

import com.chelumore.xpenze.domain.model.DateRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCashFlowUseCase @Inject constructor(
    private val getIncomeVsExpenseUseCase: GetIncomeVsExpenseUseCase
) {
    operator fun invoke(range: DateRange): Flow<CashFlowSummary> {
        return getIncomeVsExpenseUseCase(range).map { summary ->
            CashFlowSummary(
                incomeCents = summary.incomeCents,
                expenseCents = summary.expenseCents,
                netCents = summary.netCents
            )
        }
    }
}


