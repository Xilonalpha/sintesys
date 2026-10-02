package com.synthesis.service

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sqrt

data class FitsMetadata(
    val isSimple: Boolean = true,
    val bitPix: Int = 16,
    val nAxis: Int = 2,
    val width: Int = 0,
    val height: Int = 0,
    val bZero: Double = 32768.0,
    val bScale: Double = 1.0,
    val dateObs: String = "",
    val expTimeSeconds: Double = 0.0,
    val targetObject: String = "",
    val telescope: String = "",
    val filter: String = "",
    val dataOffsetBytes: Int = 2880
)

data class FitsImageData(
    val metadata: FitsMetadata,
    val pixels: FloatArray
) {
    fun getPixel(x: Int, y: Int): Float {
        if (x < 0 || x >= metadata.width || y < 0 || y >= metadata.height) return 0f
        return pixels[y * metadata.width + x]
    }
}

data class AperturePhotometryResult(
    val targetX: Float,
    val targetY: Float,
    val apertureRadiusPix: Float,
    val totalApertureFlux: Double,
    val skyBackgroundPerPix: Double,
    val netSourceFlux: Double,
    val instrumentalMagnitude: Double,
    val signalToNoiseRatio: Double,
    val aperturePixelCount: Int
)

/**
 * High-performance pure-Kotlin binary FITS (Flexible Image Transport System) parser.
 * Reads astronomical CCD frames conforming to NASA IAU standards (2880-byte header blocks,
 * 16-bit / 32-bit signed and unsigned pixel arrays) and performs synthetic aperture photometry.
 */
@Singleton
class FitsImageParser @Inject constructor() {

    fun parseFitsHeader(bytes: ByteArray): FitsMetadata {
        if (bytes.size < 2880) {
            throw IllegalArgumentException("Invalid FITS: byte array is smaller than minimum 2880-byte header block.")
        }

        var bitPix = 16
        var nAxis = 2
        var width = 0
        var height = 0
        var bZero = 32768.0
        var bScale = 1.0
        var dateObs = ""
        var expTime = 0.0
        var objName = ""
        var telescope = ""
        var filter = ""

        var headerOffset = 0
        var reachedEnd = false

        while (headerOffset + 80 <= bytes.size && !reachedEnd) {
            val card = String(bytes, headerOffset, 80, Charsets.US_ASCII)
            val key = card.take(8).trim()

            when (key) {
                "BITPIX" -> bitPix = extractIntVal(card) ?: bitPix
                "NAXIS" -> nAxis = extractIntVal(card) ?: nAxis
                "NAXIS1" -> width = extractIntVal(card) ?: width
                "NAXIS2" -> height = extractIntVal(card) ?: height
                "BZERO" -> bZero = extractDoubleVal(card) ?: bZero
                "BSCALE" -> bScale = extractDoubleVal(card) ?: bScale
                "DATE-OBS" -> dateObs = extractStringVal(card) ?: dateObs
                "EXPTIME" -> expTime = extractDoubleVal(card) ?: expTime
                "OBJECT" -> objName = extractStringVal(card) ?: objName
                "TELESCOP" -> telescope = extractStringVal(card) ?: telescope
                "FILTER" -> filter = extractStringVal(card) ?: filter
                "END" -> {
                    reachedEnd = true
                }
            }

            headerOffset += 80
        }

        // Align data offset to next 2880-byte block boundary
        val remainder = headerOffset % 2880
        val dataOffset = if (remainder == 0) headerOffset else headerOffset + (2880 - remainder)

        return FitsMetadata(
            isSimple = true,
            bitPix = bitPix,
            nAxis = nAxis,
            width = width,
            height = height,
            bZero = bZero,
            bScale = bScale,
            dateObs = dateObs,
            expTimeSeconds = expTime,
            targetObject = objName,
            telescope = telescope,
            filter = filter,
            dataOffsetBytes = dataOffset
        )
    }

