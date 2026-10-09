package com.synthesis.service.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.synthesis.domain.model.exoplanet.TessSurveyCriteria
import com.synthesis.domain.model.exoplanet.TessSurveyScoutStatus
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TessSurveyWorkManager @Inject constructor() {

    fun schedulePeriodicSurvey(
        context: Context,
        repeatHours: Long = 6,
        criteria: TessSurveyCriteria = TessSurveyCriteria()
    ) {
        saveCriteria(context, criteria)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<TessSurveyScannerWorker>(
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
            .addTag(TAG_TESS_SCOUT)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        Log.i(TAG, "Scheduled periodic $repeatHours-hour TESS survey scout task.")
    }

    fun triggerImmediateSurvey(
        context: Context,
        criteria: TessSurveyCriteria = TessSurveyCriteria()
    ) {
        saveCriteria(context, criteria)
        val prefs = context.getSharedPreferences(TessSurveyScannerWorker.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(TessSurveyScannerWorker.KEY_LAST_RUN_STATUS, "SCANARE ÎN CURS (Activ)...")
            .apply()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<TessSurveyScannerWorker>()
            .setConstraints(constraints)
            .addTag(TAG_TESS_SCOUT)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
        Log.i(TAG, "Enqueued immediate TESS survey scan task.")
    }

    fun cancelPeriodicSurvey(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        val prefs = context.getSharedPreferences(TessSurveyScannerWorker.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(TessSurveyScannerWorker.KEY_LAST_RUN_STATUS, "STANDBY (Dezactivat)")
            .apply()
        Log.i(TAG, "Cancelled periodic TESS survey scout task.")
    }

    fun getScoutStatus(context: Context): TessSurveyScoutStatus {
        val prefs = context.getSharedPreferences(TessSurveyScannerWorker.PREFS_NAME, Context.MODE_PRIVATE)
        return TessSurveyScoutStatus(
            isPeriodicEnabled = true,
            lastRunTimestampMs = prefs.getLong(TessSurveyScannerWorker.KEY_LAST_RUN_TIMESTAMP, 0L),
            lastRunStatus = prefs.getString(TessSurveyScannerWorker.KEY_LAST_RUN_STATUS, "STANDBY (Programat)") ?: "STANDBY",
            totalStarsEvaluated = prefs.getInt(TessSurveyScannerWorker.KEY_TOTAL_STARS_EVALUATED, 0),
            discoveriesCount = prefs.getInt(TessSurveyScannerWorker.KEY_DISCOVERIES_COUNT, 0),
            lastDiscoveredTic = prefs.getString(TessSurveyScannerWorker.KEY_LAST_DISCOVERED_TIC, null)
        )
    }

    fun saveCriteria(context: Context, criteria: TessSurveyCriteria) {
        val prefs = context.getSharedPreferences(TessSurveyScannerWorker.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(TessSurveyScannerWorker.KEY_CRITERIA_MAX_TMAG, criteria.maxTmag.toFloat())
            .putFloat(TessSurveyScannerWorker.KEY_CRITERIA_MIN_SNR, criteria.minSnr.toFloat())
            .putFloat(TessSurveyScannerWorker.KEY_CRITERIA_MIN_DEPTH_PPM, criteria.minDepthPpm.toFloat())
            .putBoolean(TessSurveyScannerWorker.KEY_CRITERIA_DWARF_ONLY, criteria.dwarfStarsOnly)
            .putBoolean(TessSurveyScannerWorker.KEY_CRITERIA_UNVETTED_ONLY, criteria.unvettedNonToiPreferred)
            .putInt(TessSurveyScannerWorker.KEY_CRITERIA_BATCH_SIZE, criteria.scanBatchSize)
            .apply()
    }

    fun loadCriteria(context: Context): TessSurveyCriteria {
        val prefs = context.getSharedPreferences(TessSurveyScannerWorker.PREFS_NAME, Context.MODE_PRIVATE)
        return TessSurveyCriteria(
            maxTmag = prefs.getFloat(TessSurveyScannerWorker.KEY_CRITERIA_MAX_TMAG, 12.0f).toDouble(),
            minSnr = prefs.getFloat(TessSurveyScannerWorker.KEY_CRITERIA_MIN_SNR, 7.1f).toDouble(),
            minDepthPpm = prefs.getFloat(TessSurveyScannerWorker.KEY_CRITERIA_MIN_DEPTH_PPM, 400.0f).toDouble(),
            dwarfStarsOnly = prefs.getBoolean(TessSurveyScannerWorker.KEY_CRITERIA_DWARF_ONLY, true),
            unvettedNonToiPreferred = prefs.getBoolean(TessSurveyScannerWorker.KEY_CRITERIA_UNVETTED_ONLY, true),
            scanBatchSize = prefs.getInt(TessSurveyScannerWorker.KEY_CRITERIA_BATCH_SIZE, 6)
        )
    }

    companion object {
        const val TAG = "TessSurveyWorkManager"
        const val UNIQUE_PERIODIC_WORK_NAME = "synthesis_tess_periodic_survey"
        const val UNIQUE_IMMEDIATE_WORK_NAME = "synthesis_tess_immediate_survey"
        const val TAG_TESS_SCOUT = "tess_survey_scout"
    }
}
