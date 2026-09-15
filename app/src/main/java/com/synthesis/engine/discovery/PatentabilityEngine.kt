package com.synthesis.engine.discovery

import com.synthesis.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Evaluates whether a detected pattern / discovery has realistic patent potential:
 * novelty, non-obviousness, utility, and prior-art risk, then produces a
 * PatentAssessment with a recommended IPC class and a draft summary.
 */
@Singleton
class PatentabilityEngine @Inject constructor() {

    private val ipcClassByDomainPair: List<Triple<Set<String>, String, String>> = listOf(
        Triple(setOf("ASTRONOMICAL", "ATMOSPHERIC"), "G01W", "Meteorology / astronomical monitoring"),
        Triple(setOf("GEOLOGICAL", "SATELLITE"), "G01V", "Geophysics / remote geological sensing"),
        Triple(setOf("BIOLOGICAL", "ATMOSPHERIC"), "A01G", "Biological response to environmental conditions"),
        Triple(setOf("GEOLOGICAL", "ATMOSPHERIC"), "G01N", "Investigating / analysing materials by physical means"),
        Triple(setOf("SATELLITE", "BIOLOGICAL"), "G06N", "Data-driven biological pattern recognition"),
        Triple(setOf("ASTRONOMICAL", "GEOLOGICAL"), "G01C", "Measuring distances, levels or bearings"),
        Triple(setOf("HISTORICAL", "GEOLOGICAL"), "G06Q", "Data processing systems for prediction / forecasting")
    )

    fun assess(discovery: Discovery, pattern: PatternDetector.DetectedPattern): PatentAssessment {
        val noveltyScore = computeNovelty(pattern)
        val nonObviousnessScore = computeNonObviousness(pattern, discovery.domains)
        val utilityScore = computeUtility(discovery)
        val priorArtRisk = computePriorArtRisk(pattern, discovery)
        val defensibility = ((noveltyScore * 0.4f) + (nonObviousnessScore * 0.35f) + ((1f - priorArtRisk) * 0.25f))
            .coerceIn(0f, 1f)

        val ipcClass = recommendIpcClass(discovery.domains)
        val marketSize = estimateMarketSizeUsd(discovery, utilityScore)
        val timeToMarket = estimateTimeToMarketMonths(discovery)

        return PatentAssessment(
            discoveryId = discovery.id,
            noveltyScore = noveltyScore,
            nonObviousnessScore = nonObviousnessScore,
            utilityScore = utilityScore,
            priorArtRisk = priorArtRisk,
            recommendedIpcClass = ipcClass,
            patentDraftSummary = buildDraftSummary(discovery, pattern, ipcClass),
            defensibilityScore = defensibility,
            marketSizeEstimateUsd = marketSize,
            timeToMarketMonths = timeToMarket
        )
    }

    // Cât de rar / neobișnuit e pattern-ul (statistica din PatternDetector + domenii implicate)
    private fun computeNovelty(pattern: PatternDetector.DetectedPattern): Float {
        val domainBonus = min(pattern.domains.size, 4) / 4f
        return (pattern.significance * 0.7f + domainBonus * 0.3f).coerceIn(0f, 1f)
    }

    // Cât de puțin evident e ca cineva să fi combinat aceste domenii înainte
    private fun computeNonObviousness(pattern: PatternDetector.DetectedPattern, domains: List<String>): Float {
        val crossDomainFactor = if (domains.distinct().size >= 3) 0.9f else if (domains.distinct().size == 2) 0.65f else 0.3f
        val typeFactor = when (pattern.patternType) {
            PatternType.CAUSAL_CHAIN, PatternType.STRUCTURAL_ANALOGY -> 0.9f
            PatternType.STATISTICAL_OUTLIER, PatternType.PREDICTIVE_SIGNAL -> 0.7f
            else -> 0.5f
        }
        return ((crossDomainFactor + typeFactor) / 2f).coerceIn(0f, 1f)
    }

    // Aplicabilitate practică — un pattern e util dacă poate fi transformat într-un semnal de alertă/predicție
    private fun computeUtility(discovery: Discovery): Float {
        val actionBonus = min(discovery.recommendedActions.size, 3) / 3f
        return (discovery.impactScore * 0.6f + actionBonus * 0.4f).coerceIn(0f, 1f)
    }

    // Risc ca ceva similar să existe deja publicat — aproximat din numărul de surse independente implicate
    // (mai multe surse independente confirmând același pattern = mai puțin probabil să fie deja brevetat izolat,
    // dar și mai vizibil, deci risc moderat)
    private fun computePriorArtRisk(pattern: PatternDetector.DetectedPattern, discovery: Discovery): Float {
        val sourceSpread = min(pattern.involvedSources.size, 5) / 5f
        val obscurityBonus = 1f - (discovery.confidenceScore * 0.3f)
        return (sourceSpread * 0.5f + obscurityBonus * 0.5f).coerceIn(0.1f, 0.9f)
    }

    private fun recommendIpcClass(domains: List<String>): String? {
        val domainSet = domains.map { it.uppercase() }.toSet()
        return ipcClassByDomainPair.firstOrNull { it.first.any { d -> d in domainSet } }?.second
    }

    private fun estimateMarketSizeUsd(discovery: Discovery, utilityScore: Float): Long {
        val base = 5_000_000L
        val multiplier = 1 + (discovery.domains.distinct().size * 4)
        return (base * multiplier * (0.5 + utilityScore)).toLong()
    }

    private fun estimateTimeToMarketMonths(discovery: Discovery): Int {
        val complexity = discovery.domains.distinct().size
        return (12 + complexity * 6).coerceIn(12, 48)
    }

    private fun buildDraftSummary(discovery: Discovery, pattern: PatternDetector.DetectedPattern, ipcClass: String?): String {
        return "Provisional patent scope: a method and system for detecting and predicting " +
                "${pattern.patternType.name.lowercase().replace('_', ' ')} phenomena across the domains " +
                "${discovery.domains.distinct().joinToString(", ")}, based on cross-referencing independent " +
                "public data sources (${pattern.involvedSources.joinToString(", ")}). " +
                "Suggested IPC classification: ${ipcClass ?: "unclassified — requires manual review"}. " +
                "Core claim candidate: a computer-implemented method for identifying statistically significant " +
                "correlations between heterogeneous observation streams and generating an early-warning or " +
                "commercial signal therefrom."
    }
}
