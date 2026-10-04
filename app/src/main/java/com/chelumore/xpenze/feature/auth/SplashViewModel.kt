package com.chelumore.xpenze.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.SyncRepository
import com.chelumore.xpenze.domain.repository.SyncState
import com.chelumore.xpenze.domain.repository.UserPreferences
import com.chelumore.xpenze.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class SplashUiState(
    val isLoading: Boolean = true,
    val destination: SplashDestination? = null
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
                    }
                }
        }

        viewModelScope.launch {
            authRepository.observeAuthState()
                .distinctUntilChanged()
                .collectLatest { user ->
                    var prefs = userRepository.userPreferences.first()
                    val isEmailVerificationPending =
                        user != null && user.email != null && !user.isEmailVerified

                    // Safety net for a guest whose adoption was deferred until they verified their
                    // email: AuthRepository handles the sign-in itself. Must run before the generic
                    // "different user" wipe below, which would otherwise delete their records.
                    val isGuestAdoptingAccount = user != null &&
                        prefs.isGuestMode &&
                        prefs.userId.isNullOrBlank() &&
                        !isEmailVerificationPending

                    // Keep local preferences in sync (covers "already signed in" sessions on app start).
                    if (isGuestAdoptingAccount) {
                        userRepository.promoteGuestToUser(
                            userId = user.uid,
                            email = user.email,
                            name = user.displayName,
                            photoUrl = user.photoUrl
                        )
                        prefs = userRepository.userPreferences.first()
                    } else if (user == null && !prefs.userId.isNullOrBlank()) {
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
                    } else if (user != null && !prefs.isGuestMode && prefs.userId != user.uid) {
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

                    _uiState.value = SplashUiState(isLoading = true)

                    when {
                        // Still a guest: either never signed in, or signed up but not yet verified.
                        // Either way their data lives locally and they keep full access to it.
                        prefs.isGuestMode -> {
                            val localAccounts = accountRepository.getAll().first()
                            if (!prefs.isOnboardingComplete && localAccounts.isEmpty()) {
                                // Incomplete guest session: never auto-resume onboarding.
                                // Require an explicit "Continue without account" choice again.
                                userRepository.setGuestMode(false)
                                userRepository.clearTrialState()
                                userRepository.setPremium(false)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        destination = SplashDestination.Welcome
                                    )
                                }
                            } else {
                                resolveGuestDestination(prefs)
                            }
                        }

                        user == null -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    destination = SplashDestination.Welcome
                                )
                            }
                        }

                        isEmailVerificationPending -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    destination = SplashDestination.Verification
                                )
                            }
                        }

                        else -> resolveSignedInDestination(prefs)
                    }
                }
        }
    }

    /**
     * Guests have no cloud account, so there is nothing to restore and no sync to wait on.
     * Route straight off the splash using local state only.
     */
    private suspend fun resolveGuestDestination(prefs: UserPreferences) {
        val localAccounts = accountRepository.getAll().first()
        if (localAccounts.isNotEmpty()) {
            syncDefaultAccountSelection(localAccounts, prefs)
            if (!prefs.isOnboardingComplete) {
                userRepository.setOnboardingComplete(true)
            }
        }

        val destination = if (localAccounts.isNotEmpty() || prefs.isOnboardingComplete) {
            SplashDestination.Main
        } else {
            SplashDestination.Onboarding
        }

        _uiState.update { it.copy(isLoading = false, destination = destination) }
    }

    private suspend fun resolveSignedInDestination(prefs: UserPreferences) {
        _uiState.update { it.copy(isLoading = true, destination = null) }
        ensureBackgroundSync()

        val localAccounts = accountRepository.getAll().first()
        if (localAccounts.isNotEmpty()) {
            finalizeMainDestination(localAccounts, prefs)
            return
        }

        val restoreResult = attemptInitialBootstrapSync()
        if (restoreResult.accounts.isNotEmpty()) {
            finalizeMainDestination(restoreResult.accounts, prefs)
            return
        }

        // No cloud/local accounts: a brand-new signed-in user must go through onboarding.
        // Never skip setup just because restore timed out or failed — that left new Google
        // accounts on an empty Main screen ("0.00") with no first-account flow.
        val destination = if (prefs.isOnboardingComplete) {
            SplashDestination.Main
        } else {
            SplashDestination.Onboarding
        }
        finalizeSignedInFallbackDestination(destination)
    }

    private suspend fun attemptInitialBootstrapSync(): BootstrapRestoreResult {
        val syncOutcome = runStartupSyncAttempt(timeoutMs = INITIAL_BOOTSTRAP_SYNC_TIMEOUT_MS)
        val accounts = accountRepository.getAll().first()
            .takeIf { it.isNotEmpty() }
            ?: withTimeoutOrNull(BOOTSTRAP_ACCOUNT_APPEAR_WAIT_TIMEOUT_MS) {
                accountRepository.getAll()
                    .filter { it.isNotEmpty() }
                    .first()
            }
            ?: emptyList()

        return BootstrapRestoreResult(
            syncCompleted = syncOutcome.completed,
            accounts = accounts
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
        finalizeSignedInFallbackDestination(SplashDestination.Main)
    }

    private fun finalizeSignedInFallbackDestination(destination: SplashDestination) {
        _uiState.update {
            it.copy(
                isLoading = false,
                destination = destination
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
        val accounts: List<Account>
    )

    private data class SyncAttemptResult(
        val completed: Boolean
    )

    private suspend fun runStartupSyncAttempt(timeoutMs: Long): SyncAttemptResult {
        return withTimeoutOrNull(timeoutMs) {
            val currentState = syncRepository.syncState.first()
            val finalSyncState = when (currentState) {
                is SyncState.Syncing -> {
                    syncRepository.syncState
                        .filter { it !is SyncState.Syncing }
                        .first()
                }
                else -> {
                    val fullSyncResult = runCatching { syncRepository.fullSync() }
                    if (fullSyncResult.isFailure) {
                        return@withTimeoutOrNull SyncAttemptResult(completed = false)
                    }
                    syncRepository.syncState.first()
                }
            }

            when (finalSyncState) {
                is SyncState.Success -> SyncAttemptResult(completed = true)
                is SyncState.Error -> SyncAttemptResult(completed = false)
                else -> SyncAttemptResult(completed = false)
            }
        } ?: SyncAttemptResult(completed = false)
    }

    private fun ensureBackgroundSync() {
        syncScheduler.schedulePeriodic()
        syncScheduler.scheduleNow()
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
        const val INITIAL_BOOTSTRAP_SYNC_TIMEOUT_MS = 6_000L
        const val BOOTSTRAP_ACCOUNT_APPEAR_WAIT_TIMEOUT_MS = 2_000L
    }
}





