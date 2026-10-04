package com.chelumore.xpenze.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute

import com.chelumore.xpenze.feature.dashboard.DashboardScreen
import com.chelumore.xpenze.feature.accounts.list.AccountsScreen
import com.chelumore.xpenze.feature.accounts.addedit.AddEditAccountScreen
import com.chelumore.xpenze.feature.accounts.detail.AccountDetailScreen
import com.chelumore.xpenze.feature.accounts.transfer.TransferScreen
import com.chelumore.xpenze.feature.transactions.list.TransactionsScreen
import com.chelumore.xpenze.feature.transactions.addedit.AddEditTransactionScreen
import com.chelumore.xpenze.feature.transactions.detail.TransactionDetailScreen
import com.chelumore.xpenze.feature.auth.LoginScreen
import com.chelumore.xpenze.feature.auth.SignUpScreen
import com.chelumore.xpenze.feature.auth.ForgotPasswordScreen
import com.chelumore.xpenze.feature.auth.GuestSessionDestination
import com.chelumore.xpenze.feature.auth.VerificationScreen
import com.chelumore.xpenze.feature.auth.SplashScreen
import com.chelumore.xpenze.feature.auth.WelcomeScreen
import com.chelumore.xpenze.feature.about.AboutScreen
import com.chelumore.xpenze.feature.ai.chat.FinanceAiChatScreen
import com.chelumore.xpenze.feature.autocapture.DraftInboxScreen
import com.chelumore.xpenze.feature.autocapture.AutoCaptureSetupScreen
import com.chelumore.xpenze.feature.export.ExportDataScreen
import com.chelumore.xpenze.feature.onboarding.OnboardingScreen
import com.chelumore.xpenze.feature.recurring.RecurringRulesScreen
import com.chelumore.xpenze.feature.reports.AdvancedReportsScreen
import com.chelumore.xpenze.feature.reports.ReportsScreen
import com.chelumore.xpenze.feature.subscription.SubscriptionScreen
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.feature.categories.CategoriesScreen
import com.chelumore.xpenze.feature.categories.addedit.AddEditCategoryScreen
import com.chelumore.xpenze.feature.budgets.addedit.AddEditBudgetScreen
import com.chelumore.xpenze.feature.budgets.detail.BudgetDetailScreen

/**
 * Main navigation graph for the app.
 * Placeholder composables will be replaced with actual screen implementations.
 */
