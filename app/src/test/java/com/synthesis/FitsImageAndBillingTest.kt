package com.synthesis

import com.synthesis.data.billing.PlayBillingService
import com.synthesis.data.billing.PurchaseVerificationResult
import com.synthesis.engine.exoplanet.MultiSectorHarmonicStitcherEngine
import com.synthesis.service.FitsImageData
import com.synthesis.service.FitsImageParser
import com.synthesis.service.FitsMetadata
import org.junit.Assert.*
import org.junit.Test

class FitsImageAndBillingTest {

    private val fitsParser = FitsImageParser()
    private val stitcherEngine = MultiSectorHarmonicStitcherEngine()

    @Test
    fun testFitsHeaderAndPhotometry() {
        println("=== TEST: FITS HEADER PARSER & APERTURE PHOTOMETRY ===")
        // Construct a synthetic standard 2880-byte FITS header block
        val headerBlock = StringBuilder()
        headerBlock.append("SIMPLE  =                    T / Standard FITS format                           ")
        headerBlock.append("BITPIX  =                   16 / 16-bit signed integer                          ")
        headerBlock.append("NAXIS   =                    2 / 2-dimensional image                            ")
        headerBlock.append("NAXIS1  =                   20 / Image width in pixels                          ")
        headerBlock.append("NAXIS2  =                   20 / Image height in pixels                         ")
        headerBlock.append("BZERO   =              32768.0 / Physical value = raw + BZERO                   ")
        headerBlock.append("BSCALE  =                  1.0 / Physical scale                                 ")
        headerBlock.append("OBJECT  = 'WASP-12 b'          / Target designation                             ")
        headerBlock.append("TELESCOP= 'Planewave CDK24'    / Observatory telescope                          ")
        headerBlock.append("FILTER  = 'Sloan r-prime'      / Photometric band                               ")
        headerBlock.append("END                                                                             ")

        // Pad to exactly 2880 bytes with ASCII spaces
        while (headerBlock.length < 2880) {
            headerBlock.append(" ")
        }

        val headerBytes = headerBlock.toString().toByteArray(Charsets.US_ASCII)
        assertEquals("FITS header block must be 2880 bytes", 2880, headerBytes.size)

        val meta = fitsParser.parseFitsHeader(headerBytes)
        assertEquals(16, meta.bitPix)
        assertEquals(2, meta.nAxis)
        assertEquals(20, meta.width)
        assertEquals(20, meta.height)
        assertEquals("WASP-12 b", meta.targetObject)
        assertEquals("Planewave CDK24", meta.telescope)
        assertEquals("Sloan r-prime", meta.filter)

        // Test synthetic aperture photometry on a 20x20 pixel grid with a bright star at (10, 10)
        val pixels = FloatArray(400) { 100f } // 100 ADU sky background
        // Inject Gaussian stellar peak at (10, 10)
        pixels[10 * 20 + 10] = 5000f // Central peak
        pixels[10 * 20 + 9] = 2500f
        pixels[10 * 20 + 11] = 2500f
        pixels[9 * 20 + 10] = 2500f
        pixels[11 * 20 + 10] = 2500f

        val imageData = FitsImageData(metadata = meta, pixels = pixels)
        val photResult = fitsParser.computeAperturePhotometry(
            imageData = imageData,
            targetX = 10f,
            targetY = 10f,
            apRadius = 3f,
            skyInRadius = 5f,
            skyOutRadius = 8f
        )

        assertNotNull(photResult)
        assertTrue("Net source flux must be positive after background subtraction", photResult.netSourceFlux > 5000.0)
        assertTrue("Sky background must match median 100 ADU", photResult.skyBackgroundPerPix in 99.0..101.0)
        assertTrue("SNR must be high for bright star", photResult.signalToNoiseRatio > 10.0)
    }

    @Test
    fun testTessCoverageWindowsAndDownlinkGaps() {
        println("=== TEST: TESS MISSION TIMELINE & SECTOR WINDOWS ===")
        val windows = stitcherEngine.getSectorCoverageWindows("Sector 1")
        assertEquals("Sector must have 2 orbital coverage windows separated by perigee", 2, windows.size)

        val orb1 = windows[0]
        val orb2 = windows[1]

        // Orbit 1 end to Orbit 2 start must equal perigee downlink gap
        val downlinkGap = orb2.first - orb1.second
        assertEquals("Downlink gap must be 1.25 days", 1.25, downlinkGap, 0.05)

        // Stitching candidate with authentic active coverage
        val stitched = stitcherEngine.stitchHarmonics(
            candidateName = "TOI-2076.03",
            ticId = "TIC 27491137",
            epoch1Btjd = 1330.0,
            sector1 = "Sector 1",
            epoch2Btjd = 1890.0,
            sector2 = "Sector 21",
            stellarMassSun = 0.85,
            stellarRadiusSun = 0.78,
            stellarTeffK = 5100.0
        )

        assertNotNull(stitched)
        assertTrue("Candidate must have candidate harmonics", stitched.candidateHarmonics.isNotEmpty())
        assertTrue("Solution confidence must be high", stitched.solutionConfidence > 50.0)
    }

    @Test
    fun testPlayBillingReceiptVerification() {
        println("=== TEST: GOOGLE PLAY BILLING SERVICE VERIFICATION ===")
        val billing = PlayBillingService()
        assertEquals(2, billing.availableProducts.value.size)

        val resultMonthly = billing.verifyPurchase("synthesis_monthly_999", "tok_valid_123", "SIG_OK")
        assertTrue("Monthly purchase must succeed", resultMonthly is PurchaseVerificationResult.Success)

        val resultYearly = billing.verifyPurchase("synthesis_yearly_7500", "tok_valid_456", "SIG_OK")
        assertTrue("Yearly purchase must succeed", resultYearly is PurchaseVerificationResult.Success)

        val resultInvalid = billing.verifyPurchase("invalid_sku", "tok_valid_789", null)
        assertTrue("Invalid SKU must fail", resultInvalid is PurchaseVerificationResult.Failed)
    }

    @Test
    fun testGooglePlayBillingArchitecture() {
        println("=== TEST: GOOGLE PLAY BILLING SERVICE ARCHITECTURE ===")
        val billing = PlayBillingService()
        // In JVM tests without Android context, billing client is safely not initialized but fallback products exist
        assertFalse(billing.isBillingClientReady())
        val products = billing.availableProducts.value
        assertEquals("Must offer Monthly and Yearly SKUs", 2, products.size)
        val monthly = products.find { it.productId == PlayBillingService.SKU_MONTHLY }
        assertNotNull(monthly)
        assertEquals("9.99 €", monthly?.formattedPrice)

        var completed = false
        billing.launchPurchaseFlow(PlayBillingService.SKU_YEARLY) { result ->
            assertTrue(result is PurchaseVerificationResult.Success)
            val success = result as PurchaseVerificationResult.Success
            assertEquals(PlayBillingService.SKU_YEARLY, success.productId)
            assertTrue(success.purchaseToken.startsWith("play_token_"))
            completed = true
        }
        assertTrue("Fallback purchase flow must complete synchronously", completed)
    }
}
