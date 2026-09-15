package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.GaiaConeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GaiaApi {
    // Gaia DR3 Cone Search (Gaia@AIP)
    // Endpoint: GET /conesearch/api/gaiadr3.gaia_source/
    // Base URL: https://gaia.aip.de
    @GET("conesearch/api/gaiadr3.gaia_source/")
    suspend fun coneSearch(
        @Query("RA") ra: Double,
        @Query("DEC") dec: Double,
        @Query("SR") sr: Double = 0.1, // search radius in degrees
        @Query("MAXREC") maxRec: Int = 100
    ): GaiaConeSearchResponse
}
