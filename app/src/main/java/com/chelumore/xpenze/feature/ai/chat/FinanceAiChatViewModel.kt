package com.chelumore.xpenze.feature.ai.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.ai.GeminiFinanceAssistant
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.buildMochiSuggestedPrompts
import com.chelumore.xpenze.core.growth.buildPremiumOpportunities
import com.chelumore.xpenze.core.growth.formatPremiumOpportunity
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.localization.localizedUncategorized
import com.chelumore.xpenze.core.localization.localizedUnknown
import com.chelumore.xpenze.core.localization.localizedUnknownAccount
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@Serializable
data class FinanceChatMessageUi(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class FinanceAiChatUiState(
    val hasPremiumAccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val currencyCode: String = "USD",
    val appLanguageTag: String = "en",
    val userDisplayName: String? = null,
    val nudgeVariant: PremiumEmotionVariant = PremiumEmotionVariant.PLAYFUL,
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val messages: List<FinanceChatMessageUi> = emptyList(),
    val suggestedPrompts: List<String> = emptyList()
)

@HiltViewModel
class FinanceAiChatViewModel @Inject constructor(
    private val geminiFinanceAssistant: GeminiFinanceAssistant,
    private val userRepository: UserRepository,
    private val premiumEntitlementTracker: PremiumEntitlementTracker,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val recurringRuleRepository: RecurringRuleRepository,
    private val json: Json
) : ViewModel() {
    private val _uiState = MutableStateFlow(FinanceAiChatUiState())
    val uiState: StateFlow<FinanceAiChatUiState> = _uiState
    private var chatSessionVersion: Int = 0

    init {
        viewModelScope.launch {
            combine(
                userRepository.userPreferences,
                premiumEntitlementTracker.entitlement
            ) { prefs, entitlement ->
                prefs to entitlement
            }.collect { (prefs, entitlement) ->
                _uiState.update {
                    it.copy(
                        hasPremiumAccess = entitlement.hasPremiumAccess,
                        isTrialActive = entitlement.isTrialActive,
                        currencyCode = prefs.defaultCurrency,
                        appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag),
                        userDisplayName = prefs.userName ?: prefs.userEmail?.substringBefore("@"),
                        nudgeVariant = PremiumEmotionVariant.fromSeed(
                            prefs.userId ?: prefs.userEmail ?: prefs.defaultCurrency
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            userRepository.aiChatHistoryJson.collect { raw ->
                val restoredMessages = decodeHistory(raw)
                _uiState.update { it.copy(messages = restoredMessages) }
            }
        }

        viewModelScope.launch {
            combine(
                getTransactionsUseCase(),
                getCategoriesUseCase(),
                getBudgetsUseCase(),
                userRepository.userPreferences
            ) { transactions, categories, budgets, prefs ->
                val languageTag = AppLanguages.normalizeTag(prefs.appLanguageTag)
                buildMochiSuggestedPrompts(
                    opportunities = buildPremiumOpportunities(
                        transactions = transactions,
                        categories = categories,
                        budgets = budgets,
                        currencyCode = prefs.defaultCurrency
                    ),
                    currencyCode = prefs.defaultCurrency,
                    languageTag = languageTag
                )
            }.collect { prompts ->
                _uiState.update { it.copy(suggestedPrompts = prompts) }
            }
        }
    }

    fun setInput(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun prefillInput(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        _uiState.update { it.copy(inputText = normalized, error = null) }
    }

    fun sendMessage() {
        submitPrompt(_uiState.value.inputText)
    }

    fun sendSuggestedPrompt(prompt: String) {
        submitPrompt(prompt)
    }

    private fun submitPrompt(rawPrompt: String) {
        val question = rawPrompt.trim()
        if (question.isBlank() || _uiState.value.isSending) return
        val requestVersion = chatSessionVersion
        appendMessage(
            text = question,
            isUser = true
        )
        _uiState.update { it.copy(inputText = "", isSending = true, error = null) }

        viewModelScope.launch {
            if (requestVersion != chatSessionVersion) return@launch
            val currentState = _uiState.value
            if (!currentState.hasPremiumAccess) {
                if (requestVersion != chatSessionVersion) return@launch
                appendMessage(
                    text = localizedPremiumRequiredMessage(currentState.appLanguageTag),
                    isUser = false
                )
                _uiState.update { it.copy(isSending = false) }
                return@launch
            }

            runCatching {
                val summary = buildFinanceSummary(
                    currencyCode = currentState.currencyCode,
                    userDisplayName = currentState.userDisplayName,
                    appLanguageTag = currentState.appLanguageTag
                )
                val conversationContext = buildConversationContext(currentState.messages)
                geminiFinanceAssistant.askFinanceQuestion(
                    question = question,
                    financeSummary = summary,
                    conversationContext = conversationContext,
                    userDisplayName = currentState.userDisplayName,
                    preferredLanguageTag = currentState.appLanguageTag
                )
            }.onSuccess { answer ->
                if (requestVersion != chatSessionVersion) return@onSuccess
                appendMessage(text = answer, isUser = false)
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { error ->
                if (requestVersion != chatSessionVersion) return@onFailure
                appendMessage(
                    text = localizedTemporaryFailureMessage(currentState.appLanguageTag),
                    isUser = false
                )
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = error.message ?: "AI request failed"
                    )
                }
            }
        }
    }

    fun startNewChat() {
        chatSessionVersion += 1
        _uiState.update {
            it.copy(
                inputText = "",
                isSending = false,
                error = null,
                messages = emptyList()
            )
        }
        persistHistory(emptyList())
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun appendMessage(text: String, isUser: Boolean) {
        var updatedMessages: List<FinanceChatMessageUi> = emptyList()
        _uiState.update { state ->
            updatedMessages = (state.messages + FinanceChatMessageUi(
                id = UUID.randomUUID().toString(),
                text = text.trim(),
                isUser = isUser
            )).takeLast(MAX_PERSISTED_MESSAGES)
            state.copy(messages = updatedMessages)
        }
        persistHistory(updatedMessages)
    }

    private fun buildConversationContext(messages: List<FinanceChatMessageUi>): String {
        val relevant = messages
            .filter { it.text.isNotBlank() }
            .takeLast(MAX_CONTEXT_MESSAGES)

        if (relevant.isEmpty()) return "none"

        return relevant.joinToString(separator = "\n") { message ->
            val role = if (message.isUser) "User" else "Assistant"
            "$role: ${message.text.trim().replace("\n", " ").take(MAX_CONTEXT_CHARS_PER_MESSAGE)}"
        }
    }

    private fun decodeHistory(raw: String?): List<FinanceChatMessageUi> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(FinanceChatMessageUi.serializer()), raw)
                .filter { it.id != "welcome" && it.text.isNotBlank() }
                .takeLast(MAX_PERSISTED_MESSAGES)
        }.getOrElse { emptyList() }
    }

    private fun persistHistory(messages: List<FinanceChatMessageUi>) {
        viewModelScope.launch {
            val payload = runCatching {
                json.encodeToString(
                    ListSerializer(FinanceChatMessageUi.serializer()),
                    messages.takeLast(MAX_PERSISTED_MESSAGES)
                )
            }.getOrNull()
            userRepository.setAiChatHistoryJson(payload)
        }
    }

    private suspend fun buildFinanceSummary(
        currencyCode: String,
        userDisplayName: String?,
        appLanguageTag: String
    ): String {
        val now = System.currentTimeMillis()
        val lookbackStart = now - LOOKBACK_DAYS * 24L * 60L * 60L * 1000L
        val zoneId = ZoneId.systemDefault()
        val currentMonth = YearMonth.now(zoneId)
        val lastMonth = currentMonth.minusMonths(1)

        val transactions = getTransactionsUseCase().first()
            .filter { it.money.currency == currencyCode }
        val recentTransactions = transactions.filter { it.dateTime in lookbackStart..now }

        val categoriesById = getCategoriesUseCase().first().associateBy { it.id }
        val categoriesCount = categoriesById.size
        val accounts = getAccountsUseCase(includeArchived = false).first()
            .filter { it.currency == currencyCode }
        val accountsById = accounts.associateBy { it.id }
        val budgets = getBudgetsUseCase(activeOnly = false).first()
            .filter { !it.deleted }
        val recurringRules = recurringRuleRepository.getAll().first()
            .filter { !it.deleted && it.isActive && it.money.currency == currencyCode }

        fun monthRange(month: YearMonth): Pair<Long, Long> {
            val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
            return start to end
        }

        fun summarizeRange(start: Long, end: Long): Triple<Long, Long, Long> {
            val rangeTransactions = transactions.filter { it.dateTime in start..end }
            val incomeTotal = rangeTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.money.amountCents }
            val expenseTotal = rangeTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.money.amountCents }
            return Triple(incomeTotal, expenseTotal, incomeTotal - expenseTotal)
        }

        val (currentMonthStart, currentMonthEnd) = monthRange(currentMonth)
        val (lastMonthStart, lastMonthEnd) = monthRange(lastMonth)
        val (currentMonthIncome, currentMonthExpense, currentMonthNet) = summarizeRange(
            currentMonthStart,
            currentMonthEnd
        )
        val (lastMonthIncome, lastMonthExpense, lastMonthNet) = summarizeRange(
            lastMonthStart,
            lastMonthEnd
        )

        val income = recentTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.money.amountCents }
        val expense = recentTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.money.amountCents }
        val net = income - expense

        val topExpenseCategories = recentTransactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.money.amountCents } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (categoryId, cents) ->
                val name = categoryId?.let { categoriesById[it]?.name } ?: localizedUncategorized(appLanguageTag)
                "$name: ${cents.toCurrencyString(currencyCode)}"
            }
        val currentMonthTopExpenseCategories = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.dateTime in currentMonthStart..currentMonthEnd }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.money.amountCents } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (categoryId, cents) ->
                val name = categoryId?.let { categoriesById[it]?.name } ?: localizedUncategorized(appLanguageTag)
                "$name: ${cents.toCurrencyString(currencyCode)}"
            }

        val allBudgetSummaries = budgets
            .sortedByDescending { it.updatedAt }
            .take(MAX_BUDGET_CONTEXT_ITEMS)
            .map { budget ->
                val window = budget.currentPeriodWindow(now)
                val spent = if (window == null) {
                    0L
                } else {
                    transactions.asSequence()
                        .filter { it.type == TransactionType.EXPENSE }
                        .filter { it.dateTime in window.spentRange.startMillis..window.spentRange.endMillis }
                        .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                        .filter { budget.accountId == null || it.accountId == budget.accountId }
                        .sumOf { it.money.amountCents }
                }

                val remaining = budget.amountLimitCents - spent
                val used = if (budget.amountLimitCents > 0L) {
                    ((spent.toDouble() / budget.amountLimitCents.toDouble()) * 100.0).toInt()
                } else {
                    0
                }
                val categoryName = budget.categoryId?.let { categoriesById[it]?.name } ?: "All categories"
                val accountName = budget.accountId?.let { accountsById[it]?.name } ?: "All accounts"
                val status = when {
                    !budget.isActive -> "inactive"
                    window == null -> "inactive_this_period"
                    else -> "active"
                }
                "${budget.name} [$status, ${budget.period.name.lowercase()}] " +
                    "scope=($categoryName, $accountName), " +
                    "spent=${spent.toCurrencyString(currencyCode)}, " +
                    "limit=${budget.amountLimitCents.toCurrencyString(currencyCode)}, " +
                    "remaining=${remaining.toCurrencyString(currencyCode)}, used=$used%"
            }
        val hiddenBudgetCount = (budgets.size - allBudgetSummaries.size).coerceAtLeast(0)
        val activeBudgetsCount = budgets.count { it.isActive }
        val overBudgetCount = budgets.count { budget ->
            if (budget.amountLimitCents <= 0L) return@count false
            val window = budget.currentPeriodWindow(now) ?: return@count false
            val spent = transactions.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .filter { it.dateTime in window.spentRange.startMillis..window.spentRange.endMillis }
                .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                .filter { budget.accountId == null || it.accountId == budget.accountId }
                .sumOf { it.money.amountCents }
            spent > budget.amountLimitCents
        }

        val accountBalances = accounts
            .sortedByDescending { it.currentBalanceCents }
            .take(5)
            .joinToString(separator = "; ") { account ->
                "${account.name}=${account.currentBalanceCents.toCurrencyString(currencyCode)}"
            }
        val monthlySeries = generateSequence(currentMonth) { it.minusMonths(1) }
            .take(MAX_MONTHLY_CONTEXT_ITEMS)
            .toList()
            .asReversed()
            .map { month ->
                val (monthStart, monthEnd) = monthRange(month)
                val (monthIncome, monthExpense, monthNet) = summarizeRange(monthStart, monthEnd)
                "${month.year}-${month.monthValue.toString().padStart(2, '0')}: " +
                    "income=${monthIncome.toCurrencyString(currencyCode)}, " +
                    "expense=${monthExpense.toCurrencyString(currencyCode)}, " +
                    "net=${monthNet.toCurrencyString(currencyCode)}"
            }
        val recentDailyBuckets = recentTransactions.groupBy { transaction ->
            Instant.ofEpochMilli(transaction.dateTime).atZone(zoneId).toLocalDate()
        }
        val highestExpenseDay = recentDailyBuckets
            .mapValues { (_, txs) ->
                txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
            }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0L }
            ?.let { (date, cents) -> "$date=${cents.toCurrencyString(currencyCode)}" }
        val highestIncomeDay = recentDailyBuckets
            .mapValues { (_, txs) ->
                txs.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }
            }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0L }
            ?.let { (date, cents) -> "$date=${cents.toCurrencyString(currencyCode)}" }
        val worstNetDay = recentDailyBuckets
            .mapValues { (_, txs) ->
                txs.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents } -
                    txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
            }
            .minByOrNull { it.value }
            ?.takeIf { it.value < 0L }
            ?.let { (date, cents) -> "$date=${cents.toCurrencyString(currencyCode)}" }
        val largestRecentExpenses = recentTransactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.money.amountCents }
            .take(MAX_LARGEST_EXPENSE_CONTEXT_ITEMS)
            .map { transaction ->
                val categoryName = transaction.categoryId?.let { categoriesById[it]?.name } ?: localizedUncategorized(appLanguageTag)
                "${transaction.title}: ${transaction.money.amountCents.toCurrencyString(currencyCode)} ($categoryName)"
            }
            .toList()
        val tagSummary = recentTransactions.asSequence()
            .flatMap { it.tags.asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it.lowercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(MAX_TAG_CONTEXT_ITEMS)
            .map { (tag, count) -> "$tag x$count" }
        val receiptCount = recentTransactions.count { !it.receiptUri.isNullOrBlank() }
        val uncategorizedExpense = recentTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId == null }
            .sumOf { it.money.amountCents }

        val recentTransactionSummaries = recentTransactions
            .sortedByDescending { it.dateTime }
            .take(MAX_RECENT_TRANSACTIONS_CONTEXT_ITEMS)
            .map { transaction ->
                val categoryName = transaction.categoryId?.let { categoriesById[it]?.name } ?: localizedUncategorized(appLanguageTag)
                val accountName = accountsById[transaction.accountId]?.name ?: localizedUnknownAccount(appLanguageTag)
                "${transaction.title} | ${transaction.type.name} | " +
                    "${transaction.money.amountCents.toCurrencyString(currencyCode)} | " +
                    "category=$categoryName | account=$accountName | at=${transaction.dateTime}"
            }

        val recurringExpenseCount = recentTransactions.count {
            it.type == TransactionType.EXPENSE && it.isRecurring
        }
        val recurringRuleSummary = recurringRules
            .sortedBy { it.nextOccurrence }
            .take(MAX_RECURRING_RULE_CONTEXT_ITEMS)
            .map { rule ->
                val categoryName = rule.categoryId?.let { categoriesById[it]?.name } ?: localizedUncategorized(appLanguageTag)
                val accountName = accountsById[rule.accountId]?.name ?: localizedUnknownAccount(appLanguageTag)
                val nextDate = Instant.ofEpochMilli(rule.nextOccurrence).atZone(zoneId).toLocalDate()
                "${rule.title} [${rule.frequency.name.lowercase()} x${rule.interval}] " +
                    "amount=${rule.money.amountCents.toCurrencyString(currencyCode)}, " +
                    "account=$accountName, category=$categoryName, next=$nextDate"
            }
        val opportunitySummary = buildPremiumOpportunities(
            transactions = transactions,
            categories = categoriesById.values.toList(),
            budgets = budgets,
            currencyCode = currencyCode
        )
            .take(3)
            .map { opportunity ->
                val copy = formatPremiumOpportunity(
                    opportunity = opportunity,
                    currencyCode = currencyCode,
                    languageTag = appLanguageTag
                )
                "${copy.title}: ${copy.body}"
            }

        return buildString {
            appendLine("User: ${if (userDisplayName.isNullOrBlank()) localizedUnknown(appLanguageTag) else userDisplayName.trim()}")
            appendLine("Preferred app language: ${AppLanguages.normalizeTag(appLanguageTag)}")
            appendLine("Window: last $LOOKBACK_DAYS days")
            appendLine("Currency: $currencyCode")
            appendLine("Accounts tracked: ${accounts.size}")
            appendLine("Categories tracked: $categoriesCount")
            appendLine("Income: ${income.toCurrencyString(currencyCode)}")
            appendLine("Expense: ${expense.toCurrencyString(currencyCode)}")
            appendLine("Net: ${net.toCurrencyString(currencyCode)}")
            appendLine(
                "Current month analytics: income=${currentMonthIncome.toCurrencyString(currencyCode)}, " +
                    "expense=${currentMonthExpense.toCurrencyString(currencyCode)}, " +
                    "net=${currentMonthNet.toCurrencyString(currencyCode)}"
            )
            appendLine(
                "Last month analytics: income=${lastMonthIncome.toCurrencyString(currencyCode)}, " +
                    "expense=${lastMonthExpense.toCurrencyString(currencyCode)}, " +
                    "net=${lastMonthNet.toCurrencyString(currencyCode)}"
            )
            appendLine("Recurring expenses count: $recurringExpenseCount")
            appendLine("Top expense categories: ${topExpenseCategories.ifEmpty { listOf("none") }.joinToString(" | ")}")
            appendLine(
                "Current month top expense categories: " +
                    currentMonthTopExpenseCategories.ifEmpty { listOf("none") }.joinToString(" | ")
            )
            appendLine(
                "Budgets summary: total=${budgets.size}, active=$activeBudgetsCount, overLimit=$overBudgetCount"
            )
            appendLine("Budget details: ${allBudgetSummaries.ifEmpty { listOf("none") }.joinToString(" | ")}")
            if (hiddenBudgetCount > 0) {
                appendLine("Additional budgets not shown due to context limit: $hiddenBudgetCount")
            }
            appendLine("Monthly trend overview: ${monthlySeries.ifEmpty { listOf("none") }.joinToString(" | ")}")
            appendLine(
                "Trend signals: highestExpenseDay=${highestExpenseDay ?: "none"}, " +
                    "highestIncomeDay=${highestIncomeDay ?: "none"}, worstNetDay=${worstNetDay ?: "none"}"
            )
            appendLine(
                "Recent transactions: " +
                    recentTransactionSummaries.ifEmpty { listOf("none") }.joinToString(" | ")
            )
            appendLine(
                "Largest recent expenses: " +
                    largestRecentExpenses.ifEmpty { listOf("none") }.joinToString(" | ")
            )
            appendLine(
                "Top tags in recent transactions: " +
                    tagSummary.ifEmpty { listOf("none") }.joinToString(" | ")
            )
            appendLine("Receipts attached in window: $receiptCount")
            appendLine("${localizedUncategorized(appLanguageTag)} expense in window: ${uncategorizedExpense.toCurrencyString(currencyCode)}")
            appendLine(
                "Recurring rules summary: total=${recurringRules.size}, " +
                    "details=${recurringRuleSummary.ifEmpty { listOf("none") }.joinToString(" | ")}"
            )
            appendLine("Best savings opportunities: ${opportunitySummary.ifEmpty { listOf("none") }.joinToString(" | ")}")
            append("Top account balances: ${if (accountBalances.isBlank()) "none" else accountBalances}")
        }
    }

    private companion object {
        const val LOOKBACK_DAYS = 90L
        const val MAX_BUDGET_CONTEXT_ITEMS = 20
        const val MAX_RECENT_TRANSACTIONS_CONTEXT_ITEMS = 12
        const val MAX_MONTHLY_CONTEXT_ITEMS = 6
        const val MAX_RECURRING_RULE_CONTEXT_ITEMS = 8
        const val MAX_LARGEST_EXPENSE_CONTEXT_ITEMS = 6
        const val MAX_TAG_CONTEXT_ITEMS = 8
        const val MAX_PERSISTED_MESSAGES = 50
        const val MAX_CONTEXT_MESSAGES = 12
        const val MAX_CONTEXT_CHARS_PER_MESSAGE = 180
    }
}

