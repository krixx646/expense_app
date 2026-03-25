# Budget & Expense Tracker App — Complete Build Steps

> **Project**: Kotlin Android Budget & Expense Tracker  
> **Stack**: Kotlin + Jetpack Compose + Room + Firebase + Play Billing  
> **Architecture**: Clean Architecture + MVVM + Offline-First  

---

## Legend

- `[ ]` = Not started  
- `[~]` = In progress  
- `[x]` = Completed  
- `[!]` = Blocked / Issue  

---

# PHASE 1: PROJECT FOUNDATION & SETUP

## 1.1 Android Project Initialization

- [x] Create new Android project in Android Studio (Empty Compose Activity)
- [x] Set minimum SDK to API 26 (Android 8.0) for broad compatibility
- [x] Set target SDK to latest stable (API 36)
- [x] Configure app namespace and application ID (`com.chelumore.xpenze`)
- [x] Set up Kotlin DSL for Gradle (`build.gradle.kts`)
- [x] Enable Jetpack Compose in build configuration
- [x] Configure Java 17 compatibility (required for latest Android tooling)

## 1.2 Version Catalog & Dependency Management

- [x] Create `gradle/libs.versions.toml` for centralized dependency management
- [x] Define version variables for all libraries:
  - [x] Kotlin version
  - [x] Compose BOM version
  - [x] AndroidX Core/Lifecycle versions
  - [x] Room version
  - [x] Hilt version
  - [x] Navigation Compose version
  - [x] Firebase BOM version
  - [x] Play Billing version
  - [x] Coroutines version
  - [x] WorkManager version
  - [x] Chart library version (Vico)
  - [x] Testing library versions

## 1.3 Core Dependencies Setup

- [x] Add Jetpack Compose dependencies (via BOM)
- [x] Add Material 3 design dependency
- [x] Add Compose Navigation dependency
- [x] Add Room dependencies (runtime, compiler, ktx)
- [x] Add Hilt dependencies (android, compiler, navigation-compose)
- [x] Add Kotlin Coroutines dependencies (core, android)
- [x] Add Lifecycle dependencies (viewmodel-compose, runtime-compose)
- [x] Add WorkManager dependency
- [x] Add DataStore Preferences dependency
- [x] Add kotlinx-serialization dependency
- [x] Configure Hilt plugin in project-level and app-level Gradle
- [x] Configure KSP (Kotlin Symbol Processing) for Room and Hilt

## 1.4 Project Structure Setup

- [x] Create base package structure:
  ```
  com.chelumore.xpenze/
  ├── XpenzeApplication.kt    # Application class
  ├── MainActivity.kt         # Main activity
  ├── core/
  │   ├── common/             # Common utilities, extensions
  │   ├── database/           # Room database, entities, DAOs
  │   ├── datastore/          # DataStore preferences
  │   ├── di/                 # Hilt modules
  │   ├── navigation/         # Navigation setup
  │   └── ui/
  │       ├── components/     # Reusable UI components
  │       └── theme/          # App theme, colors, typography
  ├── feature/
  │   ├── auth/               # Authentication feature
  │   ├── dashboard/          # Main dashboard
  │   ├── transactions/       # Transaction management
  │   ├── accounts/           # Account management
  │   ├── categories/         # Category management
  │   ├── budgets/            # Budget management
  │   ├── reports/            # Analytics & reports
  │   ├── settings/           # App settings
  │   └── subscription/       # Subscription management
  ├── data/
  │   ├── repository/         # Repository implementations
  │   ├── model/              # Domain models
  │   └── sync/               # Sync logic
  └── domain/
      ├── repository/         # Repository interfaces
      └── usecase/            # Use cases
  ```
- [x] Create placeholder files for each package

## 1.5 Application Class & Hilt Setup

- [x] Create `XpenzeApplication` class annotated with `@HiltAndroidApp`
- [x] Register Application class in `AndroidManifest.xml`
- [x] Create `AppModule` for providing app-wide dependencies
- [x] Create `DatabaseModule` for Room database provision
- [x] Create `RepositoryModule` for repository bindings
- [x] Create `DispatcherModule` for coroutine dispatchers

## 1.6 Theme & Design System

- [x] Define color palette (light theme)
- [x] Define color palette (dark theme)
- [x] Create `Color.kt` with all app colors
- [x] Create `Typography.kt` with app typography (font families, text styles)
- [x] Create `Shape.kt` with corner radius definitions
- [x] Create `Theme.kt` with `XpenzeTheme` composable
- [x] Add dynamic color support (Material You) for Android 12+
- [x] Create `Dimensions.kt` for consistent spacing values

---

# PHASE 2: LOCAL DATABASE (ROOM)

## 2.1 Base Database Setup

- [x] Create `AppDatabase` abstract class extending `RoomDatabase`
- [x] Define database version and migration strategy
- [x] Configure database builder in Hilt module
- [x] Enable foreign key constraints

## 2.2 Entity Definitions

### Transaction Entity
- [x] Create `TransactionEntity` data class with fields:
  - `id: String` (UUID, Primary Key)
  - `accountId: String` (Foreign Key)
  - `categoryId: String` (Foreign Key)
  - `amount: Long` (in cents)
  - `currency: String` (ISO 4217 code)
  - `type: TransactionType` (INCOME, EXPENSE, TRANSFER)
  - `title: String`
  - `notes: String?`
  - `dateTime: Long` (epoch milliseconds)
  - `receiptUri: String?`
  - `isRecurring: Boolean`
  - `recurringRuleId: String?`
  - `tags: String` (JSON array or comma-separated)
  - `synced: Boolean`
  - `deleted: Boolean` (soft delete)
  - `createdAt: Long`
  - `updatedAt: Long`

### Account Entity
- [x] Create `AccountEntity` data class with fields:
  - `id: String` (UUID, Primary Key)
  - `name: String`
  - `type: AccountType` (CASH, BANK, CREDIT_CARD, SAVINGS, INVESTMENT)
  - `currency: String`
  - `initialBalance: Long`
  - `currentBalance: Long` (cached, computed)
  - `colorHex: String`
  - `iconName: String`
  - `isDefault: Boolean`
  - `isArchived: Boolean`
  - `synced: Boolean`
  - `deleted: Boolean`
  - `createdAt: Long`
  - `updatedAt: Long`

### Category Entity
- [x] Create `CategoryEntity` data class with fields:
  - `id: String` (UUID, Primary Key)
  - `name: String`
  - `type: CategoryType` (INCOME, EXPENSE)
  - `parentCategoryId: String?` (for subcategories)
  - `colorHex: String`
  - `iconName: String`
  - `isDefault: Boolean` (system-provided)
  - `sortOrder: Int`
  - `synced: Boolean`
  - `deleted: Boolean`
  - `createdAt: Long`
  - `updatedAt: Long`

### Budget Entity
- [x] Create `BudgetEntity` data class with fields:
  - `id: String` (UUID, Primary Key)
  - `name: String`
  - `categoryId: String?` (null = all categories)
  - `accountId: String?` (null = all accounts)
  - `amountLimitCents: Long`
  - `period: BudgetPeriod` (DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM)
  - `startDate: Long`
  - `endDate: Long?`
  - `alertThresholdPercent: Int` (e.g., 80)
  - `isActive: Boolean`
  - `synced: Boolean`
  - `deleted: Boolean`
  - `createdAt: Long`
  - `updatedAt: Long`

### Recurring Rule Entity
- [x] Create `RecurringRuleEntity` data class with fields:
  - `id: String` (UUID, Primary Key)
  - `transactionTemplateId: String`
  - `frequency: RecurringFrequency` (DAILY, WEEKLY, MONTHLY, YEARLY)
  - `interval: Int` (every N periods)
  - `dayOfWeek: Int?`
  - `dayOfMonth: Int?`
  - `monthOfYear: Int?`
  - `startDate: Long`
  - `endDate: Long?`
  - `nextOccurrence: Long`
  - `isActive: Boolean`
  - `synced: Boolean`
  - `deleted: Boolean`
  - `createdAt: Long`
  - `updatedAt: Long`

