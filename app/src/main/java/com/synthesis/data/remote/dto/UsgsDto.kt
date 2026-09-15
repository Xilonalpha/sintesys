package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// USGS Earthquake GeoJSON Response
// Endpoint: GET /fdsnws/event/1/query?format=geojson&starttime={}&endtime={}&minmagnitude={}
// Base URL: https://earthquake.usgs.gov

data class UsgsEarthquakeResponse(
    @SerializedName("type") val type: String?,
    @SerializedName("metadata") val metadata: UsgsMetadata?,
    @SerializedName("features") val features: List<UsgsFeature>?,
    @SerializedName("bbox") val bbox: List<Double>?
)

data class UsgsMetadata(
    @SerializedName("generated") val generated: Long?,
    @SerializedName("url") val url: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("status") val status: Int?,
    @SerializedName("api") val api: String?,
    @SerializedName("count") val count: Int?
)

data class UsgsFeature(
    @SerializedName("type") val type: String?,
    @SerializedName("properties") val properties: UsgsProperties?,
    @SerializedName("geometry") val geometry: UsgsGeometry?,
    @SerializedName("id") val id: String?
)

data class UsgsProperties(
    @SerializedName("mag") val mag: Double?,
    @SerializedName("place") val place: String?,
    @SerializedName("time") val time: Long?,
    @SerializedName("updated") val updated: Long?,
    @SerializedName("tz") val tz: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("detail") val detail: String?,
    @SerializedName("felt") val felt: Int?,
    @SerializedName("cdi") val cdi: Double?,
    @SerializedName("mmi") val mmi: Double?,
    @SerializedName("alert") val alert: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("tsunami") val tsunami: Int?,
    @SerializedName("sig") val sig: Int?,
    @SerializedName("net") val net: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("ids") val ids: String?,
    @SerializedName("sources") val sources: String?,
    @SerializedName("types") val types: String?,
    @SerializedName("nst") val nst: Int?,
    @SerializedName("dmin") val dmin: Double?,
    @SerializedName("rms") val rms: Double?,
    @SerializedName("gap") val gap: Double?,
    @SerializedName("magType") val magType: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?
)

data class UsgsGeometry(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val coordinates: List<Double>?
)
