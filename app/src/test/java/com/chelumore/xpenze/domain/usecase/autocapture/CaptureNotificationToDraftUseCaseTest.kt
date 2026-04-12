package com.chelumore.xpenze.domain.usecase.autocapture

import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.testutil.InMemoryAutoCaptureDraftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureNotificationToDraftUseCaseTest {

    @Test
    fun `captures bank-like debit notification into pending expense draft`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = CaptureNotificationToDraftUseCase(repo)

        val draft = useCase(
            packageName = "com.google.android.apps.messaging",
            appLabel = "Messages",
            notificationTitle = "Debit alert",
            notificationText = "Acct 1234 debited with NGN 1200.00 for POS purchase at Uber",
            postedAt = 1234L,
            accountId = "wallet",
            fallbackCurrency = "NGN"
        )

        assertTrue(draft != null)
        val drafts = repo.getPending().first()
        assertEquals(1, drafts.size)
        val storedDraft = drafts.first()
        assertEquals(AutoCaptureDraftStatus.PENDING, storedDraft.status)
        assertEquals(TransactionType.EXPENSE, storedDraft.suggestedType)
        assertEquals(120000L, storedDraft.amountCents)
        assertEquals("wallet", storedDraft.accountId)
    }

    @Test
    fun `rejects otp notification`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = CaptureNotificationToDraftUseCase(repo)

        val draft = useCase(
            packageName = "com.google.android.apps.messaging",
            appLabel = "Messages",
            notificationTitle = "OTP",
            notificationText = "Your verification code is 123456",
            postedAt = 1234L,
            accountId = "wallet",
            fallbackCurrency = "USD"
        )

        assertFalse(draft != null)
        assertEquals(0, repo.getPending().first().size)
    }

    @Test
    fun `captures multilingual transfer-like notification as needs review`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = CaptureNotificationToDraftUseCase(repo)

        val draft = useCase(
            packageName = "com.google.android.apps.messaging",
            appLabel = "Messages",
            notificationTitle = "Alerta de pago",
            notificationText = "Cuenta 9981 acreditado con MXN 200.00 recibido via transferencia",
            postedAt = 5678L,
            accountId = "main",
            fallbackCurrency = "MXN"
        )

        assertTrue(draft != null)
        val storedDraft = repo.getPending().first().first()
        assertEquals(AutoCaptureDraftStatus.NEEDS_REVIEW, storedDraft.status)
        assertEquals(TransactionType.TRANSFER, storedDraft.suggestedType)
        assertEquals(20000L, storedDraft.amountCents)
    }

    @Test
    fun `captures debit notification even when available balance is present`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = CaptureNotificationToDraftUseCase(repo)

        val draft = useCase(
            packageName = "com.acme.mobile",
            appLabel = "Acme Bank",
            notificationTitle = "Debit alert",
            notificationText = "Acct 1234 debited with NGN 1200.00 at POS. Available balance NGN 9800.00",
            postedAt = 9876L,
            accountId = "wallet",
            fallbackCurrency = "NGN"
        )

        assertTrue(draft != null)
        val storedDraft = repo.getPending().first().first()
        assertEquals(AutoCaptureDraftStatus.PENDING, storedDraft.status)
        assertEquals(TransactionType.EXPENSE, storedDraft.suggestedType)
        assertEquals(120000L, storedDraft.amountCents)
    }
}