    fun parseFitsImage(bytes: ByteArray): FitsImageData {
        val meta = parseFitsHeader(bytes)
        val numPixels = meta.width * meta.height
        if (numPixels <= 0) {
            throw IllegalArgumentException("Invalid FITS dimensions: ${meta.width}x${meta.height}")
        }

        val pixels = FloatArray(numPixels)
        val buffer = ByteBuffer.wrap(bytes, meta.dataOffsetBytes, bytes.size - meta.dataOffsetBytes)
        buffer.order(ByteOrder.BIG_ENDIAN) // FITS data is strictly big-endian

        when (meta.bitPix) {
            16 -> {
                for (i in 0 until numPixels) {
                    if (buffer.remaining() < 2) break
                    val rawShort = buffer.short
                    // Standard FITS unsigned 16-bit conversion using BZERO
                    val physicalValue = (rawShort.toInt() + meta.bZero) * meta.bScale
                    pixels[i] = physicalValue.toFloat()
                }
            }
            -32 -> {
                for (i in 0 until numPixels) {
                    if (buffer.remaining() < 4) break
                    pixels[i] = buffer.float
                }
            }
            8 -> {
                for (i in 0 until numPixels) {
                    if (buffer.remaining() < 1) break
                    pixels[i] = (buffer.get().toInt() and 0xFF).toFloat()
                }
            }
            else -> { // Default 16-bit
                for (i in 0 until numPixels) {
                    if (buffer.remaining() < 2) break
                    pixels[i] = buffer.short.toFloat()
                }
            }
        }

        return FitsImageData(metadata = meta, pixels = pixels)
    }

    /**
     * Performs standard synthetic aperture photometry with local sky background subtraction.
     */
    fun computeAperturePhotometry(
        imageData: FitsImageData,
        targetX: Float,
        targetY: Float,
        apRadius: Float = 5.0f,
        skyInRadius: Float = 10.0f,
        skyOutRadius: Float = 15.0f
    ): AperturePhotometryResult {
        var apFluxSum = 0.0
        var apPixelCount = 0

        val skyPixelValues = mutableListOf<Float>()

        val xMin = (targetX - skyOutRadius).toInt().coerceAtLeast(0)
        val xMax = (targetX + skyOutRadius).toInt().coerceAtMost(imageData.metadata.width - 1)
        val yMin = (targetY - skyOutRadius).toInt().coerceAtLeast(0)
        val yMax = (targetY + skyOutRadius).toInt().coerceAtMost(imageData.metadata.height - 1)

        val apR2 = apRadius * apRadius
        val skyInR2 = skyInRadius * skyInRadius
        val skyOutR2 = skyOutRadius * skyOutRadius

        for (y in yMin..yMax) {
            for (x in xMin..xMax) {
                val dx = x - targetX
                val dy = y - targetY
                val dist2 = dx * dx + dy * dy

                val pixVal = imageData.getPixel(x, y)

                if (dist2 <= apR2) {
                    apFluxSum += pixVal
                    apPixelCount++
                } else if (dist2 in skyInR2..skyOutR2) {
                    skyPixelValues.add(pixVal)
                }
            }
        }

        // Sky background median
        skyPixelValues.sort()
        val skyBkgMedian = if (skyPixelValues.isNotEmpty()) {
            skyPixelValues[skyPixelValues.size / 2].toDouble()
        } else 0.0

        val netFlux = (apFluxSum - (apPixelCount * skyBkgMedian)).coerceAtLeast(1.0)
        val instrumentalMag = -2.5 * kotlin.math.log10(netFlux) + 25.0
        val snr = netFlux / sqrt(netFlux + (apPixelCount * skyBkgMedian.coerceAtLeast(1.0)))

        return AperturePhotometryResult(
            targetX = targetX,
            targetY = targetY,
            apertureRadiusPix = apRadius,
            totalApertureFlux = apFluxSum,
            skyBackgroundPerPix = skyBkgMedian,
            netSourceFlux = netFlux,
            instrumentalMagnitude = instrumentalMag,
            signalToNoiseRatio = snr.coerceAtLeast(0.1),
            aperturePixelCount = apPixelCount
        )
    }

    private fun extractIntVal(card: String): Int? {
        val eqIdx = card.indexOf('=')
        if (eqIdx == -1) return null
        val sub = card.substring(eqIdx + 1).split("/")[0].trim()
        return sub.toIntOrNull()
    }

    private fun extractDoubleVal(card: String): Double? {
        val eqIdx = card.indexOf('=')
        if (eqIdx == -1) return null
        val sub = card.substring(eqIdx + 1).split("/")[0].trim()
        return sub.toDoubleOrNull()
    }

    private fun extractStringVal(card: String): String? {
        val firstQuote = card.indexOf('\'')
        if (firstQuote == -1) return null
        val secondQuote = card.indexOf('\'', firstQuote + 1)
        if (secondQuote == -1) return null
        return card.substring(firstQuote + 1, secondQuote).trim()
    }
}
