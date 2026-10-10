package com.synthesis

import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
import com.synthesis.domain.model.exoplanet.StellarTemplateCategory
import com.synthesis.service.StellarTemplateLibraryService
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class StellarTemplateLibraryServiceTest {

    private val service = StellarTemplateLibraryService()

    @Test
    fun testTemplateLibraryCompleteness() {
        println("=== TEST: STELLAR TEMPLATE LIBRARY - COMPLETENESS ===")

        val templates = service.getAllTemplates()
        assertEquals("Library should have 8 pre-defined astrophysical templates", 8, templates.size)

        // Check required space agency templates
        val transit = service.getTemplateById("planetary_transit_mandel_agol")
        assertNotNull("Planetary transit template must exist", transit)
        assertEquals(StellarTemplateCategory.PLANETARY_TRANSIT, transit?.category)
        assertTrue("Transit must have diagnostic features", transit?.diagnosticVerificationFeatures?.isNotEmpty() == true)
        assertTrue("Transit must have U-shape morphology description", transit?.morphology?.contains("U-shape") == true)

        val detachedEclipsing = service.getTemplateById("eclipsing_binary_detached_ea")
        assertNotNull("Detached eclipsing binary must exist", detachedEclipsing)
        assertEquals(StellarTemplateCategory.ECLIPSING_BINARY, detachedEclipsing?.category)
        assertTrue("Detached binary must mention secondary eclipse", detachedEclipsing?.diagnosticVerificationFeatures?.any { it.contains("secundar") } == true)

        val contactBinary = service.getTemplateById("eclipsing_binary_contact_ew")
        assertNotNull("Contact binary must exist", contactBinary)
        assertEquals(StellarTemplateCategory.ECLIPSING_BINARY, contactBinary?.category)

        val grazingBinary = service.getTemplateById("grazing_eclipsing_binary_v_shape")
        assertNotNull("Grazing binary must exist", grazingBinary)
        assertEquals(StellarTemplateCategory.ECLIPSING_BINARY, grazingBinary?.category)
        assertTrue("Grazing binary must highlight V-shape false positive", grazingBinary?.morphology?.contains("V") == true)

        val flare = service.getTemplateById("stellar_flare_davenport")
        assertNotNull("Stellar flare template must exist", flare)
        assertEquals(StellarTemplateCategory.STELLAR_FLARE, flare?.category)
        assertEquals(false, flare?.isPeriodic)

        val starspot = service.getTemplateById("starspot_rotational_bydra")
        assertNotNull("Starspot template must exist", starspot)
        assertEquals(StellarTemplateCategory.ROTATIONAL_VARIABILITY, starspot?.category)

        val pulsating = service.getTemplateById("pulsating_rr_lyrae")
        assertNotNull("Pulsating variable template must exist", pulsating)
        assertEquals(StellarTemplateCategory.PULSATING_VARIABLE, pulsating?.category)

        val microlensing = service.getTemplateById("gravitational_microlensing_paczynski")
        assertNotNull("Microlensing template must exist", microlensing)
        assertEquals(StellarTemplateCategory.GRAVITATIONAL_MICROLENSING, microlensing?.category)
    }

    @Test
    fun testTheoreticalCurveMorphologies() {
        println("=== TEST: STELLAR TEMPLATE LIBRARY - THEORETICAL CURVES ===")

        // 1. Planetary Transit Curve: flat bottom at phase 0, baseline at phase +/- 0.4
        val transit = service.getTemplateById("planetary_transit_mandel_agol")!!
        val transitCurve = transit.generateCurve(numPoints = 101, customDepthPpm = 10000.0) // 1% depth
        assertEquals(101, transitCurve.size)

        val outOfTransitFlux = transitCurve.first().normalizedFlux
        assertEquals("Out of transit flux should be baseline 1.0", 1.0, outOfTransitFlux, 0.001)

        val centerTransitFlux = transitCurve[50].normalizedFlux // phase = 0.0
        assertTrue("Mid-transit flux should be dipped by ~1%", centerTransitFlux < 0.995)
        println("Planetary Transit: Baseline = $outOfTransitFlux, Mid-Transit = $centerTransitFlux")

        // 2. Detached Eclipsing Binary: primary at phase 0.0, secondary at phase 0.5
        val ea = service.getTemplateById("eclipsing_binary_detached_ea")!!
        val eaCurve = ea.generateCurve(numPoints = 101)
        val eaPrimary = eaCurve[50].normalizedFlux // phase 0.0
        val eaSecondary = eaCurve[0].normalizedFlux // phase -0.5 (or +0.5)
        assertTrue("EA Primary should have deep eclipse", eaPrimary < 0.88)
        assertTrue("EA Secondary at phase 0.5 should show secondary eclipse", eaSecondary < 0.98)
        assertTrue("Primary should be deeper than secondary", eaPrimary < eaSecondary)
        println("Detached EB: Primary = $eaPrimary, Secondary = $eaSecondary")

        // 3. Stellar Flare (Davenport): positive emission spike > 1.0
        val flare = service.getTemplateById("stellar_flare_davenport")!!
        val flareCurve = flare.generateCurve(numPoints = 101)
        val flarePeak = flareCurve[50].normalizedFlux // phase 0.0
        assertTrue("Flare peak must exceed baseline 1.0 (positive flux emission)", flarePeak > 1.05)
        println("Stellar Flare Peak: $flarePeak > 1.0")

        // 4. Microlensing: symmetric amplification > 1.0
        val micro = service.getTemplateById("gravitational_microlensing_paczynski")!!
        val microCurve = micro.generateCurve(numPoints = 101)
        val microPeak = microCurve[50].normalizedFlux
        assertTrue("Microlensing must amplify brightness > 1.0", microPeak > 1.1)
        // Symmetry test: flux at index 40 (phase -0.1) equals flux at index 60 (phase +0.1)
        assertEquals("Microlensing curve must be strictly symmetric", microCurve[40].normalizedFlux, microCurve[60].normalizedFlux, 1e-4)
    }

    @Test
    fun testStatisticalMatchingOnTransitData() {
        println("=== TEST: STELLAR TEMPLATE LIBRARY - STATISTICAL MATCHING ===")

        // Synthesize a realistic planetary transit light curve (depth 1.4%, WASP-12 b style)
        val syntheticTransit = mutableListOf<PhotometricDataPoint>()
        val nPoints = 50
        for (i in 0 until nPoints) {
            val phase = -0.25 + (i.toDouble() / nPoints.toDouble()) * 0.50
            // Dip in phase [-0.04, +0.04]
            val flux = if (abs(phase) <= 0.04) {
                0.9860 + (phase * phase * 2.0) // curved U-shape floor
            } else {
                1.0000
            }
            syntheticTransit.add(
                PhotometricDataPoint(
                    minuteOffset = i * 6.0,
                    bjdTime = 2460000.0 + (i * 0.004),
                    relativeFlux = flux,
                    fluxError = 0.0008
                )
            )
        }

        // Run matching
        val scores = service.matchObservedLightCurve(syntheticTransit, periodDays = 1.09)
        assertNotNull(scores)
        assertEquals(8, scores.size)

        // Best match should be planetary transit or grazing/detached EB
        val best = scores.first()
        println("Best match: ${best.template.name} with ${best.confidencePercent}% Bayesian confidence (Reduced χ² = ${best.reducedChiSquared})")
        assertTrue("Top match must have high confidence", best.confidencePercent > 20.0)
        assertTrue("Best match should have isBestMatch = true", best.isBestMatch)
        assertTrue("Reduced Chi-Squared should be reasonable for top match", best.reducedChiSquared < 150.0)
        assertEquals("Planetary transit should be top match", "planetary_transit_mandel_agol", best.template.id)

        // Non-transit templates like flare should have much lower Bayesian posterior
        val flareScore = scores.find { it.template.id == "stellar_flare_davenport" }
        assertNotNull(flareScore)
        assertTrue("Flare confidence should be much lower than best transit fit", (flareScore?.confidencePercent ?: 0.0) < best.confidencePercent)
    }
}
