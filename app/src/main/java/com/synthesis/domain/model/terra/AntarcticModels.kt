package com.synthesis.domain.model.terra

/**
 * Domain models for scientific Antarctica Subglacial Exploration
 * based on open peer-reviewed datasets (BedMachine Antarctica, Bedmap2, ICESat-2, CryoSat-2).
 */
data class AntarcticSubglacialLake(
    val id: String,
    val name: String,
    val region: String,
    val iceThicknessMeters: Int,
    val estimatedWaterDepthMeters: Int,
    val pressureAtmospheres: Double,
    val isolationAgeYears: String,
    val extremaType: String,
    val geothermalHeatFluxMwM2: Double,
    val researchStatus: String,
    val scientificSignificance: String
)

data class AntarcticBedrockFeature(
    val id: String,
    val name: String,
    val type: String, // "Canion Abisal Subglaciar", "Lanț Montan Îngropat", "Bazin Marin Subglaciar"
    val bedrockElevationMeters: Int, // Can be negative (below sea level)
    val iceCoverThicknessMeters: Int,
    val locationDescription: String,
    val discoveryMethod: String, // e.g. "Radar Aerian Penetrabil de Gheață (Ice-penetrating Radar)"
    val isostaticReboundEstimateMeters: Int,
    val description: String
)

data class PolarAltimetryState(
    val activeMission: String,
    val sensorType: String,
    val photonCountPerPulse: String,
    val totalIceVolumeKm3: Double,
    val annualIceMassBalanceGtYear: Double,
    val seaLevelEquivalentTotalMeters: Double,
    val iceVelocityMaxMetersPerYear: Double,
    val radarPenetrationDepthMeters: Int,
    val lastCalibrationTime: String
)

data class IsostaticReboundSimulation(
    val iceMeltPercent: Float, // 0.0f to 1.0f
    val iceVolumeLostKm3: Double,
    val globalSeaLevelRiseMeters: Double,
    val crustalReboundMaxMeters: Double,
    val newExposedLandAreaKm2: Double
)
