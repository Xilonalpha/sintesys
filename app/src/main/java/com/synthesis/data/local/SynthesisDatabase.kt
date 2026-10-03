package com.synthesis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.synthesis.data.local.dao.*
import com.synthesis.data.local.entity.*

@Database(
    entities = [
        InvestigationEntity::class,
        ObservationEntity::class,
        AnomalyEntity::class,
        HypothesisEntity::class,
        EvidenceNodeEntity::class,
        XilonDiscoveryEntity::class,
        CachedExoplanetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SynthesisDatabase : RoomDatabase() {
    abstract fun investigationDao(): InvestigationDao
    abstract fun observationDao(): ObservationDao
    abstract fun anomalyDao(): AnomalyDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun evidenceNodeDao(): EvidenceNodeDao
    abstract fun xilonDiscoveryDao(): XilonDiscoveryDao
    abstract fun cachedExoplanetDao(): CachedExoplanetDao
}
