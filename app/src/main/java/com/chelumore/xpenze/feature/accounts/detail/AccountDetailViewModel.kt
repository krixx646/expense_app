package com.chelumore.xpenze.feature.accounts.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.usecase.account.DeleteAccountUseCase
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.account.UpdateAccountUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val accountId = MutableStateFlow<String?>(null)

    private val accountFlow = accountId
        .filterNotNull()
        .flatMapLatest { id ->
            getAccountsUseCase(includeArchived = true).map { accounts ->
                accounts.firstOrNull { it.id == id }
            }
        }

    private val txFlow = accountId
        .filterNotNull()
        .flatMapLatest { id ->
            getTransactionsUseCase(TransactionQuery(accountId = id))
        }

    val uiState: StateFlow<AccountDetailUiState> = combine(
        accountFlow,
        txFlow,
        userRepository.userPreferences
    ) { account, tx, prefs ->
        val overrideStyle = account?.id?.let { prefs.accountCardStyleOverrides[it] }
        AccountDetailUiState(
            isLoading = false,
            account = account,
            transactions = tx.sortedByDescending { it.dateTime }.take(30),
            globalCardStyle = prefs.accountCardStyle,
            accountCardStyleOverride = overrideStyle
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailUiState()
    )

    fun load(id: String) {
        accountId.value = id
    }

    fun toggleArchive() {
        val account = uiState.value.account ?: return
        viewModelScope.launch {
            updateAccountUseCase(account.copy(isArchived = !account.isArchived))
        }
    }

    fun deleteAccount() {
        val account = uiState.value.account ?: return
        viewModelScope.launch {
            userRepository.setAccountCardStyleOverride(account.id, null)
            deleteAccountUseCase(account.id)
        }
    }

    fun setCardStyleOverride(style: AccountCardStyle?) {
        val account = uiState.value.account ?: return
        viewModelScope.launch {
            userRepository.setAccountCardStyleOverride(account.id, style)
        }
    }
}


