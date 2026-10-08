package com.synthesis.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.synthesis.data.local.entity.SynthesizedResearchEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for synthesized astronomical research records.
 * Adheres to reactive Flow streams and non-blocking coroutines.
 */
@Dao
interface SynthesizedResearchDao {

    @Query("SELECT * FROM synthesized_research ORDER BY timestamp DESC")
    fun getAllResearch(): Flow<List<SynthesizedResearchEntity>>

    @Query("SELECT * FROM synthesized_research WHERE id = :id")
    fun getResearchById(id: String): Flow<SynthesizedResearchEntity?>

    @Query("SELECT * FROM synthesized_research WHERE researchCategory = :category ORDER BY timestamp DESC")
    fun getResearchByCategory(category: String): Flow<List<SynthesizedResearchEntity>>

    @Query("SELECT * FROM synthesized_research WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedResearch(): Flow<List<SynthesizedResearchEntity>>

    @Query("SELECT * FROM synthesized_research WHERE title LIKE '%' || :query || '%' OR targetName LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchResearch(query: String): Flow<List<SynthesizedResearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResearch(research: SynthesizedResearchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(researchList: List<SynthesizedResearchEntity>)

    @Update
    suspend fun updateResearch(research: SynthesizedResearchEntity)

    @Query("UPDATE synthesized_research SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmarkStatus(id: String, isBookmarked: Boolean)

    @Delete
    suspend fun deleteResearch(research: SynthesizedResearchEntity)

    @Query("DELETE FROM synthesized_research WHERE id = :id")
    suspend fun deleteResearchById(id: String)

    @Query("DELETE FROM synthesized_research")
    suspend fun clearAllResearch()
}
