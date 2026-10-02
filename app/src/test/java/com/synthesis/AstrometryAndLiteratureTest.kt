package com.synthesis

import com.synthesis.engine.astrometry.AstrometryEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class AstrometryAndLiteratureTest {

    private val astrometryEngine = AstrometryEngine()

    @Test
    fun testNorthCelestialPoleGalacticCoordinates() {
        // North Celestial Pole has RA = 0°, Dec = +90°
        // By IAU 1958 definition, Galactic latitude b = +27.12825° and Galactic longitude l = 122.93192°
        val galactic = astrometryEngine.equatorialToGalactic(raDeg = 0.0, decDeg = 90.0)

        assertEquals(27.128, galactic.bDeg, 0.05)
        assertEquals(122.932, galactic.lDeg, 0.05)
    }

    @Test
    fun testGalacticCenterCoordinates() {
        // Galactic Center (Sgr A*) is near RA = 266.4168°, Dec = -29.0078°
        // In Galactic frame, l ≈ 359.94° or 0°, b ≈ -0.05°
        val galactic = astrometryEngine.equatorialToGalactic(raDeg = 266.4168, decDeg = -29.0078)

        assertTrue("Galactic longitude should be near 0° or 360° (got ${galactic.lDeg})", galactic.lDeg < 1.0 || galactic.lDeg > 359.0)
        assertEquals(0.0, galactic.bDeg, 0.2)
    }

    @Test
    fun testSexagesimalFormatting() {
        // 180° RA = 12h 00m 00.00s
        val raStr = astrometryEngine.formatSexagesimalRa(180.0)
        assertTrue(raStr.startsWith("12h 00m"))

        // -45.5° Dec = -45° 30' 00.0"
        val decStr = astrometryEngine.formatSexagesimalDec(-45.5)
        assertTrue(decStr.contains("-45° 30'"))
    }

    @Test
    fun testAstronomicalTimeBaselines() {
        // Test with a known epoch: 2024-01-01 00:00:00 UTC = 1704067200000 ms
        val unixMs = 1704067200000L
        val bjd = astrometryEngine.unixMsToBjdTdb(unixMs)
        val btjd = astrometryEngine.unixMsToBtjd(unixMs)

        // JD on 2024-01-01 00:00:00 UTC is ~ 2460310.5
        assertEquals(2460310.5, bjd, 0.01)
        // BTJD = BJD - 2457000 ≈ 3310.5
        assertEquals(3310.5, btjd, 0.01)

        val formattedBjd = astrometryEngine.formatBjd(bjd)
        assertTrue(formattedBjd.startsWith("BJD 2460310"))
    }

    @Test
    fun testNasaAdsAndArxivQueryUrls() {
        val adsUrl = astrometryEngine.buildNasaAdsQueryUrl("WASP-126 b", "272074660")
        assertTrue(adsUrl.contains("ui.adsabs.harvard.edu"))
        assertTrue(adsUrl.contains("TIC+272074660") || adsUrl.contains("WASP-126"))

        val arxivUrl = astrometryEngine.buildArxivQueryUrl("WASP-126 b", "272074660")
        assertTrue(arxivUrl.contains("arxiv.org/search/astro-ph"))

        val simbadUrl = astrometryEngine.buildSimbadUrl("272074660", "WASP-126 b")
        assertTrue(simbadUrl.contains("simbad.u-strasbg.fr"))
    }
}
