package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.R
import com.synthesis.domain.model.exoplanet.JwstTransmissionSpectrumResult
import com.synthesis.ui.theme.*

@Composable
fun JwstTransmissionSpectroscopyView(
    spectra: List<JwstTransmissionSpectrumResult>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BlurOn, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.theory_jwst_wasp39_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.theory_jwst_wasp39_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(spectra) { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(result.planetName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Stea: ${result.hostStar} | T_eq: ${result.equilibriumTempK.toInt()} K | g: ${String.format("%.1f", result.surfaceGravityMs2)} m/s²", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.transmissionSpectroscopyMetric >= 50.0) AccentAmber.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "TSM: ${String.format("%.1f", result.transmissionSpectroscopyMetric)}",
                                color = if (result.transmissionSpectroscopyMetric >= 50.0) AccentAmber else AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(result.jwstFollowUpPriority, color = AccentGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Physical metrics row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Înălțime Scară H", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.1f", result.scaleHeightKm)} km", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Metrică ESM", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.1f", result.emissionSpectroscopyMetric)}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fotochimie UV", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text(if (result.photochemistryDetected) "SO2 Detectat" else "Nedetectat", color = if (result.photochemistryDetected) AccentAmber else TextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Gaze Detectate în Spectru:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        result.detectedAbsorptionFeatures.forEach { mol ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (mol) {
                                    "SO2" -> AccentAmber.copy(alpha = 0.25f)
                                    "CO2" -> AccentCyan.copy(alpha = 0.25f)
                                    "H2O" -> AccentCyan.copy(alpha = 0.4f)
                                    else -> AccentGreen.copy(alpha = 0.25f)
                                }
                            ) {
                                Text(
                                    mol,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spectral Table Sample
                    Text("Spectrogramă de Transmisie (Eșantion lungimi de undă):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F131C), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.spectrumPoints.filter { it.absorbingMolecule != null }.take(5).forEach { pt ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${pt.wavelengthMicrons} µm", color = AccentCyan, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                Text("${pt.transitDepthPpm.toInt()} ppm (±${pt.instrumentalErrorPpm.toInt()})", color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                Text(pt.absorbingMolecule ?: "", color = AccentAmber, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(result.atmosphericSummary, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
