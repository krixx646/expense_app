package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM recurring_rules WHERE id = :id AND deleted = 0")
    fun getById(id: String): Flow<RecurringRuleEntity?>

    @Query("SELECT * FROM recurring_rules WHERE id = :id AND deleted = 0")
    suspend fun getByIdOnce(id: String): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getByIdAnyOnce(id: String): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules WHERE deleted = 0 ORDER BY title ASC")
    fun getAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE deleted = 0 AND isActive = 1 ORDER BY nextOccurrence ASC")
    fun getActive(): Flow<List<RecurringRuleEntity>>

    @Query("""
        SELECT * FROM recurring_rules 
        WHERE deleted = 0 AND isActive = 1 AND nextOccurrence <= :currentTime
        ORDER BY nextOccurrence ASC
    """)
    suspend fun getDueRules(currentTime: Long): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET nextOccurrence = :nextOccurrence, lastProcessed = :lastProcessed, synced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNextOccurrence(id: String, nextOccurrence: Long, lastProcessed: Long, updatedAt: Long)

    @Query("UPDATE recurring_rules SET isActive = :isActive, synced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean, updatedAt: Long)

    @Query("SELECT * FROM recurring_rules WHERE synced = 0 AND deleted = 0")
    suspend fun getUnsynced(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE deleted = 1")
    suspend fun getDeleted(): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE recurring_rules SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)

    @Query("""
        UPDATE recurring_rules
        SET categoryId = :toId, synced = 0, updatedAt = :updatedAt
        WHERE deleted = 0 AND categoryId = :fromId
    """)
    suspend fun reassignCategory(fromId: String, toId: String, updatedAt: Long)

    // Count
    @Query("SELECT COUNT(*) FROM recurring_rules WHERE deleted = 0 AND isActive = 1")
    fun getActiveCount(): Flow<Int>

    // Delete all (for testing/reset)
    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}

