package com.synthesis.domain.model.exoplanet

/**
 * Domain models for the 5 advanced discovery engines:
 * 1. Biosignatures & Technosignatures (Chemical disequilibrium & Dyson megastructure light curve asymmetry)
 * 2. False-Positive Rescuer (Gaia DR3 dilution de-blender recovering rocky planets mislabeled as grazing binaries)
 * 3. Multi-Sector Harmonic Stitcher (Cross-sector multi-year ephemeris stitcher)
 * 4. 1D Planetary Climate Simulator (Budyko-Sellers energy balance & runaway greenhouse bifurcation)
 * 5. Live Transit Sky Radar (Ephemeris countdown & observation planner)
 */

// 1. Astrobiology & Biosignature Models
data class AtmosphericMolecule(
    val formula: String,
    val name: String,
    val abundancePpm: Double,
    val detectionSigma: Double,
    val isBiosignatureCandidate: Boolean,
    val isTechnosignatureCandidate: Boolean
)

data class BiosignatureAssessment(
    val planetName: String,
    val hostStar: String,
    val equilibriumTempK: Double,
    val thermodynamicDisequilibriumMetric: Double, // Krissansen-Totton 2018 Gibbs free energy (kJ/mol)
    val detectedMolecules: List<AtmosphericMolecule>,
    val biosignatureScore: Double, // 0 - 100
    val biologicalExplanation: String,
    val abioticAlternativeRisk: String
)

data class TechnosignatureCandidate(
    val candidateName: String,
    val hostStar: String,
    val transitAsymmetryIndex: Double, // Ingress vs Egress slope difference
    val ingressDurationHours: Double,
    val egressDurationHours: Double,
    val depthVariancePpm: Double,
    val cfcPollutionSigma: Double?, // e.g. CFC-11 or CFC-12 absorption bands
    val artificialMegastructureProbability: Double, // 0 - 100%
    val physicalDescription: String,
    val naturalPhenomenaHypothesis: String
)

// 2. Dilution De-Blender Models (False Positive Rescuer)
data class DilutionRescueCandidate(
    val candidateName: String,
    val ticId: String,
    val originalNasaStatus: String, // e.g. "False Positive (FP - Grazing EB)"
    val observedTransitDepthPpm: Double,
    val targetStarMagGaia: Double,
    val blendingStarMagGaia: Double,
    val angularSeparationArcsec: Double,
    val stellarRadiusSun: Double,
    // De-blended true astrophysical properties
    val dilutionCorrectionFactor: Double,
    val trueTransitDepthPpm: Double,
    val apparentRadiusEarth: Double,
    val trueCorrectedRadiusEarth: Double,
    val isRescuedToRockyPlanet: Boolean,
    val rescueVerdict: String
)

// 3. Multi-Sector Harmonic Stitcher Models
data class HarmonicPeriodHypothesis(
    val harmonicIndexK: Int,
    val periodDays: Double,
    val semiMajorAxisAu: Double,
    val isHabitableZone: Boolean,
    val isRuledOutBySurveyCoverage: Boolean,
    val ruleOutReason: String?
)

data class HarmonicStitchCandidate(
    val candidateName: String,
    val ticId: String,
    val epoch1Btjd: Double,
    val sector1: String,
    val epoch2Btjd: Double,
    val sector2: String,
    val deltaDays: Double,
    val candidateHarmonics: List<HarmonicPeriodHypothesis>,
    val bestUniquePeriodDays: Double,
    val bestSemiMajorAxisAu: Double,
    val stellarTeffK: Double,
    val isHabitableZone: Boolean,
    val solutionConfidence: Double
)

// 4. 1D Energy Balance Climate Simulator Models
enum class ClimateRegime(val title: String, val description: String) {
    RUNAWAY_GREENHOUSE("Runaway Greenhouse (Venusian Hell)", "Oceans boiled away into stratosphere; thick CO2 steam envelope."),
    TROPICAL_SUPER_HABITABLE("Super-Habitable Maritime", "Ice-free poles, expansive temperate oceans, ideal for biodiversity."),
    TEMPERATE_EARTH_LIKE("Temperate (Earth-Analog)", "Stable polar ice caps, equator-to-pole circulation, liquid surface oceans."),
    SNOWBALL_GLACIATED("Snowball Glaciation", "Runaway ice-albedo feedback froze the planet from poles to equator.")
}

