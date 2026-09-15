package com.synthesis.domain.model

data class Hypothesis(
    val id: String,
    val investigationId: String,
    val title: String,
    val description: String,
    val evidenceCount: Int,
    val contradictionCount: Int,
    val confidence: Float,
    val discriminatingTests: List<DiscriminatingTest>,
    val status: HypothesisStatus
)

data class DiscriminatingTest(
    val description: String,
    val requiredData: String,
    val requiredPrecision: String,
    val timeframe: String
)

enum class HypothesisStatus {
    ACTIVE, DISPROVEN, CONFIRMED, PENDING
}
