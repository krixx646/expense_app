package com.chelumore.xpenze.domain.usecase.autocapture

import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.localization.localizedAutoCaptureDefaultTitle
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.chelumore.xpenze.domain.usecase.transaction.AddTransactionUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class ResolveAutoCaptureDraftResult(
    val draft: AutoCaptureDraft,
    val resolution: AutoCaptureResolution,
    val createdTransactionId: String? = null
)

sealed class ResolveAutoCaptureDraftException(message: String) : IllegalStateException(message) {
    data object DraftMissing : ResolveAutoCaptureDraftException("draft_missing")
    data object ConfirmAmountFirst : ResolveAutoCaptureDraftException("confirm_amount_first")
    data object ChooseAccountFirst : ResolveAutoCaptureDraftException("choose_account_first")
    data object ChooseDestinationFirst : ResolveAutoCaptureDraftException("choose_destination_first")
}

class ResolveAutoCaptureDraftUseCase @Inject constructor(
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val preferencesDataStore: PreferencesDataStore
) {

    suspend fun saveAsTransaction(
        draftId: String,
        type: TransactionType,
        resolution: AutoCaptureResolution
    ): ResolveAutoCaptureDraftResult {
        val draft = requireDraft(draftId)
        val amount = draft.amountCents
        if (amount == null || amount <= 0L) {
            throw ResolveAutoCaptureDraftException.ConfirmAmountFirst
        }

        val prefs = preferencesDataStore.userPreferences.first()
        val resolvedAccountId = draft.accountId
            ?: prefs.defaultAccountId
            ?: accountRepository.getActive().first().firstOrNull()?.id
        if (resolvedAccountId.isNullOrBlank()) {
            throw ResolveAutoCaptureDraftException.ChooseAccountFirst
        }
        if (type == TransactionType.TRANSFER && draft.toAccountId.isNullOrBlank()) {
            throw ResolveAutoCaptureDraftException.ChooseDestinationFirst
        }

        val transactionId = addTransactionUseCase(
            accountId = resolvedAccountId,
            toAccountId = if (type == TransactionType.TRANSFER) draft.toAccountId else null,
            categoryId = if (type == TransactionType.TRANSFER) null else draft.categoryId,
            amountCents = amount,
            currency = draft.currency.ifBlank { prefs.defaultCurrency },
            type = type,
            title = draft.title.ifBlank { localizedAutoCaptureDefaultTitle() },
            notes = draft.notes,
            dateTime = draft.occurredAt,
            receiptUri = draft.receiptUri,
            tags = draft.tags.distinct()
        )
        autoCaptureDraftRepository.markResolved(
            id = draftId,
            resolution = resolution,
            createdTransactionId = transactionId
        )
        return ResolveAutoCaptureDraftResult(
            draft = draft,
            resolution = resolution,
            createdTransactionId = transactionId
        )
    }

    suspend fun markResolved(
        draftId: String,
        resolution: AutoCaptureResolution
    ): ResolveAutoCaptureDraftResult {
        val draft = requireDraft(draftId)
        autoCaptureDraftRepository.markResolved(draftId, resolution)
        return ResolveAutoCaptureDraftResult(
            draft = draft,
            resolution = resolution
        )
    }

    private suspend fun requireDraft(draftId: String): AutoCaptureDraft {
        return autoCaptureDraftRepository.getByIdOnce(draftId)
            ?: throw ResolveAutoCaptureDraftException.DraftMissing
    }
}
