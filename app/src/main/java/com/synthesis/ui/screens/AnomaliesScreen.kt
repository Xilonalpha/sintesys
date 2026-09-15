package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.Anomaly
import com.synthesis.domain.model.AnomalySeverity
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.InvestigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomaliesScreen(
    investigationId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvestigationViewModel = hiltViewModel()
) {
    val anomalies by viewModel.anomalies.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ANOMALY DETECTION", color = AccentRed) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(anomalies) { anomaly -> AnomalyDetailCard(anomaly = anomaly) }
            if (anomalies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No anomalies detected yet.", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AnomalyDetailCard(anomaly: Anomaly) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ANOMALY #${anomaly.id.takeLast(6)}", color = AccentRed, style = MaterialTheme.typography.titleLarge)
                SeverityBadge(anomaly.severity)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ValueColumn("EXPECTED", String.format("%.2f", anomaly.expectedValue), TextSecondary)
                ValueColumn("OBSERVED", String.format("%.2f", anomaly.observedValue), AccentCyan)
                ValueColumn("DEVIATION", "${String.format("%.1f", anomaly.deviationPercent)}%", AccentRed)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("ANOMALY SCORE: ${(anomaly.anomalyScore * 100).toInt()}%", color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("POSSIBLE CAUSES:", color = AccentAmber, style = MaterialTheme.typography.labelLarge)
            anomaly.possibleCauses.forEach { cause ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cause.description, color = TextSecondary)
                    Text("${(cause.probability * 100).toInt()}%", color = AccentCyan)
                }
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: AnomalySeverity) {
    val (color, text) = when (severity) {
        AnomalySeverity.CRITICAL -> AccentRed to "CRITICAL"
        AnomalySeverity.HIGH -> AccentAmber to "HIGH"
        AnomalySeverity.MEDIUM -> Color(0xFFFCD34D) to "MEDIUM"
        AnomalySeverity.LOW -> AccentGreen to "LOW"
    }
    Surface(color = color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
        Text(text, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ValueColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.headlineLarge)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
    }
}
