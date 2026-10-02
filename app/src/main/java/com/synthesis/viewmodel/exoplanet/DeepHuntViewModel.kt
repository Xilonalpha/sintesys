package com.synthesis.viewmodel.exoplanet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.exoplanet.UnvettedHuntRepository
import com.synthesis.domain.model.exoplanet.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeepHuntTab(val title: String) {
    MONO_TRANSITS("Mono-Transits (27d Blindspot)"),
    TTV_INVERSION("TTV Gravitational Inversion"),
    ATMOSPHERIC_TSM("JWST Atmospheric TSM"),
    NEGLECTED_JEWELS("NASA Backlog Purgatory")
}

@HiltViewModel
class DeepHuntViewModel @Inject constructor(
    private val repository: UnvettedHuntRepository
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

    // Interactive custom single-transit inversion solver state
    private val _customCandidate = MutableStateFlow<MonoTransitCandidate?>(null)
    val customCandidate: StateFlow<MonoTransitCandidate?> = _customCandidate.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: DeepHuntTab) {
        _selectedTab.value = tab
    }

    private fun loadData() {
        _monoTransits.value = repository.getCuratedMonoTransits()
        _ttvSystems.value = repository.getCuratedTtvSystems()
        _atmosphericScores.value = repository.getTopAtmosphericTsmScores()
        _neglectedJewels.value = repository.getNeglectedJewels()
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
}
