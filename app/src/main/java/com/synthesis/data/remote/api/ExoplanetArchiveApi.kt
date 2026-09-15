package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.TessObjectOfInterestDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ExoplanetArchiveApi {
    // Real endpoint. ADQL query is passed raw as the "query" param.
    // Example query built by the repository:
    // select toi,tid,tfopwg_disp,pl_orbper,pl_trandurh,pl_trandep,pl_rade,pl_insol,pl_eqt,
    //        st_tmag,st_dist,st_teff,st_rad,toi_created,rowupdate
    // from toi where tfopwg_disp in ('PC','APC')
    @GET("TAP/sync")
    suspend fun queryToiTable(
        @Query("query") adqlQuery: String,
        @Query("format") format: String = "json"
    ): List<TessObjectOfInterestDto>
}
