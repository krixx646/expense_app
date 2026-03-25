package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.notifications.DailySummaryNotifier
import com.chelumore.xpenze.core.notifications.NotificationChannels
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferencesDataStore: PreferencesDataStore,
    private val transactionRepository: TransactionRepository,
    private val notifier: DailySummaryNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = preferencesDataStore.userPreferences.first()
            val notificationsEnabled = prefs.notificationsEnabled && prefs.dailySummaryEnabled

            if (!notificationsEnabled) {
                notifier.cancelDailySummary()
                return Result.success()
            }

            NotificationChannels.ensureCreated(applicationContext, prefs.appLanguageTag)
            if (!notifier.canPostNotifications()) return Result.success()

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val txs = transactionRepository.getByDateRange(start, end).first()
            val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }
            val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
            val net = income - expense

            notifier.notifyDailySummary(
                incomeCents = income,
                expenseCents = expense,
                netCents = net,
                currencyCode = prefs.defaultCurrency,
                transactionCount = txs.size,
                appLanguageTag = prefs.appLanguageTag
            )

            Result.success()
        } catch (_: Exception) {
            // Avoid retry loops; we can try again on the next run.
            Result.success()
        }
    }
}


