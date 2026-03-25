package com.chelumore.xpenze.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetProgressUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.max
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private val _filterState = MutableStateFlow(BudgetFilterState())
    private val _sortOption = MutableStateFlow(BudgetSortOption.USAGE_DESC)

    // Intermediate data class to hold all repository data
    private data class BudgetsData(
        val budgets: List<Budget>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val accounts: List<Account>
    )

    // Combine repository flows into one data flow
    private val dataFlow = combine(
        getBudgetsUseCase(activeOnly = false),
        getTransactionsUseCase(),
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true)
    ) { budgets, transactions, categories, accounts ->
        BudgetsData(budgets, transactions, categories, accounts)
    }

    // Combine data flow with UI control flows
    val uiState: StateFlow<BudgetsUiState> = combine(
        dataFlow,
        _filterState,
        _sortOption,
        premiumEntitlementTracker.entitlement
    ) { data, filter, sort, entitlement ->
        val budgets = data.budgets
        val transactions = data.transactions
        val categories = data.categories
        val accounts = data.accounts
        
        // Map Budgets to UI Models
        val budgetUiModels = budgets.map { budget ->
            val category = categories.find { it.id == budget.categoryId }
            val account = accounts.find { it.id == budget.accountId }
            
            val now = System.currentTimeMillis()
            val window = budget.currentPeriodWindow(now)
            val progressResult = window?.let {
                getBudgetProgressUseCase(
                    budget = budget,
                    transactions = transactions,
                    range = it.spentRange
                )
            }
            val spent = progressResult?.spentCents ?: 0L
            val progress = progressResult?.progress ?: 0f

            val status = when {
                progress >= 1.0f -> BudgetStatus.EXCEEDED
                progress >= (budget.alertThresholdPercent / 100f) -> BudgetStatus.WARNING
                else -> BudgetStatus.ON_TRACK
            }

            val daysLeft = when {
                window == null -> 0
                budget.period == BudgetPeriod.CUSTOM && budget.endDate == null -> 0
                else -> computeDaysLeft(now, window.periodEndMillis)
            }
            val remaining = (budget.amountLimitCents - spent).coerceAtLeast(0L)
            val dailyLimit = if (daysLeft > 0) (remaining / 100.0) / daysLeft else 0.0
            val currentPeriodLabel = when {
                window != null -> "Current period: ${formatDateRange(window.periodStartMillis, window.periodEndMillis)}"
                now < budget.startDate -> "Starts ${formatDate(budget.startDate)}"
                budget.endDate != null && now > budget.endDate -> "Ended ${formatDate(budget.endDate)}"
                else -> "Not active right now"
            }

            BudgetUiModel(
                id = budget.id,
                name = budget.name,
                categoryId = budget.categoryId,
                accountId = budget.accountId,
                categoryName = category?.name ?: "All Categories",
                accountName = account?.name ?: "All Accounts",
                limitAmount = budget.amountLimitCents,
                spentAmount = spent,
                period = budget.period,
                currentPeriodLabel = currentPeriodLabel,
                daysLeft = daysLeft,
                dailySpendLimit = max(0.0, dailyLimit),
                progress = progress,
                status = status
            )
        }

        // Apply Filters
        var filteredList = budgetUiModels
        if (filter.status != BudgetFilter.ALL) {
            val targetStatus = when (filter.status) {
                BudgetFilter.ON_TRACK -> BudgetStatus.ON_TRACK
                BudgetFilter.WARNING -> BudgetStatus.WARNING
                BudgetFilter.EXCEEDED -> BudgetStatus.EXCEEDED
                else -> null // Should not happen for specific status filters
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

        // Apply Sort
        filteredList = when (sort) {
            BudgetSortOption.NAME -> filteredList.sortedBy { it.name }
            BudgetSortOption.USAGE_DESC -> filteredList.sortedByDescending { it.progress }
            BudgetSortOption.REMAINING_ASC -> filteredList.sortedBy { it.remainingAmount }
        }

        // Calculate Counts
        val active = budgetUiModels.count { it.status == BudgetStatus.ON_TRACK }
        val warning = budgetUiModels.count { it.status == BudgetStatus.WARNING }
        val exceeded = budgetUiModels.count { it.status == BudgetStatus.EXCEEDED }

        BudgetsUiState(
            isLoading = false,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            budgets = filteredList,
            selectedFilter = filter.status,
            selectedPeriod = filter.period,
            selectedAccountId = filter.accountId,
            selectedCategoryId = filter.categoryId,
            accounts = accounts,
            categories = categories,
            activeCount = active,
            warningCount = warning,
            exceededCount = exceeded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun setFilter(filter: BudgetFilter) {
        _filterState.update { it.copy(status = filter) }
    }

    fun setPeriodFilter(period: com.chelumore.xpenze.domain.model.BudgetPeriod?) {
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

    private fun computeDaysLeft(nowMillis: Long, endMillis: Long): Int {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(today, endDate).toInt()
        return max(0, daysBetween + 1) // inclusive
    }

    private fun formatDateRange(startMillis: Long, endMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("MMM d")
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
    val period: com.chelumore.xpenze.domain.model.BudgetPeriod? = null,
    val accountId: String? = null,
    val categoryId: String? = null
)
