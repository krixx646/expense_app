package com.chelumore.xpenze.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_capture_drafts",
    indices = [
        Index(value = ["status"]),
        Index(value = ["occurredAt"]),
        Index(value = ["fingerprint"], unique = true)
    ]
)
data class AutoCaptureDraftEntity(
    @PrimaryKey
    val id: String,
    val source: String,
    val status: String,
    val confidence: String,
    val title: String,
    val rawText: String? = null,
    val sourcePackage: String? = null,
    val sourceLabel: String? = null,
    val amountCents: Long? = null,
    val currency: String,
    val occurredAt: Long,
    val suggestedType: String? = null,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val notes: String? = null,
    val receiptUri: String? = null,
    val tags: List<String> = emptyList(),
    val needsReviewReason: String? = null,
    val fingerprint: String,
    val resolution: String,
    val createdTransactionId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
