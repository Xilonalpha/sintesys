package com.synthesis

import com.synthesis.domain.model.exoplanet.TtvObservedTransit
import com.synthesis.engine.exoplanet.AtmosphericTsmEngine
import com.synthesis.engine.exoplanet.ExoplanetCandidateFinder
import com.synthesis.engine.exoplanet.MonoTransitHunterEngine
import com.synthesis.engine.exoplanet.TtvInversionEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DeepHuntEnginesTest {

    private val finder = ExoplanetCandidateFinder()
    private val monoHunter = MonoTransitHunterEngine(finder)
    private val ttvEngine = TtvInversionEngine()
    private val tsmEngine = AtmosphericTsmEngine(finder)

    @Test
    fun testEarthSunMonoTransitInversion() {
        println("=== TEST 1: EARTH-SUN MONO-TRANSIT CIRCULAR INVERSION ===")
        // Central transit of an Earth-analog across a Sun-analog:
        // Tau = 13.0 hours, Depth = 84 ppm, M* = 1.0 M_sun, R* = 1.0 R_sun, Teff = 5778 K
        val earthMono = monoHunter.analyzeSingleTransit(
            ticId = "000000001",
            candidateName = "Earth-Sun Blindspot Benchmark",
            sector = "s0001",
            epochBtjd = 1000.0,
            transitDurationHours = 13.0,
            transitDepthPpm = 84.0,
            stellarTeffK = 5778.0,
            stellarRadiusSun = 1.0,
            stellarMassSun = 1.0,
            stellarDistancePc = 10.0,
            apparentMagV = 4.8
        )

        println("Calculated Semi-Major Axis: ${earthMono.semiMajorAxisAu} AU")
        println("Calculated Circular Period: ${earthMono.circularEstimatedPeriodDays} days")
        println("Calculated Planetary Radius: ${earthMono.planetRadiusEarth} R_earth")
        println("Insolation: ${earthMono.stellarInsolationEarth} S_earth")
        println("Equilibrium Temp: ${earthMono.equilibriumTempK} K")
        println("Is Habitable Zone: ${earthMono.isHabitableZone}")

        // Earth orbits at 1.0 AU in 365.25 days with R = 1.0 R_earth
        assertTrue("Semi-major axis should be ~1.0 AU", abs(earthMono.semiMajorAxisAu - 1.0) < 0.15)
        assertTrue("Estimated period should be ~365 days", abs(earthMono.circularEstimatedPeriodDays - 365.0) < 50.0)
        assertTrue("Planetary radius should be ~1.0 R_earth", abs(earthMono.planetRadiusEarth - 1.0) < 0.1)
        assertTrue("Earth analog must be in circumstellar Habitable Zone", earthMono.isHabitableZone)
        assertNotNull("Earth Similarity Index must be calculated", earthMono.earthSimilarityIndex)
    }

    @Test
    fun testTtvGravitationalCompanionInversion() {
        println("=== TEST 2: TTV GRAVITATIONAL COMPANION INVERSION (TOI-216) ===")
        val transits = listOf(
            TtvObservedTransit(0, 1325.321, 0.0, 1.2),
            TtvObservedTransit(1, 1342.450, 12.8, 1.4),
            TtvObservedTransit(2, 1359.605, 34.2, 1.3),
            TtvObservedTransit(3, 1376.790, 68.5, 1.5),
            TtvObservedTransit(4, 1393.990, 85.1, 1.6),
            TtvObservedTransit(5, 1411.165, 71.4, 1.4),
            TtvObservedTransit(6, 1428.310, 32.0, 1.3),
            TtvObservedTransit(7, 1445.420, -14.6, 1.5)
        )

        val analysis = ttvEngine.analyzeTtvResiduals(
            candidateName = "TOI-216.01",
            primaryPeriodDays = 17.085,
            primaryRadiusEarth = 8.2,
            stellarMassSun = 0.77,
            stellarRadiusSun = 0.75,
            stellarTeffK = 5026.0,
            observedTransits = transits
        )

        println("TTV Amplitude: ±${analysis.ttvAmplitudeMinutes} min")
        println("Inverted Companion Mass: ${analysis.estimatedCompanionMassEarth} M_earth")
        println("Companion Period: ${analysis.estimatedCompanionPeriodDays} days")
        println("Resonance: ${analysis.resonanceRatio}")
        println("Confidence: ${analysis.confidenceSigma}σ")

        assertTrue("TTV amplitude must be detected (>30 min)", analysis.ttvAmplitudeMinutes > 30.0)
        assertTrue("Inverted companion mass must be positive and physical", analysis.estimatedCompanionMassEarth > 1.0)
        assertTrue("Confidence sigma should be high (>4σ)", analysis.confidenceSigma >= 4.0)
    }

    @Test
    fun testAtmosphericTsmEsmEngine() {
        println("=== TEST 3: ATMOSPHERIC TSM & JWST SUITABILITY RANKING ===")
        // TOI-1231 b (Cool Sub-Neptune: Rp = 3.65 R_earth, Mp = 15.4 M_earth, J = 8.88 mag)
        val score = tsmEngine.calculateAtmosphericScore(
            candidateName = "TOI-1231.01",
            ticId = "447061717",
            planetRadiusEarth = 3.65,
            measuredMassEarth = 15.4,
            equilibriumTempK = 330.0,
            stellarRadiusSun = 0.47,
            jMagnitude = 8.88,
            kMagnitude = 8.07
        )

        println("Candidate: ${score.candidateName}")
        println("TSM Score: ${score.tsmScore}")
        println("ESM Score: ${score.esmScore}")
        println("Scale Height (H): ${score.atmosphericScaleHeightKm} km")
        println("Atmospheric Feature Depth: ${score.transitFeatureAmplitudePpm} ppm")
        println("Recommendation: ${score.jwstRecommendation}")

        assertTrue("TSM score must be high (>80) for TOI-1231 benchmark", score.tsmScore >= 80.0)
        assertTrue("Scale height must be positive and physically realistic", score.atmosphericScaleHeightKm in 50.0..2500.0)
        assertTrue("Feature amplitude must be measurable (>50 ppm)", score.transitFeatureAmplitudePpm > 50.0)
        assertTrue("Recommendation must qualify as Tier 1 Gold Target", score.jwstRecommendation.contains("GOLD"))
    }
}
