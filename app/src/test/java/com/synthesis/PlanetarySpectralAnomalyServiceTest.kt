package com.synthesis

import com.synthesis.data.remote.api.ExoplanetArchiveApi
import com.synthesis.data.remote.dto.*
import com.synthesis.data.repository.exoplanet.PublicSpectralDatasetRegistry
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.service.PlanetarySpectralAnomalyService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlanetarySpectralAnomalyServiceTest {

    private lateinit var registry: PublicSpectralDatasetRegistry
    private lateinit var mockApi: ExoplanetArchiveApi
    private lateinit var service: PlanetarySpectralAnomalyService

    @Before
    fun setUp() {
        registry = PublicSpectralDatasetRegistry()
        mockApi = object : ExoplanetArchiveApi {
            override suspend fun queryToiTable(adqlQuery: String, format: String): List<TessObjectOfInterestDto> = emptyList()
            override suspend fun queryKeplerTable(adqlQuery: String, format: String): List<KeplerKoiDto> = emptyList()
            override suspend fun queryK2Table(adqlQuery: String, format: String): List<K2CandidateDto> = emptyList()
            override suspend fun queryConfirmedTable(adqlQuery: String, format: String): List<ConfirmedExoplanetDto> {
                return if (adqlQuery.contains("WASP-39")) {
                    listOf(
                        ConfirmedExoplanetDto(
                            plName = "WASP-39 b",
                            hostName = "WASP-39",
                            ticId = null,
                            discoveryMethod = "Transit",
                            discYear = 2011,
                            discFacility = "WASP-South / JWST Transiting Exoplanet ERS",
                            plOrbPer = 4.055,
                            plTranDur = 2.8,
                            plTranDep = 21200.0,
                            plRadE = 14.3,
                            plBmassE = 89.0,
                            plEqt = 1166.0,
                            plInsol = null,
                            syDist = 215.0,
                            syVmag = 12.1,
                            stTeff = 5485.0,
                            stRad = 0.93
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }
        service = PlanetarySpectralAnomalyService(registry, mockApi)
    }

    @Test
    fun testPublicRegistryContainsExpectedSpaceTelescopeBenchmarks() {
        val benchmarks = registry.getAllBenchmarks()
        assertTrue("Registry must contain published benchmarks", benchmarks.isNotEmpty())

        val wasp39b = registry.findBenchmarkById("NASA-JWST-WASP-39b-PRISM")
        assertNotNull("WASP-39 b JWST ERS benchmark must be present", wasp39b)
        assertTrue(wasp39b!!.verifiedMolecules.contains("CO2"))
        assertTrue(wasp39b.verifiedMolecules.contains("SO2"))

        val k218b = registry.findBenchmarkById("NASA-JWST-K2-18b-NIRSPEC")
        assertNotNull("K2-18 b Hycean benchmark must be present", k218b)
        assertTrue(k218b!!.verifiedMolecules.contains("CH4"))
    }

    @Test
    fun testIdentifyMoleculesAtWavelength() {
        val co2Bands = registry.identifyMoleculesAtWavelength(4.30)
        assertTrue("4.30 um should match CO2 or PH3", co2Bands.any { it.moleculeFormula == "CO2" || it.moleculeFormula == "PH3" })

        val so2Bands = registry.identifyMoleculesAtWavelength(4.05)
        assertTrue("4.05 um should match SO2 photochemistry", so2Bands.any { it.moleculeFormula == "SO2" })

        val dmsBands = registry.identifyMoleculesAtWavelength(3.40)
        assertTrue("3.40 um should match DMS biosignature candidate", dmsBands.any { it.moleculeFormula == "DMS" })
        assertTrue(registry.isExoticOrBiosignatureMolecule("DMS"))
    }

    @Test
    fun testCompareUnknownTargetIdentifiesUniqueAnomaliesAndHighNovelty() {
        val curated = service.getCuratedRetrievedSpectra()
        val candidate = curated.first { it.targetName.contains("TIC 141913165") }

        val result = service.compareAgainstPublicRegistry(candidate)

        assertNotNull(result)
        assertFalse("Candidate must not be registered in public benchmark archive", result.isTargetKnownInPublicRegistry)
        assertTrue("Novelty index must be very high for uncataloged candidate with DMS & PH3", result.noveltyIndexPercent >= 75.0)
        assertTrue("Identified anomalies list must not be empty", result.identifiedAnomalies.isNotEmpty())

        val zeroPrior = result.identifiedAnomalies.find { it.anomalyType == SpectralAnomalyType.ZERO_PRIOR_ARCHIVE_TARGET }
        assertNotNull("Must include zero prior archive anomaly", zeroPrior)

        val exoticAnomalies = result.identifiedAnomalies.filter { it.anomalyType == SpectralAnomalyType.UNPRECEDENTED_MOLECULAR_BAND }
        assertTrue("Must detect unprecedented molecular band anomaly", exoticAnomalies.isNotEmpty())
        assertTrue("Max deviation sigma should be high (> 4 sigma)", result.maxDeviationSigma >= 4.0)
    }

    @Test
    fun testCompareWasp39bVerificationConformsWithPublicRegistry() {
        val curated = service.getCuratedRetrievedSpectra()
        val wasp39 = curated.first { it.targetName.contains("WASP-39") }

        val result = service.compareAgainstPublicRegistry(wasp39, "NASA-JWST-WASP-39b-PRISM")

        assertTrue("WASP-39 b is a recognized benchmark target", result.isTargetKnownInPublicRegistry)
        assertTrue("Novelty index should be low for exact calibration benchmark", result.noveltyIndexPercent < 50.0)
        assertTrue("Verdict should indicate conformance", result.scientificVerdict.contains("CONFORM") || result.scientificVerdict.contains("MODERAT"))
    }

    @Test
    fun testMetastableHeliumExosphereEscapeDetection() {
        val curated = service.getCuratedRetrievedSpectra()
        val heliumTarget = curated.first { it.targetName.contains("TOI-1895") }

        val result = service.compareAgainstPublicRegistry(heliumTarget)

        val heAnomaly = result.identifiedAnomalies.find {
            it.anomalyType == SpectralAnomalyType.ANOMALOUS_DEPTH_SURGE &&
                    it.candidateMoleculesOrMechanism.any { mol -> mol.contains("He I") }
        }
        assertNotNull("Must identify metastable Helium 1.083 um exosphere escape anomaly", heAnomaly)
        assertTrue("Helium anomaly significance must be > 3 sigma", heAnomaly!!.statisticalSignificanceSigma >= 3.0)
    }

    @Test
    fun testNasaArchiveTapDispositionQuery() = runBlocking {
        val knownDisposition = service.queryNasaArchiveForTarget("WASP-39 b")
        assertTrue("WASP-39 b should be found in mock archive", knownDisposition.isPresentInNasaArchive)
        assertEquals(1, knownDisposition.confirmedPlanetsCount)

        val unknownDisposition = service.queryNasaArchiveForTarget("TIC 141913165.01")
        assertFalse("Uncataloged TIC target should have zero archive records", unknownDisposition.isPresentInNasaArchive)
        assertEquals(0, unknownDisposition.confirmedPlanetsCount)
    }
}
