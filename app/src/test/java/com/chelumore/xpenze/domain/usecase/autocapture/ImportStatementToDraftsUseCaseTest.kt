package com.chelumore.xpenze.domain.usecase.autocapture

import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.testutil.InMemoryAutoCaptureDraftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportStatementToDraftsUseCaseTest {

    @Test
    fun `imports statement rows into expense and income drafts`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = ImportStatementToDraftsUseCase(repo)

        val csv = """
            Date,Description,Debit,Credit,Reference
            2025-02-01,Coffee Shop,10.50,,ref-1
            2025-02-02,Salary,,2500.00,ref-2
        """.trimIndent()

        val result = useCase(
            rawStatement = csv,
            accountId = "main",
            fallbackCurrency = "USD"
        )

        assertEquals(2, result.draftsCreated)
        assertEquals(0, result.rowsSkipped)

        val drafts = repo.getPending().first()
        assertEquals(2, drafts.size)
        assertTrue(drafts.any { it.title == "Coffee Shop" && it.suggestedType == TransactionType.EXPENSE })
        assertTrue(drafts.any { it.title == "Salary" && it.suggestedType == TransactionType.INCOME })
    }

    @Test
    fun `marks transfer-like statement rows as needing review and ignores duplicates`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = ImportStatementToDraftsUseCase(repo)

        val csv = """
            Date,Description,Amount,Type,Reference
            2025-02-03,Own account transfer,150.00,Transfer,ref-3
        """.trimIndent()

        val firstResult = useCase(
            rawStatement = csv,
            accountId = "main",
            fallbackCurrency = "USD"
        )
        val secondResult = useCase(
            rawStatement = csv,
            accountId = "main",
            fallbackCurrency = "USD"
        )

        assertEquals(1, firstResult.draftsCreated)
        assertEquals(0, secondResult.draftsCreated)

        val draft = repo.getPending().first().first()
        assertEquals(AutoCaptureDraftStatus.NEEDS_REVIEW, draft.status)
        assertEquals(TransactionType.TRANSFER, draft.suggestedType)
        assertEquals(15000L, draft.amountCents)
    }
}
