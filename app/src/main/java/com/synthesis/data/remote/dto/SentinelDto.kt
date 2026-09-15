package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// Sentinel Hub Catalog API Response
// Endpoint: POST https://services.sentinel-hub.com/api/v1/catalog/search
// Body: {"bbox": [...], "datetime": "...", "collections": ["sentinel-2-l2a"], "limit": 50}

data class SentinelCatalogRequest(
    @SerializedName("bbox") val bbox: List<Double>?,
    @SerializedName("datetime") val datetime: String?,
    @SerializedName("collections") val collections: List<String>?,
    @SerializedName("limit") val limit: Int = 50
)

data class SentinelCatalogResponse(
    @SerializedName("type") val type: String?,
    @SerializedName("features") val features: List<SentinelFeature>?,
    @SerializedName("context") val context: SentinelContext?
)

data class SentinelFeature(
    @SerializedName("id") val id: String?,
    @SerializedName("bbox") val bbox: List<Double>?,
    @SerializedName("geometry") val geometry: SentinelGeometry?,
    @SerializedName("properties") val properties: SentinelProperties?
)

data class SentinelGeometry(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val coordinates: List<List<List<Double>>>?
)

data class SentinelProperties(
    @SerializedName("datetime") val datetime: String?,
    @SerializedName("eo:cloud_cover") val cloudCover: Double?,
    @SerializedName("sara:resolution") val resolution: Double?,
    @SerializedName("sar:polarizations") val polarizations: List<String>?,
    @SerializedName("sar:instrument_mode") val instrumentMode: String?,
    @SerializedName("collection") val collection: String?,
    @SerializedName("proj:epsg") val epsg: Int?
)

data class SentinelContext(
    @SerializedName("limit") val limit: Int?,
    @SerializedName("returned") val returned: Int?
)
