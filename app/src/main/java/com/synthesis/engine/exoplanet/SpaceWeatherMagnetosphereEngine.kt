package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.MagnetosphereShieldingResult
import com.synthesis.domain.model.exoplanet.ShieldingVerdict
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Space Weather & Magnetospheric Shielding Engine (2026 Edition)
 *
 * Evaluates whether terrestrial exoplanets orbiting active M-dwarf stars
 * retain their atmospheres or get stripped by stellar CMEs and UV flares.
 *
 * Implements:
 * 1. Christensen et al. (2009, Nature 457:167) convective core dynamo scaling relations.
 * 2. Magnetopause standoff radius under stellar wind ram pressure (Vidotto et al. 2013).
 * 3. Owen & Wu (2013, ApJ 775:105) energy-limited XUV atmospheric photoevaporation.
 */
@Singleton
class SpaceWeatherMagnetosphereEngine @Inject constructor() {

    companion object {
        const val MU_0 = 4.0 * PI * 1e-7 // H / m (permeability of vacuum)
        const val PROTON_MASS_KG = 1.6726e-27
        const val G_CONST = 6.67430e-11
        const val M_EARTH_KG = 5.972e24
        const val R_EARTH_M = 6.371e6
        const val B_EARTH_EQUATORIAL_TESLA = 3.1e-5 // ~ 0.31 Gauss
        const val EARTH_DIPOLE_MOMENT_AM2 = 7.94e22 // A * m^2
    }

