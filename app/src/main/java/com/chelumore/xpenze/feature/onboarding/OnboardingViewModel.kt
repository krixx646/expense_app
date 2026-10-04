package com.chelumore.xpenze.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.common.toCents
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.FounderTrialActivationResult
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.TrialActivationResult
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.hasPremiumAccess
import com.chelumore.xpenze.domain.repository.isTrialActive
import com.chelumore.xpenze.domain.usecase.account.AddAccountUseCase
import com.chelumore.xpenze.domain.usecase.budget.CreateBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    INTRO,
    LANGUAGE,
    CURRENCY,
    ACCOUNT,
    BUDGET,
    AUTO_CAPTURE,
    DONE
}

enum class OnboardingTrialBanner {
    NONE,
    SIGN_IN_NEEDED,
    STARTED,
    FOUNDER_STARTED,
    ALREADY_ACTIVE
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.INTRO,
    val isSaving: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false,

    // Language
    val appLanguageTag: String = "en",

    // Currency
    val currencyCode: String = "USD",

    // Account
    val accountName: String = "",
    val accountType: AccountType = AccountType.CASH,
    val initialBalanceText: String = "",

    // Optional budget
    val addBudget: Boolean = false,
    val budgetName: String = "",
    val budgetAmountText: String = "",
    val budgetPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,

    // Auto Capture / trial
    val isSignedIn: Boolean = false,
    val hasPremiumAccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialBanner: OnboardingTrialBanner = OnboardingTrialBanner.NONE,
    val notificationListenerGranted: Boolean = false,
    val notificationCaptureEnabled: Boolean = false,
    val isClaimingTrial: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val addAccountUseCase: AddAccountUseCase,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val subscriptionRepository: SubscriptionRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    private var trialClaimAttempted = false

