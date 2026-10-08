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
    }
}
