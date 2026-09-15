package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// Real NASA Exoplanet Archive TAP response for the "toi" table (TESS Objects of Interest).
// Endpoint: GET https://exoplanetarchive.ipac.caltech.edu/TAP/sync?query=<ADQL>&format=json
// Column definitions: https://exoplanetarchive.ipac.caltech.edu/docs/API_TOI_columns.html
data class TessObjectOfInterestDto(
    @SerializedName("toi") val toi: Double?,
    @SerializedName("tid") val tid: Long?,
    @SerializedName("tfopwg_disp") val tfopwgDisp: String?,
    @SerializedName("pl_orbper") val plOrbPer: Double?,
    @SerializedName("pl_trandurh") val plTranDurH: Double?,
    @SerializedName("pl_trandep") val plTranDep: Double?,
    @SerializedName("pl_rade") val plRadE: Double?,
    @SerializedName("pl_insol") val plInsol: Double?,
    @SerializedName("pl_eqt") val plEqt: Double?,
    @SerializedName("st_tmag") val stTmag: Double?,
    @SerializedName("st_dist") val stDist: Double?,
    @SerializedName("st_teff") val stTeff: Double?,
    @SerializedName("st_rad") val stRad: Double?,
    @SerializedName("toi_created") val toiCreated: String?,
    @SerializedName("rowupdate") val rowUpdate: String?
)
