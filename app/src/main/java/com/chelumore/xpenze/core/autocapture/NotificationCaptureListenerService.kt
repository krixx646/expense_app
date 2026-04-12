package com.chelumore.xpenze.core.autocapture

import android.content.ComponentName
import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.chelumore.xpenze.core.ui.util.hasNotificationListenerAccess
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

        val title = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        ).mapNotNull { it?.toString()?.trim() }
            .firstOrNull { it.isNotBlank() }

        val text = buildList {
            add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
            add(extras.getCharSequence(Notification.EXTRA_TEXT))
            add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
            add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
            add(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.forEach { add(it) }
            add(safeNotification.notification.tickerText)
        }.mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
            .ifBlank { null }
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationCaptureServiceEntryPoint::class.java
        )

        serviceScope.launch {
            val prefs = entryPoint.userRepository().userPreferences.first()
            if (!prefs.hasPremiumAccess()) return@launch
            if (!prefs.autoCaptureEnabled || !prefs.autoCaptureNotificationEnabled) return@launch
            val allowedPackages = prefs.autoCaptureNotificationAllowedPackages
            if (
                allowedPackages.isNotEmpty() &&
                safeNotification.packageName !in allowedPackages
            ) {
                return@launch
            }

            val appLabel = runCatching {
                val packageInfo = packageManager.getApplicationInfo(safeNotification.packageName, 0)
                packageManager.getApplicationLabel(packageInfo).toString()
            }.getOrNull()

            val draft = entryPoint.captureNotificationToDraftUseCase().invoke(
                packageName = safeNotification.packageName,
                appLabel = appLabel,
                notificationTitle = title,
                notificationText = text,
                postedAt = safeNotification.postTime,
                accountId = prefs.defaultAccountId,
                fallbackCurrency = prefs.defaultCurrency
            )
            if (draft != null) {
                entryPoint.autoCaptureDraftNotifier().notifyDraftCaptured(
                    draft = draft,
                    appLanguageTag = prefs.appLanguageTag
                )
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerDisconnected() {
        requestRebindIfAllowed(applicationContext)
        super.onListenerDisconnected()
    }

    companion object {
        fun requestRebindIfAllowed(context: Context) {
            if (!context.hasNotificationListenerAccess()) return
            runCatching {
                requestRebind(
                    ComponentName(
                        context,
                        NotificationCaptureListenerService::class.java
                    )
                )
            }
        }
    }
}
