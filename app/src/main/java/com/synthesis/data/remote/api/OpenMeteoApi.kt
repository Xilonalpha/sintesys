package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.OpenMeteoForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    // Forecast - weather data for any location, no API key required
    // Real endpoint: https://api.open-meteo.com/v1/forecast
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,pressure_msl,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,pressure_msl,wind_speed_10m,precipitation",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoForecastResponse

    // Historical data - weather data from 1940 onwards
    @GET("v1/forecast")
    suspend fun getHistoricalData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponse
}
