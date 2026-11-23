package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class EmmaProactiveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val outcome = ProactiveEmmaCoordinator.createDefault(applicationContext)
                .evaluateAndNotifyIfDue(forceTest = false)
            when (outcome) {
                is ProactiveEvaluationOutcome.Notified,
                ProactiveEvaluationOutcome.SkippedDisabled,
                ProactiveEvaluationOutcome.SkippedCooldown,
                ProactiveEvaluationOutcome.SkippedNoHabitSignal,
                ProactiveEvaluationOutcome.SkippedNoPermission -> Result.success()
                ProactiveEvaluationOutcome.SkippedOffline,
                ProactiveEvaluationOutcome.SkippedAiUnavailable -> Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object EmmaProactiveScheduler {

    private const val UNIQUE_WORK_NAME = "emma_proactive_habit_check"

    fun ensureScheduled(context: Context, enabled: Boolean) {
        runCatching {
            val workManager = WorkManager.getInstance(context.applicationContext)
            if (!enabled) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<EmmaProactiveWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
