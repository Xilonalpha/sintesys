package com.synthesis.domain.model

import java.time.Instant
import java.util.UUID

data class Investigation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val query: String,
    val status: InvestigationStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourcesConsulted: Int = 0,
    val anomaliesFound: Int = 0,
    val confidenceScore: Float = 0f
)

enum class InvestigationStatus {
    PENDING,
    ACTIVE,
    ANALYZING,
    CORRELATING,
    COMPLETED,
    FAILED
}
