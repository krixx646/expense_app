package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.AutoCaptureDraftEntity
import com.chelumore.xpenze.domain.model.AutoCaptureConfidence
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.AutoCaptureSource
import com.chelumore.xpenze.domain.model.TransactionType

fun AutoCaptureDraftEntity.toDomain(): AutoCaptureDraft {
    return AutoCaptureDraft(
        id = id,
        source = runCatching { AutoCaptureSource.valueOf(source) }
            .getOrDefault(AutoCaptureSource.STATEMENT_IMPORT),
        status = runCatching { AutoCaptureDraftStatus.valueOf(status) }
            .getOrDefault(AutoCaptureDraftStatus.NEEDS_REVIEW),
        confidence = runCatching { AutoCaptureConfidence.valueOf(confidence) }
            .getOrDefault(AutoCaptureConfidence.LOW),
        title = title,
        rawText = rawText,
        sourcePackage = sourcePackage,
        sourceLabel = sourceLabel,
        amountCents = amountCents,
        currency = currency,
        occurredAt = occurredAt,
        suggestedType = suggestedType?.let { value ->
            runCatching { TransactionType.valueOf(value) }.getOrNull()
        },
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        notes = notes,
        receiptUri = receiptUri,
        tags = tags,
        needsReviewReason = needsReviewReason,
        fingerprint = fingerprint,
        resolution = runCatching { AutoCaptureResolution.valueOf(resolution) }
            .getOrDefault(AutoCaptureResolution.NONE),
        createdTransactionId = createdTransactionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun AutoCaptureDraft.toEntity(): AutoCaptureDraftEntity {
    return AutoCaptureDraftEntity(
        id = id,
        source = source.name,
        status = status.name,
        confidence = confidence.name,
        title = title,
        rawText = rawText,
        sourcePackage = sourcePackage,
        sourceLabel = sourceLabel,
        amountCents = amountCents,
        currency = currency,
        occurredAt = occurredAt,
        suggestedType = suggestedType?.name,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        notes = notes,
        receiptUri = receiptUri,
        tags = tags,
        needsReviewReason = needsReviewReason,
        fingerprint = fingerprint,
        resolution = resolution.name,
        createdTransactionId = createdTransactionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
