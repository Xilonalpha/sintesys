package com.synthesis.domain.model

data class Correlation(
    val id: String,
    val investigationId: String,
    val sourceA: String,
    val sourceB: String,
    val correlationType: CorrelationType,
    val strength: Float,
    val description: String,
    val causalityEstablished: Boolean = false,
    val domains: List<String>
)

enum class CorrelationType {
    TEMPORAL, SPATIAL, CAUSAL, STATISTICAL, UNKNOWN
}
