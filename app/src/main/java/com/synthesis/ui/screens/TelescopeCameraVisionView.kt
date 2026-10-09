package com.synthesis.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.ui.components.exoplanet.PeriodicTransitAnalysisCard
import com.synthesis.ui.components.exoplanet.RechartsLightCurveSampleView
import com.synthesis.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TelescopeCameraVisionView(
    visionState: TelescopeVisionState,
    viewfinderBitmap: Bitmap?,
    onStartPipeline: (TelescopeCameraSource) -> Unit,
    onStopPipeline: () -> Unit,
    onResetPhotometry: () -> Unit,
    onSelectStarAt: (Float, Float, Boolean) -> Unit,
    onIngestFitsBytes: (ByteArray) -> Unit,
    onUpdateApertureRadii: (Float, Float, Float) -> Unit,
    onTransferToOfficialSession: () -> Unit,
    onRunPeriodicBlsScan: (Double?, Double?, Double, Double) -> Unit = { _, _, _, _ -> },
    onOpenFitsSummary: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSelectTargetMode by remember { mutableStateOf(true) }
    var showApertureSettings by remember { mutableStateOf(false) }
    var chartMode by remember { mutableStateOf(0) } // 0: Canvas Nativ, 1: Recharts Interactiv

    var apertureSlider by remember { mutableStateOf(visionState.apertureRadiusPix) }
    var skyInSlider by remember { mutableStateOf(visionState.skyInnerRadiusPix) }
    var skyOutSlider by remember { mutableStateOf(visionState.skyOuterRadiusPix) }

    // FITS file picker launcher
    val fitsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    onIngestFitsBytes(bytes)
                }
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER CARD: STATUS & SOURCE SELECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (visionState.isRunning) AccentGreen else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (visionState.isRunning) AccentGreen else TextSecondary,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "TELESCOPE CAMERA VISION & FITS PHOTOMETRY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (visionState.isRunning) AccentGreen.copy(alpha = 0.2f) else DarkBorder
                        ) {
                            Text(
                                if (visionState.isRunning) "${String.format("%.1f", visionState.fps)} FPS" else "OPRIT",
                                color = if (visionState.isRunning) AccentGreen else TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Procesează fluxul video live direct de la senzorul optic al telescopului (ocular, CMOS, stream IP Seestar/DWARF) sau cadre brute FITS pentru extragerea fotometriei de tranzit în timp real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Source chips
                    Text("Sursă Intrare Video / Cadre:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelescopeCameraSource.values().forEach { src ->
                            val isSelected = visionState.source == src
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStartPipeline(src) },
                                label = {
                                    Text(
                                        when (src) {
                                            TelescopeCameraSource.PHONE_EYEPIECE_CAMERA -> "📷 Ocular / Camera2"
                                            TelescopeCameraSource.SMART_SCOPE_STREAM -> "📡 Stream IP Scope"
                                            TelescopeCameraSource.FITS_RAW_FRAMES -> "💾 Fișiere FITS"
                                            TelescopeCameraSource.OPTICAL_TEST_FEED -> "🔬 Test Calibrat"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = AccentCyan
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) AccentCyan else DarkBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. LIVE TELESCOPE VIEWFINDER & RETICLE HUD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CANVAS OPTIC LIVE (${visionState.frameWidth}x${visionState.frameHeight})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Mode selector: Target vs Comparison
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelectTargetMode) AccentGreen.copy(alpha = 0.25f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSelectTargetMode) AccentGreen else DarkBorder),
                                modifier = Modifier.clickable { isSelectTargetMode = true }
                            ) {
                                Text(
                                    "Țintă (T)",
                                    color = if (isSelectTargetMode) AccentGreen else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (!isSelectTargetMode) AccentCyan.copy(alpha = 0.25f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (!isSelectTargetMode) AccentCyan else DarkBorder),
                                modifier = Modifier.clickable { isSelectTargetMode = false }
                            ) {
                                Text(
                                    "Comparație (C)",
                                    color = if (!isSelectTargetMode) AccentCyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Viewfinder Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF030712))
                            .pointerInput(visionState.frameWidth, visionState.frameHeight) {
                                detectTapGestures { offset ->
                                    val canvasW = size.width.toFloat()
                                    val canvasH = size.height.toFloat()
                                    if (canvasW > 0 && canvasH > 0) {
                                        val frameX = (offset.x / canvasW) * visionState.frameWidth
                                        val frameY = (offset.y / canvasH) * visionState.frameHeight
                                        onSelectStarAt(frameX, frameY, isSelectTargetMode)
                                    }
                                }
                            }
                    ) {
                        // Render camera bitmap or star field
                        if (viewfinderBitmap != null && !viewfinderBitmap.isRecycled) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = viewfinderBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                )
                            }
                        }

                        // Astronomical HUD Overlays (Centroid apertures, annular rings, labels)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scaleX = size.width / visionState.frameWidth.coerceAtLeast(1)
                            val scaleY = size.height / visionState.frameHeight.coerceAtLeast(1)

                            // 1. Draw all candidate stars (small dashed circles)
                            visionState.detectedStars.forEach { star ->
                                if (!star.isTarget && !star.isComparison) {
                                    val sx = star.x * scaleX
                                    val sy = star.y * scaleY
                                    drawCircle(
                                        color = Color.Yellow.copy(alpha = 0.4f),
                                        radius = 8f,
                                        center = Offset(sx, sy),
                                        style = Stroke(width = 1f)
                                    )
                                }
                            }

                            // 2. Draw Comparison stars (blue aperture circles)
                            visionState.comparisonStars.forEachIndexed { idx, comp ->
                                val sx = comp.x * scaleX
                                val sy = comp.y * scaleY
                                val apR = visionState.apertureRadiusPix * scaleX

                                drawCircle(
                                    color = AccentCyan,
                                    radius = apR,
                                    center = Offset(sx, sy),
                                    style = Stroke(width = 1.8f)
                                )
                                // Crosshair
                                drawLine(
                                    color = AccentCyan.copy(alpha = 0.6f),
                                    start = Offset(sx - apR - 4f, sy),
                                    end = Offset(sx + apR + 4f, sy),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = AccentCyan.copy(alpha = 0.6f),
                                    start = Offset(sx, sy - apR - 4f),
                                    end = Offset(sx, sy + apR + 4f),
                                    strokeWidth = 1f
                                )
                            }

                            // 3. Draw Target Star (green aperture circle + sky background annular ring)
                            val target = visionState.targetStar
                            if (target != null) {
                                val tx = target.x * scaleX
                                val ty = target.y * scaleY
                                val apR = visionState.apertureRadiusPix * scaleX
                                val skyInR = visionState.skyInnerRadiusPix * scaleX
                                val skyOutR = visionState.skyOuterRadiusPix * scaleX

                                // Target aperture
                                drawCircle(
                                    color = AccentGreen,
                                    radius = apR,
                                    center = Offset(tx, ty),
                                    style = Stroke(width = 2.2f)
                                )
                                // Sky Inner ring
                                drawCircle(
                                    color = Color.Yellow.copy(alpha = 0.5f),
                                    radius = skyInR,
                                    center = Offset(tx, ty),
                                    style = Stroke(width = 1f)
                                )
                                // Sky Outer ring
                                drawCircle(
                                    color = Color.Yellow.copy(alpha = 0.5f),
                                    radius = skyOutR,
                                    center = Offset(tx, ty),
                                    style = Stroke(width = 1f)
                                )
                                // Precision target reticle
                                drawLine(
                                    color = AccentGreen,
                                    start = Offset(tx - skyOutR - 6f, ty),
                                    end = Offset(tx + skyOutR + 6f, ty),
                                    strokeWidth = 1.2f
                                )
                                drawLine(
                                    color = AccentGreen,
                                    start = Offset(tx, ty - skyOutR - 6f),
                                    end = Offset(tx, ty + skyOutR + 6f),
                                    strokeWidth = 1.2f
                                )
                            }
                        }

                        // HUD Text overlay (bottom-left)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            val target = visionState.targetStar
                            if (target != null) {
                                Text(
                                    "ȚINTĂ: X=${String.format("%.1f", target.x)} Y=${String.format("%.1f", target.y)} | FWHM=${String.format("%.1f", target.fwhmPix)}px",
                                    color = AccentGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Flux Net: ${target.netFlux.toInt()} ADU | SNR: ${String.format("%.1f", target.snr)}σ",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("Apasă pe o stea pentru a seta Ținta exoplanetară.", color = AccentAmber, fontSize = 10.sp)
                            }
                            Text(
                                "Derivă Montură: ΔX=${String.format("%+.2f", visionState.mountDriftTotalX)}px, ΔY=${String.format("%+.2f", visionState.mountDriftTotalY)}px",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        visionState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 3. REAL-TIME LIVE PHOTOMETRIC LIGHT CURVE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CURBĂ DE LUMINĂ ÎN TIMP REAL (MĂSURĂTORI SENZOR)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            Text(
                                "Flux Relativ Normalizat = F_țintă / Σ F_comparație",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (chartMode == 1) AccentCyan.copy(alpha = 0.25f) else DarkBackground,
                                border = BorderStroke(1.dp, if (chartMode == 1) AccentCyan else DarkBorder),
                                modifier = Modifier.clickable { chartMode = 1 }
                            ) {
                                Text(
                                    "🌐 Recharts",
                                    color = if (chartMode == 1) AccentCyan else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (chartMode == 0) AccentCyan.copy(alpha = 0.25f) else DarkBackground,
                                border = BorderStroke(1.dp, if (chartMode == 0) AccentCyan else DarkBorder),
                                modifier = Modifier.clickable { chartMode = 0 }
                            ) {
                                Text(
                                    "📈 Canvas",
                                    color = if (chartMode == 0) AccentCyan else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (visionState.isTransitDetected) AccentGreen.copy(alpha = 0.2f) else DarkBorder
                            ) {
                                Text(
                                    if (visionState.isTransitDetected) "TRANZIT DETECTAT (${String.format("%.0f", visionState.measuredTransitDepthPpm)} ppm)" else "MONITORIZARE",
                                    color = if (visionState.isTransitDetected) AccentGreen else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (chartMode == 1) {
                        // Interactive Recharts Component
                        RechartsLightCurveSampleView(
                            samples = visionState.photometrySamples,
                            periodicResult = visionState.periodicDetectionResult,
                            targetName = "Cadre FITS / Senzor Telescop",
                            modifier = Modifier.height(300.dp)
                        )
                    } else {
                        // Light Curve Chart Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                val samples = visionState.photometrySamples
                                val w = size.width
                                val h = size.height

                                // Draw baseline y = 1.000 line
                                val yCenter = h * 0.35f
                                drawLine(
                                    color = TextSecondary.copy(alpha = 0.3f),
                                    start = Offset(0f, yCenter),
                                    end = Offset(w, yCenter),
                                    strokeWidth = 1.5f
                                )

                                // Y-axis labels / ticks
                                val yBottom = h * 0.85f
                                drawLine(
                                    color = DarkBorder,
                                    start = Offset(0f, yBottom),
                                    end = Offset(w, yBottom),
                                    strokeWidth = 1f
                                )

                                if (samples.size >= 2) {
                                    val minTime = samples.first().timeMinutes
                                    val maxTime = samples.last().timeMinutes.coerceAtLeast(minTime + 0.1)

                                    val minFlux = 0.970
                                    val maxFlux = 1.015

                                    // Draw transit dip line if measured
                                    if (visionState.measuredTransitDepthPpm > 0.0) {
                                        val dipFraction = visionState.measuredTransitDepthPpm / 1_000_000.0
                                        val dipY = yCenter + ((dipFraction / (maxFlux - minFlux)) * h).toFloat()
                                        drawLine(
                                            color = AccentGreen.copy(alpha = 0.5f),
                                            start = Offset(0f, dipY),
                                            end = Offset(w, dipY),
                                            strokeWidth = 1f
                                        )
                                    }

                                    // Plot data points with error bars
                                    val pointPath = Path()
                                    samples.forEachIndexed { i, pt ->
                                        val xNorm = ((pt.timeMinutes - minTime) / (maxTime - minTime)).toFloat().coerceIn(0f, 1f)
                                        val px = xNorm * w
                                        val yNorm = ((maxFlux - pt.normalizedFlux) / (maxFlux - minFlux)).toFloat().coerceIn(0f, 1f)
                                        val py = yNorm * h

                                        // Error bar
                                        val errNorm = (pt.fluxError / (maxFlux - minFlux)).toFloat() * h
                                        drawLine(
                                            color = AccentCyan.copy(alpha = 0.4f),
                                            start = Offset(px, py - errNorm),
                                            end = Offset(px, py + errNorm),
                                            strokeWidth = 1f
                                        )

                                        // Point circle
                                        drawCircle(
                                            color = if (pt.normalizedFlux < 0.990) AccentGreen else AccentCyan,
                                            radius = 3.5f,
                                            center = Offset(px, py)
                                        )

                                        if (i == 0) pointPath.moveTo(px, py) else pointPath.lineTo(px, py)
                                    }

                                    drawPath(
                                        path = pointPath,
                                        color = AccentCyan.copy(alpha = 0.25f),
                                        style = Stroke(width = 1.5f)
                                    )
                                }
                            }

                            if (visionState.photometrySamples.isEmpty()) {
                                Text(
                                    "Așteptare cadre senzor...",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transit Inversion Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge(
                            label = "ADÂNCIME (ppm)",
                            value = "${String.format("%.0f", visionState.measuredTransitDepthPpm)} ppm",
                            color = AccentGreen
                        )
                        MetricBadge(
                            label = "RAZĂ PLANETĂ",
                            value = "${String.format("%.2f", visionState.measuredPlanetRadiusEarth)} R⊕",
                            color = AccentCyan
                        )
                        MetricBadge(
                            label = "SNR DETECȚIE",
                            value = "${String.format("%.1f", visionState.transitSnr)} σ",
                            color = if (visionState.transitSnr >= 4.0) AccentGreen else AccentAmber
                        )
                        MetricBadge(
                            label = "CADRE",
                            value = "${visionState.totalFramesProcessed}",
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4. PERIODIC TRANSIT ANALYSIS & BLS PERIODOGRAM
        item {
            PeriodicTransitAnalysisCard(
                result = visionState.periodicDetectionResult,
                onRunScan = onRunPeriodicBlsScan,
                dataPoints = visionState.photometrySamples.map {
                    PhotometricDataPoint(it.timeMinutes, it.bjd, it.normalizedFlux, it.fluxError)
                }
            )
        }

        // 5. PHOTOMETRIC CONTROLS & FITS BATCH LOADER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "COMENZI ACHIZIȚIE & IMPORT FITS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (visionState.isRunning) onStopPipeline() else onStartPipeline(visionState.source)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (visionState.isRunning) AccentRed else AccentGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (visionState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (visionState.isRunning) "Pauză" else "Start Video", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { fitsPickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                            border = BorderStroke(1.dp, AccentCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Încarcă FITS", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetPhotometry,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Curba")
                        }

                        Button(
                            onClick = onTransferToOfficialSession,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trimite ETD/AAVSO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenFitsSummary,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📋 Deschide Sumar Metadate Header FITS & Recharts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Aperture Geometry Expansion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showApertureSettings = !showApertureSettings },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Parametri Apertură Sintetică (R_ap, Inel Fond Cer)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan
                        )
                        Icon(
                            if (showApertureSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AccentCyan
                        )
                    }

                    if (showApertureSettings) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Rază Apertură: ${apertureSlider.toInt()} px",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Slider(
                            value = apertureSlider,
                            onValueChange = {
                                apertureSlider = it
                                onUpdateApertureRadii(apertureSlider, skyInSlider, skyOutSlider)
                            },
                            valueRange = 3f..20f,
                            colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                        )

                        Text(
                            "Inel Interior Cer: ${skyInSlider.toInt()} px | Inel Exterior: ${skyOutSlider.toInt()} px",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Slider(
                            value = skyOutSlider,
                            onValueChange = {
                                skyOutSlider = it
                                skyInSlider = (it - 6f).coerceAtLeast(apertureSlider + 2f)
                                onUpdateApertureRadii(apertureSlider, skyInSlider, skyOutSlider)
                            },
                            valueRange = 10f..40f,
                            colors = SliderDefaults.colors(thumbColor = Color.Yellow, activeTrackColor = Color.Yellow)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String, color: Color) {
    Surface(
        color = DarkBackground,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
