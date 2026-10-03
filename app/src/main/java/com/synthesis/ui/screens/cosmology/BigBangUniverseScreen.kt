package com.synthesis.ui.screens.cosmology

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.R
import com.synthesis.domain.model.cosmology.CosmicEpoch
import com.synthesis.domain.model.cosmology.ExpansionCurvePoint
import com.synthesis.engine.cosmology.BigBangCosmologyEngine
import com.synthesis.ui.theme.*

@Composable
fun BigBangUniverseScreen(
    engine: BigBangCosmologyEngine,
    modifier: Modifier = Modifier
) {
    var h0 by remember { mutableStateOf(67.4) }
    var omegaM by remember { mutableStateOf(0.315) }
    var omegaLambda by remember { mutableStateOf(0.685) }

    val simulationResult = remember(h0, omegaM, omegaLambda) {
        engine.simulateCosmicExpansion(h0, omegaM, omegaLambda)
    }

    val epochs = remember { engine.getCosmicEpochs() }
    var selectedEpoch by remember { mutableStateOf(epochs[4]) } // Default to CMB Recombination

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🌌 " + stringResource(R.string.theory_flrw_title).uppercase(),
                            color = AccentCyan,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = AccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ΛCDM COSMOLOGY",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.theory_flrw_desc),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Live Parameters & Age Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mărimi Fundamentale Rezultate:", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Vârsta: ${simulationResult.cosmicAgeGyr} Miliarde Ani",
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CosmoMetric("CONSTANTA H₀", "${simulationResult.hubbleConstantH0} km/s/Mpc", AccentCyan)
                        CosmoMetric("TEMPERATURĂ CMB", "${simulationResult.cmbTemperatureK} K", AccentAmber)
                        CosmoMetric("ORIZONT SONIC r_s", "${simulationResult.soundHorizonMpc} Mpc", AccentPurple)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CosmoMetric("PARAM. DECELERARE q₀", "${simulationResult.decelerationParameterQ0} (Accelerat)", AccentGreen)
                        CosmoMetric("CURBURĂ SPAȚIALĂ Ω_k", "${simulationResult.curvatureOmegaK} (Plan)", TextPrimary)
                        CosmoMetric("TENSIUNE HUBBLE Δ", "±${simulationResult.hubbleTensionDeltaKmS} km/s", AccentRed)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Sliders for dynamic tuning
                    Text("Reglare Parametri Cosmologici (Interactive FLRW):", color = TextSecondary, style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Constanta Hubble H₀: ${String.format("%.1f", h0)}", color = TextPrimary, fontSize = 11.sp)
                        Text(if (h0 < 70.0) "Planck CMB (67.4)" else "SH0ES Cepheide (73.0)", color = AccentCyan, fontSize = 10.sp)
                    }
                    Slider(
                        value = h0.toFloat(),
                        onValueChange = { h0 = it.toDouble() },
                        valueRange = 60.0f..80.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Densitate Materie Ω_m: ${String.format("%.3f", omegaM)}", color = TextPrimary, fontSize = 11.sp)
                        Text("Energie Întunecată Ω_Λ: ${String.format("%.3f", omegaLambda)}", color = AccentAmber, fontSize = 11.sp)
                    }
                    Slider(
                        value = omegaM.toFloat(),
                        onValueChange = {
                            omegaM = it.toDouble()
                            omegaLambda = (1.0 - omegaM).coerceIn(0.0, 1.0)
                        },
                        valueRange = 0.10f..0.50f,
                        colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
                    )
                }
            }
        }

        // Primordial Nucleosynthesis (BBN) Abundance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Abundențe Primordiale (Big Bang Nucleosynthesis):", color = AccentAmber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("t ~ 3 - 20 min", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CosmoMetric("HIDROGEN (¹H)", "${(simulationResult.bbnAbundances.hydrogenFraction * 100).toInt()}%", AccentCyan)
                        CosmoMetric("HELIU-4 (⁴He)", "${String.format("%.2f", simulationResult.bbnAbundances.helium4MassFractionYp * 100)}%", AccentGreen)
                        CosmoMetric("DEUTERIU (D/H)", "${String.format("%.2f", simulationResult.bbnAbundances.deuteriumAbundanceRatioDoverH * 1e5)} × 10⁻⁵", AccentAmber)
                        CosmoMetric("LITIU-7 (⁷Li/H)", "${String.format("%.1f", simulationResult.bbnAbundances.lithium7AbundanceRatio * 1e10)} × 10⁻¹⁰", AccentPurple)
                    }
                }
            }
        }

        // Timeline of Cosmic Epochs
        item {
            Text("Cronologia Epocilor Cosmice (Big Bang -> Prezent):", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                epochs.forEach { epoch ->
                    val isSelected = epoch.epochName == selectedEpoch.epochName
                    Surface(
                        color = if (isSelected) AccentCyan.copy(alpha = 0.15f) else DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AccentCyan else Color.Gray.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEpoch = epoch }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(epoch.epochName, color = if (isSelected) AccentCyan else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(epoch.timeSinceBigBang, color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Redshift z: ${if (epoch.redshiftZ > 1e4) String.format("%.1e", epoch.redshiftZ) else epoch.redshiftZ}", color = TextSecondary, fontSize = 10.sp)
                                Text("Temp: ${if (epoch.temperatureKelvin > 1e4) String.format("%.1e", epoch.temperatureKelvin) else epoch.temperatureKelvin} K", color = AccentGreen, fontSize = 10.sp)
                                Text("Energie: ${epoch.dominantEnergy}", color = TextSecondary, fontSize = 10.sp)
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Fizică & Procese:", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(epoch.physicalProcesses, color = TextPrimary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Observabile Cheie:", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(epoch.keyObservables, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CosmoMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextSecondary, fontSize = 9.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
