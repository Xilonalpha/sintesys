package com.synthesis.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface JplHorizonsApi {
    // JPL Horizons Ephemeris API
    // Endpoint: GET /api/horizons.api
    // Base URL: https://ssd.jpl.nasa.gov
    @GET("api/horizons.api")
    suspend fun getEphemeris(
        @Query("format") format: String = "text",
        @Query("COMMAND") command: String, // e.g. "'499'" for Mars
        @Query("OBJ_DATA") objData: String = "YES",
        @Query("MAKE_EPHEM") makeEphem: String = "YES",
        @Query("EPHEM_TYPE") ephemType: String = "VECTORS",
        @Query("CENTER") center: String = "@sun", // or @399 for Earth
        @Query("START_TIME") startTime: String,
        @Query("STOP_TIME") stopTime: String,
        @Query("STEP_SIZE") stepSize: String = "1d",
        @Query("QUANTITIES") quantities: String = "1,9,20,23" // range, light-time, velocity, range-rate
    ): ResponseBody
}
