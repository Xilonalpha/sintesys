package com.synthesis.domain.model.exoplanet

/**
 * Filter and astrophysical screening criteria for periodic background TESS survey scanning.
 */
data class TessSurveyCriteria(
    val maxTmag: Double = 12.0,           // Host star T-magnitude brightness cutoff (fainter = noisier)
    val minSnr: Double = 7.1,             // Signal-to-noise ratio threshold (NASA 7.1 sigma benchmark)
    val minDepthPpm: Double = 400.0,      // Minimum transit depth in parts per million
    val dwarfStarsOnly: Boolean = true,   // Prioritize dwarf stars (Rs <= 1.3 R_sun) where transits are pronounced
    val unvettedNonToiPreferred: Boolean = true, // Focus on unvetted / uncatalogued targets
    val scanBatchSize: Int = 6            // Number of star targets evaluated per WorkManager execution cycle
)

/**
 * Potential transit candidate discovered by the autonomous background survey.
 */
data class TessSurveyDiscovery(
    val ticId: String,
    val starName: String,
    val depthPpm: Double,
    val depthMagnitudeDelta: Double,
    val orbitalPeriodDays: Double,
    val derivedRadiusEarth: Double,
    val snr: Double,
    val hostStarTmag: Double,
    val hostStarRadiusSun: Double,
    val transitDurationHours: Double,
    val verdictLabel: String,
    val scientificRationale: String,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Telemetry and status of the background WorkManager survey worker.
 */
data class TessSurveyScoutStatus(
    val isPeriodicEnabled: Boolean,
    val lastRunTimestampMs: Long,
    val lastRunStatus: String,
    val totalStarsEvaluated: Int,
    val discoveriesCount: Int,
    val lastDiscoveredTic: String?
) {
    val formattedLastRun: String
        get() {
            if (lastRunTimestampMs <= 0L) return "Programat (Așteptare ciclu)"
            val diffSec = (System.currentTimeMillis() - lastRunTimestampMs) / 1000
            return when {
                diffSec < 60 -> "Chiar acum"
                diffSec < 3600 -> "${diffSec / 60}m în urmă"
                diffSec < 86400 -> "${diffSec / 3600}h în urmă"
                else -> java.text.SimpleDateFormat("dd MMM, HH:mm 'UTC'", java.util.Locale.US).format(java.util.Date(lastRunTimestampMs))
            }
        }
}
