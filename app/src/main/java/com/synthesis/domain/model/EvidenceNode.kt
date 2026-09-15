package com.synthesis.domain.model

data class EvidenceNode(
    val id: String,
    val investigationId: String,
    val label: String,
    val type: NodeType,
    val source: String,
    val relationship: RelationshipType,
    val description: String,
    val timestamp: Long,
    val x: Float = 0f,
    val y: Float = 0f
)

enum class NodeType {
    OBSERVATION, HYPOTHESIS, DATABASE, SATELLITE, PAPER, MEASUREMENT, SIGNAL, HISTORICAL
}

enum class RelationshipType {
    CONFIRMED, WEAK, CONTRADICTION, UNVERIFIED
}
