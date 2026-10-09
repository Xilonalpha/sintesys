package com.synthesis

import com.synthesis.domain.model.exoplanet.TessSurveyCriteria
import com.synthesis.domain.model.exoplanet.TessSurveyDiscovery
import com.synthesis.domain.model.exoplanet.TessSurveyScoutStatus
import org.junit.Assert.*
import org.junit.Test

class TessSurveyWorkManagerTest {

    @Test
    fun testTessSurveyCriteriaDefaultsAndCustomization() {
        val defaultCriteria = TessSurveyCriteria()
        assertEquals(12.0, defaultCriteria.maxTmag, 0.001)
        assertEquals(7.1, defaultCriteria.minSnr, 0.001)
        assertEquals(400.0, defaultCriteria.minDepthPpm, 0.001)
        assertTrue(defaultCriteria.dwarfStarsOnly)
        assertTrue(defaultCriteria.unvettedNonToiPreferred)
        assertEquals(6, defaultCriteria.scanBatchSize)

        val customCriteria = defaultCriteria.copy(
            maxTmag = 10.5,
            minSnr = 10.0,
            minDepthPpm = 800.0,
            dwarfStarsOnly = false,
            scanBatchSize = 12
        )
        assertEquals(10.5, customCriteria.maxTmag, 0.001)
        assertEquals(10.0, customCriteria.minSnr, 0.001)
        assertEquals(800.0, customCriteria.minDepthPpm, 0.001)
        assertFalse(customCriteria.dwarfStarsOnly)
        assertEquals(12, customCriteria.scanBatchSize)
    }

    @Test
    fun testTessSurveyScoutStatusFormatting() {
        val now = System.currentTimeMillis()

        val justNowStatus = TessSurveyScoutStatus(
            isPeriodicEnabled = true,
            lastRunTimestampMs = now - 15_000L, // 15s ago
            lastRunStatus = "SUCCESS",
            totalStarsEvaluated = 18,
            discoveriesCount = 2,
            lastDiscoveredTic = "384159646"
        )
        assertEquals("Chiar acum", justNowStatus.formattedLastRun)

        val minutesAgoStatus = TessSurveyScoutStatus(
            isPeriodicEnabled = true,
            lastRunTimestampMs = now - (25 * 60 * 1000L), // 25 min ago
            lastRunStatus = "SUCCESS",
            totalStarsEvaluated = 36,
            discoveriesCount = 4,
            lastDiscoveredTic = "268301217"
        )
        assertEquals("25m în urmă", minutesAgoStatus.formattedLastRun)

        val scheduledStatus = TessSurveyScoutStatus(
            isPeriodicEnabled = true,
            lastRunTimestampMs = 0L,
            lastRunStatus = "STANDBY (Programat)",
            totalStarsEvaluated = 0,
            discoveriesCount = 0,
            lastDiscoveredTic = null
        )
        assertEquals("Programat (Așteptare ciclu)", scheduledStatus.formattedLastRun)
    }

    @Test
    fun testTessSurveyDiscoveryCreationAndIntegrity() {
        val discovery = TessSurveyDiscovery(
            ticId = "384159646",
            starName = "TOI-1895.01",
            depthPpm = 4850.0,
            depthMagnitudeDelta = 0.0053,
            orbitalPeriodDays = 748.07,
            derivedRadiusEarth = 10.5,
            snr = 21.4,
            hostStarTmag = 9.75,
            hostStarRadiusSun = 0.98,
            transitDurationHours = 14.8,
            verdictLabel = "AUTONOMOUS EXOPLANET CANDIDATE",
            scientificRationale = "Semnal fotometric robust validat."
        )

        assertEquals("384159646", discovery.ticId)
        assertEquals(4850.0, discovery.depthPpm, 0.01)
        assertEquals(21.4, discovery.snr, 0.01)
        assertTrue(discovery.derivedRadiusEarth > 0)
        assertTrue(discovery.timestampMs > 0)
    }
}