data class ClimateSimulationResult(
    val planetName: String,
    val stellarInsolationSolar: Double,
    val co2LevelBar: Double,
    val surfacePressureBar: Double,
    val bondAlbedo: Double,
    val greenhouseWarmingK: Double,
    val meanSurfaceTempC: Double,
    val polarIceLatitudeDeg: Double, // 90° = ice-free, 0° = completely frozen snowball
    val liquidOceanCoveragePercent: Double,
    val climateRegime: ClimateRegime,
    val runawayThresholdInsolation: Double,
    val snowballThresholdInsolation: Double,
    val climateSummary: String
)

// 5. Live Transit Sky Radar Models
data class LiveTransitTarget(
    val candidateName: String,
    val ticId: String,
    val constellation: String,
    val rightAscension: String,
    val declination: String,
    val apparentVisualMag: Double,
    val periodDays: Double,
    val transitDurationHours: Double,
    val nextTransitEpochBtjd: Double,
    val hoursUntilIngress: Double,
    val transitDepthPpm: Double,
    val amateurTelescopeFeasibility: String // e.g. "Feasible (8-inch SCT + CMOS)"
)

// ==================== 2026 ADVANCED MODULES ====================

// 6. Smart Scope Direct-Sync Models (ZWO Seestar S50/S30, DWARF 3, Vaonis Vespera, Unistellar)
enum class SmartScopeModel(val label: String, val defaultPort: Int, val protocol: String) {
    ZWO_SEESTAR_S50("ZWO Seestar S50 / S30", 4700, "Seestar TCP/REST API"),
    DWARF_3("DwarfLab DWARF II / 3", 9988, "DwarfLab WebSocket/REST"),
    VAONIS_VESPERA("Vaonis Vespera / Stellina", 8080, "Vaonis Native REST"),
    UNISTELLAR_EVSCOPE("Unistellar eVscope / Odyssey", 5000, "Unistellar Socket Protocol"),
    ASCOM_ALPACA("ASCOM Alpaca Universal Hub", 11112, "ASCOM Standard REST")
}

data class SmartTelescopeTelemetry(
    val model: SmartScopeModel = SmartScopeModel.ZWO_SEESTAR_S50,
    val ipAddress: String = "192.168.1.100",
    val port: Int = 4700,
    val isConnected: Boolean = false,
    val batteryPercent: Int = 88,
    val sensorTempC: Double = -5.2,
    val isTracking: Boolean = false,
    val isStacking: Boolean = false,
    val exposureTimeSec: Double = 10.0,
    val stackedFramesCount: Int = 0,
    val targetName: String? = null,
    val targetRa: String? = null,
    val targetDec: String? = null,
    val liveLog: List<String> = emptyList()
)

// 7. PLATO Asteroseismology & Age Solver Models (ESA 2026)
data class PlatoAsteroseismicResult(
    val starName: String,
    val ticId: String,
    val nuMaxMicroHz: Double, // Frequency of maximum oscillation power
    val deltaNuMicroHz: Double, // Large frequency separation of p-modes
    val teffK: Double,
    val metallicityFeH: Double,
    val observedTransitDepthPpm: Double,
    // Asteroseismically solved stellar parameters (1% precision)
    val stellarMassSun: Double,
    val stellarRadiusSun: Double,
    val meanStellarDensitySun: Double,
    val surfaceGravityLogG: Double,
    val stellarAgeGyr: Double,
    val mainSequenceLifetimeGyr: Double,
    // Ultra-precise exoplanet parameters
    val planetRadiusEarth: Double,
    val planetRadiusErrorPercent: Double, // Target: <= 1.2%
    val isOldEnoughForLife: Boolean, // System age >= 2.0 Gyr
    val habitabilityVerdict: String,
    val asteroseismicMethodSummary: String
)

// 8. JWST Atmospheric Transmission Synthesizer Models (Cycle 4/5)
data class SpectralBandPoint(
    val wavelengthMicrons: Double,
    val transitDepthPpm: Double,
    val absorbingMolecule: String?,
    val instrumentalErrorPpm: Double
)

