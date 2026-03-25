package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_PERIODIC_PREFIX = "daily_summary_periodic_"
        private const val UNIQUE_ON_DEMAND = "daily_summary_on_demand"
        private const val TAG = "daily_summary"
        private val DEFAULT_REMINDER_SLOTS: List<Pair<Int, Int>> = listOf(
            7 to 30,   // Morning wake-up reminder
            8 to 0,    // Morning kick-off
            9 to 0,    // Early focus check-in
            10 to 30,  // Late morning check-in
            12 to 0,   // Midday check-in
            13 to 30,  // Post-lunch reminder
            15 to 0,   // Afternoon nudge
            17 to 0,   // Late afternoon closeout
            18 to 30,  // Early evening reminder
            20 to 0,   // Night check-in
            21 to 0,   // End-of-day reminder
            22 to 30   // Last nightly reminder
        )
    }

    fun scheduleDaily() {
        val slots = DEFAULT_REMINDER_SLOTS
            .distinct()
            .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val wm = WorkManager.getInstance(context)

        slots.forEach { (hour, minute) ->
            var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next).toMillis().coerceAtLeast(0L)

            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build()

            wm.enqueueUniquePeriodicWork(
                "$UNIQUE_PERIODIC_PREFIX${hour}_${minute}",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun scheduleNow() {
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ON_DEMAND, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(UNIQUE_ON_DEMAND)
        wm.cancelAllWorkByTag(TAG)
    }
}