### User Subscription Entity
- [x] Create `UserSubscriptionEntity` data class with fields:
  - `id: String` (Primary Key)
  - `productId: String`
  - `purchaseToken: String`
  - `purchaseTime: Long`
  - `expiryTime: Long`
  - `isAutoRenewing: Boolean`
  - `isActive: Boolean`
  - `acknowledgedAt: Long?`
  - `updatedAt: Long`

### Sync Metadata Entity
- [ ] Create `SyncMetadataEntity` data class with fields: (deferred to Phase 14)
  - `id: String` (Primary Key)
  - `tableName: String`
  - `lastSyncTime: Long`
  - `lastSyncStatus: SyncStatus`

## 2.3 Type Converters

- [x] Create `Converters` class with `@TypeConverter` methods:
  - [x] TransactionType enum <-> String
  - [x] AccountType enum <-> String
  - [x] CategoryType enum <-> String
  - [x] BudgetPeriod enum <-> String
  - [x] RecurringFrequency enum <-> String
  - [x] List<String> <-> String (for tags)
- [x] Register converters in AppDatabase

## 2.4 DAO Interfaces

### TransactionDao
- [x] Create `TransactionDao` interface with:
  - [ ] `insert(transaction: TransactionEntity)`
  - [ ] `insertAll(transactions: List<TransactionEntity>)`
  - [ ] `update(transaction: TransactionEntity)`
  - [ ] `delete(transaction: TransactionEntity)`
  - [ ] `softDelete(id: String, updatedAt: Long)`
  - [ ] `getById(id: String): Flow<TransactionEntity?>`
  - [ ] `getAll(): Flow<List<TransactionEntity>>`
  - [ ] `getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>`
  - [ ] `getByAccount(accountId: String): Flow<List<TransactionEntity>>`
  - [ ] `getByCategory(categoryId: String): Flow<List<TransactionEntity>>`
  - [ ] `getByType(type: TransactionType): Flow<List<TransactionEntity>>`
  - [ ] `getUnsynced(): List<TransactionEntity>`
  - [ ] `markSynced(id: String)`
  - [ ] `getExpensesByDateRange(start: Long, end: Long): Flow<Long>` (sum)
  - [ ] `getIncomeByDateRange(start: Long, end: Long): Flow<Long>` (sum)
  - [ ] `getCategorySpending(categoryId: String, start: Long, end: Long): Flow<Long>`

### AccountDao
- [x] Create `AccountDao` interface with:
  - [ ] `insert(account: AccountEntity)`
  - [ ] `update(account: AccountEntity)`
  - [ ] `delete(account: AccountEntity)`
  - [ ] `softDelete(id: String, updatedAt: Long)`
  - [ ] `getById(id: String): Flow<AccountEntity?>`
  - [ ] `getAll(): Flow<List<AccountEntity>>`
  - [ ] `getActive(): Flow<List<AccountEntity>>`
  - [ ] `getDefault(): Flow<AccountEntity?>`
  - [ ] `setDefault(id: String)`
  - [ ] `updateBalance(id: String, newBalance: Long, updatedAt: Long)`
  - [ ] `getUnsynced(): List<AccountEntity>`
  - [ ] `markSynced(id: String)`
  - [ ] `getTotalBalance(): Flow<Long>`

### CategoryDao
- [x] Create `CategoryDao` interface with:
  - [ ] `insert(category: CategoryEntity)`
  - [ ] `insertAll(categories: List<CategoryEntity>)`
  - [ ] `update(category: CategoryEntity)`
  - [ ] `delete(category: CategoryEntity)`
  - [ ] `softDelete(id: String, updatedAt: Long)`
  - [ ] `getById(id: String): Flow<CategoryEntity?>`
  - [ ] `getAll(): Flow<List<CategoryEntity>>`
  - [ ] `getByType(type: CategoryType): Flow<List<CategoryEntity>>`
  - [ ] `getSubcategories(parentId: String): Flow<List<CategoryEntity>>`
  - [ ] `getUnsynced(): List<CategoryEntity>`
  - [ ] `markSynced(id: String)`

### BudgetDao
- [x] Create `BudgetDao` interface with:
  - [ ] `insert(budget: BudgetEntity)`
  - [ ] `update(budget: BudgetEntity)`
  - [ ] `delete(budget: BudgetEntity)`
  - [ ] `softDelete(id: String, updatedAt: Long)`
  - [ ] `getById(id: String): Flow<BudgetEntity?>`
  - [ ] `getAll(): Flow<List<BudgetEntity>>`
  - [ ] `getActive(): Flow<List<BudgetEntity>>`
  - [ ] `getByCategory(categoryId: String): Flow<List<BudgetEntity>>`
  - [ ] `getUnsynced(): List<BudgetEntity>`
  - [ ] `markSynced(id: String)`

### RecurringRuleDao
- [x] Create `RecurringRuleDao` interface with:
  - [ ] `insert(rule: RecurringRuleEntity)`
  - [ ] `update(rule: RecurringRuleEntity)`
  - [ ] `delete(rule: RecurringRuleEntity)`
  - [ ] `getById(id: String): Flow<RecurringRuleEntity?>`
  - [ ] `getAll(): Flow<List<RecurringRuleEntity>>`
  - [ ] `getDueRecurringRules(currentTime: Long): List<RecurringRuleEntity>`
  - [ ] `updateNextOccurrence(id: String, nextOccurrence: Long)`
  - [ ] `getUnsynced(): List<RecurringRuleEntity>`

### SubscriptionDao
- [x] Create `SubscriptionDao` interface with:
  - [ ] `insert(subscription: UserSubscriptionEntity)`
  - [ ] `update(subscription: UserSubscriptionEntity)`
  - [ ] `getActive(): Flow<UserSubscriptionEntity?>`
  - [ ] `getByProductId(productId: String): UserSubscriptionEntity?`
  - [ ] `deleteAll()`

## 2.5 Database Seeding

- [x] Create `DatabaseSeeder` class to pre-populate:
  - [x] Default expense categories (Food, Transport, Entertainment, etc.)
  - [x] Default income categories (Salary, Freelance, Investment, etc.)
  - [ ] Default account (Cash)
- [x] Create seed data JSON/Kotlin constants (in CategoryRepositoryImpl)
- [ ] Implement `RoomDatabase.Callback` to seed on first creation

## 2.6 Database Migrations

- [ ] Create migration strategy document
- [ ] Implement `Migration` objects for future schema changes
- [ ] Configure fallback to destructive migration for development
- [ ] Create migration testing utilities

---

# PHASE 3: DOMAIN LAYER

## 3.1 Domain Models

- [x] Create `Transaction` domain model
- [x] Create `Account` domain model
- [x] Create `Category` domain model
- [x] Create `Budget` domain model with spending progress
- [x] Create `RecurringRule` domain model
- [x] Create `UserSubscription` domain model
- [x] Create `DateRange` utility class
- [x] Create `Money` value class for type-safe currency handling

## 3.2 Mappers

- [x] Create `TransactionMapper` (Entity <-> Domain)
- [x] Create `AccountMapper` (Entity <-> Domain)
- [x] Create `CategoryMapper` (Entity <-> Domain)
- [x] Create `BudgetMapper` (Entity <-> Domain)
- [x] Create `RecurringRuleMapper` (Entity <-> Domain)
- [x] Create extension functions for mapping lists

## 3.3 Repository Interfaces

