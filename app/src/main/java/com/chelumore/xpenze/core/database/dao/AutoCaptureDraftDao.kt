package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.AutoCaptureDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoCaptureDraftDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(draft: AutoCaptureDraftEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(drafts: List<AutoCaptureDraftEntity>): List<Long>

    @Update
    suspend fun update(draft: AutoCaptureDraftEntity)

    @Query(
        """
        SELECT * FROM auto_capture_drafts
        WHERE status != 'RESOLVED'
        ORDER BY occurredAt DESC, createdAt DESC
        """
    )
    fun getPending(): Flow<List<AutoCaptureDraftEntity>>

    @Query("SELECT COUNT(*) FROM auto_capture_drafts WHERE status != 'RESOLVED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM auto_capture_drafts WHERE id = :id")
    fun getById(id: String): Flow<AutoCaptureDraftEntity?>

    @Query("SELECT * FROM auto_capture_drafts WHERE id = :id")
    suspend fun getByIdOnce(id: String): AutoCaptureDraftEntity?

    @Query("SELECT * FROM auto_capture_drafts WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): AutoCaptureDraftEntity?

    @Query(
        """
        UPDATE auto_capture_drafts
        SET status = 'RESOLVED',
            resolution = :resolution,
            createdTransactionId = :createdTransactionId,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markResolved(
        id: String,
        resolution: String,
        createdTransactionId: String?,
        updatedAt: Long
    )
}
