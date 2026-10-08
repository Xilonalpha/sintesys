package com.synthesis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.synthesis.domain.model.exoplanet.LiveObservationSession
import com.synthesis.domain.model.exoplanet.PeriodicTransitDetectionResult
import com.synthesis.ui.components.exoplanet.PeriodicTransitAnalysisCard
import com.synthesis.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun LivePhotometryView(
    session: LiveObservationSession,
    onImportRealData: (String, String?, String?) -> Unit,
    onLoadEmpiricalDataset: () -> Unit,
    onUpdateObserver: (String, String, String, String, String, String, String, Double, String, Double) -> Unit,
    onReset: () -> Unit,
    onGenerateEtd: () -> Unit,
    onGenerateAavso: () -> Unit,
    onOpenVisionEngine: () -> Unit = {},
    periodicResult: PeriodicTransitDetectionResult? = null,
    onRunPeriodicBlsScan: (Double?, Double?, Double, Double) -> Unit = { _, _, _, _ -> }
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER STATUS OBSERVATOR & APARATURĂ
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (session.isRealImportedData) AccentGreen else AccentCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (session.isRealImportedData) AccentGreen.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (session.isRealImportedData) AccentGreen else AccentCyan)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (session.isRealImportedData) AccentGreen else AccentCyan,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (session.isRealImportedData) "100% MĂSURĂTORI REALE IMPORTATE" else "SET DE DATE CALIBRARE (DEMO)",
                                    color = if (session.isRealImportedData) AccentGreen else AccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "SNR ${String.format("%.1f", session.signalToNoiseRatio)}σ",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "${session.candidateName} • ${session.hostStar}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Observator: ${session.observerName} (AAVSO: ${session.aavsoObserverCode}) • ${session.observatoryLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "Optică: ${session.telescopeSpecs} • Cameră: ${session.cameraModel} • Filtru: ${session.filterBand}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timelapse, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Stare: ${session.currentPhase}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // BUTOANE DE IMPORT ȘI CONFIGURARE REALĂ
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenVisionEngine,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DESCHIDE CAMERA TELESCOP & ANALIZĂ FITS LIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ÎNCARCĂ DATE REALE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SETĂRI OBSERVATOR", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // GRAFIC NATIV CANVAS: CURBA DE LUMINĂ A TRANZITULUI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CURBA DE LUMINĂ MĂSURATĂ (FLUX RELATIV)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan
                        )
                        Text(
                            "Puncte valide: ${session.dataPoints.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(Color(0xFF020408), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        val points = session.dataPoints
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val padX = 24.dp.toPx()
                            val padY = 20.dp.toPx()

                            val chartW = w - padX * 2
                            val chartH = h - padY * 2

                            val minY = if (points.isNotEmpty()) (points.minOfOrNull { it.relativeFlux } ?: 0.98).coerceAtMost(0.985) - 0.003 else 0.985
                            val maxY = if (points.isNotEmpty()) (points.maxOfOrNull { it.relativeFlux } ?: 1.005).coerceAtLeast(1.002) + 0.003 else 1.005

                            val minX = 0.0
                            val maxX = if (points.isNotEmpty()) points.maxOfOrNull { it.minuteOffset }?.coerceAtLeast(60.0) ?: 180.0 else 180.0

                            fun mapX(minute: Double): Float =
                                padX + ((minute - minX) / (maxX - minX)).toFloat() * chartW

                            fun mapY(flux: Double): Float =
                                padY + ((maxY - flux) / (maxY - minY)).toFloat() * chartH

                            // 1.0000 Baseline line
                            val baselineY = mapY(1.0000)
                            drawLine(
                                color = Color(0xFF334155),
                                start = Offset(padX, baselineY),
                                end = Offset(w - padX, baselineY),
                                strokeWidth = 1.5f
                            )

                            // Grid line at measured dip
                            val bottomY = mapY(1.0000 - (session.measuredDepthPpm / 1_000_000.0))
                            drawLine(
                                color = AccentGreen.copy(alpha = 0.35f),
                                start = Offset(padX, bottomY),
                                end = Offset(w - padX, bottomY),
                                strokeWidth = 1f
                            )

                            // Draw Individual Real Photometric Points with Error Bars
                            points.forEach { pt ->
                                val px = mapX(pt.minuteOffset)
                                val py = mapY(pt.relativeFlux)
                                val errPx = ((pt.fluxError) / (maxY - minY)).toFloat() * chartH

                                // Error bar
                                drawLine(
                                    color = AccentCyan.copy(alpha = 0.45f),
                                    start = Offset(px, py - errPx),
                                    end = Offset(px, py + errPx),
                                    strokeWidth = 1.2f
                                )

                                // Point
                                drawCircle(
                                    color = if (session.isRealImportedData) AccentGreen else AccentCyan,
                                    radius = 3.5f,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pre-Tranzit", fontSize = 10.sp, color = TextSecondary)
                        Text("Mid-Transit BJD: ${String.format("%.4f", session.calculatedMidTransitBjd)}", fontSize = 10.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                        Text("Post-Tranzit", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        // METRICI DERIVATE DIRECTE DIN ANALIZA CURBEI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PARAMETRI PLANETARI MĂSURAȚI DIRECT:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PhotometryMetricItem("Adâncime Tranzit (δ)", "${session.measuredDepthPpm.roundToInt()} ppm", highlightColor = AccentGreen)
                        PhotometryMetricItem("Scădere Magnitudine", String.format("%.4f mag", session.measuredDepthPpm / 1_000_000.0 * 1.0857), highlightColor = AccentAmber)
                        PhotometryMetricItem("Rază Planetară", String.format("%.2f R⊕", session.measuredRadiusEarth), highlightColor = AccentCyan)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PhotometryMetricItem("T0 Mid-Transit (BJD)", String.format("%.4f", session.calculatedMidTransitBjd))
                        PhotometryMetricItem("Durată Tranzit", "${session.expectedDurationMinutes.roundToInt()} min")
                        PhotometryMetricItem("Calitate Fit (χ²/dof)", String.format("%.2f", session.chiSquaredPerDof))
                    }
                }
            }
        }

        // ANALIZĂ PERIODICITATE BLS & CURBĂ PLIATĂ ÎN FAZĂ
        item {
            PeriodicTransitAnalysisCard(
                result = periodicResult,
                onRunScan = onRunPeriodicBlsScan,
                dataPoints = session.dataPoints
            )
        }

        // SECȚIUNE GENERARE RAPORT OFICIAL DE DESCOPERIRE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF05101A)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Article, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "RAPORT OFICIAL DE TRANSMITERE ȘTIINȚIFICĂ",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Exportă raportul bazat pe datele tale reale. Formatul generat este recunoscut de ETD (Exoplanet Transit Database) și asociația AAVSO pentru creditarea oficială a observației.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onGenerateEtd,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RAPORT ETD (CAS)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onGenerateAavso,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FORMAT AAVSO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // DIALOG IMPORT DATE REALE (CSV / TSV / AstroImageJ)
    if (showImportDialog) {
        RealDataImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text, target, host ->
                onImportRealData(text, target, host)
                showImportDialog = false
            },
            onLoadSampleEmpirical = {
                onLoadEmpiricalDataset()
                showImportDialog = false
            }
        )
    }

    // DIALOG CONFIGURARE OBSERVATOR REAL
    if (showConfigDialog) {
        RealObserverConfigDialog(
            session = session,
            onDismiss = { showConfigDialog = false },
            onSave = { name, code, loc, tel, cam, filt, cStar, cMag, kStar, kMag ->
                onUpdateObserver(name, code, loc, tel, cam, filt, cStar, cMag, kStar, kMag)
                showConfigDialog = false
            }
        )
    }
}

