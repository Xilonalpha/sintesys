package com.synthesis.ui.components.exoplanet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.ui.theme.*
import java.util.Locale

private val DarkSurfaceVariant = Color(0xFF161F30)

/**
 * Enterprise-grade Jetpack Compose screen view for managing and monitoring the
 * MAST Batch Processing Engine, enabling large-scale, automated planet candidate
 * discovery across multiple star systems.
 */
@Composable
fun MastBatchProcessingView(
    jobs: List<MastBatchJob>,
    summary: MastBatchQueueSummary,
    isRunning: Boolean,
    campaigns: List<BatchPresetCampaign>,
    onStartProcessing: () -> Unit,
    onPauseProcessing: () -> Unit,
    onEnqueueCampaign: (BatchPresetCampaign) -> Unit,
    onEnqueueBulkText: (String, String) -> Unit,
    onCancelJob: (String) -> Unit,
    onClearQueue: () -> Unit,
    onRetryFailed: () -> Unit,
    onInspectJobInLightCurve: (MastBatchJob) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBulkDialog by remember { mutableStateOf(false) }
    var showCampaignsDialog by remember { mutableStateOf(false) }
    var selectedFilterTab by remember { mutableStateOf("ALL") } // "ALL", "QUEUED", "COMPLETED", "DISCOVERIES"

    val filteredJobs = remember(jobs, selectedFilterTab) {
        when (selectedFilterTab) {
            "QUEUED" -> jobs.filter { it.status == MastBatchJobStatus.QUEUED || it.status == MastBatchJobStatus.DOWNLOADING || it.status == MastBatchJobStatus.ANALYZING }
            "COMPLETED" -> jobs.filter { it.status == MastBatchJobStatus.COMPLETED }
            "DISCOVERIES" -> jobs.filter { it.isCandidateDiscovered }
            else -> jobs
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero & Status Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isRunning) AccentCyan else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isRunning) AccentGreen else AccentAmber,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "MOTOR PROCESARE BATCH MAST",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                "Descoperire automată la scară largă a exoplanetelor din arhivele STScI",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isRunning) AccentGreen.copy(alpha = 0.2f) else DarkBorder,
                            border = BorderStroke(1.dp, if (isRunning) AccentGreen else Color.Gray)
                        ) {
                            Text(
                                if (isRunning) "ACTIV (RULARE)" else "STANDBY (OPRIT)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) AccentGreen else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Statistics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile(label = "TOTAL ÎN COADĂ", value = "${summary.totalJobs}", color = Color.White)
                        StatTile(label = "FINALIZATE", value = "${summary.completedCount}", color = AccentCyan)
                        StatTile(label = "CANDIDAȚI NOI", value = "${summary.candidatesDiscoveredCount}", color = AccentGreen, isHighlighted = true)
                        StatTile(label = "PUNCTE FOTO", value = "${summary.totalPhotometricPointsAnalyzed}", color = AccentAmber)
                    }

                    if (summary.averageExecutionTimeMs > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Viteză medie de analiză: ${summary.averageExecutionTimeMs} ms / sistem stelar (BLS + Bayesian + Șabloane)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 3. Action Toolbar (Start, Pause, Presets, Bulk Import)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "ACȚIUNI & CONTROL COADĂ:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start / Pause Toggle Button
                        Button(
                            onClick = { if (isRunning) onPauseProcessing() else onStartProcessing() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) AccentAmber else AccentGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isRunning) "PAUZĂ BATCH" else "PORNEȘTE BATCH",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 11.sp
                            )
                        }

                        // Add Campaign Button
                        OutlinedButton(
                            onClick = { showCampaignsDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CAMPANII", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Bulk Ingestion Button
                        OutlinedButton(
                            onClick = { showBulkDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentPurple),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Queue, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BULK TIC", color = AccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (summary.failedCount > 0 || summary.completedCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (summary.failedCount > 0) {
                                TextButton(onClick = onRetryFailed) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reîncearcă Eșuate (${summary.failedCount})", color = AccentAmber, fontSize = 10.sp)
                                }
                            }
                            TextButton(onClick = onClearQueue) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Curăță Coada", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabChip(
                    title = "Toate (${jobs.size})",
                    isSelected = selectedFilterTab == "ALL",
                    onClick = { selectedFilterTab = "ALL" },
                    modifier = Modifier.weight(1f)
                )
                FilterTabChip(
                    title = "În Coadă (${summary.queuedCount + summary.inProgressCount})",
                    isSelected = selectedFilterTab == "QUEUED",
                    onClick = { selectedFilterTab = "QUEUED" },
                    modifier = Modifier.weight(1f)
                )
                FilterTabChip(
                    title = "Finalizate (${summary.completedCount})",
                    isSelected = selectedFilterTab == "COMPLETED",
                    onClick = { selectedFilterTab = "COMPLETED" },
                    modifier = Modifier.weight(1f)
                )
                FilterTabChip(
                    title = "★ Candidați (${summary.candidatesDiscoveredCount})",
                    isSelected = selectedFilterTab == "DISCOVERIES",
                    onClick = { selectedFilterTab = "DISCOVERIES" },
                    activeColor = AccentGreen,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }

        // 5. Jobs List
        if (filteredJobs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Niciun sistem stelar în această categorie",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Apăsați pe 'CAMPANII' sau 'BULK TIC' pentru a adăuga sisteme stelare în coada de analiză.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(filteredJobs, key = { it.id }) { job ->
                MastBatchJobCard(
                    job = job,
                    onCancel = { onCancelJob(job.id) },
                    onInspect = { onInspectJobInLightCurve(job) }
                )
            }
        }
    }

    // Dialog for Campaign Presets
    if (showCampaignsDialog) {
        AlertDialog(
            onDismissRequest = { showCampaignsDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Selectează Campanie de Analiză MAST", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Adaugă pachete pre-configurate de ținte etalon sau candidați din arhivele TESS/Kepler:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    campaigns.forEach { camp ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEnqueueCampaign(camp)
                                    showCampaignsDialog = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(camp.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentCyan.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "${camp.targets.size} ȚINTE",
                                            color = AccentCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(camp.description, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCampaignsDialog = false }) {
                    Text("Închide", color = AccentCyan)
                }
            }
        )
    }

    // Dialog for Bulk TIC Ingestion
    if (showBulkDialog) {
        var bulkInput by remember { mutableStateOf("") }
        var selectedMission by remember { mutableStateOf("TESS") }

        AlertDialog(
            onDismissRequest = { showBulkDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Import Bulk TIC (Masto-Scanner)", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Lipiți o listă de identificatori TIC separați prin virgulă, spațiu sau rând nou (ex: 384159646, 268301217, 100100827, 25155310):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = bulkInput,
                        onValueChange = { bulkInput = it },
                        placeholder = { Text("384159646, 268301217, 100100827...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TESS", "Kepler").forEach { m ->
                            val isSel = selectedMission == m
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) AccentPurple.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSel) AccentPurple else DarkBorder),
                                modifier = Modifier.clickable { selectedMission = m }
                            ) {
                                Text(
                                    m,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSel) AccentPurple else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bulkInput.isNotBlank()) {
                            onEnqueueBulkText(bulkInput, selectedMission)
                            showBulkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Adaugă în Coadă", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDialog = false }) {
                    Text("Anulează", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    color: Color,
    isHighlighted: Boolean = false
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 8.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) AccentGreen else color,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun FilterTabChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color = AccentCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.2f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) activeColor else DarkBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MastBatchJobCard(
    job: MastBatchJob,
    onCancel: () -> Unit,
    onInspect: () -> Unit
) {
    val borderColor = when {
        job.isCandidateDiscovered -> AccentGreen
        job.status == MastBatchJobStatus.DOWNLOADING || job.status == MastBatchJobStatus.ANALYZING -> AccentCyan
        job.status == MastBatchJobStatus.FAILED -> AccentRed
        else -> DarkBorder
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Star / TIC & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (job.isCandidateDiscovered) {
                        Surface(
                            shape = CircleShape,
                            color = AccentGreen,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        job.targetName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "[${job.mission} ${job.sectorOrQuarter}]",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (job.status) {
                        MastBatchJobStatus.COMPLETED -> if (job.isCandidateDiscovered) AccentGreen.copy(alpha = 0.2f) else DarkBorder
                        MastBatchJobStatus.DOWNLOADING, MastBatchJobStatus.ANALYZING -> AccentCyan.copy(alpha = 0.2f)
                        MastBatchJobStatus.FAILED -> AccentRed.copy(alpha = 0.2f)
                        else -> DarkBorder
                    }
                ) {
                    Text(
                        if (job.isCandidateDiscovered) "★ CANDIDAT DETECTAT" else job.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (job.status) {
                            MastBatchJobStatus.COMPLETED -> if (job.isCandidateDiscovered) AccentGreen else Color.White
                            MastBatchJobStatus.DOWNLOADING, MastBatchJobStatus.ANALYZING -> AccentCyan
                            MastBatchJobStatus.FAILED -> AccentRed
                            else -> TextSecondary
                        },
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                job.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 10.sp
            )

            // Progress Bar if in progress
            if (job.status == MastBatchJobStatus.DOWNLOADING || job.status == MastBatchJobStatus.ANALYZING) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { job.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentCyan,
                    trackColor = DarkSurfaceVariant
                )
            }

            // Results details if completed
            if (job.status == MastBatchJobStatus.COMPLETED && job.result != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PERIOADĂ BLS", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 8.sp)
                            Text(
                                "${String.format(Locale.US, "%.3f", job.candidatePeriodDays ?: 0.0)} d",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("ADÂNCIME TRANZIT", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 8.sp)
                            Text(
                                "${String.format(Locale.US, "%.0f", job.candidateDepthPpm ?: 0.0)} ppm",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("SNR BAYESIAN", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 8.sp)
                            Text(
                                String.format(Locale.US, "%.1f", job.candidateSnr ?: 0.0),
                                style = MaterialTheme.typography.labelSmall,
                                color = if ((job.candidateSnr ?: 0.0) >= 7.1) AccentGreen else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("PUNCTE FOTO", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 8.sp)
                            Text(
                                "${job.result.dataPoints.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (job.bestTemplateName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Șablon: ${job.bestTemplateName} (${String.format(Locale.US, "%.1f", job.bestTemplateConfidence ?: 0.0)}% Bayesian)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentCyan,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Action: Transfer to Light Curve for Visual Verification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onInspect,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inspectează în Recharts FITS", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
