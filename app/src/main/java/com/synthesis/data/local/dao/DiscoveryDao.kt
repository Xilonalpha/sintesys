package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.DiscoveryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local persistence of synthesized discoveries.
 * Returns reactive Flow streams and uses non-blocking coroutines.
 */
@Dao
interface DiscoveryDao {

    @Query("SELECT * FROM discoveries ORDER BY estimatedValueUsd DESC, createdAt DESC")
    fun getAll(): Flow<List<DiscoveryEntity>>

    @Query("SELECT * FROM discoveries WHERE investigationId = :investigationId ORDER BY estimatedValueUsd DESC, createdAt DESC")
    fun getByInvestigation(investigationId: String): Flow<List<DiscoveryEntity>>

    @Query("SELECT * FROM discoveries WHERE id = :id")
    suspend fun getById(id: String): DiscoveryEntity?

    @Query("SELECT * FROM discoveries WHERE discoveryType = :discoveryType ORDER BY estimatedValueUsd DESC")
    fun getByType(discoveryType: String): Flow<List<DiscoveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discovery: DiscoveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(discoveries: List<DiscoveryEntity>)

    @Update
    suspend fun update(discovery: DiscoveryEntity)

    @Delete
    suspend fun delete(discovery: DiscoveryEntity)

    @Query("DELETE FROM discoveries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM discoveries WHERE investigationId = :investigationId")
    suspend fun deleteByInvestigation(investigationId: String)

    @Query("SELECT COUNT(*) FROM discoveries")
    fun getCount(): Flow<Int>
}
