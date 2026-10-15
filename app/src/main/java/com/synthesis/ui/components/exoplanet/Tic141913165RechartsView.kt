package com.synthesis.ui.components.exoplanet

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.synthesis.domain.model.xilon.Tic141913165PhotometryGenerator
import com.synthesis.domain.model.xilon.Tic141913165ReportData
import com.synthesis.ui.theme.*
import java.util.Locale
import kotlin.math.abs

enum class Tic1419AnalysisTab(val label: String, val icon: String) {
    TRANSIT_DEPTH("Adâncime (1600 ppm)", "📉"),
    PROFILE_SYMMETRY("Simetrie (0.598σ)", "⚖️"),
    JSON_REPORT("Raport & JSON", "📋")
}

/**
 * Composable view that uses Recharts (via an embedded, crash-protected WebView)
 * to render the transit depth (1600 ppm) and profile symmetry (delta sigma = 0.598)
 * of TIC 141913165 based on the CTOI candidate JSON data.
 *
 * Includes dual-mode support (Recharts Web + Native Compose Canvas) and full
 * resilience against emulator hardware acceleration and renderer termination errors.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Tic141913165RechartsView(
    modifier: Modifier = Modifier,
    reportData: Tic141913165ReportData = Tic141913165ReportData.DEFAULT,
    onOpenInXilonScan: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedEngine by remember { mutableStateOf(TicRenderEngine.RECHARTS) }
    var selectedTab by remember { mutableStateOf(Tic1419AnalysisTab.TRANSIT_DEPTH) }
    var hasRendererCrashed by remember { mutableStateOf(false) }

    val transitPoints = remember(reportData) {
        Tic141913165PhotometryGenerator.generateTransitPoints(reportData)
    }
    val symmetryPoints = remember(reportData) {
        Tic141913165PhotometryGenerator.generateSymmetryPoints(reportData)
    }

    val htmlContent = remember(reportData) {
        Tic141913165RechartsHtmlBuilder.buildHtml(reportData)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tic_141913165_recharts_view"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // 1. Header with Target Name, Badges & Engine Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            Text(
                                "CTOI CANDIDATE",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "TESS Sector 6 (s0006)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "TIC 141913165 (Candidat Virgin • Fără TOI)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Engine Toggle
                Row(
                    modifier = Modifier
                        .background(DarkBackground, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TicRenderEngine.values().forEach { engine ->
                        val isSelected = selectedEngine == engine
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) AccentCyan else Color.Transparent,
                            modifier = Modifier
                                .clickable { selectedEngine = engine }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${engine.icon} ${engine.label}",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Quick Scientific Metrics Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TicMetricBox(
                    title = "ADÂNCIME (δ)",
                    value = "${reportData.transitObservationalParameters.depthPpm.toInt()} ppm",
                    subValue = "1.60 ppt",
                    accentColor = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                TicMetricBox(
                    title = "SIMETRIE Δσ",
                    value = String.format(Locale.US, "%.3fσ", reportData.transitObservationalParameters.profileSymmetryDeltaSigma),
                    subValue = "< 4.0σ Simetric",
                    accentColor = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                TicMetricBox(
                    title = "SNR TRANZIT",
                    value = String.format(Locale.US, "%.1fσ", reportData.transitObservationalParameters.transitSnrSigma),
                    subValue = "Prag > 7.1σ",
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                TicMetricBox(
                    title = "RAZĂ R⊕",
                    value = String.format(Locale.US, "%.2f R⊕", reportData.derivedPlanetaryCharacteristics.planetRadiusEarth),
                    subValue = "Sub-Saturn",
                    accentColor = Color(0xFFFFD600),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Tic1419AnalysisTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = tab },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkBackground,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AccentCyan else Color(0xFF1E293B)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tab.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AccentCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Primary Visualization Area
            when (selectedTab) {
                Tic1419AnalysisTab.TRANSIT_DEPTH -> {
                    if (selectedEngine == TicRenderEngine.RECHARTS && !hasRendererCrashed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                            cacheMode = WebSettings.LOAD_NO_CACHE
                                        }
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        webViewClient = object : WebViewClient() {
                                            override fun onRenderProcessGone(
                                                view: WebView?,
                                                detail: RenderProcessGoneDetail?
                                            ): Boolean {
                                                hasRendererCrashed = true
                                                selectedEngine = TicRenderEngine.NATIVE_CANVAS
                                                return true
                                            }
                                        }
                                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Native Compose Canvas
                        Tic1419TransitCanvasChart(
                            transitPoints = transitPoints,
                            reportData = reportData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                        )
                    }
                }
                Tic1419AnalysisTab.PROFILE_SYMMETRY -> {
                    if (selectedEngine == TicRenderEngine.RECHARTS && !hasRendererCrashed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                                .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                        }
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        webViewClient = object : WebViewClient() {
                                            override fun onRenderProcessGone(
                                                view: WebView?,
                                                detail: RenderProcessGoneDetail?
                                            ): Boolean {
                                                hasRendererCrashed = true
                                                selectedEngine = TicRenderEngine.NATIVE_CANVAS
                                                return true
                                            }
                                        }
                                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Tic1419SymmetryCanvasChart(
                            symmetryPoints = symmetryPoints,
                            reportData = reportData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                        )
                    }
                }
                Tic1419AnalysisTab.JSON_REPORT -> {
                    Tic1419JsonCard(
                        reportData = reportData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Scientific Astrophysical Breakdown Card (Chen & Kipping)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "PARAMETRI DERIVAȚI (CHEN & KIPPING 2017) • SUB-SATURN",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Rază Planetară (Rp):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.3f", reportData.derivedPlanetaryCharacteristics.planetRadiusEarth)} R⊕",
                            color = Color(0xFFFFD600),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Masă Estimată (Mp):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.2f", reportData.derivedPlanetaryCharacteristics.estimatedMassEarth)} M⊕",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Densitate Medie Bulk (ρ):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.3f", reportData.derivedPlanetaryCharacteristics.bulkDensityGCm3)} g/cm³ (Saturn = 0.687)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Gravitație la Suprafață:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.3f", reportData.derivedPlanetaryCharacteristics.surfaceGravityG)} g⊕",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Viteză de Evadare:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.2f", reportData.derivedPlanetaryCharacteristics.escapeVelocityKmS)} km/s",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Temperatură de Echilibru (Teq):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.1f", reportData.derivedPlanetaryCharacteristics.equilibriumTempKelvin)} K (~563.6 °C)",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Semi-Axă Mare (a):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "${String.format(Locale.US, "%.4f", reportData.derivedPlanetaryCharacteristics.semiMajorAxisAu)} AU",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Eclipsă Secundară (Occultație):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "0.0 ppm (Non-stelară)",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "☀️ Steaua Gazdă: Tip Solar G2V (5780 K), Rază 1.02 R☉, Masă 1.01 M☉, Distanță 165.4 pc. Magnitudine TESS 10.10. Stea extrem de strălucitoare în cerul sudic!",
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Action Bar: Open in Xilon Scan & Copy CTOI JSON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TIC 141913165 CTOI JSON", Tic141913165ReportData.RAW_JSON_REPORT)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Raport CTOI TIC 141913165 copiat în clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiază JSON", color = Color.White, fontSize = 11.sp)
                }

                if (onOpenInXilonScan != null) {
                    Button(
                        onClick = { onOpenInXilonScan("141913165") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanează în Xilon", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TicMetricBox(
    title: String,
    value: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DarkBackground,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                subValue,
                fontSize = 8.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * High-performance Native Jetpack Compose Canvas for TIC 141913165 transit depth.
 */
