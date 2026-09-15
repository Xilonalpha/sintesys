package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.Investigation
import com.synthesis.domain.model.InvestigationStatus
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onInvestigationClick: (String) -> Unit,
    onNewInvestigation: () -> Unit,
    onViewExoplanets: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val investigations by viewModel.investigations.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SYNTHESIS", style = MaterialTheme.typography.headlineLarge, color = AccentCyan)
                        Text("SCIENTIFIC INTELLIGENCE", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewInvestigation,
                containerColor = AccentCyan,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Investigation")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SystemStatusCard(systemStatus) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onViewExoplanets() },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("EXOPLANET CANDIDATES", style = MaterialTheme.typography.labelLarge, color = AccentCyan)
                            Text("Live, unconfirmed TESS candidates from NASA's archive", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
            item {
                Text("ACTIVE INVESTIGATIONS", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(investigations) { investigation ->
                InvestigationCard(investigation = investigation, onClick = { onInvestigationClick(investigation.id) })
            }
            if (investigations.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No active investigations.\nTap + to start.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard(status: HomeViewModel.SystemStatus) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SYSTEM STATUS", style = MaterialTheme.typography.labelLarge, color = AccentCyan)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusItem("DATA SOURCES", status.dataSources.toString(), AccentCyan)
                StatusItem("LIVE CONN.", status.liveConnections.toString(), AccentGreen)
                StatusItem("ANALYSES", status.activeAnalyses.toString(), AccentAmber)
                StatusItem("ANOMALIES", status.anomalies.toString(), AccentRed)
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    }
}

@Composable
fun InvestigationCard(investigation: Investigation, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(
                    when (investigation.status) {
                        InvestigationStatus.ACTIVE -> AccentGreen
                        InvestigationStatus.COMPLETED -> AccentCyan
                        InvestigationStatus.FAILED -> AccentRed
                        else -> AccentAmber
                    }
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(investigation.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(investigation.query, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Sources: ${investigation.sourcesConsulted} | Anomalies: ${investigation.anomaliesFound} | Confidence: ${(investigation.confidenceScore * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
