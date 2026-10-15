package com.synthesis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synthesis.data.local.entity.PureTicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PureTicDao {

    @Query("SELECT * FROM pure_tic_candidates ORDER BY ingestionTimestamp DESC")
    fun observeAll(): Flow<List<PureTicEntity>>

    @Query("SELECT * FROM pure_tic_candidates ORDER BY ingestionTimestamp DESC")
    suspend fun getAll(): List<PureTicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candidates: List<PureTicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(candidate: PureTicEntity)

    @Query("SELECT * FROM pure_tic_candidates WHERE ticId = :ticId LIMIT 1")
    suspend fun getByTicId(ticId: String): PureTicEntity?

    @Query("SELECT COUNT(*) FROM pure_tic_candidates")
    suspend fun getCount(): Int

    @Query("DELETE FROM pure_tic_candidates WHERE ticId IN (SELECT ticId FROM pure_tic_candidates WHERE isLiveIngested = 1 ORDER BY ingestionTimestamp ASC LIMIT :count)")
    suspend fun deleteOldestLive(count: Int)

    @Query("DELETE FROM pure_tic_candidates")
    suspend fun clearAll()
}
