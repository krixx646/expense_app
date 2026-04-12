package com.chelumore.xpenze.feature.settings

import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.repository.AccountCardStyle

data class SettingsUiState(
    // Profile
    val userName: String = "Guest",
    val userEmail: String = "",
    val isSignedIn: Boolean = false,
    val userPhotoUrl: String? = null,
    val isPremium: Boolean = false,
    val hasPremiumAccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialExpiresAt: Long = 0L,
    val hasFounderReward: Boolean = false,
    
    // Preferences
    val appLanguageTag: String = "en",
    val accountCardStyle: AccountCardStyle = AccountCardStyle.MATTE,
    val currency: String = Constants.DEFAULT_CURRENCY,
    val defaultAccountId: String? = null,
    val defaultAccountName: String = "Not set",
    val accounts: List<Account> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    
    // Notifications
    val notificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,

    // Auto-capture
    val autoCaptureEnabled: Boolean = false,
    val autoCaptureStatementImportEnabled: Boolean = false,
    val autoCaptureOcrEnabled: Boolean = false,
    val autoCaptureNotificationEnabled: Boolean = false,
    val autoCaptureNotificationAllowedPackages: Set<String> = emptySet(),
    val autoCaptureMerchantMemoryEnabled: Boolean = false,
    val autoCaptureRecurringSuggestionsEnabled: Boolean = false,
    val autoCaptureDisclosureAccepted: Boolean = false,
    
    // App Info
    val appVersion: String = "",

    // Cloud sync
    val lastSyncTimeMillis: Long = 0L,
    val isSyncing: Boolean = false,
    val syncError: String? = null
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

