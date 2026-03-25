package com.chelumore.xpenze.data.repository

import com.chelumore.xpenze.core.database.dao.AutoCaptureDraftDao
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoCaptureDraftRepositoryImpl @Inject constructor(
    private val draftDao: AutoCaptureDraftDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AutoCaptureDraftRepository {

    override fun getPending(): Flow<List<AutoCaptureDraft>> =
        draftDao.getPending().map { drafts -> drafts.map { it.toDomain() } }

    override fun getPendingCount(): Flow<Int> = draftDao.getPendingCount()

    override fun getById(id: String): Flow<AutoCaptureDraft?> =
        draftDao.getById(id).map { it?.toDomain() }

    override suspend fun getByIdOnce(id: String): AutoCaptureDraft? = withContext(ioDispatcher) {
        draftDao.getByIdOnce(id)?.toDomain()
    }

    override suspend fun addOrIgnore(draft: AutoCaptureDraft): Boolean = withContext(ioDispatcher) {
        if (draftDao.getByFingerprint(draft.fingerprint) != null) return@withContext false
        draftDao.insert(draft.toEntity()) != -1L
    }

    override suspend fun addAllOrIgnore(drafts: List<AutoCaptureDraft>): Int = withContext(ioDispatcher) {
        var added = 0
        drafts.forEach { draft ->
            if (draftDao.getByFingerprint(draft.fingerprint) == null) {
                val result = draftDao.insert(draft.toEntity())
                if (result != -1L) added++
            }
        }
        added
    }

    override suspend fun markResolved(
        id: String,
        resolution: AutoCaptureResolution,
        createdTransactionId: String?
    ) = withContext(ioDispatcher) {
        draftDao.markResolved(
            id = id,
            resolution = resolution.name,
            createdTransactionId = createdTransactionId,
            updatedAt = System.currentTimeMillis()
        )
    }
}