- [x] Create `TransactionRepository` interface
- [x] Create `AccountRepository` interface
- [x] Create `CategoryRepository` interface
- [x] Create `BudgetRepository` interface
- [x] Create `RecurringRuleRepository` interface
- [x] Create `UserRepository` interface (auth state, preferences)
- [x] Create `SubscriptionRepository` interface
- [x] Create `SyncRepository` interface

## 3.4 Use Cases

### Transaction Use Cases
- [x] `AddTransactionUseCase`
- [x] `UpdateTransactionUseCase`
- [x] `DeleteTransactionUseCase`
- [x] `GetTransactionsUseCase` (with filters)
- [x] `GetTransactionDetailsUseCase`
- [x] `SearchTransactionsUseCase`

### Account Use Cases
- [x] `AddAccountUseCase`
- [x] `UpdateAccountUseCase`
- [x] `DeleteAccountUseCase`
- [x] `GetAccountsUseCase`
- [x] `GetAccountBalanceUseCase`
- [x] `TransferBetweenAccountsUseCase`

### Category Use Cases
- [x] `AddCategoryUseCase`
- [x] `UpdateCategoryUseCase`
- [x] `DeleteCategoryUseCase`
- [x] `GetCategoriesUseCase`

### Budget Use Cases
- [x] `CreateBudgetUseCase`
- [x] `UpdateBudgetUseCase`
- [x] `DeleteBudgetUseCase`
- [x] `GetBudgetsUseCase`
- [x] `GetBudgetProgressUseCase`
- [x] `CheckBudgetAlertsUseCase`

### Analytics Use Cases
- [x] `GetSpendingByCategoryUseCase`
- [x] `GetIncomeVsExpenseUseCase`
- [x] `GetCashFlowUseCase`
- [x] `GetTrendDataUseCase`
- [x] `GetTopSpendingCategoriesUseCase`

### Export Use Cases
- [x] `ExportToCsvUseCase`
- [~] `ExportToPdfUseCase` (stub)
- [~] `ImportFromCsvUseCase` (stub)

---

# PHASE 4: DATA LAYER (REPOSITORIES)

## 4.1 Repository Implementations

- [x] Create `TransactionRepositoryImpl`
  - [x] Implement all interface methods
  - [x] Handle offline-first logic
  - [x] Update account balances on transaction changes
  
- [x] Create `AccountRepositoryImpl`
  - [x] Implement all interface methods
  - [x] Calculate and cache balances
  
- [x] Create `CategoryRepositoryImpl`
  - [x] Implement all interface methods
  - [x] Handle default category protection
  
- [x] Create `BudgetRepositoryImpl`
  - [x] Implement all interface methods
  - [x] Calculate spending progress from transactions
  
- [x] Create `RecurringRuleRepositoryImpl`
  - [x] Implement all interface methods
  - [x] Calculate next occurrence dates
  
- [x] Create `UserRepositoryImpl`
  - [~] Manage user authentication state (local auth-state only; Firebase auth pending in Phase 6)
  - [x] Handle user preferences via DataStore
  
- [x] Create `SubscriptionRepositoryImpl`
  - [x] Manage subscription state
  - [x] Check premium feature access

## 4.2 DataStore Setup

- [x] Create `UserPreferences` data class
- [x] Create `PreferencesDataStore` wrapper class
- [x] Implement preference keys:
  - [x] `isOnboardingComplete`
  - [x] `defaultCurrency`
  - [x] `defaultAccountId`
  - [x] `themeMode` (system, light, dark)
  - [x] `notificationsEnabled`
  - [x] `budgetAlertEnabled`
  - [x] `lastSyncTime`
  - [x] `userId`
  - [x] `userEmail`
  - [x] `isPremium`

---

# PHASE 5: NAVIGATION & CORE UI

## 5.1 Navigation Setup

- [x] Create `Screen` sealed class with all app destinations
- [x] Create `NavGraph` composable with all routes
- [x] Implement nested navigation graphs:
  - [x] Auth graph (welcome/login/sign up/forgot password/verification)
  - [x] Onboarding graph (placeholder; screens TBD)
  - [x] Main graph (dashboard, transactions, accounts, budgets, reports, settings)
- [x] Create `BottomNavBar` composable
- [x] Handle deep linking configuration
- [x] Implement navigation arguments passing

## 5.2 MainActivity Setup

- [x] Annotate `MainActivity` with `@AndroidEntryPoint`
- [x] Set up Compose content with theme
- [x] Implement navigation host
- [x] Handle system UI (status bar, navigation bar colors)
- [x] Implement edge-to-edge display

## 5.3 Core UI Components

### Input Components
- [x] Create `ExpenseTextField` (styled text input)
- [x] Create `AmountInputField` (currency-aware input with formatting)
- [x] Create `DatePickerField` (date selection)
- [x] Create `TimePickerField` (time selection)
- [x] Create `DropdownSelector` (styled dropdown)
- [x] Create `CategoryPicker` (icon + color grid)
- [x] Create `AccountPicker` (account selection)
- [x] Create `CurrencyPicker` (currency selection)

### Display Components
- [x] Create `TransactionListItem` (transaction row)
- [x] Create `AccountCard` (account display)
- [x] Create `CategoryChip` (category display)
- [x] Create `BudgetProgressCard` (budget with progress bar)
- [x] Create `StatCard` (summary statistic display)
- [x] Create `AmountText` (formatted currency display)
- [x] Create `DateHeader` (section header for lists)

### Layout Components
- [x] Create `LoadingScreen` (full-screen loading)
- [x] Create `EmptyState` (empty list placeholder)
- [x] Create `ErrorState` (error display with retry)
- [x] Create `BottomSheet` (reusable bottom sheet)
- [x] Create `ConfirmationDialog` (delete/action confirmation)
- [x] Create `TopAppBar` (consistent app bar)

### Chart Components
- [x] Create `PieChart` composable (category breakdown)
- [x] Create `BarChart` composable (income vs expense)
- [x] Create `LineChart` composable (trend over time)
- [x] Create `ProgressBar` (budget progress)

---

# PHASE 6: AUTHENTICATION

## 6.0 Auth Screens UI (Completed)

- [x] Create `WelcomeScreen` (Splash/Landing)
- [x] Create `SignUpScreen`
- [x] Create `LoginScreen`
- [x] Create `ForgotPasswordScreen`
- [x] Create `VerificationScreen`
- [x] Update Navigation Graph with Auth routes
- [x] Handle BottomBar/FAB visibility for Auth screens

## 6.1 Firebase Setup

- [x] Create Firebase project in Firebase Console
- [x] Add Android app to Firebase project
- [x] Download `google-services.json` and add to app module
- [x] Add Firebase SDK dependencies (BOM + Auth)
- [x] Add Google Services plugin to Gradle (conditionally applied when `google-services.json` exists)
- [ ] Configure Firebase Auth providers (Firebase Console → Authentication → Sign-in method):
  - [ ] Enable **Email/Password**
  - [ ] Enable **Google**
  - [ ] Ensure the app’s **SHA-1 + SHA-256** fingerprints are added (Project settings → Your apps)
  - [ ] Re-download `google-services.json` after any provider/app config changes

## 6.2 Google Sign-In Setup

- [ ] Configure OAuth consent screen in Google Cloud Console
- [ ] Create OAuth 2.0 credentials (Android client ID)
- [x] Add SHA-1 and SHA-256 fingerprints to Firebase
- [x] Add Google Identity Services dependency (`googleid`)
- [x] Add Credential Manager dependency (`androidx.credentials`)

Notes:
- The **Web client ID** used to request an ID token is usually generated by the google-services plugin as `default_web_client_id`.
- The app looks up `default_web_client_id` dynamically at runtime so the project can still compile even if `google-services.json` is missing.

## 6.3 Auth Implementation

