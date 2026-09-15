package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// NCBI Datasets Virus Genome Response
// Endpoint: GET https://api.ncbi.nlm.nih.gov/datasets/v2/virus/taxon/{taxon_id}/genome

data class NcbiVirusGenomeResponse(
    @SerializedName("reports") val reports: List<NcbiVirusReport>?,
    @SerializedName("total_count") val totalCount: Int?
)

data class NcbiVirusReport(
    @SerializedName("accession") val accession: String?,
    @SerializedName("organism") val organism: NcbiVirusOrganism?,
    @SerializedName("isolate") val isolate: NcbiVirusIsolate?,
    @SerializedName("length") val length: Int?,
    @SerializedName("completeness") val completeness: String?,
    @SerializedName("is_complete") val isComplete: Boolean?,
    @SerializedName("is_annotation") val isAnnotation: Boolean?,
    @SerializedName("is_refseq") val isRefseq: Boolean?,
    @SerializedName("gene_count") val geneCount: Int?,
    @SerializedName("protein_count") val proteinCount: Int?,
    @SerializedName("nucleotide") val nucleotide: NcbiVirusNucleotide?,
    @SerializedName("annotation") val annotation: NcbiVirusAnnotation?
)

data class NcbiVirusOrganism(
    @SerializedName("tax_id") val taxId: Int?,
    @SerializedName("organism_name") val organismName: String?,
    @SerializedName("common_name") val commonName: String?,
    @SerializedName("lineage") val lineage: List<NcbiVirusLineage>?
)

data class NcbiVirusLineage(
    @SerializedName("tax_id") val taxId: Int?,
    @SerializedName("name") val name: String?
)

data class NcbiVirusIsolate(
    @SerializedName("name") val name: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("collection_date") val collectionDate: String?,
    @SerializedName("geo_location") val geoLocation: String?,
    @SerializedName("host") val host: String?
)

data class NcbiVirusNucleotide(
    @SerializedName("sequence_id") val sequenceId: String?,
    @SerializedName("length") val length: Int?
)

data class NcbiVirusAnnotation(
    @SerializedName("genes") val genes: List<NcbiVirusGene>?
)

data class NcbiVirusGene(
    @SerializedName("name") val name: String?,
    @SerializedName("range") val range: List<NcbiVirusRange>?
)

data class NcbiVirusRange(
    @SerializedName("begin") val begin: String?,
    @SerializedName("end") val end: String?,
    @SerializedName("orientation") val orientation: String?
)
