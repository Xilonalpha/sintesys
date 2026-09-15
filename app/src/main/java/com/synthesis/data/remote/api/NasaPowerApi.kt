package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.NasaPowerDailyResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NasaPowerApi {
    // Daily Point Data - climate/solar data for a specific location
    // Real endpoint: https://power.larc.nasa.gov/api/temporal/daily/point
    @GET("api/temporal/daily/point")
    suspend fun getDailyPointData(
        @Query("parameters") parameters: String = "T2M,PS,WS10M,PRECTOT",
        @Query("community") community: String = "RE",
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("format") format: String = "JSON"
    ): NasaPowerDailyResponse
}
