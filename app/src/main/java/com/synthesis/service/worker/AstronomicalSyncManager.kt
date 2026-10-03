package com.synthesis.service.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CelestialSyncInfo(
    val lastSyncMs: Long,
    val status: String,
    val syncedBodies: Int,
    val exoplanetCount: Int,
    val gmstHours: Float
) {
    val formattedLastSync: String
        get() {
            if (lastSyncMs <= 0L) return "Never (Scheduled)"
            val diffSec = (System.currentTimeMillis() - lastSyncMs) / 1000
            return when {
                diffSec < 60 -> "Just now"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                else -> SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).format(Date(lastSyncMs))
            }
        }
}

@Singleton
class AstronomicalSyncManager @Inject constructor() {

    fun schedulePeriodicCelestialSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // WorkManager minimum periodic interval is 15 minutes
        val periodicRequest = PeriodicWorkRequestBuilder<CelestialEphemerisWorker>(
            repeatInterval = 1,
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
            .addTag(WORK_TAG_CELESTIAL)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        Log.i(TAG, "Scheduled periodic 1-hour celestial ephemeris and astronomical sync task.")
    }

    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<CelestialEphemerisWorker>()
            .setConstraints(constraints)
            .addTag(WORK_TAG_CELESTIAL)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
        Log.i(TAG, "Enqueued immediate celestial ephemeris update task.")
    }

    fun getSyncInfo(context: Context): CelestialSyncInfo {
        val prefs = context.getSharedPreferences(
            CelestialEphemerisWorker.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        return CelestialSyncInfo(
            lastSyncMs = prefs.getLong(CelestialEphemerisWorker.KEY_LAST_SYNC_TIMESTAMP, 0L),
            status = prefs.getString(CelestialEphemerisWorker.KEY_LAST_SYNC_STATUS, "STANDBY") ?: "STANDBY",
            syncedBodies = prefs.getInt(CelestialEphemerisWorker.KEY_SYNCED_BODIES_COUNT, 0),
            exoplanetCount = prefs.getInt(CelestialEphemerisWorker.KEY_EXOPLANET_COUNT, 0),
            gmstHours = prefs.getFloat(CelestialEphemerisWorker.KEY_CURRENT_GMST, 0.0f)
        )
    }

    companion object {
        const val TAG = "AstronomicalSyncManager"
        const val UNIQUE_PERIODIC_WORK_NAME = "synthesis_celestial_periodic_sync"
        const val UNIQUE_IMMEDIATE_WORK_NAME = "synthesis_celestial_immediate_sync"
        const val WORK_TAG_CELESTIAL = "celestial_ephemeris_sync"
    }
}
