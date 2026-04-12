package com.chelumore.xpenze.data.repository
import androidx.room.withTransaction
import com.chelumore.xpenze.core.common.currentTimeMillis
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.database.dao.AccountDao
import com.chelumore.xpenze.core.database.dao.TransactionDao
import com.chelumore.xpenze.core.database.entity.TransactionEntity
import com.chelumore.xpenze.core.database.entity.TransactionType as EntityTransactionType
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> =
        transactionDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getById(id: String): Flow<Transaction?> =
        transactionDao.getById(id).map { it?.toDomain() }

    override fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getByAccount(accountId: String): Flow<List<Transaction>> =
        transactionDao.getByAccount(accountId).map { list -> list.map { it.toDomain() } }

    override fun getByCategory(categoryId: String): Flow<List<Transaction>> =
        transactionDao.getByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getByType(EntityTransactionType.valueOf(type.name))
            .map { list -> list.map { it.toDomain() } }

    override fun getRecent(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Transaction>> =
        transactionDao.search(query).map { list -> list.map { it.toDomain() } }

    override fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<Long> =
        transactionDao.getExpensesByDateRange(startTime, endTime)

    override fun getIncomeByDateRange(startTime: Long, endTime: Long): Flow<Long> =
        transactionDao.getIncomeByDateRange(startTime, endTime)

    override fun getCategorySpending(categoryId: String, startTime: Long, endTime: Long): Flow<Long> =
        transactionDao.getCategorySpending(categoryId, startTime, endTime)

    override suspend fun insert(transaction: Transaction) = withContext(ioDispatcher) {
        appDatabase.withTransaction {
            val entity = transaction.toEntity()
            transactionDao.insert(entity)
            applyAccountDeltas(deltasFor(entity))
        }
        syncScheduler.scheduleNow()
    }

    override suspend fun update(transaction: Transaction) = withContext(ioDispatcher) {
        appDatabase.withTransaction {
            val now = currentTimeMillis()
            val oldEntity = transactionDao.getByIdAnyOnce(transaction.id)
            val newEntity = transaction.copy(updatedAt = now, synced = false).toEntity()
            transactionDao.update(newEntity)
            val oldDeltas = oldEntity?.takeIf { !it.deleted }?.let(::deltasFor).orEmpty()
            val newDeltas = deltasFor(newEntity)
            applyAccountDeltas(diffDeltas(oldDeltas, newDeltas))
        }
        syncScheduler.scheduleNow()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        appDatabase.withTransaction {
            val now = currentTimeMillis()
            val existing = transactionDao.getByIdAnyOnce(id)
            transactionDao.softDelete(id, now)
            val reverse = existing
                ?.takeIf { !it.deleted }
                ?.let(::deltasFor)
                ?.mapValues { (_, value) -> -value }
                .orEmpty()
            applyAccountDeltas(reverse)
        }
        syncScheduler.scheduleNow()
    }

    override suspend fun getUnsynced(): List<Transaction> = withContext(ioDispatcher) {
        transactionDao.getUnsynced().map { it.toDomain() }
    }

    override suspend fun markSynced(id: String) = withContext(ioDispatcher) {
        transactionDao.markSynced(id)
    }

    private fun deltasFor(tx: TransactionEntity): Map<String, Long> {
        val amount = tx.amount
        val deltas = mutableMapOf<String, Long>()
        fun add(accountId: String?, delta: Long) {
            if (accountId.isNullOrBlank()) return
            deltas[accountId] = (deltas[accountId] ?: 0L) + delta
        }

        when (tx.type) {
            EntityTransactionType.INCOME -> add(tx.accountId, amount)
            EntityTransactionType.EXPENSE -> add(tx.accountId, -amount)
            EntityTransactionType.TRANSFER -> {
                add(tx.accountId, -amount)
                add(tx.toAccountId, amount)
            }
        }

        return deltas
    }

    private fun diffDeltas(old: Map<String, Long>, new: Map<String, Long>): Map<String, Long> {
        if (old.isEmpty()) return new
        if (new.isEmpty()) return old.mapValues { (_, v) -> -v }

        val keys = old.keys + new.keys
        val out = mutableMapOf<String, Long>()
        for (k in keys) {
            val delta = (new[k] ?: 0L) - (old[k] ?: 0L)
            if (delta != 0L) out[k] = delta
        }
        return out
    }

    private suspend fun applyAccountDeltas(deltas: Map<String, Long>) {
        if (deltas.isEmpty()) return
        for ((accountId, delta) in deltas) {
            if (delta == 0L) continue
            val account = accountDao.getByIdOnce(accountId) ?: continue
            val newBalance = account.currentBalance + delta
            accountDao.updateBalance(accountId, newBalance)
        }
    }
}

