package com.synthesis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.R
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.exoplanet.AdvancedDiscoveryViewModel

@Composable
fun JwstTransmissionSpectroscopyView(
    spectra: List<JwstTransmissionSpectrumResult>,
    viewModel: AdvancedDiscoveryViewModel? = null
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Spectral Anomaly Comparator, 1 = JWST Models

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Segmented Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(Color(0xFF0F131C), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeSubTab = 0 },
                shape = RoundedCornerShape(10.dp),
                color = if (activeSubTab == 0) AccentCyan.copy(alpha = 0.25f) else Color.Transparent,
                border = if (activeSubTab == 0) BorderStroke(1.dp, AccentCyan) else null
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = if (activeSubTab == 0) AccentCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Comparator Registru & Anomalii",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeSubTab == 0) Color.White else TextSecondary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeSubTab = 1 },
                shape = RoundedCornerShape(10.dp),
                color = if (activeSubTab == 1) AccentAmber.copy(alpha = 0.25f) else Color.Transparent,
                border = if (activeSubTab == 1) BorderStroke(1.dp, AccentAmber) else null
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.BlurOn,
                        contentDescription = null,
                        tint = if (activeSubTab == 1) AccentAmber else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Modele Transmisie JWST",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeSubTab == 1) Color.White else TextSecondary
                    )
                }
            }
        }

        if (activeSubTab == 0 && viewModel != null) {
            PlanetarySpectralAnomalyComparatorSection(viewModel = viewModel)
        } else {
            JwstTransmissionModelsSection(spectra = spectra)
        }
    }
}