- [ ] Create `AuthService` interface (optional refactor)
- [ ] Create `FirebaseAuthService` implementation (optional refactor)
- [x] Implement Google Sign-In flow (Credential Manager → Google ID token → FirebaseAuth)
- [x] Implement email/password sign-up + sign-in (Firebase Auth)
- [x] Implement forgot password (send reset email)
- [x] Implement email verification send + verification check (reload)
- [x] Implement sign-out flow (Firebase signOut + clear DataStore session)
- [x] Implement auth state observation (Flow)
- [x] Create `AuthRepository` to manage auth (email/password)
- [x] Store user info in DataStore on successful login

## 6.4 Auth UI

- [x] Create `LoginScreen` composable (incl. "Continue with Google" button)
- [x] Create `LoginViewModel` (email/password + Google sign-in)
- [ ] Add "Continue without account" option (anonymous mode)
- [ ] Add Terms and privacy policy links
- [x] Create splash screen logic (check auth state + verification + onboarding completion)

## 6.5 Onboarding Flow

- [x] Create `OnboardingScreen` composable (currency → first account → optional budget)
- [x] Create `OnboardingViewModel`
- [x] Save onboarding completion to DataStore
- [x] Navigate to dashboard on completion

---

# PHASE 7: DASHBOARD FEATURE

## 7.1 Dashboard Screen

- [ ] Create `DashboardScreen` composable with:
  - [ ] Month selector (current month by default)
  - [ ] Total balance summary card
  - [ ] Income vs Expense summary cards
  - [ ] Quick action buttons (Add income, Add expense, Transfer)
  - [ ] Spending by category pie chart
  - [ ] Recent transactions list (last 5-10)
  - [ ] Budget alerts section (if budgets exceeded)
  - [ ] "See all" navigation links

## 7.2 Dashboard ViewModel

- [ ] Create `DashboardViewModel`:
  - [ ] Load current month's summary
  - [ ] Calculate total balance across accounts
  - [ ] Get income vs expense totals
  - [ ] Get spending by category
  - [ ] Get recent transactions
  - [ ] Get active budget alerts
  - [ ] Handle month navigation
- [ ] Implement state management with `StateFlow`
- [ ] Handle loading and error states

## 7.3 Dashboard UI State

- [ ] Create `DashboardUiState` data class:
  - [ ] `isLoading: Boolean`
  - [ ] `selectedMonth: YearMonth`
  - [ ] `totalBalance: Long`
  - [ ] `monthlyIncome: Long`
  - [ ] `monthlyExpense: Long`
  - [ ] `spendingByCategory: List<CategorySpending>`
  - [ ] `recentTransactions: List<Transaction>`
  - [ ] `budgetAlerts: List<BudgetAlert>`
  - [ ] `error: String?`

---

# PHASE 8: TRANSACTIONS FEATURE

## 8.1 Transaction List Screen

- [x] Create `TransactionsScreen` composable:
  - [x] Filter chips (All, Income, Expense)
  - [x] Date range selector
  - [x] Search bar
  - [x] Transaction list grouped by date
  - [x] Floating action button (Add transaction)
  - [x] Swipe to delete
  - [x] Pull to refresh
  - [x] Empty state when no transactions

## 8.2 Add/Edit Transaction Screen

- [x] Create `AddEditTransactionScreen` composable:
  - [x] Transaction type toggle (Income/Expense/Transfer)
  - [x] Amount input with currency
  - [x] Category selector (grid or list)
  - [x] Account selector (From/To for transfers)
  - [x] Date and time pickers
  - [x] Title/description input
  - [x] Notes input (optional)
  - [x] Tags input (optional)
  - [x] Receipt photo attachment (optional)
  - [x] Recurring toggle with frequency options
  - [x] Save and Cancel buttons

## 8.3 Transaction Detail Screen

- [x] Create `TransactionDetailScreen` composable:
  - [x] Full transaction details display
  - [x] Receipt image (if attached)
  - [x] Edit button
  - [x] Delete button with confirmation
  - [ ] Related recurring rule info (if applicable)

## 8.4 Transaction ViewModels

- [x] Create `TransactionsViewModel`:
  - [x] Load transactions with filters
  - [x] Handle search
  - [x] Handle delete
  - [x] Manage filter state
  
- [x] Create `AddEditTransactionViewModel`:
  - [x] Load categories and accounts
  - [x] Handle form state
  - [x] Validate input
  - [x] Save transaction (create + update)
  - [ ] Handle recurring rule creation
  
- [x] Create `TransactionDetailViewModel`:
  - [x] Load transaction details
  - [x] Handle delete

## 8.5 Quick Add Widget (Optional)

- [ ] Create home screen widget for quick expense entry
- [ ] Implement widget configuration
- [ ] Handle widget-to-app navigation

---

# PHASE 9: ACCOUNTS FEATURE

## 9.1 Accounts List Screen

- [x] Create `AccountsScreen` composable:
  - [x] Total balance across all accounts
  - [x] Account cards list
  - [x] Add account FAB
  - [x] Show/hide archived accounts toggle
  - [ ] Reorder accounts (drag and drop - optional)

## 9.2 Add/Edit Account Screen

- [x] Create `AddEditAccountScreen` composable:
  - [x] Account name input
  - [x] Account type selector (Cash, Bank, Credit Card, etc.)
  - [x] Currency selector
  - [x] Initial balance input
  - [x] Color picker
  - [x] Icon picker
  - [x] Set as default toggle
  - [x] Save button

## 9.3 Account Detail Screen

- [x] Create `AccountDetailScreen` composable:
  - [x] Account info and current balance
  - [x] Recent transactions for this account
  - [x] Edit button
  - [x] Archive/Delete options
  - [x] Transfer to another account action

## 9.4 Transfer Screen

- [x] Create `TransferScreen` composable:
  - [x] From account selector
  - [x] To account selector
  - [x] Amount input
  - [~] Date picker (deferred; uses current date for now)
  - [x] Notes (optional)
  - [x] Confirm button

## 9.5 Account ViewModels

- [x] Create `AccountsViewModel`
- [x] Create `AddEditAccountViewModel`
- [x] Create `AccountDetailViewModel`
- [x] Create `TransferViewModel`

---

# PHASE 10: CATEGORIES FEATURE

## 10.1 Categories List Screen

- [x] Create `CategoriesScreen` composable:
  - [x] Tab layout (Expense / Income)
  - [x] Category grid or list
  - [x] Add category FAB
  - [x] Edit/Delete on long press

## 10.2 Add/Edit Category Screen

- [x] Create `AddEditCategoryScreen` composable:
  - [x] Category name input
  - [x] Category type (Income/Expense)
  - [x] Parent category selector (for subcategories)
  - [x] Color picker
  - [x] Icon picker
  - [x] Save button

## 10.3 Category ViewModels

- [x] Create `CategoriesViewModel`
- [x] Create `AddEditCategoryViewModel`

---

# PHASE 11: BUDGETS FEATURE

## 11.1 Budgets List Screen

- [x] Create `BudgetsScreen` composable:
  - [x] Active budgets with progress bars
  - [x] Exceeded budgets highlighted
  - [x] Add budget FAB
  - [x] Period filter (This month, All time)

## 11.2 Add/Edit Budget Screen

- [x] Create `AddEditBudgetScreen` composable:
  - [x] Budget name input
  - [x] Amount limit input
  - [x] Category selector (or all categories)
  - [x] Account selector (or all accounts)
  - [x] Period selector (Daily, Weekly, Monthly, Yearly)
  - [ ] Start date picker
  - [ ] End date picker (optional)
  - [x] Alert threshold slider (e.g., 80%)
  - [x] Save button

## 11.3 Budget Detail Screen

