package com.muslimtime.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class PrayerTimesRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!PrayerPreferences.hasCompletedInitialSetup(applicationContext)) {
            return Result.success()
        }
        return PrayerDataSyncManager.syncToday(applicationContext).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "prayer_times_refresh"
        private const val IMMEDIATE_WORK_NAME = "prayer_times_refresh_now"

        fun schedule(context: Context) {
            if (!PrayerDataSyncManager.shouldUsePeriodicWorker(context)) {
                cancel(context)
                return
            }

            val request = PeriodicWorkRequestBuilder<PrayerTimesRefreshWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<PrayerTimesRefreshWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
