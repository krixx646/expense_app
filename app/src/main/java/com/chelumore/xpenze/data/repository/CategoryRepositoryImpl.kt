package com.chelumore.xpenze.data.repository

import com.chelumore.xpenze.core.common.currentTimeMillis
import com.chelumore.xpenze.core.database.dao.CategoryDao
import com.chelumore.xpenze.core.database.entity.CategoryEntity
import com.chelumore.xpenze.core.database.entity.CategoryType as EntityCategoryType
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByType(type: CategoryType): Flow<List<Category>> =
        categoryDao.getByType(EntityCategoryType.valueOf(type.name))
            .map { list -> list.map { it.toDomain() } }

    override fun getById(id: String): Flow<Category?> =
        categoryDao.getById(id).map { it?.toDomain() }

    override fun getSubcategories(parentId: String): Flow<List<Category>> =
        categoryDao.getSubcategories(parentId).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(category: Category) = withContext(ioDispatcher) {
        categoryDao.insert(category.toEntity())
        syncScheduler.scheduleNow()
    }

    override suspend fun update(category: Category) = withContext(ioDispatcher) {
        categoryDao.update(category.copy(updatedAt = currentTimeMillis(), synced = false).toEntity())
        syncScheduler.scheduleNow()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        val existing = categoryDao.getByIdOnce(id) ?: return@withContext
        if (existing.isDefault) {
            throw IllegalStateException("Default categories cannot be deleted.")
        }
        categoryDao.softDelete(id, currentTimeMillis())
        syncScheduler.scheduleNow()
    }

    override suspend fun seedDefaultCategories() = withContext(ioDispatcher) {
        val existingCount = categoryDao.getCount()
        if (existingCount > 0) return@withContext

        val now = currentTimeMillis()
        val defaultCategories = buildDefaultCategories(now)
        categoryDao.insertAll(defaultCategories)
        syncScheduler.scheduleNow()
    }

    override suspend fun getUnsynced(): List<Category> = withContext(ioDispatcher) {
        categoryDao.getUnsynced().map { it.toDomain() }
    }

    override suspend fun markSynced(id: String) = withContext(ioDispatcher) {
        categoryDao.markSynced(id)
    }

    private fun buildDefaultCategories(timestamp: Long): List<CategoryEntity> {
        val expenseCategories = listOf(
            Triple("Food & Dining", "#FF6B6B", "restaurant"),
            Triple("Transportation", "#4ECDC4", "directions_car"),
            Triple("Shopping", "#45B7D1", "shopping_bag"),
            Triple("Entertainment", "#96CEB4", "movie"),
            Triple("Bills & Utilities", "#FFEAA7", "receipt"),
            Triple("Health & Fitness", "#DDA0DD", "fitness_center"),
            Triple("Travel", "#98D8C8", "flight"),
            Triple("Education", "#F7DC6F", "school"),
            Triple("Personal Care", "#BB8FCE", "spa"),
            Triple("Gifts & Donations", "#F1948A", "card_giftcard"),
            Triple("Home", "#85C1E9", "home"),
            Triple("Insurance", "#73C6B6", "security"),
            Triple("Subscriptions", "#F8B500", "subscriptions"),
            Triple("Other", "#BDC3C7", "more_horiz")
        )

        val incomeCategories = listOf(
            Triple("Salary", "#2ECC71", "work"),
            Triple("Freelance", "#3498DB", "laptop"),
            Triple("Investment", "#9B59B6", "trending_up"),
            Triple("Business", "#E74C3C", "business"),
            Triple("Rental Income", "#1ABC9C", "apartment"),
            Triple("Gifts Received", "#E91E63", "redeem"),
            Triple("Refunds", "#00BCD4", "replay"),
            Triple("Other Income", "#95A5A6", "attach_money")
        )

        val allCategories = mutableListOf<CategoryEntity>()

        expenseCategories.forEachIndexed { index, (name, color, icon) ->
            allCategories.add(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = EntityCategoryType.EXPENSE,
                    parentCategoryId = null,
                    colorHex = color,
                    iconName = icon,
                    isDefault = true,
                    sortOrder = index,
                    synced = false,
                    deleted = false,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }

        incomeCategories.forEachIndexed { index, (name, color, icon) ->
            allCategories.add(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = EntityCategoryType.INCOME,
                    parentCategoryId = null,
                    colorHex = color,
                    iconName = icon,
                    isDefault = true,
                    sortOrder = index,
                    synced = false,
                    deleted = false,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }

        return allCategories
    }
}

