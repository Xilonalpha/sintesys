package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.UsgsEarthquakeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface UsgsApi {
    // Earthquake query - real GeoJSON feed
    // Real endpoint: https://earthquake.usgs.gov/fdsnws/event/1/query
    @GET("fdsnws/event/1/query")
    suspend fun getEarthquakes(
        @Query("format") format: String = "geojson",
        @Query("starttime") startTime: String? = null,
        @Query("endtime") endTime: String? = null,
        @Query("minmagnitude") minMagnitude: Double? = null,
        @Query("maxmagnitude") maxMagnitude: Double? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("maxradiuskm") maxRadiusKm: Double? = null,
        @Query("limit") limit: Int = 100,
        @Query("orderby") orderBy: String = "time"
    ): UsgsEarthquakeResponse
}
