package com.synthesis.domain.model

/**
 * Universal scientific model for exoplanets and exoplanetary candidates
 * pulled directly from the real NASA Exoplanet Archive (IPAC/Caltech TAP service).
 *
 * Supports TESS (toi), Kepler (cumulative KOI), K2 (k2candidates), and Confirmed benchmarks (pscomppars).
 */
data class ExoplanetCandidate(
    val toiId: String,                  // Planet or Candidate Designation (e.g. "TOI-4398.01", "KOI-4878.01", "TRAPPIST-1 e")
    val ticId: String,                  // Host star identifier (TIC, KIC, EPIC, Host name)
    val disposition: TfopwgDisposition,
    val catalogSource: ExoplanetCatalogSource = ExoplanetCatalogSource.TESS,
    val orbitalPeriodDays: Double?,
    val transitDurationHours: Double?,
    val transitDepthPpm: Double?,       // parts-per-million dimming during transit
    val planetRadiusEarth: Double?,     // in Earth radii (R⊕)
    val planetMassEarth: Double?,       // in Earth masses (M⊕)
    val insolationEarthFlux: Double?,   // relative to Earth solar flux (S⊕)
    val equilibriumTempK: Double?,      // in Kelvin
    val stellarTessMagnitude: Double?,  // host star brightness (lower = brighter)
    val stellarDistancePc: Double?,
    val stellarEffTempK: Double?,       // Teff in Kelvin
    val stellarRadiusSun: Double?,      // R☉
    val createdDate: String?,           // date flagged/added
    val lastUpdated: String?,
    val worthInvestigatingScore: Float, // 0.0 to 1.0 triage score
    val earthSimilarityIndex: Float?,   // 0.0 to 1.0 ESI (Earth Similarity Index)
    val isInHabitableZone: Boolean = false,
    val isUnvettedCandidate: Boolean = false, // genuine unconfirmed archival discovery
    val anomalyType: String? = null,    // Detected physical anomaly (e.g. "Neptunian Desert Anomaly", "Ultra-Short Period")
    val discoveryMethod: String? = null,
    val discoveryYear: Int? = null,
    val scoreReasoning: List<String>
)

enum class TfopwgDisposition {
    PC,         // Planetary Candidate — unconfirmed
    APC,        // Ambiguous Planetary Candidate — unconfirmed, blend risk
    CANDIDATE,  // Kepler/K2 Candidate
    CONFIRMED,  // Confirmed Exoplanet
    CP,         // Confirmed Planet
    KP,         // Known Planet
    FP,         // False Positive
    FA,         // False Alarm
    UNKNOWN
}

enum class ExoplanetCatalogSource(val displayName: String, val badgeColorHex: Long) {
    TESS("NASA TESS (TOI)", 0xFF00E5FF),
    KEPLER_KOI("Kepler Prime (KOI)", 0xFFFF9100),
    K2("Kepler K2 Mission", 0xFFFFD700),
    PHL_HABITABLE("PHL Habitable (Arecibo)", 0xFF00E676),
    CHEOPS_GAIA("ESA CHEOPS & Gaia", 0xFF00B0FF),
    UNVETTED_DISCOVERY("Unvetted Candidate", 0xFFE040FB),
    CONFIRMED_BENCHMARK("Confirmed Exoplanet", 0xFF7C4DFF)
}
