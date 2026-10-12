package com.synthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.data.repository.xilon.PureTicCatalogRepository
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.service.worker.AstronomicalSyncManager
import com.synthesis.service.worker.CelestialSyncInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExoplanetFilter(val title: String) {
    ALL("All Candidates"),
    PURE_TIC("TIC fără TOI (Virgin)"),
    PHL_HABITABLE("PHL Habitable (ESI)"),
    HABITABLE_ZONE("Habitable Zone"),
    UNVETTED_NEW_WORLDS("Unvetted Discoveries"),
    TESS("TESS (TOI)"),
    KEPLER("Kepler (KOI & K2)"),
    CHEOPS_GAIA("CHEOPS & Gaia"),
    CONFIRMED("Confirmed Planets")
}

sealed interface ExoplanetUiState {
    data object Loading : ExoplanetUiState
    data class Success(
        val allCount: Int,
        val displayedCandidates: List<ExoplanetCandidate>,
        val unvettedCount: Int,
        val habitableCount: Int,
        val phlCount: Int = 0,
        val cheopsCount: Int = 0,
        val pureTicCount: Int = 0
    ) : ExoplanetUiState
    data class Error(val message: String) : ExoplanetUiState
}

@HiltViewModel
class ExoplanetViewModel @Inject constructor(
    private val repository: SynthesisRepository,
    private val astronomicalSyncManager: AstronomicalSyncManager,
    private val pureTicCatalogRepo: PureTicCatalogRepository
) : ViewModel() {

    private val _rawCandidates = MutableStateFlow<List<ExoplanetCandidate>>(emptyList())
    private val _selectedFilter = MutableStateFlow(ExoplanetFilter.ALL)
    val selectedFilter: StateFlow<ExoplanetFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<ExoplanetUiState>(ExoplanetUiState.Loading)
    val uiState: StateFlow<ExoplanetUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncInfo = MutableStateFlow(
        CelestialSyncInfo(System.currentTimeMillis() - 120_000L, "ONLINE (JPL & NASA TAP)", 4, 5200, 14.25f)
    )
    val syncInfo: StateFlow<CelestialSyncInfo> = _syncInfo.asStateFlow()

    init {
        loadCandidates()
    }

    fun loadSyncInfo(context: Context) {
        val info = astronomicalSyncManager.getSyncInfo(context)
        if (info.lastSyncMs > 0) {
            _syncInfo.value = info
        }
    }

    fun triggerEphemerisSync(context: Context) {
        astronomicalSyncManager.triggerImmediateSync(context)
        _syncInfo.value = _syncInfo.value.copy(status = "SYNCING JPL/NASA...")
        refreshCandidates()
    }

    fun loadCandidates() {
        viewModelScope.launch {
            _uiState.value = ExoplanetUiState.Loading
            fetchCandidatesInternal()
        }
    }

    fun refreshCandidates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchCandidatesInternal()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchCandidatesInternal() {
        val pureCandidates = pureTicCatalogRepo.getPureTicCatalog().map {
            pureTicCatalogRepo.toExoplanetCandidate(it)
        }

        repository.fetchAllExoplanets()
            .onSuccess { candidates ->
                _rawCandidates.value = pureCandidates + candidates
                applyFilters()
            }
            .onFailure { error ->
                // If combined fails (e.g. timeout on 4 streams), fallback to TESS + Kepler individually
                repository.fetchExoplanetCandidates()
                    .onSuccess { fallbackCandidates ->
                        _rawCandidates.value = pureCandidates + fallbackCandidates
                        applyFilters()
                    }
                    .onFailure { fallbackError ->
                        // Even if remote fails, we have the pure TIC catalog!
                        if (pureCandidates.isNotEmpty()) {
                            _rawCandidates.value = pureCandidates
                            applyFilters()
                        } else {
                            _uiState.value = ExoplanetUiState.Error(fallbackError.localizedMessage ?: "Archive query failed")
                        }
                    }
            }
    }

    fun setFilter(filter: ExoplanetFilter) {
        _selectedFilter.value = filter
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        val raw = _rawCandidates.value
        val filter = _selectedFilter.value
        val query = _searchQuery.value.trim().lowercase()

        val filteredByTab = when (filter) {
            ExoplanetFilter.ALL -> raw
            ExoplanetFilter.PURE_TIC -> raw.filter { it.catalogSource == ExoplanetCatalogSource.PURE_TIC_CATALOG || (!it.toiId.startsWith("TOI") && it.ticId.startsWith("TIC")) }
            ExoplanetFilter.PHL_HABITABLE -> raw.filter { it.catalogSource == ExoplanetCatalogSource.PHL_HABITABLE }
            ExoplanetFilter.HABITABLE_ZONE -> raw.filter { it.isInHabitableZone }
            ExoplanetFilter.UNVETTED_NEW_WORLDS -> raw.filter { it.isUnvettedCandidate }
            ExoplanetFilter.TESS -> raw.filter { it.catalogSource == ExoplanetCatalogSource.TESS || (it.catalogSource == ExoplanetCatalogSource.UNVETTED_DISCOVERY && it.toiId.startsWith("TOI")) }
            ExoplanetFilter.KEPLER -> raw.filter { it.catalogSource == ExoplanetCatalogSource.KEPLER_KOI || it.catalogSource == ExoplanetCatalogSource.K2 || (it.catalogSource == ExoplanetCatalogSource.UNVETTED_DISCOVERY && it.toiId.startsWith("K")) }
            ExoplanetFilter.CHEOPS_GAIA -> raw.filter { it.catalogSource == ExoplanetCatalogSource.CHEOPS_GAIA }
            ExoplanetFilter.CONFIRMED -> raw.filter { it.catalogSource == ExoplanetCatalogSource.CONFIRMED_BENCHMARK || it.catalogSource == ExoplanetCatalogSource.PHL_HABITABLE || it.catalogSource == ExoplanetCatalogSource.CHEOPS_GAIA }
        }

        val filteredBySearch = if (query.isEmpty()) {
            filteredByTab
        } else {
            filteredByTab.filter { candidate ->
                candidate.toiId.lowercase().contains(query) ||
                candidate.ticId.lowercase().contains(query) ||
                (candidate.discoveryMethod?.lowercase()?.contains(query) == true)
            }
        }

        val unvettedTotal = raw.count { it.isUnvettedCandidate }
        val habitableTotal = raw.count { it.isInHabitableZone }
        val phlTotal = raw.count { it.catalogSource == ExoplanetCatalogSource.PHL_HABITABLE }
        val cheopsTotal = raw.count { it.catalogSource == ExoplanetCatalogSource.CHEOPS_GAIA }
        val pureTicTotal = raw.count { it.catalogSource == ExoplanetCatalogSource.PURE_TIC_CATALOG }

        _uiState.value = ExoplanetUiState.Success(
            allCount = raw.size,
            displayedCandidates = filteredBySearch,
            unvettedCount = unvettedTotal,
            habitableCount = habitableTotal,
            phlCount = phlTotal,
            cheopsCount = cheopsTotal,
            pureTicCount = pureTicTotal
        )
    }
}
