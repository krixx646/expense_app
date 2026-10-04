package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("UPDATE budgets SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM budgets WHERE id = :id AND deleted = 0")
    fun getById(id: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE id = :id AND deleted = 0")
    suspend fun getByIdOnce(id: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getByIdAnyOnce(id: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE deleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE deleted = 0")
    suspend fun getAllAliveOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE deleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE deleted = 0 AND categoryId = :categoryId")
    fun getByCategory(categoryId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE deleted = 0 AND accountId = :accountId")
    fun getByAccount(accountId: String): Flow<List<BudgetEntity>>

    @Query("""
        SELECT * FROM budgets 
        WHERE deleted = 0 AND isActive = 1 
        AND (startDate <= :currentTime) 
        AND (endDate IS NULL OR endDate >= :currentTime)
        ORDER BY endDate ASC, name ASC
    """)
    fun getCurrentBudgets(currentTime: Long): Flow<List<BudgetEntity>>

    @Query("""
        SELECT * FROM budgets
        WHERE deleted = 0 AND isActive = 1
        AND endDate IS NOT NULL
        AND endDate < :currentTime
        ORDER BY endDate DESC, name ASC
    """)
    fun getPastBudgets(currentTime: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE synced = 0 AND deleted = 0")
    suspend fun getUnsynced(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE deleted = 1")
    suspend fun getDeleted(): List<BudgetEntity>

    @Query("UPDATE budgets SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE budgets SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)

    @Query("""
        UPDATE budgets
        SET categoryId = :toId, synced = 0, updatedAt = :updatedAt
        WHERE deleted = 0 AND categoryId = :fromId
    """)
    suspend fun reassignCategory(fromId: String, toId: String, updatedAt: Long)

    @Query("UPDATE budgets SET isActive = :isActive, synced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean, updatedAt: Long)

    // Count
    @Query("SELECT COUNT(*) FROM budgets WHERE deleted = 0 AND isActive = 1")
    fun getActiveCount(): Flow<Int>

    // Delete all (for testing/reset)
    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

