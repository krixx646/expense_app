package com.chelumore.xpenze.domain.usecase.recurring

import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.usecase.transaction.AddTransactionUseCase
import javax.inject.Inject

data class ProcessRecurringResult(
    val processed: Int
)

class ProcessDueRecurringRulesUseCase @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) {
    suspend operator fun invoke(): ProcessRecurringResult {
        val due = recurringRuleRepository.getDueRules()
        var processed = 0

        for (rule in due) {
            if (!rule.isActive || rule.deleted) continue

            val whenMillis = rule.nextOccurrence
            val deterministicId = "${rule.id}_$whenMillis"
            addTransactionUseCase(
                accountId = rule.accountId,
                toAccountId = if (rule.type == TransactionType.TRANSFER) rule.toAccountId else null,
                categoryId = if (rule.type == TransactionType.TRANSFER) null else rule.categoryId,
                amountCents = rule.money.amountCents,
                currency = rule.money.currency,
                type = rule.type,
                title = rule.title,
                notes = rule.notes,
                dateTime = whenMillis,
                isRecurring = true,
                recurringRuleId = rule.id,
                id = deterministicId
            )

            recurringRuleRepository.updateNextOccurrence(
                id = rule.id,
                nextOccurrence = 0L, // compute next
                lastProcessed = whenMillis
            )
            processed++
        }

        return ProcessRecurringResult(processed)
    }
}


