package com.chelumore.xpenze.feature.autocapture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.localization.localizedAutoCaptureChooseAccountFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureChooseDestinationFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureConfirmAmountFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureDraftMissingMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureEnableOcrFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureEnableStatementFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureExcludedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureMarkedPassThroughMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureOcrAddedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureOcrFailedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureOcrFailedParseMessage
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumRequiredMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureResolvedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSaveFailedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSavedToTransactionsMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatementFormatError
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatementImportFailedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatementImportSummaryMessage
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.chelumore.xpenze.domain.usecase.account.GetAccountsUseCase
import com.chelumore.xpenze.domain.usecase.autocapture.CaptureReceiptToDraftUseCase
import com.chelumore.xpenze.domain.usecase.autocapture.ImportStatementToDraftsUseCase
import com.chelumore.xpenze.domain.usecase.autocapture.ResolveAutoCaptureDraftException
import com.chelumore.xpenze.domain.usecase.autocapture.ResolveAutoCaptureDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DraftInboxUiState(
    val isWorking: Boolean = false,
    val hasPremiumAccess: Boolean = false,
    val isGuestMode: Boolean = false,
    val appLanguageTag: String = "en",
    val autoCaptureEnabled: Boolean = false,
    val autoCaptureDisclosureAccepted: Boolean = false,
    val statementImportEnabled: Boolean = false,
    val ocrEnabled: Boolean = false,
    val notificationCaptureEnabled: Boolean = false,
    val notificationAllowedPackages: Set<String> = emptySet(),
    val merchantMemoryEnabled: Boolean = false,
    val recurringSuggestionsEnabled: Boolean = false,
    val drafts: List<AutoCaptureDraft> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedImportAccountId: String? = null,
    val defaultCurrency: String = Constants.DEFAULT_CURRENCY
) {
    val selectedImportAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedImportAccountId } ?: accounts.firstOrNull()
}

