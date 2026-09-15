package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// Copernicus Data Space OData API Response
// Endpoint: GET https://catalogue.dataspace.copernicus.eu/odata/v1/Products?$filter=...

data class CopernicusODataResponse(
    @SerializedName("@odata.context") val odataContext: String?,
    @SerializedName("value") val value: List<CopernicusProduct>?
)

data class CopernicusProduct(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
    @SerializedName("ContentType") val contentType: String?,
    @SerializedName("ContentLength") val contentLength: Long?,
    @SerializedName("OriginDate") val originDate: String?,
    @SerializedName("PublicationDate") val publicationDate: String?,
    @SerializedName("ModificationDate") val modificationDate: String?,
    @SerializedName("Online") val online: Boolean?,
    @SerializedName("EvictionDate") val evictionDate: String?,
    @SerializedName("S3Path") val s3Path: String?,
    @SerializedName("Checksum") val checksum: List<CopernicusChecksum>?,
    @SerializedName("ContentGeometry") val contentGeometry: String?,
    @SerializedName("GeoFootprint") val geoFootprint: CopernicusGeoFootprint?,
    @SerializedName("Attributes") val attributes: List<CopernicusAttribute>?,
    @SerializedName("Assets") val assets: List<CopernicusAsset>?
)

data class CopernicusChecksum(
    @SerializedName("Algorithm") val algorithm: String?,
    @SerializedName("Value") val value: String?,
    @SerializedName("ChecksumDate") val checksumDate: String?
)

data class CopernicusGeoFootprint(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val coordinates: List<List<List<Double>>>?
)

data class CopernicusAttribute(
    @SerializedName("Name") val name: String?,
    @SerializedName("ValueType") val valueType: String?,
    @SerializedName("Value") val value: Any?
)

data class CopernicusAsset(
    @SerializedName("Type") val type: String?,
    @SerializedName("Href") val href: String?,
    @SerializedName("Size") val size: Long?
)
