package com.chelumore.xpenze.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain-level abstraction for cloud sync.
 *
 * NOTE: This is an interface only. Implementations (e.g., Firestore) are part of Phase 14.
 */
interface SyncRepository {

    val syncState: Flow<SyncState>

    suspend fun syncTransactions()
    suspend fun syncAccounts()
    suspend fun syncCategories()
    suspend fun syncBudgets()

    suspend fun fullSync()
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val lastSyncTimeMillis: Long) : SyncState
    data class Error(val message: String, val throwable: Throwable? = null) : SyncState
}


