package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.nextTimeframeRange
import com.chelumore.xpenze.domain.model.resolvedEndDate
import com.chelumore.xpenze.domain.repository.BudgetRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.hasPremiumAccess
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ProcessRecurringBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()) {
        val prefs = userRepository.userPreferences.first()
        if (!prefs.hasPremiumAccess(nowMillis)) return

        val zoneId = ZoneId.systemDefault()
        val recurringBudgets = budgetRepository.getAll().first()
            .asSequence()
            .filter { it.isRecurring && it.isActive && !it.deleted }
            .sortedBy { it.startDate }
            .toList()
        if (recurringBudgets.isEmpty()) return

        val budgetsBySeries = recurringBudgets
            .groupBy { it.seriesId?.takeIf(String::isNotBlank) ?: it.id }
            .mapValues { (_, budgets) -> budgets.sortedBy { it.startDate }.toMutableList() }

        for ((seriesId, seriesBudgets) in budgetsBySeries) {
            var current = seriesBudgets.maxByOrNull { it.startDate } ?: continue
            current = normalizeBudget(current, seriesId, zoneId, nowMillis)
            replaceBudget(seriesBudgets, current)

            while (current.isRecurring) {
                val currentEnd = current.resolvedEndDate(zoneId) ?: break
                if (currentEnd >= nowMillis) break

                val nextRange = current.nextTimeframeRange(zoneId) ?: break
                if (nextRange.startMillis <= current.startDate) break

                val existingNext = seriesBudgets.firstOrNull { it.startDate == nextRange.startMillis && !it.deleted }
                if (existingNext != null) {
                    current = normalizeBudget(existingNext, seriesId, zoneId, nowMillis)
                    replaceBudget(seriesBudgets, current)
                    continue
                }

                val createdAt = System.currentTimeMillis()
                val nextBudget = current.copy(
                    id = UUID.randomUUID().toString(),
                    startDate = nextRange.startMillis,
                    endDate = nextRange.endMillis,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    synced = false,
                    deleted = false,
                    isActive = true,
                    isRecurring = true,
                    seriesId = seriesId
                )
                budgetRepository.insert(nextBudget)
                seriesBudgets.add(nextBudget)
                seriesBudgets.sortBy { it.startDate }
                current = nextBudget
            }
        }
    }

    private suspend fun normalizeBudget(
        budget: Budget,
        seriesId: String,
        zoneId: ZoneId,
        nowMillis: Long
    ): Budget {
        val normalizedEndDate = budget.resolvedEndDate(zoneId)
        val needsUpdate = budget.seriesId != seriesId || budget.endDate != normalizedEndDate
        if (!needsUpdate) return budget

        val updatedBudget = budget.copy(
            endDate = normalizedEndDate,
            seriesId = seriesId,
            updatedAt = nowMillis,
            synced = false
        )
        budgetRepository.update(updatedBudget)
        return updatedBudget
    }

    private fun replaceBudget(seriesBudgets: MutableList<Budget>, budget: Budget) {
        val index = seriesBudgets.indexOfFirst { it.id == budget.id }
        if (index >= 0) {
            seriesBudgets[index] = budget
        } else {
            seriesBudgets.add(budget)
            seriesBudgets.sortBy { it.startDate }
        }
    }
}
