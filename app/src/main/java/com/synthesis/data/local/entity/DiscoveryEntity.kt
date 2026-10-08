package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing synthesized discoveries and cross-domain findings
 * to persist research data locally.
 */
@Entity(tableName = "discoveries")
data class DiscoveryEntity(
    @PrimaryKey
    val id: String,
    val investigationId: String,
    val title: String,
    val description: String,
    val discoveryType: String,
    val noveltyScore: Float,
    val confidenceScore: Float,
    val impactScore: Float,
    val commercialScore: Float,
    val patentabilityScore: Float,
    val estimatedValueUsd: Long,
    val supportingEvidenceJson: String,
    val domainsJson: String,
    val sourceIdsJson: String,
    val hiddenPatternJson: String,
    val recommendedActionsJson: String,
    val risksJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
