package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        transactionRepository.update(transaction)
    }
}


