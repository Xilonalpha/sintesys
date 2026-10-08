package com.synthesis.domain.model.exoplanet

/**
 * Model representing an astronomically detected star in a telescope camera frame
 * with sub-pixel centroid coordinates and photometric properties.
 */
data class DetectedStar(
    val id: Int,
    val x: Float, // Sub-pixel X centroid
    val y: Float, // Sub-pixel Y centroid
    val peakAdu: Float, // Peak pixel value in Analog-to-Digital Units
    val fwhmPix: Float, // Full Width at Half Maximum (seeing) in pixels
    val totalFlux: Double, // Raw aperture flux
    val skyFluxPerPix: Double, // Local sky background median
    val netFlux: Double, // Net flux after sky subtraction
    val snr: Double, // Signal to Noise Ratio
    val isTarget: Boolean = false, // True if exoplanet host star
    val isComparison: Boolean = false, // True if reference star
    val isSaturated: Boolean = false // Flagged if ADU exceeds linear dynamic range
)

/**
 * Photometric measurement extracted from a single video frame or FITS exposure.
 */
data class FramePhotometrySample(
    val frameIndex: Long,
    val timestampMs: Long,
    val timeMinutes: Double,
    val bjd: Double,
    val targetFlux: Double,
    val comparisonFlux: Double,
    val relativeFlux: Double,
    val normalizedFlux: Double,
    val fluxError: Double,
    val targetX: Float,
    val targetY: Float,
    val driftXPix: Float,
    val driftYPix: Float,
    val seeingFwhmPix: Float,
    val skyBackgroundAdu: Double
)

/**
 * Input source of the telescope camera imagery.
 */
enum class TelescopeCameraSource(val label: String) {
    PHONE_EYEPIECE_CAMERA("Cameră Telescop (Camera2 / Ocular)"),
    SMART_SCOPE_STREAM("Stream Rețea IP (Seestar / DWARF / Alpaca)"),
    FITS_RAW_FRAMES("Cadre Brute FITS (CCD/CMOS Astro)"),
    OPTICAL_TEST_FEED("Generator Optic Calibrat (Test Senzor)")
}

/**
 * State of the Telescope Vision & Photometry pipeline.
 */
data class TelescopeVisionState(
    val isRunning: Boolean = false,
    val source: TelescopeCameraSource = TelescopeCameraSource.PHONE_EYEPIECE_CAMERA,
    val fps: Float = 0f,
    val frameWidth: Int = 640,
    val frameHeight: Int = 480,
    val exposureTimeMs: Double = 50.0,
    val isoGain: Int = 800,
    val apertureRadiusPix: Float = 6.0f,
    val skyInnerRadiusPix: Float = 12.0f,
    val skyOuterRadiusPix: Float = 20.0f,
    val detectedStars: List<DetectedStar> = emptyList(),
    val targetStar: DetectedStar? = null,
    val comparisonStars: List<DetectedStar> = emptyList(),
    val photometrySamples: List<FramePhotometrySample> = emptyList(),
    val totalFramesProcessed: Long = 0,
    val mountDriftTotalX: Float = 0f,
    val mountDriftTotalY: Float = 0f,
    val currentFwhmPix: Float = 2.4f,
    val measuredTransitDepthPpm: Double = 0.0,
    val measuredPlanetRadiusEarth: Double = 0.0,
    val transitSnr: Double = 0.0,
    val chiSquared: Double = 1.0,
    val isTransitDetected: Boolean = false,
    val periodicDetectionResult: PeriodicTransitDetectionResult? = null,
    val lastLoadedFitsHeader: com.synthesis.service.FitsHeader? = null,
    val snrFilterThreshold: Float = 3.0f,
    val atmosphericNoiseSigmaMad: Float = 14.2f,
    val statusMessage: String = "Sistem pregătit pentru achiziție cadre."
)