- [x] Create `BudgetDetailScreen` composable:
  - [x] Budget info and progress
  - [ ] Spending breakdown by day/week
  - [x] Transactions contributing to this budget
  - [x] Edit button
  - [x] Delete button

## 11.4 Budget Alerts

- [x] Implement `BudgetAlertWorker` (WorkManager)
- [x] Create notification channel for budget alerts
- [x] Show notification when budget threshold exceeded
- [x] Check budgets daily or on transaction save/delete

## 11.5 Budget ViewModels

- [x] Create `BudgetsViewModel`
- [x] Create `AddEditBudgetViewModel`
- [x] Create `BudgetDetailViewModel`

---

# PHASE 12: REPORTS & ANALYTICS FEATURE

## 12.1 Reports Screen (Free Tier)

- [x] Create `ReportsScreen` composable:
  - [x] Date range selector
  - [x] Income vs Expense bar chart (Summary Cards + Trend Line)
  - [x] Spending by category pie chart (Donut)
  - [x] Top spending categories list
  - [x] Monthly trend line chart
  - [x] Export button (CSV/PDF/Excel)

## 12.2 Advanced Reports Screen (Premium)

- [x] Create `AdvancedReportsScreen` composable (Integrated as locked section in Reports):
  - [x] Trend forecasting chart
  - [x] Year-over-year comparison
  - [x] Category trend analysis
  - [x] Recurring expense analysis
  - [ ] Custom date range deep dive
  - [x] PDF export option
  - [x] Lock icon / upgrade prompt for free users

## 12.3 Export Functionality

- [~] Implement CSV export:
  - [x] Generate CSV string from transactions
  - [x] Save to device (system file picker / CreateDocument)
  - [x] Share via Intent
  
- [ ] Implement PDF export (Premium):
  - [ ] Use Android PdfDocument API
  - [ ] Generate formatted report
  - [ ] Include charts as images
  - [ ] Save to Downloads folder
  - [ ] Share via Intent

## 12.4 Reports ViewModels

- [x] Create `ReportsViewModel`
- [ ] Create `AdvancedReportsViewModel`
- [ ] Implement chart data transformations

---

# PHASE 13: SETTINGS FEATURE

## 13.1 Settings Screen

- [x] Create `SettingsScreen` composable with sections:

  **Account**
  - [x] User profile info (name, email, photo)
  - [x] Sign out button
  - [ ] Link Google account (if anonymous)
  - [x] Delete account option

  **Preferences**
  - [x] Default currency selector
  - [x] Default account selector
  - [x] Theme selector (System, Light, Dark)
  - [ ] Language selector (if supporting multiple)

  **Data**
  - [ ] Export data (backup)
  - [ ] Import data (restore)
  - [x] Clear all data (with confirmation)

  **Notifications**
  - [x] Enable/disable notifications toggle
  - [x] Budget alert notifications toggle
  - [ ] Recurring transaction reminders toggle

  **Subscription**
  - [ ] Current plan display
  - [ ] Upgrade to Premium button
  - [ ] Restore purchases button

  **About**
  - [x] App version
  - [x] Privacy policy link
  - [x] Terms of service link
  - [x] Open source licenses
  - [x] Contact/Feedback link

## 13.2 Settings ViewModel

- [ ] Create `SettingsViewModel`:
  - [ ] Load user preferences
  - [ ] Handle preference changes
  - [ ] Handle sign out
  - [ ] Handle data export/import
  - [ ] Handle account deletion

---

# PHASE 14: CLOUD SYNC (FIREBASE FIRESTORE)

## 14.1 Firestore Data Structure

- [ ] Design Firestore collections structure:
  ```
  users/{userId}/
  ├── profile/
  │   └── info (name, email, settings)
  ├── transactions/
  │   └── {transactionId}
  ├── accounts/
  │   └── {accountId}
  ├── categories/
  │   └── {categoryId}
  ├── budgets/
  │   └── {budgetId}
  └── recurringRules/
      └── {ruleId}
  ```
- [ ] Define Firestore data models (may differ from Room entities)
- [ ] Create mappers between Room entities and Firestore documents

## 14.2 Sync Service

- [ ] Create `SyncService` interface
- [ ] Create `FirestoreSyncService` implementation
- [ ] Implement sync methods:
  - [ ] `syncTransactions()`
  - [ ] `syncAccounts()`
  - [ ] `syncCategories()`
  - [ ] `syncBudgets()`
  - [ ] `fullSync()` (initial sync)
  - [ ] `deltaSync()` (incremental sync)

## 14.3 Sync Strategy Implementation

- [ ] Implement offline-first with local-first writes:
  1. Write to Room immediately
  2. Mark as `synced = false`
  3. Queue for sync
  4. Sync to Firestore when online
  5. Mark as `synced = true`

- [ ] Implement pull sync:
  1. Fetch remote changes since last sync
  2. Apply changes to local Room DB
  3. Handle conflicts (last-write-wins)
  4. Update sync timestamp

- [ ] Implement conflict resolution:
  - [ ] Compare `updatedAt` timestamps
  - [ ] Server wins for same timestamp
  - [ ] Log conflicts for debugging

## 14.4 WorkManager Sync Jobs

- [ ] Create `SyncWorker` class:
  - [ ] Extend `CoroutineWorker`
  - [ ] Implement `doWork()` for sync logic
  - [ ] Handle network constraints
  - [ ] Implement retry with exponential backoff

- [ ] Schedule sync jobs:
  - [ ] Immediate sync on important changes
  - [ ] Periodic sync (every 15-30 minutes when online)
  - [ ] Sync on app startup
  - [ ] Sync on connectivity restored

- [ ] Create `SyncManager` class:
  - [ ] Queue sync requests
  - [ ] Manage sync state (syncing, idle, error)
  - [ ] Expose sync status as Flow

## 14.5 Firestore Security Rules

- [ ] Write Firestore security rules:
  ```
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId}/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
  ```
- [ ] Test security rules in Firebase Emulator
- [ ] Deploy security rules

## 14.6 Network Connectivity Handling

- [ ] Create `ConnectivityObserver` interface
- [ ] Implement `NetworkConnectivityObserver` using `ConnectivityManager`
- [ ] Expose connectivity state as Flow
- [ ] Trigger sync on connectivity restored
- [ ] Show offline indicator in UI

---

# PHASE 15: IN-APP SUBSCRIPTIONS (PLAY BILLING)

## 15.1 Play Console Setup

- [ ] Create Google Play Developer account ($25 one-time fee)
- [ ] Create app in Play Console
- [ ] Set up subscription products:
  - [ ] Monthly subscription (e.g., `premium_monthly`)
  - [ ] Yearly subscription (e.g., `premium_yearly`)
- [ ] Configure pricing and regions
- [ ] Set up test license testers

## 15.2 Play Billing Integration

- [ ] Add Play Billing Library dependency
- [ ] Create `BillingService` interface
- [ ] Create `PlayBillingService` implementation:
  - [ ] Initialize `BillingClient`
  - [ ] Connect to Play Billing service
  - [ ] Query available products
  - [ ] Launch purchase flow
  - [ ] Handle purchase results
  - [ ] Acknowledge purchases
  - [ ] Query existing purchases (restore)
  - [ ] Handle subscription state changes

## 15.3 Subscription State Management

- [ ] Create `SubscriptionManager` class:
  - [ ] Check subscription status on app start
  - [ ] Cache subscription state locally
  - [ ] Expose `isPremium` as StateFlow
  - [ ] Handle subscription expiry

- [ ] Store subscription info in Room:
  - [ ] Product ID
  - [ ] Purchase token
  - [ ] Expiry date
  - [ ] Auto-renew status

## 15.4 Subscription UI

