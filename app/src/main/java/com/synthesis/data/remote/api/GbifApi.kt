package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.GbifOccurrenceResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GbifApi {
    // GBIF Occurrence Search
    // Endpoint: GET /v1/occurrence/search
    // Base URL: https://api.gbif.org
    @GET("v1/occurrence/search")
    suspend fun searchOccurrences(
        @Query("q") query: String? = null,
        @Query("scientificName") scientificName: String? = null,
        @Query("decimalLatitude") latitude: Double? = null,
        @Query("decimalLongitude") longitude: Double? = null,
        @Query("radius") radius: Double? = null,
        @Query("year") year: String? = null,
        @Query("hasCoordinate") hasCoordinate: Boolean? = true,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): GbifOccurrenceResponse
}
