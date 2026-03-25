package com.chelumore.xpenze.feature.dashboard

import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import java.time.LocalDate
import java.time.YearMonth

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val userName: String = "User",
    val userPhotoUrl: String? = null,
    val hasPremiumAccess: Boolean = false,
    val currencyCode: String = "USD",
    val appLanguageTag: String = "en",
    val autoCaptureEnabled: Boolean = false,
    val pendingAutoCaptureDraftCount: Int = 0,
    val accountCardStyle: AccountCardStyle = AccountCardStyle.MATTE,
    val accountCardStyleOverrides: Map<String, AccountCardStyle> = emptyMap(),
    val areBalancesHidden: Boolean = false,
    
    // Balance
    val totalBalance: Long = 0,
    val balanceChangePercent: Double = 0.0,
    
    // Income/Expense
    val monthlyIncome: Long = 0,
    val monthlyExpense: Long = 0,
    
    // Spending
    val categorySpending: List<CategorySpendingUi> = emptyList(),
    
    // Recent Transactions
    val recentTransactions: List<TransactionUi> = emptyList(),
    
    // Budget Alerts
    val budgetAlerts: List<BudgetAlertUi> = emptyList(),

    // Premium value
    val savingsOpportunities: List<PremiumOpportunity> = emptyList(),
    
    // Accounts
    val accounts: List<Account> = emptyList()
)

data class CategorySpendingUi(
    val name: String,
    val amount: Long,
    val colorHex: String,
    val percent: Float
)

data class TransactionUi(
    val id: String,
    val title: String,
    val amount: Long,
    val currency: String,
    val type: TransactionType,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val accountName: String,
    val toAccountName: String? = null,
    val date: LocalDate,
    val isRecurring: Boolean = false,
    val hasReceipt: Boolean = false
)

data class BudgetAlertUi(
    val budgetName: String,
    val percentUsed: Int,
    val alertThresholdPercent: Int,
    val overspentAmount: Long
)

