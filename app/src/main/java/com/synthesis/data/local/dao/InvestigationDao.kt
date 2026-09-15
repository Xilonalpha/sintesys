package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.InvestigationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestigationDao {
    @Query("SELECT * FROM investigations ORDER BY createdAt DESC")
    fun getAll(): Flow<List<InvestigationEntity>>

    @Query("SELECT * FROM investigations WHERE id = :id")
    suspend fun getById(id: String): InvestigationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(investigation: InvestigationEntity)

    @Update
    suspend fun update(investigation: InvestigationEntity)

    @Delete
    suspend fun delete(investigation: InvestigationEntity)

    @Query("SELECT COUNT(*) FROM investigations WHERE status = 'ACTIVE'")
    fun getActiveCount(): Flow<Int>
}
