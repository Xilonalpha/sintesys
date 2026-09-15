package com.synthesis.data.remote.dto

// JPL Horizons API Response (text/plain)
// Endpoint: GET https://ssd.jpl.nasa.gov/api/horizons.api?format=text&COMMAND='...'&... 
// Returns plain text ephemeris data that we parse manually

data class JplHorizonsParsedData(
    val targetName: String?,
    val targetId: String?,
    val startTime: String?,
    val stopTime: String?,
    val stepSize: String?,
    val ephemerisLines: List<JplEphemerisLine>,
    val rawText: String
)

data class JplEphemerisLine(
    val date: String?,
    val jd: Double?,
    val x: Double?,
    val y: Double?,
    val z: Double?,
    val vx: Double?,
    val vy: Double?,
    val vz: Double?,
    val lt: Double?, // light time
    val rg: Double?, // range
    val rr: Double?  // range rate
)
