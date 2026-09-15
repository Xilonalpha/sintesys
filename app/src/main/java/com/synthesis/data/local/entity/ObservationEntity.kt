package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val sourceId: String,
    val sourceName: String,
    val entity: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val value: Double,
    val unit: String,
    val expectedValue: Double?,
    val deviation: Double?,
    val rawData: String,
    val normalizedData: String,
    val uncertainty: Float,
    val observationType: String
)