- [ ] Create `SubscriptionScreen` composable:
  - [ ] Feature comparison (Free vs Premium)
  - [ ] Pricing display
  - [ ] Subscribe buttons (Monthly/Yearly)
  - [ ] Current subscription status
  - [ ] Cancel subscription info
  - [ ] Restore purchases button

- [ ] Create `SubscriptionViewModel`:
  - [ ] Load products from Play
  - [ ] Handle purchase flow
  - [ ] Handle restore purchases
  - [ ] Manage loading/error states

## 15.5 Premium Feature Gating

- [ ] Create `PremiumFeatureGate` composable wrapper
- [ ] Implement feature checks:
  - [ ] Advanced reports
  - [ ] PDF export
  - [ ] Custom recurring rules
  - [ ] Priority sync
  - [ ] Ad-free (if using ads in free tier)
- [ ] Show upgrade prompts when accessing premium features

## 15.6 Server-Side Validation (Optional but Recommended)

- [ ] Set up Firebase Cloud Functions for receipt validation
- [ ] Implement Real-Time Developer Notifications (RTDN) webhook
- [ ] Validate purchases server-side
- [ ] Handle subscription lifecycle events

---

# PHASE 16: RECURRING TRANSACTIONS

## 16.1 Recurring Transaction Logic

- [ ] Create `RecurringTransactionProcessor` class:
  - [ ] Calculate next occurrence from rule
  - [ ] Generate transaction from template
  - [ ] Update next occurrence date

## 16.2 Background Processing

- [ ] Create `RecurringTransactionWorker`:
  - [ ] Run daily to check due transactions
  - [ ] Create transactions for due rules
  - [ ] Update next occurrence dates
  - [ ] Handle missed occurrences

- [ ] Schedule worker with WorkManager:
  - [ ] Daily at midnight or specified time
  - [ ] With device idle constraints

## 16.3 Recurring UI

- [ ] Add recurring toggle in Add Transaction screen
- [ ] Create `RecurringRulesScreen` to manage all rules
- [ ] Show recurring indicator on transactions
- [ ] Allow editing/stopping recurring rules

---

# PHASE 17: NOTIFICATIONS

## 17.1 Notification Setup

- [ ] Create notification channels:
  - [ ] Budget alerts (high importance)
  - [ ] Recurring reminders (default importance)
  - [ ] Sync status (low importance)

- [ ] Request notification permission (Android 13+)

## 17.2 Budget Alert Notifications

- [ ] Create `BudgetAlertNotificationService`:
  - [ ] Show notification when budget exceeds threshold
  - [ ] Include budget name and percentage
  - [ ] Action: View budget detail

## 17.3 Recurring Transaction Reminders

- [ ] Create `RecurringReminderNotificationService`:
  - [ ] Remind before recurring transaction is due
  - [ ] Action: Confirm or skip

---

# PHASE 18: SECURITY & PRIVACY

## 18.1 Data Encryption

- [ ] Implement EncryptedSharedPreferences for sensitive prefs
- [ ] Store auth tokens securely in Android Keystore
- [ ] Consider SQLCipher for database encryption (optional)

## 18.2 API Key Protection

- [ ] Store Firebase config in `google-services.json` (normal)
- [ ] Use BuildConfig for any additional API keys
- [ ] Never commit API keys to version control
- [ ] Use ProGuard/R8 to obfuscate code

## 18.3 Network Security

- [ ] Ensure TLS for all network calls (Firebase uses HTTPS)
- [ ] Implement certificate pinning (optional, advanced)
- [ ] Create `network_security_config.xml` for debug/release

## 18.4 Privacy Compliance

- [ ] Create Privacy Policy document/page
- [ ] Implement data export feature (GDPR)
- [ ] Implement data deletion feature (GDPR)
- [ ] Add privacy policy link in app settings
- [ ] Handle data deletion on account delete

## 18.5 Biometric Lock (Optional)

- [ ] Add BiometricPrompt dependency
- [ ] Implement app lock with fingerprint/face
- [ ] Add setting to enable/disable biometric lock

---

# PHASE 19: TESTING

## 19.1 Unit Tests

- [ ] Add test dependencies (JUnit, MockK, Turbine for Flows)
- [ ] Test use cases:
  - [ ] `AddTransactionUseCaseTest`
  - [ ] `GetBudgetProgressUseCaseTest`
  - [ ] `ExportToCsvUseCaseTest`
- [ ] Test repositories (with fake DAOs)
- [ ] Test mappers
- [ ] Test utility functions

## 19.2 DAO Tests

- [ ] Add Room testing dependency
- [ ] Create in-memory database for tests
- [ ] Test all DAO operations:
  - [ ] `TransactionDaoTest`
  - [ ] `AccountDaoTest`
  - [ ] `CategoryDaoTest`
  - [ ] `BudgetDaoTest`

## 19.3 ViewModel Tests

- [ ] Test ViewModels with fake repositories:
  - [ ] `DashboardViewModelTest`
  - [ ] `TransactionsViewModelTest`
  - [ ] `AddEditTransactionViewModelTest`
  - [ ] `BudgetsViewModelTest`

## 19.4 UI Tests (Instrumentation)

- [ ] Add Compose testing dependencies
- [ ] Test critical user flows:
  - [ ] Login flow
  - [ ] Add transaction flow
  - [ ] View dashboard
  - [ ] Create budget
  - [ ] Export data
- [ ] Test navigation

## 19.5 Integration Tests

- [ ] Test Room + Repository integration
- [ ] Test Firestore sync (with emulator)
- [ ] Test Play Billing (with test products)

---

# PHASE 20: CI/CD & RELEASE

## 20.1 GitHub Actions Setup

- [ ] Create `.github/workflows/android.yml`:
  - [ ] Trigger on push to main/develop
  - [ ] Trigger on pull requests
  - [ ] Set up JDK
  - [ ] Cache Gradle dependencies
  - [ ] Run linting
  - [ ] Run unit tests
  - [ ] Build debug APK
  - [ ] Upload APK as artifact

## 20.2 Release Signing

- [ ] Generate release signing key (keystore)
- [ ] Store keystore securely (NOT in version control)
- [ ] Add signing config to Gradle
- [ ] Configure GitHub Secrets for CI signing
- [ ] Enable Google Play App Signing (recommended)

## 20.3 Release Build Configuration

- [ ] Configure release build type:
  - [ ] Enable minification (R8)
  - [ ] Enable resource shrinking
  - [ ] Configure ProGuard rules
  - [ ] Disable debugging

## 20.4 Play Store Preparation

- [ ] Create app listing:
  - [ ] App title and description
  - [ ] Screenshots (phone, tablet)
  - [ ] Feature graphic
  - [ ] App icon
  - [ ] Category selection
- [ ] Create privacy policy URL
- [ ] Complete data safety form
- [ ] Set up internal testing track
- [ ] Upload first build

## 20.5 Release Workflow

- [ ] Document release process:
  1. Update version code and name
  2. Update changelog
  3. Create git tag
  4. Build release APK/AAB
  5. Test on internal track
  6. Promote to production
- [ ] Set up automated releases (optional)

---

# PHASE 21: POLISH & OPTIMIZATION

## 21.1 Performance Optimization

- [ ] Profile app with Android Profiler
- [ ] Optimize Compose recompositions
- [ ] Implement pagination for long lists
- [ ] Optimize database queries (add indexes)
- [ ] Lazy load images
- [ ] Reduce APK size

## 21.2 Accessibility

- [ ] Add content descriptions to all images/icons
- [ ] Ensure proper focus order
- [ ] Test with TalkBack
- [ ] Support large text/display scaling
- [ ] Ensure sufficient color contrast

## 21.3 Localization

- [ ] Extract all strings to `strings.xml`
- [ ] Set up localization structure
- [ ] Add translations for target languages
- [ ] Handle RTL layouts
- [ ] Format currencies and dates per locale

