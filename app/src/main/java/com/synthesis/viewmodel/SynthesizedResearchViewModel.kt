package com.synthesis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synthesis.data.local.entity.SynthesizedResearchEntity
import com.synthesis.data.repository.SynthesizedResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SynthesizedResearchViewModel @Inject constructor(
    private val repository: SynthesizedResearchRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val bookmarkedOnly = MutableStateFlow(false)

    val allResearch: StateFlow<List<SynthesizedResearchEntity>> = repository.allResearch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredResearch: StateFlow<List<SynthesizedResearchEntity>> = combine(
        allResearch,
        searchQuery,
        selectedCategory,
        bookmarkedOnly
    ) { list, query, category, onlyBookmarked ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.targetName.contains(query, ignoreCase = true) ||
                item.summary.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.researchCategory.equals(category, ignoreCase = true)
            val matchesBookmark = !onlyBookmarked || item.isBookmarked
            matchesQuery && matchesCategory && matchesBookmark
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allResearch.first { list ->
                if (list.isEmpty()) {
                    seedInitialResearch()
                }
                true
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.value = if (selectedCategory.value == category) null else category
    }

    fun onToggleBookmarkFilter() {
        bookmarkedOnly.value = !bookmarkedOnly.value
    }

    fun toggleBookmark(item: SynthesizedResearchEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(item.id, item.isBookmarked)
        }
    }

    fun deleteResearch(id: String) {
        viewModelScope.launch {
            repository.deleteResearchById(id)
        }
    }

    fun clearAllResearch() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun saveSynthesizedResearch(
        title: String,
        targetName: String,
        researchCategory: String,
        summary: String,
        synthesizedDataJson: String,
        confidenceScore: Float,
        sourceCount: Int,
        observerNotes: String = ""
    ) {
        viewModelScope.launch {
            val entity = SynthesizedResearchEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                targetName = targetName,
                researchCategory = researchCategory,
                summary = summary,
                synthesizedDataJson = synthesizedDataJson,
                confidenceScore = confidenceScore,
                sourceCount = sourceCount,
                observerNotes = observerNotes,
                timestamp = System.currentTimeMillis(),
                isVerified = true,
                isBookmarked = false
            )
            repository.saveResearch(entity)
        }
    }

    fun seedInitialResearch() {
        viewModelScope.launch {
            val samples = listOf(
                SynthesizedResearchEntity(
                    id = "synth_jwst_lhs475b",
                    title = "Spectroscopie NIRSpec: Atmosferă LHS 475 b",
                    targetName = "LHS 475 b",
                    researchCategory = "SPECTROSCOPY",
                    summary = "Sinteză combinată de transmisie atmosferică în banda 3.0-5.0 μm. Indiciu de CO2 dens sau atmosferă subțire dominată de azot.",
                    synthesizedDataJson = """{"instrument": "JWST NIRSpec/G395H", "snr": 48.2, "co2_detected": true, "metallicity_solar": 10.5, "equilibrium_temp_k": 586}""",
                    confidenceScore = 0.94f,
                    sourceCount = 4,
                    observerNotes = "Confirmare necesară cu al doilea tranzit programat în Ciclul 3 JWST.",
                    timestamp = System.currentTimeMillis() - 86400000L * 2,
                    isVerified = true,
                    isBookmarked = true
                ),
                SynthesizedResearchEntity(
                    id = "synth_roman_rl2026",
                    title = "Anomalie Gravitațională Microlensing Roman-2026",
                    targetName = "RL-2026-BLG-0089",
                    researchCategory = "MICROLENSING",
                    summary = "Detecție de planetă rătăcitoare (Rogue Planet) de masă sub-terestră în bulbul galactic. Fără stea gazdă detectată la 0.1 mas.",
                    synthesizedDataJson = """{"lens_mass_earth": 1.42, "einstein_radius_mas": 0.082, "amplification": 145.8, "survey": "Nancy Grace Roman Sim"}""",
                    confidenceScore = 0.89f,
                    sourceCount = 3,
                    observerNotes = "Curba de lumină arată deviație caustică asimetrică de 1.8 ore.",
                    timestamp = System.currentTimeMillis() - 86400000L * 5,
                    isVerified = true,
                    isBookmarked = false
                ),
                SynthesizedResearchEntity(
                    id = "synth_plato_hd189",
                    title = "Moduri Asteroseismice și Rotație Diferențială",
                    targetName = "HD 189733 (Stea Gazdă)",
                    researchCategory = "ASTEROSEISMOLOGY",
                    summary = "Descompunerea oscilațiilor acustice de tip solar p-modes (l=0,1,2). Densitate stelară medie determinată cu precizie de 1.2%.",
                    synthesizedDataJson = """{"delta_nu_microhz": 118.4, "nu_max_microhz": 2480.0, "stellar_age_gyr": 5.4, "radius_solar": 0.781}""",
                    confidenceScore = 0.96f,
                    sourceCount = 5,
                    observerNotes = "Vârsta stelară revizuită stabilizează modelele de evaporare atmosferică a planetei.",
                    timestamp = System.currentTimeMillis() - 86400000L * 7,
                    isVerified = true,
                    isBookmarked = true
                ),
                SynthesizedResearchEntity(
                    id = "synth_tess_ocean_koi",
                    title = "Disipare Mareică și Rezistență Ocean Subcrustal",
                    targetName = "TOI-715.01",
                    researchCategory = "TIDAL_OCEAN",
                    summary = "Modelare cuplaj mareic exolună-super-Pământ în zona habitabilă. Încălzire mareică calculată la 42 mW/m², compatibilă cu ocean lichid intern.",
                    synthesizedDataJson = """{"tidal_heat_flux": 42.1, "ocean_depth_km": 85.0, "eccentricity": 0.048, "ice_shell_thickness_km": 14.2}""",
                    confidenceScore = 0.88f,
                    sourceCount = 3,
                    observerNotes = "Favorabil pentru misiuni spectrografice viitoare ELT/ANDES.",
                    timestamp = System.currentTimeMillis() - 86400000L * 9,
                    isVerified = false,
                    isBookmarked = false
                )
            )
            repository.saveAllResearch(samples)
        }
    }
}
