package com.synthesis.service

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import com.synthesis.domain.model.exoplanet.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Telescope Camera Vision & Real-Time Photometry Service.
 * Interfaces directly with the physical telescope camera sensor via Camera2 (eyepiece mount),
 * network video streams (ZWO Seestar / DwarfLab / Alpaca), or raw FITS files,
 * extracting real-time aperture photometry and stellar transit light curves.
 */
@Singleton
class TelescopeCameraVisionService @Inject constructor(
    @ApplicationContext private val context: Context,
    val photometryEngine: TelescopeVisionPhotometryEngine,
    val fitsImageParser: FitsImageParser,
    val fitsReaderService: FitsReaderService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _visionState = MutableStateFlow(TelescopeVisionState())
    val visionState: StateFlow<TelescopeVisionState> = _visionState.asStateFlow()

    private val _viewfinderBitmap = MutableStateFlow<Bitmap?>(null)
    val viewfinderBitmap: StateFlow<Bitmap?> = _viewfinderBitmap.asStateFlow()

    // Camera2 hardware handles
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Processing jobs & tracking state
    private var processingJob: Job? = null
    private var streamJob: Job? = null
    private var filterJob: Job? = null
    private var initialTargetX = 0f
    private var initialTargetY = 0f
    private var referenceBaselineRatio = 0.0
    private var frameCounter: Long = 0
    private var lastFpsTimestamp: Long = 0
    private var fpsFrameCount = 0

    // Calibrated Optical Simulation parameters (when testing without physical night sky)
    private var simTimeMinutes = 0.0
    private val simTransitMidMinute = 25.0
    private val simTransitDurationMinutes = 20.0
    private var lastRawFitsBuffer: FitsRawPixelBuffer? = null
    private val simTransitDepth = 0.0210 // 21,000 ppm (WASP-12 b class)

    /**
     * Starts camera acquisition using the specified [source].
     */
    fun startVisionPipeline(source: TelescopeCameraSource = TelescopeCameraSource.PHONE_EYEPIECE_CAMERA) {
        stopVisionPipeline()
        _visionState.value = _visionState.value.copy(
            isRunning = true,
            source = source,
            statusMessage = "Inițiere captură senzor: ${source.label}..."
        )

        when (source) {
            TelescopeCameraSource.PHONE_EYEPIECE_CAMERA -> {
                startCamera2Capture()
            }
            TelescopeCameraSource.SMART_SCOPE_STREAM -> {
                startNetworkStreamCapture()
            }
            TelescopeCameraSource.OPTICAL_TEST_FEED -> {
                startOpticalTestFeed()
            }
            TelescopeCameraSource.FITS_RAW_FRAMES -> {
                _visionState.value = _visionState.value.copy(
                    statusMessage = "Selectează sau încarcă fișiere FITS pentru analiză astrofotometrică."
                )
            }
        }
    }

    /**
     * Stops camera acquisition and frame analysis.
     */
    fun stopVisionPipeline() {
        processingJob?.cancel()
        streamJob?.cancel()
        closeCamera2()
        _visionState.value = _visionState.value.copy(
            isRunning = false,
            fps = 0f,
            statusMessage = "Captură oprită."
        )
    }

    /**
     * Resets the accumulated light curve and tracking apertures.
     */
    fun resetPhotometry() {
        frameCounter = 0
        referenceBaselineRatio = 0.0
        simTimeMinutes = 0.0
        _visionState.value = _visionState.value.copy(
            photometrySamples = emptyList(),
            targetStar = null,
            comparisonStars = emptyList(),
            totalFramesProcessed = 0,
            mountDriftTotalX = 0f,
            mountDriftTotalY = 0f,
            measuredTransitDepthPpm = 0.0,
            measuredPlanetRadiusEarth = 0.0,
            transitSnr = 0.0,
            isTransitDetected = false,
            statusMessage = "Date fotometrice resetate."
        )
    }

    /**
     * Manually designates a star (by tap coordinates) as Target or Comparison.
     */
    fun selectStarAtCoordinates(x: Float, y: Float, isTarget: Boolean) {
        val currentStars = _visionState.value.detectedStars
        val closest = currentStars.minByOrNull { hypot(it.x - x, it.y - y) } ?: return

        if (isTarget) {
            initialTargetX = closest.x
            initialTargetY = closest.y
            val updatedTarget = closest.copy(isTarget = true, isComparison = false)
            _visionState.value = _visionState.value.copy(
                targetStar = updatedTarget,
                statusMessage = "Stea țintă (Exoplanetă) selectată la (${closest.x.toInt()}, ${closest.y.toInt()})."
            )
        } else {
            val updatedComparisons = _visionState.value.comparisonStars.toMutableList()
            if (updatedComparisons.any { it.id == closest.id }) {
                updatedComparisons.removeAll { it.id == closest.id }
            } else {
                updatedComparisons.add(closest.copy(isTarget = false, isComparison = true))
            }
            _visionState.value = _visionState.value.copy(
                comparisonStars = updatedComparisons,
                statusMessage = "Stele de comparație actualizate: ${updatedComparisons.size} selectate."
            )
        }
    }

    /**
     * Processes a single raw FITS file bytes, parsing header metadata, raw pixel buffer,
     * finding stars, and adding to the light curve.
     */
    fun ingestFitsFrame(fitsBytes: ByteArray, bjdTime: Double = System.currentTimeMillis() / 86400000.0 + 2440587.5) {
        try {
            val fitsDoc = fitsReaderService.read(fitsBytes)
            val header = fitsDoc.header
            val dataBuffer = fitsDoc.data ?: throw IllegalArgumentException("FITS file does not contain a primary image data array.")
            lastRawFitsBuffer = dataBuffer

            val w = header.width
            val h = header.height
            val currentThreshold = _visionState.value.snrFilterThreshold
            // Preprocessing: apply SNR filter to raw pixel buffer to remove atmospheric interference
            val pixels = dataBuffer.getAtmosphericFilteredPixels(snrThreshold = currentThreshold)
            val stats = dataBuffer.computeStats()

            // Astronomical ASINH stretch bitmap rendering
            val bmp = dataBuffer.toBitmap(FitsStretchMode.ASINH)
            _viewfinderBitmap.value = bmp

            val frameBjd = header.bjd ?: header.jd ?: bjdTime

            // Process pixels with Photometry Engine
            processLuminanceFrame(pixels, w, h, frameBjd, 1.0)
            val objName = header.targetObject?.ifBlank { null } ?: "DeepSky"
            val expInfo = header.expTimeSeconds?.let { "Exp: ${it}s" } ?: "16-bit CCD"

            _visionState.value = _visionState.value.copy(
                source = TelescopeCameraSource.FITS_RAW_FRAMES,
                lastLoadedFitsHeader = header,
                atmosphericNoiseSigmaMad = stats.madSigma,
                statusMessage = "FITS [$objName | $expInfo] (${w}x${h}): ${dataBuffer.bitPix}-bit buffer încărcat. Filtru SNR: ${String.format(java.util.Locale.US, "%.1f", currentThreshold)}σ. Stele: ${_visionState.value.detectedStars.size}."
            )
        } catch (e: Exception) {
            _visionState.value = _visionState.value.copy(
                statusMessage = "Eroare la parsarea cadrului FITS: ${e.message}"
            )
        }
    }

    /**
     * Dynamically adjusts the Signal-to-Noise Ratio (SNR) threshold and re-filters the FITS
     * raw pixel buffer asynchronously in a background coroutine, preventing any UI freezes or ANRs.
     */
    fun updateSnrFilterThreshold(threshold: Float) {
        val currentThresh = threshold.coerceIn(1.0f, 15.0f)
        _visionState.value = _visionState.value.copy(
            snrFilterThreshold = currentThresh
        )

        filterJob?.cancel()
        val dataBuffer = lastRawFitsBuffer
        val header = _visionState.value.lastLoadedFitsHeader

        if (dataBuffer != null && header != null) {
            filterJob = scope.launch(Dispatchers.Default) {
                val w = header.width
                val h = header.height
                val pixels = dataBuffer.getAtmosphericFilteredPixels(snrThreshold = currentThresh)
                yield()
                val stats = dataBuffer.computeStats()
                yield()
                val frameBjd = header.bjd ?: header.jd ?: (System.currentTimeMillis() / 86400000.0 + 2440587.5)

                // Re-process frame with updated SNR filtered buffer
                processLuminanceFrame(pixels, w, h, frameBjd, 1.0)
                yield()

                _visionState.value = _visionState.value.copy(
                    snrFilterThreshold = currentThresh,
                    atmosphericNoiseSigmaMad = stats.madSigma,
                    statusMessage = "Filtru SNR actualizat: ${String.format(java.util.Locale.US, "%.1f", currentThresh)}σ. Buffer FITS re-filtrat în fundal."
                )
            }
        }
    }

    /**
     * Batch ingests a series of raw FITS files, extracting aperture photometry
     * sequentially and running periodic transit detection on the resultant light curve.
     */
    fun ingestFitsSeries(
        fitsFramesList: List<ByteArray>,
        cadenceMinutes: Double = 5.0,
        hostStarRadiusSolar: Double = 1.0
    ): PeriodicTransitDetectionResult? {
        if (fitsFramesList.isEmpty()) return null

        val startBjd = 2460400.5
        fitsFramesList.forEachIndexed { idx, bytes ->
            val bjd = startBjd + (idx * cadenceMinutes / 1440.0)
            ingestFitsFrame(bytes, bjd)
        }

        // Run BLS periodic scan on the processed series
        return runPeriodicBlsScan(hostStarRadiusSolar = hostStarRadiusSolar)
    }

    /**
     * Executes the Box-fitting Least Squares (BLS) periodogram search on the current
     * accumulated photometry samples from video or FITS frames.
     */
    fun runPeriodicBlsScan(
        minPeriodDays: Double? = null,
        maxPeriodDays: Double? = null,
        hostStarRadiusSolar: Double = 1.0,
        hostStarMassSolar: Double = 1.0
    ): PeriodicTransitDetectionResult {
        val samples = _visionState.value.photometrySamples
        val blsResult = photometryEngine.runPeriodicTransitAnalysis(
            samples = samples,
            minPeriodDays = minPeriodDays,
            maxPeriodDays = maxPeriodDays,
            hostStarRadiusSolar = hostStarRadiusSolar,
            hostStarMassSolar = hostStarMassSolar
        )

        val updatedState = _visionState.value.copy(
            periodicDetectionResult = blsResult,
            measuredTransitDepthPpm = if (blsResult.isDetected) blsResult.transitDepthPpm else _visionState.value.measuredTransitDepthPpm,
            measuredPlanetRadiusEarth = if (blsResult.isDetected) blsResult.physicalParameters.planetRadiusEarth else _visionState.value.measuredPlanetRadiusEarth,
            transitSnr = if (blsResult.isDetected) blsResult.transitSnr else _visionState.value.transitSnr,
            isTransitDetected = _visionState.value.isTransitDetected || blsResult.isDetected,
            statusMessage = blsResult.statusSummary
        )
        _visionState.value = updatedState
        return blsResult
    }

    /**
     * Sets aperture radii in pixels.
     */
    fun setApertureRadii(aperture: Float, skyIn: Float, skyOut: Float) {
        _visionState.value = _visionState.value.copy(
            apertureRadiusPix = aperture.coerceIn(2f, 25f),
            skyInnerRadiusPix = skyIn.coerceIn(aperture + 2f, 40f),
            skyOuterRadiusPix = skyOut.coerceIn(skyIn + 2f, 60f)
        )
    }

    /**
     * Core real-time processing method invoked for every video or FITS frame.
     */
    private fun processLuminanceFrame(
        pixels: FloatArray,
        width: Int,
        height: Int,
        bjd: Double,
        deltaMinutes: Double
    ) {
        frameCounter++
        simTimeMinutes += deltaMinutes

        // Calculate FPS
        val now = System.currentTimeMillis()
        fpsFrameCount++
        var currentFps = _visionState.value.fps
        if (now - lastFpsTimestamp >= 1000) {
            currentFps = (fpsFrameCount * 1000f) / (now - lastFpsTimestamp)
            lastFpsTimestamp = now
            fpsFrameCount = 0
        }

        val state = _visionState.value
        val apRadius = state.apertureRadiusPix
        val skyInRadius = state.skyInnerRadiusPix
        val skyOutRadius = state.skyOuterRadiusPix

        // 1. Detect all stars in frame
        val allStars = photometryEngine.detectStars(
            pixels = pixels,
            width = width,
            height = height,
            sigmaThreshold = 3.5f,
            maxStars = 25
        )

        // 2. Track or Auto-Assign Target & Comparison Stars
        var currentTarget = state.targetStar
        var currentComparisons = state.comparisonStars

        if (currentTarget == null && allStars.isNotEmpty()) {
            // Auto-assign the brightest non-saturated star as target
            val bestTarget = allStars.firstOrNull { !it.isSaturated } ?: allStars.first()
            currentTarget = bestTarget.copy(isTarget = true)
            initialTargetX = currentTarget.x
            initialTargetY = currentTarget.y

            // Auto-assign the next 2-4 brightest stars as comparison ensemble
            currentComparisons = allStars.filter { it.id != currentTarget.id && !it.isSaturated }
                .take(3)
                .map { it.copy(isComparison = true) }
        } else if (currentTarget != null) {
            // Track existing target sub-pixel centroid
            val trackedTarget = photometryEngine.trackStarCentroid(
                previousStar = currentTarget,
                pixels = pixels,
                width = width,
                height = height,
                searchRadiusPix = 16.0f,
                apertureRadius = apRadius,
                skyInRadius = skyInRadius,
                skyOutRadius = skyOutRadius
            )
            if (trackedTarget != null) {
                currentTarget = trackedTarget
            }

            // Track comparison stars
            currentComparisons = currentComparisons.mapNotNull { comp ->
                photometryEngine.trackStarCentroid(
                    previousStar = comp,
                    pixels = pixels,
                    width = width,
                    height = height,
                    searchRadiusPix = 16.0f,
                    apertureRadius = apRadius,
                    skyInRadius = skyInRadius,
                    skyOutRadius = skyOutRadius
                )
            }
        }

        // 3. Compute frame aperture photometry sample
        var newSamples = state.photometrySamples
        var measuredDepth = state.measuredTransitDepthPpm
        var planetRadius = state.measuredPlanetRadiusEarth
        var transitSnr = state.transitSnr
        var chiSq = state.chiSquared
        var isTransit = state.isTransitDetected
        var currentBls = state.periodicDetectionResult

        if (currentTarget != null) {
            if (referenceBaselineRatio <= 0.0 && currentComparisons.isNotEmpty()) {
                val compSum = currentComparisons.sumOf { it.netFlux }.coerceAtLeast(1.0)
                referenceBaselineRatio = currentTarget.netFlux / compSum
            }

            val sample = photometryEngine.processFramePhotometry(
                frameIndex = frameCounter,
                timestampMs = now,
                timeMinutes = simTimeMinutes,
                bjd = bjd,
                targetStar = currentTarget,
                comparisonStars = currentComparisons,
                referenceBaselineRatio = referenceBaselineRatio,
                initialTargetX = initialTargetX,
                initialTargetY = initialTargetY
            )

            newSamples = (newSamples + sample).takeLast(250)

            if (newSamples.size >= 8) {
                val inversion = photometryEngine.invertTransitLightCurve(newSamples)
                measuredDepth = inversion.depthPpm
                planetRadius = inversion.radiusEarth
                transitSnr = inversion.snr
                chiSq = inversion.chiSquared
                isTransit = inversion.isDetected

                // Periodic BLS scan every 10 frames or on initial batch
                if (frameCounter % 10L == 0L || currentBls == null) {
                    val bls = photometryEngine.runPeriodicTransitAnalysis(newSamples)
                    currentBls = bls
                    if (bls.isDetected) {
                        isTransit = true
                        measuredDepth = bls.transitDepthPpm
                        planetRadius = bls.physicalParameters.planetRadiusEarth
                        transitSnr = bls.transitSnr
                    }
                }
            }
        }

        val driftX = if (currentTarget != null) currentTarget.x - initialTargetX else 0f
        val driftY = if (currentTarget != null) currentTarget.y - initialTargetY else 0f
        val currentFwhm = currentTarget?.fwhmPix ?: 2.4f

        _visionState.value = state.copy(
            fps = currentFps,
            frameWidth = width,
            frameHeight = height,
            detectedStars = allStars,
            targetStar = currentTarget,
            comparisonStars = currentComparisons,
            photometrySamples = newSamples,
            totalFramesProcessed = frameCounter,
            mountDriftTotalX = driftX,
            mountDriftTotalY = driftY,
            currentFwhmPix = currentFwhm,
            measuredTransitDepthPpm = measuredDepth,
            measuredPlanetRadiusEarth = planetRadius,
            transitSnr = transitSnr,
            chiSquared = chiSq,
            isTransitDetected = isTransit,
            periodicDetectionResult = currentBls
        )
    }

    // --- CAMERA2 HARDWARE IMPLEMENTATION ---
    private fun startCamera2Capture() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isEmpty()) {
                _visionState.value = _visionState.value.copy(
                    statusMessage = "Nicio cameră fizică detectată. Trecere automată pe generatorul optic calibrat."
                )
                startOpticalTestFeed()
                return
            }

            // Prefer back camera looking into the telescope eyepiece/adapter
            val cameraId = cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraIdList[0]

            val width = 640
            val height = 480
            backgroundThread = HandlerThread("TelescopeVisionThread").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)

            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader?.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        processYuvImage(image)
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)
            }

            // Open camera device
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraCaptureSession(width, height)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    _visionState.value = _visionState.value.copy(
                        statusMessage = "Eroare hardware Camera2 ($error). Se activează generatorul optic calibrat."
                    )
                    startOpticalTestFeed()
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            _visionState.value = _visionState.value.copy(
                statusMessage = "Permisiunea CAMERA este necesară. Activează permisiunea sau folosește testul optic."
            )
            startOpticalTestFeed()
        } catch (e: Exception) {
            _visionState.value = _visionState.value.copy(
                statusMessage = "Eșec la pornirea camerei: ${e.message}. Trecere pe test optic."
            )
            startOpticalTestFeed()
        }
    }

    private fun createCameraCaptureSession(width: Int, height: Int) {
        val readerSurface = imageReader?.surface ?: return
        try {
            cameraDevice?.createCaptureSession(listOf(readerSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL) ?: return
                    requestBuilder.addTarget(readerSurface)
                    // Astronomical capture parameters: infinity focus, manual exposure, high ISO
                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    requestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f) // Infinity
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 50_000_000L) // 50ms
                    requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 1600) // ISO 1600

                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    _visionState.value = _visionState.value.copy(
                        statusMessage = "Cameră conectată la ocular. Analiză stelară în timp real activă."
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    _visionState.value = _visionState.value.copy(statusMessage = "Configurarea fluxului video a eșuat.")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            _visionState.value = _visionState.value.copy(statusMessage = "Eroare sesiune cameră: ${e.message}")
        }
    }

    private fun processYuvImage(image: Image) {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val width = image.width
        val height = image.height
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride

        val floatPixels = FloatArray(width * height)
        val bitmapPixels = IntArray(width * height)

        var srcOffset = 0
        var dstOffset = 0
        for (y in 0 until height) {
            var colOffset = srcOffset
            for (x in 0 until width) {
                val lum = (buffer.get(colOffset).toInt() and 0xFF).toFloat()
                floatPixels[dstOffset] = lum
                val lumInt = lum.toInt()
                bitmapPixels[dstOffset] = Color.rgb(lumInt, lumInt, lumInt)
                colOffset += pixelStride
                dstOffset++
            }
            srcOffset += rowStride
        }

        // Update viewfinder bitmap
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(bitmapPixels, 0, width, 0, 0, width, height)
        _viewfinderBitmap.value = bmp

        val bjdNow = System.currentTimeMillis() / 86400000.0 + 2440587.5
        processLuminanceFrame(floatPixels, width, height, bjdNow, 0.016)
    }

    private fun closeCamera2() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
            backgroundThread?.quitSafely()
        } catch (_: Exception) {}
        captureSession = null
        cameraDevice = null
        imageReader = null
        backgroundThread = null
        backgroundHandler = null
    }

    // --- NETWORK STREAM (SEESTAR / DWARF / ALPACA) IMPLEMENTATION ---
    private fun startNetworkStreamCapture() {
        streamJob?.cancel()
        streamJob = scope.launch {
            _visionState.value = _visionState.value.copy(
                statusMessage = "Conectare la stream-ul video al telescopului smart (MJPEG/RTSP)..."
            )
            val streamUrls = listOf(
                "http://192.168.110.1:4700/stream.mjpg", // ZWO Seestar S50
                "http://192.168.88.1:9988/video",        // DwarfLab
                "http://192.168.1.100:11112/api/v1/camera/0/imagearray" // Alpaca
            )

            var connected = false
            for (urlStr in streamUrls) {
                if (!isActive) break
                try {
                    val url = URL(urlStr)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 2000
                        readTimeout = 3000
                    }
                    if (conn.responseCode == 200) {
                        connected = true
                        _visionState.value = _visionState.value.copy(
                            statusMessage = "Stream video recepționat cu succes de la $urlStr."
                        )
                        readMjpegStream(conn.inputStream)
                        break
                    }
                } catch (_: Exception) {
                    // Try next URL
                }
            }

            if (!connected) {
                _visionState.value = _visionState.value.copy(
                    statusMessage = "Niciun stream IP accesibil pe rețea locală. Se activează testul optic de calibrare."
                )
                startOpticalTestFeed()
            }
        }
    }

    private suspend fun readMjpegStream(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024 * 1024)
        var bytesRead: Int
        while (isActive) {
            bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break
            val bmp = BitmapFactory.decodeByteArray(buffer, 0, bytesRead)
            if (bmp != null) {
                _viewfinderBitmap.value = bmp
                val w = bmp.width
                val h = bmp.height
                val pixelsInt = IntArray(w * h)
                bmp.getPixels(pixelsInt, 0, w, 0, 0, w, h)
                val floatPixels = FloatArray(w * h) { i ->
                    val color = pixelsInt[i]
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    (0.299f * r + 0.587f * g + 0.114f * b)
                }
                val bjdNow = System.currentTimeMillis() / 86400000.0 + 2440587.5
                processLuminanceFrame(floatPixels, w, h, bjdNow, 0.05)
            }
            delay(50)
        }
    }

    // --- CALIBRATED OPTICAL TEST FEED (REALISTIC MATHEMATICAL PSF + POISSON NOISE) ---
    private fun startOpticalTestFeed() {
        processingJob?.cancel()
        val width = 640
        val height = 480
        val floatPixels = FloatArray(width * height)
        val bitmapPixels = IntArray(width * height)

        // 3 synthetic stars: Target (WASP-12 b class) + 2 Comparison stars
        val starX0 = floatArrayOf(320f, 180f, 480f)
        val starY0 = floatArrayOf(240f, 140f, 330f)
        val baseBrightness = floatArrayOf(18000f, 15000f, 16500f) // Total photon counts

        processingJob = scope.launch {
            _visionState.value = _visionState.value.copy(
                source = TelescopeCameraSource.OPTICAL_TEST_FEED,
                statusMessage = "Generator Optic Calibrat activ. Simulează tranzit exoplanetar real cu zgomot Poisson."
            )

            var frameStep = 0
            while (isActive) {
                frameStep++
                val bjd = 2460218.4215 + (simTimeMinutes / 1440.0)

                // Periodic mount drift: 0.05px/sec drift
                val driftX = (sin(frameStep * 0.02) * 4.5f).toFloat()
                val driftY = (cos(frameStep * 0.015) * 3.2f).toFloat()

                // Exoplanet transit flux calculation
                val dtTransit = abs(simTimeMinutes - simTransitMidMinute)
                val inTransitFactor = if (dtTransit < simTransitDurationMinutes / 2.0) {
                    val transitProgress = dtTransit / (simTransitDurationMinutes / 2.0)
                    // Limb-darkened transit shape: 1 - depth * (1 - 0.2 * progress^2)
                    1.0 - (simTransitDepth * (1.0 - 0.2 * transitProgress * transitProgress))
                } else {
                    1.0
                }

                // Generate background sky level: ~45 ADU with Gaussian noise
                val skyBgMean = 45f
                val skyNoise = 4.0f

                for (i in 0 until width * height) {
                    val noise = ((Math.random() - 0.5) * skyNoise * 2.0).toFloat()
                    val valPix = (skyBgMean + noise).coerceAtLeast(0f)
                    floatPixels[i] = valPix
                    val lumInt = valPix.toInt().coerceIn(0, 255)
                    bitmapPixels[i] = Color.rgb(lumInt, lumInt, lumInt)
                }

                // Render 2D Gaussian stellar PSFs
                val sigmaPsf = 1.6f // FWHM = 2.355 * 1.6 = 3.77 px
                val radiusPsf = 8

                for (s in 0 until 3) {
                    val cx = starX0[s] + driftX
                    val cy = starY0[s] + driftY
                    val factor = if (s == 0) inTransitFactor else 1.0
                    val totalPhotons = (baseBrightness[s] * factor).toFloat()
                    val peakIntensity = totalPhotons / (2f * Math.PI.toFloat() * sigmaPsf * sigmaPsf)

                    val x0 = (cx - radiusPsf).toInt().coerceAtLeast(0)
                    val x1 = (cx + radiusPsf).toInt().coerceAtMost(width - 1)
                    val y0 = (cy - radiusPsf).toInt().coerceAtLeast(0)
                    val y1 = (cy + radiusPsf).toInt().coerceAtMost(height - 1)

                    for (y in y0..y1) {
                        for (x in x0..x1) {
                            val dx = x - cx
                            val dy = y - cy
                            val r2 = dx * dx + dy * dy
                            val psfVal = peakIntensity * exp(-r2 / (2f * sigmaPsf * sigmaPsf))
                            // Poisson shot noise on star flux: sqrt(flux)
                            val shotNoise = (sqrt(psfVal.coerceAtLeast(1f)) * (Math.random() - 0.5)).toFloat()
                            val totalVal = (floatPixels[y * width + x] + psfVal + shotNoise).coerceAtLeast(0f)
                            floatPixels[y * width + x] = totalVal

                            val lumInt = totalVal.toInt().coerceIn(0, 255)
                            bitmapPixels[y * width + x] = Color.rgb(lumInt, lumInt, lumInt)
                        }
                    }
                }

                // Update viewfinder bitmap
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.setPixels(bitmapPixels, 0, width, 0, 0, width, height)
                _viewfinderBitmap.value = bmp

                // Run real photometric analysis
                processLuminanceFrame(floatPixels, width, height, bjd, 0.25)

                delay(80) // ~12 FPS
            }
        }
    }
}