@Composable
fun PlanetarySpectralAnomalyComparatorSection(viewModel: AdvancedDiscoveryViewModel) {
    val curatedSpectra = viewModel.curatedRetrievedSpectra
    val selectedSpectrum by viewModel.selectedRetrievedSpectrum.collectAsState()
    val benchmarks by viewModel.publicRegistryBenchmarks.collectAsState()
    val selectedBenchmarkId by viewModel.selectedBenchmarkId.collectAsState()
    val comparisonResult by viewModel.spectralComparisonResult.collectAsState()
    val isComparing by viewModel.isComparingSpectra.collectAsState()
    val nasaDisposition by viewModel.nasaArchiveDisposition.collectAsState()
    val isQueryingArchive by viewModel.isQueryingNasaArchive.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Mission / Service Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, AccentCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Serviciu de Comparare Spectrală cu Registrul Public",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Compară datele spectrale planetare (JWST, HST, sol) cu arhivele publice oficiale (NASA Exoplanet Archive, MAST STScI, modele Exo-Transmit). Detectează automat dacă ținta deține semnături chimice unice (biomarkeri, fosfină, DMS, evadare de heliu) absente din literatura științifică mondială.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. Select Retrieved Spectrum Target
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "1. Alege Spectru Planetar Retras pentru Analiză:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        curatedSpectra.forEach { spec ->
                            val isSelected = spec.id == selectedSpectrum.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectRetrievedSpectrum(spec) },
                                label = {
                                    Text(
                                        spec.targetName.take(28),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (spec.ticId != null) Icons.Default.Explore else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) AccentCyan else TextSecondary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Target Parameters
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F131C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    selectedSpectrum.targetName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedSpectrum.ticId != null) AccentAmber.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        if (selectedSpectrum.ticId != null) "CANDIDAT BRUT (TIC)" else "CONFIRMAT",
                                        color = if (selectedSpectrum.ticId != null) AccentAmber else AccentGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Instrument: ${selectedSpectrum.instrumentFacility} | Gazdă: ${selectedSpectrum.hostStar}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("T_eq: ${selectedSpectrum.equilibriumTempK.toInt()} K", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("Raza: ${selectedSpectrum.planetRadiusEarth} R_⊕", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("Scară H: ${selectedSpectrum.scaleHeightKm.toInt()} km", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                                Text("Canale: ${selectedSpectrum.spectralPoints.size} puncte", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 3. Select Public Dataset Registry Baseline
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "2. Registru Public de Referință (Catalog Comparativ):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auto match option
                        FilterChip(
                            selected = selectedBenchmarkId == null,
                            onClick = { viewModel.selectBenchmarkId(null) },
                            label = { Text("Auto-Match (Optim Teoretic/Public)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentAmber.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )

                        benchmarks.forEach { bench ->
                            val isSelected = selectedBenchmarkId == bench.benchmarkId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectBenchmarkId(bench.benchmarkId) },
                                label = { Text(bench.targetName.take(24), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runSpectralAnomalyComparison() },
                            modifier = Modifier.weight(1f),
                            enabled = !isComparing,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            if (isComparing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rulează Comparare", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.queryNasaArchiveForCurrentTarget() },
                            modifier = Modifier.weight(1f),
                            enabled = !isQueryingArchive,
                            border = BorderStroke(1.dp, AccentAmber)
                        ) {
                            if (isQueryingArchive) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentAmber, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verifică TAP NASA", color = AccentAmber, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // NASA TAP Disposition Result Card (if queried)
        if (nasaDisposition != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131926)),
                    border = BorderStroke(1.dp, if (nasaDisposition!!.isPresentInNasaArchive) AccentCyan else AccentGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (nasaDisposition!!.isPresentInNasaArchive) Icons.Default.LibraryBooks else Icons.Default.FiberNew,
                                contentDescription = null,
                                tint = if (nasaDisposition!!.isPresentInNasaArchive) AccentCyan else AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (nasaDisposition!!.isPresentInNasaArchive) "Verificare Arhivă NASA: Sistem Cunoscut" else "Verificare Arhivă NASA: Zero Înregistrări Spectroscopice!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            nasaDisposition!!.archiveNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (nasaDisposition!!.discoveryFacility != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Facilitate înregistrată: ${nasaDisposition!!.discoveryFacility}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentCyan
                            )
                        }
                    }
                }
            }
        }

        // 4. Comparison Results & Novelty Index Card
        if (comparisonResult != null) {
            val res = comparisonResult!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(
                        1.5.dp,
                        when {
                            res.noveltyIndexPercent >= 80.0 -> AccentGreen
                            res.noveltyIndexPercent >= 50.0 -> AccentAmber
                            else -> AccentCyan
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "3. Rezultat Comparativ & Indice de Noutate",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    res.noveltyIndexPercent >= 80.0 -> AccentGreen.copy(alpha = 0.25f)
                                    res.noveltyIndexPercent >= 50.0 -> AccentAmber.copy(alpha = 0.25f)
                                    else -> AccentCyan.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    "Noutate: ${String.format(java.util.Locale.US, "%.1f", res.noveltyIndexPercent)}%",
                                    color = when {
                                        res.noveltyIndexPercent >= 80.0 -> AccentGreen
                                        res.noveltyIndexPercent >= 50.0 -> AccentAmber
                                        else -> AccentCyan
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Novelty Progress Bar
                        LinearProgressIndicator(
                            progress = { (res.noveltyIndexPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                res.noveltyIndexPercent >= 80.0 -> AccentGreen
                                res.noveltyIndexPercent >= 50.0 -> AccentAmber
                                else -> AccentCyan
                            },
                            trackColor = Color(0xFF1B2030)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            res.scientificVerdict,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metrics Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F131C), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Deviație Maximă", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text("${String.format(java.util.Locale.US, "%.1f", res.maxDeviationSigma)}σ", color = AccentAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("χ² redus", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text(String.format(java.util.Locale.US, "%.2f", res.reducedChiSquared), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Puncte Testate", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text("${res.totalDataPointsAnalyzed}", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anomalii Găsite", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text("${res.anomaliesCount}", color = if (res.anomaliesCount > 0) AccentGreen else TextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(res.publicArchiveStatus, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // 5. List of Identified Unique Anomalies
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Anomalii Spectrale Identificate (${res.identifiedAnomalies.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (res.identifiedAnomalies.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Nu s-au identificat abateri anormale față de registrul public. Spectrul este în deplin acord cu modelul de referință.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(res.identifiedAnomalies) { anomaly ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(
                            1.dp,
                            when (anomaly.noveltyLevel) {
                                AnomalyNoveltyLevel.BREAKTHROUGH_CANDIDATE -> AccentGreen
                                AnomalyNoveltyLevel.HIGH_SIGNIFICANCE -> AccentAmber
                                else -> Color(0xFF2A2E3D)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        anomaly.anomalyType.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Lungime de undă: ${anomaly.wavelengthMicrons} μm | Semnificație: ${String.format(java.util.Locale.US, "%.1f", anomaly.statisticalSignificanceSigma)}σ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentCyan
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (anomaly.noveltyLevel) {
                                        AnomalyNoveltyLevel.BREAKTHROUGH_CANDIDATE -> AccentGreen.copy(alpha = 0.25f)
                                        AnomalyNoveltyLevel.HIGH_SIGNIFICANCE -> AccentAmber.copy(alpha = 0.25f)
                                        else -> Color.Gray.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        anomaly.noveltyLevel.badgeLabel,
                                        color = when (anomaly.noveltyLevel) {
                                            AnomalyNoveltyLevel.BREAKTHROUGH_CANDIDATE -> AccentGreen
                                            AnomalyNoveltyLevel.HIGH_SIGNIFICANCE -> AccentAmber
                                            else -> Color.LightGray
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Numerical Residual Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F131C), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Observat: ${anomaly.observedValuePpm.toInt()} ppm", color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                Text("Așteptat: ${anomaly.registryExpectedValuePpm.toInt()} ppm", color = TextSecondary, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                Text("Reziduu: ${if (anomaly.residualDeviationPpm > 0) "+" else ""}${anomaly.residualDeviationPpm.toInt()} ppm", color = AccentAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                anomaly.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            if (anomaly.candidateMoleculesOrMechanism.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    anomaly.candidateMoleculesOrMechanism.forEach { mol ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AccentCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                mol,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Scientific Follow-Up Recommendations
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101622)),
                    border = BorderStroke(1.dp, Color(0xFF253046))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Protocol Științific de Validare & Raportare:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        res.followUpRecommendations.forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = AccentCyan, fontWeight = FontWeight.Bold)
                                Text(rec, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JwstTransmissionModelsSection(spectra: List<JwstTransmissionSpectrumResult>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, AccentAmber)
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
                border = BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(result.planetName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Stea: ${result.hostStar} | T_eq: ${result.equilibriumTempK.toInt()} K | g: ${String.format(java.util.Locale.US, "%.1f", result.surfaceGravityMs2)} m/s²", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.transmissionSpectroscopyMetric >= 50.0) AccentAmber.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "TSM: ${String.format(java.util.Locale.US, "%.1f", result.transmissionSpectroscopyMetric)}",
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
                            Text("${String.format(java.util.Locale.US, "%.1f", result.scaleHeightKm)} km", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Metrică ESM", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                            Text(String.format(java.util.Locale.US, "%.1f", result.emissionSpectroscopyMetric), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
