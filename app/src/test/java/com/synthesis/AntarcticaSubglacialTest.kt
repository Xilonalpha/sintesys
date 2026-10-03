package com.synthesis

import com.synthesis.engine.terra.TerraFrontierEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AntarcticaSubglacialTest {

    private lateinit var engine: TerraFrontierEngine

    @Before
    fun setUp() {
        engine = TerraFrontierEngine()
    }

    @Test
    fun testSubglacialLakesCatalog() {
        val lakes = engine.getAntarcticSubglacialLakes()
        assertTrue("Should catalog major Antarctic subglacial lakes", lakes.size >= 5)

        val vostok = lakes.firstOrNull { it.id == "lake_vostok" }
        assertNotNull("Lake Vostok must be present", vostok)
        assertEquals("Ice thickness must match ~3,769m", 3769, vostok!!.iceThicknessMeters)
        assertTrue("Pressure must exceed 300 atm", vostok.pressureAtmospheres > 300.0)
        assertTrue("Research status must mention sterile ice core sampling", vostok.researchStatus.contains("Vostok Core"))

        val mercer = lakes.firstOrNull { it.id == "lake_mercer" }
        assertNotNull("Lake Mercer must be present", mercer)
        assertTrue("SALSA project must be referenced", mercer!!.researchStatus.contains("SALSA"))
    }

    @Test
    fun testBedrockTopographyFeatures() {
        val features = engine.getAntarcticBedrockFeatures()
        assertTrue("Should catalog prominent subglacial bedrock features", features.size >= 4)

        val denman = features.firstOrNull { it.id == "denman_canyon" }
        assertNotNull("Denman Canyon must be present", denman)
        assertEquals("Denman elevation must be -3500m below sea level", -3500, denman!!.bedrockElevationMeters)

        val gamburtsev = features.firstOrNull { it.id == "gamburtsev_mountains" }
        assertNotNull("Gamburtsev Mountains must be present", gamburtsev)
        assertTrue("Peaks must reach positive elevation under ice", gamburtsev!!.bedrockElevationMeters > 2000)
        assertEquals(4000, gamburtsev.iceCoverThicknessMeters)
    }

    @Test
    fun testPolarAltimetryState() {
        val altimetry = engine.getPolarAltimetryState()
        assertEquals("NASA ICESat-2 (ATLAS) & ESA CryoSat-2", altimetry.activeMission)
        assertEquals(27000000.0, altimetry.totalIceVolumeKm3, 0.1)
        assertEquals(58.3, altimetry.seaLevelEquivalentTotalMeters, 0.1)
        assertTrue("Ice mass balance should be negative (net loss)", altimetry.annualIceMassBalanceGtYear < 0)
    }

    @Test
    fun testIsostaticReboundSimulation() {
        // Zero melt
        val zeroMelt = engine.computeIsostaticRebound(0.0f)
        assertEquals(0.0, zeroMelt.globalSeaLevelRiseMeters, 0.01)
        assertEquals(0.0, zeroMelt.crustalReboundMaxMeters, 0.01)

        // 100% full melt
        val fullMelt = engine.computeIsostaticRebound(1.0f)
        assertEquals(58.3, fullMelt.globalSeaLevelRiseMeters, 0.1)
        assertTrue("Crustal rebound should be substantial (~600m)", fullMelt.crustalReboundMaxMeters > 500.0)

        // 50% partial melt
        val halfMelt = engine.computeIsostaticRebound(0.5f)
        assertEquals(29.1, halfMelt.globalSeaLevelRiseMeters, 0.2)
    }
}