@Composable
private fun Tic1419TransitCanvasChart(
    transitPoints: List<com.synthesis.domain.model.xilon.Tic141913165PhotometryPoint>,
    reportData: Tic141913165ReportData,
    modifier: Modifier = Modifier
) {
    var hoveredPoint by remember { mutableStateOf<com.synthesis.domain.model.xilon.Tic141913165PhotometryPoint?>(null) }

    Column(
        modifier = modifier
            .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CURBĂ TRANZIT NATIVĂ (COMPOSE VECTOR)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
            if (hoveredPoint != null) {
                Text(
                    "Fază: ${hoveredPoint!!.phaseHours}h • Flux: ${hoveredPoint!!.normalizedFlux} • ${hoveredPoint!!.depthPpm.toInt()} ppm",
                    fontSize = 9.sp,
                    color = Color(0xFFFFD600),
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text("Apasă pe grafic pentru valori", fontSize = 9.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(transitPoints) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val paddingLeft = 35f
                        val paddingRight = 15f
                        val plotW = width - paddingLeft - paddingRight
                        val relX = (offset.x - paddingLeft) / plotW
                        val targetPhase = -13.65 + (relX * 27.3)
                        hoveredPoint = transitPoints.minByOrNull { abs(it.phaseHours - targetPhase) }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pLeft = 35.dp.toPx()
                val pRight = 15.dp.toPx()
                val pTop = 15.dp.toPx()
                val pBottom = 25.dp.toPx()

                val plotW = size.width - pLeft - pRight
                val plotH = size.height - pTop - pBottom

                val minFlux = 0.9975
                val maxFlux = 1.0015
                val fluxRange = maxFlux - minFlux

                val minPhase = -13.65
                val maxPhase = 13.65
                val phaseRange = maxPhase - minPhase

                fun toX(phase: Double): Float = pLeft + (((phase - minPhase) / phaseRange) * plotW).toFloat()
                fun toY(flux: Double): Float = pTop + (((maxFlux - flux) / fluxRange) * plotH).toFloat()

                // Grid lines
                val gridFluxes = listOf(0.9980, 0.9990, 1.0000, 1.0010)
                gridFluxes.forEach { f ->
                    val y = toY(f)
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(pLeft, y),
                        end = Offset(pLeft + plotW, y),
                        strokeWidth = 1f
                    )
                }

                // Ingress/Egress shaded transit window (-4.55h to +4.55h)
                val xIn = toX(-4.55)
                val xEg = toX(4.55)
                drawRect(
                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                    topLeft = Offset(xIn, pTop),
                    size = androidx.compose.ui.geometry.Size(xEg - xIn, plotH)
                )

                // Depth Reference Line (-1600 ppm -> 0.9984 flux)
                val y1600 = toY(0.9984)
                drawLine(
                    color = Color(0xFFFF1744),
                    start = Offset(pLeft, y1600),
                    end = Offset(pLeft + plotW, y1600),
                    strokeWidth = 2f
                )

                // Center Transit Line (phase = 0)
                val xCenter = toX(0.0)
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                    start = Offset(xCenter, pTop),
                    end = Offset(xCenter, pTop + plotH),
                    strokeWidth = 1.5f
                )

                // Draw Photometric Scatter Points
                transitPoints.forEach { pt ->
                    val px = toX(pt.phaseHours)
                    val py = toY(pt.normalizedFlux)
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.65f),
                        radius = 2.2.dp.toPx(),
                        center = Offset(px, py)
                    )
                }

                // Draw Smooth Model Curve
                val modelPath = Path()
                transitPoints.forEachIndexed { idx, pt ->
                    val px = toX(pt.phaseHours)
                    val py = toY(pt.modelFlux)
                    if (idx == 0) modelPath.moveTo(px, py) else modelPath.lineTo(px, py)
                }
                drawPath(
                    path = modelPath,
                    color = Color(0xFFFFD600),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Highlight hovered point
                hoveredPoint?.let { hp ->
                    val hx = toX(hp.phaseHours)
                    val hy = toY(hp.normalizedFlux)
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(hx, hy)
                    )
                    drawCircle(
                        color = Color(0xFFFF1744),
                        radius = 3.dp.toPx(),
                        center = Offset(hx, hy)
                    )
                }
            }
        }

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendIndicator(color = Color(0xFF38BDF8), label = "Fotoni TESS (Sector 6)")
                LegendIndicator(color = Color(0xFFFFD600), label = "Model TLS (1600 ppm)")
                LegendIndicator(color = Color(0xFFFF1744), label = "Adâncime 0.16%")
            }
            Text("P = 18.42z • 9.1h", fontSize = 9.sp, color = TextSecondary)
        }
    }
}

