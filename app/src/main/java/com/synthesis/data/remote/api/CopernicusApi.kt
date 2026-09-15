package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.CopernicusODataResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CopernicusApi {
    // Copernicus OData Product Search
    // Endpoint: GET /odata/v1/Products
    // Base URL: https://catalogue.dataspace.copernicus.eu
    @GET("odata/v1/Products")
    suspend fun searchProducts(
        @Query("\$filter") filter: String,
        @Query("\$top") top: Int = 50,
        @Query("\$skip") skip: Int = 0,
        @Query("\$orderby") orderBy: String = "ContentDate/Start%20desc",
        @Query("\$count") count: Boolean = true
    ): CopernicusODataResponse
}
