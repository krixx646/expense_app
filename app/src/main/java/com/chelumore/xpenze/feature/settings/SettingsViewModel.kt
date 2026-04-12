package com.chelumore.xpenze.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.BuildConfig
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.core.work.DailySummaryScheduler
import com.chelumore.xpenze.core.work.RecurringRuleScheduler
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.core.work.WeeklySavingsAlertScheduler
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.SyncRepository
import com.chelumore.xpenze.domain.repository.SyncState
import com.chelumore.xpenze.domain.repository.ThemeMode
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val budgetAlertScheduler: BudgetAlertScheduler,
    private val dailySummaryScheduler: DailySummaryScheduler,
    private val weeklySavingsAlertScheduler: WeeklySavingsAlertScheduler,
    private val authRepository: AuthRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val appDatabase: AppDatabase,
    private val recurringRuleScheduler: RecurringRuleScheduler,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    sealed interface SettingsEvent {
        data class Message(val text: String) : SettingsEvent
        data object AccountDeleted : SettingsEvent
    }

    private val eventsChannel = Channel<SettingsEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    // Converting UserPreferences from DataStore to UI State
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesDataStore.userPreferences,
        getAccountsUseCase(includeArchived = true),
        syncRepository.syncState,
        premiumEntitlementTracker.entitlement
    ) { prefs, accounts, syncState, entitlement ->
        val defaultAccount = accounts.firstOrNull { it.id == prefs.defaultAccountId }
            ?: accounts.firstOrNull { it.isDefault }

        SettingsUiState(
            userName = prefs.userName ?: (prefs.userEmail ?: "Guest User"),
            userEmail = prefs.userEmail ?: "Not signed in",
            isSignedIn = !prefs.userId.isNullOrBlank(),
            userPhotoUrl = prefs.userPhotoUrl,
            isPremium = entitlement.isPremium,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            isTrialActive = entitlement.isTrialActive,
            trialExpiresAt = entitlement.trialExpiresAt,
            hasFounderReward = prefs.hasFounderReward,
            appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag),
            accountCardStyle = prefs.accountCardStyle,
            currency = prefs.defaultCurrency,
            defaultAccountId = prefs.defaultAccountId,
            defaultAccountName = defaultAccount?.name ?: "Not set",
            accounts = accounts,
            themeMode = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> com.chelumore.xpenze.feature.settings.ThemeMode.SYSTEM
                ThemeMode.LIGHT -> com.chelumore.xpenze.feature.settings.ThemeMode.LIGHT
                ThemeMode.DARK -> com.chelumore.xpenze.feature.settings.ThemeMode.DARK
            },
            notificationsEnabled = prefs.notificationsEnabled,
            budgetAlertsEnabled = prefs.budgetAlertEnabled,
            dailySummaryEnabled = prefs.dailySummaryEnabled,
            autoCaptureEnabled = prefs.autoCaptureEnabled,
            autoCaptureStatementImportEnabled = prefs.autoCaptureStatementImportEnabled,
            autoCaptureOcrEnabled = prefs.autoCaptureOcrEnabled,
            autoCaptureNotificationEnabled = prefs.autoCaptureNotificationEnabled,
            autoCaptureNotificationAllowedPackages = prefs.autoCaptureNotificationAllowedPackages,
            autoCaptureMerchantMemoryEnabled = prefs.autoCaptureMerchantMemoryEnabled,
            autoCaptureRecurringSuggestionsEnabled = prefs.autoCaptureRecurringSuggestionsEnabled,
            autoCaptureDisclosureAccepted = prefs.autoCaptureDisclosureAccepted,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            lastSyncTimeMillis = prefs.lastSyncTime,
            isSyncing = syncState is SyncState.Syncing,
            syncError = (syncState as? SyncState.Error)?.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            preferencesDataStore.setDefaultCurrency(currency)
        }
    }

    fun setAppLanguage(languageTag: String) {
        viewModelScope.launch {
            preferencesDataStore.setAppLanguageTag(
                AppLanguages.normalizeTag(languageTag)
            )
        }
    }

    fun setDefaultAccount(accountId: String?) {
        viewModelScope.launch {
            preferencesDataStore.setDefaultAccountId(accountId)
            if (!accountId.isNullOrBlank()) {
                accountRepository.setDefault(accountId)
            }
        }
    }

    fun setAccountCardStyle(style: AccountCardStyle) {
        viewModelScope.launch {
            preferencesDataStore.setAccountCardStyle(style)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            try {
                preferencesDataStore.setUserName(name.trim().ifBlank { null })
                eventsChannel.send(SettingsEvent.Message("Profile updated."))
            } catch (e: Exception) {
                eventsChannel.send(SettingsEvent.Message(e.message ?: "Failed to update profile"))
            }
        }
    }

    fun updateUserPhotoUrl(photoUrl: String?) {
        viewModelScope.launch {
            try {
                preferencesDataStore.setUserPhotoUrl(photoUrl)
                eventsChannel.send(SettingsEvent.Message("Profile photo updated."))
            } catch (e: Exception) {
                eventsChannel.send(SettingsEvent.Message(e.message ?: "Failed to update photo"))
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setNotificationsEnabled(enabled)
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.notificationsEnabled && prefs.budgetAlertEnabled) {
                budgetAlertScheduler.scheduleDaily()
                budgetAlertScheduler.scheduleNow()
            } else {
                budgetAlertScheduler.cancelAll()
            }

            if (prefs.notificationsEnabled && prefs.dailySummaryEnabled) {
                dailySummaryScheduler.scheduleDaily()
                dailySummaryScheduler.scheduleNow()
            } else {
                dailySummaryScheduler.cancelAll()
            }

            if (prefs.notificationsEnabled) {
                weeklySavingsAlertScheduler.scheduleWeekly()
            } else {
                weeklySavingsAlertScheduler.cancelAll()
            }
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setBudgetAlertEnabled(enabled)
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.notificationsEnabled && prefs.budgetAlertEnabled) {
                budgetAlertScheduler.scheduleDaily()
                budgetAlertScheduler.scheduleNow()
            } else {
                budgetAlertScheduler.cancelAll()
            }
        }
    }
    
    fun toggleDailySummary(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setDailySummaryEnabled(enabled)
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.notificationsEnabled && prefs.dailySummaryEnabled) {
                dailySummaryScheduler.scheduleDaily()
                dailySummaryScheduler.scheduleNow()
            } else {
                dailySummaryScheduler.cancelAll()
            }
        }
    }

    fun setAutoCaptureDisclosureAccepted() {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureDisclosureAccepted(true)
        }
    }

    fun toggleAutoCapture(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureEnabled(enabled)
        }
    }

    fun toggleAutoCaptureStatementImport(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureStatementImportEnabled(enabled)
        }
    }

    fun toggleAutoCaptureOcr(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureOcrEnabled(enabled)
        }
    }

    fun toggleAutoCaptureNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureNotificationEnabled(enabled)
        }
    }

    fun setAutoCaptureNotificationAllowedPackages(packageNames: Set<String>) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureNotificationAllowedPackages(packageNames)
        }
    }

    fun toggleAutoCaptureMerchantMemory(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureMerchantMemoryEnabled(enabled)
        }
    }

    fun toggleAutoCaptureRecurringSuggestions(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureRecurringSuggestionsEnabled(enabled)
        }
    }

    fun setTheme(mode: com.chelumore.xpenze.feature.settings.ThemeMode) {
        viewModelScope.launch {
            val domainMode = when(mode) {
                com.chelumore.xpenze.feature.settings.ThemeMode.SYSTEM -> ThemeMode.SYSTEM
                com.chelumore.xpenze.feature.settings.ThemeMode.LIGHT -> ThemeMode.LIGHT
                com.chelumore.xpenze.feature.settings.ThemeMode.DARK -> ThemeMode.DARK
            }
            preferencesDataStore.setThemeMode(domainMode)
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.userId.isNullOrBlank()) {
                eventsChannel.send(SettingsEvent.Message("Sign in to enable cloud sync."))
                return@launch
            }
            // Run immediately (also scheduled periodically via SyncScheduler).
            syncScheduler.scheduleNow()
            eventsChannel.send(SettingsEvent.Message("Sync started."))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                budgetAlertScheduler.cancelAll()
                dailySummaryScheduler.cancelAll()
                weeklySavingsAlertScheduler.cancelAll()
                recurringRuleScheduler.cancelAll()

                appDatabase.clearAllTables()

                // Reset user/session-related prefs + onboarding so app can start fresh.
                preferencesDataStore.clearUserData()
                preferencesDataStore.setOnboardingComplete(false)
                preferencesDataStore.setDefaultAccountId(null)
                preferencesDataStore.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
                preferencesDataStore.setPremium(false)
                preferencesDataStore.updateLastSyncTime(0L)

                eventsChannel.send(SettingsEvent.Message("All local data cleared."))
            } catch (e: Exception) {
                eventsChannel.send(SettingsEvent.Message(e.message ?: "Failed to clear data"))
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                authRepository.deleteAccount()
                // Also clear local data
                budgetAlertScheduler.cancelAll()
                dailySummaryScheduler.cancelAll()
                weeklySavingsAlertScheduler.cancelAll()
                recurringRuleScheduler.cancelAll()
                appDatabase.clearAllTables()
                preferencesDataStore.clearUserData()
                preferencesDataStore.setOnboardingComplete(false)
                preferencesDataStore.setDefaultAccountId(null)
                preferencesDataStore.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
                preferencesDataStore.setPremium(false)
                preferencesDataStore.updateLastSyncTime(0L)

                eventsChannel.send(SettingsEvent.AccountDeleted)
            } catch (e: Exception) {
                eventsChannel.send(SettingsEvent.Message(e.message ?: "Failed to delete account"))
            }
        }
    }
}