/**
 * Native Compose Canvas for Ingress vs Egress Profile Symmetry comparison (0.598σ).
 */
@Composable
private fun Tic1419SymmetryCanvasChart(
    symmetryPoints: List<com.synthesis.domain.model.xilon.Tic141913165SymmetryPoint>,
    reportData: Tic141913165ReportData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ANALIZĂ SIMETRIE INGRESS VS EGRESS (Δσ = 0.598)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676)
            )
            Surface(
                color = Color(0xFF00E676).copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "SIMETRIE EXCELENTĂ (< 4.0σ)",
                    color = Color(0xFF00E676),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val pLeft = 35.dp.toPx()
            val pRight = 15.dp.toPx()
            val pTop = 15.dp.toPx()
            val pBottom = 25.dp.toPx()

            val plotW = size.width - pLeft - pRight
            val plotH = size.height - pTop - pBottom

            val minFlux = 0.9975
            val maxFlux = 1.0010
            val fluxRange = maxFlux - minFlux

            val maxT = (reportData.transitObservationalParameters.durationHours / 2.0) * 1.4 // ~6.37h

            fun toX(t: Double): Float = pLeft + ((t / maxT) * plotW).toFloat()
            fun toY(flux: Double): Float = pTop + (((maxFlux - flux) / fluxRange) * plotH).toFloat()

            // Grid lines
            listOf(0.9980, 0.9990, 1.0000).forEach { f ->
                val y = toY(f)
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(pLeft, y),
                    end = Offset(pLeft + plotW, y),
                    strokeWidth = 1f
                )
            }

            // Ingress Path (Cyan)
            val ingressPath = Path()
            symmetryPoints.forEachIndexed { i, pt ->
                val px = toX(pt.timeFromMidTransitHours)
                val py = toY(pt.ingressFlux)
                if (i == 0) ingressPath.moveTo(px, py) else ingressPath.lineTo(px, py)
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }
            drawPath(
                path = ingressPath,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 2.dp.toPx())
            )

            // Egress Mirrored Path (Purple)
            val egressPath = Path()
            symmetryPoints.forEachIndexed { i, pt ->
                val px = toX(pt.timeFromMidTransitHours)
                val py = toY(pt.egressMirroredFlux)
                if (i == 0) egressPath.moveTo(px, py) else egressPath.lineTo(px, py)
                drawCircle(
                    color = Color(0xFFA855F7),
                    radius = 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }
            drawPath(
                path = egressPath,
                color = Color(0xFFA855F7),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendIndicator(color = Color(0xFF38BDF8), label = "Ingress (-t)")
                LegendIndicator(color = Color(0xFFA855F7), label = "Egress Oglindit (+t)")
            }
            Text("Δσ = 0.598σ (Planetar)", fontSize = 9.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 9.sp, color = TextSecondary)
    }
}

/**
 * Renders formatted JSON and Archival references card for TIC 141913165.
 */
@Composable
private fun Tic1419JsonCard(
    reportData: Tic141913165ReportData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RAPORT OFICIAL CTOI (JSON FORMAT)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA7F3D0),
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TIC 141913165 CTOI JSON", Tic141913165ReportData.RAW_JSON_REPORT)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "JSON copiat!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy JSON", tint = Color(0xFFA7F3D0), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0B132B), RoundedCornerShape(6.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    Tic141913165ReportData.RAW_JSON_REPORT,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color(0xFFA7F3D0),
                    lineHeight = 13.sp
                )
            }
        }
    }
}
