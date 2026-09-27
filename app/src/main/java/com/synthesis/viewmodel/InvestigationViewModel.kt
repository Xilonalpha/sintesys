package com.synthesis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SourceOrchestrator
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.*
import com.synthesis.engine.anomaly.AnomalyDetector
import com.synthesis.engine.correlation.CorrelationEngine
import com.synthesis.engine.hypothesis.HypothesisEngine
import com.synthesis.engine.hypothesis.ImpactIpEngine
import com.synthesis.engine.discovery.DiscoveryEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InvestigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SynthesisRepository,
    private val sourceOrchestrator: SourceOrchestrator,
    private val anomalyDetector: AnomalyDetector,
    private val correlationEngine: CorrelationEngine,
    private val hypothesisEngine: HypothesisEngine,
    private val impactIpEngine: ImpactIpEngine,
    private val discoveryEngine: DiscoveryEngine
) : ViewModel() {

    private val investigationId: String = checkNotNull(savedStateHandle["investigationId"])

    val investigation: StateFlow<Investigation?> = flow {
        emit(repository.getInvestigationById(investigationId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val observations: StateFlow<List<Observation>> = repository.getObservations(investigationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val anomalies: StateFlow<List<Anomaly>> = repository.getAnomalies(investigationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _correlations = MutableStateFlow<List<Correlation>>(emptyList())
    val correlations: StateFlow<List<Correlation>> = _correlations.asStateFlow()

    val hypotheses: StateFlow<List<Hypothesis>> = repository.getHypotheses(investigationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val evidenceNodes: StateFlow<List<EvidenceNode>> = repository.getEvidenceNodes(investigationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dataSources: StateFlow<List<DataSource>> = sourceOrchestrator.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analysisState = MutableStateFlow(AnalysisState.IDLE)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _impactAssessment = MutableStateFlow<ImpactIpEngine.ImpactAssessment?>(null)
    val impactAssessment: StateFlow<ImpactIpEngine.ImpactAssessment?> = _impactAssessment.asStateFlow()

    private val _discoveries = MutableStateFlow<List<Discovery>>(emptyList())
    val discoveries: StateFlow<List<Discovery>> = _discoveries.asStateFlow()

    init {
        viewModelScope.launch { sourceOrchestrator.runHealthChecks() }
    }

    fun startAnalysis() {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.HEALTH_CHECK
            _progress.value = 0.03f
            _logs.value = listOf("Running source health checks...")

            sourceOrchestrator.runHealthChecks()
            val onlineSources = sourceOrchestrator.getOnlineSources()
            _logs.value = _logs.value + "Online sources: ${onlineSources.size}/15 — ${onlineSources.map { it.name }.joinToString()}"

            _analysisState.value = AnalysisState.FETCHING
            _progress.value = 0.06f
            repository.updateInvestigationStatus(investigationId, InvestigationStatus.ACTIVE)

            val allObservations = mutableListOf<Observation>()
            val query = investigation.value?.query ?: ""
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val endDate = sdf.format(Date())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val startDate = sdf.format(cal.time)

            // 1. NASA NEO
            if (onlineSources.any { it.id == "nasa" }) {
                _logs.value = _logs.value + "Fetching NASA NEO asteroid data..."
                repository.fetchNasaNeoData(startDate = startDate, endDate = endDate).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "NASA NEO: ${it.size} asteroids"
                }
            }
            _progress.value = 0.10f

            // 2. USGS
            if (onlineSources.any { it.id == "usgs" }) {
                _logs.value = _logs.value + "Fetching USGS earthquakes..."
                repository.fetchUsgsData(minMagnitude = 4.0).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "USGS: ${it.size} earthquakes"
                }
            }
            _progress.value = 0.16f

            // 3. Open-Meteo
            if (onlineSources.any { it.id == "open_meteo" }) {
                _logs.value = _logs.value + "Fetching Open-Meteo weather..."
                val lat = allObservations.firstOrNull { it.location != null }?.location?.latitude ?: 39.7456
                val lon = allObservations.firstOrNull { it.location != null }?.location?.longitude ?: -97.0892
                repository.fetchOpenMeteoData(latitude = lat, longitude = lon).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "Open-Meteo: ${it.size} records"
                }
            }
            _progress.value = 0.22f

            // 4. NASA POWER
            if (onlineSources.any { it.id == "nasa_power" }) {
                _logs.value = _logs.value + "Fetching NASA POWER climate..."
                val lat = allObservations.firstOrNull { it.location != null }?.location?.latitude ?: 0.0
                val lon = allObservations.firstOrNull { it.location != null }?.location?.longitude ?: 0.0
                repository.fetchNasaPowerData(latitude = lat, longitude = lon).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "NASA POWER: ${it.size} climate records"
                }
            }
            _progress.value = 0.28f

            // 5. PubMed
            if (onlineSources.any { it.id == "pubmed" }) {
                _logs.value = _logs.value + "Fetching PubMed papers..."
                repository.fetchPubMedPapers(query = query).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "PubMed: ${it.size} papers"
                }
            }
            _progress.value = 0.34f

            // 6. arXiv
            if (onlineSources.any { it.id == "arxiv" }) {
                _logs.value = _logs.value + "Fetching arXiv papers..."
                repository.fetchArxivPapers(query = query).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "arXiv: ${it.size} papers"
                }
            }
            _progress.value = 0.40f

            // 7. ESA Gaia
            if (onlineSources.any { it.id == "gaia" }) {
                _logs.value = _logs.value + "Fetching ESA Gaia DR3..."
                val lat = allObservations.firstOrNull { it.location != null }?.location?.latitude ?: 0.0
                val lon = allObservations.firstOrNull { it.location != null }?.location?.longitude ?: 0.0
                repository.fetchGaiaData(latitude = lat, longitude = lon).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "ESA Gaia: ${it.size} stellar sources"
                }
            }
            _progress.value = 0.46f

            // 8. CERN
            if (onlineSources.any { it.id == "cern" }) {
                _logs.value = _logs.value + "Fetching CERN Open Data..."
                repository.fetchCernData(query = query).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "CERN: ${it.size} records"
                }
            }
            _progress.value = 0.52f

            // 9. Copernicus
            if (onlineSources.any { it.id == "copernicus" }) {
                _logs.value = _logs.value + "Fetching Copernicus satellite data..."
                val lat = allObservations.firstOrNull { it.location != null }?.location?.latitude ?: 0.0
                val lon = allObservations.firstOrNull { it.location != null }?.location?.longitude ?: 0.0
                repository.fetchCopernicusData(latitude = lat, longitude = lon).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "Copernicus: ${it.size} products"
                }
            }
            _progress.value = 0.58f

            // 10. GBIF
            if (onlineSources.any { it.id == "gbif" }) {
                _logs.value = _logs.value + "Fetching GBIF biodiversity..."
                repository.fetchGbifData(query = query).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "GBIF: ${it.size} occurrences"
                }
            }
            _progress.value = 0.64f

            // 11. Sentinel Hub
            if (onlineSources.any { it.id == "sentinel" }) {
                _logs.value = _logs.value + "Fetching Sentinel Hub imagery..."
                val lat = allObservations.firstOrNull { it.location != null }?.location?.latitude ?: 0.0
                val lon = allObservations.firstOrNull { it.location != null }?.location?.longitude ?: 0.0
                repository.fetchSentinelData(latitude = lat, longitude = lon).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "Sentinel Hub: ${it.size} images"
                }
            }
            _progress.value = 0.70f

            // 12. NCBI Virus
            if (onlineSources.any { it.id == "ncbi_virus" }) {
                _logs.value = _logs.value + "Fetching NCBI Virus genomes..."
                repository.fetchNcbiVirusData(query = query).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "NCBI Virus: ${it.size} genomes"
                }
            }
            _progress.value = 0.76f

            // 13. World Bank
            if (onlineSources.any { it.id == "worldbank" }) {
                _logs.value = _logs.value + "Fetching World Bank indicators..."
                repository.fetchWorldBankData(country = "RO", indicator = "SP.POP.TOTL").getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "World Bank: ${it.size} indicators"
                }
            }
            _progress.value = 0.82f

            // 14. JPL Horizons
            if (onlineSources.any { it.id == "jpl" }) {
                _logs.value = _logs.value + "Fetching JPL Horizons ephemeris..."
                repository.fetchJplEphemeris(
                    target = "499", center = "@399",
                    startTime = startDate, endTime = endDate
                ).getOrNull()?.let {
                    allObservations.addAll(it.map { o -> o.copy(investigationId = investigationId) })
                    _logs.value = _logs.value + "JPL: ${it.size} ephemeris points"
                }
            }
            _progress.value = 0.86f

            // Save observations in batch
            repository.saveObservations(allObservations)
            _logs.value = _logs.value + "Total observations: ${allObservations.size}"

            // Anomaly Detection
            _analysisState.value = AnalysisState.ANALYZING
            _progress.value = 0.89f
            val detectedAnomalies = anomalyDetector.detectAnomalies(allObservations)
            detectedAnomalies.forEach { repository.saveAnomaly(it) }
            _logs.value = _logs.value + "Anomalies: ${detectedAnomalies.size}"

            // Correlation
            _analysisState.value = AnalysisState.CORRELATING
            _progress.value = 0.92f
            val detectedCorrelations = correlationEngine.findCorrelations(allObservations)
            _correlations.value = detectedCorrelations
            _logs.value = _logs.value + "Correlations: ${detectedCorrelations.size}"

            // Hypotheses
            _progress.value = 0.95f
            val generatedHypotheses = hypothesisEngine.generateHypotheses(allObservations, detectedAnomalies, detectedCorrelations)
            generatedHypotheses.forEach { repository.saveHypothesis(it) }
            _logs.value = _logs.value + "Hypotheses: ${generatedHypotheses.size}"

            // Impact IP Assessment
            _progress.value = 0.97f
            _logs.value = _logs.value + "Running Impact IP assessment..."
            val impact = impactIpEngine.assessDiscovery(
                investigationId = investigationId,
                observations = allObservations,
                anomalies = detectedAnomalies,
                correlations = detectedCorrelations,
                hypotheses = generatedHypotheses
            )
            _impactAssessment.value = impact
            _logs.value = _logs.value + "Impact IP Score: ${(impact.overallScore * 100).toInt()}%"

            // Discovery Engine
            _logs.value = _logs.value + "Scanning for hidden cross-domain patterns..."
            val generatedDiscoveries = discoveryEngine.generateDiscoveries(investigationId, allObservations)
            _discoveries.value = generatedDiscoveries
            _logs.value = _logs.value + "Discoveries: ${generatedDiscoveries.size}" +
                (generatedDiscoveries.firstOrNull()?.let { " (top estimated value: $${it.estimatedValueUsd})" } ?: "")

            // Evidence Graph
            generateEvidenceNodes(allObservations, detectedCorrelations, generatedHypotheses)

            // Update stats
            repository.updateInvestigationStats(
                id = investigationId,
                sources = onlineSources.size,
                anomalies = detectedAnomalies.size,
                confidence = (generatedHypotheses.firstOrNull()?.confidence ?: 0f) / 100f
            )

            _progress.value = 1.0f
            _analysisState.value = AnalysisState.COMPLETED
            repository.updateInvestigationStatus(investigationId, InvestigationStatus.COMPLETED)
            _logs.value = _logs.value + "Analysis completed."
        }
    }

    private suspend fun generateEvidenceNodes(observations: List<Observation>, correlations: List<Correlation>, hypotheses: List<Hypothesis>) {
        val nodes = mutableListOf<EvidenceNode>()
        observations.forEachIndexed { index, obs ->
            nodes.add(
                EvidenceNode(
                    id = "node_obs_$index",
                    investigationId = investigationId,
                    label = obs.entity.take(20),
                    type = when (obs.observationType) {
                        ObservationType.ASTRONOMICAL -> NodeType.OBSERVATION
                        ObservationType.SATELLITE -> NodeType.SATELLITE
                        ObservationType.ATMOSPHERIC -> NodeType.DATABASE
                        ObservationType.GEOLOGICAL -> NodeType.OBSERVATION
                        ObservationType.BIOLOGICAL -> NodeType.OBSERVATION
                        ObservationType.HISTORICAL -> NodeType.PAPER
                    },
                    source = obs.sourceName,
                    relationship = RelationshipType.CONFIRMED,
                    description = obs.normalizedData,
                    timestamp = obs.timestamp,
                    x = (100 + index * 80).toFloat(),
                    y = (200 + (index % 3) * 120).toFloat()
                )
            )
        }
        correlations.forEachIndexed { index, corr ->
            nodes.add(
                EvidenceNode(
                    id = "node_corr_$index",
                    investigationId = investigationId,
                    label = "CORR ${(corr.strength * 100).toInt()}%",
                    type = NodeType.DATABASE,
                    source = "${corr.sourceA} ↔ ${corr.sourceB}",
                    relationship = if (corr.strength > 0.7f) RelationshipType.CONFIRMED else RelationshipType.WEAK,
                    description = corr.description,
                    timestamp = System.currentTimeMillis(),
                    x = (300 + index * 100).toFloat(),
                    y = (400 + (index % 2) * 100).toFloat()
                )
            )
        }
        hypotheses.forEachIndexed { index, hyp ->
            nodes.add(
                EvidenceNode(
                    id = "node_hyp_$index",
                    investigationId = investigationId,
                    label = hyp.title.take(15),
                    type = NodeType.HYPOTHESIS,
                    source = "HYPOTHESIS ENGINE",
                    relationship = when {
                        hyp.confidence > 70f -> RelationshipType.CONFIRMED
                        hyp.confidence > 40f -> RelationshipType.WEAK
                        else -> RelationshipType.UNVERIFIED
                    },
                    description = hyp.description,
                    timestamp = System.currentTimeMillis(),
                    x = (500 + index * 90).toFloat(),
                    y = (150 + (index % 3) * 130).toFloat()
                )
            )
        }
        nodes.forEach { repository.saveEvidenceNode(it) }
    }

    enum class AnalysisState { IDLE, HEALTH_CHECK, FETCHING, ANALYZING, CORRELATING, COMPLETED, FAILED }
}
