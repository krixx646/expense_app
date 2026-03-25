package com.chelumore.xpenze.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for the app.
 * Using Kotlin Serialization for type-safe navigation arguments.
 */
sealed interface Screen {
    
    // Auth & Onboarding
    @Serializable
    data object Splash : Screen
    
    @Serializable
    data object Welcome : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object SignUp : Screen
    
    @Serializable
    data object ForgotPassword : Screen
    
    @Serializable
    data object Verification : Screen
    
    @Serializable
    data object Onboarding : Screen
    
    // Main screens (Bottom Navigation)
    @Serializable
    data object Dashboard : Screen
    
    @Serializable
    data object Transactions : Screen
    
    @Serializable
    data object Budgets : Screen
    
    @Serializable
    data object Reports : Screen
    
    @Serializable
    data object Settings : Screen
    
    // Transaction screens
    @Serializable
    data class AddTransaction(val type: String = "EXPENSE") : Screen
    
    @Serializable
    data class EditTransaction(val transactionId: String) : Screen
    
    @Serializable
    data class TransactionDetail(val transactionId: String) : Screen
    
    // Account screens
    @Serializable
    data object Accounts : Screen
    
    @Serializable
    data object AddAccount : Screen
    
    @Serializable
    data class EditAccount(val accountId: String) : Screen
    
    @Serializable
    data class AccountDetail(val accountId: String) : Screen
    
    @Serializable
    data object Transfer : Screen
    
    // Category screens
    @Serializable
    data object Categories : Screen
    
    @Serializable
    data class AddCategory(val type: String = "EXPENSE") : Screen
    
    @Serializable
    data class EditCategory(val categoryId: String) : Screen
    
    // Budget screens
    @Serializable
    data object AddBudget : Screen
    
    @Serializable
    data class EditBudget(val budgetId: String) : Screen
    
    @Serializable
    data class BudgetDetail(val budgetId: String) : Screen
    
    // Reports screens
    @Serializable
    data object AdvancedReports : Screen
    
    // Settings screens
    @Serializable
    data object Subscription : Screen
    
    @Serializable
    data object RecurringRules : Screen
    
    @Serializable
    data object ExportData : Screen

    @Serializable
    data object DraftInbox : Screen

    @Serializable
    data object AutoCaptureSetup : Screen

    @Serializable
    data class ReviewDraft(
        val draftId: String,
        val type: String = "EXPENSE"
    ) : Screen
    
    @Serializable
    data object About : Screen

    @Serializable
    data object PrivacyPolicy : Screen

    @Serializable
    data object TermsOfService : Screen

    @Serializable
    data object OpenSourceLicenses : Screen

    @Serializable
    data object HelpSupport : Screen

    @Serializable
    data class AiCoachChat(val prompt: String? = null) : Screen
}

/**
 * Bottom navigation destinations
 */
enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val iconName: String
) {
    DASHBOARD(Screen.Dashboard, "Home", "home"),
    TRANSACTIONS(Screen.Transactions, "Transactions", "receipt_long"),
    BUDGETS(Screen.Budgets, "Budgets", "savings"),
    REPORTS(Screen.Reports, "Reports", "bar_chart"),
    SETTINGS(Screen.Settings, "Settings", "settings")
}

