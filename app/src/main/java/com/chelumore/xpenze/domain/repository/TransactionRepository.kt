package com.chelumore.xpenze.domain.repository

import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getById(id: String): Flow<Transaction?>
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun getByAccount(accountId: String): Flow<List<Transaction>>
    fun getByCategory(categoryId: String): Flow<List<Transaction>>
    fun getByType(type: TransactionType): Flow<List<Transaction>>
    fun getRecent(limit: Int): Flow<List<Transaction>>
    fun search(query: String): Flow<List<Transaction>>
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<Long>
    fun getIncomeByDateRange(startTime: Long, endTime: Long): Flow<Long>
    fun getCategorySpending(categoryId: String, startTime: Long, endTime: Long): Flow<Long>
    
    suspend fun insert(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
    suspend fun getUnsynced(): List<Transaction>
    suspend fun markSynced(id: String)
}

