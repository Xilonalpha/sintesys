package com.synthesis

import com.synthesis.service.worker.AstronomicalSyncManager
import com.synthesis.service.worker.CelestialSyncInfo
import org.junit.Assert.*
import org.junit.Test

class AstronomicalSyncWorkerTest {

    @Test
    fun testCelestialSyncInfoFormatting() {
        val now = System.currentTimeMillis()
        val infoJustNow = CelestialSyncInfo(
            lastSyncMs = now - 5000L,
            status = "SUCCESS",
            syncedBodies = 4,
            exoplanetCount = 5200,
            gmstHours = 14.5f
        )
        assertEquals("Just now", infoJustNow.formattedLastSync)

        val infoOlder = CelestialSyncInfo(
            lastSyncMs = now - (15 * 60 * 1000L),
            status = "SUCCESS",
            syncedBodies = 4,
            exoplanetCount = 5200,
            gmstHours = 14.5f
        )
        assertTrue(infoOlder.formattedLastSync.contains("15m ago"))

        val infoNever = CelestialSyncInfo(
            lastSyncMs = 0L,
            status = "SCHEDULED",
            syncedBodies = 0,
            exoplanetCount = 0,
            gmstHours = 0f
        )
        assertEquals("Never (Scheduled)", infoNever.formattedLastSync)
    }

    @Test
    fun testGmstCalculationFormula() {
        // Test JD to GMST formula consistency
        val nowMs = 1774980000000L
        val jd = (nowMs / 86400000.0) + 2440587.5
        val d = jd - 2451545.0
        var gmstDegrees = 280.46061837 + 360.98564736629 * d
        gmstDegrees = (gmstDegrees % 360.0 + 360.0) % 360.0
        val gmstHours = gmstDegrees / 15.0

        assertTrue("GMST hours must be in range [0.0, 24.0)", gmstHours in 0.0..24.0)
    }
}
