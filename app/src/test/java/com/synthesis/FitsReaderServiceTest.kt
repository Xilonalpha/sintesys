package com.synthesis

import com.synthesis.service.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FitsReaderServiceTest {

    private val service = FitsReaderService()

    @Test
    fun testFitsHeaderParsingAndRawBufferAccess() {
        println("=== TEST: FITS READER SERVICE - HEADER & RAW BUFFER ACCESS ===")

        val width = 16
        val height = 16
        val numPixels = width * height

        // 1. Build Header Block (2880 bytes)
        val sb = StringBuilder()
        sb.append("SIMPLE  =                    T / Standard FITS format                           ")
        sb.append("BITPIX  =                   16 / 16-bit signed integer                          ")
        sb.append("NAXIS   =                    2 / 2D image matrix                                ")
        sb.append("NAXIS1  =                   16 / Width in pixels                                ")
        sb.append("NAXIS2  =                   16 / Height in pixels                               ")
        sb.append("BSCALE  =                  1.0 / Linear scale factor                            ")
        sb.append("BZERO   =              32768.0 / Offset for unsigned integers                   ")
        sb.append("OBJECT  = 'Kepler-186 f'       / Astronomical target                            ")
        sb.append("TELESCOP= 'Kepler Space Tel'   / Observatory                                    ")
        sb.append("EXPTIME =               1765.4 / Exposure time in seconds                       ")
        sb.append("CRVAL1  =            298.65432 / RA at reference pixel                          ")
        sb.append("CRVAL2  =             44.28190 / Dec at reference pixel                         ")
        sb.append("CRPIX1  =                  8.0 / Ref pixel X                                    ")
        sb.append("CRPIX2  =                  8.0 / Ref pixel Y                                    ")
        sb.append("CDELT1  =            0.0011111 / Plate scale deg/px                             ")
        sb.append("CDELT2  =            0.0011111 / Plate scale deg/px                             ")
        sb.append("END                                                                             ")

        while (sb.length < 2880) {
            sb.append(" ")
        }

        val headerBytes = sb.toString().toByteArray(Charsets.US_ASCII)

        // 2. Build Data Block (numPixels * 2 bytes = 512 bytes, padded to 2880 bytes)
        val dataBuffer = ByteBuffer.allocate(2880).order(ByteOrder.BIG_ENDIAN)
        // Background sky ~ 500 ADU physical (raw = 500 - 32768 = -32268)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val physicalVal = if (x == 8 && y == 8) 15000 else 500
                val rawShort = (physicalVal - 32768).toShort()
                dataBuffer.putShort(rawShort)
            }
        }
        // Pad the remainder with zeros
        while (dataBuffer.hasRemaining()) {
            dataBuffer.put(0.toByte())
        }

        val fullFitsBytes = headerBytes + dataBuffer.array()
        assertEquals(5760, fullFitsBytes.size)

        // 3. Parse with FitsReaderService
        val doc = service.read(fullFitsBytes)
        assertNotNull("Primary HDU must not be null", doc.primaryHdu)
        val header = doc.header
        val rawBuffer = doc.data
        assertNotNull("Data buffer must not be null", rawBuffer)

        // Verify header cards & WCS
        assertEquals(true, header.simple)
        assertEquals(16, header.bitPix)
        assertEquals(2, header.nAxis)
        assertEquals(16, header.width)
        assertEquals(16, header.height)
        assertEquals("Kepler-186 f", header.targetObject)
        assertEquals("Kepler Space Tel", header.telescope)
        assertEquals(1765.4, header.expTimeSeconds ?: 0.0, 0.01)
        assertNotNull("WCS must be parsed", header.wcs)
        assertEquals(298.65432, header.wcs?.crval1 ?: 0.0, 1e-4)

        // Verify typed card lookups
        assertEquals(16, header.getInt("BITPIX"))
        assertEquals("Kepler-186 f", header.getString("OBJECT"))

        // Verify raw buffer access
        assertNotNull("Raw ByteBuffer must be accessible", rawBuffer?.rawByteBuffer)
        val fb = rawBuffer?.asFloatBuffer()
        assertNotNull("Direct FloatBuffer must be accessible", fb)
        assertEquals(256, fb?.capacity())

        // Verify physical pixel scaling (Physical = Raw + 32768.0)
        val pixelCenter = rawBuffer?.getPixel(8, 8) ?: 0f
        assertEquals(15000f, pixelCenter, 1.0f)
        val pixelBg = rawBuffer?.getPixel(0, 0) ?: 0f
        assertEquals(500f, pixelBg, 1.0f)

        // Verify sub-cutout extraction
        val cutout = rawBuffer?.getCutout(8, 8, 2)
        assertNotNull(cutout)
        assertEquals(25, cutout?.size) // (2*2+1)^2 = 25

        // Verify pixel statistics
        val stats = rawBuffer?.computeStats()
        assertNotNull(stats)
        assertEquals(500f, stats?.min ?: 0f, 1f)
        assertEquals(15000f, stats?.max ?: 0f, 1f)
        assertEquals(500f, stats?.median ?: 0f, 10f)

        // Verify synthetic aperture photometry
        val ap = service.computeAperturePhotometry(
            buffer = rawBuffer!!,
            targetX = 8f,
            targetY = 8f,
            apertureRadiusPix = 2f,
            skyInRadiusPix = 4f,
            skyOutRadiusPix = 6f
        )
        assertTrue("Net source flux should be strongly positive", ap.netSourceFlux > 10000.0)
        assertTrue("SNR should be high for bright star", ap.signalToNoiseRatio > 50.0)
        println("Aperture Net Flux: ${ap.netSourceFlux}, SNR: ${ap.signalToNoiseRatio}")

        // 4. Test WCS Coordinate Transformation
        val (raDeg, decDeg) = header.wcs!!.pixelToRaDec(8.0, 8.0)!!
        assertEquals("RA at center pixel should match CRVAL1", 298.65432, raDeg, 1e-4)
        assertEquals("Dec at center pixel should match CRVAL2", 44.28190, decDeg, 1e-4)
    }

    @Test
    fun testFitsLongStringAndHierarchConventions() {
        println("=== TEST: FITS READER SERVICE - HIERARCH & LONG STRING CONTINUE ===")

        val sb = StringBuilder()
        sb.append("SIMPLE  =                    T / Standard FITS format                           ") // 80
        sb.append("BITPIX  =                    8 / 8-bit unsigned integer                         ") // 80
        sb.append("NAXIS   =                    0 / Header only HDU                                ") // 80
        sb.append("OBSERVER= 'National Aeronautics and Space Administration &'                     ") // 80
        sb.append("CONTINUE  'Astrophysics Division Transiting Exoplanet Survey'                   ") // 80
        sb.append("HIERARCH ESO INS FILT1 NAME = 'V#74' / European Southern Observatory Filter  ") // 80
        sb.append("END                                                                             ") // 80

        while (sb.length < 2880) {
            sb.append(" ")
        }

        val headerBytes = sb.toString().toByteArray(Charsets.US_ASCII)
        val doc = service.read(headerBytes)

        val obs = doc.header.getString("OBSERVER")
        assertTrue("Long string should be concatenated via CONTINUE", obs.contains("National Aeronautics") && obs.contains("Astrophysics Division"))

        val filterCard = doc.header.get("HIERARCH ESO INS FILT1 NAME")
        assertNotNull("HIERARCH keyword must be indexed in header cards", filterCard)
        assertEquals("V#74", filterCard?.asString())
    }

    @Test
    fun testFitsBinaryTableExtensionAndLightCurveExtraction() {
        println("=== TEST: FITS READER SERVICE - BINTABLE EXTENSION & LIGHT CURVE EXTRACTION ===")

        // 1. Primary HDU Header (NAXIS=0, header only)
        val h1 = StringBuilder()
        h1.append("SIMPLE  =                    T / Standard FITS format                           ")
        h1.append("BITPIX  =                    8 / Byte array                                     ")
        h1.append("NAXIS   =                    0 / No image array in primary                      ")
        h1.append("EXTEND  =                    T / File may contain extensions                    ")
        h1.append("OBJECT  = 'TIC 261136679'      / TESS Target Star                               ")
        h1.append("TELESCOP= 'TESS'               / Transiting Exoplanet Survey Satellite          ")
        h1.append("END                                                                             ")
        while (h1.length < 2880) h1.append(" ")
        val primaryHeaderBytes = h1.toString().toByteArray(Charsets.US_ASCII)

        // 2. BINTABLE Extension HDU Header
        // 3 columns: TIME (1D, 8B), PDCSAP_FLUX (1D, 8B), QUALITY (1J, 4B) => Row size = 20 bytes
        val numRows = 20
        val rowSize = 20
        val totalTableBytes = numRows * rowSize

        val h2 = StringBuilder()
        h2.append("XTENSION= 'BINTABLE'           / Binary Table Extension                         ")
        h2.append("BITPIX  =                    8 / 8-bit unsigned bytes                           ")
        h2.append("NAXIS   =                    2 / 2D table matrix                                ")
        h2.append("NAXIS1  =                   20 / Width of row in bytes                          ")
        h2.append("NAXIS2  =                   20 / Number of rows in table                        ")
        h2.append("PCOUNT  =                    0 / No heap area                                   ")
        h2.append("GCOUNT  =                    1 / Standard group count                           ")
        h2.append("TFIELDS =                    3 / Number of columns                              ")
        h2.append("TTYPE1  = 'TIME'               / BJD - 2457000                                  ")
        h2.append("TFORM1  = '1D'                 / 64-bit IEEE float                              ")
        h2.append("TUNIT1  = 'BJD - 2457000'      / Time unit                                      ")
        h2.append("TTYPE2  = 'PDCSAP_FLUX'        / PDC Calibrated Flux                            ")
        h2.append("TFORM2  = '1D'                 / 64-bit IEEE float                              ")
        h2.append("TUNIT2  = 'e-/s'               / Flux unit                                      ")
        h2.append("TTYPE3  = 'QUALITY'            / Quality Bit Flags                              ")
        h2.append("TFORM3  = '1J'                 / 32-bit signed int                              ")
        h2.append("END                                                                             ")
        while (h2.length < 2880) h2.append(" ")
        val extHeaderBytes = h2.toString().toByteArray(Charsets.US_ASCII)

        // 3. BINTABLE Data Block (20 rows * 20 bytes = 400 bytes, padded to 2880)
        val tableBuf = ByteBuffer.allocate(2880).order(ByteOrder.BIG_ENDIAN)
        val baseBjd = 1890.5
        val medianFlux = 45000.0

        for (r in 0 until numRows) {
            val t = baseBjd + (r * 0.02) // Cadence ~ 28.8 minutes
            // Transit dip in middle rows (r in 8..12)
            val flux = if (r in 8..12) medianFlux * 0.985 else medianFlux * 1.000
            val quality = if (r == 18) 128 else 0 // Row 18 has bad quality flag

            tableBuf.putDouble(t)
            tableBuf.putDouble(flux)
            tableBuf.putInt(quality)
        }
        while (tableBuf.hasRemaining()) {
            tableBuf.put(0.toByte())
        }

        val fullFitsBytes = primaryHeaderBytes + extHeaderBytes + tableBuf.array()
        assertEquals(2880 * 3, fullFitsBytes.size)

        // 4. Parse with FitsReaderService
        val doc = service.read(fullFitsBytes)
        assertNotNull("Doc must be parsed", doc)
        assertEquals("TIC 261136679", doc.header.targetObject)
        assertEquals("TESS", doc.header.telescope)
        assertEquals(1, doc.extensions.size)

        val table = doc.binaryTable
        assertNotNull("Binary table extension must be detected", table)
        assertEquals(20, table?.numRows)
        assertEquals(3, table?.columns?.size)

        // Verify column lookups
        assertNotNull("TIME column must be present", table?.get("TIME"))
        assertNotNull("PDCSAP_FLUX column must be present", table?.get("PDCSAP_FLUX"))
        assertNotNull("QUALITY column must be present", table?.get("QUALITY"))

        // 5. Test Light Curve Extraction
        val lightCurve = doc.extractLightCurve()
        assertNotNull("Extracted light curve must not be null", lightCurve)
        // Row 18 was filtered due to quality != 0, so size should be 19
        assertEquals(19, lightCurve?.size)

        // Verify normalization around 1.0
        val baselinePt = lightCurve?.first()
        assertNotNull(baselinePt)
        assertEquals(1.0, baselinePt?.relativeFlux ?: 0.0, 0.01)

        // Verify transit dip in middle
        val dipPt = lightCurve?.get(8)
        assertNotNull(dipPt)
        assertTrue("Dip relative flux should be ~0.985", (dipPt?.relativeFlux ?: 1.0) < 0.99)
        println("Extracted ${lightCurve?.size} valid light curve points from FITS BINTABLE! Dip flux: ${dipPt?.relativeFlux}")
    }
}
