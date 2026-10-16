package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xilon_discoveries")
data class XilonDiscoveryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ticId: String,
    val canonicalName: String,
    val sector: String,
    val verdict: String,
    val verdictTitle: String,
    val verdictSummary: String,
    val periodDays: Double,
    val depthPpm: Double,
    val durationHours: Double,
    val planetRadiusEarth: Double?,
    val planetMassEarth: Double?,
    val surfaceGravityG: Double?,
    val bulkDensityGcm3: Double?,
    val equilibriumTempK: Double?,
    val snr: Double,
    val isHabitableZone: Boolean,
    val archetype: String?,
    val archivalStatus: String,
    val scannedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val raDeg: Double? = null,
    val decDeg: Double? = null,
    val epochBjd: Double? = null
)
