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

        val added = useCase(
            packageName = "com.google.android.apps.messaging",
            appLabel = "Messages",
            notificationTitle = "Debit alert",
            notificationText = "Acct 1234 debited with NGN 1200.00 for POS purchase at Uber",
            postedAt = 1234L,
            accountId = "wallet",
            fallbackCurrency = "NGN"
        )

        assertTrue(added)
        val drafts = repo.getPending().first()
        assertEquals(1, drafts.size)
        val draft = drafts.first()
        assertEquals(AutoCaptureDraftStatus.PENDING, draft.status)
        assertEquals(TransactionType.EXPENSE, draft.suggestedType)
        assertEquals(120000L, draft.amountCents)
        assertEquals("wallet", draft.accountId)
    }

    @Test
    fun `rejects otp notification`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = CaptureNotificationToDraftUseCase(repo)

        val added = useCase(
            packageName = "com.google.android.apps.messaging",
            appLabel = "Messages",
            notificationTitle = "OTP",
            notificationText = "Your verification code is 123456",
            postedAt = 1234L,
            accountId = "wallet",
            fallbackCurrency = "USD"
        )

        assertFalse(added)
        assertEquals(0, repo.getPending().first().size)
    }

    @Test
    fun `captures multilingual transfer-like notification as needs review`() = runTest {
        val repo = InMemoryAutoCaptureDraftRepository()
        val useCase = CaptureNotificationToDraftUseCase(repo)

        val added = useCase(
            packageName = "com.google.android.apps.messaging",
            appLabel = "Messages",
            notificationTitle = "Alerta de pago",
            notificationText = "Cuenta 9981 acreditado con MXN 200.00 recibido via transferencia",
            postedAt = 5678L,
            accountId = "main",
            fallbackCurrency = "MXN"
        )

        assertTrue(added)
        val draft = repo.getPending().first().first()
        assertEquals(AutoCaptureDraftStatus.NEEDS_REVIEW, draft.status)
        assertEquals(TransactionType.TRANSFER, draft.suggestedType)
        assertEquals(20000L, draft.amountCents)
    }
}