private fun localizedPremiumRequiredMessage(languageTag: String): String = when {
    languageTag.startsWith("es") -> "Mochi es una funcion Premium. Inicia tu prueba o mejora tu plan para continuar."
    languageTag.startsWith("fr") -> "Mochi est une fonctionnalite Premium. Lancez votre essai ou passez a Premium pour continuer."
    languageTag.startsWith("de") -> "Mochi ist eine Premium-Funktion. Starte deine Testphase oder wechsle zu Premium, um fortzufahren."
    languageTag.startsWith("pt") -> "O Mochi e um recurso Premium. Inicie seu teste ou faca upgrade para continuar."
    languageTag.startsWith("ar") -> "Mochi ميزة Premium. ابدأ التجربة أو قم بالترقية للمتابعة."
    languageTag.startsWith("hi") -> "Mochi एक Premium सुविधा है। जारी रखने के लिए ट्रायल शुरू करें या अपग्रेड करें।"
    languageTag.startsWith("ja") -> "Mochi はプレミアム機能です。続行するには、トライアルを開始するかアップグレードしてください。"
    languageTag.startsWith("sw") -> "Mochi ni kipengele cha Premium. Anza jaribio lako au boresha ili uendelee."
    languageTag.startsWith("zh") -> "Mochi 是高级功能。开始试用或升级后即可继续。"
    else -> "Mochi is a Premium feature. Start your trial or upgrade to continue."
}

private fun localizedTemporaryFailureMessage(languageTag: String): String = when {
    languageTag.startsWith("es") -> "No pude responder en este momento. Intentalo de nuevo en un momento."
    languageTag.startsWith("fr") -> "Je ne peux pas repondre pour le moment. Reessayez dans un instant."
    languageTag.startsWith("de") -> "Ich kann gerade nicht antworten. Bitte versuche es gleich noch einmal."
    languageTag.startsWith("pt") -> "Nao consegui responder agora. Tente novamente em instantes."
    languageTag.startsWith("ar") -> "تعذر عليّ الرد الآن. حاول مرة أخرى بعد قليل."
    languageTag.startsWith("hi") -> "मैं अभी जवाब नहीं दे सका। कृपया थोड़ी देर में फिर प्रयास करें।"
    languageTag.startsWith("ja") -> "今は回答できません。しばらくしてからもう一度お試しください。"
    languageTag.startsWith("sw") -> "Sikuweza kujibu sasa hivi. Tafadhali jaribu tena baada ya muda mfupi."
    languageTag.startsWith("zh") -> "我暂时无法回答。请稍后再试。"
    else -> "I could not answer right now. Please try again in a moment."
}
