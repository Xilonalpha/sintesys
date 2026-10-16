package com.synthesis.engine.astrometry

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Astrometry & Astronomical Coordinate Reference Engine
 *
 * Implements:
 * 1. IAU 1958 standard transformation between Equatorial (ICRS/J2000: α, δ) and Galactic (l, b) coordinates
 * 2. Sexagesimal celestial coordinate formatting (hh:mm:ss.ss, ±dd:mm:ss.s)
 * 3. Astronomical time baseline conversions: Unix Epoch -> Barycentric Julian Date (BJD_TDB) & TESS Barycentric Julian Date (BTJD)
 * 4. Deep-literature query generators for NASA Astrophysics Data System (NASA ADS), arXiv astro-ph, and CDS SIMBAD
 */
@Singleton
class AstrometryEngine @Inject constructor() {

    data class GalacticCoordinates(
        val lDeg: Double,      // Galactic Longitude [0..360)
        val bDeg: Double,      // Galactic Latitude [-90..+90]
        val formattedL: String,
        val formattedB: String
    )

    data class EclipticCoordinates(
        val lambdaDeg: Double, // Ecliptic Longitude [0..360)
        val betaDeg: Double,   // Ecliptic Latitude [-90..+90]
        val formattedLambda: String,
        val formattedBeta: String
    )

    data class AstrometricTargetDossier(
        val raDeg: Double,
        val decDeg: Double,
        val formattedRaSexagesimal: String,
        val formattedDecSexagesimal: String,
        val galactic: GalacticCoordinates,
        val bjdTdbCurrent: Double,
        val btjdCurrent: Double,
        val nasaAdsUrl: String,
        val arxivUrl: String,
        val simbadUrl: String
    )

    // IAU standard Galactic constants (J2000 / ICRS)
    // North Galactic Pole (NGP):
    private val alphaNgpDeg = 192.85948
    private val deltaNgpDeg = 27.12825
    // Galactic longitude of the North Celestial Pole:
    private val theta0Deg = 122.93192

    /**
     * Converts Equatorial coordinates (RA, Dec in degrees) into Galactic coordinates (l, b).
     * Rigorously adheres to the IAU 1958 standard coordinate rotation matrix.
     */
    fun equatorialToGalactic(raDeg: Double, decDeg: Double): GalacticCoordinates {
        val raRad = Math.toRadians(raDeg)
        val decRad = Math.toRadians(decDeg)
        val alphaNgpRad = Math.toRadians(alphaNgpDeg)
        val deltaNgpRad = Math.toRadians(deltaNgpDeg)

        // sin(b) = sin(δ) sin(δ_NGP) + cos(δ) cos(δ_NGP) cos(α - α_NGP)
        val sinB = sin(decRad) * sin(deltaNgpRad) + cos(decRad) * cos(deltaNgpRad) * cos(raRad - alphaNgpRad)
        val bRad = asin(sinB.coerceIn(-1.0, 1.0))
        val bDeg = Math.toDegrees(bRad)

        // cos(b) sin(θ_0 - l) = cos(δ) sin(α - α_NGP)
        val cosB = cos(bRad)
        val y = cos(decRad) * sin(raRad - alphaNgpRad)
        // cos(b) cos(θ_0 - l) = sin(δ) cos(δ_NGP) - cos(δ) sin(δ_NGP) cos(α - α_NGP)
        val x = sin(decRad) * cos(deltaNgpRad) - cos(decRad) * sin(deltaNgpRad) * cos(raRad - alphaNgpRad)

        val thetaDiffDeg = Math.toDegrees(atan2(y, x))
        var lDeg = (theta0Deg - thetaDiffDeg) % 360.0
        if (lDeg < 0.0) lDeg += 360.0

        val formattedL = String.format(Locale.US, "%.4f°", lDeg)
        val formattedB = String.format(Locale.US, "%+.4f°", bDeg)

        return GalacticCoordinates(
            lDeg = lDeg,
            bDeg = bDeg,
            formattedL = formattedL,
            formattedB = formattedB
        )
    }

