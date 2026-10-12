package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.synthesis.domain.model.exoplanet.PlatoAsteroseismicResult
import com.synthesis.ui.theme.*

@Composable
fun PlatoAsteroseismologyView(
    targets: List<PlatoAsteroseismicResult>
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
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ESA PLATO ASTEROSEISMOLOGY & STELLAR AGE (2026)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Misiunea spațială PLATO (ESA 2026) determină masa, raza și densitatea stelară la 1% precizie analizând frecvențele acustice interioare (p-modes: ν_max și Δν). Acest calcul dezvăluie vârsta reală a sistemului planetar și dacă viața a avut timp să apară.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(targets) { result ->
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
                            Text(result.starName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("TIC ${result.ticId} | T_eff: ${result.teffK.toInt()} K | [Fe/H]: ${result.metallicityFeH}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.isOldEnoughForLife) AccentGreen.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                if (result.isOldEnoughForLife) "VÂRSTĂ MATURĂ (>=2 Gyr)" else "SISTEM TÂNĂR",
                                color = if (result.isOldEnoughForLife) AccentGreen else AccentAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Asteroseismic Frequencies Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frecvență ν_max", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.1f", result.nuMaxMicroHz)} µHz", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Separare Δν", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.2f", result.deltaNuMicroHz)} µHz", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Densitate medie", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.3f", result.meanStellarDensitySun)} ρ_☉", color = AccentAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Solved Properties
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Masă Stelară M*:", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.3f", result.stellarMassSun)} M_☉", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rază Stelară R*:", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.3f", result.stellarRadiusSun)} R_☉", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gravitație log(g):", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.2f", result.surfaceGravityLogG)} cgs", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Age & Planetary Precision Card
                    Surface(
                        color = Color(0xFF141923),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("VÂRSTĂ SISTEM: ${String.format("%.2f", result.stellarAgeGyr)} Gyr", color = AccentGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Secvență: ${String.format("%.1f", result.mainSequenceLifetimeGyr)} Gyr", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Rază Planetă: ${String.format("%.2f", result.planetRadiusEarth)} R⊕", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Surface(shape = RoundedCornerShape(4.dp), color = AccentCyan.copy(alpha = 0.2f)) {
                                    Text("±${String.format("%.2f", result.planetRadiusErrorPercent)}% (PLATO GOLD)", color = AccentCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.habitabilityVerdict, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(result.asteroseismicMethodSummary, color = TextSecondary, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                }
            }
        }
    }
}
