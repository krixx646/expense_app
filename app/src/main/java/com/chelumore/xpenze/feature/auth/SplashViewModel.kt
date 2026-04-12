package com.chelumore.xpenze.feature.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.SyncRepository
import com.chelumore.xpenze.domain.repository.SyncState
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.UserPreferences
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.domain.model.Account
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val destination: SplashDestination? = null,
    val error: SplashError? = null,
    val errorMessage: String? = null
)

sealed interface SplashDestination {
    data object Welcome : SplashDestination
    data object Verification : SplashDestination
    data object Onboarding : SplashDestination
    data object Main : SplashDestination
}

enum class SplashError {
    RESTORE_FAILED
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
    private val refreshRequests = MutableStateFlow(0)

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
                    }
                }
        }

        viewModelScope.launch {
            combine(
                authRepository.observeAuthState(),
                refreshRequests
            ) { user, refreshKey -> user to refreshKey }
                .distinctUntilChanged()
                .collectLatest { (user, _) ->
                    var prefs = userRepository.userPreferences.first()
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
                        prefs = userRepository.userPreferences.first()
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
                        prefs = userRepository.userPreferences.first()
                    }

                    when {
                        user == null -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    destination = SplashDestination.Welcome,
                                    error = null,
                                    errorMessage = null
                                )
                            }
                        }
                        user.email != null && !user.isEmailVerified -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    destination = SplashDestination.Verification,
                                    error = null,
                                    errorMessage = null
                                )
                            }
                        }
                        else -> {
                            resolveSignedInDestination(prefs)
                        }
                    }
                }
        }
    }

    fun retryRestore() {
        refreshRequests.update { it + 1 }
    }

    private suspend fun resolveSignedInDestination(prefs: UserPreferences) {
        _uiState.update { it.copy(isLoading = true, destination = null, error = null, errorMessage = null) }

        val restoreResult = attemptRequiredStartupSync()
        if (!restoreResult.syncCompleted) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    destination = null,
                    error = SplashError.RESTORE_FAILED,
                    errorMessage = restoreResult.errorMessage
                )
            }
            return
        }

        if (restoreResult.accounts.isNotEmpty()) {
            finalizeMainDestination(restoreResult.accounts, prefs)
            return
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                destination = if (prefs.isOnboardingComplete) {
                    SplashDestination.Main
                } else {
                    SplashDestination.Onboarding
                },
                error = null,
                errorMessage = null
            )
        }
    }

    private suspend fun attemptRequiredStartupSync(): BootstrapRestoreResult {
        var syncOutcome = SyncAttemptResult(
            completed = false,
            errorMessage = "Restore could not complete."
        )
        for (attemptIndex in 0 until MAX_REQUIRED_STARTUP_SYNC_ATTEMPTS) {
            syncOutcome = runStartupSyncAttempt()
            if (syncOutcome.completed) {
                break
            }
            val hasMoreAttempts = attemptIndex < MAX_REQUIRED_STARTUP_SYNC_ATTEMPTS - 1
            if (hasMoreAttempts) {
                delay(AUTOMATIC_RETRY_DELAY_MS * (attemptIndex + 1L))
            }
        }

        val accounts = accountRepository.getAll().first()
            .takeIf { it.isNotEmpty() }
            ?: withTimeoutOrNull(ACCOUNT_APPEAR_WAIT_TIMEOUT_MS) {
                accountRepository.getAll()
                    .filter { it.isNotEmpty() }
                    .first()
            }
            ?: emptyList()

        return BootstrapRestoreResult(
            syncCompleted = syncOutcome.completed,
            accounts = if (syncOutcome.completed) accounts else emptyList(),
            errorMessage = syncOutcome.errorMessage
        )
    }

    private suspend fun finalizeMainDestination(
        accounts: List<Account>,
        prefs: UserPreferences
    ) {
        syncDefaultAccountSelection(accounts, prefs)
        if (!prefs.isOnboardingComplete) {
            userRepository.setOnboardingComplete(true)
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                destination = SplashDestination.Main,
                error = null,
                errorMessage = null
            )
        }
        refreshSubscriptionStateInBackground()
    }

    private suspend fun syncDefaultAccountSelection(
        accounts: List<Account>,
        prefs: UserPreferences
    ) {
        val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull() ?: return
        if (prefs.defaultCurrency != defaultAccount.currency) {
            userRepository.setDefaultCurrency(defaultAccount.currency)
        }
        if (prefs.defaultAccountId != defaultAccount.id) {
            userRepository.setDefaultAccountId(defaultAccount.id)
        }
    }

    private data class BootstrapRestoreResult(
        val syncCompleted: Boolean,
        val accounts: List<Account>,
        val errorMessage: String? = null
    )

    private data class SyncAttemptResult(
        val completed: Boolean,
        val errorMessage: String? = null,
        val timedOut: Boolean = false
    )

    private suspend fun runStartupSyncAttempt(): SyncAttemptResult {
        return withTimeoutOrNull(REQUIRED_STARTUP_SYNC_TIMEOUT_MS) {
            val currentState = syncRepository.syncState.first()
            val finalSyncState = when (currentState) {
                is SyncState.Syncing -> {
                    syncRepository.syncState
                        .filter { it !is SyncState.Syncing }
                        .first()
                }
                is SyncState.Success -> currentState
                else -> {
                    val fullSyncResult = runCatching { syncRepository.fullSync() }
                    if (fullSyncResult.isFailure) {
                        return@withTimeoutOrNull SyncAttemptResult(
                            completed = false,
                            errorMessage = fullSyncResult.exceptionOrNull()?.message
                        )
                    }
                    syncRepository.syncState.first()
                }
            }

            when (finalSyncState) {
                is SyncState.Success -> SyncAttemptResult(completed = true)
                is SyncState.Error -> SyncAttemptResult(
                    completed = false,
                    errorMessage = finalSyncState.message
                )
                else -> SyncAttemptResult(completed = false)
            }
        } ?: SyncAttemptResult(
            completed = false,
            errorMessage = "Sync is taking too long. Check your connection and try again.",
            timedOut = true
        )
    }

    private fun refreshSubscriptionStateInBackground() {
        viewModelScope.launch {
            runCatching { subscriptionRepository.syncTrialStateFromBackend() }
            runCatching { subscriptionRepository.syncFounderRewardStateFromBackend() }
            runCatching { subscriptionRepository.syncPremiumStateFromBackend() }
            runCatching { subscriptionRepository.restorePurchases() }
        }
    }

    private companion object {
        const val REQUIRED_STARTUP_SYNC_TIMEOUT_MS = 45_000L
        const val ACCOUNT_APPEAR_WAIT_TIMEOUT_MS = 10_000L
        const val AUTOMATIC_RETRY_DELAY_MS = 1_500L
        const val MAX_REQUIRED_STARTUP_SYNC_ATTEMPTS = 3
    }
}