@Composable
private fun RealDataImportDialog(
    onDismiss: () -> Unit,
    onImport: (String, String, String) -> Unit,
    onLoadSampleEmpirical: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf("WASP-12 b") }
    var hostStar by remember { mutableStateOf("WASP-12") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AccentCyan),
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "IMPORTĂ DATE FOTOMETRICE REALE",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Lipește aici tabelul fotometric exportat din AstroImageJ, Siril, N.I.N.A. sau CSV (coloane: BJD_TDB, rel_flux, [flux_err]).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = { targetName = it },
                        label = { Text("Nume Țintă") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hostStar,
                        onValueChange = { hostStar = it },
                        label = { Text("Stea Gazdă") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Tabel CSV / TSV / AstroImageJ") },
                    placeholder = { Text("2460218.320, 1.0002, 0.0008\n2460218.325, 0.9998, 0.0008\n2460218.330, 0.9862, 0.0008...") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLoadSampleEmpirical,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exemplu Real (WASP-12 b)", fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            if (rawText.isNotBlank()) {
                                onImport(rawText, targetName, hostStar)
                            }
                        },
                        enabled = rawText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PROCESEAZĂ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RealObserverConfigDialog(
    session: LiveObservationSession,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Double, String, Double) -> Unit
) {
    var obsName by remember { mutableStateOf(session.observerName) }
    var aavsoCode by remember { mutableStateOf(session.aavsoObserverCode) }
    var location by remember { mutableStateOf(session.observatoryLocation) }
    var telescope by remember { mutableStateOf(session.telescopeSpecs) }
    var camera by remember { mutableStateOf(session.cameraModel) }
    var filter by remember { mutableStateOf(session.filterBand) }
    var compStar by remember { mutableStateOf(session.comparisonStarName) }
    var compMag by remember { mutableStateOf(session.comparisonStarMag.toString()) }
    var checkStar by remember { mutableStateOf(session.checkStarName) }
    var checkMag by remember { mutableStateOf(session.checkStarMag.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AccentAmber),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "SETĂRI OBSERVATOR REALE",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Aceste date vor apărea exact în raportul oficial ETD/AAVSO în locul valorilor implicite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                item {
                    OutlinedTextField(
                        value = obsName,
                        onValueChange = { obsName = it },
                        label = { Text("Nume Observator") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = aavsoCode,
                        onValueChange = { aavsoCode = it },
                        label = { Text("Cod AAVSO (ex: LYNO)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Coordonate / Oraș Observator") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = telescope,
                        onValueChange = { telescope = it },
                        label = { Text("Optică Telescop") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = camera,
                        onValueChange = { camera = it },
                        label = { Text("Model Senzor / Cameră") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        label = { Text("Filtru (ex: Sloan r', Johnson V, Clear)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = compStar,
                            onValueChange = { compStar = it },
                            label = { Text("Stea Comparație") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = compMag,
                            onValueChange = { compMag = it },
                            label = { Text("Mag Comp") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = checkStar,
                            onValueChange = { checkStar = it },
                            label = { Text("Stea Verificare") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = checkMag,
                            onValueChange = { checkMag = it },
                            label = { Text("Mag Check") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onSave(
                                obsName,
                                aavsoCode,
                                location,
                                telescope,
                                camera,
                                filter,
                                compStar,
                                compMag.toDoubleOrNull() ?: 10.45,
                                checkStar,
                                checkMag.toDoubleOrNull() ?: 10.61
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SALVEAZĂ PENTRU RAPOARTE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotometryMetricItem(
    label: String,
    value: String,
    highlightColor: Color = Color.White
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = highlightColor, fontWeight = FontWeight.SemiBold)
    }
}
