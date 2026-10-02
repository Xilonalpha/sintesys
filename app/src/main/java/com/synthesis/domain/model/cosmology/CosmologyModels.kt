package com.synthesis.domain.model.cosmology

// --- BIG BANG & COSMOLOGY MODELS ---

data class CosmicEpoch(
    val epochName: String,
    val timeSinceBigBang: String,
    val redshiftZ: Double,
    val temperatureKelvin: Double,
    val scaleFactorA: Double,
    val dominantEnergy: String,
    val physicalProcesses: String,
    val keyObservables: String
)

data class BigBangSimulationResult(
    val hubbleConstantH0: Double,
    val matterDensityOmegaM: Double,
    val darkEnergyOmegaLambda: Double,
    val radiationDensityOmegaR: Double,
    val curvatureOmegaK: Double,
    val cosmicAgeGyr: Double,
    val soundHorizonMpc: Double,
    val cmbTemperatureK: Double,
    val decelerationParameterQ0: Double,
    val hubbleTensionDeltaKmS: Double,
    val expansionTrajectory: List<ExpansionCurvePoint>,
    val bbnAbundances: BbnPrimordialAbundances
)

data class ExpansionCurvePoint(
    val redshiftZ: Double,
    val scaleFactorA: Double,
    val timeGyr: Double,
    val hubbleParamH: Double,
    val luminosityDistGpc: Double,
    val angularDiameterDistGpc: Double
)

data class BbnPrimordialAbundances(
    val hydrogenFraction: Double, // ~ 0.75
    val helium4MassFractionYp: Double, // ~ 0.245
    val deuteriumAbundanceRatioDoverH: Double, // ~ 2.5e-5
    val helium3AbundanceRatio: Double, // ~ 1.0e-5
    val lithium7AbundanceRatio: Double // ~ 1.6e-10
)

// --- BLACK HOLE & GENERAL RELATIVITY MODELS ---

data class BlackHoleTarget(
    val id: String,
    val name: String,
    val classification: String, // Supermassive, Stellar-mass, Intermediate, Binary Merger
    val massSolar: Double,
    val spinParameterAStar: Double, // 0.0 (Schwarzschild) to 0.998 (Thorne extremal Kerr)
    val distanceLightYears: Double,
    val constellation: String,
    val hostSystem: String,
    val accretionRateEddingtonFraction: Double,
    val jetLorentzFactor: Double?,
    val discoveryMethod: String,
    val description: String
)

data class RelativisticBlackHoleProfile(
    val target: BlackHoleTarget,
    val schwarzschildRadiusKm: Double, // 2GM/c^2
    val eventHorizonRadiusKm: Double, // r+ = GM/c^2 * (1 + sqrt(1 - a*^2))
    val ergosphereEquatorialRadiusKm: Double, // 2GM/c^2
    val photonSphereRadiusKm: Double, // r_ph
    val iscoRadiusKm: Double, // Innermost Stable Circular Orbit
    val shadowDiameterMicroArcsec: Double, // Angular shadow on sky as seen from Earth
    val hawkingTemperatureKelvin: Double, // T_H
    val evaporationLifetimeYears: Double, // t_evap
    val gravitationalRedshiftAt1_5Rg: Double, // z_grav
    val penroseEnergyEfficiencyPercent: Double, // up to 29% for extremal Kerr
    val gravitationalWaveChirpFrequencyHz: Double?, // For binary mergers
    val gravitationalWaveRadiatedMassSolar: Double?,
    val iscoVelocityFractionC: Double
)
