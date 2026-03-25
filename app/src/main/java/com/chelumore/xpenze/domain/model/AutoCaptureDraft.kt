package com.chelumore.xpenze.domain.model

enum class AutoCaptureSource {
    STATEMENT_IMPORT,
    OCR,
    NOTIFICATION
}

enum class AutoCaptureDraftStatus {
    PENDING,
    NEEDS_REVIEW,
    RESOLVED
}

enum class AutoCaptureConfidence {
    LOW,
    MEDIUM,
    HIGH
}

enum class AutoCaptureResolution {
    NONE,
    SAVED_AS_EXPENSE,
    SAVED_AS_INCOME,
    SAVED_AS_TRANSFER,
    SAVED_AS_REIMBURSEMENT,
    PASS_THROUGH,
    EXCLUDED
}

data class AutoCaptureDraft(
    val id: String,
    val source: AutoCaptureSource,
    val status: AutoCaptureDraftStatus,
    val confidence: AutoCaptureConfidence,
    val title: String,
    val rawText: String? = null,
    val sourcePackage: String? = null,
    val sourceLabel: String? = null,
    val amountCents: Long? = null,
    val currency: String,
    val occurredAt: Long,
    val suggestedType: TransactionType? = null,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val notes: String? = null,
    val receiptUri: String? = null,
    val tags: List<String> = emptyList(),
    val needsReviewReason: String? = null,
    val fingerprint: String,
    val resolution: AutoCaptureResolution = AutoCaptureResolution.NONE,
    val createdTransactionId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
