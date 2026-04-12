package com.chelumore.xpenze.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chelumore.xpenze.domain.usecase.budget.ProcessRecurringBudgetsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BudgetLifecycleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val processRecurringBudgetsUseCase: ProcessRecurringBudgetsUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            processRecurringBudgetsUseCase()
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
