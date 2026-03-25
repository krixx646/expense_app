package com.chelumore.xpenze.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chelumore.xpenze.MainActivity
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.localization.withAppLanguage
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG_DAILY_SUMMARY = "daily_summary"
        private const val NOTIFICATION_ID = 1001
    }

    fun canPostNotifications(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyDailySummary(
        incomeCents: Long,
        expenseCents: Long,
        netCents: Long,
        currencyCode: String,
        transactionCount: Int,
        appLanguageTag: String
    ) {
        val localizedContext = context.withAppLanguage(appLanguageTag)
        NotificationChannels.ensureCreated(context, appLanguageTag)
        // Lint doesn't understand the permission check when it's encapsulated, so guard explicitly here.
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val income = incomeCents.toCurrencyString(currencyCode)
        val expense = expenseCents.toCurrencyString(currencyCode)
        val net = netCents.toCurrencyString(currencyCode)

        val hasLoggedTransactions = transactionCount > 0
        val variant = PremiumEmotionVariant.forTimestamp(System.currentTimeMillis())
        val (title, contentText) = if (hasLoggedTransactions) {
            when (variant) {
                PremiumEmotionVariant.PLAYFUL -> {
                    localizedContext.getString(R.string.notification_daily_title_playful) to
                        localizedContext.getString(R.string.notification_daily_text_playful, income, expense, net)
                }
                PremiumEmotionVariant.ADVISOR -> {
                    localizedContext.getString(R.string.notification_daily_title_advisor) to
                        localizedContext.getString(R.string.notification_daily_text_advisor, income, expense, net)
                }
                PremiumEmotionVariant.URGENT_SOFT -> {
                    localizedContext.getString(R.string.notification_daily_title_urgent) to
                        localizedContext.getString(R.string.notification_daily_text_urgent, net)
                }
            }
        } else {
            when (variant) {
                PremiumEmotionVariant.PLAYFUL -> {
                    localizedContext.getString(R.string.notification_empty_title_playful) to
                        localizedContext.getString(R.string.notification_empty_text_playful)
                }
                PremiumEmotionVariant.ADVISOR -> {
                    localizedContext.getString(R.string.notification_empty_title_advisor) to
                        localizedContext.getString(R.string.notification_empty_text_advisor)
                }
                PremiumEmotionVariant.URGENT_SOFT -> {
                    localizedContext.getString(R.string.notification_empty_title_urgent) to
                        localizedContext.getString(R.string.notification_empty_text_urgent)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.DAILY_SUMMARY_ID)
            .setSmallIcon(R.drawable.ic_notification_xpenze)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(NotificationChannels.appNotificationSoundUri(context))

        manager.notify(TAG_DAILY_SUMMARY, NOTIFICATION_ID, builder.build())
    }

    fun cancelDailySummary() {
        NotificationManagerCompat.from(context).cancel(TAG_DAILY_SUMMARY, NOTIFICATION_ID)
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}


