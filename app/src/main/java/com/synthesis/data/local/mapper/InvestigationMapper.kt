package com.synthesis.data.local.mapper

import com.synthesis.data.local.entity.InvestigationEntity
import com.synthesis.data.local.entity.ObservationEntity
import com.synthesis.data.local.entity.AnomalyEntity
import com.synthesis.data.local.entity.HypothesisEntity
import com.synthesis.data.local.entity.EvidenceNodeEntity
import com.synthesis.domain.model.*

// Investigation
fun InvestigationEntity.toDomain(): Investigation = Investigation(
    id = id,
    title = title,
    query = query,
    status = InvestigationStatus.valueOf(status),
    sourcesConsulted = sourcesConsulted,
    anomaliesFound = anomaliesFound,
    confidenceScore = confidenceScore,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Investigation.toEntity(): InvestigationEntity = InvestigationEntity(
    id = id,
    title = title,
    query = query,
    status = status.name,
    sourcesConsulted = sourcesConsulted,
    anomaliesFound = anomaliesFound,
    confidenceScore = confidenceScore,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// Observation
fun ObservationEntity.toDomain(): Observation = Observation(
    id = id,
    investigationId = investigationId,
    sourceId = sourceId,
    sourceName = sourceName,
    entity = entity,
    timestamp = timestamp,
    location = if (latitude != null && longitude != null) GeoLocation(latitude = latitude, longitude = longitude, altitude = altitude) else null,
    measurement = Measurement(
        value = value,
        unit = unit,
        expectedValue = expectedValue,
        deviation = deviation
    ),
    rawData = rawData,
    normalizedData = normalizedData,
    uncertainty = uncertainty,
    observationType = ObservationType.valueOf(observationType)
)

fun Observation.toEntity(): ObservationEntity = ObservationEntity(
    id = id,
    investigationId = investigationId,
    sourceId = sourceId,
    sourceName = sourceName,
    entity = entity,
    timestamp = timestamp,
    latitude = location?.latitude,
    longitude = location?.longitude,
    altitude = location?.altitude,
    value = measurement.value,
    unit = measurement.unit,
    expectedValue = measurement.expectedValue,
    deviation = measurement.deviation,
    rawData = rawData,
    normalizedData = normalizedData,
    uncertainty = uncertainty,
    observationType = observationType.name
)

// Anomaly
private val gson = com.google.gson.Gson()
private val possibleCauseType = object : com.google.gson.reflect.TypeToken<List<PossibleCause>>() {}.type
private val discriminatingTestType = object : com.google.gson.reflect.TypeToken<List<DiscriminatingTest>>() {}.type

fun AnomalyEntity.toDomain(): Anomaly = Anomaly(
    id = id,
    investigationId = investigationId,
    observationId = observationId,
    expectedValue = expectedValue,
    observedValue = observedValue,
    deviation = deviation,
    deviationPercent = deviationPercent,
    anomalyScore = anomalyScore,
    possibleCauses = runCatching { gson.fromJson<List<PossibleCause>>(possibleCauses, possibleCauseType) }.getOrNull() ?: emptyList(),
    severity = runCatching { AnomalySeverity.valueOf(severity) }.getOrDefault(AnomalySeverity.MEDIUM)
)

fun Anomaly.toEntity(): AnomalyEntity = AnomalyEntity(
    id = id,
    investigationId = investigationId,
    observationId = observationId,
    expectedValue = expectedValue,
    observedValue = observedValue,
    deviation = deviation,
    deviationPercent = deviationPercent,
    anomalyScore = anomalyScore,
    possibleCauses = gson.toJson(possibleCauses),
    severity = severity.name
)

// Hypothesis
fun HypothesisEntity.toDomain(): Hypothesis = Hypothesis(
    id = id,
    investigationId = investigationId,
    title = title,
    description = description,
    confidence = confidence,
    evidenceCount = evidenceCount,
    contradictionCount = contradictionCount,
    discriminatingTests = runCatching { gson.fromJson<List<DiscriminatingTest>>(discriminatingTests, discriminatingTestType) }.getOrNull() ?: emptyList(),
    status = runCatching { HypothesisStatus.valueOf(status) }.getOrDefault(HypothesisStatus.PENDING)
)

fun Hypothesis.toEntity(): HypothesisEntity = HypothesisEntity(
    id = id,
    investigationId = investigationId,
    title = title,
    description = description,
    confidence = confidence,
    evidenceCount = evidenceCount,
    contradictionCount = contradictionCount,
    discriminatingTests = gson.toJson(discriminatingTests),
    status = status.name
)

// EvidenceNode
fun EvidenceNodeEntity.toDomain(): EvidenceNode = EvidenceNode(
    id = id,
    investigationId = investigationId,
    label = label,
    type = NodeType.valueOf(type),
    source = source,
    relationship = RelationshipType.valueOf(relationship),
    description = description,
    timestamp = timestamp,
    x = x,
    y = y
)

fun EvidenceNode.toEntity(): EvidenceNodeEntity = EvidenceNodeEntity(
    id = id,
    investigationId = investigationId,
    label = label,
    type = type.name,
    source = source,
    relationship = relationship.name,
    description = description,
    timestamp = timestamp,
    x = x,
    y = y
)
