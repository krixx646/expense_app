package com.chelumore.xpenze.core.growth

import com.chelumore.xpenze.core.localization.localizedRecurring
import com.chelumore.xpenze.core.localization.localizedUncategorized
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.model.matchesBudgetScope
import com.chelumore.xpenze.domain.model.resolvedCategoryId
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToLong

enum class PremiumOpportunityType {
    CATEGORY_CUTBACK,
    CATEGORY_SPIKE,
    BUDGET_PRESSURE,
    RECURRING_REVIEW,
    BUDGET_SUGGESTION
}

data class PremiumOpportunity(
    val type: PremiumOpportunityType,
    val label: String,
    val impactCents: Long,
    val amountCents: Long = 0L,
    val comparisonAmountCents: Long = 0L,
    val percentValue: Int = 0,
    val countValue: Int = 0
)

fun buildPremiumOpportunities(
    transactions: List<Transaction>,
    categories: List<Category>,
    budgets: List<Budget>,
    currencyCode: String,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<PremiumOpportunity> {
    val expenseTransactions = transactions.asSequence()
        .filter { !it.deleted }
        .filter { it.type == TransactionType.EXPENSE }
        .filter { it.money.currency.equals(currencyCode, ignoreCase = true) }
        .toList()
    val expenseCategoriesById = categories.asSequence()
        .filter { !it.deleted }
        .filter { it.type == CategoryType.EXPENSE }
        .associateBy { it.id }
    val expenseCategories = expenseCategoriesById.values.toList()

    val opportunities = buildList {
        buildCategoryCutbackOpportunity(
            transactions = expenseTransactions,
            categoriesById = expenseCategoriesById,
            categories = expenseCategories,
            nowMillis = nowMillis
        )?.let(::add)

        buildCategorySpikeOpportunity(
            transactions = expenseTransactions,
            categoriesById = expenseCategoriesById,
            categories = expenseCategories,
            zoneId = zoneId
        )?.let(::add)

        buildBudgetPressureOpportunity(
            transactions = expenseTransactions,
            categories = expenseCategories,
            budgets = budgets,
            nowMillis = nowMillis,
            zoneId = zoneId
        )?.let(::add)

        buildRecurringReviewOpportunity(
            transactions = expenseTransactions,
            nowMillis = nowMillis
        )?.let(::add)

        buildBudgetSuggestionOpportunity(
            transactions = expenseTransactions,
            categoriesById = expenseCategoriesById,
            categories = expenseCategories,
            budgets = budgets,
            zoneId = zoneId
        )?.let(::add)
    }

    val seenLabels = mutableSetOf<String>()
    return opportunities
        .sortedWith(
            compareBy<PremiumOpportunity>(
                { priorityFor(it.type) },
                { if (it.impactCents > 0L) 0 else 1 }
            ).thenByDescending { it.impactCents }
                .thenByDescending { it.amountCents }
                .thenByDescending { it.percentValue }
        )
        .filter { opportunity ->
            val labelKey = when (opportunity.type) {
                PremiumOpportunityType.RECURRING_REVIEW -> opportunity.type.name
                else -> opportunity.label.lowercase()
            }
            seenLabels.add(labelKey)
        }
        .take(MAX_OPPORTUNITIES)
}

private fun buildCategoryCutbackOpportunity(
    transactions: List<Transaction>,
    categoriesById: Map<String, Category>,
    categories: List<Category>,
    nowMillis: Long
): PremiumOpportunity? {
    val recentStart = nowMillis - RECENT_LOOKBACK_MILLIS
    val topCategory = transactions.asSequence()
        .filter { it.dateTime in recentStart..nowMillis }
        .groupBy { it.resolvedCategoryId(categories) }
        .mapValues { (_, values) -> values.sumOf { it.money.amountCents } }
        .maxByOrNull { it.value }
        ?: return null
    if (topCategory.value < MIN_ACTIONABLE_CENTS) return null

    val categoryName = topCategory.key?.let { categoriesById[it]?.name } ?: localizedUncategorized()
    val impact = roundToNearestCurrencyUnit((topCategory.value * 0.10).roundToLong())
    if (impact <= 0L) return null

    return PremiumOpportunity(
        type = PremiumOpportunityType.CATEGORY_CUTBACK,
        label = categoryName,
        impactCents = impact,
        amountCents = topCategory.value
    )
}

private fun buildCategorySpikeOpportunity(
    transactions: List<Transaction>,
    categoriesById: Map<String, Category>,
    categories: List<Category>,
    zoneId: ZoneId
): PremiumOpportunity? {
    val currentMonth = YearMonth.now(zoneId)
    val (currentStart, currentEnd) = monthBounds(currentMonth, zoneId)
    val (previousStart, previousEnd) = monthBounds(currentMonth.minusMonths(1), zoneId)

    val currentTotals = transactions.asSequence()
        .filter { it.dateTime in currentStart..currentEnd }
        .groupBy { it.resolvedCategoryId(categories) }
        .mapValues { (_, values) -> values.sumOf { it.money.amountCents } }
    val previousTotals = transactions.asSequence()
        .filter { it.dateTime in previousStart..previousEnd }
        .groupBy { it.resolvedCategoryId(categories) }
        .mapValues { (_, values) -> values.sumOf { it.money.amountCents } }

    return currentTotals.entries
        .mapNotNull { (categoryId, currentAmount) ->
            val previousAmount = previousTotals[categoryId] ?: return@mapNotNull null
            if (currentAmount <= previousAmount || previousAmount <= 0L) return@mapNotNull null
            val delta = currentAmount - previousAmount
            if (delta < MIN_ACTIONABLE_CENTS) return@mapNotNull null

            val percent = (((currentAmount - previousAmount).toDouble() / previousAmount.toDouble()) * 100.0)
                .roundToLong()
                .toInt()
            if (percent < MIN_SPIKE_PERCENT) return@mapNotNull null

            PremiumOpportunity(
                type = PremiumOpportunityType.CATEGORY_SPIKE,
                label = categoryId?.let { categoriesById[it]?.name } ?: localizedUncategorized(),
                impactCents = delta,
                amountCents = currentAmount,
                comparisonAmountCents = previousAmount,
                percentValue = percent
            )
        }
        .maxByOrNull { it.impactCents }
}

private fun buildBudgetPressureOpportunity(
    transactions: List<Transaction>,
    categories: List<Category>,
    budgets: List<Budget>,
    nowMillis: Long,
    zoneId: ZoneId
): PremiumOpportunity? {
    return budgets.asSequence()
        .filter { !it.deleted && it.isActive && it.amountLimitCents > 0L }
        .mapNotNull { budget ->
            val window = budget.currentPeriodWindow(nowMillis = nowMillis, zoneId = zoneId) ?: return@mapNotNull null
            val spent = transactions.asSequence()
                .filter { it.dateTime in window.spentRange.startMillis..window.spentRange.endMillis }
                .filter { it.matchesBudgetScope(budget, categories) }
                .sumOf { it.money.amountCents }
            val percentUsed = ((spent.toDouble() / budget.amountLimitCents.toDouble()) * 100.0).roundToLong().toInt()
            if (percentUsed < MIN_BUDGET_PRESSURE_PERCENT) return@mapNotNull null

            PremiumOpportunity(
                type = PremiumOpportunityType.BUDGET_PRESSURE,
                label = budget.name,
                impactCents = (spent - budget.amountLimitCents).coerceAtLeast(0L),
                amountCents = spent,
                comparisonAmountCents = budget.amountLimitCents,
                percentValue = percentUsed
            )
        }
        .maxWithOrNull(compareByDescending<PremiumOpportunity> { it.percentValue }.thenByDescending { it.impactCents })
}

private fun buildRecurringReviewOpportunity(
    transactions: List<Transaction>,
    nowMillis: Long
): PremiumOpportunity? {
    val recentStart = nowMillis - RECENT_LOOKBACK_MILLIS
    val recurring = transactions.asSequence()
        .filter { it.isRecurring }
        .filter { it.dateTime in recentStart..nowMillis }
        .toList()
    val recurringTotal = recurring.sumOf { it.money.amountCents }
    if (recurringTotal < MIN_ACTIONABLE_CENTS) return null

    val recurringCount = recurring
        .map { it.recurringRuleId ?: it.id }
        .distinct()
        .size
    val impact = roundToNearestCurrencyUnit((recurringTotal * 0.10).roundToLong())

    return PremiumOpportunity(
        type = PremiumOpportunityType.RECURRING_REVIEW,
        label = localizedRecurring(),
        impactCents = impact,
        amountCents = recurringTotal,
        countValue = recurringCount
    )
}

private fun buildBudgetSuggestionOpportunity(
    transactions: List<Transaction>,
    categoriesById: Map<String, Category>,
    categories: List<Category>,
    budgets: List<Budget>,
    zoneId: ZoneId
): PremiumOpportunity? {
    val trackedMonths = listOf(
        YearMonth.now(zoneId),
        YearMonth.now(zoneId).minusMonths(1),
        YearMonth.now(zoneId).minusMonths(2)
    )
    val totalsByCategory = mutableMapOf<String, Long>()
    val monthsWithSpend = mutableMapOf<String, Int>()

    trackedMonths.forEach { month ->
        val (start, end) = monthBounds(month, zoneId)
        val monthTotals = transactions.asSequence()
            .filter { it.dateTime in start..end }
            .groupBy { it.resolvedCategoryId(categories) }
            .mapValues { (_, values) -> values.sumOf { it.money.amountCents } }

        monthTotals.forEach { (categoryId, total) ->
            val key = categoryId ?: return@forEach
            if (total <= 0L) return@forEach
            totalsByCategory[key] = (totalsByCategory[key] ?: 0L) + total
            monthsWithSpend[key] = (monthsWithSpend[key] ?: 0) + 1
        }
    }

    val categoryIdsWithActiveBudget = budgets.asSequence()
        .filter { !it.deleted && it.isActive }
        .mapNotNull { it.categoryId }
        .toSet()

    return totalsByCategory.entries
        .asSequence()
        .filter { (categoryId, total) ->
            total >= MIN_ACTIONABLE_CENTS && categoryId !in categoryIdsWithActiveBudget
        }
        .mapNotNull { (categoryId, total) ->
            val category = categoriesById[categoryId] ?: return@mapNotNull null
            val monthsObserved = monthsWithSpend[categoryId] ?: 0
            if (monthsObserved == 0) return@mapNotNull null
            val averageMonthly = total / monthsObserved
            val suggestedBudget = roundToNearestCurrencyUnit((averageMonthly * 1.10).roundToLong())
            PremiumOpportunity(
                type = PremiumOpportunityType.BUDGET_SUGGESTION,
                label = category.name,
                impactCents = 0L,
                amountCents = suggestedBudget,
                comparisonAmountCents = averageMonthly
            )
        }
        .maxByOrNull { it.comparisonAmountCents }
}

private fun monthBounds(month: YearMonth, zoneId: ZoneId): Pair<Long, Long> {
    val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return start to (endExclusive - 1)
}

private fun roundToNearestCurrencyUnit(cents: Long): Long {
    if (cents <= 0L) return 0L
    return ((cents + 50L) / 100L) * 100L
}

private fun priorityFor(type: PremiumOpportunityType): Int = when (type) {
    PremiumOpportunityType.BUDGET_PRESSURE -> 0
    PremiumOpportunityType.CATEGORY_SPIKE -> 1
    PremiumOpportunityType.CATEGORY_CUTBACK -> 2
    PremiumOpportunityType.RECURRING_REVIEW -> 3
    PremiumOpportunityType.BUDGET_SUGGESTION -> 4
}

private const val MAX_OPPORTUNITIES = 3
private const val MIN_ACTIONABLE_CENTS = 2_000L
private const val MIN_SPIKE_PERCENT = 15
private const val MIN_BUDGET_PRESSURE_PERCENT = 80
private const val RECENT_LOOKBACK_MILLIS = 30L * 24L * 60L * 60L * 1000L
