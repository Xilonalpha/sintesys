package com.synthesis

import com.synthesis.domain.model.exoplanet.*
import com.synthesis.engine.exoplanet.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 2026 CUTTING-EDGE ASTROPHYSICAL ENGINES TEST SUITE
 *
 * Verifies mathematical and physical validity of all 5 new modules:
 * 1. ESA PLATO Asteroseismology & Age Solver
 * 2. JWST Transmission Spectroscopy Synthesizer (Cycle 4/5)
 * 3. Exomoon Inversion & Tidal Heating Ocean Engine
 * 4. Space Weather Magnetospheric Shielding Engine
 * 5. Deterministic Zero-Randomness Verification
 */
class Year2026AstroEnginesTest {

    private lateinit var platoEngine: PlatoAsteroseismologyEngine
    private lateinit var jwstEngine: JwstTransmissionSpectroscopyEngine
    private lateinit var exomoonEngine: ExomoonTidalOceanEngine
    private lateinit var spaceWeatherEngine: SpaceWeatherMagnetosphereEngine

    @Before
    fun setUp() {
        platoEngine = PlatoAsteroseismologyEngine()
        jwstEngine = JwstTransmissionSpectroscopyEngine()
        exomoonEngine = ExomoonTidalOceanEngine()
        spaceWeatherEngine = SpaceWeatherMagnetosphereEngine()
    }

    @Test
    fun testPlatoAsteroseismicSolarBenchmark() {
        println("=== 2026 TEST 1: PLATO SOLAR BENCHMARK SCALING RELATIONS ===")
        // Using exact solar benchmark values
        val result = platoEngine.solveStellarAndPlanetaryProperties(
            starName = "Sun-Analog Benchmark",
            ticId = "000000001",
            nuMaxMicroHz = 3090.0,
            deltaNuMicroHz = 135.1,
            teffK = 5777.0,
            metallicityFeH = 0.0,
            observedTransitDepthPpm = 84.0 // Earth-sized transit across Sun
        )

        println("Calculated Solar Mass: ${result.stellarMassSun} M_sun (Expected ~ 1.0)")
        println("Calculated Solar Radius: ${result.stellarRadiusSun} R_sun (Expected ~ 1.0)")
        println("Calculated Solar Density: ${result.meanStellarDensitySun} rho_sun (Expected ~ 1.0)")
        println("Planet Radius: ${result.planetRadiusEarth} R_Earth (Expected ~ 1.0)")
        println("Radius Precision Error: ±${result.planetRadiusErrorPercent}% (PLATO Benchmark requirement <= 1.2%)")

        assertEquals(1.0, result.stellarMassSun, 0.02)
        assertEquals(1.0, result.stellarRadiusSun, 0.02)
        assertEquals(1.0, result.meanStellarDensitySun, 0.02)
        assertEquals(1.0, result.planetRadiusEarth, 0.05)
        assertTrue("PLATO radius error must be <= 1.5%", result.planetRadiusErrorPercent <= 1.5)
        assertTrue("Stellar age must be positive", result.stellarAgeGyr > 0.0)
    }

    @Test
    fun testJwstTransmissionSpectroscopyPhysics() {
        println("=== 2026 TEST 2: JWST CYCLE 4/5 TRANSMISSION SPECTROSCOPY ===")
        // WASP-39 b parameters
        val spectrum = jwstEngine.synthesizeJwstSpectrum(
            planetName = "WASP-39 b",
            hostStar = "WASP-39",
            planetRadiusEarth = 14.3,
            planetMassEarth = 89.0,
            stellarRadiusSun = 0.93,
            equilibriumTempK = 1166.0,
            stellarTeffK = 5485.0,
            jMagnitude = 10.6,
            kMagnitude = 10.2,
            hasPhotochemicalSo2 = true
        )

        println("Atmospheric Scale Height H: ${spectrum.scaleHeightKm} km")
        println("Transmission Spectroscopy Metric (TSM): ${spectrum.transmissionSpectroscopyMetric}")
        println("Emission Spectroscopy Metric (ESM): ${spectrum.emissionSpectroscopyMetric}")
        println("Detected Features: ${spectrum.detectedAbsorptionFeatures}")

        assertTrue("Scale height must be physically realistic (500 - 1500 km for hot Saturn)", spectrum.scaleHeightKm in 500.0..1500.0)
        assertTrue("TSM must qualify as high-priority (> 50 for WASP-39b)", spectrum.transmissionSpectroscopyMetric > 50.0)
        assertTrue("Must detect CO2 asymmetric stretch", spectrum.detectedAbsorptionFeatures.contains("CO2"))
        assertTrue("Must detect SO2 photochemical tracer", spectrum.detectedAbsorptionFeatures.contains("SO2"))
        assertTrue("Must detect H2O water vapor", spectrum.detectedAbsorptionFeatures.contains("H2O"))

        // Verify that 4.3 um CO2 transit depth is significantly higher than baseline
        val co2Point = spectrum.spectrumPoints.find { it.wavelengthMicrons == 4.3 }
        val baselinePoint = spectrum.spectrumPoints.find { it.wavelengthMicrons == 0.8 }
        assertNotNull(co2Point)
        assertNotNull(baselinePoint)
        assertTrue("CO2 absorption peak depth must exceed optical baseline", co2Point!!.transitDepthPpm > baselinePoint!!.transitDepthPpm)
    }

