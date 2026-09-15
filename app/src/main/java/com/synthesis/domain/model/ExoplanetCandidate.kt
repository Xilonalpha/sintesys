package com.synthesis.domain.model

/**
 * A TESS Object of Interest (TOI) pulled directly from the real NASA Exoplanet
 * Archive TAP service. This is NOT a synthetic/generated discovery — every field
 * comes straight from NASA's own public database (exoplanetarchive.ipac.caltech.edu).
 *
 * disposition == PC (planet candidate) or APC (ambiguous planet candidate) means
 * TESS's follow-up team has NOT yet confirmed or rejected it — it is genuinely
 * unresolved, not because anyone is "behind", but because confirming a candidate
 * requires scarce follow-up telescope time (radial velocity, additional transits).
 */
data class ExoplanetCandidate(
    val toiId: String,              // e.g. "TOI-1234.01"
    val ticId: String,              // TESS Input Catalog host star ID
    val disposition: TfopwgDisposition,
    val orbitalPeriodDays: Double?,
    val transitDurationHours: Double?,
    val transitDepthPpm: Double?,   // parts-per-million dimming during transit
    val planetRadiusEarth: Double?, // in Earth radii
    val insolationEarthFlux: Double?,
    val equilibriumTempK: Double?,
    val stellarTessMagnitude: Double?, // lower = brighter host star = easier to confirm
    val stellarDistancePc: Double?,
    val stellarEffTempK: Double?,
    val stellarRadiusSun: Double?,
    val createdDate: String?,       // when TESS first flagged it
    val lastUpdated: String?,
    val worthInvestigatingScore: Float, // 0-1, heuristic re-ranking — see ExoplanetCandidateFinder
    val scoreReasoning: List<String>
)

enum class TfopwgDisposition {
    PC,  // Planetary Candidate — unconfirmed
    APC, // Ambiguous Planetary Candidate — unconfirmed, signal is unclear
    CP,  // Confirmed Planet
    KP,  // Known Planet
    FP,  // False Positive
    FA,  // False Alarm
    UNKNOWN
}
