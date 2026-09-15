package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.ObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations WHERE investigationId = :investigationId")
    fun getByInvestigation(investigationId: String): Flow<List<ObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: ObservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(observations: List<ObservationEntity>)

    @Query("SELECT COUNT(*) FROM observations WHERE investigationId = :investigationId")
    suspend fun countByInvestigation(investigationId: String): Int
}
