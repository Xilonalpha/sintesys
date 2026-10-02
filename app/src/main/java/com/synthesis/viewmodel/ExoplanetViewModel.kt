package com.synthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExoplanetFilter(val title: String) {
    ALL("All Candidates"),
    UNVETTED_NEW_WORLDS("Unvetted Discoveries"),
    HABITABLE_ZONE("Habitable Zone"),
    TESS("TESS (TOI)"),
    KEPLER("Kepler (KOI)"),
    CONFIRMED("Confirmed Planets")
}

sealed interface ExoplanetUiState {
    object Loading : ExoplanetUiState
    data class Success(
        val allCount: Int,
        val displayedCandidates: List<ExoplanetCandidate>,
        val unvettedCount: Int,
        val habitableCount: Int
    ) : ExoplanetUiState
    data class Error(val message: String) : ExoplanetUiState
}

@HiltViewModel
class ExoplanetViewModel @Inject constructor(
    private val repository: SynthesisRepository
) : ViewModel() {

    private val _rawCandidates = MutableStateFlow<List<ExoplanetCandidate>>(emptyList())
    private val _selectedFilter = MutableStateFlow(ExoplanetFilter.ALL)
    val selectedFilter: StateFlow<ExoplanetFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<ExoplanetUiState>(ExoplanetUiState.Loading)
    val uiState: StateFlow<ExoplanetUiState> = _uiState.asStateFlow()

    init {
        loadCandidates()
    }

    fun loadCandidates() {
        viewModelScope.launch {
            _uiState.value = ExoplanetUiState.Loading
            repository.fetchAllExoplanets()
                .onSuccess { candidates ->
                    _rawCandidates.value = candidates
                    applyFilters()
                }
                .onFailure { error ->
                    // If combined fails (e.g. timeout on 4 streams), fallback to TESS + Kepler individually
                    repository.fetchExoplanetCandidates()
                        .onSuccess { fallbackCandidates ->
                            _rawCandidates.value = fallbackCandidates
                            applyFilters()
                        }
                        .onFailure { fallbackError ->
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
            ExoplanetFilter.ALL -> raw.filter { it.catalogSource != ExoplanetCatalogSource.CONFIRMED_BENCHMARK }
            ExoplanetFilter.UNVETTED_NEW_WORLDS -> raw.filter { it.isUnvettedCandidate }
            ExoplanetFilter.HABITABLE_ZONE -> raw.filter { it.isInHabitableZone }
            ExoplanetFilter.TESS -> raw.filter { it.catalogSource == ExoplanetCatalogSource.TESS || it.catalogSource == ExoplanetCatalogSource.UNVETTED_DISCOVERY && it.toiId.startsWith("TOI") }
            ExoplanetFilter.KEPLER -> raw.filter { it.catalogSource == ExoplanetCatalogSource.KEPLER_KOI || it.catalogSource == ExoplanetCatalogSource.UNVETTED_DISCOVERY && it.toiId.startsWith("K") }
            ExoplanetFilter.CONFIRMED -> raw.filter { it.catalogSource == ExoplanetCatalogSource.CONFIRMED_BENCHMARK }
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

        _uiState.value = ExoplanetUiState.Success(
            allCount = raw.size,
            displayedCandidates = filteredBySearch,
            unvettedCount = unvettedTotal,
            habitableCount = habitableTotal
        )
    }
}
