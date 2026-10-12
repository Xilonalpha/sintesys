package com.synthesis.domain.model.exoplanet

/**
 * Domain models for planetary transmission and emission spectroscopy comparison,
 * public registry benchmarks, and unique atmospheric anomaly detection.
 */

/**
 * Individual wavelength data point in a planetary spectrum.
 */
data class SpectralDataPoint(
    val wavelengthMicrons: Double,
    val fluxOrDepthPpm: Double,
    val uncertaintyPpm: Double,
    val resolutionR: Double = 100.0,
    val candidateMolecule: String? = null,
    val isOutlier: Boolean = false
)

/**
 * Planetary spectrum retrieved from instruments (e.g. JWST NIRSpec/MIRI, HST WFC3, ground VLT)
 * or synthesis models.
 */
data class RetrievedPlanetarySpectrum(
    val id: String,
    val targetName: String,
    val ticId: String? = null,
    val hostStar: String,
    val instrumentFacility: String = "JWST NIRSpec PRISM (0.6 - 5.3 μm)",
    val observationEpochBjd: Double? = null,
    val equilibriumTempK: Double = 850.0,
    val planetRadiusEarth: Double = 4.2,
    val planetMassEarth: Double = 18.0,
    val stellarRadiusSun: Double = 0.95,
    val scaleHeightKm: Double = 350.0,
    val spectralPoints: List<SpectralDataPoint>,
    val notesOrObserver: String = "Retrieved via pipeline"
)

/**
 * Category of baseline benchmark in the public registry.
 */
enum class SpectralBenchmarkType(val displayName: String, val description: String) {
    PUBLISHED_SPACE_TELESCOPE("Observație Confirmată Spațială", "Spectru publicat oficial de NASA/ESA (JWST, HST, Spitzer)."),
    THEORETICAL_SOLAR_EQUILIBRIUM("Model Teoretic Chimie Solară (1x)", "Echilibru chimic standard pentru giganți gazoși și sub-Neptunieni."),
    SUPER_SOLAR_ENRICHED("Model Metalicitate Ridicată (100x)", "Atmosferă îmbogățită în elemente grele (H2O, CO2, CO dominat)."),
    HIGH_ALTITUDE_HAZE_DECK("Model Pătură de Aerosoli / Cețuri", "Spectru atenuat sau plat cauzat de cețuri fotochimice (tolini / silicați)."),
    SECONDARY_TERRESTRIAL("Model Atmosferă Secundară Terestră", "Greutate moleculară medie ridicată (N2, CO2, H2O) specifică planetelor telurice.")
}

/**
 * Entry in the public dataset registry representing either an officially published
 * exoplanet spectrum or a recognized theoretical baseline model.
 */
data class RegisteredSpectralBenchmark(
    val benchmarkId: String,
    val targetName: String,
    val benchmarkType: SpectralBenchmarkType,
    val facilityOrPaper: String,
    val publicationDoi: String? = null,
    val equilibriumTempK: Double,
    val verifiedMolecules: List<String>,
    val spectrumPoints: List<SpectralDataPoint>,
    val description: String
)

/**
 * Classification of anomaly identified when comparing against public registries.
 */
enum class SpectralAnomalyType(val displayName: String, val priorityTag: String) {
    UNPRECEDENTED_MOLECULAR_BAND("Semnătură Moleculară Necatalogată", "BIOMARKER / EXOTIC"),
    ANOMALOUS_DEPTH_SURGE("Surplus Masiv de Adâncime (Evadare Atmosferică)", "EXOSPHERE / TAIL"),
    UNEXPECTED_WATER_DEPLETION("Depleție Neașteptată a Apei (Atmosferă Desicată)", "DESICCATED / CONDENSED"),
    SUPER_RAYLEIGH_SCATTERING_SLOPE("Panta Super-Rayleigh Extremă (Cețuri Fotochimice)", "SMOG / STELLAR CONTAM"),
    THERMAL_INVERSION_EMISSION_SPIKE("Inversie Termică Stratosferică Neașteptată", "STRATOSPHERE"),
    TEMPORAL_ATMOSPHERIC_VARIABILITY("Variabilitate Atmosferică Temporală vs Arhivă", "WEATHER / ORBITAL PHASE"),
    ZERO_PRIOR_ARCHIVE_TARGET("Țintă Fără Istoric Spectroscopic Public (Inedită)", "100% UNPUBLISHED NOVELTY")
}

/**
 * Severity / novelty level of an identified anomaly.
 */
enum class AnomalyNoveltyLevel(val badgeLabel: String, val minSigma: Double) {
    BREAKTHROUGH_CANDIDATE("Descoperire Majoră (S/N > 5σ)", 5.0),
    HIGH_SIGNIFICANCE("Semnificativă (S/N > 3.5σ)", 3.5),
    MODERATE_DEVIATION("Deviație Moderată (2.5σ - 3.5σ)", 2.5),
    MARGINAL_OUTLIER("Posibil Zgomot Instrumental (< 2.5σ)", 0.0)
}

/**
 * Single anomalous spectral feature identified during comparison.
 */
data class SpectralAnomaly(
    val id: String,
    val anomalyType: SpectralAnomalyType,
    val wavelengthMicrons: Double,
    val observedValuePpm: Double,
    val registryExpectedValuePpm: Double,
    val residualDeviationPpm: Double,
    val statisticalSignificanceSigma: Double,
    val candidateMoleculesOrMechanism: List<String>,
    val description: String,
    val noveltyLevel: AnomalyNoveltyLevel
)

/**
 * Overall comparison report between a retrieved spectrum and the public registry.
 */
data class PublicRegistryComparisonResult(
    val comparisonId: String,
    val retrievedTargetName: String,
    val matchedBenchmarkId: String?,
    val matchedBenchmarkName: String,
    val isTargetKnownInPublicRegistry: Boolean,
    val publicArchiveStatus: String,
    val reducedChiSquared: Double,
    val degreesOfFreedom: Int,
    val maxDeviationSigma: Double,
    val totalDataPointsAnalyzed: Int,
    val anomaliesCount: Int,
    val identifiedAnomalies: List<SpectralAnomaly>,
    val noveltyIndexPercent: Double, // 0 - 100%
    val scientificVerdict: String,
    val followUpRecommendations: List<String>,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Information regarding disposition and public registration in NASA Exoplanet Archive / MAST.
 */
data class PublicArchiveDispositionInfo(
    val targetIdentifier: String,
    val isPresentInNasaArchive: Boolean,
    val confirmedPlanetsCount: Int,
    val publishedSpectraCount: Int,
    val discoveryFacility: String?,
    val hasPublishedTransmissionSpectra: Boolean,
    val archiveNotes: String
)
