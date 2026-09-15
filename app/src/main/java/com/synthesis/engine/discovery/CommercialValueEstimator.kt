package com.synthesis.engine.discovery

import com.synthesis.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Estimates the commercial value of a Discovery: market sizing (TAM/SAM/SOM),
 * licensing potential, and plausible industry applications, derived from the
 * domains involved and the discovery's impact/patentability scores.
 */
@Singleton
class CommercialValueEstimator @Inject constructor() {

    private data class IndustryProfile(
        val industry: String,
        val domains: Set<String>,
        val baseMarketUsd: Long,
        val readiness: Int
    )

    private val industryProfiles = listOf(
        IndustryProfile("Climate risk & insurance", setOf("ATMOSPHERIC", "SATELLITE", "GEOLOGICAL"), 40_000_000_000L, 6),
        IndustryProfile("Disaster early-warning systems", setOf("GEOLOGICAL", "ATMOSPHERIC", "SATELLITE"), 15_000_000_000L, 5),
        IndustryProfile("Precision agriculture", setOf("BIOLOGICAL", "ATMOSPHERIC"), 12_000_000_000L, 6),
        IndustryProfile("Space situational awareness", setOf("ASTRONOMICAL", "SATELLITE"), 8_000_000_000L, 4),
        IndustryProfile("Public health surveillance", setOf("BIOLOGICAL", "HISTORICAL"), 6_000_000_000L, 5),
        IndustryProfile("Financial/commodity risk modeling", setOf("GEOLOGICAL", "ATMOSPHERIC", "HISTORICAL"), 20_000_000_000L, 4)
    )

    fun estimate(discovery: Discovery, patentAssessment: PatentAssessment): CommercialEstimate {
        val applications = matchIndustries(discovery)
        val tam = applications.sumOf { it.marketSizeUsd.toDouble() }.toLong().let { if (it > 0) it else 1_000_000_000L }
        val sam = (tam * (0.05 + discovery.commercialScore * 0.15)).toLong()
        val som = (sam * (0.02 + discovery.confidenceScore * 0.08)).toLong()
        val year5Revenue = (som * (0.15 + discovery.impactScore * 0.35)).toLong()

        val licensing = LicensingPotential(
            score = ((discovery.patentabilityScore * 0.5f) + (discovery.commercialScore * 0.5f)).coerceIn(0f, 1f),
            potentialLicensees = applications.map { it.industry }.distinct().take(4),
            estimatedLicenseValueUsd = (sam * 0.01).toLong().coerceAtLeast(50_000L),
            exclusivityValue = (patentAssessment.defensibilityScore * 0.8f).coerceIn(0f, 1f)
        )

        return CommercialEstimate(
            discoveryId = discovery.id,
            totalAddressableMarketUsd = tam,
            serviceableAddressableMarketUsd = sam,
            serviceableObtainableMarketUsd = som,
            estimatedRevenueYear5Usd = year5Revenue,
            licensingPotential = licensing,
            industryApplications = applications,
            competitiveLandscape = buildCompetitiveLandscape(discovery, applications),
            barriersToEntry = buildBarriers(discovery, patentAssessment)
        )
    }

    private fun matchIndustries(discovery: Discovery): List<IndustryApplication> {
        val domainSet = discovery.domains.map { it.uppercase() }.toSet()
        val matched = industryProfiles.filter { it.domains.any { d -> d in domainSet } }
        val pool = if (matched.isEmpty()) industryProfiles.take(2) else matched

        return pool.map { profile ->
            val overlap = min(profile.domains.count { it in domainSet }, profile.domains.size)
            val relevance = (overlap.toFloat() / profile.domains.size).coerceIn(0.3f, 1f)
            IndustryApplication(
                industry = profile.industry,
                applicationDescription = "Applying the detected ${discovery.discoveryType.name.lowercase().replace('_', ' ')} " +
                        "pattern to ${profile.industry.lowercase()} risk models and predictive alerting.",
                marketSizeUsd = (profile.baseMarketUsd * relevance).toLong(),
                readinessLevel = profile.readiness
            )
        }
    }

    private fun buildCompetitiveLandscape(discovery: Discovery, applications: List<IndustryApplication>): String {
        val industries = applications.joinToString(", ") { it.industry }
        return "No direct competitor currently combines the source domains " +
                "(${discovery.domains.distinct().joinToString(", ")}) into a single predictive signal. " +
                "Closest adjacent players operate in: $industries, but typically within a single domain rather than cross-domain fusion."
    }

    private fun buildBarriers(discovery: Discovery, patentAssessment: PatentAssessment): List<String> {
        val barriers = mutableListOf(
            "Requires sustained access to multiple independent public data sources",
            "Model/pattern needs continuous validation against new incoming data to avoid false positives"
        )
        if (patentAssessment.priorArtRisk > 0.5f) {
            barriers.add("Moderate prior-art risk — a freedom-to-operate search is recommended before filing")
        }
        if (discovery.confidenceScore < 0.6f) {
            barriers.add("Confidence score is moderate — additional corroborating observations recommended before commercialization")
        }
        return barriers
    }
}
