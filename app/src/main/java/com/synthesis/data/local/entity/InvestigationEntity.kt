package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investigations")
data class InvestigationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val query: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sourcesConsulted: Int,
    val anomaliesFound: Int,
    val confidenceScore: Float
)
