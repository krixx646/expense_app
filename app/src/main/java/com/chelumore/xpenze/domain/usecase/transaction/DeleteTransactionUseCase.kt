package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: String) {
        require(id.isNotBlank()) { "id is required" }
        transactionRepository.delete(id)
    }
}


