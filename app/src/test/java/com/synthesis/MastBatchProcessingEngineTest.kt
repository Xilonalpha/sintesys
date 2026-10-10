package com.synthesis

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.synthesis.data.local.dao.SynthesizedResearchDao
import com.synthesis.data.local.entity.SynthesizedResearchEntity
import com.synthesis.data.remote.api.*
import com.synthesis.data.remote.dto.*
import com.synthesis.data.repository.SynthesizedResearchRepository
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.engine.exoplanet.BayesianUncertaintyEngine
import com.synthesis.service.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MastBatchProcessingEngineTest {

    private val mockMastExoApi = object : MastExoApi {
        override suspend fun getTces(ticId: String): JsonObject {
            val root = JsonObject()
            val arr = JsonArray()
            arr.add("s0039-s0039:TCE_1")
            root.add("TCE", arr)
            return root
        }

        override suspend fun getLightCurveTable(ticId: String, tce: String, sector: String): JsonObject {
            val root = JsonObject()
            val data = JsonArray()
            for (i in 0 until 40) {
                val row = JsonObject()
                val phase = -0.15 + (i * 0.0075)
                val flux = if (phase in -0.02..0.02) 0.988 else 1.000
                row.addProperty("PHASE", phase)
                row.addProperty("LC_DETREND", flux)
                data.add(row)
            }
            root.add("data", data)
            return root
        }

        override suspend fun getDvInfo(ticId: String, tce: String, sector: String): JsonObject {
            val root = JsonObject()
            val hdr = JsonObject()
            hdr.addProperty("TDUR", 4.2)
            hdr.addProperty("TDEPTH", 12000.0)
            hdr.addProperty("TPERIOD", 3.52)
            root.add("DV Data Header", hdr)
            return root
        }
    }

    private val mockMastPortalApi = object : MastPortalApi {
        override suspend fun invokeService(requestJson: String): JsonObject = JsonObject()
    }

    private val mockExoplanetArchiveApi = object : ExoplanetArchiveApi {
        override suspend fun queryToiTable(adqlQuery: String, format: String): List<TessObjectOfInterestDto> = emptyList()
        override suspend fun queryKeplerTable(adqlQuery: String, format: String): List<KeplerKoiDto> = emptyList()
        override suspend fun queryK2Table(adqlQuery: String, format: String): List<K2CandidateDto> = emptyList()
        override suspend fun queryConfirmedTable(adqlQuery: String, format: String): List<ConfirmedExoplanetDto> = emptyList()
    }

    private val bayesianEngine = BayesianUncertaintyEngine()
    private val periodicEngine = PeriodicTransitAnalysisEngine(bayesianEngine)
    private val templateService = StellarTemplateLibraryService()

    private val syncService = MastDataSynchronizationService(
        mastExoApi = mockMastExoApi,
        mastPortalApi = mockMastPortalApi,
        exoplanetArchiveApi = mockExoplanetArchiveApi,
        periodicTransitAnalysisEngine = periodicEngine,
        bayesianUncertaintyEngine = bayesianEngine,
        stellarTemplateLibraryService = templateService
    )

    private val savedEntities = mutableListOf<SynthesizedResearchEntity>()

    private val mockResearchDao = object : SynthesizedResearchDao {
        override fun getAllResearch(): Flow<List<SynthesizedResearchEntity>> = flowOf(savedEntities)
        override fun getBookmarkedResearch(): Flow<List<SynthesizedResearchEntity>> = flowOf(savedEntities.filter { it.isBookmarked })
        override fun getResearchById(id: String): Flow<SynthesizedResearchEntity?> = flowOf(savedEntities.find { it.id == id })
        override fun getResearchByCategory(category: String): Flow<List<SynthesizedResearchEntity>> = flowOf(savedEntities.filter { it.researchCategory == category })
        override fun searchResearch(query: String): Flow<List<SynthesizedResearchEntity>> = flowOf(savedEntities.filter { it.title.contains(query, ignoreCase = true) })
        override suspend fun insertResearch(research: SynthesizedResearchEntity) { savedEntities.add(research) }
        override suspend fun insertAll(researchList: List<SynthesizedResearchEntity>) { savedEntities.addAll(researchList) }
        override suspend fun updateResearch(research: SynthesizedResearchEntity) {}
        override suspend fun setBookmarkStatus(id: String, isBookmarked: Boolean) {}
        override suspend fun deleteResearch(research: SynthesizedResearchEntity) { savedEntities.remove(research) }
        override suspend fun deleteResearchById(id: String) { savedEntities.removeAll { it.id == id } }
        override suspend fun clearAllResearch() { savedEntities.clear() }
    }

    private val repository = SynthesizedResearchRepository(mockResearchDao)

    private val batchEngine = MastBatchProcessingEngine(
        mastDataSynchronizationService = syncService,
        synthesizedResearchRepository = repository
    )

    @Test
    fun testCampaignPresetsAndBulkQueueing() {
        println("=== TEST: BATCH PROCESSING ENGINE - QUEUEING & PRESETS ===")
        val campaigns = batchEngine.getAvailableCampaigns()
        assertTrue("Should offer campaigns", campaigns.isNotEmpty())
        assertTrue("Should include Habitable Zone campaign", campaigns.any { it.id == "camp_habitable_zone" })

        // Test bulk text parsing
        val rawInput = "384159646, 268301217, 100100827; 25155310\n388104525"
        val count = batchEngine.enqueueByTicString(rawInput, "TESS")
        assertEquals(5, count)

        val queue = batchEngine.jobs.value
        assertEquals(5, queue.size)
        assertEquals("384159646", queue[0].ticId)
        assertEquals("268301217", queue[1].ticId)
        assertEquals(MastBatchJobStatus.QUEUED, queue[0].status)

        // Test Summary calculation
        val summary = batchEngine.summary.value
        assertEquals(5, summary.totalJobs)
        assertEquals(5, summary.queuedCount)
        assertEquals(0, summary.completedCount)
    }

    @Test
    fun testBatchExecutionAndAutomatedDiscovery() = runBlocking {
        println("=== TEST: BATCH PROCESSING ENGINE - EXECUTION & AUTO-DISCOVERY ===")
        savedEntities.clear()

        // Enqueue 2 targets
        batchEngine.clearQueue()
        batchEngine.enqueueTarget(
            MastObservationTarget(
                id = "target_toi1895",
                targetName = "TIC 384159646 (TOI-1895.01)",
                ticId = "384159646",
                sectorOrQuarter = "s0039",
                description = "Mono-transit test target"
            )
        )

        assertEquals(1, batchEngine.jobs.value.size)
        assertEquals(MastBatchJobStatus.QUEUED, batchEngine.jobs.value.first().status)

        // Start batch processing
        batchEngine.startProcessing()
        assertTrue("Engine should be running", batchEngine.isRunning.value)

        // Wait for single job processing
        var attempts = 0
        while (batchEngine.isRunning.value && attempts < 30) {
            kotlinx.coroutines.delay(100)
            attempts++
        }

        val completedJobs = batchEngine.jobs.value
        val job = completedJobs.first()
        assertEquals(MastBatchJobStatus.COMPLETED, job.status)
        assertNotNull(job.result)
        assertTrue("Photometric points should be analyzed", (job.result?.dataPoints?.size ?: 0) >= 30)
        assertNotNull(job.candidatePeriodDays)
        assertNotNull(job.candidateSnr)

        // Verify summary
        val sum = batchEngine.summary.value
        assertEquals(1, sum.totalJobs)
        assertEquals(1, sum.completedCount)
        assertEquals(0, sum.queuedCount)

        // If candidate met discovery criteria, it should be auto-saved in Room repository
        if (job.isCandidateDiscovered) {
            assertTrue("Candidate must be saved to Room", savedEntities.isNotEmpty())
            val saved = savedEntities.first()
            assertTrue("Saved title must contain TIC", saved.title.contains("384159646"))
            assertEquals("EXOPLANET", saved.researchCategory)
            println("Auto-saved discovery in Room: ${saved.title}")
        }
    }
}
