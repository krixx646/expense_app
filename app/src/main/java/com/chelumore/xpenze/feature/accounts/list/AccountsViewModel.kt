package com.chelumore.xpenze.feature.accounts.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getAccountsUseCase: GetAccountsUseCase,
    userRepository: UserRepository
) : ViewModel() {

    private val showArchived = MutableStateFlow(false)

    private val allAccountsFlow = getAccountsUseCase(includeArchived = true)

    val uiState: StateFlow<AccountsUiState> = combine(
        allAccountsFlow,
        showArchived,
        userRepository.userPreferences
    ) { accounts, showArchived, prefs ->
        val totalBalance = accounts
            .filter { !it.isArchived }
            .sumOf { it.currentBalanceCents }

        AccountsUiState(
            isLoading = false,
            showArchived = showArchived,
            totalBalanceCents = totalBalance,
            accountCardStyle = prefs.accountCardStyle,
            accountCardStyleOverrides = prefs.accountCardStyleOverrides,
            accounts = accounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    fun setShowArchived(show: Boolean) {
        showArchived.update { show }
    }
}


