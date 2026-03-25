package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(query: String): Flow<List<Transaction>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return transactionRepository.getAll()
        return transactionRepository.search(trimmed)
    }
}


