package com.chelumore.xpenze.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.chelumore.xpenze.R

object NotificationChannels {
    // Channel IDs are versioned so sound/behavior updates apply immediately on installed apps.
    const val BUDGET_ALERTS_ID: String = "budget_alerts_v4"
    const val DAILY_SUMMARY_ID: String = "daily_summary_v4"
    const val MOCHI_SAVINGS_ID: String = "mochi_insights_v2"
    const val AUTO_CAPTURE_DRAFTS_ID: String = "auto_capture_drafts_v1"

    fun ensureCreated(context: Context, appLanguageTag: String? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = appNotificationSoundUri(context)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (manager.getNotificationChannel(BUDGET_ALERTS_ID) == null) {
            val copy = notificationChannelCopy(
                kind = NotificationChannelKind.BUDGETS,
                languageTag = appLanguageTag.orEmpty()
            )
            val channel = NotificationChannel(
                BUDGET_ALERTS_ID,
                copy.name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = copy.description
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        if (manager.getNotificationChannel(DAILY_SUMMARY_ID) == null) {
            val copy = notificationChannelCopy(
                kind = NotificationChannelKind.DAILY,
                languageTag = appLanguageTag.orEmpty()
            )
            val channel = NotificationChannel(
                DAILY_SUMMARY_ID,
                copy.name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = copy.description
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        if (manager.getNotificationChannel(MOCHI_SAVINGS_ID) == null) {
            val copy = notificationChannelCopy(
                kind = NotificationChannelKind.MOCHI,
                languageTag = appLanguageTag.orEmpty()
            )
            val channel = NotificationChannel(
                MOCHI_SAVINGS_ID,
                copy.name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = copy.description
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        if (manager.getNotificationChannel(AUTO_CAPTURE_DRAFTS_ID) == null) {
            val copy = notificationChannelCopy(
                kind = NotificationChannelKind.AUTO_CAPTURE,
                languageTag = appLanguageTag.orEmpty()
            )
            val channel = NotificationChannel(
                AUTO_CAPTURE_DRAFTS_ID,
                copy.name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = copy.description
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun appNotificationSoundUri(context: Context): Uri? {
        return Uri.parse("android.resource://${context.packageName}/${R.raw.xpenze_notify}")
    }
}


