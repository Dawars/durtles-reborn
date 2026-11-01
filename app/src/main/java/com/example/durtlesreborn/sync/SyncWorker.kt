package com.example.durtlesreborn.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.durtlesreborn.DurtlesApp
import com.example.durtlesreborn.api.ApiResult
import com.example.durtlesreborn.data.SyncStep
import java.util.concurrent.TimeUnit

/** Sends finished reviews and pulls changes in the background. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as DurtlesApp).container
        val token = container.tokenStore.current()
        if (token.isNullOrBlank()) return Result.success()
        container.token = token

        val report = container.syncEngine.sync()
        val failure = report.steps.values.filterIsInstance<SyncStep.Failed>().firstOrNull()?.result
        return when (failure) {
            null -> Result.success()
            // Retrying with a rejected token only repeats the failure; the user must fix it in the app.
            is ApiResult.Unauthorized -> Result.success()
            else -> Result.retry()
        }
    }
}

object SyncScheduler {
    private const val NAME = "background_sync"

    /** Turns the periodic background sync on or off. WorkManager's minimum period is 15 minutes. */
    fun setEnabled(context: Context, enabled: Boolean, everyMinutes: Long = 60) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncWorker>(everyMinutes.coerceAtLeast(15), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
