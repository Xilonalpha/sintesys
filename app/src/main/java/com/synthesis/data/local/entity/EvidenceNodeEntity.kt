package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evidence_nodes")
data class EvidenceNodeEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val label: String,
    val type: String,
    val source: String,
    val relationship: String,
    val description: String,
    val timestamp: Long,
    val x: Float,
    val y: Float
)
