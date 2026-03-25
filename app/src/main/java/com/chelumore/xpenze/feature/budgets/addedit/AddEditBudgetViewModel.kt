package com.chelumore.xpenze.feature.budgets.addedit

import android.content.Context
import com.chelumore.xpenze.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.CreateBudgetUseCase
import com.chelumore.xpenze.domain.usecase.budget.DeleteBudgetUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetByIdUseCase
import com.chelumore.xpenze.domain.usecase.budget.UpdateBudgetUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditBudgetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val preferencesDataStore: PreferencesDataStore,
    private val budgetAlertScheduler: BudgetAlertScheduler
) : ViewModel() {

    private val _form = MutableStateFlow(AddEditBudgetUiState())

    val uiState: StateFlow<AddEditBudgetUiState> = combine(
        _form,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true),
        preferencesDataStore.userPreferences
    ) { form, categories, accounts, prefs ->
        form.copy(
            // Keep form.isLoading as-is; init() will toggle it.
            categories = categories,
            accounts = accounts,
            currencyCode = prefs.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditBudgetUiState()
    )

    fun init(budgetId: String?) {
        if (budgetId.isNullOrBlank()) {
            _form.update { AddEditBudgetUiState(isLoading = false, isEdit = false, budgetId = null) }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(isLoading = true, error = null, isSaved = false) }
            try {
                val budget = getBudgetByIdUseCase(budgetId).first()
                    ?: throw IllegalStateException(context.getString(R.string.error_budget_not_found))
                _form.update {
                    it.copy(
                        isLoading = false,
                        isEdit = true,
                        budgetId = budget.id,
                        name = budget.name,
                        amountText = (budget.amountLimitCents / 100.0).toString(),
                        period = budget.period,
                        alertThreshold = (budget.alertThresholdPercent / 100f).coerceIn(0f, 1f),
                        isActive = budget.isActive,
                        startDate = budget.startDate,
                        endDate = budget.endDate,
                        selectedCategoryId = budget.categoryId,
                        selectedAccountId = budget.accountId
                    )
                }
            } catch (e: Exception) {
                _form.update { it.copy(isLoading = false, error = e.message ?: context.getString(R.string.error_budget_load)) }
            }
        }
    }

    fun setName(value: String) = _form.update { it.copy(name = value) }
    fun setAmount(value: String) {
        if (value.all { it.isDigit() || it == '.' }) _form.update { it.copy(amountText = value) }
    }
    fun setPeriod(value: BudgetPeriod) = _form.update { it.copy(period = value) }
    fun setAlertThreshold(value: Float) = _form.update { it.copy(alertThreshold = value) }
    fun setCategoryId(value: String?) = _form.update { it.copy(selectedCategoryId = value) }
    fun setAccountId(value: String?) = _form.update { it.copy(selectedAccountId = value) }
    fun setActive(value: Boolean) = _form.update { it.copy(isActive = value) }
    fun setStartDate(value: Long) = _form.update { it.copy(startDate = value) }
    fun setEndDate(value: Long?) = _form.update { it.copy(endDate = value) }

    fun clearError() = _form.update { it.copy(error = null) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                _form.update { it.copy(isSaving = true, error = null) }

                val amountCents = ((state.amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                val now = System.currentTimeMillis()
                val resolvedName = resolveName(state)

                if (!state.isEdit) {
                    createBudgetUseCase(
                        name = resolvedName,
                        amountLimitCents = amountCents,
                        period = state.period,
                        categoryId = state.selectedCategoryId,
                        accountId = state.selectedAccountId,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        alertThresholdPercent = (state.alertThreshold * 100).toInt(),
                        isActive = state.isActive
                    )
                } else {
                    val existing = getBudgetByIdUseCase(state.budgetId!!).first()
                        ?: throw IllegalStateException(context.getString(R.string.error_budget_not_found))
                    val updated: Budget = existing.copy(
                        name = resolvedName,
                        amountLimitCents = amountCents,
                        period = state.period,
                        categoryId = state.selectedCategoryId,
                        accountId = state.selectedAccountId,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        alertThresholdPercent = (state.alertThreshold * 100).toInt(),
                        isActive = state.isActive,
                        updatedAt = now,
                        synced = false
                    )
                    updateBudgetUseCase(updated)
                }

                budgetAlertScheduler.scheduleNow()
                _form.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _form.update { it.copy(isSaving = false, error = e.message ?: context.getString(R.string.error_budget_save)) }
            }
        }
    }

    fun delete() {
        val id = uiState.value.budgetId ?: return
        viewModelScope.launch {
            try {
                deleteBudgetUseCase(id)
                budgetAlertScheduler.scheduleNow()
                _form.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _form.update { it.copy(error = e.message ?: context.getString(R.string.error_budget_delete)) }
            }
        }
    }

    private fun resolveName(state: AddEditBudgetUiState): String {
        val explicit = state.name.trim()
        if (explicit.isNotBlank()) return explicit

        val categoryName = state.selectedCategory?.name
        val accountName = state.selectedAccount?.name

        return when {
            !categoryName.isNullOrBlank() -> context.getString(R.string.budget_name_from_category, categoryName)
            !accountName.isNullOrBlank() -> context.getString(R.string.budget_name_from_account, accountName)
            else -> when (state.period) {
                BudgetPeriod.DAILY -> context.getString(R.string.budget_default_daily_name)
                BudgetPeriod.WEEKLY -> context.getString(R.string.budget_default_weekly_name)
                BudgetPeriod.MONTHLY -> context.getString(R.string.budget_default_monthly_name)
                BudgetPeriod.YEARLY -> context.getString(R.string.budget_default_yearly_name)
                BudgetPeriod.CUSTOM -> context.getString(R.string.budget_default_custom_name)
            }
        }
    }
}


