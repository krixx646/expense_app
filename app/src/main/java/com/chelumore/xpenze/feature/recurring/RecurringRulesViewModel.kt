package com.chelumore.xpenze.feature.recurring

import android.content.Context
import com.chelumore.xpenze.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.work.RecurringRuleScheduler
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.RecurringFrequency
import com.chelumore.xpenze.domain.model.RecurringRule
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.recurring.ProcessDueRecurringRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface RecurringRulesEvent {
    data class Message(val text: String) : RecurringRulesEvent
}

data class RecurringRuleUiModel(
    val rule: RecurringRule,
    val accountName: String,
    val toAccountName: String?,
    val categoryName: String?
)

data class RecurringRulesUiState(
    val isLoading: Boolean = true,
    val rules: List<RecurringRuleUiModel> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class RecurringRulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recurringRuleRepository: RecurringRuleRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val scheduler: RecurringRuleScheduler,
    private val processDueRecurringRulesUseCase: ProcessDueRecurringRulesUseCase
) : ViewModel() {

    private val eventsChannel = Channel<RecurringRulesEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<RecurringRulesUiState> = combine(
        recurringRuleRepository.getAll(),
        getAccountsUseCase(includeArchived = true),
        getCategoriesUseCase()
    ) { rules, accounts, categories ->
        val accountsById = accounts.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }

        val uiRules = rules
            .sortedBy { it.nextOccurrence }
            .map { rule ->
                RecurringRuleUiModel(
                    rule = rule,
                    accountName = accountsById[rule.accountId]?.name
                        ?: context.getString(R.string.subscription_unknown),
                    toAccountName = rule.toAccountId?.let { accountsById[it]?.name },
                    categoryName = rule.categoryId?.let { categoriesById[it]?.name }
                )
            }

        RecurringRulesUiState(
            isLoading = false,
            rules = uiRules,
            accounts = accounts,
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringRulesUiState()
    )

    fun setActive(id: String, active: Boolean) {
        viewModelScope.launch {
            runCatching { recurringRuleRepository.setActive(id, active) }
            scheduler.scheduleNow()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { recurringRuleRepository.delete(id) }
            scheduler.scheduleNow()
            eventsChannel.send(RecurringRulesEvent.Message(context.getString(R.string.recurring_rule_deleted)))
        }
    }

    fun runDueNow() {
        viewModelScope.launch {
            val result = runCatching { processDueRecurringRulesUseCase() }
                .getOrNull()
            scheduler.scheduleNow()
            val count = result?.processed ?: 0
            eventsChannel.send(
                RecurringRulesEvent.Message(
                    context.getString(R.string.recurring_rules_processed_count, count)
                )
            )
        }
    }

    fun upsertRule(
        existingId: String? = null,
        accountId: String,
        toAccountId: String? = null,
        categoryId: String? = null,
        amountCents: Long,
        currency: String,
        type: TransactionType,
        title: String,
        notes: String? = null,
        frequency: RecurringFrequency,
        interval: Int,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        monthOfYear: Int? = null,
        startDate: Long,
        endDate: Long? = null,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = existingId ?: UUID.randomUUID().toString()
            val categoryType = when (type) {
                TransactionType.EXPENSE -> CategoryType.EXPENSE
                TransactionType.INCOME -> CategoryType.INCOME
                TransactionType.TRANSFER -> null
            }
            val resolvedCategoryId = categoryId?.takeIf { selectedId ->
                categoryType != null && uiState.value.categories.any { it.id == selectedId && it.type == categoryType }
            }
            val rule = RecurringRule(
                id = id,
                accountId = accountId,
                toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                categoryId = resolvedCategoryId,
                money = Money(amountCents = amountCents, currency = currency),
                type = type,
                title = title,
                notes = notes,
                frequency = frequency,
                interval = interval.coerceAtLeast(1),
                dayOfWeek = dayOfWeek,
                dayOfMonth = dayOfMonth,
                monthOfYear = monthOfYear,
                startDate = startDate,
                endDate = endDate,
                nextOccurrence = startDate, // repository recalculates
                lastProcessed = null,
                isActive = isActive,
                synced = false,
                deleted = false,
                createdAt = now,
                updatedAt = now
            )

            runCatching {
                if (existingId == null) recurringRuleRepository.insert(rule) else recurringRuleRepository.update(rule)
            }.onSuccess {
                scheduler.scheduleDaily()
                scheduler.scheduleNow()
                eventsChannel.send(RecurringRulesEvent.Message(context.getString(R.string.recurring_rule_saved)))
            }.onFailure { e ->
                eventsChannel.send(
                    RecurringRulesEvent.Message(
                        e.message ?: context.getString(R.string.error_recurring_rule_save)
                    )
                )
            }
        }
    }
}


