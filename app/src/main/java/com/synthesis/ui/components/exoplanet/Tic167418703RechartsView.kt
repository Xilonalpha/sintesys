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
import com.synthesis.domain.model.xilon.Tic167418703PhotometryGenerator
import com.synthesis.domain.model.xilon.Tic167418703ReportData
import com.synthesis.ui.theme.*
import java.util.Locale
import kotlin.math.abs

enum class TicRenderEngine(val label: String, val icon: String) {
    RECHARTS("Recharts Web", "🌐"),
    NATIVE_CANVAS("Canvas Nativ", "⚡")
}

enum class TicAnalysisTab(val label: String, val icon: String) {
    TRANSIT_DEPTH("Adâncime (4200 ppm)", "📉"),
    PROFILE_SYMMETRY("Simetrie (2.09σ)", "⚖️"),
    JSON_REPORT("Raport & JSON", "📋")
}

/**
 * Composable view that uses Recharts (via an embedded, crash-protected WebView)
 * to render the transit depth (4200 ppm) and profile symmetry (delta sigma = 2.09)
 * of TIC 167418703 based on the CTOI candidate JSON data.
 *
 * Includes dual-mode support (Recharts Web + Native Compose Canvas) and full
 * resilience against emulator hardware acceleration and renderer termination errors.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Tic167418703RechartsView(
    modifier: Modifier = Modifier,
    reportData: Tic167418703ReportData = Tic167418703ReportData.DEFAULT,
    onOpenInXilonScan: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedEngine by remember { mutableStateOf(TicRenderEngine.RECHARTS) }
    var selectedTab by remember { mutableStateOf(TicAnalysisTab.TRANSIT_DEPTH) }
    var hasRendererCrashed by remember { mutableStateOf(false) }

    val transitPoints = remember(reportData) {
        Tic167418703PhotometryGenerator.generateTransitPoints(reportData)
    }
    val symmetryPoints = remember(reportData) {
        Tic167418703PhotometryGenerator.generateSymmetryPoints(reportData)
    }

    val htmlContent = remember(reportData) {
        Tic167418703RechartsHtmlBuilder.buildHtml(reportData)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tic_167418703_recharts_view"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
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
                            color = Color(0xFFFF1744).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF1744))
                        ) {
                            Text(
                                "CTOI CANDIDATE",
                                color = Color(0xFFFF1744),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "TESS Sector 19",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "TIC 167418703 (Candidat Virgin • Fără TOI)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Render Engine Switcher (Recharts vs Native Canvas)
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        TicRenderEngine.values().forEach { engine ->
                            val isSelected = selectedEngine == engine
                            Surface(
                                onClick = {
                                    selectedEngine = engine
                                    hasRendererCrashed = false
                                },
                                color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("engine_${engine.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(engine.icon, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        engine.label,
                                        color = if (isSelected) AccentCyan else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Metrics Bar (Depth, Symmetry, SNR, Radius)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricChip(
                    title = "ADÂNCIME (δ)",
                    value = "${reportData.transitObservationalParameters.depthPpm.toInt()} ppm",
                    sub = "${reportData.transitObservationalParameters.depthPpt} ppt",
                    color = AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "SIMETRIE Δσ",
                    value = "${String.format(Locale.US, "%.2f", reportData.transitObservationalParameters.profileSymmetryDeltaSigma)}σ",
                    sub = "Planetary (<3σ)",
                    color = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "SNR TRANZIT",
                    value = "${String.format(Locale.US, "%.1f", reportData.transitObservationalParameters.transitSnrSigma)}σ",
                    sub = "Semnal Masiv",
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "RAZĂ PLANETĂ",
                    value = "${reportData.derivedPlanetaryCharacteristics.planetRadiusEarth} R⊕",
                    sub = "Jovian / Gigant",
                    color = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Sub-tabs Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TicAnalysisTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { selectedTab = tab },
                        color = if (isSelected) AccentCyan.copy(alpha = 0.18f) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) AccentCyan else Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tab.icon, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                tab.label,
                                color = if (isSelected) AccentCyan else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Main Visualizer Container (Recharts or Native Canvas or JSON)
            when (selectedTab) {
                TicAnalysisTab.TRANSIT_DEPTH,
                TicAnalysisTab.PROFILE_SYMMETRY -> {
                    if (selectedEngine == TicRenderEngine.RECHARTS) {
                        if (hasRendererCrashed) {
                            // Crash Fallback UI
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                color = Color(0xFF0B132B),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("⚡", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Randare Protejată MESA / Chromium",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Procesul grafic al WebView a fost securizat. Comută pe Canvas Nativ pentru randare hardware fără dependențe externe.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { selectedEngine = TicRenderEngine.NATIVE_CANVAS },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Deschide Canvas Nativ", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { hasRendererCrashed = false },
                                            border = BorderStroke(1.dp, AccentCyan),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Reîncearcă Recharts", color = AccentCyan, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Recharts WebView embedding
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                color = Color(0xFF070B14),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                                            setBackgroundColor(0xFF070B14.toInt())
                                            webViewClient = object : WebViewClient() {
                                                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                                    hasRendererCrashed = true
                                                    try {
                                                        view?.destroy()
                                                    } catch (_: Exception) {}
                                                    return true
                                                }
                                            }
                                            loadDataWithBaseURL(
                                                "https://tic167418703.recharts.astro/",
                                                htmlContent,
                                                "text/html",
                                                "UTF-8",
                                                null
                                            )
                                        }
                                    },
                                    update = { webView ->
                                        webView.loadDataWithBaseURL(
                                            "https://tic167418703.recharts.astro/",
                                            htmlContent,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        // Native Compose Canvas Visualizer
                        NativeTicCanvasVisualizer(
                            tab = selectedTab,
                            transitPoints = transitPoints,
                            symmetryPoints = symmetryPoints,
                            reportData = reportData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        )
                    }
                }

                TicAnalysisTab.JSON_REPORT -> {
                    // Raw JSON & CTOI Report Table
                    Tic167418703JsonCard(
                        reportData = reportData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Scientific Validation Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B132B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(AccentGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "VETTING STATUS: SIMETRIE & TRANZIT CONFIRMATE (2.09σ)",
                            color = AccentGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Δσ = 2.09 (prag binar critic > 3.0σ). Eclipsă secundară = 0.0 ppm. Semnal neînregistrat NASA TOI — Teren Virgin.",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TIC 167418703 CTOI JSON", Tic167418703ReportData.RAW_JSON_REPORT)
                        cm.setPrimaryClip(clip)
                        Toast.makeText(context, "Raport JSON TIC 167418703 copiat în clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiază JSON", fontSize = 11.sp)
                }

                if (onOpenInXilonScan != null) {
                    Button(
                        onClick = { onOpenInXilonScan("167418703") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanează în Xilon", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0B1120),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text(sub, color = TextSecondary.copy(alpha = 0.8f), fontSize = 8.sp)
        }
    }
}

/**
 * High-performance Native Jetpack Compose Canvas rendering of the TIC 167418703
 * Transit Depth and Profile Symmetry curves.
 */
