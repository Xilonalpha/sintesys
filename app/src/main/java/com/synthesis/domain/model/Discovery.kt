package com.synthesis.domain.model

import java.util.UUID

data class Discovery(
    val id: String = UUID.randomUUID().toString(),
    val investigationId: String,
    val title: String,
    val description: String,
    val discoveryType: DiscoveryType,
    val noveltyScore: Float, // 0-1, cât de nouă e descoperirea
    val confidenceScore: Float, // 0-1, cât de siguri suntem
    val impactScore: Float, // 0-1, impact științific
    val commercialScore: Float, // 0-1, potențial comercial
    val patentabilityScore: Float, // 0-1, șanse de brevet
    val estimatedValueUsd: Long, // valoare estimată în USD
    val supportingEvidence: List<EvidenceItem>,
    val domains: List<String>,
    val sourceIds: List<String>,
    val hiddenPattern: HiddenPattern,
    val recommendedActions: List<String>,
    val risks: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DiscoveryType {
    NOVEL_CORRELATION,      // Corelație necunoscută între fenomene
    CAUSAL_LINK,            // Lanț cauzal descoperit
    PREDICTIVE_PATTERN,     // Pattern predictiv
    ANOMALY_CLUSTER,        // Cluster de anomalii cu semnificație
    CROSS_DOMAIN_INSIGHT,   // Insight cross-domain unic
    EMERGENT_PROPERTY,      // Proprietate emergentă
    COMMERCIAL_OPPORTUNITY  // Oportunitate comercială directă
}

data class EvidenceItem(
    val source: String,
    val observationId: String,
    val description: String,
    val strength: Float
)

data class HiddenPattern(
    val patternType: PatternType,
    val description: String,
    val statisticalSignificance: Float, // p-value inverse (0-1)
    val recurrenceRate: Float, // cât de des apare pattern-ul
    val firstDetected: Long,
    val supportingDataPoints: Int
)

enum class PatternType {
    TEMPORAL_COINCIDENCE,   // Evenimente care coincid în timp
    SPATIAL_CLUSTER,        // Cluster spațial neașteptat
    STATISTICAL_OUTLIER,    // Outlier cu semnificație
    CAUSAL_CHAIN,           // Lanț cauzal
    PREDICTIVE_SIGNAL,      // Semnal predictiv
    STRUCTURAL_ANALOGY,     // Analogie structurală
    THRESHOLD_VIOLATION     // Violare de prag
}

data class PatentAssessment(
    val discoveryId: String,
    val noveltyScore: Float,
    val nonObviousnessScore: Float,
    val utilityScore: Float,
    val priorArtRisk: Float,
    val recommendedIpcClass: String?,
    val patentDraftSummary: String,
    val defensibilityScore: Float,
    val marketSizeEstimateUsd: Long,
    val timeToMarketMonths: Int
)

data class CommercialEstimate(
    val discoveryId: String,
    val totalAddressableMarketUsd: Long,
    val serviceableAddressableMarketUsd: Long,
    val serviceableObtainableMarketUsd: Long,
    val estimatedRevenueYear5Usd: Long,
    val licensingPotential: LicensingPotential,
    val industryApplications: List<IndustryApplication>,
    val competitiveLandscape: String,
    val barriersToEntry: List<String>
)

data class LicensingPotential(
    val score: Float,
    val potentialLicensees: List<String>,
    val estimatedLicenseValueUsd: Long,
    val exclusivityValue: Float
)

data class IndustryApplication(
    val industry: String,
    val applicationDescription: String,
    val marketSizeUsd: Long,
    val readinessLevel: Int // 1-9
)
