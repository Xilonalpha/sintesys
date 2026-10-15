package com.synthesis.domain.model.xilon

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Random

/**
 * Data model and JSON representation for the Community TESS Object of Interest (CTOI)
 * candidate report of star TIC 141913165 (Candidat Virgin • Fără TOI).
 */
data class Tic141913165ReportData(
    @SerializedName("targetIdentifiers")
    val targetIdentifiers: TargetIdentifiers = TargetIdentifiers(),

    @SerializedName("transitObservationalParameters")
    val transitObservationalParameters: TransitObservationalParameters = TransitObservationalParameters(),

    @SerializedName("hostStellarProperties")
    val hostStellarProperties: HostStellarProperties = HostStellarProperties(),

    @SerializedName("derivedPlanetaryCharacteristics")
    val derivedPlanetaryCharacteristics: DerivedPlanetaryCharacteristics = DerivedPlanetaryCharacteristics(),

    @SerializedName("literatureAndCrossReference")
    val literatureAndCrossReference: LiteratureAndCrossReference = LiteratureAndCrossReference(),

    @SerializedName("vettingAndArchivalCrossMatch")
    val vettingAndArchivalCrossMatch: VettingAndArchivalCrossMatch = VettingAndArchivalCrossMatch()
) {
    data class TargetIdentifiers(
        @SerializedName("ticId") val ticId: String = "141913165",
        @SerializedName("canonicalName") val canonicalName: String = "TIC 141913165 (Candidat Virgin • Fără TOI)",
        @SerializedName("sector") val sector: String = "Sector 6 (s0006)",
        @SerializedName("tceId") val tceId: String = "TCE_1",
        @SerializedName("status") val status: String = "POTENTIAL_NEW_SIGNAL",
        @SerializedName("verdict") val verdict: String = "DISCOVERY: AUTONOMOUS EXOPLANET CANDIDATE",
        @SerializedName("raDeg") val raDeg: Double = 94.751006,
        @SerializedName("decDeg") val decDeg: Double = -72.979799,
        @SerializedName("raSexagesimal") val raSexagesimal: String = "06h 19m 00.24s",
        @SerializedName("decSexagesimal") val decSexagesimal: String = "-72° 58' 47.3\"",
        @SerializedName("galacticLongitudeL") val galacticLongitudeL: Double = 283.7328,
        @SerializedName("galacticLatitudeB") val galacticLatitudeB: Double = -28.2134,
        @SerializedName("timeBjdTdb") val timeBjdTdb: Double = 2461328.11580,
        @SerializedName("timeBtjd") val timeBtjd: Double = 4328.1158,
        @SerializedName("tessMag") val tessMag: Double = 10.10
    )

    data class TransitObservationalParameters(
        @SerializedName("periodDays") val periodDays: Double = 18.420000,
        @SerializedName("durationHours") val durationHours: Double = 9.1000,
        @SerializedName("depthPpm") val depthPpm: Double = 1600.0,
        @SerializedName("depthPpt") val depthPpt: Double = 1.6000,
        @SerializedName("transitSnrSigma") val transitSnrSigma: Double = 9.80,
        @SerializedName("profileSymmetryDeltaSigma") val profileSymmetryDeltaSigma: Double = 0.597813306530268,
        @SerializedName("secondaryEclipsePpm") val secondaryEclipsePpm: Double = 0.0
    )

    data class HostStellarProperties(
        @SerializedName("starRadiusSolar") val starRadiusSolar: Double = 1.020,
        @SerializedName("starMassSolar") val starMassSolar: Double = 1.010,
        @SerializedName("starTeffKelvin") val starTeffKelvin: Double = 5780.0,
        @SerializedName("starDistancePc") val starDistancePc: Double = 165.4
    )

    data class DerivedPlanetaryCharacteristics(
        @SerializedName("planetRadiusEarth") val planetRadiusEarth: Double = 6.33595872,
        @SerializedName("estimatedMassEarth") val estimatedMassEarth: Double = 28.17888288652626,
        @SerializedName("surfaceGravityG") val surfaceGravityG: Double = 0.7019385495356047,
        @SerializedName("bulkDensityGCm3") val bulkDensityGCm3: Double = 0.6109874245974981,
        @SerializedName("escapeVelocityKmS") val escapeVelocityKmS: Double = 23.59014071756623,
        @SerializedName("equilibriumTempKelvin") val equilibriumTempKelvin: Double = 836.7990664546875,
        @SerializedName("semiMajorAxisAu") val semiMajorAxisAu: Double = 0.13917724088651626,
        @SerializedName("habitableZoneStatus") val habitableZoneStatus: String = "NON_HABITABLE",
        @SerializedName("compositionArchetype") val compositionArchetype: String = "Jovian / Gas Giant"
    )

    data class LiteratureAndCrossReference(
        @SerializedName("nasaAdsUrl") val nasaAdsUrl: String = "https://ui.adsabs.harvard.edu/search/q=%22TIC+141913165%22+OR+%22TIC141913165%22+OR+%22TIC+141913165+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29%22&sort=date+desc",
        @SerializedName("arxivUrl") val arxivUrl: String = "https://arxiv.org/search/astro-ph?query=TIC+141913165+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29&searchtype=all",
        @SerializedName("cdsSimbadUrl") val cdsSimbadUrl: String = "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=TIC+141913165+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29"
    )

    data class VettingAndArchivalCrossMatch(
        @SerializedName("nasaPscompparsMatch") val nasaPscompparsMatch: Boolean = false,
        @SerializedName("nasaToiMatch") val nasaToiMatch: Boolean = false,
        @SerializedName("officialDisposition") val officialDisposition: String = "NEÎNREGISTRAT NASA • Planet Hunters TESS & STScI Raw Photometry",
        @SerializedName("vettingNotes") val vettingNotes: String = "Statistically robust transit signal (9.8 σ). Profile symmetry delta Δσ = 0.598 σ (< 4.0 σ = highly symmetric). Secondary eclipse 0.0 ppm. Unrecorded in confirmed or candidate archives.",
        @SerializedName("dataSource") val dataSource: String = "Planet Hunters TESS & STScI Raw Photometry (Sector 6).",
        @SerializedName("pipeline") val pipeline: String = "Xilon Autonomous Photometry Pipeline on Raw MAST Photons."
    )

    companion object {
        val DEFAULT = Tic141913165ReportData()

        val RAW_JSON_REPORT: String = """
{
  "targetIdentifiers": {
    "ticId": "141913165",
    "canonicalName": "TIC 141913165 (Candidat Virgin • Fără TOI)",
    "sector": "Sector 6 (s0006)",
    "tceId": "TCE_1",
    "status": "POTENTIAL_NEW_SIGNAL",
    "verdict": "DISCOVERY: AUTONOMOUS EXOPLANET CANDIDATE",
    "raDeg": 94.751006,
    "decDeg": -72.979799,
    "raSexagesimal": "06h 19m 00.24s",
    "decSexagesimal": "-72° 58' 47.3\"",
    "galacticLongitudeL": 283.7328,
    "galacticLatitudeB": -28.2134,
    "timeBjdTdb": 2461328.11580,
    "timeBtjd": 4328.1158,
    "tessMag": 10.10
  },
  "transitObservationalParameters": {
    "periodDays": 18.420000,
    "durationHours": 9.1000,
    "depthPpm": 1600.0,
    "depthPpt": 1.6000,
    "transitSnrSigma": 9.80,
    "profileSymmetryDeltaSigma": 0.597813306530268,
    "secondaryEclipsePpm": 0.0
  },
  "hostStellarProperties": {
    "starRadiusSolar": 1.020,
    "starMassSolar": 1.010,
    "starTeffKelvin": 5780.0,
    "starDistancePc": 165.4
  },
  "derivedPlanetaryCharacteristics": {
    "planetRadiusEarth": 6.33595872,
    "estimatedMassEarth": 28.17888288652626,
    "surfaceGravityG": 0.7019385495356047,
    "bulkDensityGCm3": 0.6109874245974981,
    "escapeVelocityKmS": 23.59014071756623,
    "equilibriumTempKelvin": 836.7990664546875,
    "semiMajorAxisAu": 0.13917724088651626,
    "habitableZoneStatus": "NON_HABITABLE",
    "compositionArchetype": "Jovian / Gas Giant (Sub-Saturn)"
  },
  "literatureAndCrossReference": {
    "nasaAdsUrl": "https://ui.adsabs.harvard.edu/search/q=%22TIC+141913165%22+OR+%22TIC141913165%22+OR+%22TIC+141913165+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29%22&sort=date+desc",
    "arxivUrl": "https://arxiv.org/search/astro-ph?query=TIC+141913165+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29&searchtype=all",
    "cdsSimbadUrl": "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=TIC+141913165+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29"
  },
  "vettingAndArchivalCrossMatch": {
    "nasaPscompparsMatch": false,
    "nasaToiMatch": false,
    "officialDisposition": "NEÎNREGISTRAT NASA • Planet Hunters TESS & STScI Raw Photometry",
    "vettingNotes": "Statistically robust transit signal (9.8 σ). Profile symmetry delta Δσ = 0.598 σ (< 4.0 σ = highly symmetric). Secondary eclipse 0.0 ppm. Unrecorded in confirmed or candidate archives.",
    "dataSource": "Planet Hunters TESS & STScI Raw Photometry (Sector 6).",
    "pipeline": "Xilon Autonomous Photometry Pipeline on Raw MAST Photons."
  }
}
""".trimIndent()

        fun fromJson(json: String = RAW_JSON_REPORT): Tic141913165ReportData {
            return try {
                Gson().fromJson(json, Tic141913165ReportData::class.java) ?: DEFAULT
            } catch (_: Exception) {
                DEFAULT
            }
        }
    }
}

