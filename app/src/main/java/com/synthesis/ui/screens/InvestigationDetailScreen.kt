package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.*
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.InvestigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigationDetailScreen(
    investigationId: String,
    onNavigateBack: () -> Unit,
    onViewEvidenceGraph: () -> Unit,
    onViewAnomalies: () -> Unit,
    onViewHypotheses: () -> Unit,
    onViewDiscoveries: () -> Unit,
    viewModel: InvestigationViewModel = hiltViewModel()
) {
    val investigation by viewModel.investigation.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val discoveries by viewModel.discoveries.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val dataSources by viewModel.dataSources.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(investigation?.title ?: "Investigation", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DarkBackground).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InvestigationHeader(
                    investigation = investigation,
                    analysisState = analysisState,
                    progress = progress,
                    onStartAnalysis = { viewModel.startAnalysis() }
                )
            }

            // Data Source Status
            item {
                Text("DATA SOURCE STATUS", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dataSources.forEach { source ->
                        SourceStatusRow(source = source)
                    }
                }
            }

            // Analysis Logs
            if (logs.isNotEmpty()) {
                item {
                    Text("ANALYSIS LOG", style = MaterialTheme.typography.titleLarge, color = AccentAmber)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            logs.takeLast(8).forEach { log ->
                                Text("> $log", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item {
                ActionButtons(
                    onEvidenceGraph = onViewEvidenceGraph,
                    onAnomalies = onViewAnomalies,
                    onHypotheses = onViewHypotheses,
                    onDiscoveries = onViewDiscoveries,
                    hasData = observations.isNotEmpty(),
                    hasDiscoveries = discoveries.isNotEmpty()
                )
            }

            if (observations.isNotEmpty()) {
                item { Text("OBSERVATIONS (${observations.size})", style = MaterialTheme.typography.titleLarge, color = AccentCyan) }
                items(observations.take(10)) { obs -> ObservationCard(observation = obs) }
            }
            if (anomalies.isNotEmpty()) {
                item { Text("ANOMALIES (${anomalies.size})", style = MaterialTheme.typography.titleLarge, color = AccentRed) }
                items(anomalies.take(5)) { anomaly -> AnomalyCard(anomaly = anomaly) }
            }
            if (hypotheses.isNotEmpty()) {
                item { Text("HYPOTHESES", style = MaterialTheme.typography.titleLarge, color = AccentGreen) }
                items(hypotheses) { hypothesis -> HypothesisCard(hypothesis = hypothesis) }
            }
            if (discoveries.isNotEmpty()) {
                item { Text("TOP DISCOVERY", style = MaterialTheme.typography.titleLarge, color = AccentGreen) }
                item { DiscoveryCard(discovery = discoveries.first()) }
            }
        }
    }
}

@Composable
fun SourceStatusRow(source: DataSource) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(
                    when (source.status) {
                        SourceStatus.ONLINE -> AccentGreen
                        SourceStatus.DEGRADED -> AccentAmber
                        SourceStatus.OFFLINE -> AccentRed
                        SourceStatus.MAINTENANCE -> TextSecondary
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                source.name,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${source.status.name} | ${source.apiLatency}ms | Q:${(source.dataQuality * 100).toInt()}%",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp
        )
    }
}

@Composable
fun InvestigationHeader(
    investigation: Investigation?,
    analysisState: InvestigationViewModel.AnalysisState,
    progress: Float,
    onStartAnalysis: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(investigation?.query ?: "", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${investigation?.status?.name ?: "PENDING"}", style = MaterialTheme.typography.labelLarge,
                color = when (investigation?.status) {
                    InvestigationStatus.COMPLETED -> AccentGreen
                    InvestigationStatus.ACTIVE -> AccentAmber
                    InvestigationStatus.FAILED -> AccentRed
                    else -> TextSecondary
                }
            )
            if (analysisState != InvestigationViewModel.AnalysisState.IDLE && analysisState != InvestigationViewModel.AnalysisState.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AccentCyan, trackColor = DarkBackground)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when (analysisState) {
                        InvestigationViewModel.AnalysisState.HEALTH_CHECK -> "Checking source health..."
                        InvestigationViewModel.AnalysisState.FETCHING -> "Fetching data from real sources..."
                        InvestigationViewModel.AnalysisState.ANALYZING -> "Analyzing observations..."
                        InvestigationViewModel.AnalysisState.CORRELATING -> "Cross-domain correlation..."
                        else -> "Processing..."
                    },
                    style = MaterialTheme.typography.labelLarge, color = TextSecondary
                )
            }
            if (analysisState == InvestigationViewModel.AnalysisState.IDLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onStartAnalysis, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                    Text("START REAL DATA ANALYSIS", color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    onEvidenceGraph: () -> Unit,
    onAnomalies: () -> Unit,
    onHypotheses: () -> Unit,
    onDiscoveries: () -> Unit,
    hasData: Boolean,
    hasDiscoveries: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Evidence Graph", Icons.Default.AccountTree, onEvidenceGraph, Modifier.weight(1f), hasData)
            ActionButton("Anomalies", Icons.Default.Warning, onAnomalies, Modifier.weight(1f), hasData)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Hypotheses", Icons.Default.Psychology, onHypotheses, Modifier.weight(1f), hasData)
            ActionButton("Discoveries", Icons.Default.EmojiObjects, onDiscoveries, Modifier.weight(1f), hasDiscoveries)
        }
    }
}

@Composable
fun ActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = text)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ObservationCard(observation: Observation) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(observation.sourceName, color = AccentCyan, style = MaterialTheme.typography.labelLarge)
            Text(observation.entity, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${observation.measurement.value} ${observation.measurement.unit} | Uncertainty: ±${observation.uncertainty}",
                color = TextSecondary, style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun AnomalyCard(anomaly: Anomaly) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Anomaly Score: ${(anomaly.anomalyScore * 100).toInt()}%", color = AccentRed)
                Text(anomaly.severity.name, color = when (anomaly.severity) {
                    AnomalySeverity.CRITICAL -> AccentRed
                    AnomalySeverity.HIGH -> AccentAmber
                    else -> TextSecondary
                })
            }
            Text("Expected: ${String.format("%.2f", anomaly.expectedValue)} | Observed: ${String.format("%.2f", anomaly.observedValue)}", color = TextPrimary)
            Text("Deviation: ${String.format("%.1f", anomaly.deviationPercent)}%", color = TextSecondary)
        }
    }
}

@Composable
fun HypothesisCard(hypothesis: Hypothesis) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(hypothesis.title, color = AccentGreen, style = MaterialTheme.typography.titleLarge)
            Text(hypothesis.description, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Evidence: ${hypothesis.evidenceCount}", color = TextPrimary)
                Text("Confidence: ${hypothesis.confidence.toInt()}%", color = AccentCyan)
            }
        }
    }
}
