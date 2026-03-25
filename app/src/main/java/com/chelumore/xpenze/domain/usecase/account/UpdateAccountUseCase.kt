package com.chelumore.xpenze.domain.usecase.account

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.repository.AccountRepository
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account) {
        accountRepository.update(account)
        if (account.isDefault) {
            accountRepository.setDefault(account.id)
        }
    }
}


