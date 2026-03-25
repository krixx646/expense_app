package com.chelumore.xpenze.domain.usecase.account

import com.chelumore.xpenze.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAccountBalanceUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(accountId: String): Flow<Long> {
        require(accountId.isNotBlank()) { "accountId is required" }
        return accountRepository.getById(accountId).map { it?.currentBalanceCents ?: 0L }
    }
}


