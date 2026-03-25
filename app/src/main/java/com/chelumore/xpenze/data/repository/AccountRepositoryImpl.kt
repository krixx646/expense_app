package com.chelumore.xpenze.data.repository

import com.chelumore.xpenze.core.common.currentTimeMillis
import com.chelumore.xpenze.core.database.dao.AccountDao
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AccountRepository {

    override fun getAll(): Flow<List<Account>> =
        accountDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getActive(): Flow<List<Account>> =
        accountDao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getById(id: String): Flow<Account?> =
        accountDao.getById(id).map { it?.toDomain() }

    override fun getDefault(): Flow<Account?> =
        accountDao.getDefault().map { it?.toDomain() }

    override fun getTotalBalance(): Flow<Long> = accountDao.getTotalBalance()

    override suspend fun insert(account: Account) = withContext(ioDispatcher) {
        accountDao.insert(account.toEntity())
        syncScheduler.scheduleNow()
    }

    override suspend fun update(account: Account) = withContext(ioDispatcher) {
        accountDao.update(account.copy(updatedAt = currentTimeMillis(), synced = false).toEntity())
        syncScheduler.scheduleNow()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        accountDao.softDelete(id, currentTimeMillis())
        syncScheduler.scheduleNow()
    }

    override suspend fun setDefault(id: String) = withContext(ioDispatcher) {
        val now = currentTimeMillis()
        accountDao.clearDefault(now)
        accountDao.setDefault(id, now)
        syncScheduler.scheduleNow()
    }

    override suspend fun updateBalance(id: String, newBalance: Long) = withContext(ioDispatcher) {
        accountDao.updateBalance(id, newBalance)
    }

    override suspend fun getUnsynced(): List<Account> = withContext(ioDispatcher) {
        accountDao.getUnsynced().map { it.toDomain() }
    }

    override suspend fun markSynced(id: String) = withContext(ioDispatcher) {
        accountDao.markSynced(id)
    }
}

