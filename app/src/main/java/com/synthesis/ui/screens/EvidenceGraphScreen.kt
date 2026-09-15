package com.synthesis.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.*
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.InvestigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceGraphScreen(
    investigationId: String,
    onNavigateBack: () -> Unit,
    viewModel: InvestigationViewModel = hiltViewModel()
) {
    val nodes by viewModel.evidenceNodes.collectAsState()
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EVIDENCE GRAPH", color = AccentCyan) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale *= zoom
                        scale = scale.coerceIn(0.5f, 3f)
                        offset += pan
                    }
                }
        ) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                drawCircle(color = AccentCyan, radius = 40f * scale, center = Offset(centerX, centerY))
                drawText(textMeasurer = textMeasurer, text = "PHENOMENON", topLeft = Offset(centerX - 50f, centerY - 8f))

                nodes.forEach { node ->
                    val nodeX = centerX + (node.x - 400) * scale + offset.x
                    val nodeY = centerY + (node.y - 300) * scale + offset.y
                    val relationshipColor = when (node.relationship) {
                        RelationshipType.CONFIRMED -> AccentGreen
                        RelationshipType.WEAK -> AccentAmber
                        RelationshipType.CONTRADICTION -> AccentRed
                        else -> TextSecondary
                    }
                    drawLine(color = relationshipColor, start = Offset(centerX, centerY), end = Offset(nodeX, nodeY), strokeWidth = 2f * scale)
                    val nodeColor = when (node.type) {
                        NodeType.OBSERVATION -> Color(0xFF3B82F6)
                        NodeType.HYPOTHESIS -> Color(0xFF8B5CF6)
                        NodeType.SATELLITE -> Color(0xFFF97316)
                        NodeType.DATABASE -> Color(0xFFEC4899)
                        else -> TextSecondary
                    }
                    drawCircle(color = nodeColor, radius = 25f * scale, center = Offset(nodeX, nodeY))
                    drawText(textMeasurer = textMeasurer, text = node.label, topLeft = Offset(nodeX - 30f, nodeY + 30f * scale))
                }
            }
            Column(
                modifier = Modifier.padding(16.dp).background(DarkSurface, MaterialTheme.shapes.small).padding(12.dp)
            ) {
                Text("LEGEND", color = AccentCyan, style = MaterialTheme.typography.labelLarge)
                LegendItem("Confirmed", AccentGreen)
                LegendItem("Weak", AccentAmber)
                LegendItem("Contradiction", AccentRed)
                LegendItem("Unverified", TextSecondary)
            }
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(modifier = Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Text(text, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
    }
}
