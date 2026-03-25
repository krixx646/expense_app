package com.chelumore.xpenze.feature.budgets.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.DeleteBudgetUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetByIdUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetProgressUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import com.chelumore.xpenze.domain.usecase.transaction.TransactionQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModel @Inject constructor(
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val budgetAlertScheduler: BudgetAlertScheduler,
    getCategoriesUseCase: GetCategoriesUseCase,
    getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val budgetId = MutableStateFlow<String?>(null)

    private val budgetFlow = budgetId
        .filterNotNull()
        .flatMapLatest { id -> getBudgetByIdUseCase(id) }

    private val txFlow = budgetFlow
        .filterNotNull()
        .flatMapLatest { budget ->
            val window = budget.currentPeriodWindow()
            if (window == null) {
                flowOf(emptyList())
            } else {
                getTransactionsUseCase(
                    TransactionQuery(
                        dateRange = window.spentRange,
                        type = TransactionType.EXPENSE,
                        accountId = budget.accountId,
                        categoryId = budget.categoryId
                    )
                )
            }
        }

    val uiState: StateFlow<BudgetDetailUiState> = combine(
        budgetFlow,
        txFlow,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true)
    ) { budget, transactions, categories, accounts ->
        if (budget == null) {
            return@combine BudgetDetailUiState(isLoading = false, error = "Budget not found")
        }
        val window = budget.currentPeriodWindow()
        val progress = if (window == null) {
            getBudgetProgressUseCase(budget, emptyList())
        } else {
            getBudgetProgressUseCase(budget, transactions, window.spentRange)
        }

        val categoryName = budget.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name } ?: "All Categories"
        val accountName = budget.accountId?.let { id -> accounts.firstOrNull { it.id == id }?.name } ?: "All Accounts"

        BudgetDetailUiState(
            isLoading = false,
            budget = budget,
            categoryName = categoryName,
            accountName = accountName,
            spentCents = progress.spentCents,
            remainingCents = progress.remainingCents,
            progress = progress.progress,
            transactions = transactions.sortedByDescending { it.dateTime }.take(30)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetDetailUiState()
    )

    fun load(id: String) {
        budgetId.value = id
    }

    fun delete(onDeleted: () -> Unit) {
        val id = budgetId.value ?: return
        viewModelScope.launch {
            deleteBudgetUseCase(id)
            budgetAlertScheduler.scheduleNow()
            onDeleted()
        }
    }
}


