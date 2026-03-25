package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.TransactionEntity
import com.chelumore.xpenze.core.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM transactions WHERE id = :id AND deleted = 0")
    fun getById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id AND deleted = 0")
    suspend fun getByIdOnce(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getByIdAnyOnce(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY dateTime DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deleted = 0")
    suspend fun getAllAliveOnce(): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE deleted = 0 AND dateTime BETWEEN :startTime AND :endTime 
        ORDER BY dateTime DESC
    """)
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE deleted = 0 AND accountId = :accountId 
        ORDER BY dateTime DESC
    """)
    fun getByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE deleted = 0 AND categoryId = :categoryId 
        ORDER BY dateTime DESC
    """)
    fun getByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE deleted = 0 AND type = :type 
        ORDER BY dateTime DESC
    """)
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE synced = 0 AND deleted = 0")
    suspend fun getUnsynced(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE deleted = 1")
    suspend fun getDeleted(): List<TransactionEntity>

    @Query("UPDATE transactions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)

    @Query("""
        UPDATE transactions
        SET categoryId = :toId, synced = 0, updatedAt = :updatedAt
        WHERE deleted = 0 AND categoryId = :fromId
    """)
    suspend fun reassignCategory(fromId: String, toId: String, updatedAt: Long)

    // Aggregation queries
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE deleted = 0 AND type = 'EXPENSE' AND dateTime BETWEEN :startTime AND :endTime
    """)
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE deleted = 0 AND type = 'INCOME' AND dateTime BETWEEN :startTime AND :endTime
    """)
    fun getIncomeByDateRange(startTime: Long, endTime: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE deleted = 0 AND type = 'EXPENSE' 
        AND categoryId = :categoryId 
        AND dateTime BETWEEN :startTime AND :endTime
    """)
    fun getCategorySpending(categoryId: String, startTime: Long, endTime: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE deleted = 0 AND type = 'EXPENSE' 
        AND accountId = :accountId 
        AND dateTime BETWEEN :startTime AND :endTime
    """)
    fun getAccountSpending(accountId: String, startTime: Long, endTime: Long): Flow<Long>

    // Search
    @Query("""
        SELECT * FROM transactions 
        WHERE deleted = 0 AND (title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY dateTime DESC
    """)
    fun search(query: String): Flow<List<TransactionEntity>>

    // Recent transactions
    @Query("""
        SELECT * FROM transactions 
        WHERE deleted = 0 
        ORDER BY dateTime DESC 
        LIMIT :limit
    """)
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>

    // Count
    @Query("SELECT COUNT(*) FROM transactions WHERE deleted = 0")
    fun getCount(): Flow<Int>

    // Delete all (for testing/reset)
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

