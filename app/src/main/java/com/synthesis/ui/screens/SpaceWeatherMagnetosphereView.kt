package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.MagnetosphereShieldingResult
import com.synthesis.domain.model.exoplanet.ShieldingVerdict
import com.synthesis.ui.theme.*

@Composable
fun SpaceWeatherMagnetosphereView(
    assessments: List<MagnetosphereShieldingResult>
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
                        Icon(Icons.Default.Security, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SPACE WEATHER & MAGNETOSPHERE SHIELDING (2026)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Evaluează rezistența atmosferelor terestre la erupțiile super-flare și vântul stelar al piticelor roșii (M-dwarfs). Modelează dinamul convectiv al miezului de fier (Christensen et al. 2009), raza magnetopauzei R_mp sub presiune ram și fotoevaporarea energetică XUV (Owen & Wu 2013).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(assessments) { result ->
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
                            Text("Stea: ${result.hostStar} | Distanță: ${result.semiMajorAxisAu} AU | Rotație: ${result.rotationPeriodHours.toInt()} h", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (result.shieldingVerdict) {
                                ShieldingVerdict.MAG_SHIELDED_HABITABLE -> AccentGreen.copy(alpha = 0.2f)
                                ShieldingVerdict.CRITICAL_STRIPPING_WARNING -> AccentAmber.copy(alpha = 0.2f)
                                ShieldingVerdict.ATMOSPHERE_EVAPORATED_AIRLESS_ROCK -> AccentRed.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                when (result.shieldingVerdict) {
                                    ShieldingVerdict.MAG_SHIELDED_HABITABLE -> "SCUT ACTIV (HABITABIL)"
                                    ShieldingVerdict.CRITICAL_STRIPPING_WARNING -> "RISC EROZIUNE"
                                    ShieldingVerdict.ATMOSPHERE_EVAPORATED_AIRLESS_ROCK -> "SCUT STRĂPUNS"
                                },
                                color = when (result.shieldingVerdict) {
                                    ShieldingVerdict.MAG_SHIELDED_HABITABLE -> AccentGreen
                                    ShieldingVerdict.CRITICAL_STRIPPING_WARNING -> AccentAmber
                                    ShieldingVerdict.ATMOSPHERE_EVAPORATED_AIRLESS_ROCK -> AccentRed
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Magnetosphere metrics row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Câmp Magnetic B_p", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${String.format("%.2f", result.surfaceMagneticFieldGauss)} G", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Rază Magnetopauză", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${String.format("%.1f", result.magnetopauseStandoffRadiusRp)} R_p", color = if (result.magnetopauseStandoffRadiusRp > 1.0) AccentGreen else AccentRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Presiune Ram Solară", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${String.format("%.1f", result.solarWindRamPressureNanoPa)} nPa", color = AccentAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Space Weather & Atmosphere Retention Card
                    Surface(
                        color = Color(0xFF141923),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rată Pierdere Masă (Stripping):", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("${String.format("%.1f", result.atmosphericMassLossRateKgS)} kg/s", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Timp Retenție Atmosferă:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("${String.format("%.1f", result.estimatedAtmosphericLifetimeGyr)} Gyr", color = if (result.estimatedAtmosphericLifetimeGyr >= 4.0) AccentGreen else AccentAmber, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Iradiere XUV Stelară:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("${result.stellarXuvFluxEarthUnits.toInt()}x Terra (Flares M-Dwarf)", color = AccentAmber, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.spaceWeatherAnalysis, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
