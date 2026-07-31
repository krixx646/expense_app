package com.chelumore.xpenze.core.common

object Constants {
    // Database
    const val DATABASE_NAME = "xpenze_database"
    const val DATABASE_VERSION = 3

    // Preferences
    const val PREFERENCES_NAME = "xpenze_preferences"

    // Date Formats
    const val DATE_FORMAT_DISPLAY = "MMM dd, yyyy"
    const val DATE_FORMAT_MONTH_YEAR = "MMMM yyyy"
    const val DATE_FORMAT_ISO = "yyyy-MM-dd"
    const val TIME_FORMAT_DISPLAY = "HH:mm"
    const val DATETIME_FORMAT_DISPLAY = "MMM dd, yyyy HH:mm"

    // Currency
    const val DEFAULT_CURRENCY = "USD"

    // Sync
    const val SYNC_INTERVAL_MINUTES = 15L
    const val SYNC_FLEX_MINUTES = 5L

    // Pagination
    const val PAGE_SIZE = 20

    // Budget Alert
    const val DEFAULT_BUDGET_ALERT_THRESHOLD = 80

    // Subscription Product IDs
    const val SUBSCRIPTION_MONTHLY = "xpenze_premium_monthly"
    const val SUBSCRIPTION_YEARLY = "xpenze_premium_yearly"
    const val FOUNDER_REWARD_PRODUCT = "xpenze_preregister_founder_pack"
    const val FOUNDER_REWARD_TOTAL_TRIAL_DAYS = 14

    // Shown only until live Play Store pricing loads; keep in sync with Play Console.
    const val DEFAULT_MONTHLY_PRICE_CENTS = 399L
}