## 21.4 Edge Cases & Error Handling

- [ ] Handle no internet gracefully
- [ ] Handle sync conflicts
- [ ] Handle subscription expiry
- [ ] Handle database migration errors
- [ ] Handle auth token expiry
- [ ] Add retry mechanisms

## 21.5 Analytics & Monitoring

- [ ] Integrate Firebase Analytics:
  - [ ] Screen views
  - [ ] Key user actions (add transaction, create budget)
  - [ ] Subscription events
- [ ] Integrate Firebase Crashlytics:
  - [ ] Automatic crash reporting
  - [ ] Non-fatal error logging
- [ ] Set up custom events for funnel analysis

---

# PHASE 22: DOCUMENTATION

## 22.1 Code Documentation

- [ ] Add KDoc comments to public APIs
- [ ] Document complex algorithms
- [ ] Create architecture decision records (ADRs)

## 22.2 User Documentation

- [ ] Create in-app help/FAQ section
- [ ] Create onboarding tooltips
- [ ] Write support documentation

## 22.3 Developer Documentation

- [ ] Write README.md with setup instructions
- [ ] Document build configurations
- [ ] Document release process
- [ ] Document Firebase setup steps
- [ ] Document Play Console setup

---

# APPENDIX: FILE STRUCTURE (COMPLETE)

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/yourname/expensetracker/
│   │   │   ├── ExpenseTrackerApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   │
│   │   │   ├── core/
│   │   │   │   ├── common/
│   │   │   │   │   ├── Constants.kt
│   │   │   │   │   ├── DateUtils.kt
│   │   │   │   │   ├── CurrencyUtils.kt
│   │   │   │   │   ├── Extensions.kt
│   │   │   │   │   └── Result.kt
│   │   │   │   │
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── Converters.kt
│   │   │   │   │   ├── DatabaseSeeder.kt
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── TransactionEntity.kt
│   │   │   │   │   │   ├── AccountEntity.kt
│   │   │   │   │   │   ├── CategoryEntity.kt
│   │   │   │   │   │   ├── BudgetEntity.kt
│   │   │   │   │   │   ├── RecurringRuleEntity.kt
│   │   │   │   │   │   └── UserSubscriptionEntity.kt
│   │   │   │   │   └── dao/
│   │   │   │   │       ├── TransactionDao.kt
│   │   │   │   │       ├── AccountDao.kt
│   │   │   │   │       ├── CategoryDao.kt
│   │   │   │   │       ├── BudgetDao.kt
│   │   │   │   │       ├── RecurringRuleDao.kt
│   │   │   │   │       └── SubscriptionDao.kt
│   │   │   │   │
│   │   │   │   ├── datastore/
│   │   │   │   │   ├── UserPreferences.kt
│   │   │   │   │   └── PreferencesDataStore.kt
│   │   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   ├── RepositoryModule.kt
│   │   │   │   │   ├── DispatcherModule.kt
│   │   │   │   │   └── NetworkModule.kt
│   │   │   │   │
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── Screen.kt
│   │   │   │   │   ├── NavGraph.kt
│   │   │   │   │   └── NavigationActions.kt
│   │   │   │   │
│   │   │   │   └── ui/
│   │   │   │       ├── components/
│   │   │   │       │   ├── inputs/
│   │   │   │       │   │   ├── ExpenseTextField.kt
│   │   │   │       │   │   ├── AmountInputField.kt
│   │   │   │       │   │   ├── DatePickerField.kt
│   │   │   │       │   │   └── DropdownSelector.kt
│   │   │   │       │   ├── display/
│   │   │   │       │   │   ├── TransactionListItem.kt
│   │   │   │       │   │   ├── AccountCard.kt
│   │   │   │       │   │   ├── CategoryChip.kt
│   │   │   │       │   │   ├── BudgetProgressCard.kt
│   │   │   │       │   │   └── AmountText.kt
│   │   │   │       │   ├── layout/
│   │   │   │       │   │   ├── LoadingScreen.kt
│   │   │   │       │   │   ├── EmptyState.kt
│   │   │   │       │   │   └── ErrorState.kt
│   │   │   │       │   └── charts/
│   │   │   │       │       ├── PieChart.kt
│   │   │   │       │       ├── BarChart.kt
│   │   │   │       │       └── LineChart.kt
│   │   │   │       │
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt
│   │   │   │           ├── Typography.kt
│   │   │   │           ├── Shape.kt
│   │   │   │           ├── Dimensions.kt
│   │   │   │           └── Theme.kt
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Transaction.kt
│   │   │   │   │   ├── Account.kt
│   │   │   │   │   ├── Category.kt
│   │   │   │   │   ├── Budget.kt
│   │   │   │   │   ├── RecurringRule.kt
│   │   │   │   │   └── UserSubscription.kt
│   │   │   │   │
│   │   │   │   ├── repository/
│   │   │   │   │   ├── TransactionRepository.kt
│   │   │   │   │   ├── AccountRepository.kt
│   │   │   │   │   ├── CategoryRepository.kt
│   │   │   │   │   ├── BudgetRepository.kt
│   │   │   │   │   ├── RecurringRuleRepository.kt
│   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   ├── SubscriptionRepository.kt
│   │   │   │   │   └── SyncRepository.kt
│   │   │   │   │
│   │   │   │   └── usecase/
│   │   │   │       ├── transaction/
│   │   │   │       │   ├── AddTransactionUseCase.kt
│   │   │   │       │   ├── UpdateTransactionUseCase.kt
│   │   │   │       │   ├── DeleteTransactionUseCase.kt
│   │   │   │       │   ├── GetTransactionsUseCase.kt
│   │   │   │       │   └── SearchTransactionsUseCase.kt
│   │   │   │       ├── account/
│   │   │   │       │   ├── AddAccountUseCase.kt
│   │   │   │       │   ├── UpdateAccountUseCase.kt
│   │   │   │       │   ├── DeleteAccountUseCase.kt
│   │   │   │       │   ├── GetAccountsUseCase.kt
│   │   │   │       │   └── TransferBetweenAccountsUseCase.kt
│   │   │   │       ├── category/
│   │   │   │       │   ├── AddCategoryUseCase.kt
│   │   │   │       │   ├── UpdateCategoryUseCase.kt
│   │   │   │       │   ├── DeleteCategoryUseCase.kt
│   │   │   │       │   └── GetCategoriesUseCase.kt
│   │   │   │       ├── budget/
│   │   │   │       │   ├── CreateBudgetUseCase.kt
│   │   │   │       │   ├── UpdateBudgetUseCase.kt
│   │   │   │       │   ├── DeleteBudgetUseCase.kt
│   │   │   │       │   ├── GetBudgetsUseCase.kt
│   │   │   │       │   ├── GetBudgetProgressUseCase.kt
│   │   │   │       │   └── CheckBudgetAlertsUseCase.kt
│   │   │   │       ├── analytics/
│   │   │   │       │   ├── GetSpendingByCategoryUseCase.kt
│   │   │   │       │   ├── GetIncomeVsExpenseUseCase.kt
│   │   │   │       │   ├── GetCashFlowUseCase.kt
│   │   │   │       │   └── GetTrendDataUseCase.kt
│   │   │   │       └── export/
│   │   │   │           ├── ExportToCsvUseCase.kt
│   │   │   │           ├── ExportToPdfUseCase.kt
│   │   │   │           └── ImportFromCsvUseCase.kt
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── repository/
│   │   │   │   │   ├── TransactionRepositoryImpl.kt
│   │   │   │   │   ├── AccountRepositoryImpl.kt
│   │   │   │   │   ├── CategoryRepositoryImpl.kt
│   │   │   │   │   ├── BudgetRepositoryImpl.kt
│   │   │   │   │   ├── RecurringRuleRepositoryImpl.kt
│   │   │   │   │   ├── UserRepositoryImpl.kt
│   │   │   │   │   ├── SubscriptionRepositoryImpl.kt
│   │   │   │   │   └── SyncRepositoryImpl.kt
│   │   │   │   │
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── TransactionMapper.kt
│   │   │   │   │   ├── AccountMapper.kt
│   │   │   │   │   ├── CategoryMapper.kt
│   │   │   │   │   ├── BudgetMapper.kt
│   │   │   │   │   └── RecurringRuleMapper.kt
│   │   │   │   │
│   │   │   │   └── sync/
│   │   │   │       ├── SyncService.kt
│   │   │   │       ├── FirestoreSyncService.kt
│   │   │   │       ├── SyncManager.kt
│   │   │   │       └── SyncWorker.kt
│   │   │   │
│   │   │   ├── feature/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthService.kt
│   │   │   │   │   ├── FirebaseAuthService.kt
│   │   │   │   │   ├── GoogleAuthUiClient.kt
│   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   ├── LoginViewModel.kt
│   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   └── OnboardingViewModel.kt
│   │   │   │   │
│   │   │   │   ├── dashboard/
│   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   │   └── DashboardUiState.kt
│   │   │   │   │
│   │   │   │   ├── transactions/
│   │   │   │   │   ├── list/
│   │   │   │   │   │   ├── TransactionsScreen.kt
│   │   │   │   │   │   └── TransactionsViewModel.kt
│   │   │   │   │   ├── addedit/
│   │   │   │   │   │   ├── AddEditTransactionScreen.kt
│   │   │   │   │   │   └── AddEditTransactionViewModel.kt
│   │   │   │   │   └── detail/
│   │   │   │   │       ├── TransactionDetailScreen.kt
│   │   │   │   │       └── TransactionDetailViewModel.kt
│   │   │   │   │
│   │   │   │   ├── accounts/
│   │   │   │   │   ├── list/
│   │   │   │   │   │   ├── AccountsScreen.kt
│   │   │   │   │   │   └── AccountsViewModel.kt
│   │   │   │   │   ├── addedit/
│   │   │   │   │   │   ├── AddEditAccountScreen.kt
│   │   │   │   │   │   └── AddEditAccountViewModel.kt
│   │   │   │   │   ├── detail/
│   │   │   │   │   │   ├── AccountDetailScreen.kt
│   │   │   │   │   │   └── AccountDetailViewModel.kt
│   │   │   │   │   └── transfer/
│   │   │   │   │       ├── TransferScreen.kt
│   │   │   │   │       └── TransferViewModel.kt
│   │   │   │   │
│   │   │   │   ├── categories/
│   │   │   │   │   ├── CategoriesScreen.kt
│   │   │   │   │   ├── CategoriesViewModel.kt
│   │   │   │   │   ├── AddEditCategoryScreen.kt
│   │   │   │   │   └── AddEditCategoryViewModel.kt
│   │   │   │   │
│   │   │   │   ├── budgets/
│   │   │   │   │   ├── list/
│   │   │   │   │   │   ├── BudgetsScreen.kt
│   │   │   │   │   │   └── BudgetsViewModel.kt
│   │   │   │   │   ├── addedit/
│   │   │   │   │   │   ├── AddEditBudgetScreen.kt
│   │   │   │   │   │   └── AddEditBudgetViewModel.kt
│   │   │   │   │   └── detail/
│   │   │   │   │       ├── BudgetDetailScreen.kt
│   │   │   │   │       └── BudgetDetailViewModel.kt
│   │   │   │   │
│   │   │   │   ├── reports/
│   │   │   │   │   ├── ReportsScreen.kt
│   │   │   │   │   ├── ReportsViewModel.kt
│   │   │   │   │   ├── AdvancedReportsScreen.kt
│   │   │   │   │   └── AdvancedReportsViewModel.kt
│   │   │   │   │
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   │
│   │   │   │   └── subscription/
│   │   │   │       ├── BillingService.kt
│   │   │   │       ├── PlayBillingService.kt
│   │   │   │       ├── SubscriptionManager.kt
│   │   │   │       ├── SubscriptionScreen.kt
│   │   │   │       └── SubscriptionViewModel.kt
│   │   │   │
│   │   │   └── worker/
│   │   │       ├── RecurringTransactionWorker.kt
│   │   │       └── BudgetAlertWorker.kt
│   │   │
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   ├── drawable/
│   │   │   ├── mipmap/
│   │   │   └── xml/
│   │   │       └── network_security_config.xml
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   ├── test/
│   │   └── java/com/yourname/expensetracker/
│   │       ├── domain/usecase/
│   │       ├── data/repository/
│   │       └── core/database/dao/
│   │
│   └── androidTest/
│       └── java/com/yourname/expensetracker/
│           ├── feature/
│           └── core/database/
│
├── build.gradle.kts
├── proguard-rules.pro
└── google-services.json

