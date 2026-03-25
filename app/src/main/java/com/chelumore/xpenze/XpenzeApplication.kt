package com.chelumore.xpenze

import android.app.Application
import androidx.room.withTransaction
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkManager
import androidx.work.Configuration
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.database.dao.AccountDao
import com.chelumore.xpenze.core.database.dao.BudgetDao
import com.chelumore.xpenze.core.database.dao.CategoryDao
import com.chelumore.xpenze.core.database.dao.RecurringRuleDao
import com.chelumore.xpenze.core.database.dao.TransactionDao
import com.chelumore.xpenze.core.database.entity.CategoryEntity
import com.chelumore.xpenze.core.database.entity.TransactionEntity
import com.chelumore.xpenze.core.database.entity.TransactionType
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.notifications.NotificationChannels
import com.chelumore.xpenze.core.work.BudgetAlertScheduler
import com.chelumore.xpenze.core.work.DailySummaryScheduler
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.core.work.WeeklySavingsAlertScheduler
import com.google.android.gms.ads.MobileAds
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class XpenzeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var budgetAlertScheduler: BudgetAlertScheduler

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var dailySummaryScheduler: DailySummaryScheduler

    @Inject
    lateinit var weeklySavingsAlertScheduler: WeeklySavingsAlertScheduler

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var accountDao: AccountDao

    @Inject
    lateinit var categoryDao: CategoryDao

    @Inject
    lateinit var budgetDao: BudgetDao

    @Inject
    lateinit var recurringRuleDao: RecurringRuleDao

    @Inject
    lateinit var transactionDao: TransactionDao

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Protect callable APIs from abuse; debug builds use debug provider.
        runCatching {
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                val debugFactory = loadDebugAppCheckProviderFactory()
                if (debugFactory != null) {
                    appCheck.installAppCheckProviderFactory(debugFactory)
                } else {
                    appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        }

        // Initialize AdMob once at app startup.
        runCatching { MobileAds.initialize(this) }

        // If WorkManagerInitializer is disabled in the manifest, ensure WorkManager is initialized.
        runCatching { WorkManager.initialize(this, workManagerConfiguration) }

        // Schedule/cancel budget alerts based on user preferences.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val didDedupeRecurring = runCatching { dedupeRecurringDuplicates() }
                .getOrDefault(false)
            val didDedupeCategories = runCatching { dedupeCategoryDuplicates() }
                .getOrDefault(false)

            // Self-heal cached balances from source-of-truth transactions.
            runCatching { reconcileLocalAccountBalances() }

            val prefs = preferencesDataStore.userPreferences.first()
            NotificationChannels.ensureCreated(this@XpenzeApplication, prefs.appLanguageTag)
            if (prefs.notificationsEnabled && prefs.budgetAlertEnabled) {
                budgetAlertScheduler.scheduleDaily()
            } else {
                budgetAlertScheduler.cancelAll()
            }

            if (prefs.notificationsEnabled && prefs.dailySummaryEnabled) {
                dailySummaryScheduler.scheduleDaily()
            } else {
                dailySummaryScheduler.cancelAll()
            }

            if (prefs.notificationsEnabled) {
                weeklySavingsAlertScheduler.scheduleWeekly()
            } else {
                weeklySavingsAlertScheduler.cancelAll()
            }

            // Cloud sync only when logged in.
            if (!prefs.userId.isNullOrBlank()) {
                syncScheduler.schedulePeriodic()
                if (didDedupeRecurring || didDedupeCategories) {
                    syncScheduler.scheduleNow()
                }
            } else {
                syncScheduler.cancelAll()
            }
        }
    }

    private suspend fun dedupeRecurringDuplicates(): Boolean {
        val now = System.currentTimeMillis()
        val recurring = transactionDao.getAllAliveOnce()
            .asSequence()
            .filter { it.isRecurring && !it.recurringRuleId.isNullOrBlank() }
            .toList()
        if (recurring.isEmpty()) return false

        var changed = false
        recurring
            .groupBy { tx ->
                RecurringDuplicateKey(
                    recurringRuleId = tx.recurringRuleId.orEmpty(),
                    dateTime = tx.dateTime,
                    accountId = tx.accountId,
                    toAccountId = tx.toAccountId,
                    categoryId = tx.categoryId,
                    amount = tx.amount,
                    currency = tx.currency,
                    type = tx.type
                )
            }
            .values
            .forEach { group ->
                if (group.size <= 1) return@forEach
                group
                    .sortedWith(compareBy<TransactionEntity> { it.createdAt }.thenBy { it.id })
                    .drop(1)
                    .forEach { duplicate ->
                        transactionDao.softDelete(duplicate.id, now)
                        changed = true
                    }
            }
        return changed
    }

    private suspend fun dedupeCategoryDuplicates(): Boolean {
        var changedAny = false
        repeat(MAX_CATEGORY_DEDUPE_PASSES) {
            val passChanged = appDatabase.withTransaction {
                val categories = categoryDao.getAllAliveOnce()
                if (categories.isEmpty()) return@withTransaction false

                var changedThisPass = false
                categories
                    .groupBy { category ->
                        CategoryDuplicateKey(
                            type = category.type.name,
                            parentCategoryId = category.parentCategoryId,
                            normalizedName = normalizeCategoryName(category.name)
                        )
                    }
                    .values
                    .forEach { group ->
                        if (group.size <= 1) return@forEach
                        val survivor = group
                            .sortedWith(
                                compareByDescending<CategoryEntity> { it.isDefault }
                                    .thenBy { it.sortOrder }
                                    .thenBy { it.createdAt }
                                    .thenBy { it.id }
                            )
                            .first()
                        val now = System.currentTimeMillis()
                        group
                            .filter { it.id != survivor.id }
                            .forEach { duplicate ->
                                transactionDao.reassignCategory(duplicate.id, survivor.id, now)
                                budgetDao.reassignCategory(duplicate.id, survivor.id, now)
                                recurringRuleDao.reassignCategory(duplicate.id, survivor.id, now)
                                categoryDao.reassignParentCategory(duplicate.id, survivor.id, now)
                                categoryDao.softDelete(duplicate.id, now)
                                changedThisPass = true
                            }
                    }
                changedThisPass
            }
            changedAny = changedAny || passChanged
            if (!passChanged) return changedAny
        }
        return changedAny
    }

    private suspend fun reconcileLocalAccountBalances() {
        appDatabase.withTransaction {
            val accounts = accountDao.getAllAliveOnce()
            if (accounts.isEmpty()) return@withTransaction

            val deltas = mutableMapOf<String, Long>()
            fun add(accountId: String?, delta: Long) {
                if (accountId.isNullOrBlank() || delta == 0L) return
                deltas[accountId] = (deltas[accountId] ?: 0L) + delta
            }

            transactionDao.getAllAliveOnce().forEach { tx ->
                when (tx.type) {
                    TransactionType.INCOME -> add(tx.accountId, tx.amount)
                    TransactionType.EXPENSE -> add(tx.accountId, -tx.amount)
                    TransactionType.TRANSFER -> {
                        add(tx.accountId, -tx.amount)
                        add(tx.toAccountId, tx.amount)
                    }
                }
            }

            accounts.forEach { account ->
                val expectedBalance = account.initialBalance + (deltas[account.id] ?: 0L)
                if (expectedBalance != account.currentBalance) {
                    accountDao.updateBalance(account.id, expectedBalance)
                }
            }
        }
    }

    private data class RecurringDuplicateKey(
        val recurringRuleId: String,
        val dateTime: Long,
        val accountId: String,
        val toAccountId: String?,
        val categoryId: String?,
        val amount: Long,
        val currency: String,
        val type: TransactionType
    )

    private data class CategoryDuplicateKey(
        val type: String,
        val parentCategoryId: String?,
        val normalizedName: String
    )

    private fun normalizeCategoryName(name: String): String {
        return name.trim().lowercase().replace(Regex("""\s+"""), " ")
    }

    private fun loadDebugAppCheckProviderFactory(): AppCheckProviderFactory? {
        return runCatching {
            val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
            val getInstance = clazz.getMethod("getInstance")
            getInstance.invoke(null) as? AppCheckProviderFactory
        }.getOrNull()
    }

    private companion object {
        const val MAX_CATEGORY_DEDUPE_PASSES = 4
    }
}

