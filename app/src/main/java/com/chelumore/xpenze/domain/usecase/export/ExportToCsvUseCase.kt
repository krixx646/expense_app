package com.chelumore.xpenze.domain.usecase.export

import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class ExportToCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(range: DateRange? = null): String {
        val transactions = loadTransactions(range)
        val accountsById = accountRepository.getAll().first().associateBy { it.id }
        val categoriesById = categoryRepository.getAll().first().associateBy { it.id }

        val zoneId = ZoneId.systemDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

        val sb = StringBuilder()
        sb.appendLine(
            listOf(
                "Date",
                "Type",
                "Title",
                "Amount",
                "Currency",
                "Account",
                "To Account",
                "Category",
                "Notes",
                "Tags",
                "Id"
            ).joinToString(",")
        )

        transactions
            .sortedByDescending { it.dateTime }
            .forEach { tx ->
                val accountName = accountsById[tx.accountId]?.name.orEmpty()
                val toAccountName = tx.toAccountId?.let { accountsById[it]?.name.orEmpty() }.orEmpty()
                val categoryName = tx.categoryId?.let { categoriesById[it]?.name.orEmpty() }.orEmpty()

                val localDateTime = Instant.ofEpochMilli(tx.dateTime).atZone(zoneId).toLocalDateTime()
                val date = localDateTime.format(dateFormatter)

                val amountDecimal = String.format(Locale.US, "%.2f", tx.money.amountCents / 100.0)
                val tags = tx.tags.joinToString("|")

                val row = listOf(
                    date,
                    tx.type.name,
                    tx.title,
                    amountDecimal,
                    tx.money.currency,
                    accountName,
                    toAccountName,
                    categoryName,
                    tx.notes.orEmpty(),
                    tags,
                    tx.id
                ).joinToString(",") { csvEscape(it) }

                sb.appendLine(row)
            }

        return sb.toString()
    }

    private suspend fun loadTransactions(range: DateRange?): List<Transaction> {
        return if (range == null) {
            transactionRepository.getAll().first()
        } else {
            transactionRepository.getByDateRange(range.startMillis, range.endMillis).first()
        }
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}


