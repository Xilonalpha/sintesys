package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.HypothesisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HypothesisDao {
    @Query("SELECT * FROM hypotheses WHERE investigationId = :investigationId")
    fun getByInvestigation(investigationId: String): Flow<List<HypothesisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hypothesis: HypothesisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hypotheses: List<HypothesisEntity>)
}
