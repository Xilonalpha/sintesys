package com.synthesis

import com.synthesis.domain.model.validation.ValidationStatus
import com.synthesis.domain.model.xilon.*
import com.synthesis.engine.astrometry.AstrometryEngine
import com.synthesis.engine.validation.TessHeasarcValidationEngine
import org.junit.Assert.*
import org.junit.Test

class TessHeasarcValidationEngineTest {

    private val astrometryEngine = AstrometryEngine()
    private val validationEngine = TessHeasarcValidationEngine(astrometryEngine)

    @Test
    fun testSector75PointingErrorDetectionForTic123891044() {
        println("=== TEST 1: TESS-point HEASARC FOV Validation for TIC 123891044 ===")
        // Coordinates of TIC 123891044 in Monoceros
        val raDeg = 103.5412
        val decDeg = -8.8524

        // Claimed (erroneous) Sector 75
        val audit = validationEngine.auditTessPointing(
            ticId = "123891044",
            raDeg = raDeg,
            decDeg = decDeg,
            claimedSectorStr = "Sector 75 (s0075)"
        )

        // Verify ecliptic coordinates
        assertTrue("Star must be in Southern Ecliptic Hemisphere (beta < 0)", audit.eclipticCoords.betaDeg < -20.0)

        // Pointing mismatch MUST be detected
        assertTrue("Sector 75 pointing mismatch must be flagged", audit.pointingMismatchFlag)
        assertFalse("Sector 75 was NOT observed by TESS for this target", audit.isClaimedSectorObserved)

        // Must list real confirmed HEASARC sectors: 6, 33, 87, 110, 111
        assertTrue("Real sectors must contain Sector 6", audit.actualObservedSectors.contains(6))
        assertTrue("Real sectors must contain Sector 33", audit.actualObservedSectors.contains(33))
        assertTrue("Real sectors must contain Sector 87", audit.actualObservedSectors.contains(87))

        // Suggested sector should re-align to Sector 6 / 33
        assertTrue("Suggested sector must mention Sector 6", audit.suggestedCorrectSector.contains("6"))

        // Explanation must give astronomical context
        assertTrue("Explanation must explain field-of-view mismatch", audit.pointingExplanation.contains("HEASARC TESS-point"))
    }

    @Test
    fun testValidPointingForContinuousViewingZoneTarget() {
        println("=== TEST 2: Valid Pointing Alignment for TOI-700 (TIC 150428135) ===")
        val raDeg = 97.0957
        val decDeg = -55.5786

        val audit = validationEngine.auditTessPointing(
            ticId = "150428135",
            raDeg = raDeg,
            decDeg = decDeg,
            claimedSectorStr = "Sector 12"
        )

        assertFalse("Pointing should have NO mismatch for TOI-700 in Sector 12", audit.pointingMismatchFlag)
        assertTrue("Sector 12 must be confirmed as observed", audit.isClaimedSectorObserved)
        assertTrue("Observed sectors must include Sector 12", audit.actualObservedSectors.contains(12))
    }