@Composable
fun XpenzeNavGraph(
    navController: NavHostController,
    startDestination: Any = MainGraph,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        navigation<AuthGraph>(startDestination = Screen.Splash) {
            // Auth
            composable<Screen.Splash> {
                SplashScreen(
                    onNavigateToWelcome = {
                        navController.navigate(Screen.Welcome) {
                            popUpTo(Screen.Splash) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToVerification = {
                        navController.navigate(Screen.Verification) {
                            popUpTo(Screen.Splash) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    // Pop the whole graph, not just AuthGraph: a guest reaches sign-in from
                    // MainGraph, so popping only AuthGraph would leave their pre-sign-in
                    // Dashboard and Settings entries stranded underneath.
                    onNavigateToOnboarding = {
                        navController.navigate(OnboardingGraph) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(MainGraph) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Screen.Welcome> {
                WelcomeScreen(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp) },
                    onNavigateToLogin = { navController.navigate(Screen.Login) },
                    onContinueAsGuest = { destination ->
                        val target = when (destination) {
                            GuestSessionDestination.Onboarding -> OnboardingGraph
                            GuestSessionDestination.Main -> MainGraph
                        }
                        navController.navigate(target) {
                            popUpTo(AuthGraph) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Screen.Login> {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword) },
                    onLoginSuccess = {
                        // Route via Splash so we can apply the full decision tree:
                        // email verification + onboarding completion + auth state.
                        navController.navigate(Screen.Splash) {
                            popUpTo(AuthGraph) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRequireVerification = {
                        navController.navigate(Screen.Verification)
                    }
                )
            }

            composable<Screen.SignUp> {
                SignUpScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login) {
                            popUpTo(Screen.SignUp) { inclusive = true }
                        }
                    },
                    onSignUpSuccess = {
                        // Route via Splash so we can handle:
                        // - email/password sign up -> Verification
                        // - Google sign-in -> Onboarding/Main
                        navController.navigate(Screen.Splash) {
                            popUpTo(AuthGraph) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    // Pushed rather than replacing the stack so the user can back out of
                    // verification without losing access to data they already have locally.
                    onRequireVerification = { navController.navigate(Screen.Verification) }
                )
            }

            composable<Screen.ForgotPassword> {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login) {
                            popUpTo(Screen.ForgotPassword) { inclusive = true }
                        }
                    },
                    onSubmit = {
                        // Navigate back to Login after sending reset email
                        navController.navigate(Screen.Login)
                    }
                )
            }

            composable<Screen.Verification> {
                VerificationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onVerifySuccess = {
                        // Route via Splash so we can decide Onboarding vs Main.
                        navController.navigate(Screen.Splash) {
                            popUpTo(AuthGraph) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onResendEmail = { /* Resend logic */ }
                )
            }
        }

        navigation<OnboardingGraph>(startDestination = Screen.Onboarding) {
            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(MainGraph) {
                            popUpTo(OnboardingGraph) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        navigation<MainGraph>(startDestination = Screen.Dashboard) {
            // Main screens (Bottom Navigation)
            composable<Screen.Dashboard> {
                DashboardScreen(
                    onNavigateToAddTransaction = { type ->
                        navController.navigate(Screen.AddTransaction(type))
                    },
                    onNavigateToAllTransactions = {
                        navController.navigate(Screen.Transactions)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts)
                    },
                    onNavigateToDraftInbox = {
                        navController.navigate(Screen.DraftInbox())
                    },
                    onNavigateToDraftInboxAppPicker = {
                        navController.navigate(Screen.DraftInbox(openAppPicker = true))
                    },
                    onNavigateToAutoCaptureSetup = {
                        navController.navigate(Screen.AutoCaptureSetup)
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription)
                    },
                    onNavigateToAiChat = { prompt ->
                        navController.navigate(Screen.AiCoachChat(prompt = prompt))
                    }
                )
            }

            composable<Screen.Transactions> {
                TransactionsScreen(
                    onNavigateToAddTransaction = { type ->
                        navController.navigate(Screen.AddTransaction(type))
                    },
                    onNavigateToEditTransaction = { id ->
                        navController.navigate(Screen.EditTransaction(id))
                    },
                    onNavigateToTransactionDetail = { id ->
                        navController.navigate(Screen.TransactionDetail(id))
                    }
                )
            }

            composable<Screen.Budgets> {
                com.chelumore.xpenze.feature.budgets.BudgetsScreen(
                    onNavigateToAddBudget = { cloneFromBudgetId ->
                        navController.navigate(Screen.AddBudget(cloneFromBudgetId = cloneFromBudgetId))
                    },
                    onNavigateToBudgetDetail = { id ->
                        navController.navigate(Screen.BudgetDetail(id))
                    }
                )
            }

            composable<Screen.Reports> {
                ReportsScreen(
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) },
                    onNavigateToAdvancedReports = { navController.navigate(Screen.AdvancedReports) },
                    onNavigateToAiChat = { prompt ->
                        navController.navigate(Screen.AiCoachChat(prompt = prompt))
                    }
                )
            }

            composable<Screen.Settings> {
                com.chelumore.xpenze.feature.settings.SettingsScreen(
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories) },
                    onNavigateToExportData = { navController.navigate(Screen.ExportData) },
                    onNavigateToDraftInbox = { navController.navigate(Screen.DraftInbox()) },
                    onNavigateToDraftInboxAppPicker = {
                        navController.navigate(Screen.DraftInbox(openAppPicker = true))
                    },
                    onNavigateToAutoCaptureSetup = { navController.navigate(Screen.AutoCaptureSetup) },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) },
                    onNavigateToRecurringRules = { navController.navigate(Screen.RecurringRules) },
                    onNavigateToAbout = { navController.navigate(Screen.About) },
                    onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy) },
                    onNavigateToTerms = { navController.navigate(Screen.TermsOfService) },
                    onNavigateToLicenses = { navController.navigate(Screen.OpenSourceLicenses) },
                    onNavigateToHelp = { navController.navigate(Screen.HelpSupport) },
                    onSignedOut = {
                        navController.navigate(AuthGraph) {
                            popUpTo(MainGraph) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSignIn = { navController.navigate(Screen.Login) }
                )
            }

            composable<Screen.PrivacyPolicy> {
                com.chelumore.xpenze.feature.about.PrivacyPolicyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.TermsOfService> {
                com.chelumore.xpenze.feature.about.TermsOfServiceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.OpenSourceLicenses> {
                com.chelumore.xpenze.feature.about.OpenSourceLicensesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.HelpSupport> {
                com.chelumore.xpenze.feature.about.HelpSupportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Transaction screens
            composable<Screen.AddTransaction> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AddTransaction>()
                AddEditTransactionScreen(
                    transactionId = null,
                    initialType = args.type,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.EditTransaction> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.EditTransaction>()
                AddEditTransactionScreen(
                    transactionId = args.transactionId,
                    initialType = "EXPENSE",
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.TransactionDetail>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "xpenze://transaction/{transactionId}" }
                )
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.TransactionDetail>()
                TransactionDetailScreen(
                    transactionId = args.transactionId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditTransaction(id)) }
                )
            }

            composable<Screen.DraftInbox> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.DraftInbox>()
                DraftInboxScreen(
                    openAppPicker = args.openAppPicker,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) },
                    onNavigateToAutoCaptureSetup = { navController.navigate(Screen.AutoCaptureSetup) },
                    onNavigateToReviewDraft = { draftId, type ->
                        navController.navigate(Screen.ReviewDraft(draftId = draftId, type = type))
                    }
                )
            }

            composable<Screen.AutoCaptureSetup> {
                AutoCaptureSetupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) },
                    onNavigateToDraftInbox = {
                        navController.navigate(Screen.DraftInbox()) {
                            popUpTo(Screen.AutoCaptureSetup) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Screen.ReviewDraft> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.ReviewDraft>()
                AddEditTransactionScreen(
                    transactionId = null,
                    draftId = args.draftId,
                    initialType = args.type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Account screens
            composable<Screen.Accounts> {
                AccountsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddAccount = { navController.navigate(Screen.AddAccount) },
                    onNavigateToAccountDetail = { id -> navController.navigate(Screen.AccountDetail(id)) }
                )
            }

            composable<Screen.AddAccount> {
                AddEditAccountScreen(
                    accountId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.EditAccount> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.EditAccount>()
                AddEditAccountScreen(
                    accountId = args.accountId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.AccountDetail>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "xpenze://account/{accountId}" }
                )
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AccountDetail>()
                AccountDetailScreen(
                    accountId = args.accountId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditAccount(id)) },
                    onNavigateToTransfer = { navController.navigate(Screen.Transfer) }
                )
            }

            composable<Screen.Transfer> {
                TransferScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Category screens
            composable<Screen.Categories> {
                CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdd = { type -> navController.navigate(Screen.AddCategory(type.name)) },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditCategory(id)) }
                )
            }

            composable<Screen.AddCategory> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AddCategory>()
                val type = runCatching { CategoryType.valueOf(args.type) }.getOrDefault(CategoryType.EXPENSE)
                AddEditCategoryScreen(
                    categoryId = null,
                    initialType = type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.EditCategory> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.EditCategory>()
                AddEditCategoryScreen(
                    categoryId = args.categoryId,
                    initialType = CategoryType.EXPENSE,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Budget screens
            composable<Screen.AddBudget> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AddBudget>()
                AddEditBudgetScreen(
                    budgetId = null,
                    cloneFromBudgetId = args.cloneFromBudgetId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) }
                )
            }

            composable<Screen.EditBudget> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.EditBudget>()
                AddEditBudgetScreen(
                    budgetId = args.budgetId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) }
                )
            }

            composable<Screen.BudgetDetail>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "xpenze://budget/{budgetId}" }
                )
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.BudgetDetail>()
                BudgetDetailScreen(
                    budgetId = args.budgetId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditBudget(id)) },
                    onNavigateToAddBudget = { id -> navController.navigate(Screen.AddBudget(cloneFromBudgetId = id)) }
                )
            }

            // Reports screens
            composable<Screen.AdvancedReports> {
                AdvancedReportsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) }
                )
            }

            // Settings screens
            composable<Screen.Subscription> {
                SubscriptionScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<Screen.RecurringRules> {
                RecurringRulesScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<Screen.ExportData> {
                ExportDataScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) }
                )
            }

            composable<Screen.About> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy) },
                    onNavigateToTerms = { navController.navigate(Screen.TermsOfService) },
                    onNavigateToLicenses = { navController.navigate(Screen.OpenSourceLicenses) },
                    onNavigateToHelp = { navController.navigate(Screen.HelpSupport) }
                )
            }

            composable<Screen.AiCoachChat> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.AiCoachChat>()
                FinanceAiChatScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription) },
                    initialPrompt = route.prompt
                )
            }
        }
    }
}
