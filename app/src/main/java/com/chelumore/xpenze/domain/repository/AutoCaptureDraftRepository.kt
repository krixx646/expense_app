package com.chelumore.xpenze.domain.repository

import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import kotlinx.coroutines.flow.Flow

interface AutoCaptureDraftRepository {
    fun getPending(): Flow<List<AutoCaptureDraft>>
    fun getPendingCount(): Flow<Int>
    fun getById(id: String): Flow<AutoCaptureDraft?>
    suspend fun getByIdOnce(id: String): AutoCaptureDraft?
    suspend fun addOrIgnore(draft: AutoCaptureDraft): Boolean
    suspend fun addAllOrIgnore(drafts: List<AutoCaptureDraft>): Int
    suspend fun markResolved(
        id: String,
        resolution: AutoCaptureResolution,
        createdTransactionId: String? = null
    )
}
