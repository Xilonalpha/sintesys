package com.synthesis.domain.model.xilon

/**
 * Model for stars observed in the TESS Input Catalog (TIC) that have verified candidate
 * transit signals but have NOT been cataloged as a TESS Object of Interest (TOI) by NASA.
 *
 * These represent pure uncatalogued signals (Planet Hunters citizen science discoveries,
 * mono-transit blindspots, community TLS detections, and sub-threshold TCEs).
 */
data class PureTicCandidate(
    val ticId: String,
    val name: String,
    val sector: String,
    val apparentMag: Double,
    val estimatedPeriodDays: Double?,
    val transitDurationHours: Double,
    val transitDepthPpm: Double,
    val stellarTeffK: Double,
    val stellarRadiusSun: Double,
    val stellarMassSun: Double,
    val distancePc: Double,
    val discoveryOrigin: String,
    val whyNoToi: String,
    val potentialPlanetaryType: String,
    val isInHabitableZone: Boolean = false,
    val scientificPriority: String = "PRIORITATE MAXIMĂ • TEREN VIRGIN",
    val scientificNotes: String,
    val isLiveIngested: Boolean = false,
    val ingestionTimestamp: Long = System.currentTimeMillis(),
    val snrSigma: Double = 9.8,
    val profileSymmetryDeltaSigma: Double = 1.2
)

enum class TicSortOption(val label: String) {
    LIVE_NEWEST("Recente / Live"),
    SNR_DESC("SNR Descrescător"),
    DEPTH_DESC("Adâncime (ppm)"),
    PERIOD_ASC("Perioadă Orbitală"),
    DISTANCE_ASC("Distanță Minimă")
}