@Composable
private fun NativeTicCanvasVisualizer(
    tab: TicAnalysisTab,
    transitPoints: List<com.synthesis.domain.model.xilon.Tic167418703PhotometryPoint>,
    symmetryPoints: List<com.synthesis.domain.model.xilon.Tic167418703SymmetryPoint>,
    reportData: Tic167418703ReportData,
    modifier: Modifier = Modifier
) {
    var hoveredPointIndex by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = modifier,
        color = Color(0xFF090E1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {

            // Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(tab, transitPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val paddingX = 40f
                            val chartW = width - (paddingX * 2f)
                            if (tab == TicAnalysisTab.TRANSIT_DEPTH && transitPoints.isNotEmpty()) {
                                val ratio = ((offset.x - paddingX) / chartW).coerceIn(0f, 1f)
                                val idx = (ratio * (transitPoints.size - 1)).toInt()
                                hoveredPointIndex = idx
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val padL = 40f
                    val padR = 20f
                    val padT = 20f
                    val padB = 30f
                    val plotW = w - padL - padR
                    val plotH = h - padT - padB

                    // Grid lines (Flux: 0.9940 to 1.0020)
                    val minFlux = 0.9940f
                    val maxFlux = 1.0020f
                    val fluxRange = maxFlux - minFlux

                    fun fluxToY(f: Float): Float {
                        val norm = (f - minFlux) / fluxRange
                        return padT + (1f - norm) * plotH
                    }

                    // Horizontal Grid
                    listOf(1.0000f, 0.9980f, 0.9958f, 0.9940f).forEach { fl ->
                        val y = fluxToY(fl)
                        drawLine(
                            color = if (fl == 0.9958f) Color(0xFFFF1744).copy(alpha = 0.6f) else Color(0xFF1E293B),
                            start = Offset(padL, y),
                            end = Offset(w - padR, y),
                            strokeWidth = if (fl == 0.9958f) 2f else 1f,
                            pathEffect = if (fl == 0.9958f) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
                        )
                    }

                    // Vertical Center Line
                    val midX = padL + plotW / 2f
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                        start = Offset(midX, padT),
                        end = Offset(midX, h - padB),
                        strokeWidth = 1.5f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    )

                    if (tab == TicAnalysisTab.TRANSIT_DEPTH) {
                        // Plot Transit Depth
                        val minT = -2.88f
                        val maxT = 2.88f
                        val tRange = maxT - minT

                        fun timeToX(t: Float): Float {
                            val norm = (t - minT) / tRange
                            return padL + norm * plotW
                        }

                        // Transit Area Shading
                        val ingressX = timeToX(-1.6f)
                        val egressX = timeToX(1.6f)
                        drawRect(
                            color = Color(0xFF00E5FF).copy(alpha = 0.06f),
                            topLeft = Offset(ingressX, padT),
                            size = androidx.compose.ui.geometry.Size(egressX - ingressX, plotH)
                        )

                        // Scatter Photons
                        transitPoints.forEach { pt ->
                            val x = timeToX(pt.phaseHours.toFloat())
                            val y = fluxToY(pt.normalizedFlux.toFloat())
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                                radius = 3.5f,
                                center = Offset(x, y)
                            )
                        }

                        // Mandel-Agol Fitted Model Line
                        val modelPath = Path()
                        transitPoints.forEachIndexed { i, pt ->
                            val x = timeToX(pt.phaseHours.toFloat())
                            val y = fluxToY(pt.modelFlux.toFloat())
                            if (i == 0) modelPath.moveTo(x, y) else modelPath.lineTo(x, y)
                        }
                        drawPath(
                            path = modelPath,
                            color = Color(0xFFFFD600),
                            style = Stroke(width = 3.5f)
                        )
                    } else {
                        // Plot Profile Symmetry (Ingress vs Mirrored Egress)
                        val maxDist = 2.4f
                        fun distToX(d: Float): Float {
                            return padL + (d / maxDist) * plotW
                        }

                        val ingressPath = Path()
                        val egressPath = Path()

                        symmetryPoints.forEachIndexed { i, pt ->
                            val x = distToX(pt.timeFromMidTransitHours.toFloat())
                            val yIn = fluxToY(pt.ingressFlux.toFloat())
                            val yEg = fluxToY(pt.egressMirroredFlux.toFloat())

                            if (i == 0) {
                                ingressPath.moveTo(x, yIn)
                                egressPath.moveTo(x, yEg)
                            } else {
                                ingressPath.lineTo(x, yIn)
                                egressPath.lineTo(x, yEg)
                            }

                            // Scatter points
                            drawCircle(color = Color(0xFF00E5FF), radius = 3f, center = Offset(x, yIn))
                            drawCircle(color = Color(0xFFFF9100), radius = 3f, center = Offset(x, yEg))
                        }

                        drawPath(ingressPath, color = Color(0xFF00E5FF), style = Stroke(width = 3f))
                        drawPath(
                            egressPath,
                            color = Color(0xFFFF9100),
                            style = Stroke(
                                width = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )
                        )
                    }
                }
            }

            // Legend Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab == TicAnalysisTab.TRANSIT_DEPTH) {
                    LegendItem(color = Color(0xFF38BDF8), label = "Fotoni TESS (Flux)")
                    LegendItem(color = Color(0xFFFFD600), label = "Model TLS (Mandel-Agol)")
                    LegendItem(color = Color(0xFFFF1744), label = "Prag 4200 ppm (0.9958)")
                } else {
                    LegendItem(color = Color(0xFF00E5FF), label = "Panta Ingress (t < 0)")
                    LegendItem(color = Color(0xFFFF9100), label = "Panta Egress Oglindită (t > 0)")
                    LegendItem(color = Color(0xFF00E676), label = "Simetric (Δσ = 2.09)")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}

