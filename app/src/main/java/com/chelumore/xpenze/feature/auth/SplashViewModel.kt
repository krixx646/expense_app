package com.chelumore.xpenze.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.SyncRepository
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.core.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val destination: SplashDestination? = null,
    val error: String? = null
)

sealed interface SplashDestination {
    data object Welcome : SplashDestination
    data object Verification : SplashDestination
    data object Onboarding : SplashDestination
    data object Main : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val syncRepository: SyncRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val syncScheduler: SyncScheduler,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        // Start/stop cloud sync based on auth state.
        viewModelScope.launch {
            authRepository.observeAuthState()
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid.isNullOrBlank()) {
                        syncScheduler.cancelAll()
                    } else {
                        syncScheduler.schedulePeriodic()
                        // Pull cloud data immediately after sign-in/account switch.
                        syncScheduler.scheduleNow()
                        runCatching { subscriptionRepository.syncTrialStateFromBackend() }
                        runCatching { subscriptionRepository.syncPremiumStateFromBackend() }
                        runCatching { subscriptionRepository.restorePurchases() }
                    }
                }
        }

        viewModelScope.launch {
            combine(
                authRepository.observeAuthState(),
                userRepository.userPreferences
            ) { user, prefs -> user to prefs }
                .distinctUntilChanged()
                .collectLatest { (user, prefs) ->
                    // Keep local preferences in sync (covers "already signed in" sessions on app start).
                    if (user == null && !prefs.userId.isNullOrBlank()) {
                        withContext(Dispatchers.IO) {
                            appDatabase.clearAllTables()
                        }
                        userRepository.clearUserData()
                        userRepository.setOnboardingComplete(false)
                        userRepository.setDefaultAccountId(null)
                        userRepository.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
                        userRepository.updateLastSyncTime(0L)
                        userRepository.setPremium(false)
                        userRepository.clearTrialState()
                        return@collectLatest
                    } else if (user != null && prefs.userId != user.uid) {
                        withContext(Dispatchers.IO) {
                            appDatabase.clearAllTables()
                        }
                        userRepository.setOnboardingComplete(false)
                        userRepository.setDefaultAccountId(null)
                        userRepository.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
                        userRepository.updateLastSyncTime(0L)
                        userRepository.setPremium(false)
                        userRepository.clearTrialState()
                        userRepository.setUserInfo(
                            userId = user.uid,
                            email = user.email,
                            name = user.displayName,
                            photoUrl = user.photoUrl
                        )
                        return@collectLatest
                    }

                    when {
                        user == null -> {
                            _uiState.update { it.copy(isLoading = false, destination = SplashDestination.Welcome, error = null) }
                        }
                        user.email != null && !user.isEmailVerified -> {
                            _uiState.update { it.copy(isLoading = false, destination = SplashDestination.Verification, error = null) }
                        }
                        prefs.isOnboardingComplete -> {
                            _uiState.update { it.copy(isLoading = false, destination = SplashDestination.Main, error = null) }
                        }
                        else -> {
                            // New install on another device: prefer pulling existing cloud accounts over forcing a new onboarding account.
                            _uiState.update { it.copy(isLoading = true, destination = null, error = null) }

                            // Immediate bootstrap sync for signed-in users so cross-device login
                            // can see existing cloud accounts before we decide onboarding vs main.
                            withTimeoutOrNull(FIRST_BOOTSTRAP_SYNC_TIMEOUT_MS) {
                                runCatching { syncRepository.fullSync() }
                            }

                            val existingAccounts = accountRepository.getAll().first()
                            val hasAccounts = if (existingAccounts.isNotEmpty()) {
                                true
                            } else {
                                withTimeoutOrNull(ACCOUNT_APPEAR_WAIT_TIMEOUT_MS) {
                                    accountRepository.getAll()
                                        .map { it.isNotEmpty() }
                                        .filter { it }
                                        .first()
                                } != null
                            }

                            if (hasAccounts) {
                                accountRepository.getAll()
                                    .filter { it.isNotEmpty() }
                                    .first()
                            } else {
                                emptyList()
                            }.let { accounts ->
                                if (accounts.isNotEmpty()) {
                                val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.first()
                                userRepository.setDefaultCurrency(defaultAccount.currency)
                                userRepository.setDefaultAccountId(defaultAccount.id)
                                userRepository.setOnboardingComplete(true)

                                _uiState.update { it.copy(isLoading = false, destination = SplashDestination.Main, error = null) }
                            } else {
                                _uiState.update { it.copy(isLoading = false, destination = SplashDestination.Onboarding, error = null) }
                            }
                            }
                        }
                    }
                }
        }
    }

    private companion object {
        const val FIRST_BOOTSTRAP_SYNC_TIMEOUT_MS = 8_000L
        const val ACCOUNT_APPEAR_WAIT_TIMEOUT_MS = 2_000L
    }
}





