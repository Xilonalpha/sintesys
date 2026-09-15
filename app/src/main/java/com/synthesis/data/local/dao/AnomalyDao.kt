package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.AnomalyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnomalyDao {
    @Query("SELECT * FROM anomalies WHERE investigationId = :investigationId")
    fun getByInvestigation(investigationId: String): Flow<List<AnomalyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anomaly: AnomalyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(anomalies: List<AnomalyEntity>)
}
