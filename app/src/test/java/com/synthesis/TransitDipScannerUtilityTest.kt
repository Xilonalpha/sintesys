package com.synthesis

import com.synthesis.domain.model.exoplanet.DipRecurrenceVerdict
import com.synthesis.domain.model.exoplanet.DipShapeClassification
import com.synthesis.domain.model.xilon.MastPhotometryPoint
import com.synthesis.engine.exoplanet.TransitDipScannerUtility
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class TransitDipScannerUtilityTest {

    private lateinit var scannerUtility: TransitDipScannerUtility

    @Before
    fun setUp() {
        scannerUtility = TransitDipScannerUtility()
    }

    @Test
    fun `scanForRecurringDips detects single planetary transit with U-shape profile`() {
        // Generate simulated light curve around phase 0.0 with 7,000 ppm dip
        val points = mutableListOf<MastPhotometryPoint>()
        val random = Random(42)

        for (i in -50..50) {
            val phase = i * 0.01 // -0.5 to +0.5
            val noise = (random.nextDouble() - 0.5) * 0.0006
            val inTransit = Math.abs(phase) < 0.05
            val dip = if (inTransit) -0.0070 else 0.0
            points.add(
                MastPhotometryPoint(
                    phase = phase,
                    detrendedFlux = dip + noise,
                    modelFlux = if (inTransit) -0.0070 else 0.0
                )
            )
        }

        val report = scannerUtility.scanForRecurringDips(
            ticId = "25155310",
            points = points,
            nominalPeriodDays = 3.288,
            expectedDurationHours = 3.4,
            sigmaThreshold = 3.5
        )

        assertEquals("25155310", report.ticId)
        assertTrue("Should detect at least 1 transit dip", report.totalDipsFound >= 1)
        assertNotNull("Primary dip should not be null", report.primaryDip)

        val primary = report.primaryDip!!
        assertTrue("Depth should be close to 7,000 ppm (was ${primary.depthPpm})", primary.depthPpm in 5500.0..8500.0)
        assertTrue("SNR should be strong (> 5 sigma, was ${primary.snrSigma})", primary.snrSigma > 5.0)
        assertTrue("Magnitude drop should be positive", primary.depthMagnitudeDelta > 0.0)
        assertEquals(DipShapeClassification.U_SHAPED_TRANSIT, primary.shapeClassification)
        assertEquals(DipRecurrenceVerdict.MONO_TRANSIT_ISOLATED, report.recurrenceVerdict)
    }

    @Test
    fun `scanForRecurringDips evaluates primary and secondary occultation dips`() {
        // Generate simulated light curve with primary dip at phase 0.0 and secondary occultation at phase 0.5
        val points = mutableListOf<MastPhotometryPoint>()
        val random = Random(123)

        for (i in -100..100) {
            val phase = i * 0.005 // -0.5 to +0.5
            val noise = (random.nextDouble() - 0.5) * 0.0004
            val inPrimary = Math.abs(phase) < 0.03
            val inSecondary = Math.abs(Math.abs(phase) - 0.5) < 0.03

            val flux = when {
                inPrimary -> -0.0080
                inSecondary -> -0.0012
                else -> 0.0
            }

            points.add(
                MastPhotometryPoint(
                    phase = phase,
                    detrendedFlux = flux + noise,
                    modelFlux = flux
                )
            )
        }

        val report = scannerUtility.scanForRecurringDips(
            ticId = "388104525",
            points = points,
            nominalPeriodDays = 2.5,
            expectedDurationHours = 2.8,
            sigmaThreshold = 3.0
        )

        assertTrue("Should detect primary and secondary dips", report.totalDipsFound >= 2)
        assertEquals(DipRecurrenceVerdict.PRIMARY_AND_SECONDARY_OCCULTATION, report.recurrenceVerdict)
        assertNotNull(report.primaryDip)
        assertNotNull(report.secondaryDip)
        assertTrue(report.primaryDip!!.depthPpm > report.secondaryDip!!.depthPpm)
    }

    @Test
    fun `scanForRecurringDips handles flat light curve without dips`() {
        val points = mutableListOf<MastPhotometryPoint>()
        val random = Random(999)

        for (i in -50..50) {
            val phase = i * 0.01
            val noise = (random.nextDouble() - 0.5) * 0.0003
            points.add(
                MastPhotometryPoint(
                    phase = phase,
                    detrendedFlux = noise,
                    modelFlux = null
                )
            )
        }

        val report = scannerUtility.scanForRecurringDips(
            ticId = "99999999",
            points = points,
            nominalPeriodDays = 10.0,
            sigmaThreshold = 4.0
        )

        assertEquals(0, report.totalDipsFound)
        assertNull(report.primaryDip)
        assertEquals(DipRecurrenceVerdict.IRREGULAR_NOISE_OR_STELLAR_VARIATION, report.recurrenceVerdict)
    }
}
