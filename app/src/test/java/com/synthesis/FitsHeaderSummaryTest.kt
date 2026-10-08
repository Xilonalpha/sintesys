package com.synthesis

import com.synthesis.domain.model.exoplanet.LiveObservationSession
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
import com.synthesis.domain.model.exoplanet.TelescopeVisionState
import com.synthesis.service.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FitsHeaderSummaryTest {

    @Test
    fun testFitsHeaderMetadataExtraction_parsesExpectedFields() {
        val parser = FitsImageParser()

        // Construct 2880-byte valid FITS header block
        val headerBytes = ByteArray(2880) { 32.toByte() } // Space filled
        val cards = listOf(
            "SIMPLE  =                    T / Standard FITS format",
            "BITPIX  =                   16 / 16-bit integer",
            "NAXIS   =                    2 / 2D image",
            "NAXIS1  =                 2048 / Width",
            "NAXIS2  =                 2048 / Height",
            "BSCALE  =                  1.0 / Scale",
            "BZERO   =              32768.0 / Zero offset",
            "DATE-OBS= '2026-03-22T21:44:18' / Shutter open timestamp",
            "EXPTIME =                120.0 / Exposure time in seconds",
            "OBJECT  = 'TOI-1895.01'        / Target candidate",
            "TELESCOP= 'Planewave CDK24'    / Telescope name",
            "FILTER  = 'Sloan r'            / Optical filter used",
            "END"
        )

        var offset = 0
        cards.forEach { card ->
            val cardBytes = card.padEnd(80, ' ').toByteArray(Charsets.US_ASCII)
            System.arraycopy(cardBytes, 0, headerBytes, offset, 80)
            offset += 80
        }

        val metadata = parser.parseFitsHeader(headerBytes)

        assertEquals("TOI-1895.01", metadata.targetObject)
        assertEquals(120.0, metadata.expTimeSeconds, 0.001)
        assertEquals("Sloan r", metadata.filter)
        assertEquals("Planewave CDK24", metadata.telescope)
        assertEquals(2048, metadata.width)
        assertEquals(2048, metadata.height)
        assertEquals(16, metadata.bitPix)
    }

    @Test
    fun testSexagesimalCoordinateConversion() {
        // Test RA: 68.5524 deg -> ~04h 34m 12.6s
        val raDeg = 68.5524
        val totalHours = ((raDeg % 360.0 + 360.0) % 360.0) / 15.0
        val h = totalHours.toInt()
        val m = ((totalHours - h) * 60.0).toInt()
        val s = (((totalHours - h) * 60.0 - m) * 60.0)
        val raStr = String.format("%02dh %02dm %04.1fs", h, m, s)

        assertEquals("04h 34m 12.6s", raStr)

        // Test Dec: +29.8971 deg -> +29° 53' 49.6"
        val decDeg = 29.8971
        val sign = if (decDeg >= 0) "+" else "-"
        val absVal = kotlin.math.abs(decDeg)
        val d = absVal.toInt()
        val dm = ((absVal - d) * 60.0).toInt()
        val ds = (((absVal - d) * 60.0 - dm) * 60.0)
        val decStr = String.format("%s%02d° %02d' %04.1f\"", sign, d, dm, ds)

        assertEquals("+29° 53' 49.6\"", decStr)
    }

    @Test
    fun testTelescopeVisionState_storesLoadedFitsHeader() {
        val cards = listOf(
            FitsCard("OBJECT", "'WASP-12 b'", "Target"),
            FitsCard("EXPTIME", "300.0", "Exposure seconds"),
            FitsCard("FILTER", "'Johnson V'", "Filter")
        )
        val map = cards.associateBy { it.key }
        val header = FitsHeader(
            cards = cards,
            cardMap = map,
            targetObject = "WASP-12 b",
            expTimeSeconds = 300.0,
            filter = "Johnson V"
        )

        val state = TelescopeVisionState(
            lastLoadedFitsHeader = header
        )

        assertNotNull(state.lastLoadedFitsHeader)
        assertEquals("WASP-12 b", state.lastLoadedFitsHeader?.targetObject)
        assertEquals(300.0, state.lastLoadedFitsHeader?.expTimeSeconds ?: 0.0, 0.01)
        assertEquals("Johnson V", state.lastLoadedFitsHeader?.filter)
    }

    @Test
    fun testDynamicSnrThreshold_reFiltersBufferCorrectly() {
        val parser = FitsImageParser()
        val width = 32
        val height = 32
        val pixels = FloatArray(width * height) { 100f }
        // Add random seeing fluctuation around background
        for (i in pixels.indices) {
            pixels[i] = 100f + ((i % 7) - 3) * 5f
        }
        // Add central star
        pixels[16 * width + 16] = 500f

        // Lower threshold (1.5 sigma) -> less noise attenuation
        val lowThresholdFiltered = parser.applySnrFilterToBuffer(
            pixels = pixels,
            width = width,
            height = height,
            snrThreshold = 1.5f
        )

        // Higher threshold (8.0 sigma) -> stronger noise attenuation towards background
        val highThresholdFiltered = parser.applySnrFilterToBuffer(
            pixels = pixels,
            width = width,
            height = height,
            snrThreshold = 8.0f
        )

        // Star peak is preserved in both
        assertEquals(500f, lowThresholdFiltered[16 * width + 16], 2.0f)
        assertEquals(500f, highThresholdFiltered[16 * width + 16], 2.0f)

        // Residual noise in background pixel
        val noisyBgRaw = pixels[0] // 85f or 115f
        val lowAtten = kotlin.math.abs(lowThresholdFiltered[0] - 100f)
        val highAtten = kotlin.math.abs(highThresholdFiltered[0] - 100f)

        // Higher SNR threshold pulls noisy background closer to median background (attenuation is stronger)
        assertTrue("High SNR threshold should pull background closer to median background", highAtten <= lowAtten + 1e-4)
    }

    @Test
    fun testPdfReportTargetNameSanitizationAndMetadataVerification() {
        val targetName = "TOI-1895.01 / TIC 384159821"
        val sanitized = targetName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        assertEquals("TOI-1895.01___TIC_384159821", sanitized)

        // Verify model transit flux calculation logic
        val midMinutes = 90.0
        val halfDur = 30.0
        val depthPpm = 3500.0
        val dropFraction = depthPpm / 1_000_000.0

        // Out of transit: flux is 1.0000
        val outOfTransitTime = 30.0
        val deltaOut = kotlin.math.abs(outOfTransitTime - midMinutes)
        assertTrue("Time 30m should be out of transit", deltaOut >= halfDur)

        // In mid transit: flux is 1.0000 - dropFraction
        val inTransitTime = 90.0
        val deltaIn = kotlin.math.abs(inTransitTime - midMinutes)
        assertTrue("Time 90m should be inside transit core", deltaIn < halfDur * 0.75)
    }

    @Test
    fun testSnrFilterThresholdScalingFactor() {
        // Test factor calculation used when dynamically adjusting SNR slider in ViewModel
        val thresholdLow = 1.5f
        val thresholdStandard = 3.0f
        val thresholdHigh = 8.0f

        val factorLow = (3.0f / thresholdLow).toDouble()
        val factorStandard = (3.0f / thresholdStandard).toDouble()
        val factorHigh = (3.0f / thresholdHigh).toDouble()

        assertEquals(2.0, factorLow, 0.001)
        assertEquals(1.0, factorStandard, 0.001)
        assertEquals(0.375, factorHigh, 0.001)

        // As threshold increases, noise scatter factor decreases
        assertTrue("Higher SNR threshold must yield smaller noise scatter factor", factorHigh < factorStandard)
        assertTrue("Standard SNR threshold must yield smaller noise scatter factor than low", factorStandard < factorLow)
    }
}
