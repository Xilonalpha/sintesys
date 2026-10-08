package com.synthesis

import android.content.Context
import com.synthesis.engine.exoplanet.RomanMicrolensingEngine
import com.synthesis.service.ArSkyTrackerService
import com.synthesis.service.DeviceSkyOrientation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class RomanAndArEnginesTest {

    private lateinit var romanEngine: RomanMicrolensingEngine

    @Before
    fun setUp() {
        romanEngine = RomanMicrolensingEngine()
    }

    @Test
    fun testPaczynskiMagnificationAnalyticValues() {
        // At u = 1.0, A(1) = (1 + 2) / (1 * sqrt(1 + 4)) = 3 / sqrt(5) ~ 1.34164
        val a1 = romanEngine.paczynskiMagnification(1.0)
        assertEquals(1.34164, a1, 0.001)

        // As u -> infinity, A(u) -> 1.0
        val aFar = romanEngine.paczynskiMagnification(10.0)
        assertTrue(aFar > 1.0 && aFar < 1.05)

        // At small impact parameter u = 0.05, A >> 1
        val aClose = romanEngine.paczynskiMagnification(0.05)
        assertTrue("Magnification for small u must exceed 15x", aClose > 15.0)
    }

    @Test
    fun testRomanRoguePlanetInversion() {
        val events = romanEngine.getBenchmarkEvents()
        val ogleRogue = events.first { it.eventId == "OGLE-2016-BLG-1928" }

        val result = romanEngine.invertMicrolensingEvent(ogleRogue)

        assertTrue("OGLE-2016-BLG-1928 must be classified as free-floating planet", result.isFreeFloating)
        assertTrue("Rogue planet mass should be sub-Earth to low Earth mass", result.inferredMassEarth in 0.1..10.0)
        assertTrue("Einstein crossing time must be under 5 hours", result.crossingDurationHours < 5.0)
        assertTrue("Light curve must be populated", result.lightCurve.isNotEmpty())
        assertEquals(ogleRogue.eventId, result.eventId)
    }

    @Test
    fun testRomanColdJupiterBinaryCausticLightCurve() {
        val events = romanEngine.getBenchmarkEvents()
        val jupiterEvent = events.first { it.eventId == "ROMAN-2026-SNOWLINE-J1" }

        val result = romanEngine.invertMicrolensingEvent(jupiterEvent)

        assertFalse("Bound Jupiter cannot be free floating", result.isFreeFloating)
        assertTrue("Inferred Jupiter mass should exceed 0.5 M_Jup", result.inferredMassJupiter > 0.5)
        assertTrue("Peak magnification must be high (> 8x)", result.peakMagnification >= 8.0)

        // Check caustic crossing existence
        val hasCaustic = result.lightCurve.any { it.isCausticCrossing }
        assertTrue("Binary planet-star lens must generate caustic crossing points", hasCaustic)
    }

    @Test
    fun testHorizontalCoordinateConversion() {
        // Trappist-1: RA = 346.62 deg, Dec = -5.04 deg
        // Observer at Bucharest (lat = 44.4 deg, lon = 26.1 deg)
        val (alt, az) = ArSkyTrackerService.computeHorizontalCoordinates(
            raDeg = 346.622,
            decDeg = -5.041,
            latDeg = 44.4268,
            lonDeg = 26.1025,
            epochMillis = 1775000000000L
        )

        assertTrue("Altitude must be within [-90, +90]", alt in -90.0..90.0)
        assertTrue("Azimuth must be within [0, 360]", az in 0.0..360.0)
    }

    @Test
    fun testArTargetScreenProjectionAlignment() {
        // Find coords for Trappist-1
        val (alt, az) = ArSkyTrackerService.computeHorizontalCoordinates(
            raDeg = 346.622,
            decDeg = -5.041,
            latDeg = 44.4268,
            lonDeg = 26.1025
        )

        // Point device exactly towards target Azimuth and Altitude
        val alignedOrientation = DeviceSkyOrientation(
            compassAzimuthDeg = az,
            altitudePitchDeg = alt,
            sensorActive = true,
            observerLatitude = 44.4268,
            observerLongitude = 26.1025
        )

        val projected = ArSkyTrackerService.computeProjectedTargets(alignedOrientation)
        val trappistTarget = projected.first { it.id == "trappist1" }

        if (alt > -5.0) {
            assertTrue("Target must be inside FOV when pointing directly at it and above horizon", trappistTarget.isInsideCameraFov)
        }
        assertEquals(0.5f, trappistTarget.screenXFraction, 0.05f)
        assertEquals(0.5f, trappistTarget.screenYFraction, 0.05f)
        assertEquals(0.0, trappistTarget.deltaAzDeg, 0.1)
        assertEquals(0.0, trappistTarget.deltaAltDeg, 0.1)
    }
}
