# Xpenze UI Specification & Data Requirements

> Complete breakdown of all screens, attributes, data flows, and UI components

---

## Table of Contents
1. [Home (Dashboard)](#1-home-dashboard)
2. [Transactions](#2-transactions)
3. [Budgets](#3-budgets)
4. [Reports](#4-reports)
5. [Settings](#5-settings)
6. [Shared Components](#6-shared-components)
7. [Navigation & Modals](#7-navigation--modals)

---

# 1. HOME (DASHBOARD)

## 1.1 Dashboard Main Screen

### Purpose
Quick overview of financial health, recent activity, and actionable insights.

### Data Attributes Required

#### Header Section
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `currentMonth` | YearMonth | System | Currently displayed month (e.g., "December 2025") |
| `userName` | String? | UserRepository | User's display name (for greeting) |
| `userPhotoUrl` | String? | UserRepository | Profile photo URL |
| `isPremium` | Boolean | SubscriptionRepository | Show premium badge or upgrade prompt |

#### Balance Summary Card
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `totalBalance` | Long | AccountRepository.getTotalBalance() | Sum of all account balances (in cents) |
| `defaultCurrency` | String | UserRepository | Currency for display (e.g., "USD") |
| `balanceChangePercent` | Double | Calculated | % change from previous month |
| `balanceChangeDirection` | Enum | Calculated | UP, DOWN, NEUTRAL |

#### Income vs Expense Summary
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `monthlyIncome` | Long | TransactionRepository.getIncomeByDateRange() | Total income this month |
| `monthlyExpense` | Long | TransactionRepository.getExpensesByDateRange() | Total expenses this month |
| `netCashFlow` | Long | Calculated | income - expense |
| `incomeVsLastMonth` | Double | Calculated | % change in income |
| `expenseVsLastMonth` | Double | Calculated | % change in expenses |

#### Spending by Category (Pie Chart)
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `categorySpending` | List<CategorySpending> | Aggregated | Category breakdown |
| → `categoryId` | String | | Category identifier |
| → `categoryName` | String | | Display name |
| → `categoryColor` | String | | Hex color for chart |
| → `categoryIcon` | String | | Icon name |
| → `amount` | Long | | Spent amount in cents |
| → `percentage` | Double | | % of total spending |
| `topCategories` | List<CategorySpending> | Filtered | Top 5 categories |
| `otherCategoriesTotal` | Long | Calculated | Sum of remaining categories |

#### Quick Actions
| Action | Navigation Target | Icon |
|--------|-------------------|------|
| Add Income | AddTransaction(type="INCOME") | add_circle |
| Add Expense | AddTransaction(type="EXPENSE") | remove_circle |
| Transfer | Transfer | swap_horiz |
| View All Accounts | Accounts | account_balance_wallet |

#### Recent Transactions
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `recentTransactions` | List<Transaction> | TransactionRepository.getRecent(5) | Last 5 transactions |
| → `id` | String | | Transaction ID |
| → `title` | String | | Transaction title |
| → `amount` | Long | | Amount in cents |
| → `type` | TransactionType | | INCOME, EXPENSE, TRANSFER |
| → `categoryName` | String | | Category display name |
| → `categoryColor` | String | | Category color |
| → `categoryIcon` | String | | Category icon |
| → `accountName` | String | | Account name |
| → `dateTime` | Long | | Epoch timestamp |
| → `formattedDate` | String | | "Today", "Yesterday", or date |

#### Budget Alerts
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `budgetAlerts` | List<BudgetAlert> | BudgetRepository + calculations | Budgets near/over limit |
| → `budgetId` | String | | Budget ID |
| → `budgetName` | String | | Budget name |
| → `categoryName` | String? | | Associated category |
| → `limitAmount` | Long | | Budget limit |
| → `spentAmount` | Long | | Amount spent |
| → `percentUsed` | Int | | Usage percentage |
| → `status` | AlertStatus | | WARNING (≥80%), EXCEEDED (>100%) |
| → `remainingDays` | Int | | Days left in budget period |

#### Accounts Overview (Optional Mini-List)
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `accounts` | List<Account> | AccountRepository.getActive() | Active accounts |
| → `id` | String | | Account ID |
| → `name` | String | | Account name |
| → `type` | AccountType | | CASH, BANK, etc. |
| → `balance` | Long | | Current balance |
| → `currency` | String | | Account currency |
| → `color` | String | | Account color |
| → `icon` | String | | Account icon |

### UI Components Needed
- [ ] `BalanceSummaryCard` - Large card with total balance
- [ ] `IncomeExpenseRow` - Side-by-side income/expense stats
- [ ] `SpendingPieChart` - Interactive pie chart
- [ ] `QuickActionButtons` - Row of action buttons
- [ ] `TransactionListItem` - Compact transaction row
- [ ] `BudgetAlertCard` - Warning/exceeded budget display
- [ ] `AccountMiniCard` - Small account balance card
- [ ] `MonthSelector` - Month navigation (< December 2025 >)
- [ ] `SectionHeader` - "Recent Transactions" with "See All" link

### States to Handle
- [ ] Loading state (shimmer/skeleton)
- [ ] Empty state (no transactions yet)
- [ ] Error state (failed to load data)
- [ ] No accounts state (prompt to create first account)

---

# 2. TRANSACTIONS

## 2.1 Transactions List Screen

### Purpose
View, search, filter, and manage all transactions.

### Data Attributes Required

#### Filter Bar
| Attribute | Type | Options | Description |
|-----------|------|---------|-------------|
| `selectedType` | TransactionType? | ALL, INCOME, EXPENSE, TRANSFER | Filter by type |
| `selectedAccount` | String? | Account IDs | Filter by account |
| `selectedCategory` | String? | Category IDs | Filter by category |
| `dateRange` | DateRange | Custom | Start and end date |
| → `startDate` | Long | | Range start |
| → `endDate` | Long | | Range end |
| `searchQuery` | String | | Text search in title/notes |
| `sortOrder` | SortOrder | DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC | Sort order |

#### Transaction List
| Attribute | Type | Source | Description |
|-----------|------|--------|-------------|
| `transactions` | List<TransactionGroup> | Grouped by date | Transactions grouped by date |
| → `date` | LocalDate | | Group date |
| → `dateLabel` | String | | "Today", "Yesterday", "Mon, Dec 8" |
| → `dailyTotal` | Long | | Net for that day |
| → `transactions` | List<Transaction> | | Transactions for that day |

#### Individual Transaction (in list)
| Attribute | Type | Description |
|-----------|------|-------------|
| `id` | String | Transaction ID |
| `title` | String | Transaction title |
| `amount` | Long | Amount in cents |
| `formattedAmount` | String | Formatted with currency & sign |
| `type` | TransactionType | INCOME, EXPENSE, TRANSFER |
| `categoryName` | String | Category name |
| `categoryColor` | String | For icon background |
| `categoryIcon` | String | Icon name |
| `accountName` | String | Account name |
| `toAccountName` | String? | For transfers |
| `dateTime` | Long | Timestamp |
| `formattedTime` | String | "2:30 PM" |
| `notes` | String? | Preview of notes |
| `hasReceipt` | Boolean | Show attachment indicator |
| `isRecurring` | Boolean | Show recurring indicator |
| `tags` | List<String> | Tag chips |

#### Summary Header
| Attribute | Type | Description |
|-----------|------|-------------|
| `periodIncome` | Long | Total income in filtered period |
| `periodExpense` | Long | Total expense in filtered period |
| `periodNet` | Long | Net cash flow |
| `transactionCount` | Int | Number of transactions |

### UI Components Needed
- [ ] `TransactionFilterBar` - Chips/dropdowns for filters
- [ ] `SearchBar` - Text search input
- [ ] `DateRangePicker` - Date range selection
- [ ] `TransactionListItem` - Full transaction row
- [ ] `DateSectionHeader` - Date divider with daily total
- [ ] `TransactionSummaryHeader` - Period totals
- [ ] `SwipeToDelete` - Swipe action for delete
- [ ] `FloatingActionButton` - Add transaction
- [ ] `FilterBottomSheet` - Advanced filter options
- [ ] `SortBottomSheet` - Sort options

### States to Handle
- [ ] Loading state
- [ ] Empty state (no transactions)
- [ ] Empty search results
- [ ] Pull-to-refresh

### Actions Available
| Action | Trigger | Navigation |
|--------|---------|------------|
| Add Transaction | FAB | AddTransaction |
| View Details | Tap item | TransactionDetail |
| Edit | Long press → Edit | EditTransaction |
| Delete | Swipe or long press | Confirmation dialog |
| Filter | Filter chips | FilterBottomSheet |
| Search | Search icon | Expand search bar |

---

## 2.2 Add/Edit Transaction Screen

### Purpose
Create new or edit existing transaction.

### Form Fields

| Field | Type | Required | Validation | Default |
|-------|------|----------|------------|---------|
| `type` | TransactionType | Yes | Must select | EXPENSE |
| `amount` | Long | Yes | > 0 | 0 |
| `currency` | String | Yes | Valid ISO code | User default |
| `title` | String | Yes | 1-100 chars | "" |
| `categoryId` | String | Yes* | Must exist | null |
| `accountId` | String | Yes | Must exist | Default account |
| `toAccountId` | String | Yes** | Must exist, ≠ accountId | null |
| `dateTime` | Long | Yes | Not future (configurable) | Now |
| `notes` | String | No | 0-500 chars | null |
| `tags` | List<String> | No | Max 10 tags | [] |
| `receiptUri` | String | No | Valid URI | null |
| `isRecurring` | Boolean | No | - | false |
| `recurringFrequency` | RecurringFrequency | If recurring | - | MONTHLY |
| `recurringEndDate` | Long | No | > startDate | null |

*Required for INCOME/EXPENSE, optional for TRANSFER
**Required only for TRANSFER

### UI Layout

#### Transaction Type Selector
```
┌─────────────────────────────────────────┐
│  [EXPENSE]    [INCOME]    [TRANSFER]    │
│     🔴          🟢           🔵          │
└─────────────────────────────────────────┘
```

#### Amount Input
```
┌─────────────────────────────────────────┐
│              $ 0.00                     │
│         [Currency: USD ▼]               │
└─────────────────────────────────────────┘
```

#### Category Selector (Grid)
```
┌─────────────────────────────────────────┐
│ Category                      [+ Add]   │
├─────────────────────────────────────────┤
│ 🍔 Food    🚗 Transport   🛍️ Shopping  │
│ 🎬 Fun     💡 Bills       💪 Health    │
│ ✈️ Travel  📚 Education   💇 Personal  │
│ ... (scrollable grid)                   │
└─────────────────────────────────────────┘
```

#### Account Selector
```
┌─────────────────────────────────────────┐
│ From Account                            │
│ ┌─────────────────────────────────────┐ │
│ │ 💰 Cash                    $1,234   │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ To Account (for transfers)              │
│ ┌─────────────────────────────────────┐ │
│ │ 🏦 Bank Account            $5,678   │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

#### Date & Time
```
┌─────────────────────────────────────────┐
│ Date        │ Mon, Dec 8, 2025    [📅] │
│ Time        │ 2:30 PM             [🕐] │
└─────────────────────────────────────────┘
```

#### Additional Fields
```
┌─────────────────────────────────────────┐
│ Title *                                 │
│ ┌─────────────────────────────────────┐ │
│ │ Grocery shopping                    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Notes                                   │
│ ┌─────────────────────────────────────┐ │
│ │ Weekly groceries from Walmart       │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Tags                                    │
│ [groceries] [weekly] [+ Add tag]        │
│                                         │
│ Receipt                                 │
│ [📷 Add photo]  [📁 Choose file]        │
│                                         │
│ ☐ Make this recurring                   │
│   └─ Frequency: [Monthly ▼]             │
│   └─ End date: [Never ▼]                │
└─────────────────────────────────────────┘
```

### UI Components Needed
- [ ] `TransactionTypeSelector` - Segmented button
- [ ] `AmountInput` - Large currency input with keypad
- [ ] `CurrencySelector` - Dropdown/bottom sheet
- [ ] `CategoryGrid` - Scrollable grid of categories
- [ ] `AccountSelector` - Dropdown with balance display
- [ ] `DateTimePicker` - Date and time selection
- [ ] `TagInput` - Chip input with suggestions
- [ ] `PhotoPicker` - Camera/gallery picker
- [ ] `RecurringOptions` - Frequency and end date

### States to Handle
- [ ] Form validation errors
- [ ] Saving in progress
- [ ] Save success (navigate back)
- [ ] Save error
- [ ] Unsaved changes warning on back

---

## 2.3 Transaction Detail Screen

### Purpose
View full transaction details with edit/delete options.

### Data Displayed
| Field | Display Format |
|-------|----------------|
| Type | Icon + "Expense" / "Income" / "Transfer" |
| Amount | Large formatted amount with sign and color |
| Category | Icon + name |
| Account | "From: Account Name" |
| To Account | "To: Account Name" (transfers) |
| Date | "Monday, December 8, 2025" |
| Time | "2:30 PM" |
| Title | Full title |
| Notes | Full notes text |
| Tags | Chip list |
| Receipt | Thumbnail image (tap to view full) |
| Recurring | Frequency + next occurrence |
| Created | "Created Dec 8, 2025" |
| Modified | "Modified Dec 8, 2025" |

### Actions
- [ ] Edit (→ EditTransaction)
- [ ] Delete (confirmation dialog)
- [ ] Duplicate (→ AddTransaction with prefilled data)
- [ ] Share (export as text)
- [ ] View recurring rule (if recurring)

---

# 3. BUDGETS

## 3.1 Budgets List Screen

### Purpose
View and manage all budgets with progress tracking.

### Data Attributes Required

#### Budget List Item
| Attribute | Type | Description |
|-----------|------|-------------|
| `id` | String | Budget ID |
| `name` | String | Budget name |
| `categoryId` | String? | Associated category (null = all) |
| `categoryName` | String | "All Categories" or category name |
| `categoryColor` | String | For progress bar |
| `categoryIcon` | String | Category icon |
| `accountId` | String? | Associated account (null = all) |
| `accountName` | String | "All Accounts" or account name |
| `period` | BudgetPeriod | DAILY, WEEKLY, MONTHLY, YEARLY |
| `periodLabel` | String | "This Month", "This Week", etc. |
| `limitAmount` | Long | Budget limit in cents |
| `spentAmount` | Long | Amount spent so far |
| `remainingAmount` | Long | limit - spent |
| `percentUsed` | Int | 0-100+ (can exceed 100) |
| `status` | BudgetStatus | ON_TRACK, WARNING, EXCEEDED |
| `daysRemaining` | Int | Days left in period |
| `dailyAllowance` | Long | remaining / daysRemaining |
| `isActive` | Boolean | Is budget active |
| `startDate` | Long | Budget start date |
| `endDate` | Long? | Budget end date (null = ongoing) |

#### Summary Stats
| Attribute | Type | Description |
|-----------|------|-------------|
| `totalBudgeted` | Long | Sum of all budget limits |
| `totalSpent` | Long | Sum of all spending |
| `budgetsOnTrack` | Int | Count of budgets < 80% |
| `budgetsWarning` | Int | Count of budgets 80-100% |
| `budgetsExceeded` | Int | Count of budgets > 100% |

### UI Layout

#### Budget Card
```
┌─────────────────────────────────────────┐
│ 🍔 Food & Dining              MONTHLY   │
│                                         │
│ $450 / $600                       75%   │
│ ████████████████████░░░░░░░░           │
│                                         │
│ $150 remaining · 8 days left            │
│ ~$18.75/day to stay on track            │
└─────────────────────────────────────────┘
```

#### Exceeded Budget Card (Red accent)
```
┌─────────────────────────────────────────┐
│ 🛍️ Shopping                   MONTHLY   │
│                              ⚠️ EXCEEDED │
│ $520 / $400                      130%   │
│ █████████████████████████████████████   │
│                                         │
│ $120 over budget                        │
└─────────────────────────────────────────┘
```

### UI Components Needed
- [ ] `BudgetCard` - Card with progress bar
- [ ] `BudgetProgressBar` - Colored progress bar (green → yellow → red)
- [ ] `BudgetSummaryHeader` - Overview stats
- [ ] `PeriodFilter` - Filter by period type
- [ ] `BudgetStatusChip` - ON_TRACK, WARNING, EXCEEDED

### States to Handle
- [ ] Loading
- [ ] Empty (no budgets created)
- [ ] All budgets on track (celebration state?)
- [ ] Multiple exceeded (alert state)

---

## 3.2 Add/Edit Budget Screen

### Form Fields

| Field | Type | Required | Validation | Default |
|-------|------|----------|------------|---------|
| `name` | String | Yes | 1-50 chars | "" |
| `limitAmount` | Long | Yes | > 0 | 0 |
| `period` | BudgetPeriod | Yes | - | MONTHLY |
| `categoryId` | String | No | Must exist | null (all) |
| `accountId` | String | No | Must exist | null (all) |
| `startDate` | Long | Yes | - | Start of current period |
| `endDate` | Long | No | > startDate | null |
| `alertThreshold` | Int | Yes | 1-100 | 80 |
| `isActive` | Boolean | Yes | - | true |

### UI Layout
```
┌─────────────────────────────────────────┐
│ Budget Name *                           │
│ ┌─────────────────────────────────────┐ │
│ │ Monthly Food Budget                 │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Budget Limit *                          │
│ ┌─────────────────────────────────────┐ │
│ │ $ 600.00                            │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Period *                                │
│ [Daily] [Weekly] [MONTHLY] [Yearly]     │
│                                         │
│ Category (optional)                     │
│ ┌─────────────────────────────────────┐ │
│ │ 🍔 Food & Dining              ▼    │ │
│ └─────────────────────────────────────┘ │
│ Leave empty to track all categories     │
│                                         │
│ Account (optional)                      │
│ ┌─────────────────────────────────────┐ │
│ │ All Accounts                  ▼    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Alert me when I reach                   │
│ ──────────●────────── 80%               │
│                                         │
│ Start Date                              │
│ ┌─────────────────────────────────────┐ │
│ │ Dec 1, 2025                   [📅] │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ End Date (optional)                     │
│ ┌─────────────────────────────────────┐ │
│ │ Never                         [📅] │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ☑ Budget is active                      │
└─────────────────────────────────────────┘
```

---

## 3.3 Budget Detail Screen

### Data Displayed
| Field | Description |
|-------|-------------|
| Budget header | Name, period, status |
| Progress circle | Visual progress indicator |
| Spent vs Limit | Amount spent / limit |
| Remaining | Amount left |
| Days remaining | Days left in period |
| Daily allowance | Suggested daily spending |
| Spending trend | Chart of daily spending |
| Transactions | List of transactions in this budget |
| Category breakdown | If budget covers all categories |

### Actions
- [ ] Edit budget
- [ ] Delete budget
- [ ] Pause/Resume budget
- [ ] View all transactions
- [ ] Adjust limit

---

# 4. REPORTS

## 4.1 Reports Main Screen (Free Tier)

### Purpose
Visualize spending patterns and financial trends.

### Data Attributes Required

#### Date Range Selector
| Attribute | Type | Options |
|-----------|------|---------|
| `selectedRange` | DateRangeType | THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, CUSTOM |
| `startDate` | Long | Range start |
| `endDate` | Long | Range end |
| `comparisonEnabled` | Boolean | Compare to previous period |

#### Income vs Expense Chart (Bar Chart)
| Attribute | Type | Description |
|-----------|------|-------------|
| `periods` | List<PeriodData> | Data points for chart |
| → `label` | String | "Jan", "Feb", or "Week 1" |
| → `income` | Long | Total income |
| → `expense` | Long | Total expense |
| → `net` | Long | income - expense |

#### Spending by Category (Pie/Donut Chart)
| Attribute | Type | Description |
|-----------|------|-------------|
| `categoryData` | List<CategorySpending> | Category breakdown |
| → `categoryId` | String | Category ID |
| → `categoryName` | String | Display name |
| → `categoryColor` | String | Chart color |
| → `amount` | Long | Total spent |
| → `percentage` | Double | % of total |
| → `transactionCount` | Int | Number of transactions |

#### Top Spending Categories
| Attribute | Type | Description |
|-----------|------|-------------|
| `topCategories` | List<CategorySpending> | Top 5 by amount |
| → `rank` | Int | 1, 2, 3, etc. |
| → `changeFromLastPeriod` | Double | % change |

#### Cash Flow Summary
| Attribute | Type | Description |
|-----------|------|-------------|
| `totalIncome` | Long | Period income |
| `totalExpense` | Long | Period expense |
| `netCashFlow` | Long | Net flow |
| `savingsRate` | Double | (income - expense) / income |

#### Monthly Trend (Line Chart)
| Attribute | Type | Description |
|-----------|------|-------------|
| `trendData` | List<TrendPoint> | Points for line chart |
| → `date` | Long | Date |
| → `cumulativeExpense` | Long | Running total |
| → `cumulativeIncome` | Long | Running total |
| → `balance` | Long | Running balance |

### UI Components Needed
- [ ] `DateRangeSelector` - Preset ranges + custom
- [ ] `BarChart` - Income vs Expense comparison
- [ ] `PieChart` / `DonutChart` - Category breakdown
- [ ] `LineChart` - Trend over time
- [ ] `CategoryRankingList` - Top categories with bars
- [ ] `SummaryStatsRow` - Income / Expense / Net
- [ ] `ComparisonToggle` - Compare to previous period
- [ ] `ExportButton` - CSV export

### Export Options (Free)
| Format | Contents |
|--------|----------|
| CSV | All transactions in date range |

---

## 4.2 Advanced Reports Screen (Premium)

### Additional Features

#### Trend Forecasting
| Attribute | Type | Description |
|-----------|------|-------------|
| `forecastData` | List<ForecastPoint> | Predicted future spending |
| → `date` | Long | Future date |
| → `predictedExpense` | Long | Predicted amount |
| → `confidence` | Double | Confidence interval |
| `projectedMonthEnd` | Long | Predicted month-end balance |
| `projectedYearEnd` | Long | Predicted year-end balance |

#### Year-over-Year Comparison
| Attribute | Type | Description |
|-----------|------|-------------|
| `yearComparison` | List<YearData> | Compare years |
| → `year` | Int | Year |
| → `totalIncome` | Long | Year income |
| → `totalExpense` | Long | Year expense |
| → `avgMonthlySpend` | Long | Average monthly |
| → `topCategory` | String | Highest spending category |

#### Recurring Expense Analysis
| Attribute | Type | Description |
|-----------|------|-------------|
| `recurringExpenses` | List<RecurringAnalysis> | Subscription analysis |
| → `title` | String | Expense name |
| → `frequency` | RecurringFrequency | How often |
| → `amount` | Long | Per occurrence |
| → `annualCost` | Long | Yearly total |
| → `categoryName` | String | Category |
| → `lastOccurrence` | Long | Last charged date |

#### Category Trends
| Attribute | Type | Description |
|-----------|------|-------------|
| `categoryTrends` | List<CategoryTrend> | 6-month trends per category |
| → `categoryId` | String | Category |
| → `categoryName` | String | Name |
| → `monthlyData` | List<Long> | Last 6 months spending |
| → `trend` | TrendDirection | UP, DOWN, STABLE |
| → `changePercent` | Double | 6-month change |

### Export Options (Premium)
| Format | Contents |
|--------|----------|
| PDF | Formatted report with charts |
| CSV | Detailed transaction export |
| Excel | Multi-sheet workbook |

### UI Components Needed (Premium)
- [ ] `ForecastChart` - Predicted trend line with confidence band
- [ ] `YearComparisonTable` - Side-by-side year stats
- [ ] `RecurringExpenseCard` - Subscription tracker
- [ ] `CategoryTrendChart` - Sparkline trends
- [ ] `PremiumBadge` - Indicator for premium features
- [ ] `UpgradePrompt` - Shown to free users

---

# 5. SETTINGS

## 5.1 Settings Main Screen

### Sections & Items

#### Profile Section
| Item | Type | Data | Action |
|------|------|------|--------|
| User Avatar | Image | `userPhotoUrl` | Tap to change |
| User Name | Text | `userName` | Tap to edit |
| User Email | Text | `userEmail` | Display only |
| Sign Out | Button | - | Confirm → sign out |
| Link Google Account | Button | If anonymous | → Google sign-in |

#### Preferences Section
| Item | Type | Data | Options |
|------|------|------|---------|
| Default Currency | Dropdown | `defaultCurrency` | List of currencies |
| Default Account | Dropdown | `defaultAccountId` | User's accounts |
| Theme | Selector | `themeMode` | System, Light, Dark |
| Language | Dropdown | `language` | Supported languages |
| First Day of Week | Dropdown | `firstDayOfWeek` | Sun, Mon |
| Date Format | Dropdown | `dateFormat` | MM/DD/YY, DD/MM/YY, etc. |

#### Notifications Section
| Item | Type | Data | Description |
|------|------|------|-------------|
| Enable Notifications | Toggle | `notificationsEnabled` | Master toggle |
| Budget Alerts | Toggle | `budgetAlertEnabled` | When budget limit near |
| Recurring Reminders | Toggle | `recurringReminders` | Before recurring transactions |
| Daily Summary | Toggle | `dailySummary` | Daily spending recap |
| Weekly Report | Toggle | `weeklyReport` | Weekly email/notification |

#### Data Section
| Item | Type | Action |
|------|------|--------|
| Accounts | Navigation | → Accounts screen |
| Categories | Navigation | → Categories screen |
| Recurring Transactions | Navigation | → RecurringRules screen |
| Export Data | Navigation | → ExportData screen |
| Import Data | Navigation | → ImportData flow |
| Backup & Sync | Navigation | → Sync settings |
| Clear All Data | Button | Confirm → delete all |

#### Subscription Section
| Item | Type | Data | Action |
|------|------|------|--------|
| Current Plan | Text | Free / Premium | Display |
| Expiry Date | Text | `expiryDate` | If premium |
| Upgrade | Button | - | → Subscription screen |
| Restore Purchases | Button | - | Restore from Play |
| Manage Subscription | Button | - | → Play Store |

#### About Section
| Item | Type | Action |
|------|------|--------|
| App Version | Text | Display version |
| Privacy Policy | Link | Open browser |
| Terms of Service | Link | Open browser |
| Open Source Licenses | Navigation | → Licenses screen |
| Rate the App | Link | Open Play Store |
| Send Feedback | Link | Open email |
| Help & FAQ | Link | Open help page |

### UI Components Needed
- [ ] `SettingsSection` - Grouped settings with header
- [ ] `SettingsItem` - Single setting row
- [ ] `SettingsToggle` - Toggle switch item
- [ ] `SettingsDropdown` - Dropdown selection
- [ ] `SettingsNavigation` - Navigates to sub-screen
- [ ] `UserProfileCard` - Avatar, name, email
- [ ] `SubscriptionStatusCard` - Current plan display
- [ ] `DangerButton` - For destructive actions

---

## 5.2 Accounts Management Screen

### Data Displayed
| Attribute | Type | Description |
|-----------|------|-------------|
| `accounts` | List<Account> | All accounts |
| → `id` | String | Account ID |
| → `name` | String | Account name |
| → `type` | AccountType | CASH, BANK, etc. |
| → `balance` | Long | Current balance |
| → `currency` | String | Account currency |
| → `color` | String | Account color |
| → `icon` | String | Account icon |
| → `isDefault` | Boolean | Default account marker |
| → `isArchived` | Boolean | Archived status |
| `totalBalance` | Long | Sum of all balances |

### Actions
- [ ] Add account
- [ ] Edit account
- [ ] Archive account
- [ ] Delete account (if no transactions)
- [ ] Set as default
- [ ] Reorder accounts (drag & drop)

---

## 5.3 Categories Management Screen

### Data Displayed
| Attribute | Type | Description |
|-----------|------|-------------|
| `expenseCategories` | List<Category> | Expense categories |
| `incomeCategories` | List<Category> | Income categories |
| → `id` | String | Category ID |
| → `name` | String | Category name |
| → `color` | String | Category color |
| → `icon` | String | Category icon |
| → `isDefault` | Boolean | System category (can't delete) |
| → `transactionCount` | Int | Number of transactions |

### Actions
- [ ] Add category
- [ ] Edit category
- [ ] Delete category (if no transactions, not default)
- [ ] Reorder categories

---

## 5.4 Export Data Screen

### Export Options
| Option | Format | Content | Tier |
|--------|--------|---------|------|
| Transactions CSV | .csv | All transactions | Free |
| Full Backup | .json | All app data | Free |
| Report PDF | .pdf | Formatted report | Premium |
| Excel Export | .xlsx | Multi-sheet workbook | Premium |

### Fields to Select (for CSV)
- [ ] Date
- [ ] Type
- [ ] Amount
- [ ] Currency
- [ ] Category
- [ ] Account
- [ ] Title
- [ ] Notes
- [ ] Tags

### Date Range Selection
- All time
- This year
- This month
- Custom range

---

## 5.5 Subscription Screen

### Data Displayed
| Attribute | Type | Description |
|-----------|------|-------------|
| `currentPlan` | String | "Free" or "Premium" |
| `isPremium` | Boolean | Premium status |
| `expiryDate` | Long? | When subscription ends |
| `isAutoRenewing` | Boolean | Auto-renew status |
| `products` | List<SubscriptionProduct> | Available products |
| → `productId` | String | Play product ID |
| → `title` | String | "Monthly" / "Yearly" |
| → `price` | String | Formatted price |
| → `pricePeriod` | String | "/month" or "/year" |
| → `savings` | String? | "Save 20%" for yearly |

### UI Layout
```
┌─────────────────────────────────────────┐
│          ⭐ XPENZE PREMIUM ⭐            │
│                                         │
│ Unlock powerful features:               │
│ ✓ Advanced analytics & forecasting      │
│ ✓ PDF report exports                    │
│ ✓ Unlimited budgets                     │
│ ✓ Priority sync                         │
│ ✓ No ads                                │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ YEARLY           $29.99/year        │ │
│ │                  Save 40%! 🔥       │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ MONTHLY          $4.99/month        │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ [Restore Purchases]                     │
│                                         │
│ Terms · Privacy · Cancel anytime        │
└─────────────────────────────────────────┘
```

---

# 6. SHARED COMPONENTS

## 6.1 Common UI Components

| Component | Usage | Props |
|-----------|-------|-------|
| `AmountText` | Display formatted currency | amount, currency, showSign, size |
| `CategoryIcon` | Category with colored background | iconName, color, size |
| `AccountIcon` | Account type icon | accountType, color, size |
| `TransactionTypeIcon` | Income/Expense/Transfer icon | type, size |
| `DateText` | Formatted date display | timestamp, format |
| `ProgressBar` | Budget/goal progress | value, max, color |
| `EmptyState` | No data placeholder | icon, title, message, action |
| `ErrorState` | Error display | message, retryAction |
| `LoadingState` | Loading indicator | message |
| `ConfirmDialog` | Confirmation modal | title, message, onConfirm, onCancel |
| `BottomSheet` | Modal bottom sheet | content, onDismiss |
| `Chip` | Tag/filter chip | label, selected, onClick |
| `Badge` | Notification badge | count, color |

## 6.2 Form Components

| Component | Usage | Props |
|-----------|-------|-------|
| `TextField` | Text input | value, onValueChange, label, error |
| `AmountField` | Currency input | value, onValueChange, currency |
| `DateField` | Date picker trigger | value, onValueChange, label |
| `TimeField` | Time picker trigger | value, onValueChange, label |
| `DropdownField` | Dropdown selector | value, options, onSelect, label |
| `ToggleField` | Boolean toggle | value, onValueChange, label |
| `SliderField` | Range slider | value, onValueChange, min, max, label |
| `ChipGroup` | Multi-select chips | selected, options, onSelectionChange |

## 6.3 List Components

| Component | Usage | Props |
|-----------|-------|-------|
| `TransactionListItem` | Transaction in list | transaction, onClick, onLongClick |
| `AccountListItem` | Account in list | account, onClick, onLongClick |
| `CategoryListItem` | Category in list | category, onClick, onLongClick |
| `BudgetListItem` | Budget in list | budget, spent, onClick |
| `SectionHeader` | List section header | title, action |
| `SwipeableItem` | Swipe to reveal actions | content, actions |

---

# 7. NAVIGATION & MODALS

## 7.1 Bottom Navigation
| Tab | Icon | Screen | Badge |
|-----|------|--------|-------|
| Home | home | Dashboard | - |
| Transactions | receipt_long | Transactions | - |
| (FAB) | add | AddTransaction | - |
| Budgets | savings | Budgets | Count of exceeded |
| Reports | bar_chart | Reports | - |
| Settings | settings | Settings | Notification dot |

## 7.2 Common Modals/Bottom Sheets

| Modal | Trigger | Content |
|-------|---------|---------|
| AddTransactionSheet | FAB tap | Quick type selection |
| FilterSheet | Filter button | Advanced filters |
| SortSheet | Sort button | Sort options |
| CategoryPickerSheet | Category field | Category grid |
| AccountPickerSheet | Account field | Account list |
| DatePickerDialog | Date field | Calendar |
| TimePickerDialog | Time field | Clock |
| ConfirmDeleteDialog | Delete action | Confirmation |
| PremiumUpgradeSheet | Premium feature tap | Upgrade prompt |

## 7.3 Navigation Arguments

| Route | Arguments |
|-------|-----------|
| TransactionDetail | transactionId: String |
| EditTransaction | transactionId: String |
| AddTransaction | type: String (optional) |
| AccountDetail | accountId: String |
| EditAccount | accountId: String |
| BudgetDetail | budgetId: String |
| EditBudget | budgetId: String |
| EditCategory | categoryId: String |
| AddCategory | type: String |

---

# 8. DATA FLOW SUMMARY

## 8.1 Repository → Screen Mapping

| Screen | Primary Repositories |
|--------|---------------------|
| Dashboard | TransactionRepo, AccountRepo, BudgetRepo, CategoryRepo |
| Transactions | TransactionRepo, CategoryRepo, AccountRepo |
| AddTransaction | TransactionRepo, CategoryRepo, AccountRepo, RecurringRepo |
| Budgets | BudgetRepo, TransactionRepo, CategoryRepo |
| Reports | TransactionRepo, CategoryRepo, AccountRepo |
| Settings | UserRepo, SubscriptionRepo, All repos for data ops |
| Accounts | AccountRepo |
| Categories | CategoryRepo |

## 8.2 Real-time Updates (Flow)

| Data | Screens That Observe |
|------|---------------------|
| Transactions | Dashboard, Transactions, Reports, BudgetDetail |
| Accounts | Dashboard, Transactions, AddTransaction, Accounts |
| Categories | Dashboard, Transactions, AddTransaction, Categories, Budgets |
| Budgets | Dashboard, Budgets |
| UserPreferences | Settings, Theme (app-wide) |
| Subscription | Settings, Reports (premium features) |

---

# 9. THEME & STYLING NOTES

## 9.1 Color Semantics
| Color Use | Light Theme | Dark Theme |
|-----------|-------------|------------|
| Income | Green (#2ECC71) | Green (#2ECC71) |
| Expense | Red (#E74C3C) | Red (#E74C3C) |
| Transfer | Blue (#3498DB) | Blue (#3498DB) |
| Warning | Orange (#F39C12) | Orange (#F39C12) |
| Budget OK | Green | Green |
| Budget Warning | Yellow | Yellow |
| Budget Exceeded | Red | Red |

## 9.2 Typography Scale
| Style | Use |
|-------|-----|
| Display Large | Total balance on dashboard |
| Headline | Screen titles |
| Title Large | Card headers |
| Title Medium | List item primary text |
| Body Large | Main content |
| Body Medium | Secondary content |
| Label | Chips, badges |
| Caption | Timestamps, hints |

## 9.3 Spacing
| Size | Value | Use |
|------|-------|-----|
| xs | 4dp | Tight spacing |
| sm | 8dp | Related elements |
| md | 16dp | Standard spacing |
| lg | 24dp | Section spacing |
| xl | 32dp | Screen padding |

---

**Document Version**: 1.0  
**Last Updated**: December 8, 2025  
**Status**: Ready for UI Implementation

