package com.synthesis.domain.model

data class Observation(
    val id: String,
    val investigationId: String,
    val sourceId: String,
    val sourceName: String,
    val entity: String,
    val timestamp: Long,
    val location: GeoLocation?,
    val measurement: Measurement,
    val rawData: String,
    val normalizedData: String,
    val uncertainty: Float,
    val observationType: ObservationType
)

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null
)

data class Measurement(
    val value: Double,
    val unit: String,
    val expectedValue: Double? = null,
    val deviation: Double? = null
)

enum class ObservationType {
    ASTRONOMICAL, ATMOSPHERIC, GEOLOGICAL, BIOLOGICAL, SATELLITE, HISTORICAL
}
