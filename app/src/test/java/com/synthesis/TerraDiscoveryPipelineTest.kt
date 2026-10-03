package com.synthesis

import com.synthesis.domain.model.terra.PipelineStageStatus
import com.synthesis.domain.model.terra.TerraDomain
import com.synthesis.engine.terra.TerraFrontierEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TerraDiscoveryPipelineTest {

    private lateinit var engine: TerraFrontierEngine

    @Before
    fun setUp() {
        engine = TerraFrontierEngine()
    }

    @Test
    fun testPipelineStagesCompleteness() {
        val stages = engine.getInitialPipelineStages()
        assertEquals("Should have 4 distinct discovery pipeline stages", 4, stages.size)
        assertEquals("Stage 1 must be telemetry ingestion", "TELEMETRY_INGESTION", stages[0].codeName)
        assertEquals("Stage 2 must be tomographic inversion", "TOMOGRAPHY_INVERSION", stages[1].codeName)
        assertEquals("Stage 3 must be astrobiological correlation", "ASTROBIOLOGY_CROSS_CORRELATION", stages[2].codeName)
        assertEquals("Stage 4 must be hypothesis synthesis", "HYPOTHESIS_SYNTHESIS", stages[3].codeName)

        stages.forEach { stage ->
            assertTrue("Processed samples must be positive", stage.processedSamples > 0)
            assertTrue("Stage details should be non-empty", stage.details.isNotEmpty())
            assertNotNull("Status must be defined", stage.status)
        }
    }

    @Test
    fun testRealtimeTelemetryFeeds() {
        val feeds = engine.getRealtimeTelemetryFeeds()
        assertTrue("Must provide a live stream of telemetry feeds", feeds.size >= 4)
        feeds.forEach { feed ->
            assertTrue("Anomaly score should be between 0.0 and 1.0", feed.anomalyScore in 0.0..1.0)
            assertTrue("Timestamp must be populated", feed.timestamp.isNotBlank())
            assertTrue("Formula badge must be present", feed.formulaBadge.isNotBlank())
            assertNotNull(feed.sourceDomain)
        }
    }

    @Test
    fun testFrontierProgressMetrics() {
        val metrics = engine.getFrontierProgressMetrics()
        assertEquals(5, metrics.size)
        metrics.forEach { metric ->
            assertTrue("Progress percent should be in range [0, 1]", metric.currentPercent in 0.0f..1.0f)
            assertTrue(metric.unitValue.isNotBlank())
            assertTrue(metric.targetLabel.isNotBlank())
        }
    }
}
