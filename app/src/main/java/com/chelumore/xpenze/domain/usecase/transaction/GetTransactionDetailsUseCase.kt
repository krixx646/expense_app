package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionDetailsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(id: String): Flow<Transaction?> {
        require(id.isNotBlank()) { "id is required" }
        return transactionRepository.getById(id)
    }
}


