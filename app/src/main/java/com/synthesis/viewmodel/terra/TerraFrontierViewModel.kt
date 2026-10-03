package com.synthesis.viewmodel.terra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.terra.*
import com.synthesis.engine.terra.TerraFrontierEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerraFrontierViewModel @Inject constructor(
    private val engine: TerraFrontierEngine,
    private val repository: SynthesisRepository
) : ViewModel() {

    private val _selectedDomain = MutableStateFlow<TerraDomain?>(null)
    val selectedDomain: StateFlow<TerraDomain?> = _selectedDomain.asStateFlow()

    private val _allMysteries = MutableStateFlow(engine.getFrontierMysteries())
    val allMysteries: StateFlow<List<TerraFrontierMystery>> = _allMysteries.asStateFlow()

    val filteredMysteries: StateFlow<List<TerraFrontierMystery>> = combine(_allMysteries, _selectedDomain) { list, domain ->
        if (domain == null) list else list.filter { it.domain == domain }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hadalTrenches: StateFlow<List<HadalTrenchProfile>> = MutableStateFlow(engine.getHadalTrenchCatalog()).asStateFlow()

    private val _selectedTrench = MutableStateFlow<HadalTrenchProfile?>(engine.getHadalTrenchCatalog().firstOrNull())
    val selectedTrench: StateFlow<HadalTrenchProfile?> = _selectedTrench.asStateFlow()

    // Interactive Mantle Hydration Slider (wt% H2O in Ringwoodite)
    private val _mantleWaterWtPercent = MutableStateFlow(1.5)
    val mantleWaterWtPercent: StateFlow<Double> = _mantleWaterWtPercent.asStateFlow()

    val mantleHydrationState: StateFlow<MantleWaterHydrationState> = _mantleWaterWtPercent.map { wt ->
        engine.computeMantleHydrationBudget(wt)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        engine.computeMantleHydrationBudget(1.5)
    )

    // Interactive SAA Geomagnetic Simulation Year
    private val _simulationYear = MutableStateFlow(2026)
    val simulationYear: StateFlow<Int> = _simulationYear.asStateFlow()

    val geomagneticState: StateFlow<GeomagneticAnomalousState> = _simulationYear.map { yr ->
        engine.simulateGeomagneticDecayAndDrift(yr)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        engine.simulateGeomagneticDecayAndDrift(2026)
    )

    // Deep Subduction & Seafloor Earthquakes telemetry
    private val _deepSubductionEventsCount = MutableStateFlow(42)
    val deepSubductionEventsCount: StateFlow<Int> = _deepSubductionEventsCount.asStateFlow()

    // Pipeline Data-Driven Dashboard States
    private val _pipelineStages = MutableStateFlow(engine.getInitialPipelineStages())
    val pipelineStages: StateFlow<List<PipelineStageItem>> = _pipelineStages.asStateFlow()

    private val _telemetryFeeds = MutableStateFlow(engine.getRealtimeTelemetryFeeds())
    val telemetryFeeds: StateFlow<List<TelemetryFeedItem>> = _telemetryFeeds.asStateFlow()

    val frontierMetrics: StateFlow<List<FrontierProgressMetric>> = MutableStateFlow(engine.getFrontierProgressMetrics()).asStateFlow()

    private val _pipelineOverview = MutableStateFlow(
        TerraPipelineOverview(
            overallProgressPercent = 78,
            activeThreads = 16,
            totalObservationsProcessed = 316310L,
            activeAnomaliesCount = 4,
            pipelineStatusLabel = "NOMINAL (Acoperire Multi-Senzor)",
            isSynthesizing = false
        )
    )
    val pipelineOverview: StateFlow<TerraPipelineOverview> = _pipelineOverview.asStateFlow()

    // Antarctica Subglacial Explorer States
    val subglacialLakes: StateFlow<List<AntarcticSubglacialLake>> =
        MutableStateFlow(engine.getAntarcticSubglacialLakes()).asStateFlow()

    private val _selectedSubglacialLake = MutableStateFlow<AntarcticSubglacialLake?>(engine.getAntarcticSubglacialLakes().firstOrNull())
    val selectedSubglacialLake: StateFlow<AntarcticSubglacialLake?> = _selectedSubglacialLake.asStateFlow()

    val bedrockFeatures: StateFlow<List<AntarcticBedrockFeature>> =
        MutableStateFlow(engine.getAntarcticBedrockFeatures()).asStateFlow()

    private val _selectedBedrockFeature = MutableStateFlow<AntarcticBedrockFeature?>(engine.getAntarcticBedrockFeatures().firstOrNull())
    val selectedBedrockFeature: StateFlow<AntarcticBedrockFeature?> = _selectedBedrockFeature.asStateFlow()

    val polarAltimetry: StateFlow<PolarAltimetryState> =
        MutableStateFlow(engine.getPolarAltimetryState()).asStateFlow()

    private val _iceMeltFraction = MutableStateFlow(0.0f)
    val iceMeltFraction: StateFlow<Float> = _iceMeltFraction.asStateFlow()

    val isostaticRebound: StateFlow<IsostaticReboundSimulation> = _iceMeltFraction.map { fraction ->
        engine.computeIsostaticRebound(fraction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), engine.computeIsostaticRebound(0.0f))

    init {
        loadEarthTelemetry()
    }

    fun triggerSynthesisScan() {
        if (_pipelineOverview.value.isSynthesizing) return
        viewModelScope.launch {
            _pipelineOverview.value = _pipelineOverview.value.copy(
                isSynthesizing = true,
                pipelineStatusLabel = "SINTEZĂ ACTIVĂ (Inversie Tomografică & Bilanț Hadal...)"
            )

            // Step 1: Animate processing stage 2 & 3
            _pipelineStages.value = _pipelineStages.value.map { stage ->
                if (stage.stageNumber in 2..3) stage.copy(status = PipelineStageStatus.PROCESSING) else stage
            }
            kotlinx.coroutines.delay(1200)

            // Step 2: Push new telemetry pulse
            val newFeed = TelemetryFeedItem(
                timestamp = "ACUM 1s",
                sourceDomain = TerraDomain.HADAL_OCEANS,
                sourceName = "Autonomous Synthesis Engine",
                eventMessage = "Convergență confirmată: Serpentinizarea foselor abisale produce ~14.8 mol H₂/m²·an energie prebiotică",
                anomalyScore = 0.94,
                formulaBadge = "P = 1.086 atm & ΔH_chem"
            )
            _telemetryFeeds.value = listOf(newFeed) + _telemetryFeeds.value.take(6)

            // Step 3: Complete scan with elevated progress
            _pipelineStages.value = _pipelineStages.value.map { stage ->
                when (stage.stageNumber) {
                    2 -> stage.copy(status = PipelineStageStatus.COMPLETED, processedSamples = stage.processedSamples + 1520L)
                    3 -> stage.copy(status = PipelineStageStatus.FLAGGED_ANOMALY, processedSamples = stage.processedSamples + 890L)
                    4 -> stage.copy(status = PipelineStageStatus.COMPLETED, processedSamples = stage.processedSamples + 12L)
                    else -> stage
                }
            }

            _pipelineOverview.value = _pipelineOverview.value.copy(
                overallProgressPercent = 86,
                totalObservationsProcessed = _pipelineOverview.value.totalObservationsProcessed + 2422L,
                pipelineStatusLabel = "SINTEZĂ FINALIZATĂ (Ipoteze Geofizice Revalidate)",
                isSynthesizing = false
            )
        }
    }

    fun selectDomain(domain: TerraDomain?) {
        _selectedDomain.value = domain
    }

    fun selectTrench(trench: HadalTrenchProfile) {
        _selectedTrench.value = trench
    }

    fun updateMantleWaterSlider(wtPercent: Double) {
        _mantleWaterWtPercent.value = wtPercent
    }

    fun updateSimulationYear(year: Int) {
        _simulationYear.value = year
    }

    fun selectSubglacialLake(lake: AntarcticSubglacialLake) {
        _selectedSubglacialLake.value = lake
    }

    fun selectBedrockFeature(feature: AntarcticBedrockFeature) {
        _selectedBedrockFeature.value = feature
    }

    fun updateIceMeltSlider(fraction: Float) {
        _iceMeltFraction.value = fraction
    }

    private fun loadEarthTelemetry() {
        viewModelScope.launch {
            try {
                val earthquakes = repository.fetchUsgsData(minMagnitude = 4.5).getOrNull()
                if (earthquakes != null && earthquakes.isNotEmpty()) {
                    _deepSubductionEventsCount.value = earthquakes.size
                }
            } catch (_: Exception) {
                // Keep default baseline
            }
        }
    }
}
