package com.synthesis.data.remote.api

import com.google.gson.JsonObject
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Access to Space Telescope Science Institute (STScI) MAST Exo APIs
 * for retrieving raw and detrended TESS light curve photometry time series.
 */
interface MastExoApi {

    /**
     * Retrieves available TCEs and observation sectors for a given TESS Target (TIC ID)
     * e.g. https://exo.mast.stsci.edu/api/v0.1/dvdata/tess/{ticId}/tces/
     */
    @GET("api/v0.1/dvdata/tess/{ticId}/tces/")
    suspend fun getTces(
        @Path("ticId") ticId: String
    ): JsonObject

    /**
     * Retrieves detrended and raw light curve photometric time series data
     * e.g. https://exo.mast.stsci.edu/api/v0.1/dvdata/tess/{ticId}/table/?tce={tce}&sector={sector}
     */
    @GET("api/v0.1/dvdata/tess/{ticId}/table/")
    suspend fun getLightCurveTable(
        @Path("ticId") ticId: String,
        @Query("tce") tce: String,
        @Query("sector") sector: String
    ): JsonObject

    /**
     * Retrieves metadata & fitted parameters (TDUR, TDEPTH, TSNR, TPERIOD, PRADIUS)
     * e.g. https://exo.mast.stsci.edu/api/v0.1/dvdata/tess/{ticId}/info/?tce={tce}&sector={sector}
     */
    @GET("api/v0.1/dvdata/tess/{ticId}/info/")
    suspend fun getDvInfo(
        @Path("ticId") ticId: String,
        @Query("tce") tce: String,
        @Query("sector") sector: String
    ): JsonObject
}

/**
 * Interface for querying the MAST TIC (TESS Input Catalog) for stellar parameters
 * e.g. https://mast.stsci.edu/api/v0/invoke
 */
interface MastPortalApi {

    @FormUrlEncoded
    @POST("api/v0/invoke")
    suspend fun invokeService(
        @Field("request") requestJson: String
    ): JsonObject
}
