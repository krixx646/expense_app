package com.chelumore.xpenze.domain.usecase.transaction

import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.resolveCategorySelectionForTransactionType
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * Creates and persists a new [Transaction].
     *
     * Returns the generated transaction id.
     */
    suspend operator fun invoke(
        accountId: String,
        toAccountId: String? = null,
        categoryId: String? = null,
        amountCents: Long,
        currency: String,
        type: TransactionType,
        title: String,
        notes: String? = null,
        dateTime: Long = System.currentTimeMillis(),
        receiptUri: String? = null,
        tags: List<String> = emptyList(),
        isRecurring: Boolean = false,
        recurringRuleId: String? = null,
        id: String? = null
    ): String {
        require(accountId.isNotBlank()) { "accountId is required" }
        require(currency.isNotBlank()) { "currency is required" }
        require(title.isNotBlank()) { "title is required" }
        require(amountCents > 0) { "amountCents must be > 0" }

        if (type == TransactionType.TRANSFER) {
            require(!toAccountId.isNullOrBlank()) { "toAccountId is required for transfers" }
            require(toAccountId != accountId) { "toAccountId must be different from accountId" }
        }

        val now = System.currentTimeMillis()
        val resolvedId = id ?: UUID.randomUUID().toString()
        val resolvedCategoryId = if (type == TransactionType.TRANSFER || categoryId.isNullOrBlank()) {
            null
        } else {
            resolveCategorySelectionForTransactionType(
                categoryId = categoryId,
                transactionType = type,
                categories = categoryRepository.getAll().first()
            )
        }

        val transaction = Transaction(
            id = resolvedId,
            accountId = accountId,
            toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
            categoryId = resolvedCategoryId,
            money = Money(amountCents = amountCents, currency = currency),
            type = type,
            title = title,
            notes = notes,
            dateTime = dateTime,
            receiptUri = receiptUri,
            isRecurring = isRecurring,
            recurringRuleId = recurringRuleId,
            tags = tags,
            synced = false,
            deleted = false,
            createdAt = now,
            updatedAt = now
        )

        transactionRepository.insert(transaction)
        return resolvedId
    }
}


