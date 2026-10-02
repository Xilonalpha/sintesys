package com.synthesis.ui.screens.cosmology

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.cosmology.BlackHoleTarget
import com.synthesis.domain.model.cosmology.RelativisticBlackHoleProfile
import com.synthesis.engine.cosmology.BlackHoleRelativityEngine
import com.synthesis.ui.theme.*

@Composable
fun BlackHoleRelativityScreen(
    engine: BlackHoleRelativityEngine,
    modifier: Modifier = Modifier
) {
    val targets = remember { engine.getBenchmarkBlackHoles() }
    var selectedTarget by remember { mutableStateOf(targets.first()) }
    var profile by remember(selectedTarget) {
        mutableStateOf(engine.computeRelativisticProfile(selectedTarget))
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
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentMagenta)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🕳️ GĂURI NEGRE & RELATIVITATE GENERALĂ",
                            color = AccentMagenta,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = AccentMagenta.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "KERR-SCHWARZSCHILD",
                                color = AccentMagenta,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Rezolvă metrica Kerr a găurilor negre în rotație: Orizontul Evenimentelor, Ergosfera Penrose, Sfera de Fotoni, ISCO și Dilatarea Gravitațională a Timpului.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Target Selector Chips
        item {
            Text("Selectează Gaură Neagră Reală din Arhivă:", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(targets) { target ->
                    val isSelected = target.id == selectedTarget.id
                    Surface(
                        color = if (isSelected) AccentMagenta.copy(alpha = 0.25f) else DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AccentMagenta else Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable { selectedTarget = target }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                target.name,
                                color = if (isSelected) AccentMagenta else TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${if (target.massSolar > 1e6) String.format("%.1e", target.massSolar) else target.massSolar} M☉ • Spin a*: ${target.spinParameterAStar}",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Visual Relativistic Anatomy Canvas
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
                            "Anatomie Relativistă Kerr (Secțiune Transversală)",
                            color = AccentCyan,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Spin a*: ${selectedTarget.spinParameterAStar}",
                            color = AccentMagenta,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    BlackHoleAnatomyCanvas(
                        profile = profile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem("Orizont r+", Color.Black, AccentCyan)
                        LegendItem("Ergosferă", AccentMagenta, null)
                        LegendItem("Fotoni r_ph", AccentAmber, null)
                        LegendItem("Disc ISCO", Color(0xFF10B981), null)
                    }
                }
            }
        }

        // Relativistic Parameters Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Parametri Relativiști & Metrica Spațiu-Timp:", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BhMetric("RAZĂ SCHWARZSCHILD (Rs)", "${formatLargeKm(profile.schwarzschildRadiusKm)} km", AccentCyan)
                        BhMetric("ORIZONT EVENIMENTE (r+)", "${formatLargeKm(profile.eventHorizonRadiusKm)} km", AccentMagenta)
                        BhMetric("ORBITĂ STABILĂ (ISCO)", "${formatLargeKm(profile.iscoRadiusKm)} km", AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BhMetric("DIAMETRU UMBRĂ (EHT)", "${String.format("%.2f", profile.shadowDiameterMicroArcsec)} μas", AccentAmber)
                        BhMetric("EFICIENȚĂ PENROSE (η)", "${profile.penroseEnergyEfficiencyPercent}%", AccentCyan)
                        BhMetric("VITEZĂ ISCO (v/c)", "${(profile.iscoVelocityFractionC * 100).toInt()}% c", AccentRed)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BhMetric("TEMPERATURĂ HAWKING", "${String.format("%.2e", profile.hawkingTemperatureKelvin)} K", TextSecondary)
                        BhMetric("DILATARE TIMP (z_grav)", "${profile.gravitationalRedshiftAt1_5Rg}x", AccentPurple)
                        BhMetric("TIMP EVAPORARE", "${String.format("%.1e", profile.evaporationLifetimeYears)} ani", TextPrimary)
                    }

                    profile.gravitationalWaveChirpFrequencyHz?.let { gwHz ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            BhMetric("FRECVENȚĂ CHIRP GW", "$gwHz Hz", AccentAmber)
                            BhMetric("ENERGIE RADIATĂ GW", "${profile.gravitationalWaveRadiatedMassSolar} M☉ c²", AccentRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Descriere & Metodă de Confirmare:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(selectedTarget.discoveryMethod, color = AccentCyan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(selectedTarget.description, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun BlackHoleAnatomyCanvas(
    profile: RelativisticBlackHoleProfile,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val maxRadiusScale = profile.iscoRadiusKm.coerceAtLeast(profile.schwarzschildRadiusKm * 2.0)
        val pixelScale = (w.coerceAtMost(h) * 0.42f) / maxRadiusScale.toFloat()

        val rPlusPx = (profile.eventHorizonRadiusKm * pixelScale).toFloat().coerceAtLeast(15f)
        val rRsPx = (profile.schwarzschildRadiusKm * pixelScale).toFloat().coerceAtLeast(20f)
        val rIscoPx = (profile.iscoRadiusKm * pixelScale).toFloat().coerceAtLeast(rPlusPx + 20f)
        val rPhotonPx = (profile.photonSphereRadiusKm * pixelScale).toFloat().coerceAtLeast(rPlusPx + 10f)

        // Draw Accretion Disk ISCO (Outer fiery accretion ring)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFF10B981).copy(alpha = 0.5f), Color.Transparent),
                center = center,
                radius = rIscoPx * 1.25f
            ),
            radius = rIscoPx * 1.25f,
            center = center
        )
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = 0.8f),
            radius = rIscoPx,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Ergosphere (Oblate spheroid in Kerr metric: wider at equator, touching horizon at poles)
        val ergoWidthPx = rRsPx * 2f
        val ergoHeightPx = rPlusPx * 2f
        drawOval(
            color = Color(0xFFD946EF).copy(alpha = 0.25f),
            topLeft = Offset(center.x - ergoWidthPx / 2f, center.y - ergoHeightPx / 2f),
            size = Size(ergoWidthPx, ergoHeightPx)
        )
        drawOval(
            color = Color(0xFFD946EF).copy(alpha = 0.7f),
            topLeft = Offset(center.x - ergoWidthPx / 2f, center.y - ergoHeightPx / 2f),
            size = Size(ergoWidthPx, ergoHeightPx),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw Photon Sphere (dashed ring)
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = 0.9f),
            radius = rPhotonPx,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw Kerr Event Horizon r+ (Inky pitch-black shadow interior)
        drawCircle(
            color = Color.Black,
            radius = rPlusPx,
            center = center
        )
        drawCircle(
            color = Color(0xFF06B6D4), // AccentCyan boundary glow
            radius = rPlusPx,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Central singularity indicator
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun LegendItem(label: String, fillColor: Color, strokeColor: Color?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(fillColor, CircleShape)
                .then(
                    if (strokeColor != null) Modifier.border(1.dp, strokeColor, CircleShape)
                    else Modifier
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun BhMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextSecondary, fontSize = 9.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatLargeKm(km: Double): String {
    return when {
        km >= 1.0e9 -> "${String.format("%.2f", km / 1.0e9)} Mld"
        km >= 1.0e6 -> "${String.format("%.2f", km / 1.0e6)} Mil"
        km >= 1.0e3 -> "${(km / 1.0e3).toInt()}k"
        else -> String.format("%.1f", km)
    }
}
