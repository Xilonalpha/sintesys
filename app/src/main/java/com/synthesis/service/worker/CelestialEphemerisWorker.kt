package com.synthesis.service.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synthesis.data.remote.api.JplHorizonsApi
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.GeoLocation
import com.synthesis.domain.model.Measurement
import com.synthesis.domain.model.Observation
import com.synthesis.domain.model.ObservationType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

@HiltWorker
class CelestialEphemerisWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val synthesisRepository: SynthesisRepository,
    private val jplHorizonsApi: JplHorizonsApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            Log.d(TAG, "Starting periodic celestial body and ephemeris sync...")
            val nowMs = System.currentTimeMillis()
            val gmstHours = calculateGreenwichMeanSiderealTime(nowMs)

            // 1. Fetch updated Solar System Ephemerides via JPL Horizons
            val targets = listOf(
                "301" to "Moon",
                "499" to "Mars",
                "599" to "Jupiter",
                "699" to "Saturn"
            )

            val freshObservations = mutableListOf<Observation>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val todayStr = dateFormat.format(Date(nowMs))
            val tomorrowStr = dateFormat.format(Date(nowMs + 86400000L))

            for ((bodyCode, bodyName) in targets) {
                try {
                    val jplResult = synthesisRepository.fetchJplEphemeris(
                        target = bodyCode,
                        center = "@399", // Earth geocenter
                        startTime = todayStr,
                        endTime = tomorrowStr
                    )
                    jplResult.getOrNull()?.let { observations ->
                        freshObservations.addAll(observations.take(2))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch ephemeris for $bodyName: ${e.message}")
                }
            }

            // 2. Fetch updated candidate telemetry from NASA Exoplanet Archive
            var exoplanetCount = 0
            try {
                val candidateResult = synthesisRepository.fetchAllExoplanets()
                exoplanetCount = candidateResult.getOrNull()?.size ?: 0
                Log.d(TAG, "Synchronized $exoplanetCount exoplanet candidates from NASA TAP.")
            } catch (e: Exception) {
                Log.w(TAG, "Exoplanet TAP sync transient error: ${e.message}")
            }

            // 3. Save fresh celestial observations in local Room database
            if (freshObservations.isNotEmpty()) {
                synthesisRepository.saveObservations(freshObservations)
                Log.d(TAG, "Saved ${freshObservations.size} fresh JPL celestial observations to Room DB.")
            }

            // 4. Update Astronomy Cache Preferences for Dynamic UI Display
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIMESTAMP, nowMs)
                .putString(KEY_LAST_SYNC_STATUS, "SUCCESS")
                .putInt(KEY_SYNCED_BODIES_COUNT, targets.size)
                .putInt(KEY_EXOPLANET_COUNT, exoplanetCount)
                .putFloat(KEY_CURRENT_GMST, gmstHours.toFloat())
                .apply()

            Log.d(TAG, "Celestial ephemeris sync completed successfully at GMST: ${String.format(Locale.US, "%.2fh", gmstHours)}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Celestial sync worker failed: ${e.message}", e)
            prefs.edit()
                .putString(KEY_LAST_SYNC_STATUS, "FAILED: ${e.localizedMessage}")
                .apply()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Calculates Greenwich Mean Sidereal Time (GMST) in hours (0.0 .. 24.0)
     * using the IAU convention from Julian Date.
     */
    private fun calculateGreenwichMeanSiderealTime(timestampMs: Long): Double {
        val jd = (timestampMs / 86400000.0) + 2440587.5
        val d = jd - 2451545.0
        var gmstDegrees = 280.46061837 + 360.98564736629 * d
        gmstDegrees = (gmstDegrees % 360.0 + 360.0) % 360.0
        return gmstDegrees / 15.0 // Convert degrees to hours
    }

    companion object {
        const val TAG = "CelestialEphemerisWorker"
        const val PREFS_NAME = "synthesis_astronomy_cache"
        const val KEY_LAST_SYNC_TIMESTAMP = "key_last_sync_timestamp"
        const val KEY_LAST_SYNC_STATUS = "key_last_sync_status"
        const val KEY_SYNCED_BODIES_COUNT = "key_synced_bodies_count"
        const val KEY_EXOPLANET_COUNT = "key_exoplanet_count"
        const val KEY_CURRENT_GMST = "key_current_gmst"
    }
}
