package com.chelumore.xpenze.feature.accounts.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.common.toCents
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.usecase.account.AddAccountUseCase
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.account.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val addAccountUseCase: AddAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditAccountUiState(currency = Constants.DEFAULT_CURRENCY))
    val uiState: StateFlow<AddEditAccountUiState> = _uiState

    fun loadAccount(accountId: String?) {
        if (accountId.isNullOrBlank()) {
            _uiState.update {
                AddEditAccountUiState(
                    currency = Constants.DEFAULT_CURRENCY
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSaved = false) }
            try {
                val accounts = getAccountsUseCase(includeArchived = true).first()
                val account = accounts.firstOrNull { it.id == accountId }
                    ?: throw IllegalStateException("Account not found")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEdit = true,
                        accountId = account.id,
                        existingAccount = account,
                        name = account.name,
                        type = account.type,
                        currency = account.currency,
                        initialBalanceText = (account.currentBalanceCents / 100.0).toString(),
                        colorHex = account.colorHex,
                        iconName = account.iconName,
                        isDefault = account.isDefault,
                        isArchived = account.isArchived
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setType(value: AccountType) = _uiState.update { it.copy(type = value) }
    fun setCurrency(value: String) = _uiState.update { it.copy(currency = value) }

    fun setInitialBalanceText(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(initialBalanceText = value) }
        }
    }

    fun setColorHex(value: String) = _uiState.update { it.copy(colorHex = value) }
    fun setIconName(value: String) = _uiState.update { it.copy(iconName = value) }
    fun setDefault(value: Boolean) = _uiState.update { it.copy(isDefault = value) }
    fun setArchived(value: Boolean) = _uiState.update { it.copy(isArchived = value) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }
                val enteredBalanceCents = (state.initialBalanceText.toDoubleOrNull() ?: 0.0).toCents()

                if (!state.isEdit) {
                    addAccountUseCase(
                        name = state.name.trim(),
                        type = state.type,
                        currency = state.currency.trim().uppercase(),
                        initialBalanceCents = enteredBalanceCents,
                        colorHex = state.colorHex,
                        iconName = state.iconName,
                        isDefault = state.isDefault,
                        isArchived = state.isArchived
                    )
                } else {
                    val existing = state.existingAccount ?: throw IllegalStateException("Missing existing account")
                    val netTrackedDelta = existing.currentBalanceCents - existing.initialBalanceCents
                    val updated = existing.copy(
                        name = state.name.trim(),
                        type = state.type,
                        currency = state.currency.trim().uppercase(),
                        initialBalanceCents = enteredBalanceCents - netTrackedDelta,
                        currentBalanceCents = enteredBalanceCents,
                        colorHex = state.colorHex,
                        iconName = state.iconName,
                        isDefault = state.isDefault,
                        isArchived = state.isArchived
                    )
                    updateAccountUseCase(updated)
                }

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}


