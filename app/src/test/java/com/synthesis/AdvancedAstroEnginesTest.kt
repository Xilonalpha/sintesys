package com.synthesis

import com.synthesis.domain.model.exoplanet.AtmosphericMolecule
import com.synthesis.domain.model.exoplanet.ClimateRegime
import com.synthesis.engine.exoplanet.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class AdvancedAstroEnginesTest {

    private val bioEngine = BiosignatureTechnosignatureEngine()
    private val deblendEngine = DilutionDeBlenderEngine()
    private val stitcherEngine = MultiSectorHarmonicStitcherEngine()
    private val climateEngine = EnergyBalanceClimateEngine()
    private val radarEngine = TransitSkyRadarEngine()

    @Test
    fun testBiosignatureAtmosphericDisequilibrium() {
        println("=== TEST 1: BIOSIGNATURE ATMOSPHERIC CHEMICAL DISEQUILIBRIUM ===")
        val molecules = listOf(
            AtmosphericMolecule("CH4", "Methane", 15.0, 4.2, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("O2", "Molecular Oxygen", 200000.0, 5.1, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("DMS", "Dimethyl Sulfide", 18.0, 3.2, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("CO2", "Carbon Dioxide", 400.0, 7.0, isBiosignatureCandidate = false, isTechnosignatureCandidate = false)
        )

        val bio = bioEngine.evaluateBiosignatures(
            planetName = "TRAPPIST-1 e Bio-Candidate",
            hostStar = "TRAPPIST-1",
            equilibriumTempK = 250.0,
            molecules = molecules
        )

        println("Thermodynamic Disequilibrium (ΔG): ${bio.thermodynamicDisequilibriumMetric} kJ/mol")
        println("Biosignature Score: ${bio.biosignatureScore}%")
        println("Explanation: ${bio.biologicalExplanation}")

        assertTrue("Disequilibrium metric must be positive for CH4 + O2 pair", bio.thermodynamicDisequilibriumMetric > 10.0)
        assertTrue("Biosignature score should be >= 80% for canonical biomarker pair", bio.biosignatureScore >= 80.0)
        assertTrue("Explanation should highlight canonical biological pair", bio.biologicalExplanation.contains("CANONICAL"))
    }

    @Test
    fun testTechnosignatureMegastructureAsymmetry() {
        println("=== TEST 2: TECHNO-SIGNATURE & LIGHT CURVE ASYMMETRY ===")
        val tech = bioEngine.evaluateTechnosignatures(
            candidateName = "Boyajian Megastructure Benchmark",
            hostStar = "KIC 8462852",
            ingressDurationHours = 18.0,
            egressDurationHours = 6.0,
            depthVariancePpm = 20000.0,
            cfcPollutionSigma = 3.8
        )

        println("Asymmetry Index: ${tech.transitAsymmetryIndex}")
        println("Artificial Megastructure Probability: ${tech.artificialMegastructureProbability}%")
        println("Physical Description: ${tech.physicalDescription}")

        assertTrue("Asymmetry index must be steep (>0.40) for 18h ingress vs 6h egress", tech.transitAsymmetryIndex > 0.40)
        assertTrue("Artificial probability must be elevated (>50%)", tech.artificialMegastructureProbability > 50.0)
        assertNotNull("CFC pollution must be tracked", tech.cfcPollutionSigma)
    }

    @Test
    fun testGaiaDilutionDeBlenderRescuesFalsePositive() {
        println("=== TEST 3: GAIA DILUTION DE-BLENDER RESCUES FALSE POSITIVE ===")
        // A star with Gaia mag 10.45 has a neighbor at 4.2 arcsec with mag 10.82
        // Observed shallow depth: 340 ppm.
        val rescue = deblendEngine.deblendCandidate(
            candidateName = "TOI-1785.01",
            ticId = "300015239",
            originalNasaStatus = "False Positive (FP - Grazing Binary)",
            observedTransitDepthPpm = 340.0,
            targetStarMagGaia = 10.45,
            blendingStarMagGaia = 10.82,
            angularSeparationArcsec = 4.2,
            stellarRadiusSun = 0.88
        )

        println("Dilution Factor: ${rescue.dilutionCorrectionFactor}x")
        println("Observed Depth: ${rescue.observedTransitDepthPpm} ppm -> True Depth: ${rescue.trueTransitDepthPpm} ppm")
        println("Apparent Radius: ${rescue.apparentRadiusEarth} R_earth -> True Radius: ${rescue.trueCorrectedRadiusEarth} R_earth")
        println("Is Rescued to Rocky: ${rescue.isRescuedToRockyPlanet}")
        println("Verdict: ${rescue.rescueVerdict}")

        assertTrue("Dilution factor must be > 1.4x for close Gaia neighbor", rescue.dilutionCorrectionFactor > 1.4)
        assertTrue("True depth must be greater than observed depth", rescue.trueTransitDepthPpm > rescue.observedTransitDepthPpm)
        assertTrue("Candidate should be rescued to rocky planet regime (0.8 - 3.5 R_earth)", rescue.isRescuedToRockyPlanet)
    }

    @Test
    fun testHarmonicStitcherMultiYearBridge() {
        println("=== TEST 4: MULTI-SECTOR HARMONIC STITCHER ===")
        // Sector 14 (BTJD 1683.15) to Sector 44 (BTJD 2496.15) -> Delta T = 813.0 days
        val stitch = stitcherEngine.stitchHarmonics(
            candidateName = "TOI-4633.02",
            ticId = "100100827",
            epoch1Btjd = 1683.15,
            sector1 = "s0014",
            epoch2Btjd = 2496.15,
            sector2 = "s0044",
            stellarMassSun = 1.02,
            stellarRadiusSun = 1.05,
            stellarTeffK = 5790.0,
            monitoredGaps = listOf(Pair(1900.0, 1927.0), Pair(2150.0, 2177.0))
        )

        println("Delta Days: ${stitch.deltaDays} days")
        println("Best Solved Period: ${stitch.bestUniquePeriodDays} days")
        println("Semi-Major Axis: ${stitch.bestSemiMajorAxisAu} AU")
        println("Is in Habitable Zone: ${stitch.isHabitableZone}")

        assertEquals("Delta days should be exactly 813 days", 813.0, stitch.deltaDays, 0.01)
        assertTrue("Should compute multiple harmonic hypotheses", stitch.candidateHarmonics.size >= 10)
        assertTrue("Should identify ruled-out harmonics based on survey gaps", stitch.candidateHarmonics.any { it.isRuledOutBySurveyCoverage })
    }

    @Test
    fun testBudykoSellersClimateSimulator() {
        println("=== TEST 5: 1D BUDYKO-SELLERS CLIMATE SIMULATOR ===")
        // 1. Earth Baseline (S = 1.0, CO2 = 400 ppm, P = 1 bar)
        val earthClimate = climateEngine.simulateClimate(
            planetName = "Earth Baseline",
            stellarInsolationSolar = 1.0,
            co2LevelBar = 0.0004,
            surfacePressureBar = 1.0
        )

        println("Earth Analog Mean Temp: ${earthClimate.meanSurfaceTempC}°C")
        println("Earth Analog Polar Ice Line: ${earthClimate.polarIceLatitudeDeg}°")
        println("Earth Liquid Ocean: ${earthClimate.liquidOceanCoveragePercent}%")
        println("Earth Regime: ${earthClimate.climateRegime}")

        assertTrue("Earth analog mean temp should be temperate (5 to 25 °C)", earthClimate.meanSurfaceTempC in 5.0..25.0)
        assertTrue("Earth analog ice line should be temperate (38 to 80 °)", earthClimate.polarIceLatitudeDeg in 38.0..80.0)
        assertEquals("Earth analog regime should be TEMPERATE_EARTH_LIKE", ClimateRegime.TEMPERATE_EARTH_LIKE, earthClimate.climateRegime)

        // 2. Runaway Greenhouse (S = 1.4)
        val runaway = climateEngine.simulateClimate("Venus Analog", 1.45, 0.05, 10.0)
        println("Runaway Regime: ${runaway.climateRegime}, Temp: ${runaway.meanSurfaceTempC}°C")
        assertEquals(ClimateRegime.RUNAWAY_GREENHOUSE, runaway.climateRegime)

        // 3. Snowball Glaciation (S = 0.65)
        val snowball = climateEngine.simulateClimate("Frozen Ice World", 0.65, 0.0001, 0.5)
        println("Snowball Regime: ${snowball.climateRegime}, Temp: ${snowball.meanSurfaceTempC}°C")
        assertEquals(ClimateRegime.SNOWBALL_GLACIATED, snowball.climateRegime)
    }

    @Test
    fun testTransitSkyRadarCountdown() {
        println("=== TEST 6: TRANSIT SKY RADAR COUNTDOWN ===")
        val radar = radarEngine.computeNextTransit(
            candidateName = "TOI-700 e",
            ticId = "150428135",
            constellation = "Dorado",
            rightAscension = "06h 28m 23s",
            declination = "-55° 34′ 43″",
            apparentVisualMag = 13.1,
            periodDays = 27.81,
            transitDurationHours = 2.4,
            epochBtjd = 1500.24,
            transitDepthPpm = 860.0,
            currentBtjd = 3200.0
        )

        println("Next Transit Epoch: BTJD ${radar.nextTransitEpochBtjd}")
        println("Hours Until Ingress: ${radar.hoursUntilIngress} h")
        println("Feasibility: ${radar.amateurTelescopeFeasibility}")

        assertTrue("Next epoch must be >= current epoch", radar.nextTransitEpochBtjd >= 3200.0)
        assertTrue("Hours until ingress must be non-negative", radar.hoursUntilIngress >= 0.0)
        assertNotNull(radar.amateurTelescopeFeasibility)
    }

    @Test
    fun testDynamicTransitAlertCalculations() {
        println("=== TEST 7: DYNAMIC TRANSIT ALERT MESSAGE CALCULATIONS ===")
        val radar = radarEngine.computeNextTransit(
            candidateName = "TOI-1895.01",
            ticId = "384159646",
            constellation = "Cygnus",
            rightAscension = "19h 42m 12s",
            declination = "+41° 18′ 29″",
            apparentVisualMag = 9.75,
            periodDays = 194.5,
            transitDurationHours = 14.8,
            epochBtjd = 2390.412,
            transitDepthPpm = 4850.0,
            currentBtjd = 3200.0
        )

        // Verify dynamic values
        assertTrue("Hours until ingress must be dynamically calculated", radar.hoursUntilIngress > 0.0)
        assertEquals("Cygnus", radar.constellation)
        assertEquals("19h 42m 12s", radar.rightAscension)
        assertEquals("+41° 18′ 29″", radar.declination)
    }
}
