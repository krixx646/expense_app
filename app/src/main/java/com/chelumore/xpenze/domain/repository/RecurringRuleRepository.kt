package com.chelumore.xpenze.domain.repository

import com.chelumore.xpenze.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

interface RecurringRuleRepository {
    fun getAll(): Flow<List<RecurringRule>>
    fun getActive(): Flow<List<RecurringRule>>
    fun getById(id: String): Flow<RecurringRule?>
    
    suspend fun insert(rule: RecurringRule)
    suspend fun update(rule: RecurringRule)
    suspend fun delete(id: String)
    suspend fun getDueRules(): List<RecurringRule>
    suspend fun updateNextOccurrence(id: String, nextOccurrence: Long, lastProcessed: Long)
    suspend fun setActive(id: String, isActive: Boolean)
    suspend fun getUnsynced(): List<RecurringRule>
    suspend fun markSynced(id: String)
}

