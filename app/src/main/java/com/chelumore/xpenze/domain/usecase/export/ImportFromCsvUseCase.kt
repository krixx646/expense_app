package com.chelumore.xpenze.domain.usecase.export

import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import com.chelumore.xpenze.domain.usecase.account.AddAccountUseCase
import com.chelumore.xpenze.domain.usecase.category.AddCategoryUseCase
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Imports transactions from a CSV string.
 *
 * This expects the export format produced by [ExportToCsvUseCase].
 */
data class CsvImportResult(
    val transactionsImported: Int,
    val accountsCreated: Int,
    val categoriesCreated: Int
)

class ImportFromCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val addAccountUseCase: AddAccountUseCase,
    private val addCategoryUseCase: AddCategoryUseCase
) {
    suspend operator fun invoke(csv: String): CsvImportResult {
        val lines = csv
            .splitToSequence('\n')
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()

        if (lines.isEmpty()) return CsvImportResult(0, 0, 0)

        val header = parseCsvLine(lines.first())
        if (header.isEmpty() || header.firstOrNull()?.equals("Date", ignoreCase = true) != true) {
            throw IllegalArgumentException("Invalid CSV format. Expected header row starting with 'Date'.")
        }

        val zone = ZoneId.systemDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

        val existingAccounts = accountRepository.getAll().first()
        val existingCategories = categoryRepository.getAll().first()

        val accountsByName = existingAccounts.associateBy { it.name.trim().lowercase() }.toMutableMap()
        val categoriesByKey = existingCategories
            .associateBy { (it.type.name + ":" + it.name.trim().lowercase()) }
            .toMutableMap()

        var accountsCreated = 0
        var categoriesCreated = 0
        var imported = 0

        // Map header names -> index
        val index = header.mapIndexed { i, h -> h.trim() to i }.toMap()

        fun value(row: List<String>, name: String): String {
            val i = index[name] ?: return ""
            return row.getOrNull(i).orEmpty()
        }

        for (line in lines.drop(1)) {
            val row = parseCsvLine(line)
            if (row.isEmpty()) continue

            val typeStr = value(row, "Type").trim()
            val txType = runCatching { TransactionType.valueOf(typeStr) }
                .getOrElse { throw IllegalArgumentException("Invalid transaction type: $typeStr") }

            val title = value(row, "Title").trim().ifBlank { "Imported" }

            val currency = value(row, "Currency").trim().ifBlank { "USD" }
            val amountDecimal = value(row, "Amount").trim()
            val amountCents = amountDecimal.toCents()
            if (amountCents <= 0L) continue

            val accountName = value(row, "Account").trim()
            if (accountName.isBlank()) {
                throw IllegalArgumentException("Missing Account name for row: $title")
            }
            val (accountId, accountCreated) = ensureAccount(accountsByName, accountName, currency)
            if (accountCreated) accountsCreated++

            val toAccountName = value(row, "To Account").trim()
            val toAccountId = if (txType == TransactionType.TRANSFER) {
                if (toAccountName.isBlank()) {
                    throw IllegalArgumentException("Missing To Account for transfer: $title")
                }
                val (toId, toCreated) = ensureAccount(accountsByName, toAccountName, currency)
                if (toCreated) accountsCreated++
                toId
            } else null

            val categoryName = value(row, "Category").trim()
            val categoryId = if (txType == TransactionType.TRANSFER || categoryName.isBlank()) {
                null
            } else {
                val catType = if (txType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                val (catId, catCreated) = ensureCategory(categoriesByKey, catType, categoryName)
                if (catCreated) categoriesCreated++
                catId
            }

            val notes = value(row, "Notes").takeIf { it.isNotBlank() }
            val tags = value(row, "Tags")
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val dateTime = runCatching {
                val dt = LocalDateTime.parse(value(row, "Date"), dateFormatter)
                dt.atZone(zone).toInstant().toEpochMilli()
            }.getOrElse { System.currentTimeMillis() }

            val id = value(row, "Id").trim().ifBlank { UUID.randomUUID().toString() }
            val now = System.currentTimeMillis()

            val tx = Transaction(
                id = id,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                money = Money(amountCents = amountCents, currency = currency),
                type = txType,
                title = title,
                notes = notes,
                dateTime = dateTime,
                receiptUri = null,
                isRecurring = false,
                recurringRuleId = null,
                tags = tags,
                synced = false,
                deleted = false,
                createdAt = now,
                updatedAt = now
            )

            transactionRepository.insert(tx)
            imported++
        }

        return CsvImportResult(
            transactionsImported = imported,
            accountsCreated = accountsCreated,
            categoriesCreated = categoriesCreated
        )
    }

    private suspend fun ensureAccount(
        accountsByName: MutableMap<String, com.chelumore.xpenze.domain.model.Account>,
        name: String,
        currency: String
    ): Pair<String, Boolean> {
        val key = name.trim().lowercase()
        val existing = accountsByName[key]
        if (existing != null) return existing.id to false

        val id = addAccountUseCase(
            name = name.trim(),
            type = AccountType.CASH,
            currency = currency,
            initialBalanceCents = 0L,
            isDefault = accountsByName.isEmpty()
        )

        // Refresh local cache entry
        val created = com.chelumore.xpenze.domain.model.Account(
            id = id,
            name = name.trim(),
            type = AccountType.CASH,
            currency = currency,
            initialBalanceCents = 0L,
            currentBalanceCents = 0L,
            colorHex = "#81C784",
            iconName = "wallet",
            isDefault = accountsByName.isEmpty(),
            isArchived = false,
            sortOrder = 0,
            synced = false,
            deleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        accountsByName[key] = created
        return id to true
    }

    private suspend fun ensureCategory(
        categoriesByKey: MutableMap<String, com.chelumore.xpenze.domain.model.Category>,
        type: CategoryType,
        name: String
    ): Pair<String, Boolean> {
        val key = "${type.name}:${name.trim().lowercase()}"
        val existing = categoriesByKey[key]
        if (existing != null) return existing.id to false

        val id = addCategoryUseCase(
            name = name.trim(),
            type = type,
            parentCategoryId = null,
            colorHex = "#F9D648",
            iconName = "category",
            sortOrder = 0,
            isDefault = false
        )

        // Refresh local cache entry
        val created = com.chelumore.xpenze.domain.model.Category(
            id = id,
            name = name.trim(),
            type = type,
            parentCategoryId = null,
            colorHex = "#F9D648",
            iconName = "category",
            isDefault = false,
            sortOrder = 0,
            synced = false,
            deleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        categoriesByKey[key] = created
        return id to true
    }

    private fun String.toCents(): Long {
        val clean = trim()
        if (clean.isBlank()) return 0L
        return runCatching {
            BigDecimal(clean)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrDefault(0L)
    }

    /**
     * Minimal CSV parser that supports quoted values and escaped quotes ("").
     */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}


