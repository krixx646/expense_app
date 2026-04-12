package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.testutil.InMemoryCategoryRepository
import com.chelumore.xpenze.testutil.InMemoryTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddTransactionUseCaseTest {

    @Test
    fun `expense save keeps matching expense category`() = runTest {
        val groceries = category(id = "expense-food", name = "Food & Dining", type = CategoryType.EXPENSE)
        val transactionRepository = InMemoryTransactionRepository()
        val categoryRepository = InMemoryCategoryRepository(listOf(groceries))
        val useCase = AddTransactionUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )

        val transactionId = useCase(
            accountId = "cash",
            categoryId = groceries.id,
            amountCents = 12_500L,
            currency = "NGN",
            type = TransactionType.EXPENSE,
            title = "Lunch"
        )

        val stored = transactionRepository.getById(transactionId).first()
        assertEquals(groceries.id, stored?.categoryId)
    }

    @Test
    fun `expense save remaps wrong type category to same named expense category`() = runTest {
        val wrongType = category(id = "income-other", name = "Other", type = CategoryType.INCOME)
        val correctType = category(id = "expense-other", name = "Other", type = CategoryType.EXPENSE)
        val transactionRepository = InMemoryTransactionRepository()
        val categoryRepository = InMemoryCategoryRepository(listOf(wrongType, correctType))
        val useCase = AddTransactionUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )

        val transactionId = useCase(
            accountId = "cash",
            categoryId = wrongType.id,
            amountCents = 8_000L,
            currency = "NGN",
            type = TransactionType.EXPENSE,
            title = "Snacks"
        )

        val stored = transactionRepository.getById(transactionId).first()
        assertEquals(correctType.id, stored?.categoryId)
    }

    @Test
    fun `expense save clears wrong type category when no matching expense category exists`() = runTest {
        val incomeBusiness = category(id = "income-business", name = "Business", type = CategoryType.INCOME)
        val transactionRepository = InMemoryTransactionRepository()
        val categoryRepository = InMemoryCategoryRepository(listOf(incomeBusiness))
        val useCase = AddTransactionUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )

        val transactionId = useCase(
            accountId = "cash",
            categoryId = incomeBusiness.id,
            amountCents = 25_000L,
            currency = "NGN",
            type = TransactionType.EXPENSE,
            title = "Office fuel"
        )

        val stored = transactionRepository.getById(transactionId).first()
        assertNull(stored?.categoryId)
    }

    private fun category(
        id: String,
        name: String,
        type: CategoryType
    ): Category = Category(
        id = id,
        name = name,
        type = type,
        colorHex = "#FFFFFF",
        iconName = "label",
        createdAt = 0L,
        updatedAt = 0L
    )
}
