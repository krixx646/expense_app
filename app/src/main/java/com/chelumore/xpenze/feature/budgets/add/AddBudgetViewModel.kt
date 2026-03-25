package com.chelumore.xpenze.feature.budgets.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.CreateBudgetUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class AddBudgetViewModel @Inject constructor(
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(AddBudgetUiState())
    
    // Combine repositories with form state
    val uiState: StateFlow<AddBudgetUiState> = combine(
        _formState,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true)
    ) { form, categories, accounts ->
        form.copy(
            categories = categories,
            accounts = accounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddBudgetUiState(isLoading = true)
    )

    fun setName(name: String) {
        _formState.update { it.copy(name = name) }
    }

    fun setAmount(amount: String) {
        // Simple validation: allow digits and one dot
        if (amount.all { it.isDigit() || it == '.' }) {
             _formState.update { it.copy(amountText = amount) }
        }
    }

    fun setPeriod(period: BudgetPeriod) {
        _formState.update { it.copy(selectedPeriod = period) }
    }

    fun setCategory(category: Category?) {
        _formState.update { it.copy(selectedCategory = category) }
    }

    fun setAccount(account: Account?) {
        _formState.update { it.copy(selectedAccount = account) }
    }

    fun saveBudget() {
        val currentState = uiState.value
        if (!currentState.canSave) return

        viewModelScope.launch {
            try {
                _formState.update { it.copy(isSaving = true) }
                
                val amountCents = (currentState.amountText.toDouble() * 100).toLong()
                createBudgetUseCase(
                    name = currentState.name,
                    amountLimitCents = amountCents,
                    period = currentState.selectedPeriod,
                    categoryId = currentState.selectedCategory?.id,
                    accountId = currentState.selectedAccount?.id,
                    startDate = startOfPeriodMillis(currentState.selectedPeriod, System.currentTimeMillis()),
                    endDate = null,
                    alertThresholdPercent = (currentState.alertThreshold * 100).toInt(),
                    isActive = true
                )
                _formState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun startOfPeriodMillis(period: BudgetPeriod, nowMillis: Long): Long {
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

