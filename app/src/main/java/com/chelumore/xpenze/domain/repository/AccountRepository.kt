package com.chelumore.xpenze.domain.repository

import com.chelumore.xpenze.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAll(): Flow<List<Account>>
    fun getActive(): Flow<List<Account>>
    fun getById(id: String): Flow<Account?>
    fun getDefault(): Flow<Account?>
    fun getTotalBalance(): Flow<Long>
    
    suspend fun insert(account: Account)
    suspend fun update(account: Account)
    suspend fun delete(id: String)
    suspend fun setDefault(id: String)
    suspend fun updateBalance(id: String, newBalance: Long)
    suspend fun getUnsynced(): List<Account>
    suspend fun markSynced(id: String)
}

