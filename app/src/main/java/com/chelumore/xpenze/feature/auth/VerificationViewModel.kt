package com.chelumore.xpenze.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false,
    val email: String? = null,
    val infoMessage: String? = null
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VerificationUiState(email = authRepository.getCurrentUser()?.email)
    )
    val uiState: StateFlow<VerificationUiState> = _uiState

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
                authRepository.sendEmailVerification()
                _uiState.update { it.copy(isLoading = false, infoMessage = "Verification email sent.") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toUserFriendlyAuthMessage(
                            "Unable to resend the verification email right now. Please try again."
                        )
                    )
                }
            }
        }
    }

    fun checkVerified() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
                val user = authRepository.reloadCurrentUser()
                val verified = user?.isEmailVerified == true
                if (verified) {
                    _uiState.update { it.copy(isLoading = false, isVerified = true, email = user?.email) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Not verified yet. Please open the link in your email, then try again.",
                            email = user?.email
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.toUserFriendlyAuthMessage(
                            "Unable to check verification status right now. Please try again."
                        )
                    )
                }
            }
        }
    }

    /**
     * Lightweight refresh used on screen resume.
     * Only navigates forward when verified; doesn't show an error toast/banner when not verified yet.
     */
    fun refreshOnResume() {
        viewModelScope.launch {
            runCatching { authRepository.reloadCurrentUser() }
                .onSuccess { user ->
                    if (user?.isEmailVerified == true) {
                        _uiState.update { it.copy(isVerified = true, email = user.email, error = null, infoMessage = null) }
                    } else if (user?.email != null) {
                        _uiState.update { it.copy(email = user.email) }
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, infoMessage = null) }
    }

    fun consumeVerified() {
        _uiState.update { it.copy(isVerified = false) }
    }
}


