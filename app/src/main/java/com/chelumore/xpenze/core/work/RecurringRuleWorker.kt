package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chelumore.xpenze.domain.usecase.recurring.ProcessDueRecurringRulesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RecurringRuleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val processDueRecurringRulesUseCase: ProcessDueRecurringRulesUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            processDueRecurringRulesUseCase()
            Result.success()
        } catch (_: Exception) {
            // Avoid retry loops; the next periodic run will try again.
            Result.success()
        }
    }
}


