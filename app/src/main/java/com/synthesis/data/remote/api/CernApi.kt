package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.CernOpenDataResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CernApi {
    // CERN Open Data search records
    // Endpoint: GET /api/records/
    // Base URL: https://opendata.cern.ch
    @GET("api/records/")
    suspend fun searchRecords(
        @Query("q") query: String,
        @Query("size") size: Int = 50,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "mostrecent"
    ): CernOpenDataResponse
}
