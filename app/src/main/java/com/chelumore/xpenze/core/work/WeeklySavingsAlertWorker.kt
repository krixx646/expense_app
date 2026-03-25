package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.growth.buildPremiumOpportunities
import com.chelumore.xpenze.core.notifications.buildPersonalizedMochiAlert
import com.chelumore.xpenze.core.notifications.MochiSavingsNotifier
import com.chelumore.xpenze.core.notifications.NotificationChannels
import com.chelumore.xpenze.domain.repository.BudgetRepository
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import com.chelumore.xpenze.domain.repository.hasPremiumAccess
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WeeklySavingsAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferencesDataStore: PreferencesDataStore,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringRuleRepository: RecurringRuleRepository,
    private val notifier: MochiSavingsNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = preferencesDataStore.userPreferences.first()
            if (!prefs.notificationsEnabled || !prefs.hasPremiumAccess()) {
                notifier.cancelWeeklySavingsAlert()
                return Result.success()
            }

            NotificationChannels.ensureCreated(applicationContext, prefs.appLanguageTag)
            if (!notifier.canPostNotifications()) return Result.success()

            val transactions = transactionRepository.getAll().first()
            val categories = categoryRepository.getAll().first()
            val budgets = budgetRepository.getAll().first()
            val recurringRules = recurringRuleRepository.getAll().first()
            val rawHistory = preferencesDataStore.aiChatHistoryJson.first()
            val opportunities = buildPremiumOpportunities(
                transactions = transactions,
                categories = categories,
                budgets = budgets,
                currencyCode = prefs.defaultCurrency
            )

            val personalized = buildPersonalizedMochiAlert(
                rawHistory = rawHistory,
                opportunities = opportunities,
                transactions = transactions,
                categories = categories,
                budgets = budgets,
                recurringRules = recurringRules,
                currencyCode = prefs.defaultCurrency,
                languageTag = prefs.appLanguageTag
            )

            if (personalized != null) {
                notifier.notifyAlert(
                    title = personalized.title,
                    body = personalized.body,
                    prompt = personalized.prompt,
                    appLanguageTag = prefs.appLanguageTag
                )
            } else {
                notifier.notifyWeeklySavingsAlert(
                    opportunities = opportunities,
                    currencyCode = prefs.defaultCurrency,
                    appLanguageTag = prefs.appLanguageTag,
                    userName = prefs.userName ?: prefs.userEmail?.substringBefore("@")
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
