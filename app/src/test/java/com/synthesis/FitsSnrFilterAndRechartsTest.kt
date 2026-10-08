package com.synthesis

import com.synthesis.domain.model.exoplanet.*
import com.synthesis.service.FitsImageData
import com.synthesis.service.FitsImageParser
import com.synthesis.service.FitsMetadata
import com.synthesis.ui.screens.RechartsLightCurveHtmlBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FitsSnrFilterAndRechartsTest {

    private lateinit var fitsParser: FitsImageParser

    @Before
    fun setUp() {
        fitsParser = FitsImageParser()
    }

    @Test
    fun testAtmosphericSnrFilter_suppressesLowSnrNoise() {
        val width = 64
        val height = 64
        val pixels = FloatArray(width * height)

        val bgMedian = 500f
        val noiseAmp = 30f // High atmospheric seeing noise

        // Fill with noisy sky background
        for (i in pixels.indices) {
            val pseudoNoise = ((i % 17) - 8).toFloat() / 8f * noiseAmp
            pixels[i] = bgMedian + pseudoNoise
        }

        // Add a bright star at (32, 32) with high SNR > 15
        for (dy in -3..3) {
            for (dx in -3..3) {
                val r2 = dx * dx + dy * dy
                if (r2 <= 9) {
                    val idx = (32 + dy) * width + (32 + dx)
                    pixels[idx] = bgMedian + (2000f / (1f + r2))
                }
            }
        }

        val filtered = fitsParser.applySnrFilterToBuffer(
            pixels = pixels,
            width = width,
            height = height,
            snrThreshold = 3.0f,
            gridSize = 32,
            clipCosmicRays = true
        )

        // The star peak should be preserved with high signal
        val starPeakRaw = pixels[32 * width + 32]
        val starPeakFiltered = filtered[32 * width + 32]
        assertEquals("Bright star peak must be preserved", starPeakRaw, starPeakFiltered, 1.0f)

        // Background variance in filtered image should be reduced compared to raw noisy background
        var rawVariance = 0.0
        var filteredVariance = 0.0
        val sampleCount = 100
        for (i in 0 until sampleCount) {
            rawVariance += (pixels[i] - bgMedian) * (pixels[i] - bgMedian)
            filteredVariance += (filtered[i] - bgMedian) * (filtered[i] - bgMedian)
        }

        assertTrue("Filtered background variance should be less than or equal to raw noise variance", filteredVariance <= rawVariance)
    }

    @Test
    fun testAtmosphericSnrFilter_clipsCosmicRays() {
        val width = 32
        val height = 32
        val pixels = FloatArray(width * height) { 200f }

        // Inject an isolated cosmic ray spike (single pixel) with 50,000 ADU
        val spikeIdx = 16 * width + 16
        pixels[spikeIdx] = 50000f

        val filtered = fitsParser.applySnrFilterToBuffer(
            pixels = pixels,
            width = width,
            height = height,
            snrThreshold = 3.0f,
            clipCosmicRays = true
        )

        assertTrue("Cosmic ray spike must be significantly clipped", filtered[spikeIdx] < 2000f)
    }

    @Test
    fun testRechartsLightCurveHtmlBuilder_generatesCompleteHtml() {
        val points = listOf(
            PhotometricDataPoint(minuteOffset = 0.0, bjdTime = 2460000.00, relativeFlux = 1.0005, fluxError = 0.001),
            PhotometricDataPoint(minuteOffset = 15.0, bjdTime = 2460000.01, relativeFlux = 0.9880, fluxError = 0.001), // In transit
            PhotometricDataPoint(minuteOffset = 30.0, bjdTime = 2460000.02, relativeFlux = 0.9875, fluxError = 0.001), // In transit
            PhotometricDataPoint(minuteOffset = 45.0, bjdTime = 2460000.03, relativeFlux = 1.0002, fluxError = 0.001)
        )

        val dummyVetting = TransitVettingMetrics(
            oddTransitDepthPpm = 12500.0,
            evenTransitDepthPpm = 12500.0,
            oddEvenSigmaDiff = 0.2,
            isOddEvenConsistent = true,
            secondaryEclipseDepthPpm = 0.0,
            secondaryEclipseSigma = 0.0,
            isSecondaryEclipseDetected = false,
            durationKeplerianRatio = 0.9,
            isDurationPhysical = true,
            chiSquaredFlat = 2.5,
            chiSquaredTransit = 1.0,
            deltaChiSquared = 18.0
        )

        val dummyParams = DerivedExoplanetParameters(
            planetRadiusEarth = 11.2,
            planetRadiusJupiter = 1.0,
            radiusRatioRpRs = 0.11,
            semiMajorAxisAu = 0.045,
            orbitalVelocityKmS = 140.0,
            orbitalInclinationDeg = 88.5,
            impactParameterB = 0.2,
            equilibriumTempK = 1450.0,
            insolationSolar = 250.0,
            isHabitableZone = false,
            planetClassification = "Jupiter Fierbinte (Hot Jupiter)",
            geometricTransitProbability = 0.12
        )

        val dummyResult = PeriodicTransitDetectionResult(
            isDetected = true,
            bestPeriodDays = 1.5,
            bestPeriodHours = 36.0,
            epochBjd = 2460000.015,
            transitDepthPpm = 12500.0,
            transitDepthFraction = 0.0125,
            transitDurationHours = 2.4,
            dutyCycleQ = 0.067,
            signalDetectionEfficiencySde = 7.4,
            transitSnr = 12.5,
            vetting = dummyVetting,
            physicalParameters = dummyParams,
            periodogram = listOf(BlsPeriodogramPoint(1.5, 36.0, 45.0, 12500.0, 2.4, 12.5)),
            phaseFoldedPoints = listOf(PhaseFoldedPoint(-0.02, 0.988, 0.001, 15.0, 2460000.01, 0, true)),
            binnedPhaseCurve = listOf(PhaseBinPoint(-0.02, 0.988, 0.001, 2)),
            transitModelCurve = listOf(Pair(-0.02, 0.988), Pair(0.0, 0.9875)),
            statusSummary = "★ Tranzit periodic validat la P = 1.500 zile."
        )

        val html = RechartsLightCurveHtmlBuilder.buildHtml(
            dataPoints = points,
            periodicResult = dummyResult,
            targetName = "TOI-1895 / TIC 384159821"
        )

        assertNotNull("HTML must not be null", html)
        assertTrue("HTML should import Recharts CDN", html.contains("Recharts.min.js"))
        assertTrue("HTML should contain React and ReactDOM", html.contains("react.production.min.js"))
        assertTrue("HTML should contain periodic drop highlighting", html.contains("★ TRANZIT PERIODIC IDENTIFICAT") || html.contains("dropFlux"))
        assertTrue("HTML should contain target name", html.contains("TOI-1895"))
        assertTrue("HTML should contain period and depth values", html.contains("1.500") && html.contains("12500"))
    }
}
