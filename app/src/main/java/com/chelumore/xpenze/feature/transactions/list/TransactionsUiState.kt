package com.chelumore.xpenze.feature.transactions.list

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.TransactionType
import java.time.LocalDate

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasPremiumAccess: Boolean = false,
    
    // Summary
    val periodIncome: Long = 0,
    val periodExpense: Long = 0,
    val periodNet: Long = 0,
    val incomeTrendPercent: Double = 0.0,
    val expenseTrendPercent: Double = 0.0,
    val netTrendPercent: Double = 0.0,
    
    // Filters
    val selectedType: TransactionFilterType = TransactionFilterType.ALL,
    val selectedDateRange: TransactionDateRange = TransactionDateRange.THIS_MONTH,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedSort: TransactionSortOption = TransactionSortOption.DATE_DESC,
    val searchQuery: String = "",

    // Options for filters
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    
    // Data
    val transactionsByDate: Map<LocalDate, List<TransactionItemUi>> = emptyMap()
)

enum class TransactionFilterType {
    ALL, INCOME, EXPENSE, TRANSFER
}

enum class TransactionDateRange {
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME
}

enum class TransactionSortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class TransactionItemUi(
    val id: String,
    val title: String,
    val amount: Long,
    val currency: String,
    val type: TransactionType,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val categoryName: String,
    val categoryIcon: String,
    val accountName: String, // Or "FromAccount"
    val toAccountName: String? = null, // For transfers
    val dateTimeMillis: Long,
    val date: LocalDate,
    val timeFormatted: String,
    val isRecurring: Boolean = false,
    val hasReceipt: Boolean = false,
    val notes: String? = null
)
