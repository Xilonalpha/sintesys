package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.NwsForecastResponse
import com.synthesis.data.remote.dto.NwsPointsResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

interface NoaaApi {
    // Points - get grid metadata for a lat/lon
    // Real endpoint: https://api.weather.gov/points/{lat},{lon}
    @GET("points/{latitude},{longitude}")
    @Headers("User-Agent: Synthesis-App/1.0")
    suspend fun getPoints(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): NwsPointsResponse

    // Forecast - get forecast from gridpoint URL (obtained from points response)
    @GET
    @Headers("User-Agent: Synthesis-App/1.0")
    suspend fun getForecast(@Url forecastUrl: String): NwsForecastResponse
}
