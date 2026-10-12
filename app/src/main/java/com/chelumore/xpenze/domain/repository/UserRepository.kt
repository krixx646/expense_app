package com.chelumore.xpenze.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userPreferences: Flow<UserPreferences>
    val isLoggedIn: Flow<Boolean>
    val isPremium: Flow<Boolean>
    val aiChatHistoryJson: Flow<String?>
    
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setAppLanguageTag(languageTag: String)
    suspend fun setAccountCardStyle(style: AccountCardStyle)
    suspend fun setAccountCardStyleOverride(accountId: String, style: AccountCardStyle?)
    suspend fun setBalancesHidden(hidden: Boolean)
    suspend fun setDefaultCurrency(currency: String)
    suspend fun setDefaultAccountId(accountId: String?)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setBudgetAlertEnabled(enabled: Boolean)
    suspend fun setNotificationPermissionPromptLastShownAt(timestampMillis: Long)
    suspend fun incrementAppOpenCount()
    suspend fun incrementRateEligibleActionCount()
    suspend fun setRateAppPromptLastShownAt(timestampMillis: Long)
    suspend fun setHasRatedOnPlayStore(rated: Boolean)
    suspend fun updateLastSyncTime(time: Long)
    suspend fun setGuestMode(enabled: Boolean)
    /** Atomically enable guest mode and clear trial/premium/auto-capture leftovers. */
    suspend fun enterGuestMode()
    suspend fun promoteGuestToUser(userId: String, email: String?, name: String?, photoUrl: String?)
    suspend fun setUserInfo(userId: String?, email: String?, name: String?, photoUrl: String?)
    suspend fun setAiCoachCtaSeen(seen: Boolean)
    suspend fun setAutoCaptureTutorialSeen(seen: Boolean)
    suspend fun setPremium(isPremium: Boolean)
    suspend fun setAiChatHistoryJson(historyJson: String?)
    suspend fun setTrialState(startedAt: Long, expiresAt: Long, consumed: Boolean)
    suspend fun clearTrialState()
    suspend fun setFounderRewardState(
        hasFounderReward: Boolean,
        founderTrialActivated: Boolean,
        founderRewardProductId: String?
    )
    suspend fun clearFounderRewardState()
    suspend fun clearUserData()
}

