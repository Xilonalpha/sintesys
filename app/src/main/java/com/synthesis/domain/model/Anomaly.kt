package com.synthesis.domain.model

data class Anomaly(
    val id: String,
    val investigationId: String,
    val observationId: String,
    val expectedValue: Double,
    val observedValue: Double,
    val deviation: Double,
    val deviationPercent: Double,
    val anomalyScore: Float,
    val possibleCauses: List<PossibleCause>,
    val severity: AnomalySeverity
)

data class PossibleCause(
    val description: String,
    val probability: Float
)

enum class AnomalySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
