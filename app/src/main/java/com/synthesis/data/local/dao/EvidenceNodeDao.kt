package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.EvidenceNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceNodeDao {
    @Query("SELECT * FROM evidence_nodes WHERE investigationId = :investigationId")
    fun getByInvestigation(investigationId: String): Flow<List<EvidenceNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: EvidenceNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<EvidenceNodeEntity>)
}
