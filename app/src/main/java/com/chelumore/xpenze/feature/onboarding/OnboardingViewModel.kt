package com.chelumore.xpenze.feature.onboarding

import android.content.Context
import com.chelumore.xpenze.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.common.toCents
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.usecase.account.AddAccountUseCase
import com.chelumore.xpenze.domain.usecase.budget.CreateBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    INTRO,
    LANGUAGE,
    CURRENCY,
    ACCOUNT,
    BUDGET,
    DONE
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
    val budgetPeriod: BudgetPeriod = BudgetPeriod.MONTHLY
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val addAccountUseCase: AddAccountUseCase,
    private val createBudgetUseCase: CreateBudgetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    init {
        viewModelScope.launch {
            val prefs = userRepository.userPreferences.first()
            _uiState.update {
                it.copy(
                    currencyCode = prefs.defaultCurrency,
                    appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag)
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
                OnboardingStep.DONE -> OnboardingStep.BUDGET
            }
            state.copy(step = prev, error = null)
        }
    }

    fun goNext() {
        _uiState.update { state ->
            val next = when (state.step) {
                OnboardingStep.INTRO -> OnboardingStep.LANGUAGE
                OnboardingStep.LANGUAGE -> OnboardingStep.CURRENCY
                OnboardingStep.CURRENCY -> OnboardingStep.ACCOUNT
                OnboardingStep.ACCOUNT -> OnboardingStep.BUDGET
                OnboardingStep.BUDGET -> OnboardingStep.DONE
                OnboardingStep.DONE -> OnboardingStep.DONE
            }
            state.copy(step = next, error = null)
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
                _uiState.update { it.copy(isSaving = false, completed = true) }
            } catch (e: Exception) {
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


