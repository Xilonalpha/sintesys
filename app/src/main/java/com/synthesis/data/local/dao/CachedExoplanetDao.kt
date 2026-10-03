package com.synthesis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synthesis.data.local.entity.CachedExoplanetEntity

@Dao
interface CachedExoplanetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candidates: List<CachedExoplanetEntity>)

    @Query("SELECT * FROM cached_exoplanets ORDER BY worthInvestigatingScore DESC LIMIT 500")
    suspend fun getCachedCandidates(): List<CachedExoplanetEntity>

    @Query("SELECT COUNT(*) FROM cached_exoplanets")
    suspend fun getCount(): Int

    @Query("DELETE FROM cached_exoplanets WHERE toiId NOT IN (SELECT toiId FROM cached_exoplanets ORDER BY worthInvestigatingScore DESC LIMIT 500)")
    suspend fun trimToMax500()

    @Query("DELETE FROM cached_exoplanets")
    suspend fun clearAll()
}
