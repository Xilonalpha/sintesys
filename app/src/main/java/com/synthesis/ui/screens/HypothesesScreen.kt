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
import com.synthesis.domain.model.Hypothesis
import com.synthesis.domain.model.HypothesisStatus
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.InvestigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypothesesScreen(
    investigationId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvestigationViewModel = hiltViewModel()
) {
    val hypotheses by viewModel.hypotheses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HYPOTHESIS ENGINE", color = AccentGreen) },
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
            items(hypotheses) { hypothesis -> HypothesisDetailCard(hypothesis = hypothesis) }
            if (hypotheses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No hypotheses generated yet.", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun HypothesisDetailCard(hypothesis: Hypothesis) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(hypothesis.title, color = AccentGreen, style = MaterialTheme.typography.titleLarge)
                StatusChip(hypothesis.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(hypothesis.description, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBox("Evidence", hypothesis.evidenceCount.toString(), AccentCyan)
                StatBox("Contradictions", hypothesis.contradictionCount.toString(), AccentRed)
                StatBox("Confidence", "${hypothesis.confidence.toInt()}%", AccentGreen)
            }
            if (hypothesis.discriminatingTests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("WHAT WOULD DISCRIMINATE THEM?", color = AccentAmber, style = MaterialTheme.typography.labelLarge)
                hypothesis.discriminatingTests.forEach { test ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = DarkBackground)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(test.description, color = TextPrimary)
                            Text("Required: ${test.requiredData} | Precision: ${test.requiredPrecision} | Timeframe: ${test.timeframe}", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: HypothesisStatus) {
    val (color, text) = when (status) {
        HypothesisStatus.ACTIVE -> AccentGreen to "ACTIVE"
        HypothesisStatus.PENDING -> AccentAmber to "PENDING"
        HypothesisStatus.CONFIRMED -> AccentCyan to "CONFIRMED"
        HypothesisStatus.DISPROVEN -> AccentRed to "DISPROVEN"
    }
    Surface(color = color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
        Text(text, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.headlineLarge)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
    }
}
