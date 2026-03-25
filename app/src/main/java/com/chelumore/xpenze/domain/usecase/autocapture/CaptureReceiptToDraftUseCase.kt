package com.chelumore.xpenze.domain.usecase.autocapture

import android.content.Context
import android.net.Uri
import com.chelumore.xpenze.core.autocapture.captureFingerprint
import com.chelumore.xpenze.core.autocapture.extractFirstDateMillis
import com.chelumore.xpenze.core.autocapture.extractMoneyCandidates
import com.chelumore.xpenze.core.autocapture.normalizeCaptureText
import com.chelumore.xpenze.core.autocapture.selectBestMoneyCandidate
import com.chelumore.xpenze.core.localization.localizedAutoCaptureOcrAmbiguousReason
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSourceLabel
import com.chelumore.xpenze.domain.model.AutoCaptureConfidence
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.AutoCaptureSource
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CaptureReceiptToDraftUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository
) {

    suspend operator fun invoke(
        imageUri: String,
        accountId: String?,
        fallbackCurrency: String
    ): Boolean {
        val uri = Uri.parse(imageUri)
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = try {
            recognizer.process(image).await()
        } finally {
            recognizer.close()
        }

        val lines = result.textBlocks
            .flatMap { block -> block.lines }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
        val fullText = result.text.trim()
        if (fullText.isBlank()) return false

        val amounts = extractMoneyCandidates(fullText, fallbackCurrency)
        val primaryAmount = selectBestMoneyCandidate(fullText, amounts)

        val title = lines.firstOrNull { line ->
            val lowered = line.lowercase()
            line.length >= 3 &&
                lowered.none { it.isDigit() } &&
                !lowered.contains("total") &&
                !lowered.contains("amount") &&
                !lowered.contains("receipt")
        } ?: lines.firstOrNull().orEmpty().take(80)

        if (title.isBlank()) return false

        val sample = normalizeCaptureText(fullText)
        val suggestedType = when {
            incomeKeywords.any { sample.contains(it) } -> TransactionType.INCOME
            transferKeywords.any { sample.contains(it) } -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        val status = when {
            primaryAmount == null -> AutoCaptureDraftStatus.NEEDS_REVIEW
            suggestedType == TransactionType.TRANSFER -> AutoCaptureDraftStatus.NEEDS_REVIEW
            else -> AutoCaptureDraftStatus.PENDING
        }
        val confidence = when {
            primaryAmount != null && title.isNotBlank() -> AutoCaptureConfidence.MEDIUM
            else -> AutoCaptureConfidence.LOW
        }

        val now = System.currentTimeMillis()
        val draft = AutoCaptureDraft(
            id = java.util.UUID.randomUUID().toString(),
            source = AutoCaptureSource.OCR,
            status = status,
            confidence = confidence,
            title = title,
            rawText = fullText,
            sourceLabel = localizedAutoCaptureSourceLabel(AutoCaptureSource.OCR),
            amountCents = primaryAmount?.amountCents,
            currency = primaryAmount?.currency ?: fallbackCurrency,
            occurredAt = extractFirstDateMillis(fullText) ?: now,
            suggestedType = suggestedType,
            accountId = accountId,
            notes = if (status == AutoCaptureDraftStatus.NEEDS_REVIEW) fullText else null,
            receiptUri = imageUri,
            needsReviewReason = if (status == AutoCaptureDraftStatus.NEEDS_REVIEW) {
                localizedAutoCaptureOcrAmbiguousReason()
            } else {
                null
            },
            fingerprint = captureFingerprint(
                AutoCaptureSource.OCR.name,
                accountId,
                title,
                primaryAmount?.amountCents?.toString(),
                fullText.take(280)
            ),
            resolution = AutoCaptureResolution.NONE,
            createdAt = now,
            updatedAt = now
        )
        return autoCaptureDraftRepository.addOrIgnore(draft)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        val incomeKeywords = setOf(
            "refund",
            "credited",
            "received",
            "deposit",
            "ingreso",
            "reembolso",
            "credito",
            "credito",
            "recebido",
            "remboursement",
            "gutschrift",
            "ايداع",
            "استلام",
            "जमा",
            "प्राप्त",
            "入金",
            "返金",
            "kupokea",
            "rejesho",
            "入账",
            "退款"
        )
        val transferKeywords = setOf(
            "transfer",
            "wallet",
            "bank transfer",
            "transferencia",
            "virement",
            "uberweisung",
            "تحويل",
            "ट्रांसफर",
            "振込",
            "hamisho",
            "转账"
        )
    }
}
