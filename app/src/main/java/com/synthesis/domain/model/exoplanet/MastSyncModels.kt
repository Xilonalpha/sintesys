package com.synthesis.domain.model.exoplanet

import com.synthesis.service.FitsHeader

/**
 * Metadata representing an astronomical space telescope observation target
 * retrieved from MAST (Mikulski Archive for Space Telescopes) or NASA Exoplanet Archive.
 */
data class MastObservationTarget(
    val id: String, // Unique preset or catalog ID
    val targetName: String, // e.g. "TIC 384159646 (TOI-1895.01)"
    val ticId: String,
    val mission: String = "TESS", // "TESS", "Kepler", "K2", "JWST"
    val sectorOrQuarter: String, // e.g. "s0039-s0039"
    val tceNumber: String = "1",
    val estimatedMag: Double = 10.0,
    val description: String,
    val scientificPriority: String = "PRIORITATE MAXIMĂ",
    val spectralType: String = "Solar-type G/K Dwarf",
    val raDeg: Double? = null,
    val decDeg: Double? = null,
    val isLiveSyncAvailable: Boolean = true
)

/**
 * Real-time reactive state of the MAST data synchronization module.
 */
data class MastSyncState(
    val isSyncing: Boolean = false,
    val progressFraction: Float = 0.0f,
    val statusMessage: String = "Gata pentru sincronizare live cu arhivele MAST / NASA.",
    val lastSyncTimestampMs: Long = 0L,
    val activeTarget: MastObservationTarget? = null,
    val syncedPointsCount: Int = 0,
    val syncedMission: String? = null,
    val syncedSector: String? = null,
    val activeError: String? = null
)

/**
 * Complete synchronized dataset ready for immediate multi-engine astronomical analysis.
 */
data class MastSyncResult(
    val target: MastObservationTarget,
    val dataPoints: List<PhotometricDataPoint>,
    val periodicDetection: PeriodicTransitDetectionResult,
    val templateScores: List<TemplateMatchScore>,
    val bayesianMetrics: BayesianUncertaintyMetrics?,
    val executionTimeMs: Long,
    val logSummary: String
)
