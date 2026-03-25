package com.chelumore.xpenze.domain.model

data class MerchantMemorySuggestion(
    val matchedTitle: String,
    val suggestedTitle: String,
    val suggestedCategoryId: String? = null,
    val suggestedAccountId: String? = null,
    val suggestedNotes: String? = null,
    val suggestedTags: List<String> = emptyList(),
    val confidence: AutoCaptureConfidence = AutoCaptureConfidence.MEDIUM,
    val recurringSuggestion: RecurringRuleSuggestion? = null
)

data class RecurringRuleSuggestion(
    val frequency: RecurringFrequency,
    val interval: Int
)
