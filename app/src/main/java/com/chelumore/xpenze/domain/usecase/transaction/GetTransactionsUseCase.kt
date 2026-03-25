package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class TransactionQuery(
    val dateRange: DateRange? = null,
    val type: TransactionType? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val searchQuery: String? = null
)

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(query: TransactionQuery = TransactionQuery()): Flow<List<Transaction>> {
        val base: Flow<List<Transaction>> = when {
            !query.searchQuery.isNullOrBlank() -> transactionRepository.search(query.searchQuery.trim())
            query.dateRange != null -> transactionRepository.getByDateRange(query.dateRange.startMillis, query.dateRange.endMillis)
            query.type != null -> transactionRepository.getByType(query.type)
            !query.accountId.isNullOrBlank() -> transactionRepository.getByAccount(query.accountId)
            !query.categoryId.isNullOrBlank() -> transactionRepository.getByCategory(query.categoryId)
            else -> transactionRepository.getAll()
        }

        return base.map { list ->
            list.asSequence()
                .filter { tx ->
                    val inRange = query.dateRange?.contains(tx.dateTime) ?: true
                    val typeOk = query.type?.let { it == tx.type } ?: true
                    val accountOk = query.accountId?.let { it == tx.accountId || it == tx.toAccountId } ?: true
                    val categoryOk = query.categoryId?.let { it == tx.categoryId } ?: true
                    inRange && typeOk && accountOk && categoryOk
                }
                .toList()
        }
    }
}


