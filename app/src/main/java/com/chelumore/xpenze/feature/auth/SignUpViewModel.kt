package com.chelumore.xpenze.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker.Companion.AUTH_FLOW_SIGN_UP
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker.Companion.AUTH_METHOD_EMAIL
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker.Companion.AUTH_METHOD_GOOGLE
import com.chelumore.xpenze.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    // Email sign-up always sends a verification link; Google accounts arrive already verified.
    val requiresVerification: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            try {
                if (name.trim().isBlank()) {
                    _uiState.update { it.copy(error = "Name is required") }
                    return@launch
                }
                if (password != confirmPassword) {
                    _uiState.update { it.copy(error = "Passwords do not match") }
                    return@launch
                }
                _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
                authRepository.signUp(
                    email = email,
                    password = password,
                    displayName = name.trim()
                )
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_SIGN_UP, AUTH_METHOD_EMAIL, success = true)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, requiresVerification = true)
                }
            } catch (e: Exception) {
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_SIGN_UP, AUTH_METHOD_EMAIL, success = false, error = e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toUserFriendlyAuthMessage("Unable to create your account right now. Please try again.")
                    )
                }
            }
        }
    }

    fun signUpWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
                authRepository.signInWithGoogle(idToken)
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_SIGN_UP, AUTH_METHOD_GOOGLE, success = true)
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, requiresVerification = false)
                }
            } catch (e: Exception) {
                growthAnalyticsTracker.logAuthResult(AUTH_FLOW_SIGN_UP, AUTH_METHOD_GOOGLE, success = false, error = e)
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

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false, requiresVerification = false) }
    }
}


