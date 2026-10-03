package com.synthesis.ui.components.terra

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.terra.*
import com.synthesis.ui.theme.*

@Composable
fun TerraDiscoveryPipelineDashboard(
    overview: TerraPipelineOverview,
    stages: List<PipelineStageItem>,
    telemetryFeeds: List<TelemetryFeedItem>,
    frontierMetrics: List<FrontierProgressMetric>,
    onTriggerSynthesis: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedStageNumber by remember { mutableStateOf<Int?>(2) } // Stage 2 expanded by default

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("terra_pipeline_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP SYNTHESIS PROGRESS & THROUGHPUT BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(AccentCyan, Color(0xFF7C4DFF), AccentGreen))),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F1B3B), Color(0xFF080D1D))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "TERRA INCOGNITA SYNTHESIS PIPELINE",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            overview.pipelineStatusLabel,
                            color = if (overview.isSynthesizing) AccentAmber else AccentGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }

                    // Interactive Synthesis Action Button
                    Button(
                        onClick = onTriggerSynthesis,
                        enabled = !overview.isSynthesizing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (overview.isSynthesizing) DarkSurface else AccentCyan,
                            contentColor = if (overview.isSynthesizing) TextSecondary else Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("trigger_synthesis_button")
                    ) {
                        if (overview.isSynthesizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = AccentAmber,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinteză...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sinteză", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rulează Sinteză", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Animated Progress Bar
                val animatedProgress by animateFloatAsState(
                    targetValue = overview.overallProgressPercent / 100f,
                    animationSpec = tween(durationMillis = 800),
                    label = "pipeline_progress"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "Capacitate Pipeline & Inversie Globală",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        "${overview.overallProgressPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentCyan,
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Throughput Metric Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = "OBSERVAȚII",
                        value = "${overview.totalObservationsProcessed / 1000}k",
                        color = Color.White
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = "THREAD-URI",
                        value = "${overview.activeThreads} CPU/GPU",
                        color = AccentGreen
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = "ANOMALII",
                        value = "${overview.activeAnomaliesCount} Active",
                        color = AccentAmber
                    )
                }
            }
        }

        // 2. PIPELINE STAGES STEPPER
        Text(
            "STADIILE PIPELINE-ULUI DE DESCOPERIRE",
            style = MaterialTheme.typography.titleSmall,
            color = AccentCyan,
            fontWeight = FontWeight.Bold
        )

        stages.forEach { stage ->
            val isExpanded = expandedStageNumber == stage.stageNumber
            val statusColor = when (stage.status) {
                PipelineStageStatus.COMPLETED -> AccentGreen
                PipelineStageStatus.PROCESSING -> AccentCyan
                PipelineStageStatus.FLAGGED_ANOMALY -> Color(0xFFFF5252)
                PipelineStageStatus.IDLE -> TextSecondary
            }
            val statusLabel = when (stage.status) {
                PipelineStageStatus.COMPLETED -> "FINALIZAT"
                PipelineStageStatus.PROCESSING -> "PROCESARE ACTIVĂ"
                PipelineStageStatus.FLAGGED_ANOMALY -> "ANOMALIE DETECTATĂ"
                PipelineStageStatus.IDLE -> "ÎN AȘTEPTARE"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedStageNumber = if (isExpanded) null else stage.stageNumber
                    },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isExpanded) statusColor.copy(alpha = 0.8f) else Color(0xFF1E293B)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = statusColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${stage.stageNumber}",
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    stage.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stage.subtitle,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                statusLabel,
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Throughput: ${stage.throughputRate}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                        Text(
                            stage.primaryMetric,
                            color = AccentCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Collapsible details drawer
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Operațiuni & Filtre Algoritmice Active:",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            stage.details.forEach { detail ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        detail,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. FRONTIER EXPLORATION MATRIX (DATA-DRIVEN GAUGES)
        Text(
            "MATRICEA FRONTIERELOR PLANETARE NECUNOSCUTE",
            style = MaterialTheme.typography.titleSmall,
            color = AccentAmber,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                frontierMetrics.forEach { metric ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                metric.title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                metric.unitValue,
                                color = Color(metric.badgeColorHex),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { metric.currentPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(metric.badgeColorHex),
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            metric.targetLabel,
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // 4. LIVE SYNTHESIZED TELEMETRY STREAM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FLUX LIVE TELEMETRIE & DETECȚII ANOMALII",
                style = MaterialTheme.typography.titleSmall,
                color = AccentCyan,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = AccentGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        telemetryFeeds.forEach { feed ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(feed.sourceDomain.badgeColorHex).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                feed.sourceName.uppercase(),
                                color = Color(feed.sourceDomain.badgeColorHex),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(feed.timestamp, color = TextSecondary, fontSize = 9.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        feed.eventMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF071B2F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "📐 ${feed.formulaBadge}",
                                color = AccentCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            "Scor Anomalie: ${(feed.anomalyScore * 100).toInt()}%",
                            color = if (feed.anomalyScore > 0.85) Color(0xFFFF5252) else AccentAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF040A18),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
            Text(value, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
