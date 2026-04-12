package com.chelumore.xpenze.domain.repository

data class UserPreferences(
    val isOnboardingComplete: Boolean = false,
    val defaultCurrency: String = "USD",
    val defaultAccountId: String? = null,
    // Default to DARK to match the app's brand theme. Users can still switch to SYSTEM/LIGHT in Settings.
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,
    val budgetAlertEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,
    val notificationPermissionPromptLastShownAt: Long = 0L,
    val appOpenCount: Int = 0,
    val rateAppPromptLastShownAt: Long = 0L,
    val hasRatedOnPlayStore: Boolean = false,
    val lastSyncTime: Long = 0L,
    val userId: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
    val userPhotoUrl: String? = null,
    val appLanguageTag: String = "en",
    val accountCardStyle: AccountCardStyle = AccountCardStyle.MATTE,
    val accountCardStyleOverrides: Map<String, AccountCardStyle> = emptyMap(),
    val accountCardStyleUpdatedAt: Long = 0L,
    val areBalancesHidden: Boolean = false,
    val hasSeenAiCoachCta: Boolean = false,
    // Paid premium entitlement (separate from free trial).
    val isPremium: Boolean = false,
    val trialStartedAt: Long = 0L,
    val trialExpiresAt: Long = 0L,
    val trialConsumed: Boolean = false,
    val hasFounderReward: Boolean = false,
    val founderTrialActivated: Boolean = false,
    val founderRewardProductId: String? = null,
    val autoCaptureEnabled: Boolean = false,
    val autoCaptureStatementImportEnabled: Boolean = false,
    val autoCaptureOcrEnabled: Boolean = false,
    val autoCaptureNotificationEnabled: Boolean = false,
    val autoCaptureNotificationAllowedPackages: Set<String> = emptySet(),
    val autoCaptureMerchantMemoryEnabled: Boolean = false,
    val autoCaptureRecurringSuggestionsEnabled: Boolean = false,
    val autoCaptureDisclosureAccepted: Boolean = false
)

fun UserPreferences.isTrialActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return trialConsumed && trialExpiresAt > nowMillis
}

fun UserPreferences.hasPremiumAccess(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return isPremium || isTrialActive(nowMillis)
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AccountCardStyle {
    MATTE,
    METALLIC,
    NEON,
    SUNSET,
    OCEAN,
    ROYAL
}

