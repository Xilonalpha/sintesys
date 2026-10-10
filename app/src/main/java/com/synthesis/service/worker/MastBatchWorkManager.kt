package com.synthesis.service.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for enqueueing background WorkManager jobs for MAST batch dataset processing.
 */
@Singleton
class MastBatchWorkManager @Inject constructor() {

    fun triggerOneTimeBatchJob(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MastBatchWorker>()
            .setConstraints(constraints)
            .addTag(WORK_TAG_ONE_TIME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_TAG_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedulePeriodicBatchScout(context: Context, intervalHours: Long = 12) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<MastBatchWorker>(
            intervalHours, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG_PERIODIC)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelAllBatchWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_ONE_TIME)
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_PERIODIC)
    }

    companion object {
        const val WORK_TAG_ONE_TIME = "mast_batch_worker_one_time"
        const val WORK_TAG_PERIODIC = "mast_batch_worker_periodic"
    }
}
