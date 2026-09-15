package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// NASA POWER Daily Point Response
// Endpoint: GET /api/temporal/daily/point?parameters={}&community={}&longitude={}&latitude={}&start={}&end={}&format=JSON
// Base URL: https://power.larc.nasa.gov

data class NasaPowerDailyResponse(
    @SerializedName("type") val type: String?,
    @SerializedName("geometry") val geometry: NasaPowerGeometry?,
    @SerializedName("properties") val properties: NasaPowerProperties?,
    @SerializedName("header") val header: NasaPowerHeader?,
    @SerializedName("messages") val messages: List<String>?,
    @SerializedName("parameters") val parameters: Map<String, NasaPowerParameter>?,
    @SerializedName("times") val times: NasaPowerTimes?
)

data class NasaPowerGeometry(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val coordinates: List<Double>?
)

data class NasaPowerProperties(
    @SerializedName("parameter") val parameter: Map<String, Map<String, Double>>?
)

data class NasaPowerHeader(
    @SerializedName("title") val title: String?,
    @SerializedName("api_version") val apiVersion: String?,
    @SerializedName("start") val start: String?,
    @SerializedName("end") val end: String?
)

data class NasaPowerParameter(
    @SerializedName("units") val units: String?,
    @SerializedName("longname") val longname: String?
)

data class NasaPowerTimes(
    @SerializedName("start") val start: String?,
    @SerializedName("end") val end: String?
)
