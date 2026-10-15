package com.synthesis.service.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TessMastPollingStatus(
    val isPeriodicEnabled: Boolean = true,
    val lastRunTimestampMs: Long = 0L,
    val lastRunStatus: String = "STANDBY (Programat)",
    val totalCandidatesPolled: Int = 0,
    val lastScoutedSector: Int = 40,
    val latestPolledTic: String = ""
)

@Singleton
class TessMastPollingWorkManager @Inject constructor() {

    fun schedulePeriodicPolling(
        context: Context,
        repeatHours: Long = 4
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<TessMastPollingWorker>(
            repeatInterval = repeatHours.coerceAtLeast(1),
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG_MAST_POLL)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        Log.i(TAG, "Scheduled periodic $repeatHours-hour MAST polling WorkManager task.")
    }

    fun triggerImmediatePoll(context: Context) {
        val prefs = context.getSharedPreferences(TessMastPollingWorker.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(TessMastPollingWorker.KEY_LAST_MAST_POLL_STATUS, "POLLING MAST ÎN CURS (WorkManager)...")
            .apply()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<TessMastPollingWorker>()
            .setConstraints(constraints)
            .addTag(TAG_MAST_POLL)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
        Log.i(TAG, "Enqueued immediate MAST polling WorkManager task.")
    }

    fun cancelPeriodicPolling(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        val prefs = context.getSharedPreferences(TessMastPollingWorker.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(TessMastPollingWorker.KEY_LAST_MAST_POLL_STATUS, "STANDBY (Oprit)")
            .apply()
        Log.i(TAG, "Cancelled periodic MAST polling task.")
    }

    fun getPollingStatus(context: Context): TessMastPollingStatus {
        val prefs = context.getSharedPreferences(TessMastPollingWorker.PREFS_NAME, Context.MODE_PRIVATE)
        return TessMastPollingStatus(
            isPeriodicEnabled = true,
            lastRunTimestampMs = prefs.getLong(TessMastPollingWorker.KEY_LAST_MAST_POLL_TIMESTAMP, 0L),
            lastRunStatus = prefs.getString(TessMastPollingWorker.KEY_LAST_MAST_POLL_STATUS, "STANDBY (Programat)") ?: "STANDBY",
            totalCandidatesPolled = prefs.getInt(TessMastPollingWorker.KEY_TOTAL_MAST_CANDIDATES, 0),
            lastScoutedSector = prefs.getInt(TessMastPollingWorker.KEY_LAST_SCOUTED_SECTOR, 40),
            latestPolledTic = prefs.getString(TessMastPollingWorker.KEY_LATEST_POLLED_TIC, "") ?: ""
        )
    }

    companion object {
        const val TAG = "TessMastPollingWorkMgr"
        const val UNIQUE_PERIODIC_WORK_NAME = "periodic_tess_mast_polling_work"
        const val UNIQUE_IMMEDIATE_WORK_NAME = "immediate_tess_mast_polling_work"
        const val TAG_MAST_POLL = "tess_mast_polling_tag"
    }
}
