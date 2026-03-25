package com.chelumore.xpenze.domain.usecase.analytics

import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetTrendDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Builds a day-by-day cashflow trend for the given [range].
     *
     * Includes empty days (0 totals) so charts have a stable x-axis.
     */
    operator fun invoke(
        range: DateRange,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<List<CashFlowTrendPoint>> {
        return transactionRepository.getByDateRange(range.startMillis, range.endMillis).map { transactions ->
            val byDate: Map<LocalDate, List<com.chelumore.xpenze.domain.model.Transaction>> =
                transactions.groupBy { tx ->
                    Instant.ofEpochMilli(tx.dateTime).atZone(zoneId).toLocalDate()
                }

            val startDate = Instant.ofEpochMilli(range.startMillis).atZone(zoneId).toLocalDate()
            val endDate = Instant.ofEpochMilli(range.endMillis).atZone(zoneId).toLocalDate()

            val points = mutableListOf<CashFlowTrendPoint>()
            var cursor = startDate
            while (!cursor.isAfter(endDate)) {
                val dayTx = byDate[cursor].orEmpty()
                val income = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }
                val expense = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
                val net = income - expense

                val dayStartMillis = cursor.atStartOfDay(zoneId).toInstant().toEpochMilli()
                points.add(
                    CashFlowTrendPoint(
                        dayStartMillis = dayStartMillis,
                        incomeCents = income,
                        expenseCents = expense,
                        netCents = net
                    )
                )
                cursor = cursor.plusDays(1)
            }

            points
        }
    }
}


