package com.chelumore.xpenze.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.usecase.export.ExportToCsvUseCase
import com.chelumore.xpenze.domain.usecase.export.ImportFromCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExportDataEvent {
    data class ShareFile(
        val bytes: ByteArray,
        val fileName: String,
        val mimeType: String
    ) : ExportDataEvent

    data class SaveFile(
        val bytes: ByteArray,
        val fileName: String,
        val mimeType: String
    ) : ExportDataEvent

    data class Message(val text: String) : ExportDataEvent
}

data class ExportDataUiState(
    val isWorking: Boolean = false,
    val hasPremiumAccess: Boolean = false,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null
) {
    fun rangeOrNull(): DateRange? {
        val start = startDateMillis
        val end = endDateMillis
        if (start == null && end == null) return null

        val resolvedStart = start ?: 0L
        val resolvedEnd = end ?: System.currentTimeMillis()
        return DateRange(startMillis = resolvedStart, endMillis = resolvedEnd)
    }
}

@HiltViewModel
class ExportDataViewModel @Inject constructor(
    private val exportToCsvUseCase: ExportToCsvUseCase,
    private val importFromCsvUseCase: ImportFromCsvUseCase,
    private val premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private val _baseState = MutableStateFlow(ExportDataUiState())
    val uiState: StateFlow<ExportDataUiState> = combine(
        _baseState,
        premiumEntitlementTracker.entitlement
    ) { state, entitlement ->
        state.copy(hasPremiumAccess = entitlement.hasPremiumAccess)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExportDataUiState()
    )

    private val eventsChannel = Channel<ExportDataEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun setStartDate(millis: Long?) {
        _baseState.update { state ->
            val end = state.endDateMillis
            val resolvedEnd = if (end != null && millis != null && end < millis) millis else end
            state.copy(startDateMillis = millis, endDateMillis = resolvedEnd)
        }
    }

    fun setEndDate(millis: Long?) {
        _baseState.update { state ->
            val start = state.startDateMillis
            val resolvedStart = if (start != null && millis != null && start > millis) millis else start
            state.copy(startDateMillis = resolvedStart, endDateMillis = millis)
        }
    }

    fun clearRange() {
        _baseState.update { it.copy(startDateMillis = null, endDateMillis = null) }
    }

    fun exportCsv() {
        viewModelScope.launch {
            if (!premiumEntitlementTracker.entitlement.value.hasPremiumAccess) {
                eventsChannel.send(ExportDataEvent.Message("CSV export is part of Premium."))
                return@launch
            }

            _baseState.update { it.copy(isWorking = true) }
            try {
                val (bytes, fileName) = buildCsvExport()
                eventsChannel.send(
                    ExportDataEvent.ShareFile(
                        bytes = bytes,
                        fileName = fileName,
                        mimeType = "text/csv"
                    )
                )
            } catch (e: Exception) {
                eventsChannel.send(ExportDataEvent.Message(e.message ?: "Export failed"))
            } finally {
                _baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun saveCsv() {
        viewModelScope.launch {
            if (!premiumEntitlementTracker.entitlement.value.hasPremiumAccess) {
                eventsChannel.send(ExportDataEvent.Message("CSV export is part of Premium."))
                return@launch
            }

            _baseState.update { it.copy(isWorking = true) }
            try {
                val (bytes, fileName) = buildCsvExport()
                eventsChannel.send(
                    ExportDataEvent.SaveFile(
                        bytes = bytes,
                        fileName = fileName,
                        mimeType = "text/csv"
                    )
                )
            } catch (e: Exception) {
                eventsChannel.send(ExportDataEvent.Message(e.message ?: "Export failed"))
            } finally {
                _baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            _baseState.update { it.copy(isWorking = true) }
            try {
                val result = importFromCsvUseCase(csv)
                eventsChannel.send(
                    ExportDataEvent.Message(
                        "Imported ${result.transactionsImported} transactions " +
                            "(+${result.accountsCreated} accounts, +${result.categoriesCreated} categories)."
                    )
                )
            } catch (e: Exception) {
                eventsChannel.send(ExportDataEvent.Message(e.message ?: "Import failed"))
            } finally {
                _baseState.update { it.copy(isWorking = false) }
            }
        }
    }

    private suspend fun buildCsvExport(): Pair<ByteArray, String> {
        val csv = exportToCsvUseCase(_baseState.value.rangeOrNull())
        val fileName = "xpenze-export-${System.currentTimeMillis()}.csv"
        return csv.toByteArray(Charsets.UTF_8) to fileName
    }
}