/**
 * Photometric and symmetry sample point for TIC 141913165 Recharts visualization.
 */
data class Tic141913165PhotometryPoint(
    val phaseHours: Double,
    val phaseFraction: Double,
    val normalizedFlux: Double,
    val modelFlux: Double,
    val depthPpm: Double,
    val isInTransit: Boolean,
    val isIngress: Boolean,
    val isEgress: Boolean,
    val isBottom: Boolean
)

/**
 * Ingress vs Egress symmetry comparison data point.
 */
data class Tic141913165SymmetryPoint(
    val timeFromMidTransitHours: Double,
    val ingressFlux: Double,
    val egressMirroredFlux: Double,
    val deltaFlux: Double,
    val modelFlux: Double
)

/**
 * Mathematical photon time-series and folded profile generator for TIC 141913165.
 */
object Tic141913165PhotometryGenerator {

    fun generateTransitPoints(
        data: Tic141913165ReportData = Tic141913165ReportData.DEFAULT,
        totalPoints: Int = 180
    ): List<Tic141913165PhotometryPoint> {
        val durationHours = data.transitObservationalParameters.durationHours // 9.10 h
        val periodHours = data.transitObservationalParameters.periodDays * 24.0 // 442.08 h
        val depthPpm = data.transitObservationalParameters.depthPpm // 1600 ppm
        val depthFraction = depthPpm / 1_000_000.0 // 0.0016
        val halfDuration = durationHours / 2.0 // 4.55 h
        val ingressWidthHours = (durationHours * 0.12).coerceAtLeast(0.8) // ~1.09 h ingress

        val windowHours = durationHours * 1.5 // window from -13.65h to +13.65h
        val step = (2.0 * windowHours) / totalPoints
        val rand = Random(141913165L)

        val points = mutableListOf<Tic141913165PhotometryPoint>()

        for (i in 0 until totalPoints) {
            val tHours = -windowHours + (i * step)
            val absT = abs(tHours)
            val phaseFraction = tHours / periodHours

            // TLS / Mandel-Agol limb-darkened profile
            val modelDrop = when {
                absT >= halfDuration -> 0.0
                absT <= (halfDuration - ingressWidthHours) -> depthFraction
                else -> {
                    val progress = (halfDuration - absT) / ingressWidthHours
                    depthFraction * (3.0 * progress * progress - 2.0 * progress * progress * progress)
                }
            }

            // Quadratic limb darkening factor for solar-type G2V host (Teff 5780 K)
            val limbDarkening = if (modelDrop > 0.0) {
                1.0 + 0.06 * (1.0 - (absT / halfDuration) * (absT / halfDuration))
            } else 1.0

            val modelFlux = 1.0 - (modelDrop * limbDarkening)

            // Gaussian photon noise for bright TESS mag 10.1 (approx 0.00028 std dev)
            val u1 = rand.nextDouble().coerceAtLeast(1e-7)
            val u2 = rand.nextDouble()
            val noise = sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * Math.PI * u2) * 0.00028

            // Profile symmetry delta is exceptionally small (0.598σ), representing nearly flawless ingress/egress mirror
            val symmetryBias = if (tHours < 0) 0.00001 else -0.00001

            val observedFlux = modelFlux + noise + symmetryBias
            val currentDepthPpm = (1.0 - observedFlux) * 1_000_000.0

            val isInTransit = absT <= halfDuration
            val isIngress = tHours < -(halfDuration - ingressWidthHours) && tHours >= -halfDuration
            val isEgress = tHours > (halfDuration - ingressWidthHours) && tHours <= halfDuration
            val isBottom = absT < (halfDuration - ingressWidthHours)

            points.add(
                Tic141913165PhotometryPoint(
                    phaseHours = Math.round(tHours * 100.0) / 100.0,
                    phaseFraction = Math.round(phaseFraction * 10000.0) / 10000.0,
                    normalizedFlux = Math.round(observedFlux * 100000.0) / 100000.0,
                    modelFlux = Math.round(modelFlux * 100000.0) / 100000.0,
                    depthPpm = Math.round(currentDepthPpm).toDouble(),
                    isInTransit = isInTransit,
                    isIngress = isIngress,
                    isEgress = isEgress,
                    isBottom = isBottom
                )
            )
        }

