package com.synthesis.domain.model.exoplanet

/**
 * Domain models for hunting exoplanets that fall through NASA automated pipeline blindspots:
 * 1. Mono-transits (long-period / habitable zone planets that transit only once per 27.4-day sector)
 * 2. Transit Timing Variations (TTVs) caused by unseen, non-transiting companion planets
 * 3. Atmospheric characterization metrics (TSM/ESM, scale heights) for JWST priority targets
 * 4. Neglected high-SNR candidates languishing in the multi-year TFOP backlog
 */

enum class PipelineBlindspotCategory(val label: String, val technicalReason: String) {
    MONO_TRANSIT_LONG_PERIOD(
        "Single Transit (Period > 27.4d)",
        "NASA SPOC automated BLS pipeline mandates >= 2 or 3 transits per 27.4-day sector; long-period temperate planets only transit once and are discarded as noise."
    ),
    TTV_SMEARING(
        "Transit Timing Variation Smearing",
        "Gravitational pull of non-transiting companions causes transits to arrive early or late, blurring the periodic phase-folded stack and triggering pipeline rejection."
    ),
    TFOP_BACKLOG_PURGATORY(
        "TFOP Resource Prioritization Backlog",
        "High-SNR candidate languishing in unconfirmed candidate status for >3 years due to limited ground-based RV telescope allocation."
    ),
    FLUX_DILUTION_NEB_BIAS(
        "Background Flux Dilution Bias",
        "Unresolved background stars dilute transit depth, causing automated classifiers to mislabel genuine Super-Earths as grazing eclipsing binaries."
    )
}

data class MonoTransitCandidate(
    val ticId: String,
    val candidateName: String,
    val sector: String,
    val epochBtjd: Double,
    val transitDurationHours: Double,
    val transitDepthPpm: Double,
    val stellarTeffK: Double,
    val stellarRadiusSun: Double,
    val stellarMassSun: Double,
    val stellarDistancePc: Double?,
    val apparentMagV: Double,
    // Derived astrophysics from single transit inversion (Seager & Mallen-Ornelas 2003)
    val planetRadiusEarth: Double,
    val minimumPeriodDays: Double,
    val circularEstimatedPeriodDays: Double,
    val semiMajorAxisAu: Double,
    val stellarInsolationEarth: Double,
    val equilibriumTempK: Double,
    val isHabitableZone: Boolean,
    val earthSimilarityIndex: Float?,
    val blindspotCategory: PipelineBlindspotCategory,
    val detectionNotes: String
)

data class TtvObservedTransit(
    val transitIndex: Int,
    val epochBtjd: Double,
    val observedMinusCalculatedMinutes: Double,
    val uncertaintyMinutes: Double
)

data class TtvCompanionAnalysis(
    val primaryCandidateName: String,
    val primaryPeriodDays: Double,
    val primaryRadiusEarth: Double,
    val ttvAmplitudeMinutes: Double,
    val superPeriodDays: Double,
    val estimatedCompanionMassEarth: Double,
    val estimatedCompanionPeriodDays: Double,
    val resonanceRatio: String, // e.g. "2:1 MMR", "3:2 MMR"
    val companionSemiMajorAxisAu: Double,
    val companionEquilibriumTempK: Double,
    val companionHabitableZone: Boolean,
    val confidenceSigma: Double,
    val observedTransits: List<TtvObservedTransit>,
    val theoreticalExplanation: String
)

data class AtmosphericCharacterizationScore(
    val candidateName: String,
    val ticId: String,
    val planetRadiusEarth: Double,
    val planetMassEarth: Double,
    val equilibriumTempK: Double,
    val stellarRadiusSun: Double,
    val jMagnitude: Double,
    val kMagnitude: Double,
    // Kempton et al. (2018) metrics
    val tsmScore: Double,
    val esmScore: Double,
    val atmosphericScaleHeightKm: Double,
    val transitFeatureAmplitudePpm: Double,
    val jwstRecommendation: String,
    val rankPercentile: Double
)

data class NeglectedJewelTarget(
    val candidateName: String,
    val ticId: String,
    val yearsInBacklog: Double,
    val snr: Double,
    val periodDays: Double,
    val planetRadiusEarth: Double,
    val stellarMag: Double,
    val esi: Float,
    val whyNasaIgnoredIt: String,
    val validationPath: String
)