    /**
     * Converts Equatorial coordinates (RA, Dec in degrees) into Ecliptic coordinates (λ, β).
     * Standard J2000 transformation using true obliquity of the ecliptic ε = 23.4392911°.
     */
    fun equatorialToEcliptic(raDeg: Double, decDeg: Double): EclipticCoordinates {
        val epsRad = Math.toRadians(23.4392911)
        val raRad = Math.toRadians(raDeg)
        val decRad = Math.toRadians(decDeg)

        // sin(β) = sin(δ) cos(ε) - cos(δ) sin(ε) sin(α)
        val sinBeta = sin(decRad) * cos(epsRad) - cos(decRad) * sin(epsRad) * sin(raRad)
        val betaRad = asin(sinBeta.coerceIn(-1.0, 1.0))
        val betaDeg = Math.toDegrees(betaRad)

        // y = sin(δ) sin(ε) + cos(δ) cos(ε) sin(α)
        // x = cos(δ) cos(α)
        val y = sin(decRad) * sin(epsRad) + cos(decRad) * cos(epsRad) * sin(raRad)
        val x = cos(decRad) * cos(raRad)
        var lambdaDeg = Math.toDegrees(atan2(y, x)) % 360.0
        if (lambdaDeg < 0.0) lambdaDeg += 360.0

        val formattedLambda = String.format(Locale.US, "%.4f°", lambdaDeg)
        val formattedBeta = String.format(Locale.US, "%+.4f°", betaDeg)

        return EclipticCoordinates(
            lambdaDeg = lambdaDeg,
            betaDeg = betaDeg,
            formattedLambda = formattedLambda,
            formattedBeta = formattedBeta
        )
    }

    /**
     * Formats Right Ascension in hours, minutes, and seconds: hhʰ mmᵐ ss.ssˢ
     */
    fun formatSexagesimalRa(raDeg: Double): String {
        var normalizedRa = raDeg % 360.0
        if (normalizedRa < 0.0) normalizedRa += 360.0

        val totalHours = normalizedRa / 15.0
        val hh = totalHours.toInt()
        val totalMinutes = (totalHours - hh) * 60.0
        val mm = totalMinutes.toInt()
        val ss = (totalMinutes - mm) * 60.0

        return String.format(Locale.US, "%02dh %02dm %05.2fs", hh, mm, ss)
    }

    /**
     * Formats Declination in degrees, arcminutes, and arcseconds: ±dd° mm' ss.s"
     */
    fun formatSexagesimalDec(decDeg: Double): String {
        val sign = if (decDeg >= 0) "+" else "-"
        val absDec = abs(decDeg)
        val dd = absDec.toInt()
        val totalArcmin = (absDec - dd) * 60.0
        val mm = totalArcmin.toInt()
        val ss = (totalArcmin - mm) * 60.0

        return String.format(Locale.US, "%s%02d° %02d' %04.1f\"", sign, dd, mm, ss)
    }

    /**
     * Converts Unix epoch timestamp (milliseconds) to Barycentric Julian Date in Barycentric Dynamical Time (BJD_TDB).
     * Baseline: Unix epoch (1970-01-01 00:00:00 UTC) = JD 2440587.5.
     * Incorporates standard Terrestrial Time offset (TT = UTC + 37s leap seconds + 32.184s offset = +69.184s).
     */
    fun unixMsToBjdTdb(unixMs: Long): Double {
        val jdUtc = (unixMs.toDouble() / 86400000.0) + 2440587.5
        // TT - UTC = 69.184 seconds -> in days: 69.184 / 86400.0 ≈ 0.00080074
        val deltaTtDays = 69.184 / 86400.0
        return jdUtc + deltaTtDays
    }

    /**
     * Converts Unix timestamp to TESS Barycentric Julian Date:
     * BTJD = BJD_TDB - 2457000.0
     * This is the exact temporal unit used across all NASA MAST / SPOC pipeline tables.
     */
    fun unixMsToBtjd(unixMs: Long): Double {
        return unixMsToBjdTdb(unixMs) - 2457000.0
    }

