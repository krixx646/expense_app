package com.chelumore.xpenze.feature.reports

import androidx.compose.ui.graphics.Color
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant

data class ReportsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Filters
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val isCompareEnabled: Boolean = true,
    
    // Summary Data
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val netCashFlow: Long = 0,
    
    // Previous Period Data (for comparison)
    val prevTotalIncome: Long = 0,
    val prevTotalExpense: Long = 0,
    
    // Chart Data
    val categorySpending: List<CategorySpendingUi> = emptyList(),
    val topCategories: List<CategorySpendingUi> = emptyList(),
    val monthlyTrend: List<TrendPoint> = emptyList(), // Placeholder for line chart points

    // AI Insights
    val currencyCode: String = "USD",
    val appLanguageTag: String = "en",
    val monthlyInsights: List<MonthlyInsightUi> = emptyList(),
    val budgetSuggestions: List<BudgetSuggestionUi> = emptyList(),
    val savingsOpportunities: List<PremiumOpportunity> = emptyList(),
    
    // Entitlement
    val hasPremiumAccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val nudgeVariant: PremiumEmotionVariant = PremiumEmotionVariant.PLAYFUL
)

enum class ReportPeriod {
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME
}

data class CategorySpendingUi(
    val categoryId: String,
    val categoryName: String,
    val amount: Long,
    val color: Color,
    val percentage: Float,
    val percentageChange: Float? = null // For top categories list
)

data class TrendPoint(
    val dayStartMillis: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val netCents: Long
)

data class MonthlyInsightUi(
    val categoryName: String,
    val currentCents: Long,
    val previousCents: Long,
    val changePercent: Float
)

data class BudgetSuggestionUi(
    val categoryName: String,
    val averageMonthlyCents: Long,
    val suggestedBudgetCents: Long
)
