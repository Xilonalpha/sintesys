package com.synthesis.domain.model.spark

/**
 * Data models for the SYNTHESIS SPARK In-Memory Distributed Analytics Engine.
 * Represents distributed light-curve execution metrics, partition telemetry,
 * and high-throughput transit search results.
 */
data class SparkStageInfo(
    val stageNumber: Int,
    val stageName: String,
    val durationMs: Long,
    val description: String
)

data class SparkPartitionTelemetry(
    val partitionId: Int,
    val cadenceCount: Int,
    val localRmsNoise: Double,
    val outliersRejected: Int
)

data class SparkExecutionReport(
    val ticId: String,
    val totalCadences: Int,
    val partitionCount: Int,
    val throughputCadencesPerSec: Double,
    val executionDurationMs: Long,
    val speedupFactor: Double,
    val bestPeriodDays: Double,
    val transitDepthPpm: Double,
    val signalToNoise: Double,
    val transitDurationHours: Double,
    val stages: List<SparkStageInfo>,
    val partitions: List<SparkPartitionTelemetry>,
    val isDistributedSuccess: Boolean
)
