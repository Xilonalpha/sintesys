package com.synthesis.viewmodel.xilon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import com.synthesis.data.repository.xilon.XilonRepository
import com.synthesis.data.service.gemini.GeminiAnalysisResult
import com.synthesis.data.service.gemini.GeminiAstrophysicsService
import com.synthesis.domain.model.xilon.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val geminiService: GeminiAstrophysicsService
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

    private val curatedTargets = xilonRepository.getCuratedTargets()

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

    init {
        viewModelScope.launch {
            _isApiKeySet.value = geminiService.isAiKeyConfigured()
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
                _chatMessages.value = listOf(
                    XilonAiChatMessage(
                        sender = "XILON AI (Gemini)",
                        text = "Salut! Sunt asistentul tău astrofizician neuronal alimentat de Gemini 3.5 Flash. Am analizat curba fotometrică a stelei TIC ${detection.star.ticId} și candidatul de ${String.format(java.util.Locale.US, "%.1f", rEarth)} R⊕. Pune-mi orice întrebare despre semnal, probabilitatea de fals pozitiv sau instrumentele JWST!",
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

    fun resetToIdle() {
        val current = _uiState.value
        val lastResult = if (current is XilonUiState.Success) current.result else null
        _uiState.value = XilonUiState.Idle(curatedTargets = curatedTargets, lastResult = lastResult)
    }
}