data class JwstTransmissionSpectrumResult(
    val planetName: String,
    val hostStar: String,
    val equilibriumTempK: Double,
    val surfaceGravityMs2: Double,
    val scaleHeightKm: Double,
    val transmissionSpectroscopyMetric: Double, // TSM (Kempton et al. 2018)
    val emissionSpectroscopyMetric: Double, // ESM (Kempton et al. 2018)
    val jwstFollowUpPriority: String, // e.g. "TIER 1 (Cycle 5 Golden Target)"
    val detectedAbsorptionFeatures: List<String>,
    val spectrumPoints: List<SpectralBandPoint>,
    val photochemistryDetected: Boolean, // e.g. SO2 peak at 4.05 um
    val atmosphericSummary: String
)

// 9. Exomoon Inversion & Tidal Heating Ocean Models
enum class TidalOceanRegime(val title: String, val description: String) {
    SUSTAINED_SUB_SURFACE_OCEAN("Sustained Subsurface Liquid Ocean (Europa/Enceladus Analogue)", "Tidal dissipation maintains a global liquid water ocean beneath ice shell."),
    HYPER_VOLCANIC_MAGMA_OCEAN("Hyper-Volcanic Magma Ocean (Super-Io)", "Extreme tidal heating produces continuous surface volcanism and sulfur lakes."),
    FROZEN_SOLID_ICE("Frozen Solid (Insufficient Tidal Dissipation)", "Tidal flux is too weak; water ice is frozen to the bedrock."),
    TIDALLY_LOCKED_DRY_ROCK("Dry Desiccated Rock", "Exomoon has lost volatiles due to severe tidal despinning.")
}

data class ExomoonTidalOceanResult(
    val moonName: String,
    val hostPlanet: String,
    val hostStar: String,
    val planetMassJupiter: Double,
    val moonMassEarth: Double,
    val moonRadiusEarth: Double,
    val semiMajorAxisMoonKm: Double,
    val orbitalEccentricity: Double,
    // Orbital perturbation observables
    val hillRadiusKm: Double,
    val isDynamicallyStable: Boolean,
    val ttvAmplitudeSeconds: Double, // Transit Timing Variation
    val tdvAmplitudeSeconds: Double, // Transit Duration Variation
    val ttvTdvPhaseShiftRad: Double, // Classic pi/2 signature for moon confirmation
    // Geophysics & Tidal Ocean
    val tidalHeatFluxWattsM2: Double, // W / m^2
    val estimatedIceShellThicknessKm: Double,
    val oceanVolumeEarthOceans: Double,
    val oceanRegime: TidalOceanRegime,
    val astrobiologicalPotential: String
)

// 10. Space Weather Magnetospheric Shielding Models (M-Dwarf Flares & CMEs)
enum class ShieldingVerdict(val title: String, val description: String) {
    MAG_SHIELDED_HABITABLE("Magnetically Shielded (Habitable Atmosphere Retained)", "Standoff radius exceeds surface; dipole dynamo deflects stellar CMEs."),
    CRITICAL_STRIPPING_WARNING("Severe Atmospheric Stripping Risk", "Magnetosphere is compressed; high XUV erosion depletes light gases."),
    ATMOSPHERE_EVAPORATED_AIRLESS_ROCK("Atmosphere Completely Stripped (Airless Body)", "Dynamic solar wind ram pressure crushed the magnetosphere to the surface.")
}

data class MagnetosphereShieldingResult(
    val planetName: String,
    val hostStar: String,
    val semiMajorAxisAu: Double,
    val stellarWindVelocityKms: Double,
    val stellarWindDensityProtonsCm3: Double,
    val stellarXuvFluxEarthUnits: Double,
    val coreMassFraction: Double,
    val rotationPeriodHours: Double,
    // Derived magnetic properties
    val dipoleMagneticMomentEarthUnits: Double,
    val surfaceMagneticFieldGauss: Double,
    val solarWindRamPressureNanoPa: Double,
    val magnetopauseStandoffRadiusRp: Double, // in planetary radii (must be > 1.0)
    val atmosphericMassLossRateKgS: Double,
    val estimatedAtmosphericLifetimeGyr: Double,
    val shieldingVerdict: ShieldingVerdict,
    val spaceWeatherAnalysis: String
)
