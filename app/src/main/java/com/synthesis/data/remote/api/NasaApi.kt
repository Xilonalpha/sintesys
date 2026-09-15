package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.NasaApodResponse
import com.synthesis.data.remote.dto.NasaNeoFeedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NasaApi {
    // NEO Feed - asteroids approaching Earth in a date range
    // Real endpoint: https://api.nasa.gov/neo/rest/v1/feed
    @GET("neo/rest/v1/feed")
    suspend fun getNeoFeed(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): NasaNeoFeedResponse

    // NEO Lookup - specific asteroid by ID
    // Real endpoint: https://api.nasa.gov/neo/rest/v1/neo/{asteroid_id}
    @GET("neo/rest/v1/neo/{asteroid_id}")
    suspend fun getNeoLookup(
        @Path("asteroid_id") asteroidId: String,
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): com.synthesis.data.remote.dto.NasaNeoObject

    // APOD - Astronomy Picture of the Day
    // Real endpoint: https://api.nasa.gov/planetary/apod
    @GET("planetary/apod")
    suspend fun getApod(
        @Query("date") date: String? = null,
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): NasaApodResponse
}
