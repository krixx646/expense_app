package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("UPDATE accounts SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM accounts WHERE id = :id AND deleted = 0")
    fun getById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id AND deleted = 0")
    suspend fun getByIdOnce(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getByIdAnyOnce(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE deleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE deleted = 0 AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE deleted = 0 AND isArchived = 1 ORDER BY name ASC")
    fun getArchived(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE deleted = 0 AND isDefault = 1 LIMIT 1")
    fun getDefault(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE deleted = 0 AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultOnce(): AccountEntity?

    @Query("UPDATE accounts SET isDefault = 0, synced = 0, updatedAt = :updatedAt WHERE isDefault = 1")
    suspend fun clearDefault(updatedAt: Long)

    @Query("UPDATE accounts SET isDefault = 1, synced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDefault(id: String, updatedAt: Long)

    @Query("UPDATE accounts SET currentBalance = :newBalance WHERE id = :id")
    suspend fun updateBalance(id: String, newBalance: Long)

    @Query("SELECT * FROM accounts WHERE deleted = 0")
    suspend fun getAllAliveOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE synced = 0 AND deleted = 0")
    suspend fun getUnsynced(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE deleted = 1")
    suspend fun getDeleted(): List<AccountEntity>

    @Query("UPDATE accounts SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE accounts SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)

    // Aggregation
    @Query("SELECT COALESCE(SUM(currentBalance), 0) FROM accounts WHERE deleted = 0 AND isArchived = 0")
    fun getTotalBalance(): Flow<Long>

    @Query("SELECT COALESCE(SUM(currentBalance), 0) FROM accounts WHERE deleted = 0 AND isArchived = 0 AND currency = :currency")
    fun getTotalBalanceByCurrency(currency: String): Flow<Long>

    // Count
    @Query("SELECT COUNT(*) FROM accounts WHERE deleted = 0 AND isArchived = 0")
    fun getActiveCount(): Flow<Int>

    // Delete all (for testing/reset)
    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

