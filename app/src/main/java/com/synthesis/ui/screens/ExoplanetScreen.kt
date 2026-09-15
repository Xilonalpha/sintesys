package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.TfopwgDisposition
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.ExoplanetViewModel
import com.synthesis.viewmodel.ExoplanetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoplanetScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExoplanetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val candidates = (uiState as? com.synthesis.viewmodel.ExoplanetUiState.Success)?.candidates ?: emptyList()
    val error = (uiState as? com.synthesis.viewmodel.ExoplanetUiState.Error)?.message


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EXOPLANET CANDIDATES", color = AccentCyan) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCandidates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(padding)) {
            Surface(color = AccentAmber.copy(alpha = 0.12f)) {
                Text(
                    "Pulled live from NASA's own Exoplanet Archive: TESS candidates (PC/APC) not yet " +
                        "confirmed or rejected. Ranked by signal quality, not verified. Confirming any of " +
                        "these requires real follow-up telescope observations.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(12.dp)
                )
            }

            when (uiState) {
                ExoplanetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Querying NASA Exoplanet Archive...", color = TextSecondary)
                        }
                    }
                }
                is ExoplanetUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Could not reach the archive", color = AccentRed)
                            Text(error ?: "", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                is ExoplanetUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (candidates.isNotEmpty()) {
                            item {
                                Text(
                                    "${candidates.size} unconfirmed candidates, ranked by follow-up priority",
                                    color = TextSecondary, style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        items(candidates) { candidate -> ExoplanetCandidateCard(candidate) }
                        if (candidates.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("No candidates loaded.", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExoplanetCandidateCard(candidate: ExoplanetCandidate) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(candidate.toiId, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("TIC ${candidate.ticId}", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                }
                DispositionBadge(candidate.disposition)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                candidate.planetRadiusEarth?.let {
                    MetricColumn("RADIUS (R⊕)", String.format("%.2f", it), AccentCyan)
                }
                candidate.orbitalPeriodDays?.let {
                    MetricColumn("PERIOD (days)", String.format("%.1f", it), AccentAmber)
                }
                MetricColumn("PRIORITY", "${(candidate.worthInvestigatingScore * 100).toInt()}%", AccentGreen)
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide details" else "Why this ranking?", color = AccentCyan)
            }
            if (expanded) {
                candidate.scoreReasoning.forEach { reason ->
                    Text("• $reason", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                candidate.stellarDistancePc?.let {
                    Text("Host star distance: ${String.format("%.1f", it)} pc", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                }
                candidate.equilibriumTempK?.let {
                    Text("Equilibrium temp: ${it.toInt()}K", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                }
                candidate.createdDate?.let {
                    Text("Flagged by TESS: $it", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun DispositionBadge(disposition: TfopwgDisposition) {
    val (color, label) = when (disposition) {
        TfopwgDisposition.PC -> AccentCyan to "CANDIDATE"
        TfopwgDisposition.APC -> AccentAmber to "AMBIGUOUS"
        else -> TextSecondary to disposition.name
    }
    Surface(color = color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
        Text(label, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
    }
}
