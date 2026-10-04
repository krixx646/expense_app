package com.chelumore.xpenze.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isStartingGuestSession: Boolean = false,
    val guestSessionDestination: GuestSessionDestination? = null
)

enum class GuestSessionDestination {
    Onboarding,
    Main
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker
) : ViewModel() {

    fun onSignUpSelected() {
        growthAnalyticsTracker.logWelcomeChoice(GrowthAnalyticsTracker.WELCOME_CHOICE_SIGN_UP)
    }

    fun onLogInSelected() {
        growthAnalyticsTracker.logWelcomeChoice(GrowthAnalyticsTracker.WELCOME_CHOICE_LOG_IN)
    }

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState

    fun continueWithoutAccount() {
        if (_uiState.value.isStartingGuestSession) return
        _uiState.value = WelcomeUiState(isStartingGuestSession = true)
        growthAnalyticsTracker.logWelcomeChoice(GrowthAnalyticsTracker.WELCOME_CHOICE_GUEST)
        viewModelScope.launch {
            // Entering guest mode strips any leftover Premium/trial entitlement.
            userRepository.clearTrialState()
            userRepository.setPremium(false)
            userRepository.setGuestMode(true)
            val prefs = userRepository.userPreferences.first()
            _uiState.value = WelcomeUiState(
                isStartingGuestSession = false,
                guestSessionDestination = if (prefs.isOnboardingComplete) {
                    GuestSessionDestination.Main
                } else {
                    GuestSessionDestination.Onboarding
                }
            )
        }
    }

    fun onGuestSessionNavigated() {
        _uiState.value = _uiState.value.copy(guestSessionDestination = null)
    }
}
