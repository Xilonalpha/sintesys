package com.synthesis

import com.synthesis.domain.model.exoplanet.FramePhotometrySample
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
import com.synthesis.engine.exoplanet.BayesianUncertaintyEngine
import com.synthesis.service.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class PeriodicTransitAnalysisEngineTest {

    private lateinit var bayesianEngine: BayesianUncertaintyEngine
    private lateinit var engine: PeriodicTransitAnalysisEngine
    private lateinit var photometryEngine: TelescopeVisionPhotometryEngine
    private lateinit var parser: PhotometricFileParser

    @Before
    fun setUp() {
        bayesianEngine = BayesianUncertaintyEngine()
        engine = PeriodicTransitAnalysisEngine(bayesianEngine)
        photometryEngine = TelescopeVisionPhotometryEngine(engine)
        parser = PhotometricFileParser(FitsImageParser(), engine)
    }

    @Test
    fun testPeriodicTransitDetection_knownPeriodAndDepth() {
        // Synthesize a 6-day photometric observation baseline (cadence ~ 15 min = 0.0104 days)
        // With an exoplanet having P = 1.5 days, transit duration = 2.4 hours (0.1 days), depth = 0.012 (12,000 ppm)
        val truePeriod = 1.5
        val transitDuration = 0.1
        val depth = 0.012

        val points = mutableListOf<PhotometricDataPoint>()
        var t = 0.0
        val tMax = 6.0
        val dt = 0.015 // ~21 minutes

        while (t <= tMax) {
            val phaseInPeriod = (t % truePeriod)
            val isInTransit = phaseInPeriod < transitDuration
            val flux = if (isInTransit) 1.0 - depth else 1.0
            // add tiny noise
            val noise = 0.0005 * sin(t * 50.0)
            points.add(
                PhotometricDataPoint(
                    minuteOffset = t * 1440.0,
                    bjdTime = 2460000.0 + t,
                    relativeFlux = flux + noise,
                    fluxError = 0.0008
                )
            )
            t += dt
        }

        assertTrue("Generated dataset must have sufficient points", points.size > 100)

        val result = engine.analyzeDataPoints(
            points = points,
            minPeriodDays = 0.5,
            maxPeriodDays = 3.0,
            hostStarRadiusSolar = 1.0,
            hostStarMassSolar = 1.0
        )

        assertTrue("Periodic transit should be detected", result.isDetected)
        // Check period accuracy (within 5% of 1.5 days)
        val periodError = abs(result.bestPeriodDays - truePeriod) / truePeriod
        assertTrue("Detected period (${result.bestPeriodDays}) should match true period ($truePeriod) within 5%", periodError < 0.05)

        // Check depth measurement (should be ~ 12,000 ppm)
        assertTrue("Depth should be close to 12,000 ppm (got ${result.transitDepthPpm})", result.transitDepthPpm in 9000.0..15000.0)

        // Check SDE
        assertTrue("Signal Detection Efficiency (SDE) must be high (got ${result.signalDetectionEfficiencySde})", result.signalDetectionEfficiencySde >= 4.5)

        // Check Phase folded points
        assertTrue("Phase folded points should match data count", result.phaseFoldedPoints.size == points.size)

        // Check Odd/Even test consistency
        assertTrue("Odd and Even transits should be consistent for a true planet", result.vetting.isOddEvenConsistent)

        // Check derived physical exoplanet parameters
        // Rp = R* * sqrt(depth) * 109.076 => 1.0 * sqrt(0.012) * 109.076 ~ 11.9 Earth radii (Jupiter-sized planet)
        assertTrue("Planet radius should be around 10-14 Earth radii (got ${result.physicalParameters.planetRadiusEarth})", result.physicalParameters.planetRadiusEarth in 9.0..15.0)
        assertTrue("Semi-major axis should be physical (got ${result.physicalParameters.semiMajorAxisAu} AU)", result.physicalParameters.semiMajorAxisAu in 0.01..0.1)
    }

    @Test
    fun testFlatLine_noPeriodicTransitDetected() {
        // Pure flat line observations without dips
        val points = mutableListOf<PhotometricDataPoint>()
        for (i in 0..100) {
            val t = i * 0.02
            points.add(
                PhotometricDataPoint(
                    minuteOffset = t * 1440.0,
                    bjdTime = 2460000.0 + t,
                    relativeFlux = 1.0 + (0.0002 * (i % 3 - 1)),
                    fluxError = 0.0005
                )
            )
        }

        val result = engine.analyzeDataPoints(points)
        assertFalse("Flat light curve should NOT detect periodic transits", result.isDetected)
    }

    @Test
    fun testFramePhotometrySampleAnalysis() {
        val samples = mutableListOf<FramePhotometrySample>()
        val p = 1.0
        for (i in 0..120) {
            val t = i * 0.025 // days
            val inTransit = (t % p) < 0.05
            val flux = if (inTransit) 0.985 else 1.0 // 15,000 ppm
            samples.add(
                FramePhotometrySample(
                    frameIndex = i.toLong(),
                    timestampMs = (t * 86400000).toLong(),
                    timeMinutes = t * 1440.0,
                    bjd = 2460000.0 + t,
                    targetFlux = flux * 50000.0,
                    comparisonFlux = 100000.0,
                    relativeFlux = flux,
                    normalizedFlux = flux,
                    fluxError = 0.001,
                    targetX = 100f,
                    targetY = 100f,
                    driftXPix = 0f,
                    driftYPix = 0f,
                    seeingFwhmPix = 2.4f,
                    skyBackgroundAdu = 15.0
                )
            )
        }

        val result = photometryEngine.runPeriodicTransitAnalysis(samples)
        assertTrue("Should detect periodic transit on FramePhotometrySample", result.isDetected)
        assertTrue("Period should be near 1.0 day", abs(result.bestPeriodDays - 1.0) < 0.1)
    }

    @Test
    fun testPhotometricFileParser_withPeriodicBlsIntegration() {
        val csv = buildString {
            append("BJD_TDB,Relative_Flux,Flux_Error\n")
            val p = 0.8
            for (i in 0..150) {
                val t = 2460100.0 + (i * 0.015)
                val inTransit = ((t - 2460100.0) % p) < 0.04
                val flux = if (inTransit) 0.988 else 1.0
                append("${String.format(java.util.Locale.US, "%.5f", t)},${String.format(java.util.Locale.US, "%.5f", flux)},0.001\n")
            }
        }

        val parseResult = parser.parsePhotometryData(csv)
        assertNotNull("Periodic transit result must be populated", parseResult.periodicTransitResult)
        assertTrue("Should detect period in parsed real photometric dataset", parseResult.periodicTransitResult!!.isDetected)
        assertTrue("Log summary should mention BLS detection", parseResult.logSummary.contains("BLS"))
    }
}
