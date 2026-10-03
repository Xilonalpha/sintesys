package com.synthesis.domain.model.terra

/**
 * Universal domain models for the Terra Incognita Frontier Engine.
 * Represents cutting-edge geophysical, abyssal oceanographic, and deep-mantle frontiers.
 */
enum class TerraDomain(val displayName: String, val badgeColorHex: Long) {
    HADAL_OCEANS("Oceane Hadale & Abisale", 0xFF00E5FF),
    DEEP_MANTLE_WATER("Oceanul din Mantaua Terrei", 0xFF00E676),
    THEIA_LLSVP("Reminiscențe Theia & LLSVP", 0xFFFF9100),
    GEOMAGNETIC_SAA("Anomalia Atlanticului de Sud", 0xFFFF5252),
    DEEP_BIOSPHERE("Biosfera Profundă & Radioliză", 0xFFE040FB),
    ABYSSAL_HEAT_ENGINE("Transportorul Termic Abisal", 0xFF448AFF),
    ANTARCTIC_SUBGLACIAL("Antarctica Subglaciară (BedMachine)", 0xFF80DEEA)
}

data class TerraFrontierMystery(
    val id: String,
    val title: String,
    val domain: TerraDomain,
    val misconception: String,           // Percepție eronată comună
    val scientificDiscovery: String,      // Descoperire & Realitate științifică
    val physicalPrinciple: String,       // Formula / Principiul fizic
    val keyMetricLabel: String,
    val keyMetricValue: String,
    val explorationReadinessPercent: Int, // 0 - 100% cât de mult din acest fenomen a fost explorat de omenire
    val implicationsForLife: String
)

data class HadalTrenchProfile(
    val name: String,
    val location: String,
    val maxDepthMeters: Int,
    val pressureAtmospheres: Double,
    val unmappedFloorPercent: Double,
    val temperatureCelsius: Double,
    val chemicalEnergySource: String,
    val primaryEndemicOrganisms: List<String>
)

data class MantleWaterHydrationState(
    val mineralPhase: String,            // e.g. "Ringwoodite (γ-olivine)"
    val depthRangeKm: String,            // e.g. "520 - 660 km"
    val hydrationCapacityWeightPercent: Double, // e.g. 1.5%
    val estimatedWaterMassKg: Double,    // e.g. 2.1e21 kg
    val ratioToSurfaceOceans: Double,    // e.g. 1.5x all oceans
    val subductionWaterPumpRateMtYear: Double // Megatons pumped/year
)

data class GeomagneticAnomalousState(
    val regionName: String,
    val currentIntensityMicroTesla: Double, // Normal Earth is ~30-60 µT, SAA is ~22-24 µT
    val intensityDecayRatePerDecadePercent: Double,
    val northMagneticDriftKmPerYear: Double,
    val lowEarthOrbitRadiationRisk: String, // "CRITICĂ (Van Allen Seep)", "MODERATĂ", "NOMINALĂ"
    val isSplittingIntoDoubleCore: Boolean
)

data class DeepBiosphereMetrics(
    val maxExploredDepthKm: Double,
    val totalEstimatedSubsurfaceBiomassGtCarbon: Double, // ~15-23 Gt C
    val primaryEnergyMechanism: String,  // e.g. "Radioliza Apei (U, Th, 40K) -> H2 abiotik"
    val doublingTimeYears: String        // Microorganisms dividing once every 100 - 10,000 years!
)
