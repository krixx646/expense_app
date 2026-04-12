package com.chelumore.xpenze.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.model.isExpired
import com.chelumore.xpenze.domain.model.matchesBudgetScope
import com.chelumore.xpenze.domain.model.resolvedExpenseCategoryId
import com.chelumore.xpenze.domain.model.resolvedEndDate
import com.chelumore.xpenze.domain.model.timeframeRange
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetProgressUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.budget.ProcessRecurringBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val premiumEntitlementTracker: PremiumEntitlementTracker,
    private val processRecurringBudgetsUseCase: ProcessRecurringBudgetsUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(BudgetFilterState())
    private val _sortOption = MutableStateFlow(BudgetSortOption.USAGE_DESC)

    init {
        viewModelScope.launch {
            runCatching { processRecurringBudgetsUseCase() }
        }
    }

    private data class BudgetsData(
        val budgets: List<Budget>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val accounts: List<Account>
    )

    private val dataFlow = combine(
        getBudgetsUseCase(activeOnly = false),
        getTransactionsUseCase(),
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true)
    ) { budgets, transactions, categories, accounts ->
        BudgetsData(budgets, transactions, categories, accounts)
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        dataFlow,
        _filterState,
        _sortOption,
        premiumEntitlementTracker.entitlement
    ) { data, filter, sort, entitlement ->
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val expenseCategories = data.categories.filter { it.type == CategoryType.EXPENSE }
        val selectedCategoryId = filter.categoryId?.takeIf { selectedId ->
            expenseCategories.any { it.id == selectedId }
        }
        val budgetUiModels = data.budgets
            .asSequence()
            .filter { !it.deleted }
            .map { budget ->
                buildBudgetUiModel(
                    budget = budget,
                    transactions = data.transactions,
                    categories = data.categories,
                    accounts = data.accounts,
                    now = now,
                    zone = zone
                )
            }
            .toList()

        val currentAll = budgetUiModels.filter { !it.isPast }
        val pastAll = budgetUiModels.filter { it.isPast }
        val filteredCurrent = applyFilters(currentAll, filter)
        val filteredPast = applyFilters(pastAll, filter)

        val expiredPrompt = data.budgets
            .asSequence()
            .filter { !it.deleted && it.isActive && !it.isRecurring && it.isExpired(now, zone) }
            .sortedByDescending { it.resolvedEndDate(zone) ?: 0L }
            .firstOrNull { expiredBudget -> !hasReplacement(expiredBudget, data.budgets, zone) }
            ?.let { expiredBudget ->
                val model = budgetUiModels.firstOrNull { it.id == expiredBudget.id }
                if (model == null) {
                    null
                } else {
                    ExpiredBudgetPromptUi(
                        budgetId = model.id,
                        budgetName = model.name,
                        timeframeLabel = model.timeframeLabel,
                        spentAmount = model.spentAmount,
                        limitAmount = model.limitAmount
                    )
                }
            }

        BudgetsUiState(
            isLoading = false,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            selectedFilter = filter.status,
            selectedPeriod = filter.period,
            selectedAccountId = filter.accountId,
            selectedCategoryId = selectedCategoryId,
            accounts = data.accounts,
            categories = expenseCategories,
            currentBudgets = sortCurrentBudgets(filteredCurrent, sort),
            pastBudgets = sortPastBudgets(filteredPast),
            expiredPrompt = expiredPrompt,
            currentCount = currentAll.size,
            pastCount = pastAll.size,
            activeCount = currentAll.count { it.status == BudgetStatus.ON_TRACK },
            warningCount = currentAll.count { it.status == BudgetStatus.WARNING },
            exceededCount = currentAll.count { it.status == BudgetStatus.EXCEEDED }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun setFilter(filter: BudgetFilter) {
        _filterState.update { it.copy(status = filter) }
    }

    fun setPeriodFilter(period: BudgetPeriod?) {
        _filterState.update { it.copy(period = period) }
    }

    fun setAccountFilter(accountId: String?) {
        _filterState.update { it.copy(accountId = accountId) }
    }

    fun setCategoryFilter(categoryId: String?) {
        _filterState.update { it.copy(categoryId = categoryId) }
    }

    fun setSort(sort: BudgetSortOption) {
        _sortOption.value = sort
    }

    private fun buildBudgetUiModel(
        budget: Budget,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
        now: Long,
        zone: ZoneId
    ): BudgetUiModel {
        val resolvedBudgetCategoryId = budget.resolvedExpenseCategoryId(categories)
        val category = categories.find { it.id == resolvedBudgetCategoryId }
        val account = accounts.find { it.id == budget.accountId }
        val trackingRange = budget.timeframeRange(now, zone)
        val progressResult = if (trackingRange == null) {
            getBudgetProgressUseCase(
                budget = budget,
                transactions = emptyList(),
                categories = categories
            )
        } else {
            getBudgetProgressUseCase(
                budget = budget,
                transactions = transactions,
                range = trackingRange,
                categories = categories
            )
        }
        val resolvedEnd = budget.resolvedEndDate(zone)
        val currentWindow = budget.currentPeriodWindow(now, zone)
        val isPast = resolvedEnd != null && now > resolvedEnd
        val status = when {
            progressResult.progress >= 1.0f -> BudgetStatus.EXCEEDED
            progressResult.progress >= (budget.alertThresholdPercent / 100f) -> BudgetStatus.WARNING
            else -> BudgetStatus.ON_TRACK
        }
        val daysLeft = when {
            currentWindow == null || resolvedEnd == null || isPast -> 0
            else -> computeDaysLeft(now, resolvedEnd)
        }
        val dailyLimit = if (daysLeft > 0) {
            (progressResult.remainingCents.coerceAtLeast(0L) / 100.0) / daysLeft
        } else {
            0.0
        }
        val timeframeLabel = when {
            resolvedEnd != null -> formatDateRange(budget.startDate, resolvedEnd)
            else -> formatDate(budget.startDate)
        }
        val transactionCount = countBudgetTransactions(budget, transactions, trackingRange, categories)
        val metaLabel = when {
            isPast && budget.isRecurring -> "Recurring series"
            isPast -> "Ended ${formatDate(resolvedEnd ?: budget.startDate)}"
            currentWindow != null && budget.isRecurring && resolvedEnd != null -> "Recurring - Ends ${formatDate(resolvedEnd)}"
            currentWindow != null && resolvedEnd != null -> "Ends ${formatDate(resolvedEnd)}"
            now < budget.startDate -> "Starts ${formatDate(budget.startDate)}"
            budget.isRecurring -> "Recurring budget"
            else -> "Not active right now"
        }

        return BudgetUiModel(
            id = budget.id,
            name = budget.name,
            categoryId = resolvedBudgetCategoryId,
            accountId = budget.accountId,
            categoryName = category?.name ?: "All Categories",
            accountName = account?.name ?: "All Accounts",
            categoryColorHex = category?.colorHex ?: "#F9D648",
            categoryIconName = category?.iconName ?: "payments",
            limitAmount = budget.amountLimitCents,
            spentAmount = progressResult.spentCents,
            period = budget.period,
            timeframeLabel = timeframeLabel,
            metaLabel = metaLabel,
            daysLeft = daysLeft,
            dailySpendLimit = max(0.0, dailyLimit),
            progress = progressResult.progress,
            status = status,
            isPast = isPast,
            isRecurring = budget.isRecurring,
            endDateMillis = resolvedEnd,
            transactionCount = transactionCount
        )
    }

    private fun countBudgetTransactions(
        budget: Budget,
        transactions: List<Transaction>,
        range: DateRange?,
        categories: List<Category>
    ): Int {
        return transactions.count { tx ->
            (range?.contains(tx.dateTime) ?: true) &&
                tx.matchesBudgetScope(budget, categories)
        }
    }

    private fun applyFilters(
        budgets: List<BudgetUiModel>,
        filter: BudgetFilterState
    ): List<BudgetUiModel> {
        var filteredList = budgets
        if (filter.status != BudgetFilter.ALL) {
            val targetStatus = when (filter.status) {
                BudgetFilter.ON_TRACK -> BudgetStatus.ON_TRACK
                BudgetFilter.WARNING -> BudgetStatus.WARNING
                BudgetFilter.EXCEEDED -> BudgetStatus.EXCEEDED
                BudgetFilter.ALL -> null
            }
            if (targetStatus != null) {
                filteredList = filteredList.filter { it.status == targetStatus }
            }
        }

        filter.period?.let { period ->
            filteredList = filteredList.filter { it.period == period }
        }
        filter.accountId?.let { accountId ->
            filteredList = filteredList.filter { it.accountId == accountId }
        }
        filter.categoryId?.let { categoryId ->
            filteredList = filteredList.filter { it.categoryId == categoryId }
        }

        return filteredList
    }

    private fun sortCurrentBudgets(
        budgets: List<BudgetUiModel>,
        sort: BudgetSortOption
    ): List<BudgetUiModel> {
        return when (sort) {
            BudgetSortOption.NAME -> budgets.sortedBy { it.name.lowercase() }
            BudgetSortOption.USAGE_DESC -> budgets.sortedByDescending { it.progress }
            BudgetSortOption.REMAINING_ASC -> budgets.sortedBy { it.remainingAmount }
        }
    }

    private fun sortPastBudgets(budgets: List<BudgetUiModel>): List<BudgetUiModel> {
        return budgets.sortedWith(
            compareByDescending<BudgetUiModel> { it.endDateMillis ?: Long.MIN_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun hasReplacement(
        expiredBudget: Budget,
        allBudgets: List<Budget>,
        zone: ZoneId
    ): Boolean {
        val expiredEnd = expiredBudget.resolvedEndDate(zone) ?: return true
        val normalizedName = expiredBudget.name.trim().lowercase()
        return allBudgets.any { other ->
            other.id != expiredBudget.id &&
                !other.deleted &&
                other.startDate > expiredEnd &&
                other.period == expiredBudget.period &&
                other.categoryId == expiredBudget.categoryId &&
                other.accountId == expiredBudget.accountId &&
                other.name.trim().lowercase() == normalizedName
        }
    }

    private fun computeDaysLeft(nowMillis: Long, endMillis: Long): Int {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(today, endDate).toInt()
        return max(0, daysBetween + 1)
    }

    private fun formatDateRange(startMillis: Long, endMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val formatter = if (start.year == end.year) {
            DateTimeFormatter.ofPattern("MMM d")
        } else {
            DateTimeFormatter.ofPattern("MMM d, yyyy")
        }
        return "${start.format(formatter)} - ${end.format(formatter)}"
    }

    private fun formatDate(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

data class BudgetFilterState(
    val status: BudgetFilter = BudgetFilter.ALL,
    val period: BudgetPeriod? = null,
    val accountId: String? = null,
    val categoryId: String? = null
)
