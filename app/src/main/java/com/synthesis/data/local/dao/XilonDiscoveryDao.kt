package com.synthesis.data.local.dao

import androidx.room.*
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XilonDiscoveryDao {

    @Query("SELECT * FROM xilon_discoveries ORDER BY scannedAt DESC")
    fun getAllDiscoveries(): Flow<List<XilonDiscoveryEntity>>

    @Query("SELECT * FROM xilon_discoveries ORDER BY scannedAt DESC")
    suspend fun getDiscoveriesList(): List<XilonDiscoveryEntity>

    @Query("SELECT * FROM xilon_discoveries WHERE ticId = :ticId LIMIT 1")
    suspend fun getByTicId(ticId: String): XilonDiscoveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscovery(discovery: XilonDiscoveryEntity): Long

    @Query("DELETE FROM xilon_discoveries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM xilon_discoveries")
    suspend fun clearAll()
}
