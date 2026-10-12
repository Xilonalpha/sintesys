package com.synthesis.ui.components.exoplanet

import android.annotation.SuppressLint
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.synthesis.domain.model.exoplanet.FramePhotometrySample
import com.synthesis.domain.model.exoplanet.PeriodicTransitDetectionResult
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
import com.synthesis.domain.model.exoplanet.TemplatePhasePoint
import com.synthesis.ui.screens.RechartsLightCurveHtmlBuilder
import com.synthesis.ui.theme.DarkBorder
import com.synthesis.ui.theme.DarkSurface

/**
 * Android Jetpack Compose Data Visualization component embedding an interactive
 * React + Recharts chart to plot FITS photometric light curve time-series data
 * and highlight identified periodic brightness drops.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsLightCurveView(
    dataPoints: List<PhotometricDataPoint>,
    periodicResult: PeriodicTransitDetectionResult?,
    targetName: String = "Sursă FITS CCD/CMOS",
    templatePoints: List<TemplatePhasePoint>? = null,
    templateName: String? = null,
    templateColorHex: String? = null,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(dataPoints, periodicResult, targetName, templatePoints, templateName, templateColorHex) {
        RechartsLightCurveHtmlBuilder.buildHtml(
            dataPoints = dataPoints,
            periodicResult = periodicResult,
            targetName = targetName,
            templatePoints = templatePoints,
            templateName = templateName,
            templateColorHex = templateColorHex
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        var hasRendererCrashed by remember { mutableStateOf(false) }

        if (hasRendererCrashed) {
            Box(
                modifier = Modifier.fillMaxWidth().height(380.dp).padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Randare Protejată • Mod Software", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("WebView a întâmpinat o eroare de accelerator grafic. Poți folosi modul Canvas Nativ.", color = Color.Gray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { hasRendererCrashed = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Reîncearcă Recharts", color = Color.Black, fontSize = 11.sp)
                    }
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(380.dp),
                factory = { context ->
                    WebView(context).apply {
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
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
                            "https://fits.recharts.astro/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "https://fits.recharts.astro/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            )
        }
    }
}

/**
 * Overload for FramePhotometrySample lists.
 */
@Composable
fun RechartsLightCurveSampleView(
    samples: List<FramePhotometrySample>,
    periodicResult: PeriodicTransitDetectionResult?,
    targetName: String = "Cadre FITS Senzor",
    modifier: Modifier = Modifier
) {
    val points = remember(samples) {
        samples.map { s ->
            PhotometricDataPoint(
                minuteOffset = s.timeMinutes,
                bjdTime = s.bjd,
                relativeFlux = s.normalizedFlux,
                fluxError = s.fluxError
            )
        }
    }

    RechartsLightCurveView(
        dataPoints = points,
        periodicResult = periodicResult,
        targetName = targetName,
        modifier = modifier
    )
}

/**
 * Interactive Recharts chart specifically for Xilon Detection Result imported via TIC ID
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XilonDetectionRechartsView(
    res: com.synthesis.domain.model.xilon.XilonDetectionResult,
    dipReport: com.synthesis.domain.model.exoplanet.TransitDipScanReport? = null,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(res, dipReport) {
        RechartsLightCurveHtmlBuilder.buildHtmlFromXilonDetection(res, dipReport)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        var hasRendererCrashed by remember { mutableStateOf(false) }

        if (hasRendererCrashed) {
            Box(
                modifier = Modifier.fillMaxWidth().height(350.dp).padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Randare Protejată Xilon", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Procesul grafic al WebView a fost securizat. Comută pe 'Canvas Nativ' pentru vizualizare fără latență.", color = Color.Gray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { hasRendererCrashed = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Reîncearcă Recharts", color = Color.Black, fontSize = 11.sp)
                    }
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                factory = { context ->
                    WebView(context).apply {
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
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
                            "https://xilon.recharts.astro/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "https://xilon.recharts.astro/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            )
        }
    }
}

