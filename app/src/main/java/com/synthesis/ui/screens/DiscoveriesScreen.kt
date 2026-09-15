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
import com.synthesis.domain.model.Discovery
import com.synthesis.domain.model.DiscoveryType
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.InvestigationViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveriesScreen(
    investigationId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvestigationViewModel = hiltViewModel()
) {
    val discoveries by viewModel.discoveries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DISCOVERIES", color = AccentGreen) },
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
            if (discoveries.isNotEmpty()) {
                item {
                    Text(
                        "${discoveries.size} potential discoveries ranked by estimated commercial value",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            items(discoveries) { discovery -> DiscoveryCard(discovery = discovery) }
            if (discoveries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No discoveries yet — run analysis first.", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryCard(discovery: Discovery) {
    var expanded by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(discovery.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(discovery.discoveryType.name.replace('_', ' '), color = AccentCyan, style = MaterialTheme.typography.labelLarge)
                }
                DiscoveryScoreBadge(discovery.patentabilityScore)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricColumn("NOVELTY", "${(discovery.noveltyScore * 100).toInt()}%", AccentCyan)
                MetricColumn("CONFIDENCE", "${(discovery.confidenceScore * 100).toInt()}%", AccentAmber)
                MetricColumn("IMPACT", "${(discovery.impactScore * 100).toInt()}%", AccentGreen)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(color = AccentGreen.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Est. 5-yr value", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    Text(currencyFormat.format(discovery.estimatedValueUsd), color = AccentGreen, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                discovery.description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3
            )

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Show more", color = AccentCyan)
            }

            if (expanded) {
                Text("DOMAINS: ${discovery.domains.distinct().joinToString(", ")}", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("RECOMMENDED ACTIONS", color = AccentAmber, style = MaterialTheme.typography.labelLarge)
                discovery.recommendedActions.forEach { action ->
                    Text("• $action", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("RISKS", color = AccentRed, style = MaterialTheme.typography.labelLarge)
                discovery.risks.forEach { risk ->
                    Text("• $risk", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun DiscoveryScoreBadge(patentabilityScore: Float) {
    val (color, label) = when {
        patentabilityScore >= 0.7f -> AccentGreen to "HIGH"
        patentabilityScore >= 0.4f -> AccentAmber to "MEDIUM"
        else -> TextSecondary to "LOW"
    }
    Surface(color = color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(patentabilityScore * 100).toInt()}%", color = color, style = MaterialTheme.typography.titleMedium)
            Text(label, color = color, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}
