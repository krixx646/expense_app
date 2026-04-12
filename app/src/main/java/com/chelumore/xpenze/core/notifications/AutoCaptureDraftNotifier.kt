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
import com.chelumore.xpenze.core.localization.localizedAutoCaptureDefaultTitle
import com.chelumore.xpenze.core.localization.localizedAutoCaptureDraftNotificationBody
import com.chelumore.xpenze.core.localization.withAppLanguage
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoCaptureDraftNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG_AUTO_CAPTURE_DRAFT = "auto_capture_draft"
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

    fun notifyDraftCaptured(
        draft: AutoCaptureDraft,
        appLanguageTag: String
    ) {
        NotificationChannels.ensureCreated(context, appLanguageTag)
        if (!canPostNotifications()) return

        val localizedContext = context.withAppLanguage(appLanguageTag)
        val notificationId = draft.id.hashCode()
        val amountText = draft.amountCents?.toCurrencyString(draft.currency)
        val canResolveDirectly = draft.amountCents != null && draft.amountCents > 0L

        val builder = NotificationCompat.Builder(context, NotificationChannels.AUTO_CAPTURE_DRAFTS_ID)
            .setSmallIcon(R.drawable.ic_notification_xpenze)
            .setContentTitle(draft.title.ifBlank { localizedAutoCaptureDefaultTitle(appLanguageTag) })
            .setContentText(localizedAutoCaptureDraftNotificationBody(amountText, appLanguageTag))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    listOfNotNull(
                        localizedAutoCaptureDraftNotificationBody(amountText, appLanguageTag),
                        draft.needsReviewReason,
                        draft.notes?.takeIf { it.isNotBlank() } ?: draft.rawText?.takeIf { it.isNotBlank() }
                    ).joinToString("\n\n")
                )
            )
            .setSubText(localizedContext.getString(R.string.auto_capture_draft_inbox_title))
            .setContentIntent(buildReviewPendingIntent(draft, notificationId))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(NotificationChannels.appNotificationSoundUri(context))
            .addAction(
                0,
                localizedContext.getString(R.string.auto_capture_exclude_button),
                buildActionPendingIntent(
                    action = AutoCaptureDraftActionReceiver.ACTION_EXCLUDE,
                    draftId = draft.id,
                    notificationId = notificationId,
                    requestCode = notificationId + 1
                )
            )

        if (canResolveDirectly) {
            builder.addAction(
                0,
                localizedContext.getString(R.string.auto_capture_save_expense_button),
                buildActionPendingIntent(
                    action = AutoCaptureDraftActionReceiver.ACTION_SAVE_EXPENSE,
                    draftId = draft.id,
                    notificationId = notificationId,
                    requestCode = notificationId + 2
                )
            )
            builder.addAction(
                0,
                localizedContext.getString(R.string.auto_capture_save_income_button),
                buildActionPendingIntent(
                    action = AutoCaptureDraftActionReceiver.ACTION_SAVE_INCOME,
                    draftId = draft.id,
                    notificationId = notificationId,
                    requestCode = notificationId + 3
                )
            )
        }

        NotificationManagerCompat.from(context).notify(
            TAG_AUTO_CAPTURE_DRAFT,
            notificationId,
            builder.build()
        )
    }

    fun notifyActionFeedback(
        draftId: String,
        appLanguageTag: String,
        message: String
    ) {
        NotificationChannels.ensureCreated(context, appLanguageTag)
        if (!canPostNotifications()) return

        val notificationId = draftId.hashCode()
        val feedbackNotification = NotificationCompat.Builder(context, NotificationChannels.AUTO_CAPTURE_DRAFTS_ID)
            .setSmallIcon(R.drawable.ic_notification_xpenze)
            .setContentTitle(context.withAppLanguage(appLanguageTag).getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setTimeoutAfter(2_500L)
            .build()

        NotificationManagerCompat.from(context).notify(
            TAG_AUTO_CAPTURE_DRAFT,
            notificationId,
            feedbackNotification
        )
    }

    fun cancelDraftNotification(draftId: String) {
        NotificationManagerCompat.from(context).cancel(
            TAG_AUTO_CAPTURE_DRAFT,
            draftId.hashCode()
        )
    }

    private fun buildReviewPendingIntent(
        draft: AutoCaptureDraft,
        requestCode: Int
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_REVIEW_DRAFT_ID, draft.id)
                putExtra(
                    MainActivity.EXTRA_REVIEW_DRAFT_TYPE,
                    (draft.suggestedType ?: TransactionType.EXPENSE).name
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )
    }

    private fun buildActionPendingIntent(
        action: String,
        draftId: String,
        notificationId: Int,
        requestCode: Int
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AutoCaptureDraftActionReceiver::class.java).apply {
                this.action = action
                putExtra(AutoCaptureDraftActionReceiver.EXTRA_DRAFT_ID, draftId)
                putExtra(AutoCaptureDraftActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
