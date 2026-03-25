package com.chelumore.xpenze.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class AdvancedReportsUiState(
    val isLoading: Boolean = true,
    val hasPremiumAccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val nudgeVariant: PremiumEmotionVariant = PremiumEmotionVariant.PLAYFUL,
    val currencyCode: String = "USD",
    val range: DateRange,
    val recurringExpenseCents: Long = 0L,
    val recurringIncomeCents: Long = 0L,
    val recurringCount: Int = 0
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    premiumEntitlementTracker: PremiumEntitlementTracker
) : ViewModel() {

    private val _range = MutableStateFlow(defaultMonthRange())

    private val transactionsInRange =
        _range.flatMapLatest { range -> transactionRepository.getByDateRange(range.startMillis, range.endMillis) }

    val uiState: StateFlow<AdvancedReportsUiState> = combine(
        premiumEntitlementTracker.entitlement,
        _range,
        transactionsInRange,
        userRepository.userPreferences
    ) { entitlement, range, txs, prefs ->
        val recurring = txs.filter { it.isRecurring }
        AdvancedReportsUiState(
            isLoading = false,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            isTrialActive = entitlement.isTrialActive,
            nudgeVariant = PremiumEmotionVariant.fromSeed(
                prefs.userId ?: prefs.userEmail ?: prefs.defaultCurrency
            ),
            currencyCode = prefs.defaultCurrency,
            range = range,
            recurringExpenseCents = recurring.sumOfExpenseCents(),
            recurringIncomeCents = recurring.sumOfIncomeCents(),
            recurringCount = recurring.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdvancedReportsUiState(isLoading = true, hasPremiumAccess = false, range = defaultMonthRange())
    )

    fun setStartDate(millis: Long) {
        _range.value = DateRange(startMillis = millis, endMillis = maxOf(millis, _range.value.endMillis))
    }

    fun setEndDate(millis: Long) {
        _range.value = DateRange(startMillis = minOf(millis, _range.value.startMillis), endMillis = millis)
    }

    private fun List<Transaction>.sumOfExpenseCents(): Long =
        filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }

    private fun List<Transaction>.sumOfIncomeCents(): Long =
        filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }

    private companion object {
        fun defaultMonthRange(): DateRange {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
            val end = now.toInstant().toEpochMilli()
            return DateRange(startMillis = start, endMillis = end)
        }
    }
}


