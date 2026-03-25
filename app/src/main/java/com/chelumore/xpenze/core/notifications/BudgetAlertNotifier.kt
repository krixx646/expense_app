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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG_BUDGET_ALERT = "budget_alert"
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

    fun notifyBudgetAlert(
        budgetId: String,
        budgetName: String,
        percentUsed: Int,
        alertThresholdPercent: Int,
        spentCents: Long,
        limitCents: Long,
        overspentCents: Long,
        currencyCode: String,
        appLanguageTag: String
    ) {
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

        val notificationId = budgetId.hashCode()

        val spent = spentCents.toCurrencyString(currencyCode)
        val limit = limitCents.toCurrencyString(currencyCode)
        val over = overspentCents.takeIf { it > 0L }?.toCurrencyString(currencyCode)
        val copy = budgetAlertCopy(
            languageTag = appLanguageTag,
            budgetName = budgetName,
            percentUsed = percentUsed,
            alertThresholdPercent = alertThresholdPercent,
            spentText = spent,
            limitText = limit,
            overspentText = over
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.BUDGET_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification_xpenze)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(NotificationChannels.appNotificationSoundUri(context))

        manager.notify(TAG_BUDGET_ALERT, notificationId, builder.build())
    }

    fun cancelBudgetAlert(budgetId: String) {
        NotificationManagerCompat.from(context).cancel(TAG_BUDGET_ALERT, budgetId.hashCode())
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}


