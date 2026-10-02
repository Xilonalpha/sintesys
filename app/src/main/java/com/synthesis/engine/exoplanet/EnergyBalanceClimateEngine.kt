package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.ClimateRegime
import com.synthesis.domain.model.exoplanet.ClimateSimulationResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * 1D Budyko-Sellers Energy Balance Climate Simulator
 *
 * Implements Budyko (1969) / Sellers (1969) / Kopparapu et al. (2013)
 * energy balance equations with ice-albedo feedback and runaway greenhouse bifurcation.
 */
@Singleton
class EnergyBalanceClimateEngine @Inject constructor() {

    private val SOLAR_CONSTANT = 1361.0 // W/m^2 (Earth insolation S_0)

    fun simulateClimate(
        planetName: String,
        stellarInsolationSolar: Double,
        co2LevelBar: Double,
        surfacePressureBar: Double,
        baseOceanAlbedo: Double = 0.22,
        iceAlbedo: Double = 0.65
    ): ClimateSimulationResult {
        // 1. Greenhouse Warming Model:
        // Delta T_GH ~ 5.35 * ln(C/C_0) + pressure broadening term
        val logCo2 = ln((co2LevelBar / 0.0004).coerceAtLeast(0.001))
        val greenhouseWarmingK = (5.35 * logCo2 * 0.8 + 33.0 * (surfacePressureBar).pow(0.3)).coerceIn(5.0, 180.0)

        // 2. Kopparapu et al. (2013) Runaway and Maximum Greenhouse Insolation Limits:
        val runawayThreshold = 1.12 * (1.0 + 0.05 * ln(surfacePressureBar.coerceAtLeast(0.1)))
        val snowballThreshold = 0.82 - 0.10 * ln((co2LevelBar / 0.0004).coerceAtLeast(1e-4))

        // 3. Iterative Energy Balance with Ice-Albedo Feedback
        // Start assuming temperate condition (ice line at 65 deg)
        var iceLatDeg = 65.0
        var meanTempK = 288.0

        for (iter in 0 until 5) {
            val iceFraction = (1.0 - sin(Math.toRadians(iceLatDeg))).coerceIn(0.0, 1.0)
            val effectiveAlbedo = (1.0 - iceFraction) * baseOceanAlbedo + iceFraction * iceAlbedo

            val absorbedFlux = (stellarInsolationSolar * SOLAR_CONSTANT * (1.0 - effectiveAlbedo)) / 4.0
            // Stefan-Boltzmann balance with greenhouse back-radiation:
            // sigma * T^4_eff = absorbedFlux, T_surface = T_eff + Delta T_GH
            val teffK = (absorbedFlux / 5.670374e-8).pow(0.25)
            meanTempK = teffK + (greenhouseWarmingK * (1.0 - effectiveAlbedo * 0.4))

            // Solve new ice line where local temperature crosses 263.15 K (-10 deg C)
            // T(phi) ~ T_mean + Delta T_eq_pole * (P2(sin phi))
            val equatorToPoleDelta = 35.0
            val targetLatSin = sqrt(((meanTempK - 263.15) / equatorToPoleDelta).coerceIn(0.0, 1.0))
            iceLatDeg = Math.toDegrees(asin(targetLatSin)).coerceIn(0.0, 90.0)
        }

        // Catastrophic Bifurcation Check (Snowball instability)
        // If ice advances past ~30-35 deg latitude, positive feedback freezes the entire equator
        val isSnowball = stellarInsolationSolar < snowballThreshold || iceLatDeg < 32.0
        val isRunaway = stellarInsolationSolar > runawayThreshold || meanTempK > 350.0

        val finalIceLat = if (isSnowball) 0.0 else if (isRunaway) 90.0 else iceLatDeg
        val finalTempC = if (isSnowball) -52.0 else if (isRunaway) 120.0 + (stellarInsolationSolar - runawayThreshold) * 150.0 else (meanTempK - 273.15)
        val liquidOceanPercent = if (isSnowball) 0.0 else if (isRunaway) 0.0 else (sin(Math.toRadians(finalIceLat)) * 100.0).coerceIn(5.0, 98.0)

        val regime = when {
            isRunaway -> ClimateRegime.RUNAWAY_GREENHOUSE
            isSnowball -> ClimateRegime.SNOWBALL_GLACIATED
            finalTempC in 15.0..28.0 && finalIceLat > 75.0 -> ClimateRegime.TROPICAL_SUPER_HABITABLE
            else -> ClimateRegime.TEMPERATE_EARTH_LIKE
        }

        val summary = when (regime) {
            ClimateRegime.RUNAWAY_GREENHOUSE -> "Solar insolation exceeds moist greenhouse limit (${String.format("%.2f", runawayThreshold)} S☉). Surface oceans evaporate, creating a supercritical steam atmosphere."
            ClimateRegime.SNOWBALL_GLACIATED -> "Runaway ice-albedo catastrophe: ice sheets breached critical latitude (<32°), plunging the planet into permanent global glaciation."
            ClimateRegime.TROPICAL_SUPER_HABITABLE -> "Optimal climate: Ice-free polar regions (${String.format("%.0f", finalIceLat)}°), surface oceans covering ~${liquidOceanPercent.roundToInt()}% of the globe with average temperature of ${String.format("%.1f", finalTempC)}°C."
            ClimateRegime.TEMPERATE_EARTH_LIKE -> "Stable Earth-like equilibrium: Polar ice caps begin at ${String.format("%.0f", finalIceLat)}° latitude; temperate oceans cover ~${liquidOceanPercent.roundToInt()}% of the planetary surface."
        }

        return ClimateSimulationResult(
            planetName = planetName,
            stellarInsolationSolar = stellarInsolationSolar,
            co2LevelBar = co2LevelBar,
            surfacePressureBar = surfacePressureBar,
            bondAlbedo = (1.0 - (liquidOceanPercent / 100.0)) * iceAlbedo + (liquidOceanPercent / 100.0) * baseOceanAlbedo,
            greenhouseWarmingK = greenhouseWarmingK,
            meanSurfaceTempC = finalTempC,
            polarIceLatitudeDeg = finalIceLat,
            liquidOceanCoveragePercent = liquidOceanPercent,
            climateRegime = regime,
            runawayThresholdInsolation = runawayThreshold,
            snowballThresholdInsolation = snowballThreshold,
            climateSummary = summary
        )
    }
}
