package com.synthesis

import com.synthesis.data.local.entity.CachedExoplanetEntity
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.domain.model.TfopwgDisposition
import org.junit.Assert.*
import org.junit.Test

class CachedExoplanetEntityTest {

    @Test
    fun testDomainToEntityAndBackMapping() {
        val original = ExoplanetCandidate(
            toiId = "TOI-4398.01",
            ticId = "TIC 284144353",
            disposition = TfopwgDisposition.PC,
            catalogSource = ExoplanetCatalogSource.TESS,
            orbitalPeriodDays = 14.85,
            transitDurationHours = 3.2,
            transitDepthPpm = 1450.0,
            planetRadiusEarth = 2.45,
            planetMassEarth = 6.8,
            insolationEarthFlux = 1.15,
            equilibriumTempK = 278.0,
            stellarTessMagnitude = 10.2,
            stellarDistancePc = 42.5,
            stellarEffTempK = 5400.0,
            stellarRadiusSun = 0.88,
            createdDate = "2024-05-12",
            lastUpdated = "2026-03-15",
            worthInvestigatingScore = 0.92f,
            earthSimilarityIndex = 0.84f,
            isInHabitableZone = true,
            isUnvettedCandidate = true,
            anomalyType = "Super-Earth in Habitable Zone",
            discoveryMethod = "Transit Photometry",
            discoveryYear = 2026,
            scoreReasoning = listOf("High SNR transit", "Host star is bright", "Equilibrium temperature allows liquid water")
        )

        val entity = CachedExoplanetEntity.fromDomain(original)
        assertEquals("TOI-4398.01", entity.toiId)
        assertEquals("TIC 284144353", entity.ticId)
        assertEquals("PC", entity.disposition)
        assertEquals("TESS", entity.catalogSource)
        assertEquals(3, entity.scoreReasoningJoined.split(";;").size)

        val convertedBack = entity.toDomain()
        assertEquals(original.toiId, convertedBack.toiId)
        assertEquals(original.ticId, convertedBack.ticId)
        assertEquals(original.disposition, convertedBack.disposition)
        assertEquals(original.catalogSource, convertedBack.catalogSource)
        assertEquals(original.orbitalPeriodDays, convertedBack.orbitalPeriodDays)
        assertEquals(original.transitDepthPpm, convertedBack.transitDepthPpm)
        assertEquals(original.planetRadiusEarth, convertedBack.planetRadiusEarth)
        assertEquals(original.worthInvestigatingScore, convertedBack.worthInvestigatingScore, 0.001f)
        assertEquals(original.earthSimilarityIndex, convertedBack.earthSimilarityIndex)
        assertTrue(convertedBack.isInHabitableZone)
        assertTrue(convertedBack.isUnvettedCandidate)
        assertEquals(3, convertedBack.scoreReasoning.size)
        assertEquals("High SNR transit", convertedBack.scoreReasoning[0])
    }
}
