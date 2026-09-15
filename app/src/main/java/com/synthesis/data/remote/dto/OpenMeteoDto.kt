package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// Open-Meteo Forecast Response
// Endpoint: GET /v1/forecast?latitude={}&longitude={}&current=temperature_2m,...&hourly=...
// Base URL: https://api.open-meteo.com

data class OpenMeteoForecastResponse(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("generationtime_ms") val generationTimeMs: Double?,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String?,
    @SerializedName("elevation") val elevation: Double?,
    @SerializedName("current") val current: OpenMeteoCurrent?,
    @SerializedName("hourly") val hourly: OpenMeteoHourly?,
    @SerializedName("daily") val daily: OpenMeteoDaily?
)

data class OpenMeteoCurrent(
    @SerializedName("time") val time: String?,
    @SerializedName("interval") val interval: Int?,
    @SerializedName("temperature_2m") val temperature2m: Double?,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: Int?,
    @SerializedName("pressure_msl") val pressureMsl: Double?,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double?,
    @SerializedName("wind_direction_10m") val windDirection10m: Int?
)

data class OpenMeteoHourly(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("temperature_2m") val temperature2m: List<Double>?,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: List<Int>?,
    @SerializedName("pressure_msl") val pressureMsl: List<Double>?,
    @SerializedName("wind_speed_10m") val windSpeed10m: List<Double>?,
    @SerializedName("precipitation") val precipitation: List<Double>?
)

data class OpenMeteoDaily(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>?,
    @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>?,
    @SerializedName("precipitation_sum") val precipitationSum: List<Double>?,
    @SerializedName("wind_speed_10m_max") val windSpeed10mMax: List<Double>?
)
