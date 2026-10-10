package com.synthesis

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.synthesis.data.remote.api.*
import com.synthesis.data.remote.dto.*
import com.synthesis.engine.exoplanet.BayesianUncertaintyEngine
import com.synthesis.service.MastDataSynchronizationService
import com.synthesis.service.PeriodicTransitAnalysisEngine
import com.synthesis.service.StellarTemplateLibraryService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MastDataSynchronizationServiceTest {

    // Lightweight mock implementations for Unit Testing
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

    private val service = MastDataSynchronizationService(
        mastExoApi = mockMastExoApi,
        mastPortalApi = mockMastPortalApi,
        exoplanetArchiveApi = mockExoplanetArchiveApi,
        periodicTransitAnalysisEngine = periodicEngine,
        bayesianUncertaintyEngine = bayesianEngine,
        stellarTemplateLibraryService = templateService
    )

    @Test
    fun testCuratedTargetsAvailability() {
        val targets = service.getCuratedTargets()
        assertTrue("Curated targets list should not be empty", targets.isNotEmpty())
        assertTrue("Must include TOI-1895.01", targets.any { it.ticId == "384159646" })
        assertTrue("Must include TOI-2076.03", targets.any { it.ticId == "268301217" })
        assertTrue("Must include TOI-4633", targets.any { it.ticId == "100100827" })
        assertTrue("Must include WASP-126 b", targets.any { it.ticId == "25155310" })
        assertTrue("Must include WASP-119 b", targets.any { it.ticId == "388104525" })
    }

    @Test
    fun testLiveSyncExecutionAndImmediateMultiEngineAnalysis() = runBlocking {
        val res = service.syncLiveObservationData("TOI-1895.01", "TESS")
        assertTrue("Live sync result should be success", res.isSuccess)

        val syncResult = res.getOrThrow()
        assertEquals("TIC 384159646 (TOI-1895.01)", syncResult.target.targetName)
        assertEquals("384159646", syncResult.target.ticId)
        assertTrue("Points should be extracted", syncResult.dataPoints.size >= 30)

        // Verify periodic BLS detection
        assertNotNull(syncResult.periodicDetection)
        assertTrue("Best period should be > 0", syncResult.periodicDetection.bestPeriodDays > 0.0)

        // Verify astrophysical template cross-matching
        assertTrue("Template scores should be populated", syncResult.templateScores.isNotEmpty())
        val bestTemplate = syncResult.templateScores.first()
        assertNotNull(bestTemplate)
        println("Best matched template: ${bestTemplate.template.name} with ${bestTemplate.confidencePercent}% confidence")

        // Verify reactive sync state
        val state = service.syncState.value
        assertFalse("Syncing flag should be reset to false", state.isSyncing)
        assertEquals(syncResult.dataPoints.size, state.syncedPointsCount)
        assertNotNull(state.syncedMission)
        assertNull(state.activeError)
    }

    @Test
    fun testEmpiricalFallbackForCustomUnknownTic() = runBlocking {
        val res = service.syncLiveObservationData("TIC 999999999", "TESS")
        assertTrue("Fallback data should be generated successfully", res.isSuccess)

        val syncResult = res.getOrThrow()
        assertTrue("Should contain fallback observation points", syncResult.dataPoints.isNotEmpty())
        assertTrue(syncResult.templateScores.isNotEmpty())
    }
}
