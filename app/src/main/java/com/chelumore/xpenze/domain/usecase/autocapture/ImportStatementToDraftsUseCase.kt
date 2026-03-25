package com.chelumore.xpenze.domain.usecase.autocapture

import com.chelumore.xpenze.core.autocapture.captureFingerprint
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSourceLabel
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatementAmbiguousReason
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatementFormatError
import com.chelumore.xpenze.domain.model.AutoCaptureConfidence
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.AutoCaptureSource
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class StatementImportResult(
    val draftsCreated: Int,
    val rowsSkipped: Int
)

class ImportStatementToDraftsUseCase @Inject constructor(
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository
) {

    suspend operator fun invoke(
        rawStatement: String,
        accountId: String?,
        fallbackCurrency: String
    ): StatementImportResult {
        val lines = rawStatement
            .splitToSequence('\n')
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return StatementImportResult(draftsCreated = 0, rowsSkipped = 0)

        val delimiter = detectDelimiter(lines.first())
        val header = parseLine(lines.first(), delimiter).map { it.trim() }
        val columns = resolveColumns(header)
            ?: throw IllegalArgumentException(
                localizedAutoCaptureStatementFormatError()
            )

        val drafts = mutableListOf<AutoCaptureDraft>()
        var skipped = 0
        for (line in lines.drop(1)) {
            val row = parseLine(line, delimiter)
            val parsed = parseRow(
                row = row,
                columns = columns,
                accountId = accountId,
                fallbackCurrency = fallbackCurrency
            )
            if (parsed == null) {
                skipped++
            } else {
                drafts += parsed
            }
        }

        val added = autoCaptureDraftRepository.addAllOrIgnore(drafts)
        return StatementImportResult(
            draftsCreated = added,
            rowsSkipped = skipped + (drafts.size - added)
        )
    }

    private fun parseRow(
        row: List<String>,
        columns: StatementColumns,
        accountId: String?,
        fallbackCurrency: String
    ): AutoCaptureDraft? {
        fun value(index: Int?): String = index?.let { row.getOrNull(it).orEmpty() }.orEmpty().trim()

        val rawTitle = listOf(
            value(columns.description),
            value(columns.merchant),
            value(columns.reference)
        ).firstOrNull { it.isNotBlank() }.orEmpty()
        if (rawTitle.isBlank()) return null

        val occurredAt = parseDate(value(columns.date)) ?: System.currentTimeMillis()
        val typeText = value(columns.type)
        val credit = value(columns.credit).toCentsOrNull()
        val debit = value(columns.debit).toCentsOrNull()
        val signedAmount = value(columns.amount).toSignedCentsOrNull()
        val absoluteAmount = value(columns.amount).toCentsOrNull()

        val inferredType = inferType(typeText = typeText, title = rawTitle)
        val (amountCents, suggestedType) = when {
            credit != null && credit > 0L -> credit to resolveSuggestedType(
                inferredType = inferredType,
                fallback = TransactionType.INCOME
            )
            debit != null && debit > 0L -> debit to resolveSuggestedType(
                inferredType = inferredType,
                fallback = TransactionType.EXPENSE
            )
            signedAmount != null && signedAmount < 0L -> signedAmount.absoluteValue() to resolveSuggestedType(
                inferredType = inferredType,
                fallback = TransactionType.EXPENSE
            )
            signedAmount != null && signedAmount > 0L -> signedAmount.absoluteValue() to resolveSuggestedType(
                inferredType = inferredType,
                fallback = TransactionType.INCOME
            )
            absoluteAmount != null && absoluteAmount > 0L -> {
                absoluteAmount to resolveSuggestedType(
                    inferredType = inferredType,
                    fallback = TransactionType.EXPENSE
                )
            }
            else -> return null
        }

        val notes = buildList {
            value(columns.reference).takeIf { it.isNotBlank() }?.let { add(it) }
            value(columns.type).takeIf { it.isNotBlank() }?.let { add(it) }
        }.joinToString(" • ").ifBlank { null }

        val status = if (suggestedType == TransactionType.TRANSFER) {
            AutoCaptureDraftStatus.NEEDS_REVIEW
        } else {
            AutoCaptureDraftStatus.PENDING
        }
        val confidence = if (columns.date != null) {
            AutoCaptureConfidence.HIGH
        } else {
            AutoCaptureConfidence.MEDIUM
        }
        val title = rawTitle.take(80)
        val rawText = row.joinToString(" | ")

        return AutoCaptureDraft(
            id = UUID.randomUUID().toString(),
            source = AutoCaptureSource.STATEMENT_IMPORT,
            status = status,
            confidence = confidence,
            title = title,
            rawText = rawText,
            sourceLabel = localizedAutoCaptureSourceLabel(AutoCaptureSource.STATEMENT_IMPORT),
            amountCents = amountCents,
            currency = fallbackCurrency.ifBlank { "USD" },
            occurredAt = occurredAt,
            suggestedType = suggestedType,
            accountId = accountId,
            notes = notes,
            needsReviewReason = if (status == AutoCaptureDraftStatus.NEEDS_REVIEW) {
                localizedAutoCaptureStatementAmbiguousReason()
            } else {
                null
            },
            fingerprint = captureFingerprint(
                AutoCaptureSource.STATEMENT_IMPORT.name,
                accountId,
                title,
                amountCents.toString(),
                occurredAt.toString(),
                rawText
            ),
            resolution = AutoCaptureResolution.NONE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun inferType(typeText: String, title: String): TransactionType {
        val sample = "$typeText $title".lowercase()
        return when {
            transferKeywords.any { sample.contains(it) } -> TransactionType.TRANSFER
            incomeKeywords.any { sample.contains(it) } -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
    }

    private fun resolveSuggestedType(
        inferredType: TransactionType,
        fallback: TransactionType
    ): TransactionType {
        return if (inferredType == TransactionType.TRANSFER) inferredType else fallback
    }

    private fun resolveColumns(header: List<String>): StatementColumns? {
        val normalized = header.map { it.normalizeHeader() }
        fun indexOfAny(vararg aliases: String): Int? {
            return normalized.indexOfFirst { column -> aliases.any { alias -> column == alias } }
                .takeIf { it >= 0 }
        }

        val date = indexOfAny("date", "transaction date", "posted date", "value date", "booking date")
        val description = indexOfAny("description", "details", "narration", "memo", "transaction", "particulars")
        val merchant = indexOfAny("merchant", "payee", "name")
        val amount = indexOfAny("amount", "transaction amount", "value", "amt")
        val debit = indexOfAny("debit", "withdrawal", "spent", "money out")
        val credit = indexOfAny("credit", "deposit", "received", "money in")
        val type = indexOfAny("type", "transaction type", "direction")
        val reference = indexOfAny("reference", "ref", "reference number", "transaction id")

        if (date == null || (description == null && merchant == null) || (amount == null && debit == null && credit == null)) {
            return null
        }

        return StatementColumns(
            date = date,
            description = description,
            merchant = merchant,
            amount = amount,
            debit = debit,
            credit = credit,
            type = type,
            reference = reference
        )
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == delimiter && !inQuotes -> {
                    out += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        out += current.toString()
        return out
    }

    private fun detectDelimiter(line: String): Char {
        val candidates = listOf(',', ';', '\t', '|')
        return candidates.maxByOrNull { candidate -> line.count { it == candidate } } ?: ','
    }

    private fun parseDate(raw: String): Long? {
        if (raw.isBlank()) return null
        val zone = ZoneId.systemDefault()
        val formats = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US),
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US)
        )
        formats.forEach { formatter ->
            runCatching {
                LocalDate.parse(raw.trim(), formatter).atStartOfDay(zone).toInstant().toEpochMilli()
            }.getOrNull()?.let { return it }
        }
        runCatching {
            LocalDateTime.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        return null
    }

    private fun String.normalizeHeader(): String {
        return lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.toCentsOrNull(): Long? {
        val clean = trim().replace(Regex("(?i)[^0-9.,-]"), "")
        if (clean.isBlank()) return null
        return runCatching {
            BigDecimal(clean.replace(",", ""))
                .abs()
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()
    }

    private fun String.toSignedCentsOrNull(): Long? {
        val clean = trim().replace(Regex("(?i)[^0-9.,-]"), "")
        if (clean.isBlank()) return null
        return runCatching {
            BigDecimal(clean.replace(",", ""))
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()
    }

    private fun Long.absoluteValue(): Long = kotlin.math.abs(this)

    private data class StatementColumns(
        val date: Int?,
        val description: Int?,
        val merchant: Int?,
        val amount: Int?,
        val debit: Int?,
        val credit: Int?,
        val type: Int?,
        val reference: Int?
    )

    private companion object {
        val incomeKeywords = setOf("credit", "deposit", "received", "salary", "refund")
        val transferKeywords = setOf("transfer", "own account", "internal transfer", "wallet transfer")
    }
}
