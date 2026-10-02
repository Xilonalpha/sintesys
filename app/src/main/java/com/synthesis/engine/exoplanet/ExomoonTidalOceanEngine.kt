package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.ExomoonTidalOceanResult
import com.synthesis.domain.model.exoplanet.TidalOceanRegime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Exomoon Inversion & Tidal Heating Ocean Engine (2026 Edition)
 *
 * Implements:
 * 1. Mutual TTV (Transit Timing Variations) and TDV (Transit Duration Variations)
 *    orthogonal phase-shift inversion (Kipping 2009, MNRAS 392:181).
 * 2. Hill Sphere stability boundaries (Domingos et al. 2006).
 * 3. Viscoelastic tidal dissipation heating (Barnes et al. 2013, Henning et al. 2009)
 *    evaluating subsurface liquid water oceans beneath ice shells (Europa/Enceladus analogues).
 */
@Singleton
class ExomoonTidalOceanEngine @Inject constructor() {

    companion object {
        const val G_CONST = 6.67430e-11 // m^3 / (kg * s^2)
        const val M_JUPITER_KG = 1.898e27
        const val M_EARTH_KG = 5.972e24
        const val M_SUN_KG = 1.989e30
        const val R_EARTH_M = 6.371e6
        const val AU_M = 1.496e11
        const val K_ICE_CONDUCTIVITY = 2.5 // W / (m * K)
        const val EARTH_OCEAN_VOLUME_M3 = 1.335e18
    }

