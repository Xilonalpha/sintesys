package com.synthesis

import com.synthesis.engine.exoplanet.BayesianUncertaintyEngine
import com.synthesis.engine.flight.MissionFlightBenchmarkEngine
import com.synthesis.service.AgencyReportProtocolService
import com.synthesis.service.FitsReaderService
import org.junit.Assert.*
import org.junit.Test

class MissionFlightBenchmarkTest {

    private val bayesEngine = BayesianUncertaintyEngine()
    private val benchmarkEngine = MissionFlightBenchmarkEngine(bayesEngine)
    private val agencyProtocolService = AgencyReportProtocolService(bayesEngine)
    private val fitsReaderService = FitsReaderService()

    @Test
    fun testAllTenGoldenTruthPlanetsAchieveFlightQualification() {
        println("=== PILONUL 1: NASA GROUND-TRUTH V&V BENCHMARK MATRIX ===")
        val dossier = benchmarkEngine.runFullFlightQualificationSuite()

        assertEquals("Should benchmark all 10 Golden Truth exoplanets", 10, dossier.totalBenchmarksCount)
        println("Total Benchmarks: ${dossier.totalBenchmarksCount}")
        println("Passed Benchmarks: ${dossier.passedBenchmarksCount}")
        println("Average Parameter Variance: ${String.format("%.3f", dossier.averageErrorPercent)}%")
        println("Maximum Observed Error: ${String.format("%.3f", dossier.maxObservedErrorPercent)}%")
        println("Assessed Maturity Level: TRL ${dossier.assessedTrlLevel}")

        dossier.benchmarkResults.forEach { res ->
            println(res.formatSummary())
            assertTrue("Planet ${res.planet.name} should pass with error <= 1.2%", res.isFlightQualified)
            assertTrue("Max error must be <= 1.2%", res.maxErrorPercent <= 1.2)
        }

        assertEquals("Pass rate must be 100%", 100.0, dossier.qualificationRatePercent, 0.01)
        assertEquals("Assessed maturity must be TRL 9", 9, dossier.assessedTrlLevel)
        assertTrue("Mission must be certified", dossier.isMissionCertified)
        assertNotNull("Cryptographic hash must exist", dossier.verificationHash)
    }

    @Test
    fun testPhysicalUncertaintyPropagationConformsToTrl9() {
        println("=== PILONUL 2: PHYSICAL UNCERTAINTY QUANTIFICATION ===")
        val uncert = bayesEngine.propagatePhysicalUncertainties(
            depthPpm = 624.0,
            depthErrorPpm = 35.0,
            periodDays = 37.426,
            periodErrorDays = 0.005,
            stellarRadiusSolar = 0.420,
            stellarRadiusErrorSolar = 0.015,
            stellarMassSolar = 0.416,
            stellarMassErrorSolar = 0.020,
            stellarTeffK = 3480.0,
            stellarTeffErrorK = 90.0,
            chiSquared = 98.5,
            degreesOfFreedom = 100
        )

        println("Derived Radius: ${uncert.formatRadiusWithSigma()}")
        println("Derived Semi-Major Axis: ${uncert.formatSemiMajorAxisWithSigma()}")
        println("Derived Temperature: ${uncert.formatTemperatureWithSigma()}")
        println("LaTeX Paper Format: ${uncert.formatLatexString()}")

        assertTrue("Radius must be around 1.14 Earth radii", uncert.planetRadiusEarth in 1.10..1.20)
        assertTrue("Radius 1-sigma error must be positive and realistic", uncert.planetRadiusError1Sigma > 0.01 && uncert.planetRadiusError1Sigma < 0.25)
        assertTrue("Semi-major axis must be ~0.163 AU", uncert.semiMajorAxisAu in 0.15..0.18)
        assertTrue("Reduced chi-squared must be around 0.985", uncert.reducedChiSquared in 0.8..1.2)
    }

    @Test
    fun testIvoaVoEventXmlGenerationConformsToStandard() {
        println("=== PILONUL 3: IVOA VOEVENT V2.0 ALERT PROTOCOL ===")
        val xml = agencyProtocolService.generateVoEventXml(
            targetName = "TOI-700 d",
            hostStar = "TIC 150428135",
            raDeg = 100.86975,
            decDeg = -65.57861,
            transitEpochBjd = 2458765.2504,
            periodDays = 37.426,
            depthPpm = 725.0,
            snrSigma = 14.2,
            planetRadiusEarth = 1.14
        )

        println("Generated VOEvent XML snippet (first 300 chars):")
        println(xml.take(300))

        assertTrue("Must contain VOEvent root tag", xml.contains("<voe:VOEvent"))
        assertTrue("Must declare IVOA version 2.0", xml.contains("version=\"2.0\""))
        assertTrue("Must contain Who tag", xml.contains("<Who>"))
        assertTrue("Must contain What astrophysical parameters", xml.contains("<Group name=\"AstrophysicalParameters\">"))
        assertTrue("Must contain target coordinates", xml.contains("100.86975"))
        assertTrue("Must contain transit depth in ppm", xml.contains("725.0"))
        assertTrue("Must contain Inference concept", xml.contains("<Concept>Exoplanet.TransitCandidate</Concept>"))
    }

    @Test
    fun testFlightGradeFitsReaderSafeRecoveryFromTruncatedBuffer() {
        println("=== PILONUL 4: FLIGHT-GRADE FAULT TOLERANCE ===")
        // Test with a partial 500-byte truncated packet
        val truncatedPacket = "SIMPLE  =                    T / file does conform to FITS standard             BITPIX  =                   16 / number of bits per data pixel                  NAXIS   =                    0 / number of data axes                            EXTEND  =                    T / FITS dataset may contain extensions            END                                                                             ".toByteArray(Charsets.US_ASCII)

        val result = fitsReaderService.readSafely(truncatedPacket)
        assertTrue("readSafely should not crash on truncated header block", result.isSuccess)
        val doc = result.getOrNull()
        assertNotNull(doc)
        assertEquals("Should parse primary HDU successfully", 0, doc!!.primaryHdu.header.nAxis)
    }
}
