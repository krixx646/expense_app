package com.chelumore.xpenze.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.localization.localizedAutoCaptureChooseAccountFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureChooseDestinationFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureConfirmAmountFirstMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureDraftMissingMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureExcludedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureMarkedPassThroughMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSaveFailedMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSavedToTransactionsMessage
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.usecase.autocapture.ResolveAutoCaptureDraftException
import com.chelumore.xpenze.domain.usecase.autocapture.ResolveAutoCaptureDraftUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AutoCaptureDraftActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var resolveAutoCaptureDraftUseCase: ResolveAutoCaptureDraftUseCase

    @Inject
    lateinit var autoCaptureDraftNotifier: AutoCaptureDraftNotifier

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    override fun onReceive(context: Context, intent: Intent) {
        val draftId = intent.getStringExtra(EXTRA_DRAFT_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = preferencesDataStore.userPreferences.first()
                val appLanguageTag = prefs.appLanguageTag
                val feedback = when (intent.action) {
                    ACTION_SAVE_EXPENSE -> {
                        resolveAutoCaptureDraftUseCase.saveAsTransaction(
                            draftId = draftId,
                            type = TransactionType.EXPENSE,
                            resolution = AutoCaptureResolution.SAVED_AS_EXPENSE
                        )
                        localizedAutoCaptureSavedToTransactionsMessage(appLanguageTag)
                    }

                    ACTION_SAVE_INCOME -> {
                        resolveAutoCaptureDraftUseCase.saveAsTransaction(
                            draftId = draftId,
                            type = TransactionType.INCOME,
                            resolution = AutoCaptureResolution.SAVED_AS_INCOME
                        )
                        localizedAutoCaptureSavedToTransactionsMessage(appLanguageTag)
                    }

                    ACTION_SAVE_TRANSFER -> {
                        resolveAutoCaptureDraftUseCase.saveAsTransaction(
                            draftId = draftId,
                            type = TransactionType.TRANSFER,
                            resolution = AutoCaptureResolution.SAVED_AS_TRANSFER
                        )
                        localizedAutoCaptureSavedToTransactionsMessage(appLanguageTag)
                    }

                    ACTION_MARK_PASS_THROUGH -> {
                        resolveAutoCaptureDraftUseCase.markResolved(
                            draftId = draftId,
                            resolution = AutoCaptureResolution.PASS_THROUGH
                        )
                        localizedAutoCaptureMarkedPassThroughMessage(appLanguageTag)
                    }

                    ACTION_EXCLUDE -> {
                        resolveAutoCaptureDraftUseCase.markResolved(
                            draftId = draftId,
                            resolution = AutoCaptureResolution.EXCLUDED
                        )
                        localizedAutoCaptureExcludedMessage(appLanguageTag)
                    }

                    else -> null
                } ?: return@launch

                autoCaptureDraftNotifier.notifyActionFeedback(
                    draftId = draftId,
                    appLanguageTag = appLanguageTag,
                    message = feedback
                )
            } catch (e: ResolveAutoCaptureDraftException) {
                val prefs = preferencesDataStore.userPreferences.first()
                autoCaptureDraftNotifier.notifyActionFeedback(
                    draftId = draftId,
                    appLanguageTag = prefs.appLanguageTag,
                    message = e.toUiMessage(prefs.appLanguageTag)
                )
            } catch (_: Exception) {
                val prefs = preferencesDataStore.userPreferences.first()
                autoCaptureDraftNotifier.notifyActionFeedback(
                    draftId = draftId,
                    appLanguageTag = prefs.appLanguageTag,
                    message = localizedAutoCaptureSaveFailedMessage(prefs.appLanguageTag)
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ResolveAutoCaptureDraftException.toUiMessage(languageTag: String?): String = when (this) {
        ResolveAutoCaptureDraftException.DraftMissing -> localizedAutoCaptureDraftMissingMessage(languageTag)
        ResolveAutoCaptureDraftException.ConfirmAmountFirst -> localizedAutoCaptureConfirmAmountFirstMessage(languageTag)
        ResolveAutoCaptureDraftException.ChooseAccountFirst -> localizedAutoCaptureChooseAccountFirstMessage(languageTag)
        ResolveAutoCaptureDraftException.ChooseDestinationFirst -> localizedAutoCaptureChooseDestinationFirstMessage(languageTag)
    }

    companion object {
        const val EXTRA_DRAFT_ID = "extra_draft_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val ACTION_SAVE_EXPENSE = "com.chelumore.xpenze.action.SAVE_DRAFT_EXPENSE"
        const val ACTION_SAVE_INCOME = "com.chelumore.xpenze.action.SAVE_DRAFT_INCOME"
        const val ACTION_SAVE_TRANSFER = "com.chelumore.xpenze.action.SAVE_DRAFT_TRANSFER"
        const val ACTION_MARK_PASS_THROUGH = "com.chelumore.xpenze.action.MARK_DRAFT_PASS_THROUGH"
        const val ACTION_EXCLUDE = "com.chelumore.xpenze.action.EXCLUDE_DRAFT"
    }
}
