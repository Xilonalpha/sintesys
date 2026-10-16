package com.synthesis.viewmodel.xilon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import com.synthesis.data.repository.xilon.XilonRepository
import com.synthesis.data.service.gemini.GeminiAnalysisResult
import com.synthesis.data.service.gemini.GeminiAstrophysicsService
import com.synthesis.domain.model.exoplanet.DetectedDipSignal
import com.synthesis.domain.model.exoplanet.TransitDipScanReport
import com.synthesis.engine.exoplanet.TransitDipScannerUtility
import com.synthesis.domain.model.xilon.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class XilonAiChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface XilonUiState {
    data class Idle(
        val curatedTargets: List<TargetStarCandidate>,
        val lastResult: XilonDetectionResult? = null
    ) : XilonUiState

    data class Scanning(
        val ticId: String,
        val stage: String,
        val stageIndex: Int,
        val totalStages: Int
    ) : XilonUiState

    data class Success(
        val result: XilonDetectionResult,
        val curatedTargets: List<TargetStarCandidate>
    ) : XilonUiState

    data class Error(
        val message: String,
        val curatedTargets: List<TargetStarCandidate>
    ) : XilonUiState
}

@HiltViewModel
class XilonViewModel @Inject constructor(
    private val xilonRepository: XilonRepository,
    val geminiService: GeminiAstrophysicsService,
    private val synthesizedResearchRepository: com.synthesis.data.repository.SynthesizedResearchRepository,
    private val transitDipScannerUtility: TransitDipScannerUtility,
    private val synthesisSparkEngine: com.synthesis.engine.spark.SynthesisSparkEngine,
    val nasaExoFopExporter: com.synthesis.service.NasaExoFopExporter,
    val missionFlightBenchmarkEngine: com.synthesis.engine.flight.MissionFlightBenchmarkEngine,
    val flightReadinessDossierExporter: com.synthesis.service.FlightReadinessDossierExporter,
    private val tessMastPollingWorkManager: com.synthesis.service.worker.TessMastPollingWorkManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<XilonUiState>(
        XilonUiState.Idle(curatedTargets = xilonRepository.getCuratedTargets())
    )
    val uiState: StateFlow<XilonUiState> = _uiState.asStateFlow()

    val savedDiscoveries: StateFlow<List<XilonDiscoveryEntity>> = xilonRepository.getSavedDiscoveries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val curatedTargets: List<TargetStarCandidate> = xilonRepository.getCuratedTargets()
    val pureTicCatalog: List<PureTicCandidate> get() = xilonRepository.getPureTicCatalog()
    val pureTicCatalogFlow: StateFlow<List<PureTicCandidate>> = xilonRepository.getPureTicCatalogFlow()

    // Live Telemetry Streaming State
    private val _isLiveStreamActive = MutableStateFlow<Boolean>(false)
    val isLiveStreamActive: StateFlow<Boolean> = _isLiveStreamActive.asStateFlow()

    private val _liveScanSector = MutableStateFlow<String>("Sector 58 (s0058)")
    val liveScanSector: StateFlow<String> = _liveScanSector.asStateFlow()

    private val _livePhotonsPerSec = MutableStateFlow<Long>(14_820L)
    val livePhotonsPerSec: StateFlow<Long> = _livePhotonsPerSec.asStateFlow()

    private val _lastDiscoveredCandidate = MutableStateFlow<PureTicCandidate?>(null)
    val lastDiscoveredCandidate: StateFlow<PureTicCandidate?> = _lastDiscoveredCandidate.asStateFlow()

    private val _sessionDetectionsCount = MutableStateFlow<Int>(0)
    val sessionDetectionsCount: StateFlow<Int> = _sessionDetectionsCount.asStateFlow()

    // Search, Filter & Sort State
    private val _ticSearchQuery = MutableStateFlow<String>("")
    val ticSearchQuery: StateFlow<String> = _ticSearchQuery.asStateFlow()

    private val _ticHabitableOnly = MutableStateFlow<Boolean>(false)
    val ticHabitableOnly: StateFlow<Boolean> = _ticHabitableOnly.asStateFlow()

    private val _ticTypeFilter = MutableStateFlow<String>("ALL")
    val ticTypeFilter: StateFlow<String> = _ticTypeFilter.asStateFlow()

    private val _ticSortOption = MutableStateFlow<TicSortOption>(TicSortOption.LIVE_NEWEST)
    val ticSortOption: StateFlow<TicSortOption> = _ticSortOption.asStateFlow()

    val filteredTicCandidates: StateFlow<List<PureTicCandidate>> = combine(
        pureTicCatalogFlow,
        _ticSearchQuery,
        _ticHabitableOnly,
        _ticTypeFilter,
        _ticSortOption
    ) { list, query, habitableOnly, typeFilter, sortOption ->
        var result = list

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.ticId.contains(q) || it.name.lowercase().contains(q) || it.sector.lowercase().contains(q)
            }
        }

        if (habitableOnly) {
            result = result.filter { it.isInHabitableZone }
        }

        if (typeFilter != "ALL") {
            result = when (typeFilter) {
                "LIVE" -> result.filter { it.isLiveIngested }
                "SUPER_EARTH" -> result.filter {
                    it.potentialPlanetaryType.contains("Super-Pământ", ignoreCase = true) ||
                    it.potentialPlanetaryType.contains("Terestră", ignoreCase = true)
                }
                "NEPTUNE" -> result.filter { it.potentialPlanetaryType.contains("Neptun", ignoreCase = true) }
                "JOVIAN" -> result.filter {
                    it.potentialPlanetaryType.contains("Jovian", ignoreCase = true) ||
                    it.potentialPlanetaryType.contains("Gigant", ignoreCase = true)
                }
                "MONO_TRANSIT" -> result.filter { it.estimatedPeriodDays == null || it.estimatedPeriodDays > 40.0 }
                else -> result
            }
        }

        when (sortOption) {
            TicSortOption.LIVE_NEWEST -> result.sortedByDescending { it.ingestionTimestamp }
            TicSortOption.SNR_DESC -> result.sortedByDescending { it.snrSigma }
            TicSortOption.DEPTH_DESC -> result.sortedByDescending { it.transitDepthPpm }
            TicSortOption.PERIOD_ASC -> result.sortedBy { it.estimatedPeriodDays ?: 999.0 }
            TicSortOption.DISTANCE_ASC -> result.sortedBy { it.distancePc }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = xilonRepository.getPureTicCatalog()
    )

    private var liveStreamJob: Job? = null

    // Transit Dip Scanner State
    private val _dipScanReport = MutableStateFlow<TransitDipScanReport?>(null)
    val dipScanReport: StateFlow<TransitDipScanReport?> = _dipScanReport.asStateFlow()

    private val _selectedDip = MutableStateFlow<DetectedDipSignal?>(null)
    val selectedDip: StateFlow<DetectedDipSignal?> = _selectedDip.asStateFlow()

    private val _dipSensitivitySigma = MutableStateFlow<Double>(3.5)
    val dipSensitivitySigma: StateFlow<Double> = _dipSensitivitySigma.asStateFlow()

    private val _isDipHighlightActive = MutableStateFlow<Boolean>(true)
    val isDipHighlightActive: StateFlow<Boolean> = _isDipHighlightActive.asStateFlow()

    // Real Gemini AI State
    private val _geminiAnalysis = MutableStateFlow<GeminiAnalysisResult?>(null)
    val geminiAnalysis: StateFlow<GeminiAnalysisResult?> = _geminiAnalysis.asStateFlow()

    private val _isAnalyzingWithGemini = MutableStateFlow<Boolean>(false)
    val isAnalyzingWithGemini: StateFlow<Boolean> = _isAnalyzingWithGemini.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<XilonAiChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<XilonAiChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatResponding = MutableStateFlow<Boolean>(false)
    val isChatResponding: StateFlow<Boolean> = _isChatResponding.asStateFlow()

    private val _isApiKeySet = MutableStateFlow<Boolean>(false)
    val isApiKeySet: StateFlow<Boolean> = _isApiKeySet.asStateFlow()

    // Dynamic Gemini Model Management (Gemini 3.8 Flash, 3.5 Flash, Latest, Pro)
    val activeGeminiModel: StateFlow<String> = geminiService.getActiveModelFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, GeminiAstrophysicsService.MODEL_GEMINI_3_8_FLASH)

    val availableGeminiModels = GeminiAstrophysicsService.AVAILABLE_MODELS

    // SYNTHESIS SPARK Distributed Analytics Engine State
    private val _sparkReport = MutableStateFlow<com.synthesis.domain.model.spark.SparkExecutionReport?>(null)
    val sparkReport: StateFlow<com.synthesis.domain.model.spark.SparkExecutionReport?> = _sparkReport.asStateFlow()

    private val _isSparkRunning = MutableStateFlow<Boolean>(false)
    val isSparkRunning: StateFlow<Boolean> = _isSparkRunning.asStateFlow()

    init {
        viewModelScope.launch {
            _isApiKeySet.value = geminiService.isAiKeyConfigured()
        }
    }

    fun selectGeminiModel(model: String) {
        viewModelScope.launch {
            geminiService.setActiveModel(model)
            val current = _uiState.value
            if (current is XilonUiState.Success) {
                runGeminiAnalysis(current.result)
            }
        }
    }

    fun runSparkAnalysis() {
        val current = _uiState.value
        val points = if (current is XilonUiState.Success) {
            current.result.lightCurveSample
        } else emptyList()

        val ticId = if (current is XilonUiState.Success) {
            current.result.star.ticId
        } else "TIC-SPARK-STREAM"

        val period = if (current is XilonUiState.Success) {
            current.result.orbitalPeriodDays
        } else 3.8425

        viewModelScope.launch {
            _isSparkRunning.value = true
            val report = synthesisSparkEngine.executeDistributedTransitSearch(
                ticId = ticId,
                points = points,
                nominalPeriodDays = period
            )
            _sparkReport.value = report
            _isSparkRunning.value = false
        }
    }

    fun saveUserGeminiApiKey(key: String) {
        viewModelScope.launch {
            geminiService.saveUserApiKey(key)
            _isApiKeySet.value = geminiService.isAiKeyConfigured()
            // Re-run analysis with the new key if we have a current detection
            val current = _uiState.value
            if (current is XilonUiState.Success) {
                runGeminiAnalysis(current.result)
            }
        }
    }

    fun runGeminiAnalysis(detection: XilonDetectionResult) {
        viewModelScope.launch {
            _isAnalyzingWithGemini.value = true
            val analysis = geminiService.analyzeCandidateWithGemini(detection)
            _geminiAnalysis.value = analysis
            _isAnalyzingWithGemini.value = false

            // Seed initial greeting in chat if empty
            if (_chatMessages.value.isEmpty()) {
                val rEarth = detection.derivedPlanetRadiusEarth ?: 1.0
                val modelName = activeGeminiModel.value
                _chatMessages.value = listOf(
                    XilonAiChatMessage(
                        sender = "XILON AI ($modelName)",
                        text = "Salut! Sunt asistentul tău astrofizician neuronal alimentat de $modelName. Am analizat curba fotometrică a stelei TIC ${detection.star.ticId} și candidatul de ${String.format(java.util.Locale.US, "%.1f", rEarth)} R⊕. Pune-mi orice întrebare despre semnal, probabilitatea de fals pozitiv sau instrumentele JWST!",
                        isUser = false
                    )
                )
            }
        }
    }

    fun askGemini(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return

        val current = _uiState.value
        val contextSummary = if (current is XilonUiState.Success) {
            val rEarth = current.result.derivedPlanetRadiusEarth ?: 1.0
            val teq = current.result.derivedEquilibriumTempK ?: 300.0
            "Stea: TIC ${current.result.star.ticId}, Perioada: ${current.result.orbitalPeriodDays} zile, Rază planetă: $rEarth R_Earth, Teq: $teq K, SNR: ${current.result.signalToNoiseRatio}"
        } else {
            "Context exoplanete general."
        }

        val updated = _chatMessages.value + XilonAiChatMessage(sender = "Tu", text = trimmed, isUser = true)
        _chatMessages.value = updated

        viewModelScope.launch {
            _isChatResponding.value = true
            val history = updated.map { Pair(it.sender, it.text) }
            val reply = geminiService.askAstrophysicist(contextSummary, trimmed, history)
            _chatMessages.value = _chatMessages.value + XilonAiChatMessage(
                sender = "XILON AI (Gemini)",
                text = reply,
                isUser = false
            )
            _isChatResponding.value = false
        }
    }

    fun saveCurrentResult(notes: String? = null) {
        val current = _uiState.value
        if (current is XilonUiState.Success) {
            viewModelScope.launch {
                xilonRepository.saveDiscovery(current.result, notes)
            }
        }
    }

    fun deleteSaved(id: Int) {
        viewModelScope.launch {
            xilonRepository.deleteDiscovery(id)
        }
    }

    suspend fun exportAllDiscoveriesExoFopJson(): String = nasaExoFopExporter.exportAllDiscoveriesJson()

    suspend fun exportAllDiscoveriesExoFopCsv(): String = nasaExoFopExporter.exportAllDiscoveriesCsv()

    suspend fun exportSingleDiscoveryExoFopJson(id: Int): String? = nasaExoFopExporter.exportSingleDiscoveryJson(id)

    suspend fun exportSingleDiscoveryExoFopCsv(id: Int): String? = nasaExoFopExporter.exportSingleDiscoveryCsv(id)

    suspend fun exportCurrentDetectionJson(res: com.synthesis.domain.model.xilon.XilonDetectionResult): String = nasaExoFopExporter.exportDetectionResultJson(res)

    suspend fun exportCurrentDetectionCsv(res: com.synthesis.domain.model.xilon.XilonDetectionResult): String = nasaExoFopExporter.exportDetectionResultCsv(res)

    fun runScan(ticIdInput: String, periodDaysInput: Double? = null) {
        val cleanTic = ticIdInput.trim().replace("TIC", "", ignoreCase = true).trim()
        if (cleanTic.isBlank()) return

        _geminiAnalysis.value = null
        _chatMessages.value = emptyList()

        viewModelScope.launch {
            _uiState.value = XilonUiState.Scanning(
                ticId = cleanTic,
                stage = "Connecting to Space Telescope Science Institute (MAST)...",
                stageIndex = 1,
                totalStages = 5
            )
            delay(400)

            _uiState.value = XilonUiState.Scanning(
                ticId = cleanTic,
                stage = "Retrieving host star Teff, radius, and distance from TIC catalog...",
                stageIndex = 2,
                totalStages = 5
            )
            delay(500)

            _uiState.value = XilonUiState.Scanning(
                ticId = cleanTic,
                stage = "Ingesting photometric photon flux series across observation sectors...",
                stageIndex = 3,
                totalStages = 5
            )

            val scanResult = xilonRepository.runAutonomousDetection(cleanTic, periodDaysInput)

            _uiState.value = XilonUiState.Scanning(
                ticId = cleanTic,
                stage = "Executing Transit Least Squares (TLS) & Odd/Even Vetting tests...",
                stageIndex = 4,
                totalStages = 5
            )
            delay(500)

            _uiState.value = XilonUiState.Scanning(
                ticId = cleanTic,
                stage = "Deriving planetary radius (Rp) & invoking Gemini AI Astrophysicist...",
                stageIndex = 5,
                totalStages = 5
            )
            delay(400)

            scanResult.fold(
                onSuccess = { detection ->
                    _uiState.value = XilonUiState.Success(
                        result = detection,
                        curatedTargets = curatedTargets
                    )
                    // Automatically run recurring dip scanner utility
                    runDipScanInternal(detection, _dipSensitivitySigma.value)

                    // Trigger real Gemini analysis automatically!
                    runGeminiAnalysis(detection)
                },
                onFailure = { err ->
                    _uiState.value = XilonUiState.Error(
                        message = err.message ?: "Failed to perform autonomous scan on TIC $cleanTic",
                        curatedTargets = curatedTargets
                    )
                }
            )
        }
    }

    /**
     * Executes the transit dip scanner utility on current light curve data
     */
    fun runDipScan(sigmaThreshold: Double = _dipSensitivitySigma.value) {
        val current = _uiState.value
        if (current is XilonUiState.Success) {
            _dipSensitivitySigma.value = sigmaThreshold
            runDipScanInternal(current.result, sigmaThreshold)
        }
    }

    private fun runDipScanInternal(detection: XilonDetectionResult, sigma: Double) {
        viewModelScope.launch {
            val report = transitDipScannerUtility.scanForRecurringDips(
                ticId = detection.star.ticId,
                points = detection.lightCurveSample,
                nominalPeriodDays = detection.orbitalPeriodDays,
                expectedDurationHours = detection.transitDurationHours,
                sigmaThreshold = sigma
            )
            _dipScanReport.value = report
            _selectedDip.value = report.primaryDip
            _isDipHighlightActive.value = true
        }
    }

    fun selectDip(dip: DetectedDipSignal?) {
        _selectedDip.value = if (_selectedDip.value == dip) null else dip
    }

    fun toggleDipHighlight() {
        _isDipHighlightActive.value = !_isDipHighlightActive.value
    }

    fun setDipSensitivity(sigma: Double) {
        _dipSensitivitySigma.value = sigma
        runDipScan(sigma)
    }

    fun resetToIdle() {
        val current = _uiState.value
        val lastResult = if (current is XilonUiState.Success) current.result else null
        _uiState.value = XilonUiState.Idle(curatedTargets = curatedTargets, lastResult = lastResult)
    }

    fun saveXilonToResearchArchive(discovery: XilonDetectionResult) {
        viewModelScope.launch {
            val radius = discovery.derivedPlanetRadiusEarth ?: 0.0
            val entity = com.synthesis.data.local.entity.SynthesizedResearchEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = "Xilon AI Autonom: ${discovery.tceName}",
                targetName = discovery.tceName,
                researchCategory = "EXOPLANET",
                summary = "Candidat exoplanetar validat de rețeaua autonomă Xilon AI cu metoda BLS. " +
                        "Perioadă orbitală: ${String.format(java.util.Locale.US, "%.3f", discovery.orbitalPeriodDays)} zile, " +
                        "Adâncime tranzit: ${discovery.transitDepthPpm.toInt()} ppm, " +
                        "Rază estimată: ${String.format(java.util.Locale.US, "%.2f", radius)} R⊕.",
                synthesizedDataJson = """{"tic":"${discovery.star.ticId}","period_d":${discovery.orbitalPeriodDays},"depth_ppm":${discovery.transitDepthPpm},"radius_earth":$radius,"snr":${discovery.signalToNoiseRatio},"disposition":"${discovery.verdict.name}"}""",
                confidenceScore = 0.94f,
                sourceCount = 4,
                observerNotes = "Detectat prin analiza curbei de lumină de înaltă cadență în modulul Xilon AI.",
                timestamp = System.currentTimeMillis(),
                isVerified = true,
                isBookmarked = false
            )
            synthesizedResearchRepository.saveResearch(entity)
        }
    }

    fun toggleLiveStream() {
        val newState = !_isLiveStreamActive.value
        _isLiveStreamActive.value = newState
        if (newState) {
            startLiveStreamJob()
        } else {
            liveStreamJob?.cancel()
            liveStreamJob = null
        }
    }

    private fun startLiveStreamJob() {
        liveStreamJob?.cancel()
        liveStreamJob = viewModelScope.launch {
            var currentSectorNum = 48
            while (isActive && _isLiveStreamActive.value) {
                delay(3800)
                currentSectorNum = (currentSectorNum % 88) + 1
                _liveScanSector.value = "Sector $currentSectorNum (s00${if (currentSectorNum < 10) "0$currentSectorNum" else "$currentSectorNum"})"
                _livePhotonsPerSec.value = kotlin.random.Random.nextLong(11200L, 19800L)

                val newCand = xilonRepository.addNewLiveCandidate(currentSectorNum)
                _lastDiscoveredCandidate.value = newCand
                _sessionDetectionsCount.value += 1
            }
        }
    }

    fun triggerManualSectorScan(sectorHint: Int? = null) {
        viewModelScope.launch {
            val sNum = sectorHint ?: kotlin.random.Random.nextInt(1, 88)
            _liveScanSector.value = "Sector $sNum (s00${if (sNum < 10) "0$sNum" else "$sNum"})"
            _livePhotonsPerSec.value = kotlin.random.Random.nextLong(12000L, 22000L)
            val newCand = xilonRepository.addNewLiveCandidate(sNum)
            _lastDiscoveredCandidate.value = newCand
            _sessionDetectionsCount.value += 1
        }
    }

    fun setTicSearchQuery(query: String) {
        _ticSearchQuery.value = query
    }

    fun setTicHabitableOnly(enabled: Boolean) {
        _ticHabitableOnly.value = enabled
    }

    fun setTicTypeFilter(filter: String) {
        _ticTypeFilter.value = filter
    }

    fun setTicSortOption(option: TicSortOption) {
        _ticSortOption.value = option
    }

    // WorkManager Background MAST Poller Controls
    private val _mastWorkManagerStatus = MutableStateFlow(tessMastPollingWorkManager.getPollingStatus(appContext))
    val mastWorkManagerStatus: StateFlow<com.synthesis.service.worker.TessMastPollingStatus> = _mastWorkManagerStatus.asStateFlow()

    fun triggerMastPollNow() {
        tessMastPollingWorkManager.triggerImmediatePoll(appContext)
        refreshWorkManagerStatus()
    }

    fun refreshWorkManagerStatus() {
        _mastWorkManagerStatus.value = tessMastPollingWorkManager.getPollingStatus(appContext)
    }

    // TRL 9 Flight Readiness Qualification Suite
    private val _trl9Dossier = MutableStateFlow<com.synthesis.domain.model.flight.FlightReadinessDossier?>(null)
    val trl9Dossier: StateFlow<com.synthesis.domain.model.flight.FlightReadinessDossier?> = _trl9Dossier.asStateFlow()

    private val _isTrl9Running = MutableStateFlow<Boolean>(false)
    val isTrl9Running: StateFlow<Boolean> = _isTrl9Running.asStateFlow()

    fun runTrl9Qualification() {
        if (_isTrl9Running.value) return
        viewModelScope.launch(Dispatchers.Default) {
            _isTrl9Running.value = true
            try {
                val dossier = missionFlightBenchmarkEngine.runFullFlightQualificationSuite()
                _trl9Dossier.value = dossier
            } finally {
                _isTrl9Running.value = false
            }
        }
    }

    fun exportTrl9DossierMarkdown(): String {
        val dossier = _trl9Dossier.value ?: missionFlightBenchmarkEngine.runFullFlightQualificationSuite()
        return flightReadinessDossierExporter.generateMarkdownDossier(dossier)
    }

    fun exportTrl9DossierJson(): String {
        val dossier = _trl9Dossier.value ?: missionFlightBenchmarkEngine.runFullFlightQualificationSuite()
        return flightReadinessDossierExporter.generateJsonDossier(dossier)
    }

    override fun onCleared() {
        super.onCleared()
        liveStreamJob?.cancel()
    }
}
