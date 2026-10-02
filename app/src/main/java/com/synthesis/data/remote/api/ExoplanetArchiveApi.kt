package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

interface ExoplanetArchiveApi {

    // 1. TESS Objects of Interest (toi)
    @GET("TAP/sync")
    suspend fun queryToiTable(
        @Query("query") adqlQuery: String,
        @Query("format") format: String = "json"
    ): List<TessObjectOfInterestDto>

    // 2. Kepler Cumulative KOIs (cumulative)
    @GET("TAP/sync")
    suspend fun queryKeplerTable(
        @Query("query") adqlQuery: String,
        @Query("format") format: String = "json"
    ): List<KeplerKoiDto>

    // 3. K2 Candidates (k2candidates)
    @GET("TAP/sync")
    suspend fun queryK2Table(
        @Query("query") adqlQuery: String,
        @Query("format") format: String = "json"
    ): List<K2CandidateDto>

    // 4. Confirmed Exoplanet Systems (pscomppars)
    @GET("TAP/sync")
    suspend fun queryConfirmedTable(
        @Query("query") adqlQuery: String,
        @Query("format") format: String = "json"
    ): List<ConfirmedExoplanetDto>
}
