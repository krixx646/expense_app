package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklySavingsAlertScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_WEEKLY_WORK = "mochi_savings_weekly"
        private const val TAG = "mochi_savings_weekly"
        private const val TARGET_HOUR = 9
        private const val TARGET_MINUTE = 0
    }

    fun scheduleWeekly() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var next = now.with(DayOfWeek.MONDAY)
            .withHour(TARGET_HOUR)
            .withMinute(TARGET_MINUTE)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusWeeks(1)
        val initialDelay = Duration.between(now, next).toMillis().coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<WeeklySavingsAlertWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WEEKLY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_WEEKLY_WORK)
        workManager.cancelAllWorkByTag(TAG)
    }
}