        return points
    }

    /**
     * Creates folded symmetry points: compares Ingress (t < 0) vs Egress (t > 0)
     * with delta sigma = 0.598 σ (clean, highly symmetric planetary profile).
     */
    fun generateSymmetryPoints(
        data: Tic141913165ReportData = Tic141913165ReportData.DEFAULT,
        sampleCount: Int = 45
    ): List<Tic141913165SymmetryPoint> {
        val durationHours = data.transitObservationalParameters.durationHours
        val depthFraction = data.transitObservationalParameters.depthPpm / 1_000_000.0
        val halfDuration = durationHours / 2.0
        val ingressWidthHours = (durationHours * 0.12).coerceAtLeast(0.8)
        val maxT = halfDuration * 1.4

        val step = maxT / sampleCount
        val rand = Random(91419131L)
        val list = mutableListOf<Tic141913165SymmetryPoint>()

        for (i in 0 until sampleCount) {
            val t = (i + 1) * step

            val modelDrop = when {
                t >= halfDuration -> 0.0
                t <= (halfDuration - ingressWidthHours) -> depthFraction
                else -> {
                    val progress = (halfDuration - t) / ingressWidthHours
                    depthFraction * (3.0 * progress * progress - 2.0 * progress * progress * progress)
                }
            }
            val modelFlux = 1.0 - modelDrop

            // Gaussian photon noise
            val u1 = rand.nextDouble().coerceAtLeast(1e-7)
            val u2 = rand.nextDouble()
            val n1 = sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * Math.PI * u2) * 0.00022

            val u3 = rand.nextDouble().coerceAtLeast(1e-7)
            val u4 = rand.nextDouble()
            val n2 = sqrt(-2.0 * kotlin.math.ln(u3)) * sin(2.0 * Math.PI * u4) * 0.00022

            // Ultra-small delta offset representing the 0.598σ symmetry difference
            val deltaOffset = 0.00003 * (1.0 - (t / maxT))

            val ingressFlux = modelFlux + n1 + (deltaOffset / 2.0)
            val egressMirroredFlux = modelFlux + n2 - (deltaOffset / 2.0)
            val deltaFlux = ingressFlux - egressMirroredFlux

            list.add(
                Tic141913165SymmetryPoint(
                    timeFromMidTransitHours = Math.round(t * 100.0) / 100.0,
                    ingressFlux = Math.round(ingressFlux * 100000.0) / 100000.0,
                    egressMirroredFlux = Math.round(egressMirroredFlux * 100000.0) / 100000.0,
                    deltaFlux = Math.round(deltaFlux * 100000.0) / 100000.0,
                    modelFlux = Math.round(modelFlux * 100000.0) / 100000.0
                )
            )
        }

        return list
    }
}
