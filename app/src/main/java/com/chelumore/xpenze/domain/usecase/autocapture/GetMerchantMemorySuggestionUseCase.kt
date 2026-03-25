package com.chelumore.xpenze.domain.usecase.autocapture

import com.chelumore.xpenze.core.autocapture.normalizeCaptureText
import com.chelumore.xpenze.domain.model.AutoCaptureConfidence
import com.chelumore.xpenze.domain.model.MerchantMemorySuggestion
import com.chelumore.xpenze.domain.model.RecurringRuleSuggestion
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetMerchantMemorySuggestionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurringRuleRepository: RecurringRuleRepository
) {

    suspend operator fun invoke(title: String): MerchantMemorySuggestion? {
        val normalizedQuery = normalizeCaptureText(title)
        if (normalizedQuery.length < 3) return null

        val candidates = transactionRepository.search(title.trim()).first()
            .filter { normalizeCaptureText(it.title).contains(normalizedQuery) }
            .sortedByDescending { it.updatedAt }
            .take(12)
        if (candidates.isEmpty()) return null

        val exactMatches = candidates.filter { normalizeCaptureText(it.title) == normalizedQuery }
        val memoryPool = exactMatches.ifEmpty { candidates }
        val reference = memoryPool.first()
        val recurringRule = recurringRuleRepository.getActive().first()
            .firstOrNull { normalizeCaptureText(it.title) == normalizeCaptureText(reference.title) }

        return MerchantMemorySuggestion(
            matchedTitle = reference.title,
            suggestedTitle = reference.title,
            suggestedCategoryId = memoryPool
                .mapNotNull { it.categoryId }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key,
            suggestedAccountId = memoryPool
                .map { it.accountId }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key,
            suggestedNotes = memoryPool
                .mapNotNull { it.notes?.takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key,
            suggestedTags = memoryPool
                .flatMap { it.tags }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(4)
                .map { it.key },
            confidence = if (exactMatches.isNotEmpty()) {
                AutoCaptureConfidence.HIGH
            } else {
                AutoCaptureConfidence.MEDIUM
            },
            recurringSuggestion = recurringRule?.let { rule ->
                RecurringRuleSuggestion(
                    frequency = rule.frequency,
                    interval = rule.interval.coerceAtLeast(1)
                )
            }
        )
    }
}
