package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// World Bank Indicator Response
// Endpoint: GET https://api.worldbank.org/v2/country/all/indicator/{indicator_code}?format=json&per_page=100
// Response is an array: [metadata, [data]]

data class WorldBankIndicatorResponse(
    @SerializedName("page") val page: Int?,
    @SerializedName("pages") val pages: Int?,
    @SerializedName("per_page") val perPage: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("sourceid") val sourceId: String?,
    @SerializedName("lastupdated") val lastUpdated: String?
)

data class WorldBankIndicatorData(
    @SerializedName("indicator") val indicator: WorldBankIndicatorInfo?,
    @SerializedName("country") val country: WorldBankCountryInfo?,
    @SerializedName("countryiso3code") val countryIso3Code: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("value") val value: Double?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("obs_status") val obsStatus: String?,
    @SerializedName("decimal") val decimal: Int?
)

data class WorldBankIndicatorInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("value") val value: String?
)

data class WorldBankCountryInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("value") val value: String?
)
