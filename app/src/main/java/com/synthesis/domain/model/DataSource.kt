package com.synthesis.domain.model

data class DataSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val status: SourceStatus,
    val lastUpdate: Long,
    val apiLatency: Int,
    val dataQuality: Float,
    val coverage: String,
    val license: String,
    val endpoint: String
)

enum class SourceType {
    NASA, ESA, NOAA, CERN, COPERNICUS, PUBCHEM, PUBMED, ARXIV, USGS, OPEN_METEO,
    ASTRONOMICAL_CATALOG, SATELLITE_CATALOG, ATMOSPHERIC, GEOLOGICAL, BIOLOGICAL
}

enum class SourceStatus {
    ONLINE, OFFLINE, DEGRADED, MAINTENANCE
}
