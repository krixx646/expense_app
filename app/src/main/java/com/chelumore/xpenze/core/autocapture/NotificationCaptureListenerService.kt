package com.chelumore.xpenze.core.autocapture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.chelumore.xpenze.domain.repository.hasPremiumAccess
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationCaptureListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val safeNotification = sbn ?: return
        if (safeNotification.packageName == packageName) return
        val extras = safeNotification.notification.extras ?: return
        if ((safeNotification.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationCaptureServiceEntryPoint::class.java
        )

        serviceScope.launch {
            val prefs = entryPoint.userRepository().userPreferences.first()
            if (!prefs.hasPremiumAccess()) return@launch
            if (!prefs.autoCaptureEnabled || !prefs.autoCaptureNotificationEnabled) return@launch

            val appLabel = runCatching {
                val packageInfo = packageManager.getApplicationInfo(safeNotification.packageName, 0)
                packageManager.getApplicationLabel(packageInfo).toString()
            }.getOrNull()

            entryPoint.captureNotificationToDraftUseCase().invoke(
                packageName = safeNotification.packageName,
                appLabel = appLabel,
                notificationTitle = title,
                notificationText = text,
                postedAt = safeNotification.postTime,
                accountId = prefs.defaultAccountId,
                fallbackCurrency = prefs.defaultCurrency
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
