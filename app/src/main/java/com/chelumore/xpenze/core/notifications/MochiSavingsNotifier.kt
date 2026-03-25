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
import com.chelumore.xpenze.core.growth.buildMochiPromptForOpportunity
import com.chelumore.xpenze.core.growth.formatMochiWeeklyAlert
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MochiSavingsNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG_MOCHI_SAVINGS = "mochi_savings"
        private const val NOTIFICATION_ID = 1002
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

    fun notifyWeeklySavingsAlert(
        opportunities: List<PremiumOpportunity>,
        currencyCode: String,
        appLanguageTag: String,
        userName: String?
    ) {
        val copy = formatMochiWeeklyAlert(
            opportunities = opportunities,
            currencyCode = currencyCode,
            languageTag = appLanguageTag,
            userName = userName
        ) ?: run {
            cancelWeeklySavingsAlert()
            return
        }
        val primaryOpportunity = opportunities.firstOrNull()
        val prompt = primaryOpportunity?.let {
            buildMochiPromptForOpportunity(
                opportunity = it,
                currencyCode = currencyCode,
                languageTag = appLanguageTag
            )
        }

        notifyAlert(
            title = copy.title,
            body = copy.body,
            prompt = prompt,
            appLanguageTag = appLanguageTag
        )
    }

    fun notifyAlert(
        title: String,
        body: String,
        prompt: String?,
        appLanguageTag: String
    ) {
        NotificationChannels.ensureCreated(context, appLanguageTag)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!prompt.isNullOrBlank()) {
                    putExtra(MainActivity.EXTRA_MOCHI_PROMPT, prompt)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.MOCHI_SAVINGS_ID)
            .setSmallIcon(R.drawable.ic_notification_xpenze)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(NotificationChannels.appNotificationSoundUri(context))
            .build()

        manager.notify(TAG_MOCHI_SAVINGS, NOTIFICATION_ID, notification)
    }

    fun cancelWeeklySavingsAlert() {
        NotificationManagerCompat.from(context).cancel(TAG_MOCHI_SAVINGS, NOTIFICATION_ID)
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
