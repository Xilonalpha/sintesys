package com.synthesis

import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.engine.exoplanet.ExoplanetCandidateFinder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MultiCatalogExoplanetTest {

    private lateinit var finder: ExoplanetCandidateFinder

    @Before
    fun setUp() {
        finder = ExoplanetCandidateFinder()
    }

    @Test
    fun testPhlHabitableCatalogContainsGoldStandardPlanets() {
        val phlPlanets = finder.getPhlHabitableCatalog()
        assertTrue("PHL catalog should have at least 8 top habitable candidates", phlPlanets.size >= 8)

        // Check Teegarden's Star b (highest ESI)
        val teegarden = phlPlanets.firstOrNull { it.toiId.contains("Teegarden") }
        assertNotNull("Teegarden's Star b must be present", teegarden)
        assertEquals(0.95f, teegarden!!.earthSimilarityIndex ?: 0f, 0.01f)
        assertEquals(ExoplanetCatalogSource.PHL_HABITABLE, teegarden.catalogSource)
        assertTrue(teegarden.isInHabitableZone)

        // Check TOI-700 d (TESS prime discovery)
        val toi700d = phlPlanets.firstOrNull { it.toiId == "TOI-700 d" }
        assertNotNull("TOI-700 d must be present", toi700d)
        assertEquals(0.93f, toi700d!!.earthSimilarityIndex ?: 0f, 0.01f)

        // Check Proxima Centauri b
        val proxima = phlPlanets.firstOrNull { it.toiId.contains("Proxima Centauri b") }
        assertNotNull("Proxima Centauri b must be present", proxima)
        assertEquals(0.87f, proxima!!.earthSimilarityIndex ?: 0f, 0.01f)

        // Check TRAPPIST-1 e
        val trappist = phlPlanets.firstOrNull { it.toiId.contains("TRAPPIST-1 e") }
        assertNotNull("TRAPPIST-1 e must be present", trappist)
        assertEquals(0.85f, trappist!!.earthSimilarityIndex ?: 0f, 0.01f)

        // Check LHS 1140 b
        val lhs1140 = phlPlanets.firstOrNull { it.toiId.contains("LHS 1140 b") }
        assertNotNull("LHS 1140 b must be present", lhs1140)
    }

    @Test
    fun testCheopsAndGaiaCatalog() {
        val cheopsPlanets = finder.getCheopsAndGaiaCatalog()
        assertTrue("CHEOPS & Gaia catalog should contain key European mission targets", cheopsPlanets.size >= 5)

        val nu2Lupi = cheopsPlanets.firstOrNull { it.toiId.contains("Nu2 Lupi d") }
        assertNotNull("Nu2 Lupi d must be present", nu2Lupi)
        assertEquals(ExoplanetCatalogSource.CHEOPS_GAIA, nu2Lupi!!.catalogSource)

        val ltt9779 = cheopsPlanets.firstOrNull { it.toiId.contains("LTT 9779 b") }
        assertNotNull("LTT 9779 b must be present", ltt9779)
        assertEquals("Neptunian Desert Anomaly", ltt9779!!.anomalyType)

        val wasp103 = cheopsPlanets.firstOrNull { it.toiId.contains("WASP-103 b") }
        assertNotNull("WASP-103 b must be present", wasp103)
        assertEquals("Ultra-Short Period Hot Jupiter (USP)", wasp103!!.anomalyType)
    }
}
