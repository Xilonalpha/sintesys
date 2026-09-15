package com.synthesis.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface ArxivApi {
    // arXiv search - returns Atom XML, we parse it as ResponseBody first
    // Real endpoint: http://export.arxiv.org/api/query
    @GET("api/query")
    suspend fun searchPapers(
        @Query("search_query") query: String,
        @Query("start") start: Int = 0,
        @Query("max_results") maxResults: Int = 50,
        @Query("sortBy") sortBy: String = "relevance",
        @Query("sortOrder") sortOrder: String = "descending"
    ): ResponseBody
}
