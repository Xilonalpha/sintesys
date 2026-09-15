package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// GBIF Occurrence Search Response
// Endpoint: GET https://api.gbif.org/v1/occurrence/search?q={query}&limit={limit}

data class GbifOccurrenceResponse(
    @SerializedName("offset") val offset: Int?,
    @SerializedName("limit") val limit: Int?,
    @SerializedName("endOfRecords") val endOfRecords: Boolean?,
    @SerializedName("count") val count: Int?,
    @SerializedName("results") val results: List<GbifOccurrence>?,
    @SerializedName("facets") val facets: List<GbifFacet>?
)

data class GbifOccurrence(
    @SerializedName("key") val key: Long?,
    @SerializedName("datasetKey") val datasetKey: String?,
    @SerializedName("publishingOrgKey") val publishingOrgKey: String?,
    @SerializedName("installationKey") val installationKey: String?,
    @SerializedName("hostingOrganizationKey") val hostingOrganizationKey: String?,
    @SerializedName("publishingCountry") val publishingCountry: String?,
    @SerializedName("protocol") val protocol: String?,
    @SerializedName("lastCrawled") val lastCrawled: String?,
    @SerializedName("lastParsed") val lastParsed: String?,
    @SerializedName("crawlId") val crawlId: Int?,
    @SerializedName("basisOfRecord") val basisOfRecord: String?,
    @SerializedName("occurrenceStatus") val occurrenceStatus: String?,
    @SerializedName("taxonKey") val taxonKey: Int?,
    @SerializedName("kingdomKey") val kingdomKey: Int?,
    @SerializedName("phylumKey") val phylumKey: Int?,
    @SerializedName("classKey") val classKey: Int?,
    @SerializedName("orderKey") val orderKey: Int?,
    @SerializedName("familyKey") val familyKey: Int?,
    @SerializedName("genusKey") val genusKey: Int?,
    @SerializedName("speciesKey") val speciesKey: Int?,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("kingdom") val kingdom: String?,
    @SerializedName("phylum") val phylum: String?,
    @SerializedName("order") val order: String?,
    @SerializedName("family") val family: String?,
    @SerializedName("genus") val genus: String?,
    @SerializedName("species") val species: String?,
    @SerializedName("genericName") val genericName: String?,
    @SerializedName("specificEpithet") val specificEpithet: String?,
    @SerializedName("taxonRank") val taxonRank: String?,
    @SerializedName("decimalLatitude") val decimalLatitude: Double?,
    @SerializedName("decimalLongitude") val decimalLongitude: Double?,
    @SerializedName("elevation") val elevation: Double?,
    @SerializedName("depth") val depth: Double?,
    @SerializedName("eventDate") val eventDate: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("month") val month: Int?,
    @SerializedName("day") val day: Int?,
    @SerializedName("country") val country: String?,
    @SerializedName("countryCode") val countryCode: String?,
    @SerializedName("continent") val continent: String?,
    @SerializedName("stateProvince") val stateProvince: String?,
    @SerializedName("locality") val locality: String?,
    @SerializedName("issue") val issue: String?,
    @SerializedName("media") val media: List<GbifMedia>?,
    @SerializedName("facts") val facts: List<GbifFact>?,
    @SerializedName("relations") val relations: List<GbifRelation>?
)

data class GbifMedia(
    @SerializedName("type") val type: String?,
    @SerializedName("format") val format: String?,
    @SerializedName("identifier") val identifier: String?
)

data class GbifFact(
    @SerializedName("key") val key: String?,
    @SerializedName("value") val value: String?
)

data class GbifRelation(
    @SerializedName("type") val type: String?,
    @SerializedName("id") val id: String?
)

data class GbifFacet(
    @SerializedName("field") val field: String?,
    @SerializedName("counts") val counts: List<GbifFacetCount>?
)

data class GbifFacetCount(
    @SerializedName("name") val name: String?,
    @SerializedName("count") val count: Int?
)
