package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// ESA Gaia DR3 Cone Search Response (Gaia@AIP)
// Endpoint: GET https://gaia.aip.de/conesearch/api/gaiadr3.gaia_source/?RA={ra}&DEC={dec}&SR={radius}
// Returns VOTable or JSON depending on Accept header. We request JSON.

data class GaiaConeSearchResponse(
    @SerializedName("data") val data: List<GaiaSource>?,
    @SerializedName("total_count") val totalCount: Int?
)

data class GaiaSource(
    @SerializedName("source_id") val sourceId: String?,
    @SerializedName("ra") val ra: Double?,
    @SerializedName("dec") val dec: Double?,
    @SerializedName("parallax") val parallax: Double?,
    @SerializedName("pmra") val pmra: Double?,
    @SerializedName("pmdec") val pmdec: Double?,
    @SerializedName("radial_velocity") val radialVelocity: Double?,
    @SerializedName("phot_g_mean_mag") val photGMeanMag: Double?,
    @SerializedName("phot_bp_mean_mag") val photBpMeanMag: Double?,
    @SerializedName("phot_rp_mean_mag") val photRpMeanMag: Double?,
    @SerializedName("teff_gspphot") val teffGspphot: Double?,
    @SerializedName("distance_gspphot") val distanceGspphot: Double?,
    @SerializedName("radius_gspphot") val radiusGspphot: Double?,
    @SerializedName("classprob_dsc_combmod_quasar") val quasarProb: Double?,
    @SerializedName("classprob_dsc_combmod_galaxy") val galaxyProb: Double?,
    @SerializedName("classprob_dsc_combmod_star") val starProb: Double?
)
