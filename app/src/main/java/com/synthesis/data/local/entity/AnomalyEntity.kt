package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anomalies")
data class AnomalyEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val observationId: String,
    val expectedValue: Double,
    val observedValue: Double,
    val deviation: Double,
    val deviationPercent: Double,
    val anomalyScore: Float,
    val possibleCauses: String,
    val severity: String
)
