package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing synthesized astronomical research data, findings,
 * and scientific analysis stored locally in Room.
 */
@Entity(tableName = "synthesized_research")
data class SynthesizedResearchEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val targetName: String,
    val researchCategory: String, // e.g. "EXOPLANET", "SPECTROSCOPY", "STELLAR_VARIABILITY"
    val summary: String,
    val synthesizedDataJson: String,
    val confidenceScore: Float,
    val sourceCount: Int,
    val observerNotes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val isBookmarked: Boolean = false
)