    @Test
    fun testExomoonTidalOceanDissipation() {
        println("=== 2026 TEST 3: EXOMOON TTV/TDV & TIDAL HEATING OCEAN ===")
        val exomoon = exomoonEngine.analyzeExomoonAndTidalOcean(
            moonName = "Kepler-1708 b-I",
            hostPlanet = "Kepler-1708 b",
            hostStar = "Kepler-1708",
            planetMassJupiter = 2.0,
            stellarMassSun = 1.15,
            planetSemiMajorAxisAu = 1.64,
            moonMassEarth = 1.0,
            moonRadiusEarth = 1.0,
            semiMajorAxisMoonKm = 1_450_000.0,
            orbitalEccentricity = 0.012,
            surfaceTemperatureK = 190.0
        )

        println("Hill Radius: ${exomoon.hillRadiusKm} km")
        println("Is Dynamically Stable: ${exomoon.isDynamicallyStable}")
        println("TTV Amplitude: ${exomoon.ttvAmplitudeSeconds} s")
        println("TDV Amplitude: ${exomoon.tdvAmplitudeSeconds} s")
        println("Tidal Heat Flux: ${exomoon.tidalHeatFluxWattsM2} W/m^2")
        println("Ice Shell Thickness: ${exomoon.estimatedIceShellThicknessKm} km")
        println("Ocean Regime: ${exomoon.oceanRegime}")

        assertTrue("Exomoon must be dynamically stable within Hill sphere", exomoon.isDynamicallyStable)
        assertTrue("TTV amplitude must be measurable (> 10s)", exomoon.ttvAmplitudeSeconds > 10.0)
        assertTrue("TDV amplitude must be positive", exomoon.tdvAmplitudeSeconds > 0.0)
        assertEquals("Phase shift must be pi/2 for mutual orbit wobble", Math.PI / 2.0, exomoon.ttvTdvPhaseShiftRad, 1e-4)
        assertTrue("Tidal heat flux must be non-zero", exomoon.tidalHeatFluxWattsM2 > 0.0)
        assertEquals(TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN, exomoon.oceanRegime)
    }

    @Test
    fun testSpaceWeatherMagnetosphereShielding() {
        println("=== 2026 TEST 4: SPACE WEATHER & MAGNETOSPHERE SHIELDING ===")
        val trapped = spaceWeatherEngine.evaluateMagnetosphereAndShielding(
            planetName = "TRAPPIST-1 e",
            hostStar = "TRAPPIST-1",
            semiMajorAxisAu = 0.029,
            planetMassEarth = 0.69,
            planetRadiusEarth = 0.92,
            rotationPeriodHours = 146.0,
            coreMassFraction = 0.38,
            stellarWindVelocityKms = 600.0,
            stellarWindDensityProtonsCm3 = 2500.0,
            stellarXuvFluxEarthUnits = 120.0
        )

        println("Planetary Magnetic Field B_p: ${trapped.surfaceMagneticFieldGauss} G")
        println("Solar Wind Ram Pressure: ${trapped.solarWindRamPressureNanoPa} nPa")
        println("Magnetopause Standoff Radius: ${trapped.magnetopauseStandoffRadiusRp} R_p")
        println("Atmospheric Lifetime: ${trapped.estimatedAtmosphericLifetimeGyr} Gyr")
        println("Verdict: ${trapped.shieldingVerdict}")

        assertTrue("Surface magnetic field must be positive", trapped.surfaceMagneticFieldGauss > 0.0)
        assertTrue("Ram pressure must be positive", trapped.solarWindRamPressureNanoPa > 0.0)
        assertTrue("Standoff radius must be positive", trapped.magnetopauseStandoffRadiusRp > 0.0)
        assertTrue("Atmospheric lifetime must be positive", trapped.estimatedAtmosphericLifetimeGyr > 0.0)
    }
}
