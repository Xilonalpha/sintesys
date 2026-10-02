package com.synthesis.engine.hypothesis

import com.synthesis.domain.model.*
import com.synthesis.data.repository.SynthesisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Impact IP Engine — dedicated engine for high-impact discovery scoring.
 * Evaluates scientific observations across multiple dimensions to identify
 * discoveries with potential for patents, publications, or paradigm shifts.
 */
@Singleton
class ImpactIpEngine @Inject constructor(
    private val repository: SynthesisRepository
) {

    data class ImpactAssessment(
        val discoveryId: String,
        val title: String,
        val overallScore: Float, // 0.0 - 1.0
        val dimensions: List<ImpactDimension>,
        val patentPotential: PatentPotential,
        val publicationPotential: PublicationPotential,
        val paradigmShiftScore: Float,
        val recommendedActions: List<String>,
        val riskFactors: List<RiskFactor>
    )

    data class ImpactDimension(
        val name: String,
        val score: Float,
        val weight: Float,
        val evidence: String
    )

    data class PatentPotential(
        val score: Float,
        val noveltyScore: Float,
        val utilityScore: Float,
        val nonObviousnessScore: Float,
        val priorArtDensity: Float,
        val recommendedPatentClass: String?
    )

    data class PublicationPotential(
        val score: Float,
        val journalImpactEstimate: String, // e.g. "Nature/Science tier"
        val citationPotential: Float,
        val interdisciplinaryAppeal: Float,
        val reproducibilityScore: Float
    )

    data class RiskFactor(
        val description: String,
        val severity: Float, // 0.0 - 1.0
        val mitigable: Boolean
    )

    suspend fun assessDiscovery(
        investigationId: String,
        observations: List<Observation>,
        anomalies: List<Anomaly>,
        correlations: List<Correlation>,
        hypotheses: List<Hypothesis>
    ): ImpactAssessment = withContext(Dispatchers.Default) {

        val discoveryId = "impact_${investigationId.take(8)}_${System.currentTimeMillis()}"
        val title = generateDiscoveryTitle(observations, anomalies)

        // Dimension 1: Novelty (how unexpected is this?)
        val noveltyScore = calculateNovelty(observations, anomalies, hypotheses)

        // Dimension 2: Cross-domain breadth
        val breadthScore = calculateCrossDomainBreadth(observations)

        // Dimension 3: Data quality & source independence
        val qualityScore = calculateDataQuality(observations)

        // Dimension 4: Anomaly severity & consistency
        val anomalyScore = calculateAnomalyImpact(anomalies)

        // Dimension 5: Correlation strength & causality potential
        val correlationScore = calculateCorrelationImpact(correlations)

        // Dimension 6: Reproducibility (how easy to verify?)
        val reproducibilityScore = calculateReproducibility(observations)

        val dimensions = listOf(
            ImpactDimension("Novelty", noveltyScore, 0.25f, "Based on deviation from expected values and uniqueness of observation patterns"),
            ImpactDimension("Cross-Domain Breadth", breadthScore, 0.20f, "Number of independent scientific domains providing convergent evidence"),
            ImpactDimension("Data Quality", qualityScore, 0.15f, "Source independence, measurement precision, and provenance traceability"),
            ImpactDimension("Anomaly Severity", anomalyScore, 0.15f, "Magnitude and statistical significance of detected anomalies"),
            ImpactDimension("Correlation Strength", correlationScore, 0.15f, "Strength of cross-domain correlations and causality indicators"),
            ImpactDimension("Reproducibility", reproducibilityScore, 0.10f, "Ease of independent verification with existing public data")
        )

        val overallScore = dimensions.sumOf { (it.score * it.weight).toDouble() }.toFloat()

        val patentPotential = assessPatentPotential(observations, anomalies, correlations, noveltyScore)
        val publicationPotential = assessPublicationPotential(observations, anomalies, correlations, overallScore)
        val paradigmShiftScore = calculateParadigmShiftScore(observations, anomalies, hypotheses, overallScore)

        val recommendedActions = generateRecommendedActions(overallScore, patentPotential, publicationPotential, observations)
        val riskFactors = identifyRiskFactors(observations, anomalies, hypotheses)

        ImpactAssessment(
            discoveryId = discoveryId,
            title = title,
            overallScore = overallScore,
            dimensions = dimensions,
            patentPotential = patentPotential,
            publicationPotential = publicationPotential,
            paradigmShiftScore = paradigmShiftScore,
            recommendedActions = recommendedActions,
            riskFactors = riskFactors
        )
    }

    private fun generateDiscoveryTitle(observations: List<Observation>, anomalies: List<Anomaly>): String {
        val domains = observations.map { it.observationType.name }.distinct()
        val topAnomaly = anomalies.maxByOrNull { it.anomalyScore }
        return when {
            domains.size >= 4 && (topAnomaly?.anomalyScore ?: 0f) > 0.8f ->
                "Multi-domain Anomaly: Convergent Evidence Across ${domains.size} Scientific Fields"
            domains.contains(ObservationType.ASTRONOMICAL.name) && domains.contains(ObservationType.GEOLOGICAL.name) ->
                "Astro-Geophysical Observation Pattern: Cross-Domain Co-occurrence Requiring Empirical Verification"
            domains.contains(ObservationType.BIOLOGICAL.name) && domains.contains(ObservationType.ATMOSPHERIC.name) ->
                "Bio-Climatic Pattern: Environmental Driver of Biological Anomaly"
            (topAnomaly?.anomalyScore ?: 0f) > 0.9f ->
                "Critical Anomaly: Unprecedented Deviation in ${topAnomaly?.let { "${it.observedValue} ${it.deviationPercent}" } ?: "Unknown Domain"}"
            else -> "Cross-Domain Observation Pattern: ${domains.joinToString(" + ")}"
        }
    }

    private fun calculateNovelty(observations: List<Observation>, anomalies: List<Anomaly>, hypotheses: List<Hypothesis>): Float {
        val anomalyNovelty = if (anomalies.isNotEmpty()) anomalies.maxOf { it.anomalyScore } else 0f
        val hypothesisNovelty = if (hypotheses.any { it.title.contains("unclassified", ignoreCase = true) }) 0.3f else 0f
        val uniqueSources = observations.map { it.sourceId }.distinct().size
        val sourceNovelty = (uniqueSources / 15f).coerceIn(0f, 0.3f)
        return ((anomalyNovelty * 0.5f) + hypothesisNovelty + sourceNovelty).coerceIn(0f, 1f)
    }

    private fun calculateCrossDomainBreadth(observations: List<Observation>): Float {
        val domains = observations.map { it.observationType }.distinct().size
        val sources = observations.map { it.sourceId }.distinct().size
        return ((domains / 6f) * 0.6f + (sources / 15f) * 0.4f).coerceIn(0f, 1f)
    }

    private fun calculateDataQuality(observations: List<Observation>): Float {
        if (observations.isEmpty()) return 0f
        val avgUncertainty = observations.map { it.uncertainty }.average().toFloat()
        val hasLocation = observations.count { it.location != null }.toFloat() / observations.size
        val hasRawData = observations.count { it.rawData.isNotBlank() }.toFloat() / observations.size
        return ((1f - avgUncertainty) * 0.4f + hasLocation * 0.3f + hasRawData * 0.3f).coerceIn(0f, 1f)
    }

    private fun calculateAnomalyImpact(anomalies: List<Anomaly>): Float {
        if (anomalies.isEmpty()) return 0f
        val maxScore = anomalies.maxOf { it.anomalyScore }
        val avgScore = anomalies.map { it.anomalyScore }.average().toFloat()
        val criticalCount = anomalies.count { it.severity == AnomalySeverity.CRITICAL }
        return ((maxScore * 0.5f) + (avgScore * 0.3f) + (criticalCount / 5f).coerceIn(0f, 0.2f)).coerceIn(0f, 1f)
    }

    private fun calculateCorrelationImpact(correlations: List<Correlation>): Float {
        if (correlations.isEmpty()) return 0f
        val maxStrength = correlations.maxOf { it.strength }
        val causalCount = correlations.count { it.correlationType == CorrelationType.CAUSAL }
        val crossDomainCount = correlations.count { it.domains.distinct().size > 1 }
        return ((maxStrength * 0.5f) + (causalCount / 3f).coerceIn(0f, 0.25f) + (crossDomainCount / 5f).coerceIn(0f, 0.25f)).coerceIn(0f, 1f)
    }

    private fun calculateReproducibility(observations: List<Observation>): Float {
        val publicSources = observations.count {
            it.sourceId in listOf("nasa", "usgs", "open_meteo", "pubmed", "arxiv", "gaia", "gbif", "worldbank", "jpl")
        }.toFloat() / observations.size.coerceAtLeast(1)
        val hasCoordinates = observations.count { it.location != null }.toFloat() / observations.size.coerceAtLeast(1)
        val hasTimestamps = observations.count { it.timestamp > 0 }.toFloat() / observations.size.coerceAtLeast(1)
        return (publicSources * 0.4f + hasCoordinates * 0.3f + hasTimestamps * 0.3f).coerceIn(0f, 1f)
    }

    private fun assessPatentPotential(
        observations: List<Observation>,
        anomalies: List<Anomaly>,
        correlations: List<Correlation>,
        noveltyScore: Float
    ): PatentPotential {
        val hasNovelMeasurement = anomalies.any { it.anomalyScore > 0.7f }
        val hasMethodInnovation = observations.map { it.sourceId }.distinct().size >= 3
        val hasUtility = correlations.any { it.strength > 0.6f && it.correlationType == CorrelationType.CAUSAL }

        val noveltyBonus = (if (hasNovelMeasurement) 0.2f else 0f) + (if (hasMethodInnovation) 0.2f else 0f)
        val novelty = (noveltyScore * 0.6f + noveltyBonus).coerceIn(0f, 1f)
        val utility = ((if (hasUtility) 0.7f else 0.3f) + (correlations.size / 10f)).coerceIn(0f, 1f)
        val nonObviousness = (novelty * 0.5f + utility * 0.3f + (if (hasMethodInnovation) 0.2f else 0f)).coerceIn(0f, 1f)

        val patentClass = when {
            observations.any { it.observationType == ObservationType.BIOLOGICAL } -> "C12N (Microorganisms/Enzymes)"
            observations.any { it.observationType == ObservationType.ASTRONOMICAL } -> "G01V (Geophysical/Space)"
            observations.any { it.observationType == ObservationType.GEOLOGICAL } -> "G01V (Geophysical)"
            observations.any { it.observationType == ObservationType.ATMOSPHERIC } -> "G01W (Meteorology)"
            else -> null
        }

        return PatentPotential(
            score = ((novelty + utility + nonObviousness) / 3f),
            noveltyScore = novelty,
            utilityScore = utility,
            nonObviousnessScore = nonObviousness,
            priorArtDensity = 1f - novelty, // inverse of novelty
            recommendedPatentClass = patentClass
        )
    }

    private fun assessPublicationPotential(
        observations: List<Observation>,
        anomalies: List<Anomaly>,
        correlations: List<Correlation>,
        overallScore: Float
    ): PublicationPotential {
        val domains = observations.map { it.observationType.name }.distinct().size
        val journalTier = when {
            overallScore > 0.85f && domains >= 4 -> "Nature / Science tier"
            overallScore > 0.70f && domains >= 3 -> "Nature Communications / PNAS tier"
            overallScore > 0.55f -> "Specialized high-impact journal"
            else -> "Specialized journal"
        }
        val citationPotential = (overallScore * 0.6f + (domains / 6f) * 0.2f + (correlations.size / 10f) * 0.2f).coerceIn(0f, 1f)
        val interdisciplinary = (domains / 6f).coerceIn(0f, 1f)
        val reproducibility = calculateReproducibility(observations)

        return PublicationPotential(
            score = overallScore,
            journalImpactEstimate = journalTier,
            citationPotential = citationPotential,
            interdisciplinaryAppeal = interdisciplinary,
            reproducibilityScore = reproducibility
        )
    }

    private fun calculateParadigmShiftScore(
        observations: List<Observation>,
        anomalies: List<Anomaly>,
        hypotheses: List<Hypothesis>,
        overallScore: Float
    ): Float {
        val hasContradictoryEvidence = hypotheses.any { it.contradictionCount > 0 }
        val hasUnclassifiedHypothesis = hypotheses.any { it.title.contains("unclassified", ignoreCase = true) && it.confidence > 40f }
        val hasMultiDomainAnomaly = anomalies.groupBy { observations.find { o -> o.id == it.observationId }?.observationType }.size >= 2
        val shiftIndicators = listOf(hasContradictoryEvidence, hasUnclassifiedHypothesis, hasMultiDomainAnomaly).count { it }
        return ((overallScore * 0.5f) + (shiftIndicators / 3f) * 0.5f).coerceIn(0f, 1f)
    }

    private fun generateRecommendedActions(
        overallScore: Float,
        patentPotential: PatentPotential,
        publicationPotential: PublicationPotential,
        observations: List<Observation>
    ): List<String> {
        val actions = mutableListOf<String>()
        if (overallScore > 0.80f) {
            actions.add("PRIORITY: File provisional patent application within 12 months")
            actions.add("Contact technology transfer office immediately")
        }
        if (patentPotential.score > 0.70f) {
            actions.add("Conduct comprehensive prior art search in ${patentPotential.recommendedPatentClass ?: "relevant IPC classes"}")
            actions.add("Document invention date with lab notebooks and timestamps")
        }
        if (publicationPotential.citationPotential > 0.75f) {
            actions.add("Target ${publicationPotential.journalImpactEstimate} for submission")
            actions.add("Prepare preprint for arXiv/bioRxiv to establish priority")
        }
        if (observations.size < 20) {
            actions.add("Collect additional independent observations to strengthen reproducibility")
        }
        if (observations.map { it.sourceId }.distinct().size < 3) {
            actions.add("Expand to additional data sources for cross-validation")
        }
        actions.add("Prepare data package for peer review (raw data + processing pipeline)")
        return actions.distinct()
    }

    private fun identifyRiskFactors(observations: List<Observation>, anomalies: List<Anomaly>, hypotheses: List<Hypothesis>): List<RiskFactor> {
        val risks = mutableListOf<RiskFactor>()
        if (observations.map { it.sourceId }.distinct().size < 3) {
            risks.add(RiskFactor("Limited source diversity — may be measurement artifact", 0.6f, true))
        }
        if (hypotheses.any { it.contradictionCount > it.evidenceCount }) {
            risks.add(RiskFactor("Contradictory evidence exceeds supporting evidence", 0.7f, true))
        }
        if (anomalies.any { it.possibleCauses.any { c -> c.description.contains("calibration", ignoreCase = true) && c.probability > 0.3f } }) {
            risks.add(RiskFactor("High probability of calibration error in anomalous measurements", 0.5f, true))
        }
        if (observations.count { it.location != null } < observations.size * 0.3f) {
            risks.add(RiskFactor("Most observations lack spatial coordinates — limits spatial correlation validity", 0.4f, false))
        }
        return risks
    }
}
