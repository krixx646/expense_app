package com.chelumore.xpenze.core.ai

object FinanceScopePolicy {
    private val financeKeywords = setOf(
        "finance", "financial", "money", "budget", "budgets", "budgeting",
        "expense", "expenses", "spending", "spent", "income", "salary", "paycheck",
        "saving", "savings", "debt", "loan", "credit", "cash", "cashflow", "cash flow",
        "transaction", "transactions", "category", "categories", "account", "accounts",
        "balance", "balances", "bill", "bills", "rent", "utility", "utilities",
        "subscription", "subscriptions", "recurring", "goal", "goals", "afford",
        "overspend", "deficit", "surplus", "invest", "investing", "emergency fund",
        "net worth", "forecast"
    )

    private val amountOrCurrencyRegex = Regex(
        pattern = """(?i)([$]\s?\d+(\.\d+)?|\d+(\.\d+)?\s?(usd|eur|gbp|ngn|inr|cad|aud|zar|jpy))"""
    )

    private val modelDisclosureRegex = Regex(
        pattern = """(?i)\b(gemini|gpt|llm|model id|system prompt|api key|openai|anthropic)\b"""
    )

    fun isFinanceQuestion(question: String): Boolean {
        val normalized = question.trim().lowercase()
        if (normalized.isBlank()) return false
        if (financeKeywords.any { normalized.contains(it) }) return true
        return amountOrCurrencyRegex.containsMatchIn(normalized)
    }

    fun containsModelDisclosure(text: String): Boolean {
        return modelDisclosureRegex.containsMatchIn(text)
    }

    fun offTopicReply(userName: String?): String {
        val name = userName?.trim().orEmpty()
        val greeting = if (name.isNotBlank()) "$name, " else ""
        return greeting +
            "I want to stay useful, so I can only help with your finances in Xpenze: spending, budgets, savings, " +
            "income, debts, and cash flow decisions. Ask me anything in that lane and I will stay with you."
    }

    fun modelPrivacyReply(userName: String?): String {
        val name = userName?.trim().orEmpty()
        val prefix = if (name.isNotBlank()) "$name, " else ""
        return prefix +
            "I am Mochi, here to help with your money inside this app. " +
            "Ask me about your spending, budgets, savings, or what needs attention next."
    }
}
