package com.synthesis.domain.model.exoplanet

/**
 * Individual point in the Box-fitting Least Squares (BLS) power spectrum.
 */
data class BlsPeriodogramPoint(
    val periodDays: Double,
    val periodHours: Double,
    val power: Double,
    val depthPpm: Double,
    val durationHours: Double,
    val snr: Double
)

/**
 * Photometric measurement folded in orbital phase [-0.5, +0.5).
 */
data class PhaseFoldedPoint(
    val phase: Double, // Normalized phase from mid-transit (-0.5 to +0.5)
    val relativeFlux: Double,
    val fluxError: Double,
    val timeMinutes: Double,
    val bjd: Double,
    val epochIndex: Long, // Transit cycle count
    val isInTransit: Boolean
)

/**
 * Binned average in phase space for visualization and SNR improvement.
 */
data class PhaseBinPoint(
    val phaseCenter: Double,
    val meanFlux: Double,
    val fluxError: Double,
    val pointCount: Int
)

/**
 * Astrophysical vetting metrics to distinguish planetary transits from
 * eclipsing binaries (EB) or instrumental systematic anomalies.
 */
data class TransitVettingMetrics(
    val oddTransitDepthPpm: Double,
    val evenTransitDepthPpm: Double,
    val oddEvenSigmaDiff: Double,
    val isOddEvenConsistent: Boolean, // True if Odd/Even depths are identical within errors (Planet)
    val secondaryEclipseDepthPpm: Double,
    val secondaryEclipseSigma: Double,
    val isSecondaryEclipseDetected: Boolean, // True if significant dip at phase 0.5 (indicates Binary Star)
    val durationKeplerianRatio: Double, // Ratio of measured duration vs maximum theoretical duration
    val isDurationPhysical: Boolean,
    val chiSquaredFlat: Double,
    val chiSquaredTransit: Double,
    val deltaChiSquared: Double,
    val confidenceScore: Double = 0.0, // 0.0 - 100.0% confidence based on SNR and depth consistency
    val bayesianUncertainty: BayesianUncertaintyMetrics? = null // Rigorous Bayesian credible intervals & error bars
)

/**
 * Physical exoplanetary properties derived via Kepler's Third Law and photometric transit inversion.
 */
data class DerivedExoplanetParameters(
    val planetRadiusEarth: Double,
    val planetRadiusJupiter: Double,
    val radiusRatioRpRs: Double,
    val semiMajorAxisAu: Double,
    val orbitalVelocityKmS: Double,
    val orbitalInclinationDeg: Double,
    val impactParameterB: Double,
    val equilibriumTempK: Double,
    val insolationSolar: Double,
    val isHabitableZone: Boolean,
    val planetClassification: String,
    val geometricTransitProbability: Double
)

/**
 * Complete result of the periodic light curve transit analysis.
 */
data class PeriodicTransitDetectionResult(
    val isDetected: Boolean,
    val bestPeriodDays: Double,
    val bestPeriodHours: Double,
    val epochBjd: Double,
    val transitDepthPpm: Double,
    val transitDepthFraction: Double,
    val transitDurationHours: Double,
    val dutyCycleQ: Double, // Transit duration fraction of orbital period (tau / P)
    val signalDetectionEfficiencySde: Double, // Standard BLS SDE metric (SDE >= 6.0 is gold standard)
    val transitSnr: Double,
    val vetting: TransitVettingMetrics,
    val physicalParameters: DerivedExoplanetParameters,
    val periodogram: List<BlsPeriodogramPoint>,
    val phaseFoldedPoints: List<PhaseFoldedPoint>,
    val binnedPhaseCurve: List<PhaseBinPoint>,
    val transitModelCurve: List<Pair<Double, Double>>, // List of (phase, modelFlux)
    val statusSummary: String,
    val timestampMs: Long = System.currentTimeMillis()
)
