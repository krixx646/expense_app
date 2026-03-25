package com.chelumore.xpenze.domain.usecase.export

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.testutil.InMemoryAccountRepository
import com.chelumore.xpenze.testutil.InMemoryCategoryRepository
import com.chelumore.xpenze.testutil.InMemoryTransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportToCsvUseCaseTest {

    @Test
    fun `exports header and rows sorted by date desc and escapes commas and quotes`() = runTest {
        val account = Account(
            id = "a1",
            name = "Wallet",
            type = AccountType.CASH,
            currency = "USD",
            initialBalanceCents = 0,
            currentBalanceCents = 0,
            colorHex = "#000000",
            iconName = "wallet",
            isDefault = true,
            createdAt = 0,
            updatedAt = 0
        )
        val category = Category(
            id = "c1",
            name = "Food",
            type = CategoryType.EXPENSE,
            parentCategoryId = null,
            colorHex = "#FFFFFF",
            iconName = "restaurant",
            isDefault = true,
            sortOrder = 0,
            createdAt = 0,
            updatedAt = 0
        )

        val newer = Transaction(
            id = "t2",
            accountId = "a1",
            categoryId = "c1",
            money = Money(amountCents = 12345, currency = "USD"),
            type = TransactionType.EXPENSE,
            title = "Coffee, \"latte\"",
            notes = "note",
            dateTime = 2_000L,
            receiptUri = "content://receipt",
            tags = listOf("food", "work"),
            createdAt = 0,
            updatedAt = 0
        )
        val older = newer.copy(id = "t1", title = "Older", dateTime = 1_000L)

        val txRepo = InMemoryTransactionRepository(listOf(older, newer))
        val accountRepo = InMemoryAccountRepository(listOf(account))
        val categoryRepo = InMemoryCategoryRepository(listOf(category))

        val useCase = ExportToCsvUseCase(
            transactionRepository = txRepo,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo
        )

        val csv = useCase()
        val lines = csv.trim().split('\n')
        assertTrue(lines.first().startsWith("Date,Type,Title,Amount,Currency,Account"))

        // Newer transaction should come first
        assertTrue(lines[1].contains("t2"))
        assertTrue(lines[2].contains("t1"))

        // Escaping for comma + quotes -> should be quoted and "" escaped
        assertTrue(lines[1].contains("\"Coffee, \"\"latte\"\"\""))

        // Amount formatting in decimals
        assertTrue(lines[1].contains(",123.45,USD,"))

        // Category name resolved
        assertTrue(lines[1].contains(",Food,"))

        // Tags joined with |
        assertTrue(lines[1].contains(",food|work,"))

        // Account name resolved
        assertTrue(lines[1].contains(",Wallet,"))

        // Header + 2 rows
        assertEquals(3, lines.size)
    }
}