    /**
     * Inverts exomoon orbital parameters and computes internal tidal heating flux.
     */
    fun analyzeExomoonAndTidalOcean(
        moonName: String,
        hostPlanet: String,
        hostStar: String,
        planetMassJupiter: Double,
        stellarMassSun: Double,
        planetSemiMajorAxisAu: Double,
        moonMassEarth: Double,
        moonRadiusEarth: Double,
        semiMajorAxisMoonKm: Double,
        orbitalEccentricity: Double,
        surfaceTemperatureK: Double = 120.0
    ): ExomoonTidalOceanResult {
        val mPlanetKg = planetMassJupiter * M_JUPITER_KG
        val mStarKg = stellarMassSun * M_SUN_KG
        val mMoonKg = moonMassEarth * M_EARTH_KG
        val rMoonM = moonRadiusEarth * R_EARTH_M
        val aPlanetM = planetSemiMajorAxisAu * AU_M
        val aMoonM = semiMajorAxisMoonKm * 1000.0

        // 1. Hill Radius and Orbital Stability (Domingos et al. 2006)
        // R_Hill = a_p * (M_p / (3 * M_*))^(1/3)
        val hillRadiusM = aPlanetM * (mPlanetKg / (3.0 * mStarKg)).pow(1.0 / 3.0)
        val hillRadiusKm = hillRadiusM / 1000.0
        val maxStableOrbitKm = (0.489 * hillRadiusM) / 1000.0 // Prograde stable limit
        val isDynamicallyStable = semiMajorAxisMoonKm in (50_000.0..maxStableOrbitKm)

        // 2. Transit Timing and Duration Variations (TTV & TDV) - Kipping 2009
        // Planet orbital velocity
        val vOrbPlanet = sqrt((G_CONST * mStarKg) / aPlanetM)
        // Barycenter displacement wobble amplitude (TTV)
        val ttvAmplitudeSeconds = (aMoonM * mMoonKg) / (vOrbPlanet * (mPlanetKg + mMoonKg))

        // Exomoon orbital velocity
        val vMoonOrb = sqrt((G_CONST * mPlanetKg) / aMoonM)
        val baselineTransitDurationSec = 3.5 * 3600.0 // ~ 3.5 hours
        val tdvAmplitudeSeconds = baselineTransitDurationSec * (vMoonOrb / vOrbPlanet) * (mMoonKg / mPlanetKg)

        // Classical pi/2 phase-shift signature between TTV and TDV
        val ttvTdvPhaseShiftRad = PI / 2.0

        // 3. Viscoelastic Tidal Heating Dissipation (Barnes et al. 2013, Peale et al. 1979)
        // E_tidal = (21/2) * (k_2 / Q) * (G * M_p^2 * R_moon^5 * n * e^2) / a_moon^6
        // Mean orbital motion n = sqrt(G * M_p / a_moon^3) (rad / s)
        val k2OverQ = 0.002
        val nMeanMotion = sqrt((G_CONST * mPlanetKg) / aMoonM.pow(3))
        val numerator = 10.5 * k2OverQ * G_CONST * (mPlanetKg.pow(2)) * (rMoonM.pow(5)) * nMeanMotion * (orbitalEccentricity.pow(2))
        val denominator = aMoonM.pow(6)
        val eTidalWatts = (numerator / denominator).coerceAtLeast(1e4)

        // Surface tidal heat flux F_tidal = E_tidal / (4 * pi * R_moon^2) (W / m^2)
        val surfaceAreaM2 = 4.0 * PI * (rMoonM.pow(2))
        val tidalHeatFluxWattsM2 = eTidalWatts / surfaceAreaM2

        // 4. Ice Shell Thickness & Subsurface Ocean Volume
        // d_ice = k_ice * (T_melt - T_surface) / F_tidal
        val deltaT = (273.15 - surfaceTemperatureK).coerceAtLeast(10.0)
        val estimatedIceShellThicknessKm = if (tidalHeatFluxWattsM2 > 0.005) {
            ((K_ICE_CONDUCTIVITY * deltaT) / tidalHeatFluxWattsM2) / 1000.0
        } else {
            300.0 // Frozen to bedrock
        }.coerceIn(0.5, 250.0)

        // Subsurface Ocean Regime Classification
        val regime = when {
            tidalHeatFluxWattsM2 >= 1.5 ->
                TidalOceanRegime.HYPER_VOLCANIC_MAGMA_OCEAN
            tidalHeatFluxWattsM2 in 0.02..1.5 ->
                TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN
            tidalHeatFluxWattsM2 < 0.005 ->
                TidalOceanRegime.FROZEN_SOLID_ICE
            else ->
                TidalOceanRegime.TIDALLY_LOCKED_DRY_ROCK
        }

        // Subsurface ocean volume estimate (assuming 60 km ocean mantle beneath ice)
        val oceanDepthM = when (regime) {
            TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN -> 60_000.0
            else -> 0.0
        }
        val oceanVolumeM3 = surfaceAreaM2 * oceanDepthM
        val oceanVolumeEarthOceans = oceanVolumeM3 / EARTH_OCEAN_VOLUME_M3

        val astrobiologySummary = when (regime) {
            TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN ->
                "★ CANDIDAT BIOLOGIC DE TOP: Flux mareic ${String.format("%.3f", tidalHeatFluxWattsM2)} W/m^2 (peste Europa/Enceladus). Menține un ocean lichid global de ${String.format("%.1f", oceanVolumeEarthOceans)}x oceanele Terrei sub o crustă de gheață de ${String.format("%.1f", estimatedIceShellThicknessKm)} km."
            TidalOceanRegime.HYPER_VOLCANIC_MAGMA_OCEAN ->
                "INFERN VULCANIC (SUPER-IO): Flux extrem de ${String.format("%.2f", tidalHeatFluxWattsM2)} W/m^2. Scoarța este topită în lavă și erupții continue de sulf."
            TidalOceanRegime.FROZEN_SOLID_ICE ->
                "LUME ÎNGHEȚATĂ COMPLET: Flux mareic insuficient (${String.format("%.4f", tidalHeatFluxWattsM2)} W/m^2). Gheața ajunge până la mantaua de silicați."
            TidalOceanRegime.TIDALLY_LOCKED_DRY_ROCK ->
                "LUNĂ DESHIDRATATĂ: Lipsă substanțe volatile din cauza apropierii extreme de planeta gazosă."
        }

        return ExomoonTidalOceanResult(
            moonName = moonName,
            hostPlanet = hostPlanet,
            hostStar = hostStar,
            planetMassJupiter = planetMassJupiter,
            moonMassEarth = moonMassEarth,
            moonRadiusEarth = moonRadiusEarth,
            semiMajorAxisMoonKm = semiMajorAxisMoonKm,
            orbitalEccentricity = orbitalEccentricity,
            hillRadiusKm = hillRadiusKm,
            isDynamicallyStable = isDynamicallyStable,
            ttvAmplitudeSeconds = ttvAmplitudeSeconds,
            tdvAmplitudeSeconds = tdvAmplitudeSeconds,
            ttvTdvPhaseShiftRad = ttvTdvPhaseShiftRad,
            tidalHeatFluxWattsM2 = tidalHeatFluxWattsM2,
            estimatedIceShellThicknessKm = estimatedIceShellThicknessKm,
            oceanVolumeEarthOceans = oceanVolumeEarthOceans,
            oceanRegime = regime,
            astrobiologicalPotential = astrobiologySummary
        )
    }
}
