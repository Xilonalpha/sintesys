package com.synthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.Investigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SynthesisRepository
) : ViewModel() {

    val investigations: StateFlow<List<Investigation>> = repository.getInvestigations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _systemStatus = MutableStateFlow(
        SystemStatus(dataSources = 15, liveConnections = 0, activeAnalyses = 0, anomalies = 0)
    )
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    fun createInvestigation(title: String, query: String) {
        viewModelScope.launch { repository.createInvestigation(title, query) }
    }

    data class SystemStatus(
        val dataSources: Int,
        val liveConnections: Int,
        val activeAnalyses: Int,
        val anomalies: Int
    )
}
