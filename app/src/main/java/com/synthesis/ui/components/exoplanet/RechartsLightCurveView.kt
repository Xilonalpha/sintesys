package com.synthesis.ui.components.exoplanet

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.synthesis.domain.model.exoplanet.FramePhotometrySample
import com.synthesis.domain.model.exoplanet.PeriodicTransitDetectionResult
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
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
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(dataPoints, periodicResult, targetName) {
        RechartsLightCurveHtmlBuilder.buildHtml(
            dataPoints = dataPoints,
            periodicResult = periodicResult,
            targetName = targetName
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
                    webViewClient = WebViewClient()
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
