package com.synthesis.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.MicrolensingCurvePoint
import com.synthesis.domain.model.exoplanet.MicrolensingEvent
import com.synthesis.domain.model.exoplanet.MicrolensingInversionResult
import com.synthesis.engine.exoplanet.RomanMicrolensingEngine
import com.synthesis.ui.theme.*

@Composable
fun RomanMicrolensingView(
    engine: RomanMicrolensingEngine,
    modifier: Modifier = Modifier
) {
    val events = remember { engine.getBenchmarkEvents() }
    var selectedEvent by remember { mutableStateOf(events.first()) }
    var inversionResult by remember(selectedEvent) {
        mutableStateOf(engine.invertMicrolensingEvent(selectedEvent))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🔭 NASA ROMAN SPACE TELESCOPE",
                            color = AccentAmber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = AccentAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "MICROLENSING 2026/2027",
                                color = AccentAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Detecție de planete vagaboande (Rogue Planets) orfane prin curbura spațiu-timp Paczyński și caustice în Bulbul Galactic.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Event Selector Chips
        item {
            Text("Selectează Eveniment Microlensing / Candidat Roman:", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events) { event ->
                    val isSelected = event.eventId == selectedEvent.eventId
                    Surface(
                        color = if (isSelected) AccentAmber.copy(alpha = 0.25f) else DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AccentAmber else Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable { selectedEvent = event }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                event.eventId,
                                color = if (isSelected) AccentAmber else TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (event.isFreeFloatingPlanet) "Planeta Orfană (FFP)" else "Planeta + Stea",
                                color = if (event.isFreeFloatingPlanet) AccentGreen else AccentCyan,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Light Curve Canvas
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Curba de Magnificare Einstein A(t)",
                            color = AccentCyan,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Peak: ${String.format("%.1fx", inversionResult.peakMagnification)}",
                            color = AccentAmber,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    MicrolensingLightCurveCanvas(
                        points = inversionResult.lightCurve,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-1.5 t_E", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text("t_0 (Vârf Magnificare)", color = AccentAmber, style = MaterialTheme.typography.labelSmall)
                        Text("+1.5 t_E", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Inversion Parameters Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rezultate Inversie Matematică Roman / Paczyński:", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ParameterColumn("MASĂ ESTIMATĂ", "${String.format("%.2f", inversionResult.inferredMassEarth)} M⊕", AccentGreen)
                        ParameterColumn("ECHIV. JUPITER", "${String.format("%.3f", inversionResult.inferredMassJupiter)} M_J", AccentCyan)
                        ParameterColumn("RAZĂ EINSTEIN θ_E", "${String.format("%.2f", inversionResult.einsteinRadiusMas)} mas", AccentAmber)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ParameterColumn("TIMP TRAVERSARE t_E", "${String.format("%.1f", inversionResult.crossingDurationHours)} ore", AccentPurple)
                        ParameterColumn("MIȘCARE PROPRIE μ", "${String.format("%.1f", inversionResult.properMotionMasYr)} mas/an", TextPrimary)
                        ParameterColumn("TIP LENTILĂ", if (inversionResult.isFreeFloating) "Planetă Orfană" else "Sistem Binar", AccentMagenta)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Structură Caustică:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(inversionResult.causticStructure, color = AccentCyan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rezumare Astrofizică:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(inversionResult.astrophysicalSummary, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MicrolensingLightCurveCanvas(
    points: List<MicrolensingCurvePoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        val maxMag = (points.maxOfOrNull { it.magnification } ?: 10.0).coerceAtLeast(2.0)
        val minMag = 1.0

        // Grid lines
        drawLine(
            color = Color.DarkGray.copy(alpha = 0.5f),
            start = Offset(0f, h - 2f),
            end = Offset(w, h - 2f),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.DarkGray.copy(alpha = 0.3f),
            start = Offset(w / 2f, 0f),
            end = Offset(w / 2f, h),
            strokeWidth = 1f
        )

        // Draw Paczynski curve
        val path = Path()
        val causticPoints = mutableListOf<Offset>()

        points.forEachIndexed { i, pt ->
            val x = (i.toFloat() / (points.size - 1).toFloat()) * w
            val normalizedMag = ((pt.magnification - minMag) / (maxMag - minMag)).coerceIn(0.0, 1.0).toFloat()
            val y = h - (normalizedMag * (h - 20f)) - 10f

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

            if (pt.isCausticCrossing) {
                causticPoints.add(Offset(x, y))
            }
        }

        drawPath(
            path = path,
            color = Color(0xFFF59E0B), // AccentAmber
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw caustic spikes
        causticPoints.forEach { pt ->
            drawCircle(
                color = Color(0xFF10B981), // AccentGreen
                radius = 5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun ParameterColumn(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        Text(value, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
