package com.synthesis.domain.model.exoplanet

/**
 * Lifecycle status of an individual MAST dataset batch processing job.
 */
enum class MastBatchJobStatus(val displayName: String) {
    QUEUED("În coadă"),
    DOWNLOADING("Descărcare PDCSAP"),
    ANALYZING("Analiză Multi-Engine"),
    COMPLETED("Finalizat"),
    FAILED("Eșuat"),
    CANCELLED("Anulat")
}

/**
 * Data model representing a queued MAST observation dataset in the batch processing engine.
 */
data class MastBatchJob(
    val id: String,
    val ticId: String,
    val targetName: String,
    val mission: String = "TESS",
    val sectorOrQuarter: String = "Auto",
    val priority: Int = 3, // 1 (Highest) to 5 (Lowest)
    val status: MastBatchJobStatus = MastBatchJobStatus.QUEUED,
    val progressFraction: Float = 0.0f,
    val statusMessage: String = "În așteptare în coada de procesare...",
    val addedTimestampMs: Long = System.currentTimeMillis(),
    val startedTimestampMs: Long? = null,
    val completedTimestampMs: Long? = null,
    val result: MastSyncResult? = null,
    val errorMessage: String? = null,
    val isCandidateDiscovered: Boolean = false,
    val candidatePeriodDays: Double? = null,
    val candidateDepthPpm: Double? = null,
    val candidateSnr: Double? = null,
    val bestTemplateName: String? = null,
    val bestTemplateConfidence: Double? = null,
    val dispositionLabel: String = "Nesoluționat"
)

/**
 * High-level aggregated statistics of the active batch queue.
 */
data class MastBatchQueueSummary(
    val totalJobs: Int = 0,
    val queuedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val candidatesDiscoveredCount: Int = 0,
    val totalPhotometricPointsAnalyzed: Int = 0,
    val averageExecutionTimeMs: Long = 0L,
    val isRunning: Boolean = false
)

/**
 * Pre-defined survey campaigns for batch processing across multiple star systems.
 */
data class BatchPresetCampaign(
    val id: String,
    val title: String,
    val description: String,
    val badgeText: String,
    val targets: List<MastObservationTarget>
)

/**
 * Runtime execution configuration for the MAST batch processing engine.
 */
data class BatchEngineConfig(
    val concurrency: Int = 2,
    val minSnrThreshold: Double = 7.1,
    val minDepthPpm: Double = 300.0,
    val autoSaveDiscoveriesToDb: Boolean = true,
    val filterDwarfStarsOnly: Boolean = false
)
