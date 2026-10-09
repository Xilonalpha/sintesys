package com.synthesis.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.*
import com.synthesis.service.worker.AstronomicalSyncManager
import com.synthesis.service.worker.CelestialSyncInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SynthesisRepository,
    private val astronomicalSyncManager: AstronomicalSyncManager
) : ViewModel() {

    val investigations: StateFlow<List<Investigation>> = repository.getInvestigations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveries: StateFlow<List<Discovery>> = repository.getAllDiscoveries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<Investigation>> = investigations.map { list ->
        list.filter { it.status == InvestigationStatus.ACTIVE || it.status == InvestigationStatus.ANALYZING || it.status == InvestigationStatus.CORRELATING || it.status == InvestigationStatus.PENDING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _systemStatus = MutableStateFlow(
        SystemStatus(dataSources = 15, liveConnections = 5, activeAnalyses = 2, anomalies = 4)
    )
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    private val _syncInfo = MutableStateFlow(
        CelestialSyncInfo(0L, "SCHEDULED (1h WorkManager)", 4, 0, 0f)
    )
    val syncInfo: StateFlow<CelestialSyncInfo> = _syncInfo.asStateFlow()

    init {
        // Auto-seed benchmark discoveries if database is initially empty
        viewModelScope.launch {
            repository.getAllDiscoveries().firstOrNull()?.let { current ->
                if (current.isEmpty()) {
                    seedBenchmarkDiscoveries()
                }
            }
        }
    }

    fun loadSyncInfo(context: Context) {
        _syncInfo.value = astronomicalSyncManager.getSyncInfo(context)
    }

    fun triggerLiveSync(context: Context) {
        astronomicalSyncManager.triggerImmediateSync(context)
        _syncInfo.value = _syncInfo.value.copy(status = "SYNCING LIVE...")
    }

    fun createInvestigation(title: String, query: String) {
        viewModelScope.launch { repository.createInvestigation(title, query) }
    }

    fun initiateResearchProject(
        title: String,
        query: String,
        domain: String = "Astrophysics",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val investigation = repository.createInvestigation(title = title, query = query)
            repository.updateInvestigationStats(
                id = investigation.id,
                sources = 6,
                anomalies = 2,
                confidence = 0.88f
            )
            repository.updateInvestigationStatus(investigation.id, InvestigationStatus.ACTIVE)
            onCreated(investigation.id)
        }
    }

    fun seedBenchmarkDiscoveries() {
        viewModelScope.launch {
            val benchmarkDiscoveries = listOf(
                Discovery(
                    id = "disc_wasp39b_co2",
                    investigationId = "benchmark_inv_01",
                    title = "WASP-39 b: CO₂ Atmospheric Peak & Carbon-to-Oxygen Ratio Inversion",
                    description = "JWST NIRSpec PRISM transmisie spectroscopică la 4.3 µm confirmă o semnătură clară de dioxid de carbon (CO₂) cu adâncime de 1150 ppm, indicând o metalicitate atmosferică ridicată și îmbogățire cu elemente grele.",
                    discoveryType = DiscoveryType.NOVEL_CORRELATION,
                    noveltyScore = 0.94f,
                    confidenceScore = 0.98f,
                    impactScore = 0.93f,
                    commercialScore = 0.65f,
                    patentabilityScore = 0.82f,
                    estimatedValueUsd = 14500000L,
                    supportingEvidence = listOf(
                        EvidenceItem("JWST NIRSpec", "obs_jwst_01", "Absorbție la 4.3 µm cu 15σ peste continuu", 0.98f),
                        EvidenceItem("NASA Exoplanet Archive", "obs_nasa_01", "Parametri stelari WASP-39 (G8V)", 0.95f)
                    ),
                    domains = listOf("Astrophysics", "Atmospheric Spectroscopy", "Exoplanet Science"),
                    sourceIds = listOf("jwst", "nasa", "mast"),
                    hiddenPattern = HiddenPattern(
                        patternType = PatternType.STATISTICAL_OUTLIER,
                        description = "C/O ratio inversat față de modelul protoplanetar solar standard",
                        statisticalSignificance = 0.99f,
                        recurrenceRate = 0.72f,
                        firstDetected = System.currentTimeMillis() - 86400000L,
                        supportingDataPoints = 480
                    ),
                    recommendedActions = listOf(
                        "Planificare observații suplimentare cu JWST MIRI (5-12 µm) pentru benzi de SO₂",
                        "Rulare modelare atmosferică 1D cu Simulatorul Climatic Synthesis"
                    ),
                    risks = listOf("Contaminare fotometrică din pete stelare nerezolvate")
                ),
                Discovery(
                    id = "disc_saa_geomag",
                    investigationId = "benchmark_inv_02",
                    title = "Anomalia Atlanticului de Sud (SAA): Corelație Flux Protoni LEO cu Deplasarea Polilor Dipolari",
                    description = "Telemetria satelitară combinată (NOAA POES + Swarm) relevă o scădere de 24 nT/an a intensității câmpului geomagnetic în bazinul Americii de Sud, cu o creștere direct proporțională a radiațiilor captate de sateliți în orbita joasă.",
                    discoveryType = DiscoveryType.CAUSAL_LINK,
                    noveltyScore = 0.89f,
                    confidenceScore = 0.94f,
                    impactScore = 0.91f,
                    commercialScore = 0.88f,
                    patentabilityScore = 0.75f,
                    estimatedValueUsd = 28000000L,
                    supportingEvidence = listOf(
                        EvidenceItem("NOAA SWPC", "obs_noaa_02", "Detecție protoni >10 MeV în LEO", 0.92f),
                        EvidenceItem("ESA Swarm", "obs_swarm_01", "Măsurători magnetometru vector", 0.96f)
                    ),
                    domains = listOf("Geomagnetism", "Space Weather", "Earth Core Geodynamics"),
                    sourceIds = listOf("noaa", "usgs", "esa"),
                    hiddenPattern = HiddenPattern(
                        patternType = PatternType.CAUSAL_CHAIN,
                        description = "Slăbirea nucleului exterior terestru amplifică riscul SEU pe microelectronică orbitală",
                        statisticalSignificance = 0.96f,
                        recurrenceRate = 0.88f,
                        firstDetected = System.currentTimeMillis() - 172800000L,
                        supportingDataPoints = 1240
                    ),
                    recommendedActions = listOf(
                        "Reconfigurarea ferestrelor de memorie pentru sateliții comerciali la traversarea SAA",
                        "Integrare cu modulul Vreme Spațială din laborator"
                    ),
                    risks = listOf("Anomalii hardware necunoscute la solare CME majore")
                ),
                Discovery(
                    id = "disc_tic25155310_usp",
                    investigationId = "benchmark_inv_03",
                    title = "TIC 25155310: Candidat USP (Ultra-Short Period) cu Încălzire Mareică Intensă",
                    description = "Algoritmul autonom BLS și modelarea Mandel-Agol din modulul Xilon au extras un tranzit periodic de 0.84 zile cu adâncime de 1250 ppm în jurul unei stele de tip solar. Densitatea estimată sugerează o planetă de tip Super-Pământ bogată în fier.",
                    discoveryType = DiscoveryType.PREDICTIVE_PATTERN,
                    noveltyScore = 0.92f,
                    confidenceScore = 0.89f,
                    impactScore = 0.87f,
                    commercialScore = 0.70f,
                    patentabilityScore = 0.85f,
                    estimatedValueUsd = 9200000L,
                    supportingEvidence = listOf(
                        EvidenceItem("TESS QLP MAST", "obs_tess_03", "Curba fotometrică sector 14-26", 0.91f),
                        EvidenceItem("Gaia DR3", "obs_gaia_03", "Astrometrie și lipsă blend în 2 arcsec", 0.94f)
                    ),
                    domains = listOf("Exoplanets", "Stellar Photometry", "Tidal Geophysics"),
                    sourceIds = listOf("mast", "gaia", "tess"),
                    hiddenPattern = HiddenPattern(
                        patternType = PatternType.PREDICTIVE_SIGNAL,
                        description = "Fază orbitală pliată cu SNR de 14.2 peste zgomotul instrumental",
                        statisticalSignificance = 0.95f,
                        recurrenceRate = 0.95f,
                        firstDetected = System.currentTimeMillis() - 259200000L,
                        supportingDataPoints = 3200
                    ),
                    recommendedActions = listOf(
                        "Urmărire spectroscopică de viteză radială cu instrumentele HARPS / ESPRESSO",
                        "Export raport fotometric în format AAVSO / CAS"
                    ),
                    risks = listOf("Risc redus de binară eclipsantă la unghi grazing")
                )
            )
            repository.saveDiscoveries(benchmarkDiscoveries)
        }
    }

    data class SystemStatus(
        val dataSources: Int,
        val liveConnections: Int,
        val activeAnalyses: Int,
        val anomalies: Int
    )
}