gradle/
└── libs.versions.toml

.github/
└── workflows/
    └── android.yml
```

---

# PROGRESS SUMMARY

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Project Foundation & Setup | [x] COMPLETE |
| 2 | Local Database (Room) | [x] COMPLETE |
| 3 | Domain Layer | [x] COMPLETE |
| 4 | Data Layer (Repositories) | [~] Partial (offline-first CRUD + balances + budgets + recurring + local subscription done; cloud sync pending) |
| 5 | Navigation & Core UI | [x] COMPLETE |
| 6 | Authentication | [~] Partial (email/password + Google + verification flows done; anonymous mode + legal links pending) |
| 7 | Dashboard Feature | [x] COMPLETE (real data + month selector + dynamic greeting/name/photo) |
| 8 | Transactions Feature | [x] COMPLETE (filters/sort + add/edit/detail + tags/receipt + recurring toggle) |
| 9 | Accounts Feature | [~] Implemented (list/add/edit/detail/transfer; reorder optional) |
| 10 | Categories Feature | [x] COMPLETE |
| 11 | Budgets Feature | [~] Implemented (alerts done; advanced breakdown pending) |
| 12 | Reports & Analytics Feature | [~] Implemented (core reports + CSV export; PDF/Excel pending / removed from UI) |
| 13 | Settings Feature | [x] COMPLETE (profile name/photo + theme + notifications + export/import + legal/about screens) |
| 14 | Cloud Sync (Firebase Firestore) | [~] Implemented (offline-first push/pull + tombstones + background worker; security rules + conflict UX pending) |
| 15 | In-App Subscriptions (Play Billing) | [ ] |
| 16 | Recurring Transactions | [x] COMPLETE (rules + worker + scheduler + add-from-transaction) |
| 17 | Notifications | [~] Implemented (budget alerts + daily summary; recurring reminders pending) |
| 18 | Security & Privacy | [ ] |
| 19 | Testing | [~] Partial (unit tests + build verification) |
| 20 | CI/CD & Release | [~] Partial (CI workflow + release build config; signing requires keystore) |
| 21 | Polish & Optimization | [ ] |
| 22 | Documentation | [ ] |

---

**Last Updated**: January 2, 2026

**Current Phase**: Phase 21 — Polish & production hardening (tests/release/legal links)

**Notes**:
- Phase 1–3 complete (project + Room + domain + use cases)
- Core feature set is functional end-to-end: Dashboard, Transactions (list/add/edit/detail), Budgets (list/add/edit/detail), Accounts (list/add/edit/detail/transfer), Categories (list/add/edit), Reports, Settings, Export/Import CSV, Recurring rules
- Removed/avoided placeholder UI: no `PlaceholderScreen`, no “coming soon” menus for features that aren’t implemented
- Remaining “finish” work is now mostly product hardening: real Play Billing, optional Firestore sync, recurring reminders, and deeper analytics/polish

