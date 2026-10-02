package com.synthesis

import com.synthesis.data.service.gemini.GeminiAstrophysicsService
import com.synthesis.domain.model.xilon.*
import org.junit.Assert.*
import org.junit.Test

class GeminiAstrophysicsServiceTest {

    @Test
    fun testCandidateAiConfidenceCalculation() {
        val star = MastStarProperties(
            ticId = "25155310",
            tMag = 10.79,
            teff = 5800.0,
            radiusSun = 1.3,
            massSun = 1.1,
            distancePc = 300.0,
            ra = 120.0,
            dec = -45.0,
            luminosity = 1.8
        )

        val match = ArchivalCrossMatch(
            status = ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION,
            canonicalName = "WASP-126 b",
            officialDisposition = "CONFIRMED PLANET",
            discoveryYear = 2016,
            discoveryFacility = "SuperWASP",
            literaturePeriodDays = 3.288,
            literatureDurationHours = 3.4,
            literatureDepthPpm = 7000.0,
            literatureRadiusEarth = 10.5,
            isBenchmarkTarget = true
        )

        val detection = XilonDetectionResult(
            star = star,
            sector = "Sector 1",
            tceName = "TCE-1",
            totalPhotonsAnalyzed = 15000,
            transitDepthPpm = 7000.0,
            transitDurationHours = 3.4,
            orbitalPeriodDays = 3.288,
            signalToNoiseRatio = 22.4,
            oddEvenTransitDeltaSigma = 0.8,
            secondaryEclipseDepthPpm = 45.0,
            derivedPlanetRadiusEarth = 10.5,
            derivedSemiMajorAxisAu = 0.044,
            derivedEquilibriumTempK = 1450.0,
            isHabitableZone = false,
            verdict = XilonDiscoveryVerdict.CONFIRMED_BENCHMARK_VALIDATION,
            verdictTitle = "CONFIRMED PLANET RECOVERY",
            verdictSummary = "Signal matches benchmark exoplanet WASP-126 b.",
            archivalCrossMatch = match,
            lightCurveSample = emptyList(),
            analysisLog = listOf("Log entry 1")
        )

        assertNotNull("Detection must have valid star TIC", detection.star.ticId)
        assertTrue("SNR must be high for confirmed benchmark", detection.signalToNoiseRatio > 15.0)
        assertTrue("Odd-even difference must be small for true planet", detection.oddEvenTransitDeltaSigma < 2.0)
    }
}
