package com.synthesis

import com.google.gson.JsonParser
import com.synthesis.data.local.dao.PureTicDao
import com.synthesis.data.local.dao.XilonDiscoveryDao
import com.synthesis.data.local.entity.PureTicEntity
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import com.synthesis.domain.model.xilon.*
import com.synthesis.engine.astrometry.AstrometryEngine
import com.synthesis.service.NasaExoFopExporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class NasaExoFopExporterTest {

    private val astrometryEngine = AstrometryEngine()

    private val fakeDiscoveries = mutableListOf<XilonDiscoveryEntity>()
    private val fakePureTics = mutableListOf<PureTicEntity>()

    private val fakeXilonDao = object : XilonDiscoveryDao {
        override fun getAllDiscoveries(): Flow<List<XilonDiscoveryEntity>> = flowOf(fakeDiscoveries)
        override suspend fun getDiscoveriesList(): List<XilonDiscoveryEntity> = fakeDiscoveries
        override suspend fun getByTicId(ticId: String): XilonDiscoveryEntity? = fakeDiscoveries.firstOrNull { it.ticId == ticId }
        override suspend fun insertDiscovery(discovery: XilonDiscoveryEntity): Long {
            fakeDiscoveries.add(discovery)
            return fakeDiscoveries.size.toLong()
        }
        override suspend fun deleteById(id: Int) {
            fakeDiscoveries.removeAll { it.id == id }
        }
        override suspend fun clearAll() {
            fakeDiscoveries.clear()
        }
    }

    private val fakePureTicDao = object : PureTicDao {
        override fun observeAll(): Flow<List<PureTicEntity>> = flowOf(fakePureTics)
        override suspend fun getAll(): List<PureTicEntity> = fakePureTics
        override suspend fun insertAll(candidates: List<PureTicEntity>) { fakePureTics.addAll(candidates) }
        override suspend fun insert(candidate: PureTicEntity) { fakePureTics.add(candidate) }
        override suspend fun getByTicId(ticId: String): PureTicEntity? = fakePureTics.firstOrNull { it.ticId == ticId }
        override suspend fun getCount(): Int = fakePureTics.size
        override suspend fun deleteOldestLive(count: Int) {}
        override suspend fun clearAll() { fakePureTics.clear() }
    }

    private val dummyContext: android.content.Context by lazy {
        org.mockito.Mockito.mock(android.content.Context::class.java).apply {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "synthesis_test_files")
            tempDir.mkdirs()
            org.mockito.Mockito.`when`(filesDir).thenReturn(tempDir)
        }
    }

    private lateinit var exporter: NasaExoFopExporter

    @Before
    fun setUp() {
        fakeDiscoveries.clear()
        fakePureTics.clear()

        // Seed with sample discoveries
        fakeDiscoveries.add(
            XilonDiscoveryEntity(
                id = 1,
                ticId = "150428135",
                canonicalName = "TOI-700 d",
                sector = "Sector 11-13",
                verdict = "CONFIRMED_EXOPLANET",
                verdictTitle = "EXOPLANET CONFIRMED",
                verdictSummary = "Habitable-zone Earth-size planet orbiting M-dwarf",
                periodDays = 37.426,
                depthPpm = 860.0,
                durationHours = 2.85,
                planetRadiusEarth = 1.14,
                planetMassEarth = 1.72,
                surfaceGravityG = 1.32,
                bulkDensityGcm3 = 5.82,
                equilibriumTempK = 269.0,
                snr = 14.8,
                isHabitableZone = true,
                archetype = "Super-Earth",
                archivalStatus = "CONFIRMED_PLANET_VALIDATION",
                raDeg = 97.0957,
                decDeg = -55.5786,
                epochBjd = 2458600.25,
                scannedAt = 1700000000000L
            )
        )

        fakeDiscoveries.add(
            XilonDiscoveryEntity(
                id = 2,
                ticId = "268301217",
                canonicalName = "TIC 268301217.01",
                sector = "Sector 39",
                verdict = "STRONG_CANDIDATE",
                verdictTitle = "STRONG CANDIDATE (PC)",
                verdictSummary = "Clear box transit with high SNR",
                periodDays = 8.45,
                depthPpm = 3200.0,
                durationHours = 3.1,
                planetRadiusEarth = 2.45,
                planetMassEarth = 6.8,
                surfaceGravityG = 1.13,
                bulkDensityGcm3 = 2.31,
                equilibriumTempK = 650.0,
                snr = 22.4,
                isHabitableZone = false,
                archetype = "Sub-Neptune",
                archivalStatus = "TESS_UNVETTED_CANDIDATE",
                raDeg = 117.7583,
                decDeg = -64.6147,
                epochBjd = 2459400.12,
                scannedAt = 1700000000000L
            )
        )

        val validationEngine = com.synthesis.engine.validation.TessHeasarcValidationEngine(astrometryEngine)

        exporter = NasaExoFopExporter(
            context = dummyContext,
            xilonDiscoveryDao = fakeXilonDao,
            pureTicDao = fakePureTicDao,
            astrometryEngine = astrometryEngine,
            validationEngine = validationEngine
        )
    }

    @Test
    fun testExportAllDiscoveriesJson() = runBlocking {
        println("=== TEST 1: NASA ExoFOP-TESS JSON Export Serialization ===")
        val jsonStr = exporter.exportAllDiscoveriesJson()
        assertNotNull(jsonStr)
        assertTrue(jsonStr.isNotBlank())

        val root = JsonParser.parseString(jsonStr).asJsonObject
        assertEquals("NASA Transiting Exoplanet Survey Satellite (TESS)", root.get("mission").asString)
        assertEquals(2, root.get("totalDiscoveriesExported").asInt)

        val discoveries = root.getAsJsonArray("discoveries")
        assertEquals(2, discoveries.size())

        val toi700 = discoveries[0].asJsonObject
        assertEquals(150428135L, toi700.get("ticId").asLong)
        assertEquals("TOI-700 d", toi700.get("targetName").asString)
        assertEquals(97.0957, toi700.get("raDeg").asDouble, 0.0001)
        assertEquals(-55.5786, toi700.get("decDeg").asDouble, 0.0001)
        assertEquals("ICRS_J2000", toi700.get("coordFrame").asString)
        assertEquals(860.0, toi700.get("depthPpm").asDouble, 0.1)
        assertEquals(0.086, toi700.get("depthPercent").asDouble, 0.001)
        assertEquals(14.8, toi700.get("signalToNoiseRatio").asDouble, 0.1)
        assertEquals("KP", toi700.get("ctoiDisposition").asString) // Known/Confirmed Planet
        assertTrue(toi700.get("isHabitableZone").asBoolean)
    }

    @Test
    fun testExportAllDiscoveriesCsv() = runBlocking {
        println("=== TEST 2: NASA ExoFOP-TESS CTOI CSV Export Serialization ===")
        val csvStr = exporter.exportAllDiscoveriesCsv()
        assertNotNull(csvStr)
        assertTrue(csvStr.isNotBlank())

        // Validate comment headers
        assertTrue("CSV must contain NExScI specification line", csvStr.contains("Specification: NExScI ExoFOP-TESS CTOI Standard v2.4"))
        assertTrue("CSV must contain ICRS reference frame line", csvStr.contains("Coordinate Reference Frame: ICRS J2000"))

        // Validate column headers
        assertTrue("CSV must contain column header TIC_ID", csvStr.contains("TIC_ID,TARGET_NAME,SECTOR,RA_DEG,DEC_DEG"))
        assertTrue("CSV must contain photometric columns", csvStr.contains("DEPTH_PPM,DEPTH_PERCENT,SNR_SIGMA"))

        // Validate data rows
        assertTrue("CSV must contain TOI-700 d row", csvStr.contains("150428135"))
        assertTrue("CSV must contain TIC 268301217 row", csvStr.contains("268301217"))
        assertTrue("CSV must contain PC disposition for high-SNR candidate", csvStr.contains(",PC,"))
        assertTrue("CSV must contain KP disposition for confirmed planet", csvStr.contains(",KP,"))
    }

    @Test
    fun testDirectDetectionResultExport() = runBlocking {
        println("=== TEST 3: Direct XilonDetectionResult JSON/CSV Export ===")
        val sampleStar = StellarTarget(
            ticId = "25155310",
            ra = 142.1584,
            dec = 18.2351,
            tMag = 11.2,
            radiusSun = 0.95,
            massSun = 0.92,
            tEff = 5400.0
        )

        val sampleDetection = XilonDetectionResult(
            star = sampleStar,
            sector = "Sector 45",
            tceName = "TCE_1",
            orbitalPeriodDays = 4.23,
            transitDurationHours = 2.4,
            transitDepthPpm = 2100.0,
            signalToNoiseRatio = 12.5,
            oddEvenTransitDeltaSigma = 0.3,
            secondaryEclipseDepthPpm = 0.0,
            derivedPlanetRadiusEarth = 1.95,
            derivedPlanetMassEarth = 4.5,
            surfaceGravityEarthG = 1.18,
            planetDensityGcm3 = 3.2,
            derivedEquilibriumTempK = 580.0,
            isHabitableZone = false,
            compositionClass = CompositionClass.WATER_WORLD,
            verdict = DetectionVerdict.STRONG_CANDIDATE,
            verdictTitle = "STRONG CANDIDATE (PC)",
            verdictSummary = "Candidate transit detected with high significance",
            astrometry = AstrometryEngine.AstrometricTargetDossier(
                raDeg = 142.1584,
                decDeg = 18.2351,
                formattedRaSexagesimal = "09h 28m 38.02s",
                formattedDecSexagesimal = "+18° 14' 06.4\"",
                galactic = AstrometryEngine.GalacticCoordinates(214.5, 42.1, "214.50°", "+42.10°"),
                bjdTdbCurrent = 2460100.5,
                btjdCurrent = 3100.5,
                nasaAdsUrl = "https://ads.harvard.edu",
                arxivUrl = "https://arxiv.org",
                simbadUrl = "https://simbad.u-strasbg.fr"
            ),
            archivalCrossMatch = ArchivalCrossMatch(
                canonicalName = "TIC 25155310.01",
                status = ArchivalCatalogStatus.TESS_UNVETTED_CANDIDATE,
                toiId = null,
                catalogSource = "TESS SpOC Pipeline"
            )
        )

        val json = exporter.exportDetectionResultJson(sampleDetection)
        assertNotNull(json)
        assertTrue(json.contains("\"ticId\": 25155310"))
        assertTrue(json.contains("\"depthPpm\": 2100.0"))
        assertTrue(json.contains("\"signalToNoiseRatio\": 12.5"))
        assertTrue(json.contains("\"ctoiDisposition\": \"PC\""))

        val csv = exporter.exportDetectionResultCsv(sampleDetection)
        assertNotNull(csv)
        assertTrue(csv.contains("25155310"))
        assertTrue(csv.contains("142.158400"))
        assertTrue(csv.contains("18.235100"))
        assertTrue(csv.contains("2100.0"))
    }
}
