package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// arXiv uses Atom XML. We use the JSON export via a wrapper or parse simplified fields.
// Direct endpoint: http://export.arxiv.org/api/query?search_query=all:{term}&start=0&max_results=50
// For simplicity we use a JSON wrapper service or parse the Atom feed.
// Here we define a simplified DTO for arXiv results obtained via a JSON proxy or manual parsing.

data class ArxivSearchResponse(
    @SerializedName("feed") val feed: ArxivFeed?
)

data class ArxivFeed(
    @SerializedName("entry") val entries: List<ArxivEntry>?
)

data class ArxivEntry(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("published") val published: String?,
    @SerializedName("updated") val updated: String?,
    @SerializedName("author") val authors: List<ArxivAuthor>?,
    @SerializedName("link") val links: List<ArxivLink>?,
    @SerializedName("primary_category") val primaryCategory: ArxivCategory?,
    @SerializedName("category") val categories: List<ArxivCategory>?
)

data class ArxivAuthor(
    @SerializedName("name") val name: String?
)

data class ArxivLink(
    @SerializedName("@href") val href: String?,
    @SerializedName("@rel") val rel: String?,
    @SerializedName("@type") val type: String?
)

data class ArxivCategory(
    @SerializedName("@term") val term: String?,
    @SerializedName("@scheme") val scheme: String?
)