@HiltViewModel
class DraftInboxViewModel @Inject constructor(
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository,
    private val importStatementToDraftsUseCase: ImportStatementToDraftsUseCase,
    private val captureReceiptToDraftUseCase: CaptureReceiptToDraftUseCase,
    private val resolveAutoCaptureDraftUseCase: ResolveAutoCaptureDraftUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val preferencesDataStore: PreferencesDataStore,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    sealed interface DraftInboxEvent {
        data class Message(val text: String) : DraftInboxEvent
    }

    private val baseState = MutableStateFlow(DraftInboxUiState())
    private val eventsChannel = Channel<DraftInboxEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<DraftInboxUiState> = combine(
        baseState,
        autoCaptureDraftRepository.getPending(),
        getAccountsUseCase(includeArchived = true),
        preferencesDataStore.userPreferences,
        premiumEntitlementTracker.entitlement
    ) { state, drafts, accounts, prefs, entitlement ->
        val selectedImportId = state.selectedImportAccountId
            ?: prefs.defaultAccountId
            ?: accounts.firstOrNull()?.id
        state.copy(
            hasPremiumAccess = entitlement.hasPremiumAccess,
            isGuestMode = prefs.isGuestMode || prefs.userId.isNullOrBlank(),
            appLanguageTag = prefs.appLanguageTag,
            autoCaptureEnabled = prefs.autoCaptureEnabled,
            autoCaptureDisclosureAccepted = prefs.autoCaptureDisclosureAccepted,
            statementImportEnabled = prefs.autoCaptureStatementImportEnabled,
            ocrEnabled = prefs.autoCaptureOcrEnabled,
            notificationCaptureEnabled = prefs.autoCaptureNotificationEnabled,
            notificationAllowedPackages = prefs.autoCaptureNotificationAllowedPackages,
            merchantMemoryEnabled = prefs.autoCaptureMerchantMemoryEnabled,
            recurringSuggestionsEnabled = prefs.autoCaptureRecurringSuggestionsEnabled,
            drafts = drafts,
            accounts = accounts,
            selectedImportAccountId = selectedImportId,
            defaultCurrency = prefs.defaultCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DraftInboxUiState()
    )

    fun setImportAccount(accountId: String?) {
        baseState.update { it.copy(selectedImportAccountId = accountId) }
    }

    fun setAutoCaptureDisclosureAccepted() {
        viewModelScope.launch {
            preferencesDataStore.setAutoCaptureDisclosureAccepted(true)
        }
    }

    fun toggleAutoCapture(enabled: Boolean) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureEnabled(enabled)
        }
    }

    fun toggleAutoCaptureStatementImport(enabled: Boolean) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureStatementImportEnabled(enabled)
        }
    }

    fun toggleAutoCaptureOcr(enabled: Boolean) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureOcrEnabled(enabled)
        }
    }

    fun toggleAutoCaptureNotification(enabled: Boolean) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureNotificationEnabled(enabled)
        }
    }

    fun setAutoCaptureNotificationAllowedPackages(packageNames: Set<String>) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureNotificationAllowedPackages(packageNames)
        }
    }

    fun toggleAutoCaptureMerchantMemory(enabled: Boolean) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureMerchantMemoryEnabled(enabled)
        }
    }

    fun toggleAutoCaptureRecurringSuggestions(enabled: Boolean) {
        viewModelScope.launch {
            if (!canManageAutoCapture()) return@launch
            preferencesDataStore.setAutoCaptureRecurringSuggestionsEnabled(enabled)
        }
    }

    fun importStatement(rawStatement: String) {
        val state = uiState.value
        if (!state.hasPremiumAccess) {
            sendMessage(localizedAutoCapturePremiumRequiredMessage())
            return
        }
        if (!state.autoCaptureEnabled || !state.statementImportEnabled) {
            sendMessage(localizedAutoCaptureEnableStatementFirstMessage())
            return
        }

        viewModelScope.launch {
            baseState.update { it.copy(isWorking = true) }
            try {
                val result = importStatementToDraftsUseCase(
                    rawStatement = rawStatement,
                    accountId = state.selectedImportAccount?.id,
                    fallbackCurrency = state.selectedImportAccount?.currency ?: state.defaultCurrency
                )
                eventsChannel.send(
                    DraftInboxEvent.Message(
                        localizedAutoCaptureStatementImportSummaryMessage(
                            draftsCreated = result.draftsCreated,
                            rowsSkipped = result.rowsSkipped
                        )
                    )
                )
            } catch (e: Exception) {
                eventsChannel.send(
                    DraftInboxEvent.Message(
                        if (e is IllegalArgumentException) {
                            localizedAutoCaptureStatementFormatError()
                        } else {
                            localizedAutoCaptureStatementImportFailedMessage()
                        }
                    )
                )
            } finally {
                baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun captureReceipt(uriString: String) {
        val state = uiState.value
        if (!state.hasPremiumAccess) {
            sendMessage(localizedAutoCapturePremiumRequiredMessage())
            return
        }
        if (!state.autoCaptureEnabled || !state.ocrEnabled) {
            sendMessage(localizedAutoCaptureEnableOcrFirstMessage())
            return
        }

        viewModelScope.launch {
            baseState.update { it.copy(isWorking = true) }
            try {
                val added = captureReceiptToDraftUseCase(
                    imageUri = uriString,
                    accountId = state.selectedImportAccount?.id,
                    fallbackCurrency = state.selectedImportAccount?.currency ?: state.defaultCurrency
                )
                eventsChannel.send(
                    DraftInboxEvent.Message(
                        if (added) {
                            localizedAutoCaptureOcrAddedMessage()
                        } else {
                            localizedAutoCaptureOcrFailedParseMessage()
                        }
                    )
                )
            } catch (e: Exception) {
                eventsChannel.send(
                    DraftInboxEvent.Message(
                        localizedAutoCaptureOcrFailedMessage()
                    )
                )
            } finally {
                baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun saveAsExpense(draftId: String) {
        resolveAsTransaction(
            draftId = draftId,
            type = TransactionType.EXPENSE,
            resolution = AutoCaptureResolution.SAVED_AS_EXPENSE
        )
    }

    fun saveAsIncome(draftId: String) {
        resolveAsTransaction(
            draftId = draftId,
            type = TransactionType.INCOME,
            resolution = AutoCaptureResolution.SAVED_AS_INCOME
        )
    }

    fun saveAsReimbursement(draftId: String) {
        resolveAsTransaction(
            draftId = draftId,
            type = TransactionType.INCOME,
            resolution = AutoCaptureResolution.SAVED_AS_REIMBURSEMENT
        )
    }

    fun markPassThrough(draftId: String) {
        markResolvedWithoutTransaction(draftId, AutoCaptureResolution.PASS_THROUGH)
    }

    fun markExcluded(draftId: String) {
        markResolvedWithoutTransaction(draftId, AutoCaptureResolution.EXCLUDED)
    }

    private fun resolveAsTransaction(
        draftId: String,
        type: TransactionType,
        resolution: AutoCaptureResolution
    ) {
        viewModelScope.launch {
            baseState.update { it.copy(isWorking = true) }
            try {
                resolveAutoCaptureDraftUseCase.saveAsTransaction(
                    draftId = draftId,
                    type = type,
                    resolution = resolution
                )
                eventsChannel.send(
                    DraftInboxEvent.Message(localizedAutoCaptureSavedToTransactionsMessage())
                )
            } catch (e: ResolveAutoCaptureDraftException) {
                eventsChannel.send(DraftInboxEvent.Message(e.toUiMessage()))
            } catch (e: Exception) {
                eventsChannel.send(
                    DraftInboxEvent.Message(
                        localizedAutoCaptureSaveFailedMessage()
                    )
                )
            } finally {
                baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    private fun markResolvedWithoutTransaction(
        draftId: String,
        resolution: AutoCaptureResolution
    ) {
        viewModelScope.launch {
            baseState.update { it.copy(isWorking = true) }
            try {
                resolveAutoCaptureDraftUseCase.markResolved(draftId, resolution)
                eventsChannel.send(
                    DraftInboxEvent.Message(
                        when (resolution) {
                            AutoCaptureResolution.PASS_THROUGH -> localizedAutoCaptureMarkedPassThroughMessage()
                            AutoCaptureResolution.EXCLUDED -> localizedAutoCaptureExcludedMessage()
                            else -> localizedAutoCaptureResolvedMessage()
                        }
                    )
                )
            } catch (e: ResolveAutoCaptureDraftException) {
                eventsChannel.send(DraftInboxEvent.Message(e.toUiMessage()))
            } finally {
                baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    private fun canManageAutoCapture(): Boolean {
        val state = uiState.value
        return state.hasPremiumAccess && !state.isGuestMode
    }

    private fun sendMessage(text: String) {
        viewModelScope.launch {
            eventsChannel.send(DraftInboxEvent.Message(text))
        }
    }

    private fun ResolveAutoCaptureDraftException.toUiMessage(): String = when (this) {
        ResolveAutoCaptureDraftException.DraftMissing -> localizedAutoCaptureDraftMissingMessage()
        ResolveAutoCaptureDraftException.ConfirmAmountFirst -> localizedAutoCaptureConfirmAmountFirstMessage()
        ResolveAutoCaptureDraftException.ChooseAccountFirst -> localizedAutoCaptureChooseAccountFirstMessage()
        ResolveAutoCaptureDraftException.ChooseDestinationFirst -> localizedAutoCaptureChooseDestinationFirstMessage()
    }
}
