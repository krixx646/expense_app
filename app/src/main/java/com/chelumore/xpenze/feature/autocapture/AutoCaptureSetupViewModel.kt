package com.chelumore.xpenze.feature.autocapture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutoCaptureSetupUiState(
    val appLanguageTag: String = "en",
    val hasPremiumAccess: Boolean = false,
    val autoCaptureEnabled: Boolean = false,
    val autoCaptureNotificationEnabled: Boolean = false,
    val autoCaptureNotificationAllowedPackages: Set<String> = emptySet()
)

sealed interface AutoCaptureSetupEvent {
    data object Enabled : AutoCaptureSetupEvent
}

@HiltViewModel
class AutoCaptureSetupViewModel @Inject constructor(
    userRepository: UserRepository,
    private val preferencesDataStore: PreferencesDataStore,
    premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private val eventsChannel = Channel<AutoCaptureSetupEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<AutoCaptureSetupUiState> = combine(
        userRepository.userPreferences,
        premiumEntitlementTracker.entitlement
    ) { prefs, entitlement ->
        AutoCaptureSetupUiState(
            appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag),
            hasPremiumAccess = entitlement.hasPremiumAccess,
            autoCaptureEnabled = prefs.autoCaptureEnabled,
            autoCaptureNotificationEnabled = prefs.autoCaptureNotificationEnabled,
            autoCaptureNotificationAllowedPackages = prefs.autoCaptureNotificationAllowedPackages
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutoCaptureSetupUiState()
    )

    fun enableAutomaticMode() {
        viewModelScope.launch {
            if (!uiState.value.hasPremiumAccess) return@launch

            preferencesDataStore.setAutoCaptureDisclosureAccepted(true)
            preferencesDataStore.setAutoCaptureEnabled(true)
            preferencesDataStore.setAutoCaptureStatementImportEnabled(true)
            preferencesDataStore.setAutoCaptureOcrEnabled(true)
            preferencesDataStore.setAutoCaptureMerchantMemoryEnabled(true)
            preferencesDataStore.setAutoCaptureRecurringSuggestionsEnabled(true)

            eventsChannel.send(AutoCaptureSetupEvent.Enabled)
        }
    }

    fun toggleAutoCaptureNotification(enabled: Boolean) {
        viewModelScope.launch {
            if (!uiState.value.hasPremiumAccess) return@launch
            preferencesDataStore.setAutoCaptureNotificationEnabled(enabled)
        }
    }

    fun setAutoCaptureNotificationAllowedPackages(packageNames: Set<String>) {
        viewModelScope.launch {
            if (!uiState.value.hasPremiumAccess) return@launch
            preferencesDataStore.setAutoCaptureNotificationAllowedPackages(packageNames)
        }
    }
}