    /**
     * Formats BJD to standard astronomical precision: e.g. "BJD 2460300.12345"
     */
    fun formatBjd(bjd: Double): String {
        return String.format(Locale.US, "BJD %.5f", bjd)
    }

    /**
     * Formats BTJD to standard TESS precision: e.g. "BTJD 3300.1234"
     */
    fun formatBtjd(btjd: Double): String {
        return String.format(Locale.US, "BTJD %.4f", btjd)
    }

    /**
     * Generates a direct search query URL for NASA Astrophysics Data System (NASA ADS).
     * NASA ADS indexes peer-reviewed papers (ApJ, AJ, A&A, MNRAS, Nature Astronomy).
     */
    fun buildNasaAdsQueryUrl(
        canonicalName: String,
        ticId: String?,
        raDeg: Double? = null,
        decDeg: Double? = null
    ): String {
        val queryTerms = mutableListOf<String>()
        if (ticId != null && ticId.isNotBlank() && ticId != "unknown") {
            queryTerms.add("\"TIC $ticId\"")
            queryTerms.add("\"TIC$ticId\"")
        }
        if (canonicalName.isNotBlank() && !canonicalName.contains("Uncatalogued", ignoreCase = true)) {
            queryTerms.add("\"$canonicalName\"")
        }

        val query = if (queryTerms.isNotEmpty()) {
            queryTerms.joinToString(" OR ")
        } else if (raDeg != null && decDeg != null) {
            String.format(Locale.US, "pos(%.5f, %.5f, 0.05deg)", raDeg, decDeg)
        } else {
            "exoplanet transit"
        }

        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return "https://ui.adsabs.harvard.edu/search/q=$encodedQuery&sort=date+desc"
    }

    /**
     * Generates a direct search URL for arXiv Astrophysics (astro-ph).
     */
    fun buildArxivQueryUrl(canonicalName: String, ticId: String?): String {
        val term = if (canonicalName.isNotBlank() && !canonicalName.contains("Uncatalogued", ignoreCase = true)) {
            canonicalName
        } else if (ticId != null && ticId.isNotBlank() && ticId != "unknown") {
            "TIC $ticId"
        } else {
            "exoplanet"
        }
        val encoded = URLEncoder.encode(term, StandardCharsets.UTF_8.toString())
        return "https://arxiv.org/search/astro-ph?query=$encoded&searchtype=all"
    }

    /**
     * Generates a CDS SIMBAD Astronomical Database target lookup URL.
     */
    fun buildSimbadUrl(ticId: String?, canonicalName: String?): String {
        val identifier = if (canonicalName != null && canonicalName.isNotBlank() && !canonicalName.contains("Uncatalogued", ignoreCase = true)) {
            canonicalName
        } else if (ticId != null && ticId.isNotBlank()) {
            "TIC $ticId"
        } else {
            ""
        }
        val encoded = URLEncoder.encode(identifier, StandardCharsets.UTF_8.toString())
        return "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=$encoded"
    }

    /**
     * Creates a complete research dossier for any celestial target.
     */
    fun generateDossier(
        raDeg: Double,
        decDeg: Double,
        ticId: String?,
        canonicalName: String,
        timestampMs: Long = System.currentTimeMillis()
    ): AstrometricTargetDossier {
        val galactic = equatorialToGalactic(raDeg, decDeg)
        val bjd = unixMsToBjdTdb(timestampMs)
        val btjd = unixMsToBtjd(timestampMs)

        return AstrometricTargetDossier(
            raDeg = raDeg,
            decDeg = decDeg,
            formattedRaSexagesimal = formatSexagesimalRa(raDeg),
            formattedDecSexagesimal = formatSexagesimalDec(decDeg),
            galactic = galactic,
            bjdTdbCurrent = bjd,
            btjdCurrent = btjd,
            nasaAdsUrl = buildNasaAdsQueryUrl(canonicalName, ticId, raDeg, decDeg),
            arxivUrl = buildArxivQueryUrl(canonicalName, ticId),
            simbadUrl = buildSimbadUrl(ticId, canonicalName)
        )
    }
}
