package com.chelumore.xpenze.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.localization.localizedOther
import com.chelumore.xpenze.core.localization.localizedUncategorized
import com.chelumore.xpenze.core.common.toLocalDate
import com.chelumore.xpenze.core.growth.buildPremiumOpportunities
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.model.matchesBudgetScope
import com.chelumore.xpenze.domain.model.resolvedCategory
import com.chelumore.xpenze.domain.model.resolvedCategoryId
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import java.time.ZoneId
import java.time.YearMonth

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val userRepository: UserRepository,
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private data class DashboardData(
        val transactions: List<com.chelumore.xpenze.domain.model.Transaction>,
        val accounts: List<com.chelumore.xpenze.domain.model.Account>,
        val budgets: List<com.chelumore.xpenze.domain.model.Budget>,
        val categories: List<com.chelumore.xpenze.domain.model.Category>
    )

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private val dataFlow = combine(
        getTransactionsUseCase(),
        getAccountsUseCase(includeArchived = false),
        getBudgetsUseCase(activeOnly = true),
        getCategoriesUseCase()
    ) { transactions, accounts, budgets, categories ->
        DashboardData(
            transactions = transactions,
            accounts = accounts,
            budgets = budgets,
            categories = categories
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        userRepository.userPreferences,
        premiumEntitlementTracker.entitlement,
        selectedMonth,
        dataFlow,
        autoCaptureDraftRepository.getPendingCount()
    ) { prefs, entitlement, month, data, pendingDraftCount ->
        val transactions = data.transactions
        val accounts = data.accounts
        val budgets = data.budgets
        val categories = data.categories

        val currency = prefs.defaultCurrency
        val userName = prefs.userName
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.contains("@") }
            ?: "User"

        val totalBalance = accounts.sumOf { it.currentBalanceCents }
        val savingsOpportunities = buildPremiumOpportunities(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            currencyCode = currency
        )

        val (startMillis, endMillis) = monthBounds(month)
        val monthTx = transactions.filter { it.dateTime in startMillis..endMillis }
        val monthTxCurrency = monthTx.filter { it.money.currency == currency }

        val monthlyIncome = monthTxCurrency
            .filter { it.type == com.chelumore.xpenze.domain.model.TransactionType.INCOME }
            .sumOf { it.money.amountCents }
        val monthlyExpense = monthTxCurrency
            .filter { it.type == com.chelumore.xpenze.domain.model.TransactionType.EXPENSE }
            .sumOf { it.money.amountCents }

        // “Change %” based on net cashflow vs previous month net cashflow
        val prevMonth = month.minusMonths(1)
        val (prevStart, prevEnd) = monthBounds(prevMonth)
        val prevTx = transactions.filter { it.dateTime in prevStart..prevEnd }.filter { it.money.currency == currency }
        val prevIncome = prevTx.filter { it.type == com.chelumore.xpenze.domain.model.TransactionType.INCOME }.sumOf { it.money.amountCents }
        val prevExpense = prevTx.filter { it.type == com.chelumore.xpenze.domain.model.TransactionType.EXPENSE }.sumOf { it.money.amountCents }
        val net = monthlyIncome - monthlyExpense
        val prevNet = prevIncome - prevExpense
        val changePercent = if (prevNet != 0L) ((net - prevNet).toDouble() / abs(prevNet).toDouble()) * 100.0 else 0.0

        // Spending by category (top 4 + other)
        val expenseByCategory = monthTxCurrency
            .asSequence()
            .filter { it.type == com.chelumore.xpenze.domain.model.TransactionType.EXPENSE }
            .groupBy { it.resolvedCategoryId(categories) }
            .mapValues { (_, list) -> list.sumOf { it.money.amountCents } }

        val totalExpense = monthlyExpense.coerceAtLeast(0L)
        val sortedCategorySpend = expenseByCategory.entries
            .sortedByDescending { it.value }

        val top = sortedCategorySpend.take(4)
        val otherAmount = sortedCategorySpend.drop(4).sumOf { it.value }

        val categorySpending = buildList {
            top.forEach { (categoryId, amount) ->
                val cat = categoryId?.let { id -> categories.firstOrNull { it.id == id } }
                val name = cat?.name ?: localizedUncategorized()
                val colorHex = cat?.colorHex ?: "#6C757D"
                val percent = if (totalExpense > 0) (amount.toFloat() / totalExpense.toFloat()) else 0f
                add(CategorySpendingUi(name = name, amount = amount, colorHex = colorHex, percent = percent))
            }
            if (otherAmount > 0L) {
                val percent = if (totalExpense > 0) (otherAmount.toFloat() / totalExpense.toFloat()) else 0f
                add(CategorySpendingUi(name = localizedOther(), amount = otherAmount, colorHex = "#4A4E58", percent = percent))
            }
        }

        // Recent transactions
        val recentUi = transactions
            .sortedByDescending { it.dateTime }
            .take(5)
            .map { tx ->
                val category = tx.resolvedCategory(categories)
                val account = accounts.find { it.id == tx.accountId }
                val toAccount = tx.toAccountId?.let { id -> accounts.find { it.id == id } }
                TransactionUi(
                    id = tx.id,
                    title = tx.title,
                    amount = tx.money.amountCents,
                    currency = tx.money.currency,
                    type = tx.type,
                    categoryName = category?.name ?: localizedUncategorized(),
                    categoryIcon = category?.iconName ?: "help",
                    categoryColorHex = category?.colorHex ?: "#6C757D",
                    accountName = account?.name.orEmpty(),
                    toAccountName = toAccount?.name,
                    date = tx.dateTime.toLocalDate(),
                    isRecurring = tx.isRecurring,
                    hasReceipt = tx.receiptUri != null
                )
            }

        // Budget alerts (budgets that crossed their threshold within their own start..end range)
        val now = System.currentTimeMillis()
        val budgetAlerts = budgets.asSequence()
            .filter { it.isActive }
            .mapNotNull { budget ->
                val window = budget.currentPeriodWindow(now) ?: return@mapNotNull null
                val spent = transactions.asSequence()
                    .filter { it.type == com.chelumore.xpenze.domain.model.TransactionType.EXPENSE }
                    .filter { it.money.currency == currency }
                    .filter { it.dateTime in window.spentRange.startMillis..window.spentRange.endMillis }
                    .filter { it.matchesBudgetScope(budget, categories) }
                    .sumOf { it.money.amountCents }

                val percentUsed = if (budget.amountLimitCents > 0) {
                    ((spent.toDouble() / budget.amountLimitCents.toDouble()) * 100.0).toInt()
                } else 0

                Triple(budget, percentUsed, spent)
            }
            .filter { (budget, percentUsed, _) -> percentUsed >= budget.alertThresholdPercent }
            .sortedByDescending { (_, percentUsed, _) -> percentUsed }
            .map { (budget, percentUsed, spent) ->
                BudgetAlertUi(
                    budgetName = budget.name,
                    percentUsed = percentUsed,
                    alertThresholdPercent = budget.alertThresholdPercent,
                    overspentAmount = (spent - budget.amountLimitCents).coerceAtLeast(0L)
                )
            }
            .toList()

        DashboardUiState(
            isLoading = false,
            currentMonth = month,
            userName = userName,
            userPhotoUrl = prefs.userPhotoUrl,
            hasFounderReward = entitlement.hasFounderReward,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            currencyCode = currency,
            appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag),
            autoCaptureEnabled = prefs.autoCaptureEnabled,
            pendingAutoCaptureDraftCount = pendingDraftCount,
            accountCardStyle = prefs.accountCardStyle,
            accountCardStyleOverrides = prefs.accountCardStyleOverrides,
            areBalancesHidden = prefs.areBalancesHidden,
            totalBalance = totalBalance,
            balanceChangePercent = changePercent,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            categorySpending = categorySpending,
            recentTransactions = recentUi,
            budgetAlerts = budgetAlerts,
            savingsOpportunities = savingsOpportunities,
            accounts = accounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setMonth(month: YearMonth) {
        selectedMonth.value = month
    }

    fun toggleBalanceVisibility() {
        viewModelScope.launch {
            userRepository.setBalancesHidden(!uiState.value.areBalancesHidden)
        }
    }

    private fun monthBounds(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to (endExclusive - 1)
    }
}