    @Test
    fun testPreReportValidationCandidateDossier() {
        println("=== TEST 3: Pre-Report Validation Dossier with Sector Rectification ===")
        val sampleStar = StellarTarget(
            ticId = "123891044",
            ra = 103.5412,
            dec = -8.8524,
            tMag = 12.10,
            radiusSun = 0.48,
            massSun = 0.50,
            tEff = 3700.0
        )

        val sampleDetection = XilonDetectionResult(
            star = sampleStar,
            sector = "Sector 75 (s0075)",
            tceName = "TCE_1",
            orbitalPeriodDays = 7.15,
            transitDurationHours = 2.8,
            transitDepthPpm = 1100.0,
            signalToNoiseRatio = 10.1,
            oddEvenTransitDeltaSigma = 0.4,
            secondaryEclipseDepthPpm = 0.0,
            derivedPlanetRadiusEarth = 1.62,
            derivedPlanetMassEarth = 3.2,
            surfaceGravityEarthG = 1.22,
            planetDensityGcm3 = 4.1,
            derivedEquilibriumTempK = 278.0,
            isHabitableZone = true,
            compositionClass = CompositionClass.ROCKY_TERRESTRIAL,
            verdict = DetectionVerdict.STRONG_CANDIDATE,
            verdictTitle = "HABITABLE ZONE CANDIDATE",
            verdictSummary = "Candidate transit detected in resonance",
            astrometry = AstrometryEngine.AstrometricTargetDossier(
                raDeg = 103.5412,
                decDeg = -8.8524,
                formattedRaSexagesimal = "06h 54m 09.89s",
                formattedDecSexagesimal = "-08° 51' 08.6\"",
                galactic = AstrometryEngine.GalacticCoordinates(218.4, -6.2, "218.40°", "-06.20°"),
                bjdTdbCurrent = 2460100.5,
                btjdCurrent = 3100.5,
                nasaAdsUrl = "https://ads.harvard.edu",
                arxivUrl = "https://arxiv.org",
                simbadUrl = "https://simbad.u-strasbg.fr"
            ),
            archivalCrossMatch = ArchivalCrossMatch(
                canonicalName = "TIC 123891044 (Resonant Pair)",
                status = ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL,
                toiId = null,
                catalogSource = "Planet Hunters TTV Hunt"
            )
        )

        val dossier = validationEngine.validateCandidate(sampleDetection)

        assertEquals("Status must be SECTOR_CORRECTED", ValidationStatus.SECTOR_CORRECTED, dossier.overallStatus)
        assertTrue("Validation score should remain high for genuine transit", dossier.validationScore >= 0.8)
        assertTrue("Sanitized sector must replace Sector 75 with real sectors (6/33)", dossier.sanitizedSectorLabel.contains("6"))
        assertTrue("Must contain sector correction warning", dossier.warnings.any { it.contains("ATENȚIE SECTOR") })
        assertFalse("Must confirm no eclipsing binary features", dossier.heasarcAudit.isKnownEclipsingBinary)
    }

    @Test
    fun testEclipsingBinaryFlagging() {
        println("=== TEST 4: Eclipsing Binary (EB) False Positive Identification ===")
        val binaryStar = StellarTarget(
            ticId = "99999999",
            ra = 120.0,
            dec = 30.0,
            tMag = 10.0,
            radiusSun = 1.0,
            massSun = 1.0,
            tEff = 5800.0
        )

        // Binary with high odd-even delta sigma (4.5 sigma) and secondary eclipse
        val binaryDetection = XilonDetectionResult(
            star = binaryStar,
            sector = "Sector 20",
            tceName = "TCE_EB",
            orbitalPeriodDays = 1.85,
            transitDurationHours = 2.1,
            transitDepthPpm = 45000.0, // 4.5% depth!
            signalToNoiseRatio = 35.0,
            oddEvenTransitDeltaSigma = 4.5,
            secondaryEclipseDepthPpm = 18000.0,
            derivedPlanetRadiusEarth = 8.5,
            derivedPlanetMassEarth = 80.0,
            surfaceGravityEarthG = 2.5,
            planetDensityGcm3 = 1.2,
            derivedEquilibriumTempK = 1400.0,
            isHabitableZone = false,
            compositionClass = CompositionClass.GAS_GIANT,
            verdict = DetectionVerdict.FALSE_POSITIVE,
            verdictTitle = "ECLIPSING BINARY",
            verdictSummary = "Deep alternating eclipses",
            astrometry = null,
            archivalCrossMatch = ArchivalCrossMatch(
                canonicalName = "TIC 99999999",
                status = ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL,
                toiId = null,
                catalogSource = "TESS"
            )
        )

        val dossier = validationEngine.validateCandidate(binaryDetection)

        assertEquals("Status must flag binary", ValidationStatus.SUSPECTED_ECLIPSING_BINARY, dossier.overallStatus)
        assertTrue("heasarcAudit must identify eclipsing binary", dossier.heasarcAudit.isKnownEclipsingBinary)
        assertTrue("Validation score must be penalized heavily", dossier.validationScore < 0.5)
    }
}
