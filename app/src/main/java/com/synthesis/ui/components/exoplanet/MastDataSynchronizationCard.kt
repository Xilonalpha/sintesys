package com.synthesis.ui.components.exoplanet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.MastObservationTarget
import com.synthesis.domain.model.exoplanet.MastSyncResult
import com.synthesis.domain.model.exoplanet.MastSyncState
import com.synthesis.ui.theme.*
import java.util.Locale

private val DarkSurfaceVariant = Color(0xFF161F30)

/**
 * Professional astronomical Jetpack Compose card implementing the MAST (Mikulski Archive
 * for Space Telescopes) and NASA Exoplanet Archive real-time data synchronization module.
 *
 * Downloads calibrated photometric time-series from STScI servers, extracts observation sectors,
 * and feeds the data immediately into the multi-engine analysis pipeline (BLS, Bayesian, Templates).
 */
@Composable
fun MastDataSynchronizationCard(
    syncState: MastSyncState,
    curatedTargets: List<MastObservationTarget>,
    onSyncTarget: (query: String, mission: String, period: Double?) -> Unit,
    lastResult: MastSyncResult? = null,
    onOpenBatchEngine: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var queryInput by remember { mutableStateOf("384159646") }
    var selectedMission by remember { mutableStateOf("TESS") }
    var selectedTargetId by remember { mutableStateOf("tic_384159646") }
    var showCuratedDetails by remember { mutableStateOf(false) }

    val activeSelectedTarget = remember(selectedTargetId, curatedTargets) {
        curatedTargets.find { it.id == selectedTargetId } ?: curatedTargets.firstOrNull()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (syncState.isSyncing) AccentCyan else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. Header with Title & STScI Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (syncState.isSyncing) AccentCyan else Color(0xFF9C27B0),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "SINCRONIZARE LIVE MAST / NASA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                    Text(
                        "Mikulski Archive for Space Telescopes (STScI) & NExScI Calibrated PDCSAP",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                // Active Connection Status Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (syncState.isSyncing) AccentCyan.copy(alpha = 0.15f) else Color(0xFF1B5E20).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, if (syncState.isSyncing) AccentCyan else AccentGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (syncState.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (syncState.isSyncing) AccentCyan else AccentGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            if (syncState.isSyncing) "SINCRONIZARE..." else "STScI LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncState.isSyncing) AccentCyan else AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Curated Astronomical Benchmark Targets (TESS / Kepler)
            Text(
                "ȚINTE ETALON NASA (CU TRANZITE CONFIRMATE SAU CANDIDAȚI DESCHIȘI):",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                curatedTargets.forEach { target ->
                    val isSelected = target.id == selectedTargetId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AccentCyan.copy(alpha = 0.18f) else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) AccentCyan else DarkBorder),
                        modifier = Modifier.clickable {
                            selectedTargetId = target.id
                            queryInput = target.ticId
                            selectedMission = target.mission
                        }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    target.targetName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AccentCyan else Color.White,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "[${target.sectorOrQuarter}]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentAmber,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                target.scientificPriority,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) TextSecondary else TextSecondary.copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Description of active selected curated target
            if (activeSelectedTarget != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Țintă: ${activeSelectedTarget.targetName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Mag: ${String.format(Locale.US, "%.2f", activeSelectedTarget.estimatedMag)} | ${activeSelectedTarget.spectralType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentAmber,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            activeSelectedTarget.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Search and Mission Query Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    label = { Text("ID TIC sau Nume Obiect (ex: 384159646, TOI-1895)") },
                    placeholder = { Text("Introduceți identificator TIC...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan)
                    },
                    trailingIcon = {
                        if (queryInput.isNotBlank()) {
                            IconButton(onClick = { queryInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSyncTarget(queryInput, selectedMission, null)
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Mission selector chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("TESS", "Kepler").forEach { m ->
                        val isM = selectedMission == m
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isM) AccentCyan.copy(alpha = 0.25f) else DarkSurfaceVariant,
                            border = BorderStroke(1.dp, if (isM) AccentCyan else DarkBorder),
                            modifier = Modifier.clickable { selectedMission = m }
                        ) {
                            Text(
                                m,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isM) FontWeight.Bold else FontWeight.Normal,
                                color = if (isM) AccentCyan else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Progress and Status Banner
            if (syncState.isSyncing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentCyan.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            syncState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { syncState.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = AccentCyan,
                        trackColor = DarkSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (syncState.activeError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            syncState.activeError,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. Synced Results Summary (when data exists)
            if (lastResult != null && !syncState.isSyncing) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    lastResult.target.targetName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                "${lastResult.executionTimeMs} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentAmber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Key metrics grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("PUNCTE FOTOMETRICE", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                                Text("${lastResult.dataPoints.size} puncte", style = MaterialTheme.typography.labelSmall, color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("SECTOR / CADRU", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                                Text(lastResult.target.sectorOrQuarter, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("PERIOADĂ BLS", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                                Text("${String.format(Locale.US, "%.4f", lastResult.periodicDetection.bestPeriodDays)} zile", style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("ADÂNCIME TRANZIT", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                                Text("${String.format(Locale.US, "%.0f", lastResult.periodicDetection.transitDepthFraction * 1_000_000)} ppm", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (lastResult.templateScores.isNotEmpty()) {
                            val bestTemplate = lastResult.templateScores.first()
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Șablon Astrofizic Potrivit:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    "${bestTemplate.template.name} (${String.format(Locale.US, "%.1f", bestTemplate.confidencePercent)}% Bayesian)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 6. Action Button: Execute Sync
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val targetObj = curatedTargets.find { it.ticId == queryInput.trim() }
                    onSyncTarget(queryInput.trim(), selectedMission, null)
                },
                enabled = !syncState.isSyncing && queryInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (syncState.isSyncing) "SINCRONIZARE ÎN CURS..." else "SINCRONIZEAZĂ LIVE CU ARHIVA MAST (STScI)",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }

            if (onOpenBatchEngine != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenBatchEngine,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AccentPurple)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "DESCHIDE MOTORUL BATCH (SONDAJ MASIV)",
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
