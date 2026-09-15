package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// CERN Open Data Record Response (Invenio REST API)
// Endpoint: GET https://opendata.cern.ch/api/records/?q={query}&size={size}

data class CernOpenDataResponse(
    @SerializedName("hits") val hits: CernHits?
)

data class CernHits(
    @SerializedName("total") val total: Int?,
    @SerializedName("hits") val hits: List<CernRecord>?
)

data class CernRecord(
    @SerializedName("id") val id: String?,
    @SerializedName("metadata") val metadata: CernMetadata?,
    @SerializedName("created") val created: String?,
    @SerializedName("updated") val updated: String?
)

data class CernMetadata(
    @SerializedName("title") val title: String?,
    @SerializedName("abstract") val abstract: CernDescription?,
    @SerializedName("description") val description: CernDescription?,
    @SerializedName("authors") val authors: List<CernAuthor>?,
    @SerializedName("date_published") val datePublished: String?,
    @SerializedName("experiment") val experiment: CernExperiment?,
    @SerializedName("collections") val collections: List<CernCollection>?,
    @SerializedName("type") val type: CernType?,
    @SerializedName("keywords") val keywords: List<String>?,
    @SerializedName("doi") val doi: String?
)

data class CernDescription(
    @SerializedName("description") val description: String?
)

data class CernAuthor(
    @SerializedName("name") val name: String?,
    @SerializedName("affiliation") val affiliation: String?
)

data class CernExperiment(
    @SerializedName("name") val name: String?
)

data class CernCollection(
    @SerializedName("primary") val primary: String?,
    @SerializedName("secondary") val secondary: String?
)

data class CernType(
    @SerializedName("primary") val primary: String?,
    @SerializedName("secondary") val secondary: String?
)
