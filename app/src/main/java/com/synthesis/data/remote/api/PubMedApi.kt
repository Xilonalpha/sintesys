package com.synthesis.data.remote.api

import com.synthesis.data.remote.dto.PubMedESearchResponse
import com.synthesis.data.remote.dto.PubMedESummaryResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PubMedApi {
    // ESearch - search PubMed and get PMIDs
    // Real endpoint: https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi
    @GET("esearch.fcgi")
    suspend fun searchPubMed(
        @Query("db") db: String = "pubmed",
        @Query("term") term: String,
        @Query("retmax") retmax: Int = 50,
        @Query("retmode") retmode: String = "json",
        @Query("sort") sort: String = "relevance"
    ): PubMedESearchResponse

    // ESummary - get article summaries by PMIDs
    // Real endpoint: https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi
    @GET("esummary.fcgi")
    suspend fun getArticleSummaries(
        @Query("db") db: String = "pubmed",
        @Query("id") ids: String,
        @Query("retmode") retmode: String = "json"
    ): PubMedESummaryResponse
}
