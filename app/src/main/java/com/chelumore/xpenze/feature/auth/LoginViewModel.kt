package com.chelumore.xpenze.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker.Companion.AUTH_FLOW_LOG_IN
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker.Companion.AUTH_METHOD_EMAIL
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker.Companion.AUTH_METHOD_GOOGLE
import com.chelumore.xpenze.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val needsEmailVerification: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false, needsEmailVerification = false) }
                val user = authRepository.signIn(email = email, password = password)
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_LOG_IN, AUTH_METHOD_EMAIL, success = true)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = user.isEmailVerified,
                        needsEmailVerification = !user.isEmailVerified
                    )
                }
            } catch (e: Exception) {
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_LOG_IN, AUTH_METHOD_EMAIL, success = false, error = e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toUserFriendlyAuthMessage("Unable to sign in right now. Please try again.")
                    )
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false, needsEmailVerification = false) }
                val user = authRepository.signInWithGoogle(idToken)
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_LOG_IN, AUTH_METHOD_GOOGLE, success = true)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = user.isEmailVerified,
                        needsEmailVerification = !user.isEmailVerified
                    )
                }
            } catch (e: Exception) {
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_LOG_IN, AUTH_METHOD_GOOGLE, success = false, error = e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toUserFriendlyAuthMessage("Google sign-in couldn't be completed. Please try again.")
                    )
                }
            }
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(isSuccess = false, needsEmailVerification = false) }
    }
}


