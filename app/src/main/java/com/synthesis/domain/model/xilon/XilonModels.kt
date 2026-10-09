package com.synthesis.domain.model.xilon

/**
 * Photometric photon measurement from MAST / TESS time-series
 */
data class MastPhotometryPoint(
    val phase: Double,          // Phase (hours from mid-transit or dimensionless [-0.5 .. +0.5])
    val detrendedFlux: Double,  // Fractional or relative flux delta (ppm or norm)
    val modelFlux: Double?      // Fitted transit box/trapezoid model value
)

/**
 * Host star astrophysical properties retrieved from MAST TESS Input Catalog (TIC)
 */
data class MastStarProperties(
    val ticId: String,
    val tMag: Double?,
    val teff: Double?,
    val radiusSun: Double?,
    val massSun: Double?,
    val distancePc: Double?,
    val ra: Double?,
    val dec: Double?,
    val luminosity: Double?
)

enum class XilonDiscoveryVerdict(val label: String, val colorHex: Long) {
    CONFIRMED_BENCHMARK_VALIDATION("CONFIRMED PLANET RECOVERY (BENCHMARK)", 0xFF00E676),
    PRIMARY_EXOPLANET_CANDIDATE("AUTONOMOUS EXOPLANET CANDIDATE", 0xFF00E676),
    HABITABLE_ZONE_CANDIDATE("HABITABLE ZONE PRIMARY TARGET", 0xFF00E5FF),
    ECLIPSING_BINARY_FALSE_POSITIVE("STELLAR BINARY (FALSE POSITIVE)", 0xFFFF1744),
    MARGINAL_DETECTION("MARGINAL CANDIDATE (SUB-THRESHOLD)", 0xFFFF9100),
    NO_SIGNIFICANT_TRANSIT("STELLAR NOISE / NO TRANSIT", 0xFF9E9E9E)
}

enum class ArchivalCatalogStatus {
    CONFIRMED_PLANET_VALIDATION,  // Found in NASA pscomppars (e.g., WASP-126 b, WASP-119 b)
    NASA_TOI_CANDIDATE,           // Found in NASA toi table (e.g., TOI-1895.01)
    POTENTIAL_NEW_SIGNAL          // Neither in pscomppars nor in toi
}

enum class PlanetaryCompositionClass(val label: String, val description: String, val colorHex: Long) {
    ROCKY_TERRESTRIAL("Rocky / Iron-Silicate Core", "Comparable to Earth or Mercury with high bulk density", 0xFF00E676),
    SUPER_EARTH_VOLATILE("Super-Earth / Heavy Mantle", "Dense silicate-iron interior with possible volatile envelope", 0xFF00E5FF),
    WATER_WORLD("Ocean World / Volatile-Rich", "Significant H2O volatile layer over high-pressure ice mantle", 0xFF2979FF),
    SUB_NEPTUNE("Sub-Neptune / Gas Dwarf", "Rocky core enveloped in substantial hydrogen/helium atmosphere", 0xFFAB47BC),
    JOVIAN_GAS_GIANT("Jovian / Gas Giant", "Massive hydrogen-helium fluid interior with deep metallic core", 0xFFFFB300),
    HOT_JUPITER("Hot Jupiter / Inflated Giant", "Thermally expanded Jovian atmosphere due to intense stellar insolation", 0xFFFF7043)
}

data class ArchivalCrossMatch(
    val status: ArchivalCatalogStatus,
    val canonicalName: String,             // e.g. "WASP-126 b" or "TOI-1895.01" or "Uncatalogued Target"
    val officialDisposition: String,       // e.g. "CONFIRMED PLANET" or "PLANET CANDIDATE (PC)" or "UNRECORDED"
    val discoveryYear: Int?,               // e.g. 2016
    val discoveryFacility: String?,        // e.g. "SuperWASP-South"
    val literaturePeriodDays: Double?,     // e.g. 3.28883
    val literatureDurationHours: Double?,  // e.g. 3.4368
    val literatureDepthPpm: Double?,       // e.g. 7005.7
    val literatureRadiusEarth: Double?,    // e.g. 10.51
    val isBenchmarkTarget: Boolean         // true for known confirmed validation
)

/**
 * Complete astrophysical discovery output generated directly from raw telescope photons
 */
data class XilonDetectionResult(
    val star: MastStarProperties,
    val sector: String,
    val tceName: String,
    val totalPhotonsAnalyzed: Int,
    val transitDepthPpm: Double,
    val transitDurationHours: Double,
    val orbitalPeriodDays: Double,
    val signalToNoiseRatio: Double,           // SNR in sigmas (threshold >= 7.1)
    val oddEvenTransitDeltaSigma: Double,      // Odd vs Even depth difference in sigmas
    val secondaryEclipseDepthPpm: Double,      // Occultation depth at phase ~ 0.5
    val derivedPlanetRadiusEarth: Double?,     // Rp = R* * sqrt(depth) in R⊕
    val derivedSemiMajorAxisAu: Double?,       // a = (M* * P^2)^(1/3)
    val derivedEquilibriumTempK: Double?,      // Teq in Kelvin
    val derivedPlanetMassEarth: Double? = null,       // Estimated mass in M⊕ (Chen & Kipping)
    val surfaceGravityEarthG: Double? = null,         // g / g⊕ = (M/M⊕) / (R/R⊕)^2
    val planetDensityGcm3: Double? = null,            // Mean density in g/cm³
    val escapeVelocityKms: Double? = null,            // v_esc in km/s
    val compositionClass: PlanetaryCompositionClass? = null,
    val isHabitableZone: Boolean,
    val verdict: XilonDiscoveryVerdict,
    val verdictTitle: String,
    val verdictSummary: String,
    val archivalCrossMatch: ArchivalCrossMatch,
    val lightCurveSample: List<MastPhotometryPoint>,
    val analysisLog: List<String>,
    val astrometry: com.synthesis.engine.astrometry.AstrometryEngine.AstrometricTargetDossier? = null
)

/**
 * Presets of unvetted or high-priority candidate stars available for immediate scan
 */
data class TargetStarCandidate(
    val ticId: String,
    val name: String,
    val description: String,
    val sector: String,
    val tce: String,
    val estimatedMag: Double,
    val targetType: String,
    val knownClassification: String,
    val advisoryPriority: String = "MEDIUM", // HIGH_VALUE_UNVETTED, HABITABLE_ZONE_BLINDSPOT, BENCHMARK_CONTROL
    val scientificReason: String = "Candidate star from TESS survey."
)
