package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.NcbiVirusGenomeResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NcbiVirusApi {
    // NCBI Datasets Virus Genome
    // Endpoint: GET /datasets/v2/virus/taxon/{taxon_id}/genome
    // Base URL: https://api.ncbi.nlm.nih.gov
    @GET("datasets/v2/virus/taxon/{taxon_id}/genome")
    suspend fun getVirusGenomes(
        @Path("taxon_id") taxonId: String,
        @Query("page_size") pageSize: Int = 50,
        @Query("page_token") pageToken: String? = null,
        @Query("filters.reference_only") referenceOnly: Boolean? = null,
        @Query("filters.complete_only") completeOnly: Boolean? = null
    ): NcbiVirusGenomeResponse
}
