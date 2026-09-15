package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// PubMed ESearch Response (JSON mode)
// Endpoint: GET /esearch.fcgi?db=pubmed&term={}&retmax=50&retmode=json
// Base URL: https://eutils.ncbi.nlm.nih.gov/entrez/eutils

data class PubMedESearchResponse(
    @SerializedName("header") val header: PubMedHeader?,
    @SerializedName("esearchresult") val esearchresult: PubMedESearchResult?
)

data class PubMedHeader(
    @SerializedName("type") val type: String?,
    @SerializedName("version") val version: String?
)

data class PubMedESearchResult(
    @SerializedName("count") val count: String?,
    @SerializedName("retmax") val retmax: String?,
    @SerializedName("retstart") val retstart: String?,
    @SerializedName("idlist") val idlist: List<String>?,
    @SerializedName("translationset") val translationset: List<PubMedTranslation>?,
    @SerializedName("querytranslation") val querytranslation: String?
)

data class PubMedTranslation(
    @SerializedName("from") val from: String?,
    @SerializedName("to") val to: String?
)

// PubMed ESummary Response
// Endpoint: GET /esummary.fcgi?db=pubmed&id={pmids}&retmode=json
data class PubMedESummaryResponse(
    @SerializedName("header") val header: PubMedHeader?,
    @SerializedName("result") val result: PubMedResultWrapper?
)

data class PubMedResultWrapper(
    @SerializedName("uids") val uids: List<String>?,
    @SerializedName("_id") val idMap: Map<String, PubMedArticleSummary>?
)

data class PubMedArticleSummary(
    @SerializedName("uid") val uid: String?,
    @SerializedName("pubdate") val pubdate: String?,
    @SerializedName("epubdate") val epubdate: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("authors") val authors: List<PubMedAuthor>?,
    @SerializedName("lastauthor") val lastauthor: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("sorttitle") val sorttitle: String?,
    @SerializedName("volume") val volume: String?,
    @SerializedName("issue") val issue: String?,
    @SerializedName("pages") val pages: String?,
    @SerializedName("lang") val lang: List<String>?,
    @SerializedName("nlmuniqueid") val nlmuniqueid: String?,
    @SerializedName("issn") val issn: String?,
    @SerializedName("essn") val essn: String?,
    @SerializedName("pubtype") val pubtype: List<String>?,
    @SerializedName("recordstatus") val recordstatus: String?,
    @SerializedName("pubstatus") val pubstatus: String?,
    @SerializedName("articleids") val articleids: List<PubMedArticleId>?,
    @SerializedName("history") val history: List<PubMedHistory>?,
    @SerializedName("references") val references: List<String>?,
    @SerializedName("attributes") val attributes: List<String>?,
    @SerializedName("pmcrefcount") val pmcrefcount: Int?,
    @SerializedName("fulljournalname") val fulljournalname: String?,
    @SerializedName("sortdate") val sortdate: String?,
    @SerializedName("pmclivedate") val pmclivedate: String?
)

data class PubMedAuthor(
    @SerializedName("name") val name: String?,
    @SerializedName("authtype") val authtype: String?,
    @SerializedName("clusterid") val clusterid: String?
)

data class PubMedArticleId(
    @SerializedName("idtype") val idtype: String?,
    @SerializedName("idtypen") val idtypen: Int?,
    @SerializedName("value") val value: String?
)

data class PubMedHistory(
    @SerializedName("pubstatus") val pubstatus: String?,
    @SerializedName("date") val date: String?
)
