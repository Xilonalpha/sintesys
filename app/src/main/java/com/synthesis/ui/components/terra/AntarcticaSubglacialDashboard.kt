package com.synthesis.ui.components.terra

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.terra.*
import com.synthesis.ui.theme.*

@Composable
fun AntarcticaSubglacialDashboard(
    subglacialLakes: List<AntarcticSubglacialLake>,
    selectedLake: AntarcticSubglacialLake?,
    bedrockFeatures: List<AntarcticBedrockFeature>,
    selectedFeature: AntarcticBedrockFeature?,
    polarAltimetry: PolarAltimetryState,
    iceMeltFraction: Float,
    isostaticRebound: IsostaticReboundSimulation,
    onSelectLake: (AntarcticSubglacialLake) -> Unit,
    onSelectFeature: (AntarcticBedrockFeature) -> Unit,
    onIceMeltChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("antarctica_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO HEADER: DATE DESCHISE & TRANSPARENȚĂ ȘTIINȚIFICĂ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF80DEEA), Color(0xFF4FC3F7), Color(0xFF0288D1)))),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09182B))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D243F), Color(0xFF050F1D))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF80DEEA).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "BEDMACHINE & ICESAT-2 POLAR DATA",
                            color = Color(0xFF80DEEA),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        "Tratatul Antarctic 1959",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "Antarctica Subglaciară: Topografia Reală de Sub 4 Km de Gheață",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Fără mituri și fără speculații: datele deschise ale NASA, ESA și British Antarctic Survey arată că Antarctica nu este o barieră plată, ci un continent masiv cu munți îngropați de mărimea Alpilor, cel mai adânc canion de pe continente (-3.500m) și peste 400 de lacuri subglaciare cu apă lichidă păstrată de presiune și căldura geotermală a scoarței.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Polar Metrics Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF041324),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("VOLUM GHEAȚĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            Text("27.0 M km³", color = Color(0xFF80DEEA), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = Color(0xFF041324),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("ECHIVALENT MARE", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            Text("+58.3 metri", color = AccentAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = Color(0xFF041324),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("BILANȚ MASĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            Text("-148 Gt/an", color = Color(0xFFFF5252), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. TOPOGRAFIA PATULUI STÂNCOS (BEDROCK BATHYMETRY)
        Text(
            "🏔️ RELIEFUL ASCUNS SUB GHEAȚĂ (BEDROCK RADAR BATHYMETRY)",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF80DEEA),
            fontWeight = FontWeight.Bold
        )

        // Horizontal Feature Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bedrockFeatures.forEach { feature ->
                val isSelected = selectedFeature?.id == feature.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF80DEEA).copy(alpha = 0.25f) else DarkSurface,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF80DEEA) else Color(0xFF1E293B)),
                    modifier = Modifier.clickable { onSelectFeature(feature) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSelected) "🏔️ " else "🧊 ", fontSize = 13.sp)
                        Text(
                            feature.name.take(24),
                            color = if (isSelected) Color(0xFF80DEEA) else Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Selected Bedrock Feature Detail Card
        selectedFeature?.let { feature ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF80DEEA).copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(feature.name, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(feature.locationDescription, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(
                            color = if (feature.bedrockElevationMeters < 0) Color(0xFFFF5252).copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (feature.bedrockElevationMeters < 0) "${feature.bedrockElevationMeters}m (Sub Nivel Mare)" else "+${feature.bedrockElevationMeters}m (Vârf Muntos)",
                                color = if (feature.bedrockElevationMeters < 0) Color(0xFFFF5252) else AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(feature.description, color = TextPrimary, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bedrock Metric Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF0F1A2A),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("GROSIME GHEAȚĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                Text("${feature.iceCoverThicknessMeters} metri", color = AccentCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            color = Color(0xFF0F1A2A),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("REVENIRE IZOPRESIUNE", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                Text("+${feature.isostaticReboundEstimateMeters} m ridicare", color = AccentGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Metodă Detecție: ${feature.discoveryMethod}", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }

        // 3. SIMULATOR DE REVENIRE IZOSTATICĂ (ISOSTATIC REBOUND SOLVER)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚖️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "SIMULATOR FIZIC: CE S-AR ÎNTÂMPLA FĂRĂ GHEAȚĂ?",
                        color = AccentAmber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Dacă gheața s-ar topi, scoarța terestră s-ar ridica elastic prin revenire izostatică (Δh = h_ice · ρ_ice / ρ_manta). Modifică procentul:",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Topire Calotă: ${(iceMeltFraction * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Volum Pierdut: ${(isostaticRebound.iceVolumeLostKm3 / 1_000_000.0).format(1)} M km³", color = AccentAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = iceMeltFraction,
                    onValueChange = onIceMeltChange,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentAmber,
                        activeTrackColor = AccentAmber,
                        inactiveTrackColor = Color(0xFF2A364F)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF1F1206),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("RIDICARE GLOBALĂ MĂRI", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            Text("+${isostaticRebound.globalSeaLevelRiseMeters} m", color = Color(0xFFFF5252), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = Color(0xFF0A1F13),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("RIDICARE SCOARȚĂ ANTARCTICĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            Text("+${isostaticRebound.crustalReboundMaxMeters} m", color = AccentGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. CATALOGUL LACURILOR SUBGLACIARE (EXTREMOLIMNOLOGIE)
        Text(
            "💧 LACURILE SUBGLACIARE (>400 REZERVOARE LICHIDE SUB GHEAȚĂ)",
            style = MaterialTheme.typography.titleSmall,
            color = AccentCyan,
            fontWeight = FontWeight.Bold
        )

        // Horizontal Lake Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subglacialLakes.forEach { lake ->
                val isSelected = selectedLake?.id == lake.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else DarkSurface,
                    border = BorderStroke(1.dp, if (isSelected) AccentCyan else Color(0xFF1E293B)),
                    modifier = Modifier.clickable { onSelectLake(lake) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSelected) "💧 " else "❄️ ", fontSize = 13.sp)
                        Text(
                            lake.name,
                            color = if (isSelected) AccentCyan else Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        selectedLake?.let { lake ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(lake.name, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(lake.region, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(
                            color = AccentCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Presiune: ${lake.pressureAtmospheres.toInt()} atm",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(lake.scientificSignificance, color = TextPrimary, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lake Physical Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF0C1B2C),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("GROSIME CALOTĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                Text("${lake.iceThicknessMeters} metri", color = Color(0xFF80DEEA), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            color = Color(0xFF0C1B2C),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("ADÂNCIME APĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                Text("${lake.estimatedWaterDepthMeters} metri", color = AccentGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            color = Color(0xFF0C1B2C),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("IZOLARE", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                                Text(lake.isolationAgeYears.take(12), color = AccentAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Viață Extremofilă: ${lake.extremaType}", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Statut Cercetare: ${lake.researchStatus}", color = AccentGreen, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }

        // 5. MONITORIZARE SATELITARĂ LASER & RADAR (ICESat-2 & CryoSat-2)
        Text(
            "🛰️ DATE REALE DIN ORBITĂ (ICESat-2 & CryoSat-2)",
            style = MaterialTheme.typography.titleSmall,
            color = AccentGreen,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1724))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(polarAltimetry.activeMission, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Surface(color = AccentGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("RADAR/LASER ACTIV", color = AccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text("Senzor: ${polarAltimetry.sensorType}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                Text("Frecvență & Rezoluție: ${polarAltimetry.photonCountPerPulse}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                Text("Viteză maximă flux glaciar (Thwaites/Pine Island): ${polarAltimetry.iceVelocityMaxMetersPerYear.toInt()} m/an", color = AccentAmber, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("Adâncime penetrare radar în gheață: ${polarAltimetry.radarPenetrationDepthMeters} metri", color = AccentCyan, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(polarAltimetry.lastCalibrationTime, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
            }
        }
    }
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
