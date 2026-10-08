package com.synthesis.data.local.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synthesis.data.local.entity.DiscoveryEntity
import com.synthesis.domain.model.Discovery
import com.synthesis.domain.model.DiscoveryType
import com.synthesis.domain.model.EvidenceItem
import com.synthesis.domain.model.HiddenPattern

private val discoveryGson = Gson()

fun DiscoveryEntity.toDomain(): Discovery {
    val evidenceType = object : TypeToken<List<EvidenceItem>>() {}.type
    val stringListType = object : TypeToken<List<String>>() {}.type

    val evidence: List<EvidenceItem> = try {
        discoveryGson.fromJson(supportingEvidenceJson, evidenceType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val domainsList: List<String> = try {
        discoveryGson.fromJson(domainsJson, stringListType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val sourceIdsList: List<String> = try {
        discoveryGson.fromJson(sourceIdsJson, stringListType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val pattern: HiddenPattern = try {
        discoveryGson.fromJson(hiddenPatternJson, HiddenPattern::class.java)
            ?: HiddenPattern(
                patternType = com.synthesis.domain.model.PatternType.STATISTICAL_OUTLIER,
                description = description,
                statisticalSignificance = confidenceScore,
                recurrenceRate = 0.5f,
                firstDetected = createdAt,
                supportingDataPoints = evidence.size
            )
    } catch (_: Exception) {
        HiddenPattern(
            patternType = com.synthesis.domain.model.PatternType.STATISTICAL_OUTLIER,
            description = description,
            statisticalSignificance = confidenceScore,
            recurrenceRate = 0.5f,
            firstDetected = createdAt,
            supportingDataPoints = evidence.size
        )
    }

    val actionsList: List<String> = try {
        discoveryGson.fromJson(recommendedActionsJson, stringListType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val risksList: List<String> = try {
        discoveryGson.fromJson(risksJson, stringListType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val type = try {
        DiscoveryType.valueOf(discoveryType)
    } catch (_: Exception) {
        DiscoveryType.NOVEL_CORRELATION
    }

    return Discovery(
        id = id,
        investigationId = investigationId,
        title = title,
        description = description,
        discoveryType = type,
        noveltyScore = noveltyScore,
        confidenceScore = confidenceScore,
        impactScore = impactScore,
        commercialScore = commercialScore,
        patentabilityScore = patentabilityScore,
        estimatedValueUsd = estimatedValueUsd,
        supportingEvidence = evidence,
        domains = domainsList,
        sourceIds = sourceIdsList,
        hiddenPattern = pattern,
        recommendedActions = actionsList,
        risks = risksList,
        createdAt = createdAt
    )
}

fun Discovery.toEntity(): DiscoveryEntity {
    return DiscoveryEntity(
        id = id,
        investigationId = investigationId,
        title = title,
        description = description,
        discoveryType = discoveryType.name,
        noveltyScore = noveltyScore,
        confidenceScore = confidenceScore,
        impactScore = impactScore,
        commercialScore = commercialScore,
        patentabilityScore = patentabilityScore,
        estimatedValueUsd = estimatedValueUsd,
        supportingEvidenceJson = discoveryGson.toJson(supportingEvidence),
        domainsJson = discoveryGson.toJson(domains),
        sourceIdsJson = discoveryGson.toJson(sourceIds),
        hiddenPatternJson = discoveryGson.toJson(hiddenPattern),
        recommendedActionsJson = discoveryGson.toJson(recommendedActions),
        risksJson = discoveryGson.toJson(risks),
        createdAt = createdAt
    )
}
