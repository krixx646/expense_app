package com.chelumore.xpenze.feature.accounts.transfer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.R
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.account.TransferBetweenAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val transferBetweenAccountsUseCase: TransferBetweenAccountsUseCase
) : ViewModel() {

    private val form = MutableStateFlow(TransferUiState())

    val uiState: StateFlow<TransferUiState> = combine(
        form,
        getAccountsUseCase(includeArchived = false)
    ) { formState, accounts ->
        val from = formState.fromAccount ?: accounts.firstOrNull()
        formState.copy(
            isLoading = false,
            accounts = accounts,
            fromAccount = from
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransferUiState()
    )

    fun setFromAccountId(id: String) {
        val acc = uiState.value.accounts.firstOrNull { it.id == id }
        form.update { it.copy(fromAccount = acc) }
    }

    fun setToAccountId(id: String) {
        val acc = uiState.value.accounts.firstOrNull { it.id == id }
        form.update { it.copy(toAccount = acc) }
    }

    fun setAmount(text: String) {
        if (text.isEmpty() || text.all { it.isDigit() || it == '.' }) {
            form.update { it.copy(amountText = text) }
        }
    }

    fun setNotes(text: String) {
        form.update { it.copy(notes = text) }
    }

    fun submit() {
        val state = uiState.value
        if (!state.canSave) return

        val from = state.fromAccount ?: return
        val to = state.toAccount ?: return
        val amountCents = ((state.amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()

        viewModelScope.launch {
            try {
                form.update { it.copy(isSaving = true, error = null) }
                transferBetweenAccountsUseCase(
                    fromAccountId = from.id,
                    toAccountId = to.id,
                    amountCents = amountCents,
                    currency = from.currency,
                    title = appContext.getString(R.string.transaction_default_title_transfer),
                    notes = state.notes.ifBlank { null }
                )
                form.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                form.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}


