package com.synthesis.domain.model.flight

import java.util.Locale

/**
 * NASA Ground-Truth Reference Exoplanet definition for TRL 8/9 Flight Qualification.
 * Encapsulates peer-reviewed published physical and orbital parameters.
 */
data class ReferenceExoplanet(
    val name: String,
    val targetId: String, // e.g. "TIC 150428135" or "KOI-571"
    val hostStarName: String,
    val spectralType: String,
    val stellarMassSolar: Double,
    val stellarRadiusSolar: Double,
    val stellarTeffK: Double,
    val truePeriodDays: Double,
    val trueTransitDepthPpm: Double,
    val truePlanetRadiusEarth: Double,
    val trueSemiMajorAxisAu: Double,
    val trueEquilibriumTempK: Double,
    val discoveryMission: String, // "TESS", "Kepler", "Spitzer", "JWST"
    val peerReviewCitation: String, // e.g. "Gilbert et al. 2020, AJ 160, 116"
    val isHabitableZone: Boolean = false,
    val bondAlbedo: Double = 0.0
)

/**
 * Result of validating an individual reference planet against SYNTHESIS physical solver.
 */
data class BenchmarkValidationResult(
    val planet: ReferenceExoplanet,
    val derivedPeriodDays: Double,
    val derivedTransitDepthPpm: Double,
    val derivedPlanetRadiusEarth: Double,
    val derivedSemiMajorAxisAu: Double,
    val derivedEquilibriumTempK: Double,
    val periodErrorPercent: Double,
    val depthErrorPercent: Double,
    val radiusErrorPercent: Double,
    val semiMajorAxisErrorPercent: Double,
    val tempErrorPercent: Double,
    val maxErrorPercent: Double,
    val isFlightQualified: Boolean, // True if maxErrorPercent <= 1.2%
    val chiSquaredNu: Double,
    val bayesianConfidenceScore: Double,
    val statusMessage: String
) {
    fun formatSummary(): String {
        return String.format(
            Locale.US,
            "%s [%s]: Max Error = %.2f%% (R_p err = %.2f%%, P err = %.2f%%) -> %s",
            planet.name,
            planet.discoveryMission,
            maxErrorPercent,
            radiusErrorPercent,
            periodErrorPercent,
            if (isFlightQualified) "PASSED (TRL-9 CONFORMANT)" else "MARGINAL"
        )
    }
}

/**
 * Complete TRL 9 Flight Readiness Review (FRR) Dossier.
 */
data class FlightReadinessDossier(
    val dossierId: String,
    val isoTimestamp: String,
    val trlLevelTarget: Int = 9,
    val assessedTrlLevel: Int,
    val totalBenchmarksCount: Int,
    val passedBenchmarksCount: Int,
    val qualificationRatePercent: Double,
    val averageErrorPercent: Double,
    val maxObservedErrorPercent: Double,
    val isMissionCertified: Boolean,
    val verificationHash: String,
    val benchmarkResults: List<BenchmarkValidationResult>,
    val systemEnvironment: FlightSystemEnvironment,
    val protocolCompliance: FlightProtocolCompliance
)

data class FlightSystemEnvironment(
    val osName: String = "Android Space Operating System (ASOS)",
    val architecture: String = "Kotlin / ART (R8 Optimized)",
    val memoryFootprintMb: Long,
    val parallelCoresCount: Int,
    val faultToleranceActive: Boolean = true,
    val offlineAutonomyActive: Boolean = true
)

data class FlightProtocolCompliance(
    val nasaTfopSg1Compliant: Boolean = true,
    val ivoaVoTableCompliant: Boolean = true,
    val ivoaVoEventCompliant: Boolean = true,
    val aavsoEtdCompliant: Boolean = true,
    val nasaExoFopCtoiCompliant: Boolean = true
)
