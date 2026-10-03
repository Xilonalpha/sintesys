package com.synthesis.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.repository.SynthesisRepository
import com.synthesis.domain.model.Investigation
import com.synthesis.service.worker.AstronomicalSyncManager
import com.synthesis.service.worker.CelestialSyncInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SynthesisRepository,
    private val astronomicalSyncManager: AstronomicalSyncManager
) : ViewModel() {

    val investigations: StateFlow<List<Investigation>> = repository.getInvestigations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _systemStatus = MutableStateFlow(
        SystemStatus(dataSources = 15, liveConnections = 0, activeAnalyses = 0, anomalies = 0)
    )
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    private val _syncInfo = MutableStateFlow(
        CelestialSyncInfo(0L, "SCHEDULED (1h WorkManager)", 4, 0, 0f)
    )
    val syncInfo: StateFlow<CelestialSyncInfo> = _syncInfo.asStateFlow()

    fun loadSyncInfo(context: Context) {
        _syncInfo.value = astronomicalSyncManager.getSyncInfo(context)
    }

    fun triggerLiveSync(context: Context) {
        astronomicalSyncManager.triggerImmediateSync(context)
        _syncInfo.value = _syncInfo.value.copy(status = "SYNCING LIVE...")
    }

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
