package com.synthesis.domain.model.validation

import com.synthesis.engine.astrometry.AstrometryEngine

/**
 * Verification status for candidate transit cross-referencing.
 */
enum class ValidationStatus(val label: String, val isApproved: Boolean) {
    VERIFIED_FOV_ALIGNED("VERIFICAT • ALINIAT CÂMP TESS / HEASARC", true),
    SECTOR_CORRECTED("CORECȚIE SECTOR APLICATĂ • RE-ALINIAT", true),
    FOV_MISMATCH_REJECTED("RESPINS • ÎN AFARA CÂMPULUI DE VEDERE (FOV)", false),
    SUSPECTED_ECLIPSING_BINARY("RISC FALS-POZITIV • BINARĂ CU ECLIPSĂ (TEB)", false),
    MARGINAL_SNR_AMBIGUOUS("NECLASIFICAT • SEMNAL SUB PRAGUL $7.1\\sigma$", false)
}

/**
 * Detailed astronomical pointing audit comparing target coordinates against TESS field-of-view.
 */
data class TessPointingAuditResult(
    val ticId: String,
    val raDeg: Double,
    val decDeg: Double,
    val eclipticCoords: AstrometryEngine.EclipticCoordinates,
    val claimedSector: String,
    val claimedSectorNumber: Int?,
    val isClaimedSectorObserved: Boolean,
    val actualObservedSectors: List<Int>,
    val suggestedCorrectSector: String,
    val pointingMismatchFlag: Boolean,
    val pointingExplanation: String
)

/**
 * Astrophysical sanity metrics cross-checked against NASA Exoplanet Archive and HEASARC catalogs.
 */
data class HeasarcCrossCheckResult(
    val isKnownEclipsingBinary: Boolean,
    val isNearbyBlendContaminated: Boolean,
    val centroidOffsetArcsec: Double,
    val stellarDensityCrowdingMetric: Double,
    val falseAlarmProbability: Double,
    val rocheLobeOverflowRisk: Boolean,
    val isPhysicallyPlausibleTransit: Boolean,
    val notes: String
)

/**
 * Complete Pre-Report Validation Dossier generated before finalizing ExoFOP/CTOI reports.
 */
data class PreReportValidationDossier(
    val targetTic: String,
    val canonicalName: String,
    val pointingAudit: TessPointingAuditResult,
    val heasarcAudit: HeasarcCrossCheckResult,
    val overallStatus: ValidationStatus,
    val validationScore: Double, // 0.0 to 1.0 (1.0 = pristine certified transit)
    val sanitizedSectorLabel: String,
    val warnings: List<String>,
    val recommendations: List<String>,
    val verifiedAtUtc: String
)
