package com.synthesis

import com.synthesis.data.billing.SubscriptionPlan
import com.synthesis.domain.model.exoplanet.LiveTransitTarget
import com.synthesis.service.TelescopeExportService
import org.junit.Assert.*
import org.junit.Test

class MonetizationAndExportTest {

    private val exportService = TelescopeExportService()

    private val sampleTarget = LiveTransitTarget(
        candidateName = "TIC 384159.01",
        ticId = "384159",
        constellation = "Cygnus",
        rightAscension = "19h 42m 12s",
        declination = "+41° 18′ 29″",
        apparentVisualMag = 9.75,
        periodDays = 194.5,
        transitDurationHours = 14.8,
        nextTransitEpochBtjd = 3224.46,
        hoursUntilIngress = 4.0,
        transitDepthPpm = 4850.0,
        amateurTelescopeFeasibility = "HIGH (Accessible to 8\" SCT)"
    )

    @Test
    fun testSubscriptionPlansPricing() {
        println("=== TEST 1: MONETIZATION SUBSCRIPTION PLANS ===")
        val monthly = SubscriptionPlan.MONTHLY
        val yearly = SubscriptionPlan.YEARLY

        assertEquals("Monthly price must be 9.99 €", "9.99 €", monthly.priceFormatted)
        assertEquals("Yearly price must be 75.00 €", "75.00 €", yearly.priceFormatted)
        assertTrue("Yearly plan should highlight discount", yearly.period.contains("reducere"))
    }

    @Test
    fun testNinaCsvExport() {
        println("=== TEST 2: N.I.N.A. CSV EXPORT ===")
        val csv = exportService.exportToNinaCsv(sampleTarget)
        println(csv)

        assertTrue("CSV must contain N.I.N.A header", csv.contains("N.I.N.A."))
        assertTrue("CSV must contain target name", csv.contains("TIC 384159.01"))
        assertTrue("CSV must contain RA", csv.contains("19h 42m 12s"))
        assertTrue("CSV must contain Dec", csv.contains("+41° 18′ 29″"))
    }

    @Test
    fun testStellariumJsonExport() {
        println("=== TEST 3: STELLARIUM JSON EXPORT ===")
        val json = exportService.exportToStellariumJson(sampleTarget)
        println(json)

        assertTrue("Must be valid Stellarium JSON structure", json.contains("\"targets\": ["))
        assertTrue("Must contain candidate name", json.contains("TIC 384159.01"))
        assertTrue("Must contain constellation", json.contains("Cygnus"))
    }

    @Test
    fun testAstroImageJExport() {
        println("=== TEST 4: ASTROIMAGEJ APERTURE SPEC EXPORT ===")
        val aij = exportService.exportToAstroImageJ(sampleTarget)
        println(aij)

        assertTrue("Must contain AIJ header", aij.contains("AstroImageJ"))
        assertTrue("Must specify aperture guidance", aij.contains("Recommended Aperture"))
        assertTrue("Must contain transit depth", aij.contains("4850.0 ppm"))
    }
}
