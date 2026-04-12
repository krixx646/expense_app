package com.chelumore.xpenze.feature.reports

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.localization.localizedUncategorized
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.core.growth.buildPremiumOpportunities
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.resolvedCategoryId
import com.chelumore.xpenze.domain.usecase.analytics.GetIncomeVsExpenseUseCase
import com.chelumore.xpenze.domain.usecase.analytics.GetSpendingByCategoryUseCase
import com.chelumore.xpenze.domain.usecase.analytics.GetTrendDataUseCase
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import com.chelumore.xpenze.domain.usecase.export.ExportToCsvUseCase
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

sealed interface ReportsEvent {
    data class ShareFile(
        val bytes: ByteArray,
        val fileName: String,
        val mimeType: String
    ) : ReportsEvent

    data class Message(val text: String) : ReportsEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel @Inject constructor(
    private val getIncomeVsExpenseUseCase: GetIncomeVsExpenseUseCase,
    private val getSpendingByCategoryUseCase: GetSpendingByCategoryUseCase,
    private val getTrendDataUseCase: GetTrendDataUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val exportToCsvUseCase: ExportToCsvUseCase,
    private val userRepository: UserRepository,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    private val _isCompareEnabled = MutableStateFlow(true)

    private val eventsChannel = Channel<ReportsEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val rangeFlow: StateFlow<DateRange> = _selectedPeriod
        .map { periodToRange(it) ?: DateRange(0L, System.currentTimeMillis()) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DateRange(0L, System.currentTimeMillis())
        )

    private val previousRangeFlow: StateFlow<DateRange?> = _selectedPeriod
        .map { previousPeriodRange(it) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val incomeVsExpenseFlow = rangeFlow.flatMapLatest { range ->
        getIncomeVsExpenseUseCase(range)
    }

    private val previousIncomeVsExpenseFlow = previousRangeFlow.flatMapLatest { range ->
        if (range == null) {
            flowOf(com.chelumore.xpenze.domain.usecase.analytics.IncomeVsExpense(0L, 0L, 0L))
        } else {
            getIncomeVsExpenseUseCase(range)
        }
    }

    private val spendingByCategoryFlow = rangeFlow.flatMapLatest { range ->
        getSpendingByCategoryUseCase(range)
    }

    private val previousSpendingByCategoryFlow = previousRangeFlow.flatMapLatest { range ->
        if (range == null) {
            flowOf(emptyList<com.chelumore.xpenze.domain.usecase.analytics.CategorySpending>())
        } else {
            getSpendingByCategoryUseCase(range)
        }
    }

    private val trendFlow = rangeFlow.flatMapLatest { range ->
        getTrendDataUseCase(range)
    }

    private data class BaseAnalyticsState(
        val incomeVsExpense: com.chelumore.xpenze.domain.usecase.analytics.IncomeVsExpense,
        val categorySpending: List<com.chelumore.xpenze.domain.usecase.analytics.CategorySpending>,
        val trendPoints: List<com.chelumore.xpenze.domain.usecase.analytics.CashFlowTrendPoint>
    )

    private data class PreviousComparisonState(
        val incomeVsExpense: com.chelumore.xpenze.domain.usecase.analytics.IncomeVsExpense,
        val categorySpending: List<com.chelumore.xpenze.domain.usecase.analytics.CategorySpending>
    )

    private data class AiInsightsState(
        val currencyCode: String,
        val appLanguageTag: String,
        val monthlyInsights: List<MonthlyInsightUi>,
        val budgetSuggestions: List<BudgetSuggestionUi>,
        val savingsOpportunities: List<PremiumOpportunity>
    )

    private val baseAnalyticsFlow = combine(
        incomeVsExpenseFlow,
        spendingByCategoryFlow,
        trendFlow
    ) { incomeVsExpense, categorySpending, trendPoints ->
        BaseAnalyticsState(
            incomeVsExpense = incomeVsExpense,
            categorySpending = categorySpending,
            trendPoints = trendPoints
        )
    }

    private val previousComparisonFlow = combine(
        previousIncomeVsExpenseFlow,
        previousSpendingByCategoryFlow
    ) { previousIncomeVsExpense, previousCategorySpending ->
        PreviousComparisonState(
            incomeVsExpense = previousIncomeVsExpense,
            categorySpending = previousCategorySpending
        )
    }

    private val aiInsightsFlow = combine(
        getTransactionsUseCase(),
        getCategoriesUseCase(),
        getBudgetsUseCase(),
        userRepository.userPreferences
    ) { transactions, categories, budgets, prefs ->
        val currency = prefs.defaultCurrency
        AiInsightsState(
            currencyCode = currency,
            appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag),
            monthlyInsights = buildMonthlyInsights(transactions, categories, currency),
            budgetSuggestions = buildBudgetSuggestions(transactions, categories, budgets, currency),
            savingsOpportunities = buildPremiumOpportunities(
                transactions = transactions,
                categories = categories,
                budgets = budgets,
                currencyCode = currency
            )
        )
    }
    
    private val baseUiState: StateFlow<ReportsUiState> = combine(
        baseAnalyticsFlow,
        previousComparisonFlow,
        _selectedPeriod,
        _isCompareEnabled
    ) { analytics, previousComparison, period, compareEnabled ->
        val totalIncome = analytics.incomeVsExpense.incomeCents
        val totalExpense = analytics.incomeVsExpense.expenseCents
        val netCashFlow = analytics.incomeVsExpense.netCents
        val previousAmountsByCategory = previousComparison.categorySpending.associateBy(
            keySelector = { it.categoryId ?: "uncategorized" },
            valueTransform = { it.amountCents }
        )

        val categoryUi = analytics.categorySpending.take(6).mapIndexed { index, item ->
            CategorySpendingUi(
                categoryId = item.categoryId ?: "uncategorized",
                categoryName = item.categoryName,
                amount = item.amountCents,
                color = when (index % 4) {
                    0 -> YellowPrimary
                    1 -> WarningOrange
                    2 -> InfoBlue
                    else -> Color.Gray
                },
                percentage = item.percent,
                percentageChange = if (compareEnabled) {
                    computePercentChange(
                        currentAmount = item.amountCents,
                        previousAmount = previousAmountsByCategory[item.categoryId ?: "uncategorized"] ?: 0L
                    )
                } else {
                    null
                }
            )
        }

        val trendUi = analytics.trendPoints.mapIndexed { index, p ->
            TrendPoint(
                dayStartMillis = p.dayStartMillis,
                incomeCents = p.incomeCents,
                expenseCents = p.expenseCents,
                netCents = p.netCents
            )
        }

        ReportsUiState(
            isLoading = false,
            selectedPeriod = period,
            isCompareEnabled = compareEnabled,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            prevTotalIncome = previousComparison.incomeVsExpense.incomeCents,
            prevTotalExpense = previousComparison.incomeVsExpense.expenseCents,
            categorySpending = categoryUi,
            topCategories = categoryUi.take(4),
            monthlyTrend = trendUi
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    // Final UI state includes premium/trial entitlement from user prefs.
    val uiState: StateFlow<ReportsUiState> = combine(
        baseUiState,
        aiInsightsFlow,
        premiumEntitlementTracker.entitlement,
        userRepository.userPreferences
    ) { state, ai, entitlement, prefs ->
        state.copy(
            currencyCode = ai.currencyCode,
            appLanguageTag = ai.appLanguageTag,
            monthlyInsights = ai.monthlyInsights,
            budgetSuggestions = ai.budgetSuggestions,
            savingsOpportunities = ai.savingsOpportunities,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            isTrialActive = entitlement.isTrialActive,
            nudgeVariant = PremiumEmotionVariant.fromSeed(
                prefs.userId ?: prefs.userEmail ?: prefs.defaultCurrency
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }
    
    fun toggleCompare(enabled: Boolean) {
        _isCompareEnabled.value = enabled
    }

    fun exportCsv() {
        viewModelScope.launch {
            try {
                if (!premiumEntitlementTracker.entitlement.value.hasPremiumAccess) {
                    eventsChannel.send(ReportsEvent.Message("CSV export is part of Premium."))
                    return@launch
                }
                val range = periodToRange(_selectedPeriod.value)
                val csv = exportToCsvUseCase(range)
                val fileName = "xpenze-export-${System.currentTimeMillis()}.csv"
                eventsChannel.send(
                    ReportsEvent.ShareFile(
                        bytes = csv.toByteArray(Charsets.UTF_8),
                        fileName = fileName,
                        mimeType = "text/csv"
                    )
                )
            } catch (e: Exception) {
                eventsChannel.send(ReportsEvent.Message(e.message ?: "Export failed"))
            }
        }
    }

    private fun periodToRange(period: ReportPeriod): DateRange? {
        if (period == ReportPeriod.ALL_TIME) return null

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        return when (period) {
            ReportPeriod.THIS_MONTH -> {
                val start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.toInstant().toEpochMilli()
                DateRange(startMillis = start, endMillis = end)
            }

            ReportPeriod.LAST_MONTH -> {
                val startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
                val start = startOfThisMonth.minusMonths(1).toInstant().toEpochMilli()
                val end = startOfThisMonth.minusNanos(1).toInstant().toEpochMilli()
                DateRange(startMillis = start, endMillis = end)
            }

            ReportPeriod.THIS_YEAR -> {
                val start = now.withDayOfYear(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                val end = now.toInstant().toEpochMilli()
                DateRange(startMillis = start, endMillis = end)
            }

            ReportPeriod.ALL_TIME -> null
        }
    }

    private fun previousPeriodRange(period: ReportPeriod): DateRange? {
        if (period == ReportPeriod.ALL_TIME) return null

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        return when (period) {
            ReportPeriod.THIS_MONTH -> {
                val startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
                val start = startOfThisMonth.minusMonths(1).toInstant().toEpochMilli()
                val end = now.minusMonths(1).toInstant().toEpochMilli()
                DateRange(startMillis = start, endMillis = end)
            }

            ReportPeriod.LAST_MONTH -> {
                val startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
                val endOfLastMonth = startOfThisMonth.minusNanos(1)
                val startOfLastMonth = startOfThisMonth.minusMonths(1)
                val start = startOfLastMonth.minusMonths(1).toInstant().toEpochMilli()
                val end = startOfLastMonth.minusNanos(1).toInstant().toEpochMilli()
                DateRange(startMillis = start, endMillis = minOf(end, endOfLastMonth.toInstant().toEpochMilli()))
            }

            ReportPeriod.THIS_YEAR -> {
                val startOfThisYear = now.withDayOfYear(1).toLocalDate().atStartOfDay(zone)
                val start = startOfThisYear.minusYears(1).toInstant().toEpochMilli()
                val end = now.minusYears(1).toInstant().toEpochMilli()
                DateRange(startMillis = start, endMillis = end)
            }

            ReportPeriod.ALL_TIME -> null
        }
    }

    private fun computePercentChange(currentAmount: Long, previousAmount: Long): Float? {
        if (previousAmount <= 0L) return null
        return (((currentAmount - previousAmount).toDouble() / previousAmount.toDouble()) * 100.0).toFloat()
    }

    private fun buildMonthlyInsights(
        transactions: List<com.chelumore.xpenze.domain.model.Transaction>,
        categories: List<com.chelumore.xpenze.domain.model.Category>,
        currencyCode: String
    ): List<MonthlyInsightUi> {
        val now = YearMonth.now()
        val (currentStart, currentEnd) = monthBounds(now)
        val (prevStart, prevEnd) = monthBounds(now.minusMonths(1))

        val current = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.money.currency == currencyCode }
            .filter { it.dateTime in currentStart..currentEnd }
            .groupBy { it.resolvedCategoryId(categories) }
            .mapValues { (_, list) -> list.sumOf { it.money.amountCents } }

        val previous = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.money.currency == currencyCode }
            .filter { it.dateTime in prevStart..prevEnd }
            .groupBy { it.resolvedCategoryId(categories) }
            .mapValues { (_, list) -> list.sumOf { it.money.amountCents } }

        val allKeys = current.keys + previous.keys
        return allKeys.mapNotNull { key ->
            val currentCents = current[key] ?: 0L
            val previousCents = previous[key] ?: 0L
            if (currentCents <= 0L || previousCents <= 0L) return@mapNotNull null

            val change = ((currentCents - previousCents).toDouble() / previousCents.toDouble()) * 100.0
            val categoryName = key?.let { id -> categories.firstOrNull { it.id == id }?.name } ?: localizedUncategorized()
            MonthlyInsightUi(
                categoryName = categoryName,
                currentCents = currentCents,
                previousCents = previousCents,
                changePercent = change.toFloat()
            )
        }.sortedByDescending { abs(it.changePercent) }
            .take(3)
    }

    private fun buildBudgetSuggestions(
        transactions: List<com.chelumore.xpenze.domain.model.Transaction>,
        categories: List<com.chelumore.xpenze.domain.model.Category>,
        budgets: List<com.chelumore.xpenze.domain.model.Budget>,
        currencyCode: String
    ): List<BudgetSuggestionUi> {
        val months = listOf(
            YearMonth.now(),
            YearMonth.now().minusMonths(1),
            YearMonth.now().minusMonths(2)
        )

        val totalsByCategory = mutableMapOf<String?, Long>()
        val monthsWithSpend = mutableMapOf<String?, Int>()
        for (month in months) {
            val (start, end) = monthBounds(month)
            val monthTotals = transactions.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .filter { it.money.currency == currencyCode }
                .filter { it.dateTime in start..end }
                .groupBy { it.resolvedCategoryId(categories) }
                .mapValues { (_, list) -> list.sumOf { it.money.amountCents } }

            for ((categoryId, amount) in monthTotals) {
                if (amount <= 0L) continue
                totalsByCategory[categoryId] = (totalsByCategory[categoryId] ?: 0L) + amount
                monthsWithSpend[categoryId] = (monthsWithSpend[categoryId] ?: 0) + 1
            }
        }

        val excludedCategoryIds = budgets.asSequence()
            .filter { it.isActive }
            .mapNotNull { it.categoryId }
            .toSet()

        return totalsByCategory
            .filterKeys { key -> key != null }
            .filter { (categoryId, total) ->
                total > 0L && categoryId !in excludedCategoryIds
            }
            .mapNotNull { (categoryId, total) ->
                val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                if (category.type != CategoryType.EXPENSE) return@mapNotNull null

                val monthsObserved = monthsWithSpend[categoryId] ?: 0
                if (monthsObserved <= 0) return@mapNotNull null
                val avg = total / monthsObserved
                val suggested = roundToNearestCurrencyUnit((avg * 1.1).roundToLong())
                BudgetSuggestionUi(
                    categoryName = category.name,
                    averageMonthlyCents = avg,
                    suggestedBudgetCents = suggested
                )
            }
            .sortedByDescending { it.averageMonthlyCents }
            .take(5)
    }

    private fun monthBounds(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to (endExclusive - 1)
    }

    private fun roundToNearestCurrencyUnit(cents: Long): Long {
        // Round to nearest whole currency unit (100 cents).
        if (cents <= 0L) return 0L
        return ((cents + 50) / 100) * 100
    }
}

