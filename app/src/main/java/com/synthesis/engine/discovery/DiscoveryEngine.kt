package com.synthesis.engine.discovery

import com.synthesis.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main orchestrator: takes raw observations for an investigation, detects hidden
 * patterns, scores each one for patentability and commercial value, and produces
 * ranked Discovery objects — new findings, not just an evaluation of existing ones.
 */
@Singleton
class DiscoveryEngine @Inject constructor(
    private val patternDetector: PatternDetector,
    private val patentabilityEngine: PatentabilityEngine,
    private val commercialValueEstimator: CommercialValueEstimator
) {

    suspend fun generateDiscoveries(
        investigationId: String,
        observations: List<Observation>
    ): List<Discovery> = withContext(Dispatchers.Default) {
        val patterns = patternDetector.detectHiddenPatterns(observations)
        if (patterns.isEmpty()) return@withContext emptyList()

        patterns.mapNotNull { pattern ->
            buildDiscovery(investigationId, pattern, observations)
        }.sortedByDescending { it.estimatedValueUsd }
    }

    private fun buildDiscovery(
        investigationId: String,
        pattern: PatternDetector.DetectedPattern,
        observations: List<Observation>
    ): Discovery? {
        if (pattern.significance < 0.3f) return null

        val evidence = pattern.involvedObservations.mapNotNull { obsId ->
            observations.firstOrNull { it.id == obsId }?.let { obs ->
                EvidenceItem(
                    source = obs.sourceName,
                    observationId = obs.id,
                    description = "${obs.observationType.name} measurement of ${obs.measurement.value} ${obs.measurement.unit}",
                    strength = pattern.significance
                )
            }
        }

        val hiddenPattern = HiddenPattern(
            patternType = pattern.patternType,
            description = pattern.description,
            statisticalSignificance = pattern.significance,
            recurrenceRate = estimateRecurrenceRate(pattern, observations),
            firstDetected = pattern.involvedObservations
                .mapNotNull { id -> observations.firstOrNull { it.id == id }?.timestamp }
                .minOrNull() ?: System.currentTimeMillis(),
            supportingDataPoints = pattern.involvedObservations.size
        )

        val discoveryType = mapToDiscoveryType(pattern.patternType, pattern.domains.distinct().size)
        val impactScore = computeImpactScore(pattern)
        val confidenceScore = computeConfidenceScore(pattern)

        // Draft discovery (fără scoruri comerciale finale încă) folosit ca input pentru patent engine
        var discovery = Discovery(
            investigationId = investigationId,
            title = buildTitle(discoveryType, pattern),
            description = pattern.description,
            discoveryType = discoveryType,
            noveltyScore = pattern.significance,
            confidenceScore = confidenceScore,
            impactScore = impactScore,
            commercialScore = 0f,
            patentabilityScore = 0f,
            estimatedValueUsd = 0L,
            supportingEvidence = evidence,
            domains = pattern.domains,
            sourceIds = pattern.involvedSources,
            hiddenPattern = hiddenPattern,
            recommendedActions = buildRecommendedActions(discoveryType),
            risks = buildRisks(pattern, confidenceScore)
        )

        val patentAssessment = patentabilityEngine.assess(discovery, pattern)
        val commercialEstimate = commercialValueEstimator.estimate(
            discovery.copy(patentabilityScore = patentAssessment.defensibilityScore),
            patentAssessment
        )

        discovery = discovery.copy(
            patentabilityScore = patentAssessment.defensibilityScore,
            commercialScore = commercialEstimate.licensingPotential.score,
            estimatedValueUsd = commercialEstimate.estimatedRevenueYear5Usd
        )

        return discovery
    }

    private fun mapToDiscoveryType(patternType: PatternType, domainCount: Int): DiscoveryType = when (patternType) {
        PatternType.TEMPORAL_COINCIDENCE -> DiscoveryType.NOVEL_CORRELATION
        PatternType.SPATIAL_CLUSTER -> DiscoveryType.ANOMALY_CLUSTER
        PatternType.STATISTICAL_OUTLIER -> if (domainCount >= 3) DiscoveryType.CROSS_DOMAIN_INSIGHT else DiscoveryType.NOVEL_CORRELATION
        PatternType.CAUSAL_CHAIN -> DiscoveryType.CAUSAL_LINK
        PatternType.PREDICTIVE_SIGNAL -> DiscoveryType.PREDICTIVE_PATTERN
        PatternType.STRUCTURAL_ANALOGY -> DiscoveryType.EMERGENT_PROPERTY
        PatternType.THRESHOLD_VIOLATION -> DiscoveryType.ANOMALY_CLUSTER
    }

    private fun computeImpactScore(pattern: PatternDetector.DetectedPattern): Float {
        val domainFactor = (pattern.domains.distinct().size / 4f).coerceIn(0f, 1f)
        return (pattern.significance * 0.6f + domainFactor * 0.4f).coerceIn(0f, 1f)
    }

    private fun computeConfidenceScore(pattern: PatternDetector.DetectedPattern): Float {
        val sourceFactor = (pattern.involvedSources.size / 4f).coerceIn(0f, 1f)
        val dataFactor = (pattern.involvedObservations.size / 10f).coerceIn(0f, 1f)
        return (pattern.significance * 0.5f + sourceFactor * 0.25f + dataFactor * 0.25f).coerceIn(0f, 1f)
    }

    private fun estimateRecurrenceRate(pattern: PatternDetector.DetectedPattern, observations: List<Observation>): Float {
        if (observations.isEmpty()) return 0f
        return (pattern.involvedObservations.size.toFloat() / observations.size).coerceIn(0f, 1f)
    }

    private fun buildTitle(discoveryType: DiscoveryType, pattern: PatternDetector.DetectedPattern): String {
        val domains = pattern.domains.distinct().joinToString(" × ")
        val label = when (discoveryType) {
            DiscoveryType.NOVEL_CORRELATION -> "Novel correlation"
            DiscoveryType.CAUSAL_LINK -> "Causal link"
            DiscoveryType.PREDICTIVE_PATTERN -> "Predictive pattern"
            DiscoveryType.ANOMALY_CLUSTER -> "Anomaly cluster"
            DiscoveryType.CROSS_DOMAIN_INSIGHT -> "Cross-domain insight"
            DiscoveryType.EMERGENT_PROPERTY -> "Emergent property"
            DiscoveryType.COMMERCIAL_OPPORTUNITY -> "Commercial opportunity"
        }
        return "$label: $domains"
    }

    private fun buildRecommendedActions(discoveryType: DiscoveryType): List<String> = when (discoveryType) {
        DiscoveryType.CAUSAL_LINK, DiscoveryType.EMERGENT_PROPERTY -> listOf(
            "Run a freedom-to-operate / prior-art search",
            "Draft a provisional patent application",
            "Seek independent replication with a fourth data source"
        )
        DiscoveryType.PREDICTIVE_PATTERN -> listOf(
            "Backtest the signal against historical events",
            "Build an alerting pipeline around the detected trend",
            "Evaluate licensing to relevant industry players"
        )
        else -> listOf(
            "Validate the correlation against additional independent sources",
            "Assess commercial applications before public disclosure"
        )
    }

    private fun buildRisks(pattern: PatternDetector.DetectedPattern, confidenceScore: Float): List<String> {
        val risks = mutableListOf<String>()
        if (confidenceScore < 0.5f) risks.add("Low data volume — pattern may not hold with more observations")
        if (pattern.involvedSources.size < 2) risks.add("Single-source dependency increases risk of measurement artifact")
        risks.add("Public disclosure before filing could forfeit patent rights — review timing carefully")
        return risks
    }
}
