package com.synthesis.domain.model.exoplanet

/**
 * Shape classification of a photometric dip in light curve magnitude/flux.
 */
enum class DipShapeClassification(val label: String, val description: String, val colorHex: Long) {
    U_SHAPED_TRANSIT(
        "U-Shape (Planetar)",
        "Fund aplatizat cu margini abrupte (ingress/egress), caracteristic trecerii unui corp opac peste discul stelar",
        0xFF00E676
    ),
    V_SHAPED_BINARY(
        "V-Shape (Binară Rasă)",
        "Scădere ascuțită în V, caracteristică eclipselor stelare parțiale sau binarelor de contact",
        0xFFFF5252
    ),
    SHALLOW_SUB_THRESHOLD(
        "Scădere Sub-Prag",
        "Semnal superficial apropiat de zgomotul fotometric instrumental",
        0xFFFFB74D
    ),
    FLARE_RECOVERY(
        "Anomalie / Re-emisie",
        "Formă asimetrică cu creștere rapidă și descreștere lentă",
        0xFFBA68C8
    )
}

/**
 * Recurrence evaluation across multiple detected dips in light curve time-series.
 */
enum class DipRecurrenceVerdict(val title: String, val badgeColorHex: Long) {
    PERIODIC_TRANSIT_SERIES(
        "Recurență Periodică Confirmată (Semnal Exoplanetar)",
        0xFF00E676
    ),
    PRIMARY_AND_SECONDARY_OCCULTATION(
        "Tranzit Principal + Ocultație Secundară Detectată",
        0xFF00E5FF
    ),
    MONO_TRANSIT_ISOLATED(
        "Mono-Tranzit Solitar Detectat (Candidat Perioadă Lungă)",
        0xFFFFD600
    ),
    MULTI_CANDIDATE_SUPERPOSITION(
        "Suprapunere Tranzite Multi-Planetare",
        0xFF7C4DFF
    ),
    IRREGULAR_NOISE_OR_STELLAR_VARIATION(
        "Variație Stelară sau Zgomot Neregulat",
        0xFF9E9E9E
    )
}

/**
 * An individual dip detected in the light curve.
 */
data class DetectedDipSignal(
    val dipIndex: Int,
    val label: String,
    val centerPhase: Double,
    val centerTimeHours: Double,
    val ingressPhase: Double,
    val egressPhase: Double,
    val durationHours: Double,
    val depthPpm: Double,
    val depthMagnitudeDelta: Double, // Magnitude drop Δm ≈ -2.5 * log10(1 - depthFraction)
    val snrSigma: Double,
    val shapeScore: Double, // 0.0 (V-shape) to 1.0 (ideal flat-bottom U-shape)
    val shapeClassification: DipShapeClassification,
    val inTransitPointsCount: Int,
    val isPeriodicMatch: Boolean,
    val isPrimaryTransit: Boolean,
    val phaseRangeStart: Double,
    val phaseRangeEnd: Double,
    val confidenceScore: Double = 0.0, // 0.0 - 100.0% confidence based on SNR and depth consistency
    val bayesianUncertainty: BayesianUncertaintyMetrics? = null
)

/**
 * Comprehensive analysis report from scanning light curve points for recurring dips.
 */
data class TransitDipScanReport(
    val ticId: String,
    val totalDipsFound: Int,
    val primaryDip: DetectedDipSignal?,
    val secondaryDip: DetectedDipSignal?,
    val allDips: List<DetectedDipSignal>,
    val recurrenceVerdict: DipRecurrenceVerdict,
    val recurrencePeriodCandidateDays: Double?,
    val oddEvenDepthRatio: Double?,
    val oddEvenConsistencySigma: Double?,
    val noiseFloorPpm: Double,
    val detectionThresholdSigma: Double,
    val scientificDiagnosis: String,
    val overallConfidenceScore: Double = 0.0, // 0.0 - 100.0% signal confidence based on SNR & depth consistency
    val bayesianUncertainty: BayesianUncertaintyMetrics? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
