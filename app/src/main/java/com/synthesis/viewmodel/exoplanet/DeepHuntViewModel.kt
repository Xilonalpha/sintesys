package com.synthesis.viewmodel.exoplanet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.exoplanet.UnvettedHuntRepository
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.service.worker.TessSurveyWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeepHuntTab(val title: String) {
    MONO_TRANSITS("Mono-Transits (27d Blindspot)"),
    TESS_SURVEY_SCOUT("TESS Survey Scout (WorkManager)"),
    TTV_INVERSION("TTV Gravitational Inversion"),
    ATMOSPHERIC_TSM("JWST Atmospheric TSM"),
    NEGLECTED_JEWELS("NASA Backlog Purgatory")
}

@HiltViewModel
class DeepHuntViewModel @Inject constructor(
    private val repository: UnvettedHuntRepository,
    private val synthesizedResearchRepository: com.synthesis.data.repository.SynthesizedResearchRepository,
    private val tessSurveyWorkManager: TessSurveyWorkManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DeepHuntTab.MONO_TRANSITS)
    val selectedTab: StateFlow<DeepHuntTab> = _selectedTab.asStateFlow()

    private val _monoTransits = MutableStateFlow<List<MonoTransitCandidate>>(emptyList())
    val monoTransits: StateFlow<List<MonoTransitCandidate>> = _monoTransits.asStateFlow()

    private val _ttvSystems = MutableStateFlow<List<TtvCompanionAnalysis>>(emptyList())
    val ttvSystems: StateFlow<List<TtvCompanionAnalysis>> = _ttvSystems.asStateFlow()

    private val _atmosphericScores = MutableStateFlow<List<AtmosphericCharacterizationScore>>(emptyList())
    val atmosphericScores: StateFlow<List<AtmosphericCharacterizationScore>> = _atmosphericScores.asStateFlow()

    private val _neglectedJewels = MutableStateFlow<List<NeglectedJewelTarget>>(emptyList())
    val neglectedJewels: StateFlow<List<NeglectedJewelTarget>> = _neglectedJewels.asStateFlow()

    // WorkManager TESS Survey Scout State
    private val _scoutStatus = MutableStateFlow<TessSurveyScoutStatus?>(null)
    val scoutStatus: StateFlow<TessSurveyScoutStatus?> = _scoutStatus.asStateFlow()

    private val _surveyCriteria = MutableStateFlow<TessSurveyCriteria>(TessSurveyCriteria())
    val surveyCriteria: StateFlow<TessSurveyCriteria> = _surveyCriteria.asStateFlow()

    // Interactive custom single-transit inversion solver state
    private val _customCandidate = MutableStateFlow<MonoTransitCandidate?>(null)
    val customCandidate: StateFlow<MonoTransitCandidate?> = _customCandidate.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: DeepHuntTab) {
        _selectedTab.value = tab
    }

    fun loadData() {
        _monoTransits.value = repository.getCuratedMonoTransits()
        _ttvSystems.value = repository.getCuratedTtvSystems()
        _atmosphericScores.value = repository.getTopAtmosphericTsmScores()
        _neglectedJewels.value = repository.getNeglectedJewels()
    }

    fun loadScoutStatus(context: Context) {
        _scoutStatus.value = tessSurveyWorkManager.getScoutStatus(context)
        _surveyCriteria.value = tessSurveyWorkManager.loadCriteria(context)
    }

    fun triggerImmediateSurvey(context: Context) {
        tessSurveyWorkManager.triggerImmediateSurvey(context, _surveyCriteria.value)
        loadScoutStatus(context)
    }

    fun updateSurveyCriteria(context: Context, criteria: TessSurveyCriteria) {
        _surveyCriteria.value = criteria
        tessSurveyWorkManager.saveCriteria(context, criteria)
        loadScoutStatus(context)
    }

    fun togglePeriodicScout(context: Context, enabled: Boolean) {
        if (enabled) {
            tessSurveyWorkManager.schedulePeriodicSurvey(context, repeatHours = 6, criteria = _surveyCriteria.value)
        } else {
            tessSurveyWorkManager.cancelPeriodicSurvey(context)
        }
        loadScoutStatus(context)
    }

    fun solveCustomSingleTransit(
        ticId: String,
        candidateName: String,
        sector: String,
        epochBtjd: Double,
        durationHours: Double,
        depthPpm: Double,
        teffK: Double,
        radSun: Double,
        massSun: Double,
        distPc: Double?,
        vMag: Double,
        impactB: Double = 0.0
    ) {
        viewModelScope.launch {
            val result = repository.analyzeCustomSingleTransit(
                ticId = ticId,
                candidateName = candidateName,
                sector = sector,
                epochBtjd = epochBtjd,
                durationHours = durationHours,
                depthPpm = depthPpm,
                teffK = teffK,
                radSun = radSun,
                massSun = massSun,
                distPc = distPc,
                vMag = vMag,
                impactB = impactB
            )
            _customCandidate.value = result
            // Prepend to list
            _monoTransits.value = listOf(result) + _monoTransits.value.filter { it.ticId != ticId }
        }
    }

    fun saveCandidateToResearchArchive(candidate: MonoTransitCandidate) {
        viewModelScope.launch {
            val entity = com.synthesis.data.local.entity.SynthesizedResearchEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = "Mono-Tranzit TESS: ${candidate.candidateName}",
                targetName = candidate.candidateName,
                researchCategory = "EXOPLANET",
                summary = "Candidat mono-tranzit TESS recuperat din unvetted backlog. " +
                        "Perioadă orbitală estimată: ${candidate.minimumPeriodDays.toInt()} - ${candidate.circularEstimatedPeriodDays.toInt()} zile. " +
                        "Rază planetară calculată: ${String.format(java.util.Locale.US, "%.2f", candidate.planetRadiusEarth)} R⊕. Zonă habitabilă: ${if (candidate.isHabitableZone) "DA" else "NU"}.",
                synthesizedDataJson = """{"tic":"${candidate.ticId}","duration_h":${candidate.transitDurationHours},"depth_ppm":${candidate.transitDepthPpm},"period_min_d":${candidate.minimumPeriodDays},"period_max_d":${candidate.circularEstimatedPeriodDays},"radius_earth":${candidate.planetRadiusEarth},"hz":${candidate.isHabitableZone}}""",
                confidenceScore = 0.91f,
                sourceCount = 3,
                observerNotes = "Detectat în sectorul TESS ${candidate.sector}. Necesar follow-up spectroscopic.",
                timestamp = System.currentTimeMillis(),
                isVerified = true,
                isBookmarked = false
            )
            synthesizedResearchRepository.saveResearch(entity)
        }
    }
}
