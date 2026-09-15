package com.synthesis.data.remote.api

import com.google.gson.JsonArray
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WorldBankApi {
    // World Bank Indicator Data
    // Endpoint: GET /v2/country/all/indicator/{indicator_code}
    // Base URL: https://api.worldbank.org
    @GET("v2/country/all/indicator/{indicator_code}")
    suspend fun getIndicatorData(
        @Path("indicator_code") indicatorCode: String,
        @Query("format") format: String = "json",
        @Query("date") date: String? = null,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): JsonArray

    // Specific country indicator
    @GET("v2/country/{country_code}/indicator/{indicator_code}")
    suspend fun getCountryIndicator(
        @Path("country_code") countryCode: String,
        @Path("indicator_code") indicatorCode: String,
        @Query("format") format: String = "json",
        @Query("date") date: String? = null,
        @Query("per_page") perPage: Int = 100
    ): JsonArray
}