    /**
     * Solves planetary magnetosphere standoff radius and atmospheric stripping rate.
     */
    fun evaluateMagnetosphereAndShielding(
        planetName: String,
        hostStar: String,
        semiMajorAxisAu: Double,
        planetMassEarth: Double,
        planetRadiusEarth: Double,
        rotationPeriodHours: Double = 24.0,
        coreMassFraction: Double = 0.32, // Earth core mass fraction ~ 32%
        stellarWindVelocityKms: Double = 450.0,
        stellarWindDensityProtonsCm3: Double = 1000.0, // Active M-dwarf wind is 10-100x denser than Sun at 1 AU
        stellarXuvFluxEarthUnits: Double = 50.0 // Active flare stars produce 10-500x Earth XUV
    ): MagnetosphereShieldingResult {
        val mPlanetKg = planetMassEarth * M_EARTH_KG
        val rPlanetM = planetRadiusEarth * R_EARTH_M

        // 1. Planetary Convective Dynamo Magnetic Moment (Christensen et al. 2009)
        // M_dipole / M_dipole_earth = (M_core / M_core_earth)^(1/2) * (R_p / R_earth)^3 * (Omega / Omega_earth)^(1/2)
        val coreRatio = (coreMassFraction / 0.32) * planetMassEarth
        val rotationRatio = (24.0 / rotationPeriodHours).coerceAtLeast(0.01) // Tidally locked planets rotate slower
        val dipoleMomentEarthUnits = sqrt(coreRatio.coerceAtLeast(0.01)) * (planetRadiusEarth.pow(3)) * sqrt(rotationRatio)

        val dipoleMomentAm2 = dipoleMomentEarthUnits * EARTH_DIPOLE_MOMENT_AM2

        // Equatorial surface magnetic field B_p = (mu_0 * M_dipole) / (4 * pi * R_p^3)
        val bSurfaceTesla = (MU_0 * dipoleMomentAm2) / (4.0 * PI * (rPlanetM.pow(3)))
        val bSurfaceGauss = bSurfaceTesla * 10_000.0

        // 2. Stellar Wind Dynamic Ram Pressure P_ram = rho * v^2
        // Density rho = n_p * m_p (kg / m^3)
        val densityKgM3 = (stellarWindDensityProtonsCm3 * 1e6) * PROTON_MASS_KG
        val velocityMS = stellarWindVelocityKms * 1000.0
        val pRamPa = densityKgM3 * (velocityMS.pow(2))
        val pRamNanoPa = pRamPa * 1e9

        // 3. Magnetopause Standoff Radius (Vidotto et al. 2013)
        // R_mp = R_p * [ B_p^2 / (2 * mu_0 * P_ram) ]^(1/6)
        val magneticPressurePa = (bSurfaceTesla.pow(2)) / (2.0 * MU_0)
        val standoffRatio = (magneticPressurePa / pRamPa).pow(1.0 / 6.0)
        val rStandoffRp = standoffRatio.coerceIn(0.5, 35.0)

        // 4. Atmospheric Photoevaporation / Stripping Rate (Owen & Wu 2013)
        // M_dot = (epsilon * pi * R_p^3 * F_XUV) / (G * M_p)
        // If magnetopause is large, it shields the atmosphere from coronal mass ejection erosion by factor (Rp / R_mp)^2
        val magneticShieldingFactor = if (rStandoffRp > 1.0) {
            (1.0 / rStandoffRp).pow(1.5).coerceIn(0.05, 1.0)
        } else {
            1.8 // Unshielded surface direct ion stripping
        }

        val fXuvWm2 = stellarXuvFluxEarthUnits * 0.005 // Earth XUV ~ 5 mW/m^2
        val efficiency = 0.12 // 12% photoevaporation efficiency
        val unshieldedLossKgS = (efficiency * PI * (rPlanetM.pow(3)) * fXuvWm2) / (G_CONST * mPlanetKg)
        val actualLossKgS = unshieldedLossKgS * magneticShieldingFactor

        // Typical terrestrial atmosphere mass: ~ 5.1e18 kg (Earth) * planetMass
        val initialAtmosphereMassKg = 5.1e18 * planetMassEarth
        val lossPerYearKg = actualLossKgS * 3.15576e7
        val atmosphericLifetimeYears = initialAtmosphereMassKg / lossPerYearKg
        val atmosphericLifetimeGyr = (atmosphericLifetimeYears / 1e9).coerceIn(0.01, 100.0)

        val verdict = when {
            rStandoffRp >= 2.0 && atmosphericLifetimeGyr >= 4.0 ->
                ShieldingVerdict.MAG_SHIELDED_HABITABLE
            rStandoffRp in 1.0..2.0 || atmosphericLifetimeGyr in 1.0..4.0 ->
                ShieldingVerdict.CRITICAL_STRIPPING_WARNING
            else ->
                ShieldingVerdict.ATMOSPHERE_EVAPORATED_AIRLESS_ROCK
        }

        val analysis = when (verdict) {
            ShieldingVerdict.MAG_SHIELDED_HABITABLE ->
                "★ CÂMP MAGNETIC ROBUST: Câmp ecuatorial ${String.format("%.2f", bSurfaceGauss)} Gauss. Raza magnetopauzei este la ${String.format("%.1f", rStandoffRp)} R_p (dincolo de atmosferă). Rata de pierdere de masă: ${String.format("%.1f", actualLossKgS)} kg/s. Atmosfera este protejată pe o durată de ${String.format("%.1f", atmosphericLifetimeGyr)} Gyr."
            ShieldingVerdict.CRITICAL_STRIPPING_WARNING ->
                "⚠️ COMPRESIE SEVERĂ A MAGNETOPAUZEI: Vântul stelar comprimă scutul la ${String.format("%.1f", rStandoffRp)} R_p. Timp de supraviețuire estimat al atmosferei: ${String.format("%.2f", atmosphericLifetimeGyr)} Gyr. Risc ridicat de uscare și pierdere a apei."
            ShieldingVerdict.ATMOSPHERE_EVAPORATED_AIRLESS_ROCK ->
                "SCUT STRĂPUNS (ROTCĂ STERILĂ): Presiunea vântului stelar (${String.format("%.2f", pRamNanoPa)} nPa) a comprimat magnetosfera sub suprafață (${String.format("%.2f", rStandoffRp)} R_p). Atmosfera originală a fost spulberată complet."
        }

        return MagnetosphereShieldingResult(
            planetName = planetName,
            hostStar = hostStar,
            semiMajorAxisAu = semiMajorAxisAu,
            stellarWindVelocityKms = stellarWindVelocityKms,
            stellarWindDensityProtonsCm3 = stellarWindDensityProtonsCm3,
            stellarXuvFluxEarthUnits = stellarXuvFluxEarthUnits,
            coreMassFraction = coreMassFraction,
            rotationPeriodHours = rotationPeriodHours,
            dipoleMagneticMomentEarthUnits = dipoleMomentEarthUnits,
            surfaceMagneticFieldGauss = bSurfaceGauss,
            solarWindRamPressureNanoPa = pRamNanoPa,
            magnetopauseStandoffRadiusRp = rStandoffRp,
            atmosphericMassLossRateKgS = actualLossKgS,
            estimatedAtmosphericLifetimeGyr = atmosphericLifetimeGyr,
            shieldingVerdict = verdict,
            spaceWeatherAnalysis = analysis
        )
    }
}
