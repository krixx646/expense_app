package com.chelumore.xpenze.feature.transactions.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(AddTransactionUiState())
    
    // Combine repositories with form state
    val uiState: StateFlow<AddTransactionUiState> = combine(
        _formState,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true)
    ) { form, categories, accounts ->
        val sanitizedCategory = form.selectedCategory?.takeIf { category ->
            when (form.type) {
                TransactionType.EXPENSE -> category.type == CategoryType.EXPENSE
                TransactionType.INCOME -> category.type == CategoryType.INCOME
                TransactionType.TRANSFER -> false
            }
        }
        form.copy(
            categories = categories,
            accounts = accounts,
            selectedCategory = sanitizedCategory,
            // Pre-select first account if none selected and accounts exist
            selectedAccount = form.selectedAccount ?: accounts.firstOrNull()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddTransactionUiState(isLoading = true)
    )

    fun setType(type: TransactionType) {
        _formState.update { state ->
            state.copy(
                type = type,
                selectedCategory = state.selectedCategory?.takeIf { category ->
                    when (type) {
                        TransactionType.EXPENSE -> category.type == CategoryType.EXPENSE
                        TransactionType.INCOME -> category.type == CategoryType.INCOME
                        TransactionType.TRANSFER -> false
                    }
                },
                selectedToAccount = if (type == TransactionType.TRANSFER) state.selectedToAccount else null
            )
        }
    }

    fun setAmount(amount: String) {
        if (amount.all { it.isDigit() || it == '.' }) {
             _formState.update { it.copy(amountText = amount) }
        }
    }

    fun setTitle(title: String) {
        _formState.update { it.copy(title = title) }
    }

    fun setNotes(notes: String) {
        _formState.update { it.copy(notes = notes.ifBlank { null }) }
    }
    
    fun setCategory(category: Category?) {
        _formState.update { it.copy(selectedCategory = category) }
    }
    
    fun setAccount(account: Account?) {
        _formState.update { it.copy(selectedAccount = account) }
    }
    
    fun setToAccount(account: Account?) {
        _formState.update { it.copy(selectedToAccount = account) }
    }

    fun saveTransaction() {
        val state = uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                _formState.update { it.copy(isSaving = true) }
                
                val amountInCents = ((state.amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                addTransactionUseCase(
                    accountId = state.selectedAccount!!.id,
                    toAccountId = state.selectedToAccount?.id,
                    categoryId = state.selectedCategory?.id,
                    amountCents = amountInCents,
                    currency = state.selectedAccount!!.currency,
                    type = state.type,
                    title = state.title,
                    notes = state.notes,
                    dateTime = System.currentTimeMillis()
                )
                _formState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
