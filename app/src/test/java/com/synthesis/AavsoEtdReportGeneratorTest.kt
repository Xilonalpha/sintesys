package com.synthesis

import com.synthesis.domain.model.exoplanet.LiveObservationSession
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
import com.synthesis.engine.exoplanet.BayesianUncertaintyEngine
import com.synthesis.service.AavsoEtdReportGenerator
import com.synthesis.service.FitsImageParser
import com.synthesis.service.PeriodicTransitAnalysisEngine
import com.synthesis.service.PhotometricFileParser
import org.junit.Assert.*
import org.junit.Test

class AavsoEtdReportGeneratorTest {

    private val fitsParser = FitsImageParser()
    private val generator = AavsoEtdReportGenerator()
    private val bayesEngine = BayesianUncertaintyEngine()
    private val periodicEngine = PeriodicTransitAnalysisEngine(bayesEngine)
    private val parser = PhotometricFileParser(fitsParser, periodicEngine)

    @Test
    fun testEtdReportGeneration() {
        println("=== TEST: ETD EXOPLANET PROTOCOL GENERATION ===")
        val points = listOf(
            PhotometricDataPoint(0.0, 2460218.3590, 1.0002, 0.0011),
            PhotometricDataPoint(90.0, 2460218.4215, 0.9965, 0.0011),
            PhotometricDataPoint(180.0, 2460218.4840, 0.9998, 0.0011)
        )
        val session = LiveObservationSession(
            candidateName = "TOI-1895.01",
            hostStar = "TIC 384159821",
            observerName = "Zagorka Test",
            aavsoObserverCode = "ZTEST",
            isRealImportedData = true,
            dataPoints = points,
            measuredDepthPpm = 3500.0,
            measuredRadiusEarth = 6.45,
            signalToNoiseRatio = 18.5
        )

        val report = generator.generateEtdReport(session)
        assertNotNull(report)
        assertTrue("Report must contain target name", report.contains("TOI-1895.01"))
        assertTrue("Report must contain host star TIC", report.contains("TIC 384159821"))
        assertTrue("Report must contain observer name", report.contains("Zagorka Test"))
        assertTrue("Report must indicate 100% real imported data", report.contains("100% REAL IMPORTED PHOTOMETRY"))
        assertTrue("Report must contain ETD header", report.contains("EXOPLANET TRANSIT DATABASE (ETD)"))
        assertTrue("Report must contain time series table", report.contains("DIFF_FLUX"))
    }

    @Test
    fun testAavsoExtendedReportGeneration() {
        println("=== TEST: AAVSO EXTENDED REPORT FORMAT ===")
        val points = listOf(
            PhotometricDataPoint(0.0, 2460218.3590, 1.0000, 0.0011),
            PhotometricDataPoint(90.0, 2460218.4215, 0.9965, 0.0011)
        )
        val session = LiveObservationSession(
            candidateName = "TOI-1895.01",
            observerName = "Zagorka Lynovi",
            aavsoObserverCode = "LYNO",
            isRealImportedData = true,
            comparisonStarName = "TYC 3412-1102-1",
            comparisonStarMag = 10.450,
            checkStarName = "TYC 3412-0943-1",
            checkStarMag = 10.610,
            dataPoints = points
        )

        val report = generator.generateAavsoReport(session)
        assertNotNull(report)
        assertTrue("Must have AAVSO type extended", report.contains("#TYPE=Extended"))
        assertTrue("Must have OBSCODE LYNO", report.contains("#OBSCODE=LYNO"))
        assertTrue("Must contain comparison star TYC", report.contains("TYC 3412-1102-1"))
        assertTrue("Must have date JD delimiter", report.contains("#DATE=JD"))
        assertTrue("Must end with #END", report.trim().endsWith("#END"))
    }

    @Test
    fun testRealPhotometricFileParser() {
        println("=== TEST: REAL PHOTOMETRIC FILE PARSING (ASTROIMAGEJ / CSV) ===")
        val sampleCsv = """
            # AstroImageJ Photometry Export
            BJD_TDB,rel_flux_T1,rel_flux_err_T1
            2460218.3200,1.0002,0.00085
            2460218.3250,0.9998,0.00085
            2460218.3300,1.0005,0.00085
            2460218.3350,0.9972,0.00085
            2460218.3400,0.9858,0.00085
            2460218.3450,0.9862,0.00085
            2460218.3500,0.9859,0.00085
            2460218.3550,0.9970,0.00085
            2460218.3600,1.0004,0.00085
        """.trimIndent()

        val result = parser.parsePhotometryData(sampleCsv, defaultHostStarRadiusSun = 1.0)
        assertNotNull(result)
        assertEquals("Must parse 9 points", 9, result.points.size)
        assertTrue("Depth must be around 14000 ppm", result.measuredDepthPpm > 10000.0)
        assertTrue("Radius must be around Jupiter size (~12-15 R_Earth)", result.measuredRadiusEarth > 10.0)
        assertTrue("SNR must be statistically significant", result.snr > 5.0)
    }
}
