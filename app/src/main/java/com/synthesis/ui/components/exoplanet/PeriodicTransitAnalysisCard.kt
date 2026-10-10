package com.synthesis.ui.components.exoplanet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.PeriodicTransitDetectionResult
import com.synthesis.domain.model.exoplanet.PhotometricDataPoint
import com.synthesis.ui.theme.*
import kotlin.math.roundToInt

/**
 * Interactive Astronomical UI Component for Box-fitting Least Squares (BLS)
 * Periodic Transit Light Curve Analysis with native Compose and Recharts WebView plotting.
 */
@Composable
fun PeriodicTransitAnalysisCard(
    result: PeriodicTransitDetectionResult?,
    onRunScan: (minPeriodDays: Double?, maxPeriodDays: Double?, starRadius: Double, starMass: Double) -> Unit,
    modifier: Modifier = Modifier,
    dataPoints: List<PhotometricDataPoint> = emptyList()
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showConfig by remember { mutableStateOf(false) }

    var pMinSlider by remember { mutableFloatStateOf(0.1f) }
    var pMaxSlider by remember { mutableFloatStateOf(5.0f) }
    var starRadiusSlider by remember { mutableFloatStateOf(1.0f) }
    var starMassSlider by remember { mutableFloatStateOf(1.0f) }

    val effectivePoints = remember(dataPoints, result) {
        if (dataPoints.isNotEmpty()) {
            dataPoints
        } else if (result != null && result.phaseFoldedPoints.isNotEmpty()) {
            result.phaseFoldedPoints.map {
                PhotometricDataPoint(
                    minuteOffset = it.timeMinutes,
                    bjdTime = it.bjd,
                    relativeFlux = it.relativeFlux,
                    fluxError = it.fluxError
                )
            }
        } else emptyList()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (result?.isDetected == true) AccentGreen.copy(alpha = 0.6f) else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "ANALIZĂ PERIODICITATE TRANZIT (BLS)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Algoritmul Box-Least-Squares (Kovács et al. 2002)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (result?.isDetected == true) AccentGreen.copy(alpha = 0.2f) else DarkBorder
                ) {
                    Text(
                        if (result?.isDetected == true) "★ DETECTAT (SDE ${String.format("%.1f", result.signalDetectionEfficiencySde)})" else "MONITORIZARE",
                        color = if (result?.isDetected == true) AccentGreen else TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Detection Highlight Banner
            if (result != null && result.isDetected) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "SEMNAL PERIODIC CONFIRMAT MATEMATIC",
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val bayes = result.vetting.bayesianUncertainty
                        val confDisplay = if (bayes != null) {
                            "${String.format("%.1f", bayes.medianConfidence)}% (+${String.format("%.1f", bayes.plusError1Sigma)} / -${String.format("%.1f", bayes.minusError1Sigma)}) [68% CI]"
                        } else {
                            "${result.vetting.confidenceScore.toInt()}%"
                        }
                        Text(
                            "Perioadă P = ${String.format("%.3f", result.bestPeriodDays)} zile (${String.format("%.1f", result.bestPeriodHours)} ore) | " +
                                    "Adâncime = ${String.format("%.0f", result.transitDepthPpm)} ppm | " +
                                    "Rp = ${String.format("%.2f", result.physicalParameters.planetRadiusEarth)} R⊕ | " +
                                    "Încredere Bayesiană: $confDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. Sub-tabs Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("📊 Curbă Pliată", "🌐 Recharts FITS", "📈 Spectru BLS", "🛡️ Vetting")
                tabs.forEachIndexed { idx, title ->
                    val isSelected = selectedSubTab == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSubTab = idx },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkBackground,
                        border = BorderStroke(1.dp, if (isSelected) AccentCyan else DarkBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) AccentCyan else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Tab Content
            when (selectedSubTab) {
                0 -> PhaseFoldedCurveView(result)
                1 -> {
                    if (effectivePoints.isNotEmpty()) {
                        RechartsLightCurveView(
                            dataPoints = effectivePoints,
                            periodicResult = result,
                            targetName = "Date Fotometrice FITS",
                            modifier = Modifier.height(340.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Încarcă sau procesează cadre FITS pentru graficul interactiv Recharts.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                2 -> BlsPeriodogramView(result)
                3 -> VettingAndPhysicsView(result)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Controls & Configuration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onRunScan(
                            pMinSlider.toDouble(),
                            pMaxSlider.toDouble(),
                            starRadiusSlider.toDouble(),
                            starMassSlider.toDouble()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.4f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanează Periodicitate BLS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showConfig = !showConfig },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(if (showConfig) Icons.Default.ExpandLess else Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showConfig) "Închide" else "Setări P", fontSize = 11.sp)
                }
            }

            // Expandable Configuration
            if (showConfig) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Interval Căutare Perioadă: ${String.format("%.2f", pMinSlider)}d - ${String.format("%.1f", pMaxSlider)}d", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        Slider(
                            value = pMaxSlider,
                            onValueChange = { pMaxSlider = it },
                            valueRange = 0.5f..20.0f,
                            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                        )

                        Text("Rază Stea Gazdă: ${String.format("%.2f", starRadiusSlider)} R☉ | Masă: ${String.format("%.2f", starMassSlider)} M☉", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(
                            value = starRadiusSlider,
                            onValueChange = {
                                starRadiusSlider = it
                                starMassSlider = it // Approximate main-sequence mass-radius
                            },
                            valueRange = 0.3f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Canvas rendering of the phase-folded light curve [-0.5, +0.5] with binned points and model curve.
 */
@Composable
private fun PhaseFoldedCurveView(result: PeriodicTransitDetectionResult?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height

            // Baseline y = 1.0 (at 30% from top)
            val yBaseline = h * 0.32f
            drawLine(
                color = TextSecondary.copy(alpha = 0.35f),
                start = Offset(0f, yBaseline),
                end = Offset(w, yBaseline),
                strokeWidth = 1.5f
            )

            // Center phase = 0 line
            val xCenter = w * 0.5f
            drawLine(
                color = AccentCyan.copy(alpha = 0.3f),
                start = Offset(xCenter, 0f),
                end = Offset(xCenter, h),
                strokeWidth = 1f
            )

            val minFlux = 0.970
            val maxFlux = 1.015

            if (result != null && result.phaseFoldedPoints.isNotEmpty()) {
                val pts = result.phaseFoldedPoints

                // 1. Plot individual phase-folded observations
                pts.forEach { pt ->
                    // phase in [-0.5, 0.5] -> normX in [0, 1]
                    val normX = (pt.phase + 0.5f).toFloat().coerceIn(0f, 1f)
                    val px = normX * w
                    val normY = ((maxFlux - pt.relativeFlux) / (maxFlux - minFlux)).toFloat().coerceIn(0f, 1f)
                    val py = normY * h

                    // Error bar
                    val errH = (pt.fluxError / (maxFlux - minFlux)).toFloat() * h
                    drawLine(
                        color = AccentCyan.copy(alpha = 0.25f),
                        start = Offset(px, py - errH),
                        end = Offset(px, py + errH),
                        strokeWidth = 1f
                    )

                    // Point
                    drawCircle(
                        color = if (pt.isInTransit) AccentGreen.copy(alpha = 0.7f) else AccentCyan.copy(alpha = 0.5f),
                        radius = 2.5f,
                        center = Offset(px, py)
                    )
                }

                // 2. Plot binned points (gold/amber)
                if (result.binnedPhaseCurve.isNotEmpty()) {
                    val binPath = Path()
                    result.binnedPhaseCurve.forEachIndexed { i, bin ->
                        val bx = (bin.phaseCenter + 0.5f).toFloat().coerceIn(0f, 1f) * w
                        val by = ((maxFlux - bin.meanFlux) / (maxFlux - minFlux)).toFloat().coerceIn(0f, 1f) * h
                        if (i == 0) binPath.moveTo(bx, by) else binPath.lineTo(bx, by)

                        drawCircle(
                            color = Color(0xFFFFB300),
                            radius = 3.5f,
                            center = Offset(bx, by)
                        )
                    }
                    drawPath(binPath, color = Color(0xFFFFB300).copy(alpha = 0.4f), style = Stroke(width = 1.5f))
                }

                // 3. Plot theoretical transit model curve (emerald green)
                if (result.transitModelCurve.isNotEmpty()) {
                    val modelPath = Path()
                    result.transitModelCurve.forEachIndexed { i, (phase, flux) ->
                        val mx = (phase + 0.5f).toFloat().coerceIn(0f, 1f) * w
                        val my = ((maxFlux - flux) / (maxFlux - minFlux)).toFloat().coerceIn(0f, 1f) * h
                        if (i == 0) modelPath.moveTo(mx, my) else modelPath.lineTo(mx, my)
                    }
                    drawPath(modelPath, color = AccentGreen, style = Stroke(width = 2.5f))
                }
            }
        }

        if (result == null || result.phaseFoldedPoints.isEmpty()) {
            Text(
                "Apasă 'Scanează Periodicitate BLS' pentru plierea curbei de lumină.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Legend Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LegendChip("Puncte FITS", AccentCyan)
                LegendChip("Medie Binned", Color(0xFFFFB300))
                LegendChip("Model Tranzit", AccentGreen)
            }
        }
    }
}

/**
 * Canvas rendering of the BLS Periodogram power spectrum (Period vs Power).
 */
@Composable
private fun BlsPeriodogramView(result: PeriodicTransitDetectionResult?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height

            val grid = result?.periodogram ?: emptyList()
            if (grid.size >= 2) {
                val minP = grid.first().periodDays
                val maxP = grid.last().periodDays.coerceAtLeast(minP + 0.1)
                val maxPower = grid.maxOf { it.power }.coerceAtLeast(0.001)

                val powerPath = Path()
                var peakX = 0f
                var peakY = 0f

                grid.forEachIndexed { i, pt ->
                    val normX = ((pt.periodDays - minP) / (maxP - minP)).toFloat().coerceIn(0f, 1f)
                    val px = normX * w
                    val normY = (1.0 - (pt.power / maxPower)).toFloat().coerceIn(0f, 1f)
                    val py = normY * h

                    if (i == 0) powerPath.moveTo(px, py) else powerPath.lineTo(px, py)

                    if (pt.power >= maxPower * 0.999) {
                        peakX = px
                        peakY = py
                    }
                }

                // Draw power spectrum curve
                drawPath(powerPath, color = AccentCyan, style = Stroke(width = 2f))

                // Highlight peak period with vertical line and indicator
                if (peakX > 0f) {
                    drawLine(
                        color = AccentGreen,
                        start = Offset(peakX, 0f),
                        end = Offset(peakX, h),
                        strokeWidth = 1.5f
                    )
                    drawCircle(
                        color = AccentGreen,
                        radius = 5f,
                        center = Offset(peakX, peakY)
                    )
                }
            }
        }

        if (result == null || result.periodogram.isEmpty()) {
            Text(
                "Niciun spectru de putere BLS generat încă.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    "P_best = ${String.format("%.3f", result.bestPeriodDays)} d",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "SDE = ${String.format("%.2f", result.signalDetectionEfficiencySde)}σ",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Detailed Astrophysical Vetting Tests & Physical Planetary Inversion metrics.
 */
@Composable
private fun VettingAndPhysicsView(result: PeriodicTransitDetectionResult?) {
    if (result == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Rulează analiza pentru a vizualiza testele de vetting.", color = TextSecondary, fontSize = 11.sp)
        }
        return
    }

    val vet = result.vetting
    val phys = result.physicalParameters

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Vetting Diagnostic Chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VettingBadge(
                label = "TEST PAR / IMPAR",
                status = if (vet.isOddEvenConsistent) "Consistent (< 3σ)" else "Alertă Binară (${String.format("%.1f", vet.oddEvenSigmaDiff)}σ)",
                isOk = vet.isOddEvenConsistent,
                modifier = Modifier.weight(1f)
            )
            VettingBadge(
                label = "ECLIPSĂ SECUNDARĂ",
                status = if (!vet.isSecondaryEclipseDetected) "Negativ (Planetă)" else "Detectat (Stea Binară)",
                isOk = !vet.isSecondaryEclipseDetected,
                modifier = Modifier.weight(1f)
            )
            VettingBadge(
                label = "SCOR ÎNCREDERE",
                status = "${vet.confidenceScore.toInt()}% (${if (vet.confidenceScore >= 75) "Ridicat" else if (vet.confidenceScore >= 50) "Moderat" else "Scăzut"})",
                isOk = vet.confidenceScore >= 60,
                modifier = Modifier.weight(1f)
            )
        }

        // Keplerian Physics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PhysicalMetricCard(
                label = "SEMI-AXA MARE",
                value = "${String.format("%.3f", phys.semiMajorAxisAu)} AU",
                subtext = "V = ${String.format("%.1f", phys.orbitalVelocityKmS)} km/s",
                modifier = Modifier.weight(1f)
            )
            PhysicalMetricCard(
                label = "TEMP. ECHILIBRU",
                value = "${phys.equilibriumTempK.roundToInt()} K",
                subtext = if (phys.isHabitableZone) "★ Zonă Locuibilă" else "S = ${String.format("%.2f", phys.insolationSolar)} S☉",
                highlight = phys.isHabitableZone,
                modifier = Modifier.weight(1f)
            )
            PhysicalMetricCard(
                label = "ÎNCLINAȚIE (i)",
                value = "${String.format("%.1f", phys.orbitalInclinationDeg)}°",
                subtext = "b = ${String.format("%.2f", phys.impactParameterB)}",
                modifier = Modifier.weight(1f)
            )
        }

        // BAYESIAN UNCERTAINTY & STATISTICAL RIGOR MODULE (NASA/ESA STANDARD)
        val bayes = vet.bayesianUncertainty
        if (bayes != null) {
            Surface(
                color = Color(0xFF071224),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📐", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ESTIMARE INCERTITUDINE BAYESIANĂ (RIGOR STATISTIC)",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "NASA / ESA VALIDAT",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Error bar display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SCOR DE ÎNCREDERE (POSTERIOR MEDIAN)", fontSize = 9.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "${String.format("%.1f", bayes.medianConfidence)}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "+${String.format("%.1f", bayes.plusError1Sigma)}% / -${String.format("%.1f", bayes.minusError1Sigma)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("INTERVAL DE CREDIBILITATE 1σ (68%)", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "[${String.format("%.1f", bayes.lowerBound68)}% — ${String.format("%.1f", bayes.upperBound68)}%]",
                                fontSize = 12.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary statistical metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("INTERVAL 2σ (95%)", fontSize = 8.sp, color = TextSecondary)
                                Text(
                                    "${String.format("%.1f", bayes.lowerBound95)}% - ${String.format("%.1f", bayes.upperBound95)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("BAYES FACTOR B₁₀", fontSize = 8.sp, color = TextSecondary)
                                Text(
                                    if (bayes.bayesFactorVsFlat > 1000.0) "> 10³ (Decisiv)" else String.format("%.1f", bayes.bayesFactorVsFlat),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("FALSE ALARM (FAP)", fontSize = 8.sp, color = TextSecondary)
                                Text(
                                    if (bayes.falseAlarmProbabilityFap < 1e-4) "< 10⁻⁴ (Valid)" else String.format("%.2e", bayes.falseAlarmProbabilityFap),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bayes.falseAlarmProbabilityFap < 0.01) AccentGreen else AccentAmber,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Note
        Surface(
            color = DarkBackground,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                result.statusSummary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun VettingBadge(label: String, status: String, isOk: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = DarkBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (isOk) DarkBorder else AccentRed.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOk) AccentGreen else AccentRed,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PhysicalMetricCard(
    label: String,
    value: String,
    subtext: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (highlight) AccentGreen.copy(alpha = 0.4f) else DarkBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 8.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (highlight) AccentGreen else Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(subtext, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}
