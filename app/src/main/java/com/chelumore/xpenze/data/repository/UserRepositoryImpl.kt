package com.chelumore.xpenze.data.repository

import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.ThemeMode
import com.chelumore.xpenze.domain.repository.UserPreferences
import com.chelumore.xpenze.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : UserRepository {

    override val userPreferences: Flow<UserPreferences> = preferencesDataStore.userPreferences

    override val isLoggedIn: Flow<Boolean> = preferencesDataStore.isLoggedIn

    override val isPremium: Flow<Boolean> = preferencesDataStore.isPremium

    override val aiChatHistoryJson: Flow<String?> = preferencesDataStore.aiChatHistoryJson

    override suspend fun setOnboardingComplete(complete: Boolean) {
        preferencesDataStore.setOnboardingComplete(complete)
    }

    override suspend fun setAppLanguageTag(languageTag: String) {
        preferencesDataStore.setAppLanguageTag(languageTag)
    }

    override suspend fun setAccountCardStyle(style: AccountCardStyle) {
        preferencesDataStore.setAccountCardStyle(style)
    }

    override suspend fun setAccountCardStyleOverride(accountId: String, style: AccountCardStyle?) {
        preferencesDataStore.setAccountCardStyleOverride(accountId, style)
    }

    override suspend fun setBalancesHidden(hidden: Boolean) {
        preferencesDataStore.setBalancesHidden(hidden)
    }

    override suspend fun setDefaultCurrency(currency: String) {
        preferencesDataStore.setDefaultCurrency(currency)
    }

    override suspend fun setDefaultAccountId(accountId: String?) {
        preferencesDataStore.setDefaultAccountId(accountId)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferencesDataStore.setThemeMode(mode)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesDataStore.setNotificationsEnabled(enabled)
    }

    override suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        preferencesDataStore.setBudgetAlertEnabled(enabled)
    }

    override suspend fun setNotificationPermissionPromptLastShownAt(timestampMillis: Long) {
        preferencesDataStore.setNotificationPermissionPromptLastShownAt(timestampMillis)
    }

    override suspend fun incrementAppOpenCount() {
        preferencesDataStore.incrementAppOpenCount()
    }

    override suspend fun incrementRateEligibleActionCount() {
        preferencesDataStore.incrementRateEligibleActionCount()
    }

    override suspend fun setRateAppPromptLastShownAt(timestampMillis: Long) {
        preferencesDataStore.setRateAppPromptLastShownAt(timestampMillis)
    }

    override suspend fun setHasRatedOnPlayStore(rated: Boolean) {
        preferencesDataStore.setHasRatedOnPlayStore(rated)
    }

    override suspend fun updateLastSyncTime(time: Long) {
        preferencesDataStore.updateLastSyncTime(time)
    }

    override suspend fun setGuestMode(enabled: Boolean) {
        preferencesDataStore.setGuestMode(enabled)
    }

    override suspend fun enterGuestMode() {
        preferencesDataStore.enterGuestMode()
    }

    override suspend fun promoteGuestToUser(
        userId: String,
        email: String?,
        name: String?,
        photoUrl: String?
    ) {
        preferencesDataStore.promoteGuestToUser(
            userId = userId,
            email = email,
            name = name,
            photoUrl = photoUrl
        )
    }

    override suspend fun setUserInfo(userId: String?, email: String?, name: String?, photoUrl: String?) {
        preferencesDataStore.setUserInfo(userId, email, name, photoUrl)
    }

    override suspend fun setAiCoachCtaSeen(seen: Boolean) {
        preferencesDataStore.setAiCoachCtaSeen(seen)
    }

    override suspend fun setAutoCaptureTutorialSeen(seen: Boolean) {
        preferencesDataStore.setAutoCaptureTutorialSeen(seen)
    }

    override suspend fun setPremium(isPremium: Boolean) {
        preferencesDataStore.setPremium(isPremium)
    }

    override suspend fun setAiChatHistoryJson(historyJson: String?) {
        preferencesDataStore.setAiChatHistoryJson(historyJson)
    }

    override suspend fun setTrialState(startedAt: Long, expiresAt: Long, consumed: Boolean) {
        preferencesDataStore.setTrialState(startedAt, expiresAt, consumed)
    }

    override suspend fun clearTrialState() {
        preferencesDataStore.clearTrialState()
    }

    override suspend fun setFounderRewardState(
        hasFounderReward: Boolean,
        founderTrialActivated: Boolean,
        founderRewardProductId: String?
    ) {
        preferencesDataStore.setFounderRewardState(
            hasFounderReward = hasFounderReward,
            founderTrialActivated = founderTrialActivated,
            founderRewardProductId = founderRewardProductId
        )
    }

    override suspend fun clearFounderRewardState() {
        preferencesDataStore.clearFounderRewardState()
    }

    override suspend fun clearUserData() {
        preferencesDataStore.clearUserData()
    }
}

