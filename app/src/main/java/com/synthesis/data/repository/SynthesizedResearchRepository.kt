package com.synthesis.data.repository

import com.synthesis.data.local.dao.SynthesizedResearchDao
import com.synthesis.data.local.entity.SynthesizedResearchEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository abstracting Room database operations for synthesized research data.
 */
@Singleton
class SynthesizedResearchRepository @Inject constructor(
    private val synthesizedResearchDao: SynthesizedResearchDao
) {
    val allResearch: Flow<List<SynthesizedResearchEntity>> = synthesizedResearchDao.getAllResearch()
    val bookmarkedResearch: Flow<List<SynthesizedResearchEntity>> = synthesizedResearchDao.getBookmarkedResearch()

    fun getResearchById(id: String): Flow<SynthesizedResearchEntity?> =
        synthesizedResearchDao.getResearchById(id)

    fun getResearchByCategory(category: String): Flow<List<SynthesizedResearchEntity>> =
        synthesizedResearchDao.getResearchByCategory(category)

    fun searchResearch(query: String): Flow<List<SynthesizedResearchEntity>> =
        synthesizedResearchDao.searchResearch(query)

    suspend fun saveResearch(research: SynthesizedResearchEntity) =
        synthesizedResearchDao.insertResearch(research)

    suspend fun saveAllResearch(researchList: List<SynthesizedResearchEntity>) =
        synthesizedResearchDao.insertAll(researchList)

    suspend fun updateResearch(research: SynthesizedResearchEntity) =
        synthesizedResearchDao.updateResearch(research)

    suspend fun toggleBookmark(id: String, currentStatus: Boolean) =
        synthesizedResearchDao.setBookmarkStatus(id, !currentStatus)

    suspend fun deleteResearch(research: SynthesizedResearchEntity) =
        synthesizedResearchDao.deleteResearch(research)

    suspend fun deleteResearchById(id: String) =
        synthesizedResearchDao.deleteResearchById(id)

    suspend fun clearAll() =
        synthesizedResearchDao.clearAllResearch()
}
