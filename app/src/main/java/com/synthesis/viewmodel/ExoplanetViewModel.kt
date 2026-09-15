package com.synthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.ExoplanetCandidate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExoplanetUiState {
    object Loading : ExoplanetUiState
    data class Success(val candidates: List<ExoplanetCandidate>) : ExoplanetUiState
    data class Error(val message: String) : ExoplanetUiState
}

@HiltViewModel
class ExoplanetViewModel @Inject constructor(
    private val repository: SynthesisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExoplanetUiState>(ExoplanetUiState.Loading)
    val uiState: StateFlow<ExoplanetUiState> = _uiState.asStateFlow()

    init {
        loadCandidates()
    }

    fun loadCandidates() {
        viewModelScope.launch {
            _uiState.value = ExoplanetUiState.Loading
            repository.fetchExoplanetCandidates()
                .onSuccess { candidates ->
                    _uiState.value = ExoplanetUiState.Success(candidates)
                }
                .onFailure { error ->
                    _uiState.value = ExoplanetUiState.Error(error.localizedMessage ?: "Unknown error")
                }
        }
    }
}
