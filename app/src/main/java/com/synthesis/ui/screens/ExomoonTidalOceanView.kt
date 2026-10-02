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
import com.synthesis.domain.model.exoplanet.ExomoonTidalOceanResult
import com.synthesis.domain.model.exoplanet.TidalOceanRegime
import com.synthesis.ui.theme.*

@Composable
fun ExomoonTidalOceanView(
    exomoons: List<ExomoonTidalOceanResult>
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
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "EXOMOON INVERSION & TIDAL OCEANS (2026)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Detectează exo-luni orbitând giganți gazoși prin semnătura defazată de π/2 dintre variațiile de timp (TTV) și de durată (TDV - Kipping 2009). Calculează disiparea mareică vâscoelastică (Barnes et al. 2013) pentru a identifica oceane lichide subterane sub gheață (analogi Europa/Enceladus).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(exomoons) { result ->
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
                            Text(result.moonName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Planetă: ${result.hostPlanet} | Stea: ${result.hostStar}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (result.oceanRegime) {
                                TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN -> AccentGreen.copy(alpha = 0.2f)
                                TidalOceanRegime.HYPER_VOLCANIC_MAGMA_OCEAN -> AccentAmber.copy(alpha = 0.2f)
                                else -> TextSecondary.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                if (result.oceanRegime == TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN) "OCEAN SUBTERAN ACTIV" else "REGIM EXTREM",
                                color = if (result.oceanRegime == TidalOceanRegime.SUSTAINED_SUB_SURFACE_OCEAN) AccentGreen else AccentAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transit Observables Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Amplitudine TTV", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${String.format("%.1f", result.ttvAmplitudeSeconds)} s", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Amplitudine TDV", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${String.format("%.1f", result.tdvAmplitudeSeconds)} s", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Defazaj Orbital", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("π/2 (Confirmare)", color = AccentGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Geophysics & Tidal Ocean Card
                    Surface(
                        color = Color(0xFF141923),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Flux Căldură Mareică:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("${String.format("%.3f", result.tidalHeatFluxWattsM2)} W/m² (Europa: 0.05 W/m²)", color = AccentAmber, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grosime Crustă Gheață:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("${String.format("%.1f", result.estimatedIceShellThicknessKm)} km", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Volum Ocean Lichid:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("${String.format("%.1f", result.oceanVolumeEarthOceans)}x Oceanele Terrei", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.astrobiologicalPotential, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Stabilitate sferă Hill: R_Hill = ${result.hillRadiusKm.toInt()} km | Orbită Lună: ${result.semiMajorAxisMoonKm.toInt()} km (Stabilă: ${if (result.isDynamicallyStable) "DA" else "NU"})",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}
