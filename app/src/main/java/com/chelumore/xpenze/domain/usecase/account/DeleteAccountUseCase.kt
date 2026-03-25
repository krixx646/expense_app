package com.chelumore.xpenze.domain.usecase.account

import com.chelumore.xpenze.domain.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(id: String) {
        require(id.isNotBlank()) { "id is required" }
        accountRepository.delete(id)
    }
}


