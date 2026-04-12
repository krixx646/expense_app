package com.chelumore.xpenze.feature.budgets.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.model.matchesBudgetScope
import com.chelumore.xpenze.domain.model.nextTimeframeRange
import com.chelumore.xpenze.domain.model.resolvedCategory
import com.chelumore.xpenze.domain.model.resolvedCategoryId
import com.chelumore.xpenze.domain.model.resolvedExpenseCategoryId
import com.chelumore.xpenze.domain.model.resolvedEndDate
import com.chelumore.xpenze.domain.model.timeframeRange
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.DeleteBudgetUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetByIdUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetProgressUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import com.chelumore.xpenze.domain.usecase.transaction.TransactionQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModel @Inject constructor(
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val budgetAlertScheduler: BudgetAlertScheduler,
    getCategoriesUseCase: GetCategoriesUseCase,
    getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val budgetId = MutableStateFlow<String?>(null)

    private val budgetFlow = budgetId
        .filterNotNull()
        .flatMapLatest { id -> getBudgetByIdUseCase(id) }

    private val txFlow = budgetFlow
        .filterNotNull()
        .flatMapLatest { budget ->
            val range = budget.timeframeRange()
            if (range == null) {
                flowOf(emptyList())
            } else {
                getTransactionsUseCase(
                    TransactionQuery(
                        dateRange = range,
                        type = TransactionType.EXPENSE
                    )
                )
            }
        }

    val uiState: StateFlow<BudgetDetailUiState> = combine(
        budgetFlow,
        txFlow,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true)
    ) { budget, transactions, categories, accounts ->
        if (budget == null) {
            return@combine BudgetDetailUiState(isLoading = false, error = "Budget not found")
        }

        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val range = budget.timeframeRange(now, zone)
        val scopedTransactions = transactions.filter { tx -> tx.matchesBudgetScope(budget, categories) }
        val progress = if (range == null) {
            getBudgetProgressUseCase(budget, emptyList(), categories = categories)
        } else {
            getBudgetProgressUseCase(budget, scopedTransactions, range, categories)
        }
        val currentWindow = budget.currentPeriodWindow(now, zone)
        val categoryName = budget.resolvedExpenseCategoryId(categories)
            ?.let { id -> categories.firstOrNull { it.id == id }?.name }
            ?: "All Categories"
        val accountName = budget.accountId?.let { id -> accounts.firstOrNull { it.id == id }?.name } ?: "All Accounts"
        val resolvedEnd = budget.resolvedEndDate(zone)
        val isPast = resolvedEnd != null && now > resolvedEnd
        val timeframeLabel = when {
            resolvedEnd != null -> formatDateRange(budget.startDate, resolvedEnd)
            else -> formatDate(budget.startDate)
        }
        val statusLabel = when {
            isPast && budget.isRecurring -> "Recurring budget history"
            isPast -> "Budget history"
            currentWindow != null && budget.isRecurring -> "Current recurring budget"
            currentWindow != null -> "Current budget"
            now < budget.startDate -> "Starts ${formatDate(budget.startDate)}"
            budget.isRecurring -> "Recurring budget"
            else -> "Budget"
        }
        val nextTimeframeLabel = if (isPast) {
            null
        } else {
            budget.nextTimeframeRange(zone)?.let { nextRange ->
                formatDateRange(nextRange.startMillis, nextRange.endMillis)
            }
        }
        val transactionItems = scopedTransactions
            .sortedByDescending { it.dateTime }
            .map { tx -> toTransactionUi(tx, categories, accounts) }
        val breakdownItems = buildBreakdownItems(scopedTransactions, categories)
        val largestExpense = scopedTransactions.maxByOrNull { it.money.amountCents }
        val averageSpend = if (scopedTransactions.isNotEmpty()) {
            progress.spentCents / scopedTransactions.size
        } else {
            0L
        }

        BudgetDetailUiState(
            isLoading = false,
            budget = budget,
            isPast = isPast,
            categoryName = categoryName,
            accountName = accountName,
            timeframeLabel = timeframeLabel,
            statusLabel = statusLabel,
            nextTimeframeLabel = nextTimeframeLabel,
            canCreateNextBudget = !budget.isRecurring && resolvedEnd != null && now > resolvedEnd,
            transactionCount = scopedTransactions.size,
            spentCents = progress.spentCents,
            remainingCents = progress.remainingCents,
            progress = progress.progress,
            averageSpendCents = averageSpend,
            largestExpenseTitle = largestExpense?.title,
            largestExpenseCents = largestExpense?.money?.amountCents ?: 0L,
            breakdown = breakdownItems,
            transactions = transactionItems.take(50)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetDetailUiState()
    )

    fun load(id: String) {
        budgetId.value = id
    }

    fun delete(onDeleted: () -> Unit) {
        val id = budgetId.value ?: return
        viewModelScope.launch {
            deleteBudgetUseCase(id)
            budgetAlertScheduler.scheduleNow()
            onDeleted()
        }
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

    private fun formatDate(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMM d"))
    }

    private fun formatDateTime(epochMillis: Long): String {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy - HH:mm"))
    }

    private fun toTransactionUi(
        tx: com.chelumore.xpenze.domain.model.Transaction,
        categories: List<Category>,
        accounts: List<Account>
    ): BudgetDetailTransactionUi {
        val category = tx.resolvedCategory(categories)
        val account = accounts.firstOrNull { it.id == tx.accountId }
        val categoryName = category?.name ?: "Uncategorized"
        val accountName = account?.name ?: "Unknown account"
        return BudgetDetailTransactionUi(
            id = tx.id,
            title = tx.title,
            subtitle = "$categoryName • $accountName",
            amountCents = tx.money.amountCents,
            currency = tx.money.currency,
            dateLabel = formatDateTime(tx.dateTime),
            categoryColorHex = category?.colorHex ?: "#F9D648",
            categoryIconName = category?.iconName ?: "payments",
            isRecurring = tx.isRecurring,
            hasReceipt = !tx.receiptUri.isNullOrBlank()
        )
    }

    private fun buildBreakdownItems(
        transactions: List<com.chelumore.xpenze.domain.model.Transaction>,
        categories: List<Category>
    ): List<BudgetBreakdownItemUi> {
        val totalSpent = transactions.sumOf { it.money.amountCents }.coerceAtLeast(0L)
        return transactions
            .groupBy { tx ->
                BreakdownKey(
                    title = tx.title.trim().ifBlank { "Untitled expense" },
                    categoryId = tx.resolvedCategoryId(categories)
                )
            }
            .map { (key, group) ->
                val first = group.first()
                val category = categories.firstOrNull { it.id == key.categoryId }
                val amount = group.sumOf { it.money.amountCents }
                val share = if (totalSpent > 0L) amount.toFloat() / totalSpent.toFloat() else 0f
                BudgetBreakdownItemUi(
                    title = key.title,
                    subtitle = category?.name ?: "General",
                    totalAmountCents = amount,
                    transactionCount = group.size,
                    sharePercent = share,
                    colorHex = category?.colorHex ?: "#F9D648",
                    iconName = category?.iconName ?: "payments"
                )
            }
            .sortedByDescending { it.totalAmountCents }
            .take(6)
    }

    private data class BreakdownKey(
        val title: String,
        val categoryId: String?
    )
}
