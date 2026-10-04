package com.chelumore.xpenze.feature.budgets.addedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.core.work.BudgetLifecycleScheduler
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.nextTimeframeRange
import com.chelumore.xpenze.domain.model.resolveBudgetEndDate
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.CreateBudgetUseCase
import com.chelumore.xpenze.domain.usecase.budget.DeleteBudgetUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetByIdUseCase
import com.chelumore.xpenze.domain.usecase.budget.UpdateBudgetUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val budgetAlertScheduler: BudgetAlertScheduler,
    private val budgetLifecycleScheduler: BudgetLifecycleScheduler,
    private val premiumEntitlementTracker: PremiumEntitlementTracker,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker
) : ViewModel() {

    private val _form = MutableStateFlow(AddEditBudgetUiState())

    val uiState: StateFlow<AddEditBudgetUiState> = combine(
        _form,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true),
        preferencesDataStore.userPreferences,
        premiumEntitlementTracker.entitlement
    ) { form, categories, accounts, prefs, entitlement ->
        val sanitizedCategoryId = form.selectedCategoryId?.takeIf { selectedId ->
            categories.any { it.id == selectedId && it.type == CategoryType.EXPENSE }
        }
        form.copy(
            categories = categories,
            accounts = accounts,
            selectedCategoryId = sanitizedCategoryId,
            currencyCode = prefs.defaultCurrency,
            hasPremiumAccess = entitlement.hasPremiumAccess
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditBudgetUiState()
    )

    fun init(budgetId: String?, cloneFromBudgetId: String? = null) {
        when {
            !budgetId.isNullOrBlank() -> loadBudgetForEdit(budgetId)
            !cloneFromBudgetId.isNullOrBlank() -> loadBudgetForClone(cloneFromBudgetId)
            else -> initializeFreshBudget()
        }
    }

    fun setName(value: String) = _form.update { it.copy(name = value) }

    fun setAmount(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _form.update { it.copy(amountText = value) }
        }
    }

    fun setPeriod(value: BudgetPeriod) {
        _form.update { state ->
            if (state.period == value) return@update state
            val nextStartDate = if (!state.isEdit && state.cloneFromBudgetId == null) {
                defaultStartDateForPeriod(value, System.currentTimeMillis())
            } else {
                state.startDate
            }
            val nextEndDate = when (value) {
                BudgetPeriod.CUSTOM -> state.endDate ?: resolveBudgetEndDate(
                    period = state.period,
                    startDate = state.startDate,
                    explicitEndDate = state.endDate
                )
                else -> null
            }
            state.copy(
                period = value,
                startDate = nextStartDate,
                endDate = nextEndDate
            )
        }
    }

    fun setAlertThreshold(value: Float) = _form.update { it.copy(alertThreshold = value) }
    fun setCategoryId(value: String?) = _form.update { it.copy(selectedCategoryId = value) }
    fun setAccountId(value: String?) = _form.update { it.copy(selectedAccountId = value) }
    fun setActive(value: Boolean) = _form.update { it.copy(isActive = value) }
    fun setRecurring(value: Boolean) {
        if (value && !uiState.value.hasPremiumAccess) {
            _form.update { it.copy(error = context.getString(R.string.budget_editor_recurring_premium_body)) }
            return
        }
        _form.update { it.copy(isRecurring = value) }
    }
    fun setStartDate(value: Long) = _form.update { it.copy(startDate = value) }
    fun setEndDate(value: Long?) = _form.update { it.copy(endDate = value) }

    fun clearError() = _form.update { it.copy(error = null) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        if (state.isRecurring && !state.hasPremiumAccess) {
            _form.update {
                it.copy(error = context.getString(R.string.budget_editor_recurring_premium_body))
            }
            return
        }

        viewModelScope.launch {
            try {
                _form.update { it.copy(isSaving = true, error = null) }

                val amountCents = ((state.amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                val resolvedEndDate = resolveBudgetEndDate(
                    period = state.period,
                    startDate = state.startDate,
                    explicitEndDate = state.endDate
                )
                if (state.period == BudgetPeriod.CUSTOM && resolvedEndDate == null) {
                    throw IllegalStateException(context.getString(R.string.budget_editor_custom_end_required))
                }
                val now = System.currentTimeMillis()
                val resolvedName = resolveName(state)
                val resolvedCategoryId = state.selectedCategory?.takeIf { it.type == CategoryType.EXPENSE }?.id

                if (!state.isEdit) {
                    createBudgetUseCase(
                        name = resolvedName,
                        amountLimitCents = amountCents,
                        period = state.period,
                        categoryId = resolvedCategoryId,
                        accountId = state.selectedAccountId,
                        startDate = state.startDate,
                        endDate = resolvedEndDate,
                        alertThresholdPercent = (state.alertThreshold * 100).toInt(),
                        isActive = state.isActive,
                        isRecurring = state.isRecurring
                    )
                } else {
                    val existing = getBudgetByIdUseCase(state.budgetId!!).first()
                        ?: throw IllegalStateException(context.getString(R.string.error_budget_not_found))
                    val updated: Budget = existing.copy(
                        name = resolvedName,
                        amountLimitCents = amountCents,
                        period = state.period,
                        categoryId = resolvedCategoryId,
                        accountId = state.selectedAccountId,
                        startDate = state.startDate,
                        endDate = resolvedEndDate,
                        alertThresholdPercent = (state.alertThreshold * 100).toInt(),
                        isActive = state.isActive,
                        isRecurring = state.isRecurring,
                        seriesId = when {
                            state.isRecurring -> existing.seriesId?.takeIf { it.isNotBlank() } ?: existing.id
                            else -> existing.seriesId
                        },
                        updatedAt = now,
                        synced = false
                    )
                    updateBudgetUseCase(updated)
                }

                budgetAlertScheduler.scheduleNow()
                budgetLifecycleScheduler.scheduleNow()
                if (!state.isEdit) {
                    runCatching { preferencesDataStore.incrementRateEligibleActionCount() }
                    val lifetimeActions = runCatching {
                        preferencesDataStore.userPreferences.first().rateEligibleActionCount
                    }.getOrDefault(0)
                    growthAnalyticsTracker.logBudgetCreated(lifetimeActions)
                }
                _form.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _form.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: context.getString(R.string.error_budget_save)
                    )
                }
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
                _form.update {
                    it.copy(error = e.message ?: context.getString(R.string.error_budget_delete))
                }
            }
        }
    }

    private fun loadBudgetForEdit(budgetId: String) {
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
                        cloneFromBudgetId = null,
                        name = budget.name,
                        amountText = (budget.amountLimitCents / 100.0).toString(),
                        period = budget.period,
                        alertThreshold = (budget.alertThresholdPercent / 100f).coerceIn(0f, 1f),
                        isActive = budget.isActive,
                        isRecurring = budget.isRecurring,
                        startDate = budget.startDate,
                        endDate = budget.endDate,
                        selectedCategoryId = budget.categoryId,
                        selectedAccountId = budget.accountId
                    )
                }
            } catch (e: Exception) {
                _form.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_budget_load)
                    )
                }
            }
        }
    }

    private fun loadBudgetForClone(sourceBudgetId: String) {
        viewModelScope.launch {
            _form.update { it.copy(isLoading = true, error = null, isSaved = false) }
            try {
                val sourceBudget = getBudgetByIdUseCase(sourceBudgetId).first()
                    ?: throw IllegalStateException(context.getString(R.string.error_budget_not_found))
                val now = System.currentTimeMillis()
                val nextRange = sourceBudget.nextTimeframeRange()
                _form.update {
                    it.copy(
                        isLoading = false,
                        isEdit = false,
                        budgetId = null,
                        cloneFromBudgetId = sourceBudget.id,
                        name = sourceBudget.name,
                        amountText = (sourceBudget.amountLimitCents / 100.0).toString(),
                        period = sourceBudget.period,
                        alertThreshold = (sourceBudget.alertThresholdPercent / 100f).coerceIn(0f, 1f),
                        isActive = true,
                        isRecurring = sourceBudget.isRecurring,
                        startDate = nextRange?.startMillis ?: defaultStartDateForPeriod(sourceBudget.period, now),
                        endDate = if (sourceBudget.period == BudgetPeriod.CUSTOM) nextRange?.endMillis else null,
                        selectedCategoryId = sourceBudget.categoryId,
                        selectedAccountId = sourceBudget.accountId
                    )
                }
            } catch (e: Exception) {
                _form.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_budget_load)
                    )
                }
            }
        }
    }

    private fun initializeFreshBudget() {
        val now = System.currentTimeMillis()
        _form.value = AddEditBudgetUiState(
            isLoading = false,
            isEdit = false,
            budgetId = null,
            cloneFromBudgetId = null,
            startDate = defaultStartDateForPeriod(BudgetPeriod.MONTHLY, now),
            endDate = null,
            isRecurring = false
        )
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

    private fun defaultStartDateForPeriod(period: BudgetPeriod, nowMillis: Long): Long {
        if (period == BudgetPeriod.CUSTOM) return nowMillis

        val zone = ZoneId.systemDefault()
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val startDate = when (period) {
            BudgetPeriod.DAILY -> nowDate
            BudgetPeriod.WEEKLY -> nowDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            BudgetPeriod.MONTHLY -> nowDate.withDayOfMonth(1)
            BudgetPeriod.YEARLY -> nowDate.withDayOfYear(1)
            BudgetPeriod.CUSTOM -> nowDate
        }

        return startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
