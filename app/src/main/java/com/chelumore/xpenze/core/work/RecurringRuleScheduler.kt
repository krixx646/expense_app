package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRuleScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_PERIODIC = "recurring_rules_periodic"
        private const val UNIQUE_ON_DEMAND = "recurring_rules_on_demand"
        private const val TAG = "recurring_rules"
    }

    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<RecurringRuleWorker>(1, TimeUnit.DAYS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleNow() {
        val request = OneTimeWorkRequestBuilder<RecurringRuleWorker>()
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ON_DEMAND, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(UNIQUE_ON_DEMAND)
        wm.cancelUniqueWork(UNIQUE_PERIODIC)
        wm.cancelAllWorkByTag(TAG)
    }
}


