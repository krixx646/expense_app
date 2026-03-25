package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.chelumore.xpenze.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_PERIODIC = "cloud_sync_periodic"
        private const val UNIQUE_ON_DEMAND = "cloud_sync_on_demand"
        private const val TAG = "cloud_sync"
    }

    fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            Constants.SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
            Constants.SYNC_FLEX_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10L,
                TimeUnit.SECONDS
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            // KEEP avoids interrupting an active sync worker mid-run.
            // REPLACE can cancel running sync and leave partial state.
            .enqueueUniqueWork(UNIQUE_ON_DEMAND, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(UNIQUE_ON_DEMAND)
        wm.cancelUniqueWork(UNIQUE_PERIODIC)
        wm.cancelAllWorkByTag(TAG)
    }
}