/**
 * Detailed JSON and Astrometric Parameters Card for TIC 167418703.
 */
@Composable
private fun Tic167418703JsonCard(
    reportData: Tic167418703ReportData,
    modifier: Modifier = Modifier
) {
    var showRawJson by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = Color(0xFF090E1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (showRawJson) "STRUCTURĂ RAW JSON (CTOI REPORT)" else "TABEL PARAMETRI FOTOMETRICI",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showRawJson = !showRawJson }) {
                    Text(
                        if (showRawJson) "Vezi Tabel" else "Vezi JSON Raw",
                        color = AccentAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (showRawJson) {
                Surface(
                    color = Color(0xFF070B14),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        Tic167418703ReportData.RAW_JSON_REPORT,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ParamRow("TIC ID", reportData.targetIdentifiers.ticId)
                    ParamRow("Nume Canonic", reportData.targetIdentifiers.canonicalName)
                    ParamRow("Sector TESS", reportData.targetIdentifiers.sector)
                    ParamRow("Coordonate RA / DEC", "${reportData.targetIdentifiers.raSexagesimal} / ${reportData.targetIdentifiers.decSexagesimal}")
                    ParamRow("Perioadă Orbitală", "${reportData.transitObservationalParameters.periodDays} zile")
                    ParamRow("Durată Tranzit", "${reportData.transitObservationalParameters.durationHours} ore")
                    ParamRow("Adâncime Tranzit (δ)", "${reportData.transitObservationalParameters.depthPpm} ppm (${reportData.transitObservationalParameters.depthPpt} ppt)")
                    ParamRow("Simetrie Profil (Ingress/Egress)", "${reportData.transitObservationalParameters.profileSymmetryDeltaSigma} sigma (Planetary OK)")
                    ParamRow("Eclipsă Secundară", "${reportData.transitObservationalParameters.secondaryEclipsePpm} ppm (Fără semnal)")
                    ParamRow("Rază Planetă", "${reportData.derivedPlanetaryCharacteristics.planetRadiusEarth} R⊕ (Jovian)")
                    ParamRow("Masă Planetă Estimată", "${reportData.derivedPlanetaryCharacteristics.estimatedMassEarth} M⊕")
                    ParamRow("Densitate Medie", "${reportData.derivedPlanetaryCharacteristics.bulkDensityGCm3} g/cm³")
                    ParamRow("Dispoziție Oficială", reportData.vettingAndArchivalCrossMatch.officialDisposition)
                }
            }
        }
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 9.5.sp)
        Text(value, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
