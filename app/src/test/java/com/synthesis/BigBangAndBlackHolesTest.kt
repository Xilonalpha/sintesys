package com.synthesis

import com.synthesis.engine.cosmology.BigBangCosmologyEngine
import com.synthesis.engine.cosmology.BlackHoleRelativityEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BigBangAndBlackHolesTest {

    private lateinit var bigBangEngine: BigBangCosmologyEngine
    private lateinit var blackHoleEngine: BlackHoleRelativityEngine

    @Before
    fun setUp() {
        bigBangEngine = BigBangCosmologyEngine()
        blackHoleEngine = BlackHoleRelativityEngine()
    }

    @Test
    fun testBigBangStandardLcdmCosmicAge() {
        val result = bigBangEngine.simulateCosmicExpansion(
            h0 = 67.4,
            omegaM = 0.315,
            omegaLambda = 0.685
        )

        println("=== TEST COSMIC EXPANSION LCDM ===")
        println("Cosmic Age: ${result.cosmicAgeGyr} Gyr (Expected ~ 13.8 Gyr)")
        println("Deceleration Parameter q0: ${result.decelerationParameterQ0} (Expected < 0, accelerated expansion)")
        println("Sound Horizon rs: ${result.soundHorizonMpc} Mpc (Expected ~ 147 Mpc)")

        assertTrue("Cosmic age must be between 13.5 and 14.2 Gyr", result.cosmicAgeGyr in 13.5..14.2)
        assertTrue("q0 must be negative indicating dark energy acceleration", result.decelerationParameterQ0 < 0.0)
        assertEquals(-0.53, result.decelerationParameterQ0, 0.05)
        assertTrue("Sound horizon must be close to 147 Mpc", result.soundHorizonMpc in 140.0..155.0)
        assertEquals(2.7255, result.cmbTemperatureK, 0.001)
        assertTrue("Expansion trajectory must be populated", result.expansionTrajectory.isNotEmpty())
    }

    @Test
    fun testBigBangNucleosynthesisAbundances() {
        val epochs = bigBangEngine.getCosmicEpochs()
        assertTrue("Cosmic epochs timeline must contain key epochs", epochs.size >= 8)

        val bbnEpoch = epochs.first { it.epochName.contains("Nucleosinteza") }
        assertEquals(1.0e9, bbnEpoch.temperatureKelvin, 1e8)

        val result = bigBangEngine.simulateCosmicExpansion()
        assertEquals(0.75, result.bbnAbundances.hydrogenFraction, 0.02)
        assertEquals(0.247, result.bbnAbundances.helium4MassFractionYp, 0.01)
        assertTrue("Deuterium abundance must be ~ 2.5e-5", result.bbnAbundances.deuteriumAbundanceRatioDoverH in 2.0e-5..3.0e-5)
    }

    @Test
    fun testSagittariusAStarEventHorizonAndEhtShadow() {
        val targets = blackHoleEngine.getBenchmarkBlackHoles()
        val sgrA = targets.first { it.id == "sagittarius_a_star" }

        val profile = blackHoleEngine.computeRelativisticProfile(sgrA)

        println("=== TEST SAGITTARIUS A* EHT SHADOW ===")
        println("Schwarzschild Radius Rs: ${profile.schwarzschildRadiusKm} km")
        println("Kerr Horizon r+: ${profile.eventHorizonRadiusKm} km")
        println("EHT Angular Shadow: ${profile.shadowDiameterMicroArcsec} microarcsec (Observed EHT: ~52 uas)")

        // Rs ~ 2 * 1.477 km * 4.154e6 ~ 1.22e7 km
        assertTrue("Rs must be approx 12.2 million km", profile.schwarzschildRadiusKm in 1.1e7..1.35e7)

        // For spinning Kerr black hole, r+ < Rs
        assertTrue("Kerr event horizon r+ must be smaller than Schwarzschild radius Rs", profile.eventHorizonRadiusKm < profile.schwarzschildRadiusKm)

        // EHT measured angular shadow of Sgr A* is 51.8 +- 2.3 microarcsec
        assertTrue("Angular shadow on sky must match EHT measurement ~52 uas", profile.shadowDiameterMicroArcsec in 45.0..60.0)
    }

    @Test
    fun testM87BlackHoleRelativisticShadow() {
        val targets = blackHoleEngine.getBenchmarkBlackHoles()
        val m87 = targets.first { it.id == "m87_star" }

        val profile = blackHoleEngine.computeRelativisticProfile(m87)

        println("=== TEST M87* EHT SHADOW ===")
        println("M87* Mass: ${m87.massSolar} M_sun")
        println("EHT Shadow Diameter: ${profile.shadowDiameterMicroArcsec} microarcsec (Observed EHT 2019: ~42 uas)")

        // EHT 2019 measured shadow of M87* is 42 +- 3 microarcsec
        assertTrue("M87 shadow must be in 36..48 microarcsec range", profile.shadowDiameterMicroArcsec in 36.0..48.0)
        assertTrue("Lorentz factor of jet must be high", (m87.jetLorentzFactor ?: 0.0) > 5.0)
    }

    @Test
    fun testKerrErgosphereAndPenroseExtraction() {
        val targets = blackHoleEngine.getBenchmarkBlackHoles()
        val cygnusX1 = targets.first { it.id == "cygnus_x1" }

        val profile = blackHoleEngine.computeRelativisticProfile(cygnusX1)

        println("=== TEST CYGNUS X-1 KERR SPIN & PENROSE PROCESS ===")
        println("Spin a*: ${cygnusX1.spinParameterAStar}")
        println("Penrose Energy Efficiency: ${profile.penroseEnergyEfficiencyPercent}%")
        println("ISCO Orbital Velocity: ${profile.iscoVelocityFractionC} c")

        // Penrose efficiency for a* ~ 0.98 is > 20%
        assertTrue("Penrose efficiency for near-extremal Kerr must exceed 18%", profile.penroseEnergyEfficiencyPercent > 18.0)
        assertTrue("ISCO radius must be significantly smaller than non-spinning 6 Rg", profile.iscoRadiusKm < (3.0 * profile.schwarzschildRadiusKm))
        assertTrue("Orbital velocity at ISCO must be relativistic (> 0.4 c)", profile.iscoVelocityFractionC > 0.4)
    }
}
