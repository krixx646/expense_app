package com.chelumore.xpenze.domain.repository

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetWithProgress
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAll(): Flow<List<Budget>>
    fun getActive(): Flow<List<Budget>>
    fun getById(id: String): Flow<Budget?>
    fun getByCategory(categoryId: String): Flow<List<Budget>>
    fun getCurrentBudgets(): Flow<List<Budget>>

    /**
     * Derived data: budgets with computed progress/spent from transactions.
     */
    fun getAllWithProgress(): Flow<List<BudgetWithProgress>>
    fun getActiveWithProgress(): Flow<List<BudgetWithProgress>>
    
    suspend fun insert(budget: Budget)
    suspend fun update(budget: Budget)
    suspend fun delete(id: String)
    suspend fun setActive(id: String, isActive: Boolean)
    suspend fun getUnsynced(): List<Budget>
    suspend fun markSynced(id: String)
}

