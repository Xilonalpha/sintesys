package com.synthesis.domain.model.terra

/**
 * Domain models for the Terra Incognita autonomous discovery and synthesis pipeline.
 */
enum class PipelineStageStatus {
    IDLE,
    PROCESSING,
    COMPLETED,
    FLAGGED_ANOMALY
}

data class PipelineStageItem(
    val stageNumber: Int,
    val codeName: String,
    val title: String,
    val subtitle: String,
    val status: PipelineStageStatus,
    val throughputRate: String,
    val processedSamples: Long,
    val primaryMetric: String,
    val details: List<String>
)

data class TelemetryFeedItem(
    val timestamp: String,
    val sourceDomain: TerraDomain,
    val sourceName: String,
    val eventMessage: String,
    val anomalyScore: Double, // 0.0 to 1.0
    val formulaBadge: String
)

data class FrontierProgressMetric(
    val title: String,
    val currentPercent: Float, // 0.0f to 1.0f
    val targetLabel: String,
    val unitValue: String,
    val badgeColorHex: Long
)

data class TerraPipelineOverview(
    val overallProgressPercent: Int,
    val activeThreads: Int,
    val totalObservationsProcessed: Long,
    val activeAnomaliesCount: Int,
    val pipelineStatusLabel: String,
    val isSynthesizing: Boolean
)
