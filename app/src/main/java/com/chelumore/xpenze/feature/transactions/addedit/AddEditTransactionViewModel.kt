package com.chelumore.xpenze.feature.transactions.addedit

import android.content.Context
import com.chelumore.xpenze.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.core.work.RecurringRuleScheduler
import com.chelumore.xpenze.core.common.toEpochMillis
import com.chelumore.xpenze.core.common.toLocalDateTime
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.Money
import com.chelumore.xpenze.domain.model.RecurringFrequency
import com.chelumore.xpenze.domain.model.RecurringRule
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.repository.hasPremiumAccess
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.autocapture.GetMerchantMemorySuggestionUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.AddTransactionUseCase
import com.chelumore.xpenze.domain.usecase.transaction.DeleteTransactionUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionDetailsUseCase
import com.chelumore.xpenze.domain.usecase.transaction.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getTransactionDetailsUseCase: GetTransactionDetailsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val preferencesDataStore: PreferencesDataStore,
    private val budgetAlertScheduler: BudgetAlertScheduler,
    private val recurringRuleRepository: RecurringRuleRepository,
    private val recurringRuleScheduler: RecurringRuleScheduler,
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository,
    private val getMerchantMemorySuggestionUseCase: GetMerchantMemorySuggestionUseCase,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker
) : ViewModel() {

    private val _formState = MutableStateFlow(AddEditTransactionUiState())
    private var suggestionJob: Job? = null

    // Combine repositories with form state
    val uiState: StateFlow<AddEditTransactionUiState> = combine(
        _formState,
        getCategoriesUseCase(),
        getAccountsUseCase(includeArchived = true),
        preferencesDataStore.userPreferences
    ) { form, categories, accounts, prefs ->
        val fallbackAccountId = form.selectedAccountId ?: accounts.firstOrNull()?.id
        val accountCurrency = accounts.firstOrNull { it.id == fallbackAccountId }?.currency
        val resolvedCurrency = accountCurrency ?: form.currency.ifBlank { prefs.defaultCurrency }
        val allowedCategories = when (form.type) {
            TransactionType.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE }
            TransactionType.INCOME -> categories.filter { it.type == CategoryType.INCOME }
            TransactionType.TRANSFER -> emptyList()
        }
        val sanitizedCategoryId = form.selectedCategoryId?.takeIf { selectedId ->
            allowedCategories.any { it.id == selectedId }
        }
        form.copy(
            // Keep form.isLoading as-is; init() will toggle it.
            categories = categories,
            accounts = accounts,
            selectedCategoryId = sanitizedCategoryId,
            selectedAccountId = fallbackAccountId,
            currency = resolvedCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditTransactionUiState()
    )

    fun init(
        transactionId: String?,
        draftId: String? = null,
        initialType: TransactionType
    ) {
        if (transactionId.isNullOrBlank() && draftId.isNullOrBlank()) {
            _formState.update {
                it.copy(
                    isLoading = false,
                    isEdit = false,
                    transactionId = null,
                    draftId = null,
                    type = initialType,
                    receiptUri = null,
                    tagsText = "",
                    isRecurring = false,
                    recurringRuleId = null,
                    recurringFrequency = RecurringFrequency.MONTHLY,
                    recurringIntervalText = "1",
                    merchantSuggestion = null
                )
            }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, error = null, isSaved = false) }
            try {
                if (!draftId.isNullOrBlank() && transactionId.isNullOrBlank()) {
                    val draft = autoCaptureDraftRepository.getByIdOnce(draftId)
                        ?: throw IllegalStateException("Draft not found.")
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            isEdit = false,
                            transactionId = null,
                            draftId = draft.id,
                            type = draft.suggestedType ?: initialType,
                            amountText = draft.amountCents?.let { cents -> (cents / 100.0).toString() }.orEmpty(),
                            currency = draft.currency,
                            title = draft.title,
                            notes = draft.notes,
                            dateTime = draft.occurredAt,
                            receiptUri = draft.receiptUri,
                            tagsText = draft.tags.joinToString(", "),
                            isRecurring = false,
                            recurringRuleId = null,
                            recurringFrequency = RecurringFrequency.MONTHLY,
                            recurringIntervalText = "1",
                            selectedCategoryId = draft.categoryId,
                            selectedAccountId = draft.accountId,
                            selectedToAccountId = draft.toAccountId,
                            merchantSuggestion = null
                        )
                    }
                    if (draft.status == AutoCaptureDraftStatus.PENDING) {
                        loadMerchantSuggestion(draft.title)
                    }
                    return@launch
                }

                val tx = getTransactionDetailsUseCase(transactionId!!).first()
                    ?: throw IllegalStateException(context.getString(R.string.error_transaction_not_found))

                val rule = tx.recurringRuleId?.let { id -> recurringRuleRepository.getById(id).first() }

                _formState.update {
                    it.copy(
                        isLoading = false,
                        isEdit = true,
                        transactionId = tx.id,
                        draftId = null,
                        type = tx.type,
                        amountText = (tx.money.amountCents / 100.0).toString(),
                        currency = tx.money.currency,
                        title = tx.title,
                        notes = tx.notes,
                        dateTime = tx.dateTime,
                        receiptUri = tx.receiptUri,
                        tagsText = tx.tags.joinToString(", "),
                        isRecurring = tx.isRecurring,
                        recurringRuleId = tx.recurringRuleId,
                        recurringFrequency = rule?.frequency ?: RecurringFrequency.MONTHLY,
                        recurringIntervalText = (rule?.interval ?: 1).toString(),
                        selectedCategoryId = tx.categoryId,
                        selectedAccountId = tx.accountId,
                        selectedToAccountId = tx.toAccountId,
                        merchantSuggestion = null
                    )
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_transaction_load)
                    )
                }
            }
        }
    }

    fun setType(type: TransactionType) {
        _formState.update {
            it.copy(
                type = type,
                selectedCategoryId = if (type == TransactionType.TRANSFER) null else it.selectedCategoryId,
                selectedToAccountId = if (type == TransactionType.TRANSFER) it.selectedToAccountId else null
            )
        }
    }

    fun setAmount(amount: String) {
        if (amount.all { it.isDigit() || it == '.' }) {
            _formState.update { it.copy(amountText = amount) }
        }
    }

    fun setTitle(title: String) {
        _formState.update { it.copy(title = title) }
        loadMerchantSuggestion(title)
    }

    fun setNotes(notes: String) {
        _formState.update { it.copy(notes = notes.ifBlank { null }) }
    }

    fun setReceiptUri(uri: String?) {
        _formState.update { it.copy(receiptUri = uri) }
    }

    fun clearReceipt() {
        _formState.update { it.copy(receiptUri = null) }
    }

    fun setTagsText(text: String) {
        _formState.update { it.copy(tagsText = text) }
    }

    fun setRecurringEnabled(enabled: Boolean) {
        _formState.update { it.copy(isRecurring = enabled) }
    }

    fun setRecurringFrequency(frequency: RecurringFrequency) {
        _formState.update { it.copy(recurringFrequency = frequency) }
    }

    fun setRecurringIntervalText(text: String) {
        val filtered = text.filter { it.isDigit() }.take(2)
        _formState.update { it.copy(recurringIntervalText = filtered) }
    }

    fun setCategoryId(categoryId: String?) {
        _formState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun setAccountId(accountId: String?) {
        val currency = uiState.value.accounts.firstOrNull { it.id == accountId }?.currency
        _formState.update { it.copy(selectedAccountId = accountId, currency = currency ?: it.currency) }
    }

    fun setToAccountId(accountId: String?) {
        _formState.update { it.copy(selectedToAccountId = accountId) }
    }

    fun setDate(localStartOfDayMillis: Long) {
        val zone = ZoneId.systemDefault()
        val current = uiState.value.dateTime.toLocalDateTime(zone)
        val newDate = Instant.ofEpochMilli(localStartOfDayMillis).atZone(zone).toLocalDate()
        val updated = current.withYear(newDate.year).withMonth(newDate.monthValue).withDayOfMonth(newDate.dayOfMonth)
        _formState.update { it.copy(dateTime = updated.toEpochMillis(zone)) }
    }

    fun setTime(time: LocalTime) {
        val zone = ZoneId.systemDefault()
        val current = uiState.value.dateTime.toLocalDateTime(zone)
        val updated = current.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        _formState.update { it.copy(dateTime = updated.toEpochMillis(zone)) }
    }

    fun clearError() = _formState.update { it.copy(error = null) }

    fun applyMerchantSuggestion() {
        val suggestion = uiState.value.merchantSuggestion ?: return
        _formState.update { state ->
            state.copy(
                title = suggestion.suggestedTitle,
                notes = state.notes ?: suggestion.suggestedNotes,
                tagsText = if (state.tagsText.isBlank()) {
                    suggestion.suggestedTags.joinToString(", ")
                } else {
                    state.tagsText
                },
                selectedCategoryId = if (state.type == TransactionType.TRANSFER) {
                    null
                } else {
                    state.selectedCategoryId ?: suggestion.suggestedCategoryId
                },
                selectedAccountId = state.selectedAccountId ?: suggestion.suggestedAccountId
            )
        }
    }

    fun applyRecurringSuggestion() {
        val recurringSuggestion = uiState.value.merchantSuggestion?.recurringSuggestion ?: return
        _formState.update {
            it.copy(
                isRecurring = true,
                recurringFrequency = recurringSuggestion.frequency,
                recurringIntervalText = recurringSuggestion.interval.toString()
            )
        }
    }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                _formState.update { it.copy(isSaving = true, error = null) }

                val amountCents = ((state.amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                val now = System.currentTimeMillis()
                val tags = state.tagsText
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                val interval = state.recurringIntervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val resolvedTitle = resolveTitle(state)
                val resolvedCategoryId = resolveCategoryIdForSave(state)
                val resolvedRuleId = if (state.isRecurring) {
                    state.recurringRuleId ?: UUID.randomUUID().toString()
                } else null

                if (state.isRecurring && state.recurringRuleId == null) {
                    _formState.update { it.copy(recurringRuleId = resolvedRuleId) }
                }

                // Upsert recurring rule before saving transaction to keep references valid.
                val existingRule = if (state.isRecurring) {
                    resolvedRuleId?.let { id -> recurringRuleRepository.getById(id).first() }
                } else null

                val createdNewRule = state.isRecurring && existingRule == null && resolvedRuleId != null

                if (state.isRecurring && resolvedRuleId != null) {
                    val rule = RecurringRule(
                        id = resolvedRuleId,
                        accountId = state.selectedAccount!!.id,
                        toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                        categoryId = resolvedCategoryId,
                        money = Money(amountCents = amountCents, currency = state.currency),
                        type = state.type,
                        title = resolvedTitle,
                        notes = state.notes,
                        frequency = state.recurringFrequency,
                        interval = interval,
                        dayOfWeek = null,
                        dayOfMonth = null,
                        monthOfYear = null,
                        startDate = state.dateTime,
                        endDate = existingRule?.endDate,
                        nextOccurrence = state.dateTime, // repository recalculates
                        lastProcessed = state.dateTime, // prevent duplicate at start date/time
                        isActive = true,
                        synced = false,
                        deleted = false,
                        createdAt = existingRule?.createdAt ?: now,
                        updatedAt = now
                    )

                    if (existingRule == null) recurringRuleRepository.insert(rule) else recurringRuleRepository.update(rule)
                    recurringRuleScheduler.scheduleDaily()
                    recurringRuleScheduler.scheduleNow()
                } else if (!state.isRecurring && state.recurringRuleId != null) {
                    // Turning off: stop future occurrences.
                    recurringRuleRepository.setActive(state.recurringRuleId, false)
                    recurringRuleScheduler.scheduleNow()
                }

                if (!state.isEdit) {
                    runCatching {
                        val createdTransactionId = addTransactionUseCase(
                            accountId = state.selectedAccount!!.id,
                            toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                            categoryId = resolvedCategoryId,
                            amountCents = amountCents,
                            currency = state.currency,
                            type = state.type,
                            title = resolvedTitle,
                            notes = state.notes,
                            dateTime = state.dateTime,
                            receiptUri = state.receiptUri,
                            tags = tags,
                            isRecurring = state.isRecurring,
                            recurringRuleId = resolvedRuleId
                        )
                        if (!state.draftId.isNullOrBlank()) {
                            autoCaptureDraftRepository.markResolved(
                                id = state.draftId,
                                resolution = state.type.toDraftResolution(),
                                createdTransactionId = createdTransactionId
                            )
                        }
                    }.onFailure { e ->
                        // If we created a brand new rule but failed to create the first occurrence, clean up the rule.
                        if (createdNewRule) {
                            runCatching { recurringRuleRepository.delete(resolvedRuleId!!) }
                        }
                        throw e
                    }.getOrThrow()
                } else {
                    val existing = getTransactionDetailsUseCase(state.transactionId!!).first()
                        ?: throw IllegalStateException(context.getString(R.string.error_transaction_not_found))
                    val updated = existing.copy(
                        accountId = state.selectedAccount!!.id,
                        toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                        categoryId = resolvedCategoryId,
                        money = Money(amountCents = amountCents, currency = state.currency),
                        type = state.type,
                        title = resolvedTitle,
                        notes = state.notes,
                        dateTime = state.dateTime,
                        receiptUri = state.receiptUri,
                        tags = tags,
                        isRecurring = state.isRecurring,
                        recurringRuleId = resolvedRuleId,
                        updatedAt = now,
                        synced = false
                    )
                    updateTransactionUseCase(updated)
                }

                budgetAlertScheduler.scheduleNow()
                if (!state.isEdit) {
                    runCatching { preferencesDataStore.incrementRateEligibleActionCount() }
                    val lifetimeActions = runCatching {
                        preferencesDataStore.userPreferences.first().rateEligibleActionCount
                    }.getOrDefault(0)
                    growthAnalyticsTracker.logTransactionCreated(state.type.name, lifetimeActions)
                }
                _formState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: context.getString(R.string.error_transaction_save)
                    )
                }
            }
        }
    }

    fun delete() {
        val id = uiState.value.transactionId ?: return
        viewModelScope.launch {
            try {
                deleteTransactionUseCase(id)
                budgetAlertScheduler.scheduleNow()
                _formState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _formState.update { it.copy(error = e.message ?: context.getString(R.string.error_transaction_delete)) }
            }
        }
    }

    private fun resolveTitle(state: AddEditTransactionUiState): String {
        val explicit = state.title.trim()
        if (explicit.isNotBlank()) return explicit

        return when (state.type) {
            TransactionType.TRANSFER -> context.getString(R.string.transaction_default_title_transfer)
            TransactionType.INCOME -> state.selectedCategory?.name
                ?: context.getString(R.string.transaction_default_title_income)
            TransactionType.EXPENSE -> state.selectedCategory?.name
                ?: context.getString(R.string.transaction_default_title_expense)
        }
    }

    private fun resolveCategoryIdForSave(state: AddEditTransactionUiState): String? {
        return when (state.type) {
            TransactionType.EXPENSE -> state.selectedCategory?.takeIf { it.type == CategoryType.EXPENSE }?.id
            TransactionType.INCOME -> state.selectedCategory?.takeIf { it.type == CategoryType.INCOME }?.id
            TransactionType.TRANSFER -> null
        }
    }

    private fun loadMerchantSuggestion(title: String) {
        suggestionJob?.cancel()
        if (title.trim().length < 3) {
            _formState.update { it.copy(merchantSuggestion = null) }
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(180)
            val prefs = preferencesDataStore.userPreferences.first()
            if (!prefs.hasPremiumAccess() || !prefs.autoCaptureEnabled || !prefs.autoCaptureMerchantMemoryEnabled) {
                _formState.update { it.copy(merchantSuggestion = null) }
                return@launch
            }
            val suggestion = getMerchantMemorySuggestionUseCase(title)
            _formState.update {
                it.copy(
                    merchantSuggestion = suggestion?.copy(
                        recurringSuggestion = suggestion.recurringSuggestion.takeIf {
                            prefs.autoCaptureRecurringSuggestionsEnabled
                        }
                    )
                )
            }
        }
    }

    private fun TransactionType.toDraftResolution(): AutoCaptureResolution = when (this) {
        TransactionType.INCOME -> AutoCaptureResolution.SAVED_AS_INCOME
        TransactionType.EXPENSE -> AutoCaptureResolution.SAVED_AS_EXPENSE
        TransactionType.TRANSFER -> AutoCaptureResolution.SAVED_AS_TRANSFER
    }
}


