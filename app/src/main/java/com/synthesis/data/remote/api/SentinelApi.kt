package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.SentinelCatalogRequest
import com.synthesis.data.remote.dto.SentinelCatalogResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SentinelApi {
    // Sentinel Hub Catalog API - search for satellite imagery
    // Endpoint: POST /api/v1/catalog/search
    // Base URL: https://services.sentinel-hub.com
    @POST("api/v1/catalog/search")
    suspend fun searchCatalog(
        @Body request: SentinelCatalogRequest
    ): SentinelCatalogResponse
}
