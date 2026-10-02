package com.synthesis.engine.hypothesis

import com.synthesis.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HypothesisEngine @Inject constructor() {

    data class HypothesisResult(
        val title: String,
        val description: String,
        val evidenceCount: Int,
        val contradictionCount: Int,
        val confidence: Float,
        val discriminatingTests: List<DiscriminatingTest>,
        val status: HypothesisStatus
    )

    fun generateHypotheses(
        observations: List<Observation>,
        anomalies: List<Anomaly>,
        correlations: List<Correlation>
    ): List<Hypothesis> {
        val investigationId = observations.firstOrNull()?.investigationId ?: ""
        val hypotheses = mutableListOf<Hypothesis>()

        // Hypothesis A: Known phenomenon
        val knownEvidence = observations.size + correlations.size
        val knownContradictions = anomalies.size
        val knownConfidence = if (knownEvidence + knownContradictions > 0) {
            (knownEvidence.toFloat() / (knownEvidence + knownContradictions).toFloat()) * 100
        } else 0f

        hypotheses.add(
            Hypothesis(
                id = "hyp_known",
                investigationId = investigationId,
                title = "Known phenomenon",
                description = "The observed data can be explained by previously catalogued natural phenomena. The correlations align with established scientific models.",
                evidenceCount = knownEvidence,
                contradictionCount = knownContradictions,
                confidence = knownConfidence.coerceIn(0f, 100f),
                discriminatingTests = listOf(
                    DiscriminatingTest(
                        "Compare with historical records of similar events",
                        "Historical database query",
                        "±0.5 sigma match",
                        "T+48h"
                    ),
                    DiscriminatingTest(
                        "Verify instrument calibration",
                        "Calibration logs",
                        "±0.02",
                        "T+24h"
                    )
                ),
                status = HypothesisStatus.ACTIVE
            )
        )

        // Hypothesis B: Previously unclassified phenomenon
        val unclassEvidence = anomalies.size + correlations.filter { it.strength > 0.7f }.size
        // Baseline control observations are scaled logarithmically so large catalog volumes don't artificially crush confidence to 0%
        val nonAnomalousCount = (observations.size - anomalies.size).coerceAtLeast(0)
        val unclassContradictions = if (nonAnomalousCount > 0) {
            (kotlin.math.ln(1.0 + nonAnomalousCount) * 2.5).toInt().coerceAtMost(nonAnomalousCount)
        } else 0
        val unclassConfidence = if (unclassEvidence + unclassContradictions > 0) {
            (unclassEvidence.toFloat() / (unclassEvidence + unclassContradictions).toFloat()) * 100
        } else 0f

        hypotheses.add(
            Hypothesis(
                id = "hyp_unclass",
                investigationId = investigationId,
                title = "Previously unclassified phenomenon",
                description = "The observed anomalies and correlations suggest a phenomenon not currently catalogued in scientific databases. Requires further verification.",
                evidenceCount = unclassEvidence,
                contradictionCount = unclassContradictions,
                confidence = unclassConfidence.coerceIn(0f, 100f),
                discriminatingTests = listOf(
                    DiscriminatingTest(
                        "Obtain multi-wavelength observation",
                        "IR + Optical + Radio data",
                        "±0.02",
                        "T+72h"
                    ),
                    DiscriminatingTest(
                        "Cross-reference with all known catalogs",
                        "Multi-database query",
                        "100% coverage",
                        "T+48h"
                    ),
                    DiscriminatingTest(
                        "Independent instrument verification",
                        "Secondary observatory data",
                        "±0.01",
                        "T+96h"
                    )
                ),
                status = HypothesisStatus.ACTIVE
            )
        )

        // Hypothesis C: Measurement artifact
        val artifactEvidence = if (anomalies.isEmpty()) observations.size else anomalies.filter { it.anomalyScore < 0.6f }.size
        val artifactContradictions = anomalies.filter { it.anomalyScore > 0.7f }.size + correlations.size
        val artifactConfidence = if (artifactEvidence + artifactContradictions > 0) {
            (artifactEvidence.toFloat() / (artifactEvidence + artifactContradictions).toFloat()) * 100
        } else 0f

        hypotheses.add(
            Hypothesis(
                id = "hyp_artifact",
                investigationId = investigationId,
                title = "Measurement artifact",
                description = "The observed deviations are consistent with known instrumentation errors, calibration drift, or data processing artifacts.",
                evidenceCount = artifactEvidence,
                contradictionCount = artifactContradictions,
                confidence = artifactConfidence.coerceIn(0f, 100f),
                discriminatingTests = listOf(
                    DiscriminatingTest(
                        "Reprocess raw data with alternative pipeline",
                        "Raw sensor data",
                        "±0.01",
                        "T+24h"
                    ),
                    DiscriminatingTest(
                        "Check instrument health logs",
                        "Telemetry data",
                        "Binary pass/fail",
                        "T+12h"
                    )
                ),
                status = HypothesisStatus.ACTIVE
            )
        )

        // Hypothesis D: Data synchronization problem
        if (correlations.any { it.strength < 0.3f }) {
            hypotheses.add(
                Hypothesis(
                    id = "hyp_sync",
                    investigationId = investigationId,
                    title = "Data synchronization problem",
                    description = "Inconsistent timestamps or spatial misalignment between data sources suggest a synchronization or coordinate transformation error.",
                    evidenceCount = correlations.filter { it.strength < 0.3f }.size,
                    contradictionCount = correlations.filter { it.strength > 0.5f }.size,
                    confidence = 15f,
                    discriminatingTests = listOf(
                        DiscriminatingTest(
                            "Verify UTC timestamps across all sources",
                            "Metadata audit",
                            "±1s",
                            "T+6h"
                        )
                    ),
                    status = HypothesisStatus.PENDING
                )
            )
        }

        return hypotheses.sortedByDescending { it.confidence }
    }

    fun generateNextActions(hypotheses: List<Hypothesis>): List<String> {
        val actions = mutableListOf<String>()
        val topHypothesis = hypotheses.firstOrNull()

        when {
            topHypothesis?.confidence ?: 0f < 30f -> {
                actions.add("Obtain additional independent observations")
                actions.add("Expand data source coverage")
            }
            topHypothesis?.title == "Previously unclassified phenomenon" -> {
                actions.add("Request follow-up observations at T+48h")
                actions.add("Alert relevant scientific community")
                actions.add("Schedule multi-wavelength observations")
            }
            topHypothesis?.title == "Measurement artifact" -> {
                actions.add("Perform instrument calibration check")
                actions.add("Reprocess data with alternative pipeline")
            }
            else -> {
                actions.add("Compare with historical records")
                actions.add("Run orbital simulation if applicable")
                actions.add("Search scientific literature for similar cases")
            }
        }

        return actions.distinct()
    }
}
