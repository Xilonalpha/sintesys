package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hypotheses")
data class HypothesisEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val title: String,
    val description: String,
    val evidenceCount: Int,
    val contradictionCount: Int,
    val confidence: Float,
    val discriminatingTests: String,
    val status: String
)
