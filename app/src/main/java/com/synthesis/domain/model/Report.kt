package com.synthesis.domain.model

data class ScientificReport(
    val id: String,
    val investigationId: String,
    val title: String,
    val executiveSummary: String,
    val researchQuestion: String,
    val dataSources: List<String>,
    val rawObservations: List<String>,
    val computedParameters: List<String>,
    val correlations: List<String>,
    val anomalies: List<String>,
    val hypotheses: List<String>,
    val contradictions: List<String>,
    val uncertainty: String,
    val requiredVerification: List<String>,
    val conclusion: String,
    val sourceProvenance: List<SourceProvenance>,
    val generatedAt: Long = System.currentTimeMillis()
)

data class SourceProvenance(
    val sourceName: String,
    val recordId: String,
    val retrievedAt: Long,
    val rawDataAvailable: Boolean,
    val transformations: List<String>,
    val calculationEngine: String,
    val derivedValue: String,
    val uncertainty: String,
    val status: String
)
