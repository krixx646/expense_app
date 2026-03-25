package com.chelumore.xpenze.domain.usecase.account

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<Account>> {
        return if (includeArchived) accountRepository.getAll() else accountRepository.getActive()
    }
}