    init {
        growthAnalyticsTracker.logOnboardingStep(OnboardingStep.INTRO.name)
        viewModelScope.launch {
            val prefs = userRepository.userPreferences.first()
            _uiState.update {
                it.copy(
                    currencyCode = prefs.defaultCurrency,
                    appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag),
                    isSignedIn = !prefs.userId.isNullOrBlank() && !prefs.isGuestMode,
                    hasPremiumAccess = prefs.hasPremiumAccess(),
                    isTrialActive = prefs.isTrialActive(),
                    notificationCaptureEnabled = prefs.autoCaptureEnabled &&
                        prefs.autoCaptureNotificationEnabled
                )
            }
        }
    }

    fun goBack() {
        _uiState.update { state ->
            val prev = when (state.step) {
                OnboardingStep.INTRO -> OnboardingStep.INTRO
                OnboardingStep.LANGUAGE -> OnboardingStep.INTRO
                OnboardingStep.CURRENCY -> OnboardingStep.LANGUAGE
                OnboardingStep.ACCOUNT -> OnboardingStep.CURRENCY
                OnboardingStep.BUDGET -> OnboardingStep.ACCOUNT
                OnboardingStep.AUTO_CAPTURE -> OnboardingStep.BUDGET
                OnboardingStep.DONE -> OnboardingStep.AUTO_CAPTURE
            }
            state.copy(step = prev, error = null)
        }
        growthAnalyticsTracker.logOnboardingStep(_uiState.value.step.name)
    }

    fun goNext() {
        val previous = _uiState.value.step
        _uiState.update { state ->
            val next = when (state.step) {
                OnboardingStep.INTRO -> OnboardingStep.LANGUAGE
                OnboardingStep.LANGUAGE -> OnboardingStep.CURRENCY
                OnboardingStep.CURRENCY -> OnboardingStep.ACCOUNT
                OnboardingStep.ACCOUNT -> OnboardingStep.BUDGET
                OnboardingStep.BUDGET -> OnboardingStep.AUTO_CAPTURE
                OnboardingStep.AUTO_CAPTURE -> OnboardingStep.DONE
                OnboardingStep.DONE -> OnboardingStep.DONE
            }
            state.copy(step = next, error = null)
        }
        growthAnalyticsTracker.logOnboardingStep(_uiState.value.step.name)
        if (previous != OnboardingStep.AUTO_CAPTURE &&
            _uiState.value.step == OnboardingStep.AUTO_CAPTURE
        ) {
            ensureOnboardingTrial()
        }
    }

    fun setCurrency(code: String) {
        _uiState.update { it.copy(currencyCode = code) }
    }

    fun setAppLanguage(languageTag: String) {
        val normalizedTag = AppLanguages.normalizeTag(languageTag)
        _uiState.update { it.copy(appLanguageTag = normalizedTag) }
        viewModelScope.launch {
            userRepository.setAppLanguageTag(normalizedTag)
        }
    }

    fun setAccountName(value: String) {
        _uiState.update { it.copy(accountName = value) }
    }

    fun setAccountType(type: AccountType) {
        _uiState.update { it.copy(accountType = type) }
    }

    fun setInitialBalance(text: String) {
        _uiState.update { it.copy(initialBalanceText = text) }
    }

    fun setAddBudget(enabled: Boolean) {
        _uiState.update { it.copy(addBudget = enabled) }
    }

    fun setBudgetName(value: String) {
        _uiState.update { it.copy(budgetName = value) }
    }

    fun setBudgetAmount(text: String) {
        _uiState.update { it.copy(budgetAmountText = text) }
    }

    fun setBudgetPeriod(period: BudgetPeriod) {
        _uiState.update { it.copy(budgetPeriod = period) }
    }

    fun refreshNotificationListenerAccess(granted: Boolean) {
        _uiState.update { it.copy(notificationListenerGranted = granted) }
        if (granted) {
            enableNotificationCaptureIfEligible()
        }
    }

    fun enableNotificationCaptureIfEligible() {
        viewModelScope.launch {
            val prefs = userRepository.userPreferences.first()
            val signedIn = !prefs.userId.isNullOrBlank() && !prefs.isGuestMode
            val hasAccess = prefs.hasPremiumAccess()
            _uiState.update {
                it.copy(
                    isSignedIn = signedIn,
                    hasPremiumAccess = hasAccess,
                    isTrialActive = prefs.isTrialActive()
                )
            }
            // Guests never unlock Auto Capture / notification capture.
            if (!signedIn || !hasAccess || !_uiState.value.notificationListenerGranted) return@launch

            preferencesDataStore.setAutoCaptureDisclosureAccepted(true)
            preferencesDataStore.setAutoCaptureEnabled(true)
            preferencesDataStore.setAutoCaptureStatementImportEnabled(true)
            preferencesDataStore.setAutoCaptureOcrEnabled(true)
            preferencesDataStore.setAutoCaptureMerchantMemoryEnabled(true)
            preferencesDataStore.setAutoCaptureRecurringSuggestionsEnabled(true)
            preferencesDataStore.setAutoCaptureNotificationEnabled(true)
            _uiState.update { it.copy(notificationCaptureEnabled = true) }
        }
    }

    fun markNotificationPermissionPromptShown() {
        viewModelScope.launch {
            userRepository.setNotificationPermissionPromptLastShownAt(System.currentTimeMillis())
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value

        viewModelScope.launch {
            try {
                // If this device already pulled accounts from cloud sync, don't create a duplicate "first account".
                val existingAccounts = accountRepository.getAll().first()
                if (existingAccounts.isNotEmpty()) {
                    val defaultAccount = existingAccounts.firstOrNull { it.isDefault } ?: existingAccounts.first()
                    userRepository.setAppLanguageTag(AppLanguages.normalizeTag(state.appLanguageTag))
                    userRepository.setDefaultCurrency(defaultAccount.currency)
                    userRepository.setDefaultAccountId(defaultAccount.id)
                    userRepository.setOnboardingComplete(true)
                    growthAnalyticsTracker.logOnboardingCompleted(addedBudget = false)
                    _uiState.update { it.copy(isSaving = false, completed = true, error = null) }
                    return@launch
                }

                val currency = state.currencyCode.ifBlank { "USD" }
                val accountName = state.accountName.trim().ifBlank { defaultAccountName(state.accountType) }

                _uiState.update { it.copy(isSaving = true, error = null) }

                // Persist preferences first so the app uses the selected currency immediately.
                userRepository.setAppLanguageTag(AppLanguages.normalizeTag(state.appLanguageTag))
                userRepository.setDefaultCurrency(currency)

                val initialBalanceCents = (state.initialBalanceText.toDoubleOrNull() ?: 0.0).toCents()
                val accountId = addAccountUseCase(
                    name = accountName,
                    type = state.accountType,
                    currency = currency,
                    initialBalanceCents = initialBalanceCents,
                    isDefault = true
                )
                userRepository.setDefaultAccountId(accountId)

                if (state.addBudget) {
                    val budgetName = state.budgetName.trim().ifBlank { defaultBudgetName(state.budgetPeriod) }
                    val budgetAmountCents = (state.budgetAmountText.toDoubleOrNull() ?: 0.0).toCents()
                    if (budgetAmountCents <= 0L) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = context.getString(R.string.onboarding_budget_amount_required_now)
                            )
                        }
                        return@launch
                    }
                    createBudgetUseCase(
                        name = budgetName,
                        amountLimitCents = budgetAmountCents,
                        period = state.budgetPeriod,
                        categoryId = null,
                        accountId = null
                    )
                }

                userRepository.setOnboardingComplete(true)
                growthAnalyticsTracker.logOnboardingCompleted(addedBudget = state.addBudget)
                _uiState.update { it.copy(isSaving = false, completed = true) }
            } catch (e: Exception) {
                growthAnalyticsTracker.logOnboardingFailed(e::class.simpleName ?: "unknown")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: context.getString(R.string.error_onboarding_finish)
                    )
                }
            }
        }
    }

    fun consumeCompleted() {
        _uiState.update { it.copy(completed = false) }
    }

    private fun ensureOnboardingTrial() {
        if (trialClaimAttempted) return
        trialClaimAttempted = true

        viewModelScope.launch {
            _uiState.update { it.copy(isClaimingTrial = true) }
            val prefs = userRepository.userPreferences.first()
            val signedIn = !prefs.userId.isNullOrBlank() && !prefs.isGuestMode
            _uiState.update {
                it.copy(
                    isSignedIn = signedIn,
                    hasPremiumAccess = prefs.hasPremiumAccess(),
                    isTrialActive = prefs.isTrialActive()
                )
            }

            if (!signedIn) {
                growthAnalyticsTracker.logSubscriptionTrialBlocked("not_signed_in")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        trialBanner = OnboardingTrialBanner.SIGN_IN_NEEDED
                    )
                }
                return@launch
            }

            if (prefs.isPremium) {
                growthAnalyticsTracker.logSubscriptionTrialBlocked("already_premium")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        hasPremiumAccess = true,
                        trialBanner = OnboardingTrialBanner.NONE
                    )
                }
                return@launch
            }

            if (prefs.isTrialActive()) {
                growthAnalyticsTracker.logSubscriptionTrialBlocked("trial_already_active")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        isTrialActive = true,
                        hasPremiumAccess = true,
                        trialBanner = OnboardingTrialBanner.ALREADY_ACTIVE
                    )
                }
                return@launch
            }

            if (prefs.hasFounderReward && !prefs.founderTrialActivated) {
                when (val result = subscriptionRepository.activateFounderTrial()) {
                    is FounderTrialActivationResult.Started,
                    is FounderTrialActivationResult.AlreadyActivated -> {
                        growthAnalyticsTracker.logSubscriptionTrialStarted()
                        refreshPremiumFlags(
                            banner = OnboardingTrialBanner.FOUNDER_STARTED,
                            claiming = false
                        )
                    }
                    FounderTrialActivationResult.AlreadyPremium -> {
                        growthAnalyticsTracker.logSubscriptionTrialBlocked("already_premium")
                        refreshPremiumFlags(banner = OnboardingTrialBanner.NONE, claiming = false)
                    }
                    FounderTrialActivationResult.NotSignedIn -> {
                        growthAnalyticsTracker.logSubscriptionTrialBlocked("not_signed_in")
                        _uiState.update {
                            it.copy(
                                isClaimingTrial = false,
                                trialBanner = OnboardingTrialBanner.SIGN_IN_NEEDED
                            )
                        }
                    }
                    FounderTrialActivationResult.RewardNotOwned,
                    FounderTrialActivationResult.BackendUnavailable,
                    is FounderTrialActivationResult.Failure -> {
                        // Fall through to standard 7-day trial when founder path is unavailable.
                        claimStandardTrial()
                    }
                }
                return@launch
            }

            if (prefs.trialConsumed) {
                growthAnalyticsTracker.logSubscriptionTrialBlocked("already_consumed_expired")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        trialBanner = OnboardingTrialBanner.NONE
                    )
                }
                return@launch
            }

            claimStandardTrial()
        }
    }

    private suspend fun claimStandardTrial() {
        when (val result = subscriptionRepository.claimTrial(days = 7)) {
            is TrialActivationResult.Started -> {
                growthAnalyticsTracker.logSubscriptionTrialStarted()
                refreshPremiumFlags(banner = OnboardingTrialBanner.STARTED, claiming = false)
            }
            is TrialActivationResult.AlreadyConsumed -> {
                val active = result.expiresAt > System.currentTimeMillis()
                growthAnalyticsTracker.logSubscriptionTrialBlocked(
                    if (active) "already_consumed_active" else "already_consumed_expired"
                )
                refreshPremiumFlags(
                    banner = if (active) {
                        OnboardingTrialBanner.ALREADY_ACTIVE
                    } else {
                        OnboardingTrialBanner.NONE
                    },
                    claiming = false
                )
            }
            TrialActivationResult.NotSignedIn -> {
                growthAnalyticsTracker.logSubscriptionTrialBlocked("not_signed_in")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        trialBanner = OnboardingTrialBanner.SIGN_IN_NEEDED
                    )
                }
            }
            TrialActivationResult.BackendUnavailable -> {
                growthAnalyticsTracker.logSubscriptionTrialFailed("backend_unavailable")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        trialBanner = OnboardingTrialBanner.NONE
                    )
                }
            }
            is TrialActivationResult.Failure -> {
                growthAnalyticsTracker.logSubscriptionTrialFailed("backend_failure")
                _uiState.update {
                    it.copy(
                        isClaimingTrial = false,
                        trialBanner = OnboardingTrialBanner.NONE
                    )
                }
            }
        }
    }

    private suspend fun refreshPremiumFlags(
        banner: OnboardingTrialBanner,
        claiming: Boolean
    ) {
        val prefs = userRepository.userPreferences.first()
        _uiState.update {
            it.copy(
                isClaimingTrial = claiming,
                isSignedIn = !prefs.userId.isNullOrBlank() && !prefs.isGuestMode,
                hasPremiumAccess = prefs.hasPremiumAccess(),
                isTrialActive = prefs.isTrialActive(),
                trialBanner = banner,
                notificationCaptureEnabled = prefs.autoCaptureEnabled &&
                    prefs.autoCaptureNotificationEnabled
            )
        }
        if (_uiState.value.notificationListenerGranted && prefs.hasPremiumAccess()) {
            enableNotificationCaptureIfEligible()
        }
    }

    private fun defaultAccountName(type: AccountType): String = when (type) {
        AccountType.CASH -> context.getString(R.string.account_type_cash)
        AccountType.BANK -> context.getString(R.string.account_type_bank)
        AccountType.CREDIT_CARD -> context.getString(R.string.account_type_credit_card)
        AccountType.SAVINGS -> context.getString(R.string.account_type_savings)
        AccountType.INVESTMENT -> context.getString(R.string.account_type_investment)
        AccountType.OTHER -> context.getString(R.string.account_type_other)
    }

    private fun defaultBudgetName(period: BudgetPeriod): String = when (period) {
        BudgetPeriod.DAILY -> context.getString(R.string.budget_default_daily_name)
        BudgetPeriod.WEEKLY -> context.getString(R.string.budget_default_weekly_name)
        BudgetPeriod.MONTHLY -> context.getString(R.string.budget_default_monthly_name)
        BudgetPeriod.YEARLY -> context.getString(R.string.budget_default_yearly_name)
        BudgetPeriod.CUSTOM -> context.getString(R.string.budget_default_custom_name)
    }
}
