package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.CategoryEntity
import com.chelumore.xpenze.core.database.entity.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM categories WHERE id = :id AND deleted = 0")
    fun getById(id: String): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :id AND deleted = 0")
    suspend fun getByIdOnce(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getByIdAnyOnce(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deleted = 0")
    suspend fun getAllAliveOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE deleted = 0 AND type = :type ORDER BY sortOrder ASC, name ASC")
    fun getByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deleted = 0 AND parentCategoryId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun getSubcategories(parentId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deleted = 0 AND parentCategoryId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deleted = 0 AND isDefault = 1")
    fun getDefaultCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE synced = 0 AND deleted = 0")
    suspend fun getUnsynced(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE deleted = 1")
    suspend fun getDeleted(): List<CategoryEntity>

    @Query("UPDATE categories SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE categories SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)

    @Query("""
        UPDATE categories
        SET parentCategoryId = :toId, synced = 0, updatedAt = :updatedAt
        WHERE deleted = 0 AND parentCategoryId = :fromId
    """)
    suspend fun reassignParentCategory(fromId: String, toId: String, updatedAt: Long)

    // Check if categories exist (for seeding)
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    // Delete all (for testing/reset)
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

