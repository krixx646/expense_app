package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.notifications.BudgetAlertNotifier
import com.chelumore.xpenze.core.notifications.NotificationChannels
import com.chelumore.xpenze.domain.repository.BudgetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import kotlinx.coroutines.flow.first

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferencesDataStore: PreferencesDataStore,
    private val budgetRepository: BudgetRepository,
    private val notifier: BudgetAlertNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = preferencesDataStore.userPreferences.first()
            val notificationsEnabled = prefs.notificationsEnabled && prefs.budgetAlertEnabled

            // If alerts are disabled, clear any previously-posted budget notifications.
            if (!notificationsEnabled) {
                clearPreviousAlerts()
                return Result.success()
            }

            NotificationChannels.ensureCreated(applicationContext, prefs.appLanguageTag)
            if (!notifier.canPostNotifications()) return Result.success()

            val budgetsWithProgress = budgetRepository.getActiveWithProgress().first()
            val currencyCode = prefs.defaultCurrency

            val currentAlertIds = mutableSetOf<String>()

            budgetsWithProgress.forEach { item ->
                val budget = item.budget
                val limit = budget.amountLimitCents
                if (!budget.isActive || limit <= 0L) return@forEach

                val spent = item.spentCents
                val percentUsed = ((spent.toDouble() / limit.toDouble()) * 100.0).toInt()

                if (percentUsed >= budget.alertThresholdPercent) {
                    currentAlertIds.add(budget.id)
                    val overspent = (spent - limit).coerceAtLeast(0L)
                    notifier.notifyBudgetAlert(
                        budgetId = budget.id,
                        budgetName = budget.name,
                        percentUsed = percentUsed,
                        alertThresholdPercent = budget.alertThresholdPercent,
                        spentCents = spent,
                        limitCents = limit,
                        overspentCents = overspent,
                        currencyCode = currencyCode,
                        appLanguageTag = prefs.appLanguageTag
                    )
                }
            }

            // Cancel stale alerts (e.g., after transaction deletes/edits or budget edits).
            val previousAlertIds = preferencesDataStore.getBudgetAlertActiveIds()
            val toCancel = previousAlertIds - currentAlertIds
            toCancel.forEach { notifier.cancelBudgetAlert(it) }
            preferencesDataStore.setBudgetAlertActiveIds(currentAlertIds)

            Result.success()
        } catch (_: Exception) {
            // Avoid retry loops; we can try again on the next periodic/triggered run.
            Result.success()
        }
    }

    private suspend fun clearPreviousAlerts() {
        val previousAlertIds = preferencesDataStore.getBudgetAlertActiveIds()
        previousAlertIds.forEach { notifier.cancelBudgetAlert(it) }
        preferencesDataStore.setBudgetAlertActiveIds(emptySet())
    }
}


