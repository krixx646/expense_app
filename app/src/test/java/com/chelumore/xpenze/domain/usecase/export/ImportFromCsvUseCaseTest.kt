package com.chelumore.xpenze.domain.usecase.export

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.usecase.account.AddAccountUseCase
import com.chelumore.xpenze.domain.usecase.category.AddCategoryUseCase
import com.chelumore.xpenze.testutil.InMemoryAccountRepository
import com.chelumore.xpenze.testutil.InMemoryCategoryRepository
import com.chelumore.xpenze.testutil.InMemoryTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportFromCsvUseCaseTest {

    @Test
    fun `imports csv and creates missing accounts and categories`() = runTest {
        val accountRepo = InMemoryAccountRepository(emptyList())
        val categoryRepo = InMemoryCategoryRepository(emptyList())
        val txRepo = InMemoryTransactionRepository(emptyList())

        val addAccount = AddAccountUseCase(accountRepo)
        val addCategory = AddCategoryUseCase(categoryRepo)

        val useCase = ImportFromCsvUseCase(
            transactionRepository = txRepo,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            addAccountUseCase = addAccount,
            addCategoryUseCase = addCategory
        )

        val csv = """
            Date,Type,Title,Amount,Currency,Account,To Account,Category,Notes,Tags,Id
            2025-01-01 10:00:00,EXPENSE,Lunch,10.50,USD,Wallet,,Food,Note,food|work,tx1
        """.trimIndent()

        val result = useCase(csv)
        assertEquals(1, result.transactionsImported)
        assertEquals(1, result.accountsCreated)
        assertEquals(1, result.categoriesCreated)

        val accounts = accountRepo.getAll().first()
        assertEquals(1, accounts.size)
        assertEquals("Wallet", accounts.first().name)

        val cats = categoryRepo.getAll().first()
        assertEquals(1, cats.size)
        assertEquals("Food", cats.first().name)
        assertEquals(CategoryType.EXPENSE, cats.first().type)

        val importedTx = txRepo.getById("tx1").first()
        assertNotNull(importedTx)
        assertEquals(1050L, importedTx!!.money.amountCents)
        assertEquals(listOf("food", "work"), importedTx.tags)
        assertTrue(importedTx.title.isNotBlank())
    }

    @Test
    fun `re-importing same id replaces transaction`() = runTest {
        val existingAccount = Account(
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
        val existingCategory = Category(
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

        val accountRepo = InMemoryAccountRepository(listOf(existingAccount))
        val categoryRepo = InMemoryCategoryRepository(listOf(existingCategory))
        val txRepo = InMemoryTransactionRepository(emptyList())

        val useCase = ImportFromCsvUseCase(
            transactionRepository = txRepo,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            addAccountUseCase = AddAccountUseCase(accountRepo),
            addCategoryUseCase = AddCategoryUseCase(categoryRepo)
        )

        val csv1 = """
            Date,Type,Title,Amount,Currency,Account,To Account,Category,Notes,Tags,Id
            2025-01-01 10:00:00,EXPENSE,Old title,1.00,USD,Wallet,,Food,,a,tx1
        """.trimIndent()
        val csv2 = """
            Date,Type,Title,Amount,Currency,Account,To Account,Category,Notes,Tags,Id
            2025-01-01 10:00:00,EXPENSE,New title,2.00,USD,Wallet,,Food,,b,tx1
        """.trimIndent()

        useCase(csv1)
        useCase(csv2)

        val tx = txRepo.getById("tx1").first()
        assertNotNull(tx)
        assertEquals("New title", tx!!.title)
        assertEquals(200L, tx.money.amountCents)
        assertEquals(listOf("b"), tx.tags)
    }
}


