package com.chelumore.xpenze.feature.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.localization.localizedUncategorized
import com.chelumore.xpenze.core.localization.localizedUnknown
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.common.toLocalDate
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.AddTransactionUseCase
import com.chelumore.xpenze.domain.usecase.transaction.DeleteTransactionUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val budgetAlertScheduler: BudgetAlertScheduler,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionsFilterState())

    sealed interface TransactionsEvent {
        data class Message(val text: String) : TransactionsEvent
        data class ShareText(val text: String) : TransactionsEvent
    }

    private val eventsChannel = Channel<TransactionsEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    
    // Combine repositories with filter state
    val uiState: StateFlow<TransactionsUiState> = combine(
        getTransactionsUseCase(),
        getAccountsUseCase(includeArchived = true),
        getCategoriesUseCase(),
        _filterState,
        premiumEntitlementTracker.entitlement
    ) { transactions: List<Transaction>,
         accounts,
         categories,
         filter: TransactionsFilterState,
         entitlement ->
        
        // 1. Filter Transactions
        var filteredList = transactions

        // Date Range
        val nowMillis = System.currentTimeMillis()
        val range = dateRangeToBounds(filter.dateRange, nowMillis)
        if (range != null) {
            filteredList = filteredList.filter { it.dateTime in range.first..range.second }
        }

        // Filter by Account (include transfers where account is either from or to)
        filter.accountId?.let { accountId ->
            filteredList = filteredList.filter { it.accountId == accountId || it.toAccountId == accountId }
        }

        // Filter by Category
        filter.categoryId?.let { categoryId ->
            filteredList = filteredList.filter { it.categoryId == categoryId }
        }
        
        // Filter by Type
        if (filter.type != TransactionFilterType.ALL) {
            val typeEnum = when(filter.type) {
                TransactionFilterType.INCOME -> TransactionType.INCOME
                TransactionFilterType.EXPENSE -> TransactionType.EXPENSE
                TransactionFilterType.TRANSFER -> TransactionType.TRANSFER
                else -> null
            }
            if (typeEnum != null) {
                filteredList = filteredList.filter { it.type == typeEnum }
            }
        }
        
        // Filter by Search
        if (filter.searchQuery.isNotBlank()) {
            val query = filter.searchQuery.lowercase()
            filteredList = filteredList.filter { 
                it.title.lowercase().contains(query) || 
                (it.notes?.lowercase()?.contains(query) == true) ||
                it.tags.any { tag -> tag.lowercase().contains(query) }
            }
        }

        // Sort
        filteredList = when (filter.sort) {
            TransactionSortOption.DATE_DESC -> filteredList.sortedByDescending { it.dateTime }
            TransactionSortOption.DATE_ASC -> filteredList.sortedBy { it.dateTime }
            TransactionSortOption.AMOUNT_DESC -> filteredList.sortedByDescending { it.money.amountCents }
            TransactionSortOption.AMOUNT_ASC -> filteredList.sortedBy { it.money.amountCents }
        }

        // 2. Map to UI Model
        val uiItems = filteredList.map { tx ->
            val category = categories.find { it.id == tx.categoryId }
            val account = accounts.find { it.id == tx.accountId }
            val toAccount = if (tx.type == TransactionType.TRANSFER) {
                accounts.find { it.id == tx.toAccountId }
            } else null

            TransactionItemUi(
                id = tx.id,
                title = tx.title,
                amount = tx.money.amountCents,
                currency = tx.money.currency,
                type = tx.type,
                accountId = tx.accountId,
                toAccountId = tx.toAccountId,
                categoryId = tx.categoryId,
                categoryName = category?.name ?: localizedUncategorized(),
                categoryIcon = category?.iconName ?: "help",
                accountName = account?.name ?: localizedUnknown(),
                toAccountName = toAccount?.name,
                dateTimeMillis = tx.dateTime,
                date = tx.dateTime.toLocalDate(),
                timeFormatted = tx.dateTime.toFormattedDateTime("HH:mm"),
                isRecurring = tx.isRecurring,
                hasReceipt = tx.receiptUri != null,
                notes = tx.notes
            )
        }

        // 3. Group by Date
        val grouped = uiItems.groupBy { it.date }

        // 4. Calculate Summary (based on filtered list or whole list? usually whole list for the period)
        // For now, let's use the filtered list for summary to reflect what's seen
        val income = filteredList.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }
        val expense = filteredList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
        val net = income - expense

        // 5. Trend vs previous comparable period (same filters, previous date window)
        val prevBounds = previousRangeToBounds(filter.dateRange, nowMillis)
        val (prevIncome, prevExpense, prevNet) = if (prevBounds == null) {
            Triple(0L, 0L, 0L)
        } else {
            var prevList = transactions.filter { it.dateTime in prevBounds.first..prevBounds.second }

            // Same account/category/type/search filters
            filter.accountId?.let { accountId ->
                prevList = prevList.filter { it.accountId == accountId || it.toAccountId == accountId }
            }
            filter.categoryId?.let { categoryId ->
                prevList = prevList.filter { it.categoryId == categoryId }
            }
            if (filter.type != TransactionFilterType.ALL) {
                val typeEnum = when (filter.type) {
                    TransactionFilterType.INCOME -> TransactionType.INCOME
                    TransactionFilterType.EXPENSE -> TransactionType.EXPENSE
                    TransactionFilterType.TRANSFER -> TransactionType.TRANSFER
                    else -> null
                }
                if (typeEnum != null) {
                    prevList = prevList.filter { it.type == typeEnum }
                }
            }
            if (filter.searchQuery.isNotBlank()) {
                val query = filter.searchQuery.lowercase()
                prevList = prevList.filter {
                    it.title.lowercase().contains(query) ||
                        (it.notes?.lowercase()?.contains(query) == true) ||
                        it.tags.any { tag -> tag.lowercase().contains(query) }
                }
            }

            val pi = prevList.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }
            val pe = prevList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
            Triple(pi, pe, pi - pe)
        }

        TransactionsUiState(
            isLoading = false,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            periodIncome = income,
            periodExpense = expense,
            periodNet = net,
            incomeTrendPercent = trendPercent(current = income, previous = prevIncome),
            expenseTrendPercent = trendPercent(current = expense, previous = prevExpense),
            netTrendPercent = trendPercent(current = net, previous = prevNet),
            selectedType = filter.type,
            selectedDateRange = filter.dateRange,
            selectedAccountId = filter.accountId,
            selectedCategoryId = filter.categoryId,
            selectedSort = filter.sort,
            searchQuery = filter.searchQuery,
            accounts = accounts,
            categories = categories,
            transactionsByDate = grouped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setFilterType(type: TransactionFilterType) {
        _filterState.update { it.copy(type = type) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun setDateRange(range: TransactionDateRange) {
        _filterState.update { it.copy(dateRange = range) }
    }

    fun setAccountFilter(accountId: String?) {
        _filterState.update { it.copy(accountId = accountId) }
    }

    fun setCategoryFilter(categoryId: String?) {
        _filterState.update { it.copy(categoryId = categoryId) }
    }

    fun setSort(sort: TransactionSortOption) {
        _filterState.update { it.copy(sort = sort) }
    }

    fun clearAllFilters() {
        _filterState.value = TransactionsFilterState()
    }
    
    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
            budgetAlertScheduler.scheduleNow()
        }
    }

    fun duplicateTransaction(item: TransactionItemUi) {
        viewModelScope.launch {
            runCatching {
                addTransactionUseCase(
                    accountId = item.accountId,
                    toAccountId = item.toAccountId,
                    categoryId = item.categoryId,
                    amountCents = item.amount,
                    currency = item.currency,
                    type = item.type,
                    title = item.title,
                    notes = item.notes,
                    dateTime = item.dateTimeMillis
                )
            }.onSuccess {
                budgetAlertScheduler.scheduleNow()
                eventsChannel.send(TransactionsEvent.Message("Transaction duplicated"))
            }.onFailure { e ->
                eventsChannel.send(TransactionsEvent.Message(e.message ?: "Failed to duplicate transaction"))
            }
        }
    }

    fun shareTransaction(item: TransactionItemUi) {
        viewModelScope.launch {
            val text = buildString {
                appendLine("Xpenze transaction")
                appendLine(item.title)
                appendLine()
                appendLine("Type: ${item.type.name}")
                appendLine("Amount: ${item.amount.toCurrencyString(item.currency)}")
                appendLine(
                    if (item.type == TransactionType.TRANSFER) {
                        "From: ${item.accountName} → ${item.toAccountName.orEmpty()}"
                    } else {
                        "Category: ${item.categoryName} • Account: ${item.accountName}"
                    }
                )
                appendLine("Date: ${item.date} ${item.timeFormatted}")
                item.notes?.takeIf { it.isNotBlank() }?.let {
                    appendLine("Notes: $it")
                }
            }
            eventsChannel.send(TransactionsEvent.ShareText(text))
        }
    }

    private fun dateRangeToBounds(range: TransactionDateRange, nowMillis: Long): Pair<Long, Long>? {
        if (range == TransactionDateRange.ALL_TIME) return null

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        return when (range) {
            TransactionDateRange.THIS_MONTH -> {
                val start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                start to nowMillis
            }
            TransactionDateRange.LAST_MONTH -> {
                val startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
                val start = startOfThisMonth.minusMonths(1).toInstant().toEpochMilli()
                val end = startOfThisMonth.minusNanos(1).toInstant().toEpochMilli()
                start to end
            }
            TransactionDateRange.THIS_YEAR -> {
                val start = now.withDayOfYear(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                start to nowMillis
            }
            TransactionDateRange.ALL_TIME -> null
        }
    }

    private fun previousRangeToBounds(range: TransactionDateRange, nowMillis: Long): Pair<Long, Long>? {
        if (range == TransactionDateRange.ALL_TIME) return null

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        return when (range) {
            TransactionDateRange.THIS_MONTH -> {
                val startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
                val startThis = startOfThisMonth.toInstant().toEpochMilli()
                val elapsed = (nowMillis - startThis).coerceAtLeast(0L)

                val startOfPrev = startOfThisMonth.minusMonths(1)
                val startPrevMillis = startOfPrev.toInstant().toEpochMilli()
                val endOfPrevMonth = startOfThisMonth.minusNanos(1).toInstant().toEpochMilli()
                val end = minOf(startPrevMillis + elapsed, endOfPrevMonth)
                startPrevMillis to end
            }

            TransactionDateRange.LAST_MONTH -> {
                val startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
                val startOfLastMonth = startOfThisMonth.minusMonths(1)
                val startOfPrev = startOfThisMonth.minusMonths(2)
                val startPrevMillis = startOfPrev.toInstant().toEpochMilli()
                val end = startOfLastMonth.minusNanos(1).toInstant().toEpochMilli()
                startPrevMillis to end
            }

            TransactionDateRange.THIS_YEAR -> {
                val startOfThisYear = now.withDayOfYear(1).toLocalDate().atStartOfDay(zone)
                val startThis = startOfThisYear.toInstant().toEpochMilli()
                val elapsed = (nowMillis - startThis).coerceAtLeast(0L)

                val startOfPrev = startOfThisYear.minusYears(1)
                val startPrevMillis = startOfPrev.toInstant().toEpochMilli()
                val endOfPrevYear = startOfThisYear.minusNanos(1).toInstant().toEpochMilli()
                val end = minOf(startPrevMillis + elapsed, endOfPrevYear)
                startPrevMillis to end
            }

            TransactionDateRange.ALL_TIME -> null
        }
    }

    private fun trendPercent(current: Long, previous: Long): Double {
        val denom = abs(previous.toDouble())
        if (denom < 1.0) return 0.0
        val percent = ((current - previous).toDouble() / denom) * 100.0
        return (percent * 10).roundToInt() / 10.0
    }
}

data class TransactionsFilterState(
    val type: TransactionFilterType = TransactionFilterType.ALL,
    val searchQuery: String = "",
    val dateRange: TransactionDateRange = TransactionDateRange.THIS_MONTH,
    val accountId: String? = null,
    val categoryId: String? = null,
    val sort: TransactionSortOption = TransactionSortOption.DATE_DESC
)
