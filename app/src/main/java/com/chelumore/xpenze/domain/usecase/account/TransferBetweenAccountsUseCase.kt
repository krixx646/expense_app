package com.chelumore.xpenze.domain.usecase.account

import com.chelumore.xpenze.core.localization.localizedTransfer
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.usecase.transaction.AddTransactionUseCase
import javax.inject.Inject

class TransferBetweenAccountsUseCase @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase
) {
    /**
     * Creates a TRANSFER transaction. Returns the generated transaction id.
     *
     * Account balances are updated by the TransactionRepository implementation.
     */
    suspend operator fun invoke(
        fromAccountId: String,
        toAccountId: String,
        amountCents: Long,
        currency: String,
        title: String = localizedTransfer(),
        notes: String? = null,
        dateTime: Long = System.currentTimeMillis()
    ): String {
        return addTransactionUseCase(
            accountId = fromAccountId,
            toAccountId = toAccountId,
            categoryId = null,
            amountCents = amountCents,
            currency = currency,
            type = TransactionType.TRANSFER,
            title = title,
            notes = notes,
            dateTime = dateTime
        )
    }
}


