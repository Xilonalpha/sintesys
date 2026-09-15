package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// NOAA NWS Points Response
// Endpoint: GET /points/{latitude},{longitude}
// Base URL: https://api.weather.gov

data class NwsPointsResponse(
    @SerializedName("@context") val context: List<Any>?,
    @SerializedName("id") val id: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("geometry") val geometry: NwsGeometry?,
    @SerializedName("properties") val properties: NwsPointsProperties?
)

data class NwsGeometry(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val coordinates: List<Double>?
)

data class NwsPointsProperties(
    @SerializedName("@id") val id: String?,
    @SerializedName("@type") val type: String?,
    @SerializedName("cwa") val cwa: String?,
    @SerializedName("forecastOffice") val forecastOffice: String?,
    @SerializedName("gridId") val gridId: String?,
    @SerializedName("gridX") val gridX: Int?,
    @SerializedName("gridY") val gridY: Int?,
    @SerializedName("forecast") val forecast: String?,
    @SerializedName("forecastHourly") val forecastHourly: String?,
    @SerializedName("forecastGridData") val forecastGridData: String?,
    @SerializedName("observationStations") val observationStations: String?,
    @SerializedName("relativeLocation") val relativeLocation: NwsRelativeLocation?,
    @SerializedName("forecastZone") val forecastZone: String?,
    @SerializedName("county") val county: String?,
    @SerializedName("fireWeatherZone") val fireWeatherZone: String?,
    @SerializedName("timeZone") val timeZone: String?,
    @SerializedName("radarStation") val radarStation: String?
)

data class NwsRelativeLocation(
    @SerializedName("type") val type: String?,
    @SerializedName("geometry") val geometry: NwsGeometry?,
    @SerializedName("properties") val properties: NwsRelativeLocationProperties?
)

data class NwsRelativeLocationProperties(
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("distance") val distance: NwsDistance?,
    @SerializedName("bearing") val bearing: NwsBearing?
)

data class NwsDistance(
    @SerializedName("unitCode") val unitCode: String?,
    @SerializedName("value") val value: Double?
)

data class NwsBearing(
    @SerializedName("unitCode") val unitCode: String?,
    @SerializedName("value") val value: Int?
)

// NOAA NWS Forecast Response
// Endpoint: GET /gridpoints/{office}/{gridX},{gridY}/forecast
data class NwsForecastResponse(
    @SerializedName("@context") val context: List<Any>?,
    @SerializedName("type") val type: String?,
    @SerializedName("geometry") val geometry: NwsGeometry?,
    @SerializedName("properties") val properties: NwsForecastProperties?
)

data class NwsForecastProperties(
    @SerializedName("updated") val updated: String?,
    @SerializedName("units") val units: String?,
    @SerializedName("forecastGenerator") val forecastGenerator: String?,
    @SerializedName("generatedAt") val generatedAt: String?,
    @SerializedName("updateTime") val updateTime: String?,
    @SerializedName("validTimes") val validTimes: String?,
    @SerializedName("elevation") val elevation: NwsElevation?,
    @SerializedName("periods") val periods: List<NwsForecastPeriod>?
)

data class NwsElevation(
    @SerializedName("unitCode") val unitCode: String?,
    @SerializedName("value") val value: Double?
)

data class NwsForecastPeriod(
    @SerializedName("number") val number: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("isDaytime") val isDaytime: Boolean?,
    @SerializedName("temperature") val temperature: Int?,
    @SerializedName("temperatureUnit") val temperatureUnit: String?,
    @SerializedName("temperatureTrend") val temperatureTrend: String?,
    @SerializedName("probabilityOfPrecipitation") val probabilityOfPrecipitation: NwsProbability?,
    @SerializedName("windSpeed") val windSpeed: String?,
    @SerializedName("windDirection") val windDirection: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("shortForecast") val shortForecast: String?,
    @SerializedName("detailedForecast") val detailedForecast: String?
)

data class NwsProbability(
    @SerializedName("unitCode") val unitCode: String?,
    @SerializedName("value") val value: Int?
)
