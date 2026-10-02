package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// 1. TESS Objects of Interest (table: "toi")
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

// 2. Kepler Objects of Interest (table: "cumulative")
// Real unvetted/candidate Kepler archive: https://exoplanetarchive.ipac.caltech.edu/docs/API_kepcandidate_columns.html
data class KeplerKoiDto(
    @SerializedName("kepid") val kepId: Long?,
    @SerializedName("kepoi_name") val kepoiName: String?,
    @SerializedName("kepler_name") val keplerName: String?,
    @SerializedName("koi_disposition") val koiDisposition: String?,
    @SerializedName("koi_pdisposition") val koiPdisposition: String?,
    @SerializedName("koi_score") val koiScore: Double?,
    @SerializedName("koi_period") val koiPeriod: Double?,
    @SerializedName("koi_duration") val koiDuration: Double?,
    @SerializedName("koi_depth") val koiDepth: Double?,
    @SerializedName("koi_prad") val koiPrad: Double?,
    @SerializedName("koi_insol") val koiInsol: Double?,
    @SerializedName("koi_teq") val koiTeq: Double?,
    @SerializedName("koi_steff") val koiSteff: Double?,
    @SerializedName("koi_srad") val koiSrad: Double?,
    @SerializedName("koi_kepmag") val koiKepMag: Double?
)

// 3. K2 Candidates (table: "k2candidates")
// K2 mission candidates
data class K2CandidateDto(
    @SerializedName("epic_name") val epicName: String?,
    @SerializedName("epic_candname") val epicCandName: String?,
    @SerializedName("k2c_disp") val k2cDisp: String?,
    @SerializedName("pl_orbper") val plOrbPer: Double?,
    @SerializedName("pl_trandur") val plTranDur: Double?,
    @SerializedName("pl_trandep") val plTranDep: Double?,
    @SerializedName("pl_rade") val plRadE: Double?,
    @SerializedName("pl_insol") val plInsol: Double?,
    @SerializedName("pl_eqt") val plEqt: Double?,
    @SerializedName("st_teff") val stTeff: Double?,
    @SerializedName("st_rad") val stRad: Double?,
    @SerializedName("st_optmag") val stOptMag: Double?
)

// 4. Confirmed Exoplanets (table: "pscomppars")
// Composite parameters for all humanity-confirmed planets
data class ConfirmedExoplanetDto(
    @SerializedName("pl_name") val plName: String?,
    @SerializedName("hostname") val hostName: String?,
    @SerializedName("tic_id") val ticId: String?,
    @SerializedName("discoverymethod") val discoveryMethod: String?,
    @SerializedName("disc_year") val discYear: Int?,
    @SerializedName("disc_facility") val discFacility: String?,
    @SerializedName("pl_orbper") val plOrbPer: Double?,
    @SerializedName("pl_trandur") val plTranDur: Double?,
    @SerializedName("pl_trandep") val plTranDep: Double?,
    @SerializedName("pl_rade") val plRadE: Double?,
    @SerializedName("pl_bmasse") val plBmassE: Double?,
    @SerializedName("pl_eqt") val plEqt: Double?,
    @SerializedName("pl_insol") val plInsol: Double?,
    @SerializedName("sy_dist") val syDist: Double?,
    @SerializedName("sy_vmag") val syVmag: Double?,
    @SerializedName("st_teff") val stTeff: Double?,
    @SerializedName("st_rad") val stRad: Double?
)
