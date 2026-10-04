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
    // Counts successful "value" actions (e.g. creating a transaction or budget) used to
    // avoid asking for a rating before the user has done something useful.
    val rateEligibleActionCount: Int = 0,
    val rateAppPromptLastShownAt: Long = 0L,
    val hasRatedOnPlayStore: Boolean = false,
    val lastSyncTime: Long = 0L,
    val userId: String? = null,
    // Set when the user chose to use the app without an account. Local-only until they sign in.
    val isGuestMode: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val userPhotoUrl: String? = null,
    val appLanguageTag: String = "en",
    val accountCardStyle: AccountCardStyle = AccountCardStyle.MATTE,
    val accountCardStyleOverrides: Map<String, AccountCardStyle> = emptyMap(),
    val accountCardStyleUpdatedAt: Long = 0L,
    val areBalancesHidden: Boolean = false,
    val hasSeenAiCoachCta: Boolean = false,
    val hasSeenAutoCaptureTutorial: Boolean = false,
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
    // Guests / signed-out sessions never inherit leftover trial access from a prior login.
    if (userId.isNullOrBlank() || isGuestMode) return false
    return trialConsumed && trialExpiresAt > nowMillis
}

fun UserPreferences.hasPremiumAccess(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return isPremium || isTrialActive(nowMillis)
}

/** True when an active trial has at most [windowMs] remaining (default: 2 days). */
fun UserPreferences.isTrialEndingSoon(
    nowMillis: Long = System.currentTimeMillis(),
    windowMs: Long = TRIAL_ENDING_SOON_WINDOW_MS
): Boolean {
    if (!isTrialActive(nowMillis)) return false
    val remaining = trialExpiresAt - nowMillis
    return remaining in 1L..windowMs
}

const val TRIAL_ENDING_SOON_WINDOW_MS: Long = 2L * 24L * 60L * 60L * 1000L

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

