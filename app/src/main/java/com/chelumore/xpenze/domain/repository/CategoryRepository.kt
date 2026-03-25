package com.chelumore.xpenze.domain.repository

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    fun getByType(type: CategoryType): Flow<List<Category>>
    fun getById(id: String): Flow<Category?>
    fun getSubcategories(parentId: String): Flow<List<Category>>
    
    suspend fun insert(category: Category)
    suspend fun update(category: Category)
    suspend fun delete(id: String)
    suspend fun seedDefaultCategories()
    suspend fun getUnsynced(): List<Category>
    suspend fun markSynced(id: String)
}

