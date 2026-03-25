package com.chelumore.xpenze.feature.transactions.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.DeleteTransactionUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModel @Inject constructor(
    private val getTransactionDetailsUseCase: GetTransactionDetailsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val budgetAlertScheduler: BudgetAlertScheduler,
    getAccountsUseCase: GetAccountsUseCase,
    getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val transactionId = MutableStateFlow<String?>(null)

    private val txFlow = transactionId
        .filterNotNull()
        .flatMapLatest { id -> getTransactionDetailsUseCase(id) }

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        txFlow,
        getAccountsUseCase(includeArchived = true),
        getCategoriesUseCase()
    ) { tx, accounts, categories ->
        if (tx == null) {
            return@combine TransactionDetailUiState(isLoading = false, error = "Transaction not found")
        }
        val accountName = accounts.firstOrNull { it.id == tx.accountId }?.name
        val toAccountName = tx.toAccountId?.let { toId -> accounts.firstOrNull { it.id == toId }?.name }
        val categoryName = tx.categoryId?.let { cId -> categories.firstOrNull { it.id == cId }?.name }
        TransactionDetailUiState(
            isLoading = false,
            transaction = tx,
            accountName = accountName,
            toAccountName = toAccountName,
            categoryName = categoryName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailUiState()
    )

    fun load(id: String) {
        transactionId.value = id
    }

    fun delete(onDeleted: () -> Unit) {
        val id = transactionId.value ?: return
        viewModelScope.launch {
            deleteTransactionUseCase(id)
            budgetAlertScheduler.scheduleNow()
            onDeleted()
        }
    }
}


