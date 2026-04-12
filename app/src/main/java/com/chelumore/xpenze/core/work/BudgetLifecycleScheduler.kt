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
class BudgetLifecycleScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_PERIODIC = "budget_lifecycle_periodic"
        private const val UNIQUE_ON_DEMAND = "budget_lifecycle_on_demand"
        private const val TAG = "budget_lifecycle"
    }

    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<BudgetLifecycleWorker>(1, TimeUnit.DAYS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleNow() {
        val request = OneTimeWorkRequestBuilder<BudgetLifecycleWorker>()
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
