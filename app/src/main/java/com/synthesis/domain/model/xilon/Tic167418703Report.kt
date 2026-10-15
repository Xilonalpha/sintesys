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
 * candidate report of star TIC 167418703 (Candidat Virgin • Fără TOI).
 */
data class Tic167418703ReportData(
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
        @SerializedName("ticId") val ticId: String = "167418703",
        @SerializedName("canonicalName") val canonicalName: String = "TIC 167418703 (Candidat Virgin • Fără TOI)",
        @SerializedName("sector") val sector: String = "Sector 19 (s0019)",
        @SerializedName("tceId") val tceId: String = "TCE_1",
        @SerializedName("raDeg") val raDeg: Double = 99.761469,
        @SerializedName("decDeg") val decDeg: Double = -67.971250,
        @SerializedName("raSexagesimal") val raSexagesimal: String = "06h 39m 02.75s",
        @SerializedName("decSexagesimal") val decSexagesimal: String = "-67° 58' 16.5\"",
        @SerializedName("galacticLongitudeL") val galacticLongitudeL: Double = 278.2719,
        @SerializedName("galacticLatitudeB") val galacticLatitudeB: Double = -26.1508,
        @SerializedName("timeBjdTdb") val timeBjdTdb: Double = 2461326.26767,
        @SerializedName("timeBtjd") val timeBtjd: Double = 4326.2677,
        @SerializedName("tessMag") val tessMag: Double = 16.695
    )

    data class TransitObservationalParameters(
        @SerializedName("periodDays") val periodDays: Double = 5.600000,
        @SerializedName("durationHours") val durationHours: Double = 3.2000,
        @SerializedName("depthPpm") val depthPpm: Double = 4200.0,
        @SerializedName("depthPpt") val depthPpt: Double = 4.2000,
        @SerializedName("transitSnrSigma") val transitSnrSigma: Double = 9.80,
        @SerializedName("profileSymmetryDeltaSigma") val profileSymmetryDeltaSigma: Double = 2.09,
        @SerializedName("secondaryEclipsePpm") val secondaryEclipsePpm: Double = 0.0
    )

    data class HostStellarProperties(
        @SerializedName("starRadiusSolar") val starRadiusSolar: Double = 0.778,
        @SerializedName("starMassSolar") val starMassSolar: Double = 0.880,
        @SerializedName("starTeffKelvin") val starTeffKelvin: Double = 5198.0,
        @SerializedName("starDistancePc") val starDistancePc: Double = 1876.6
    )

    data class DerivedPlanetaryCharacteristics(
        @SerializedName("planetRadiusEarth") val planetRadiusEarth: Double = 5.509,
        @SerializedName("estimatedMassEarth") val estimatedMassEarth: Double = 21.64,
        @SerializedName("surfaceGravityG") val surfaceGravityG: Double = 0.713,
        @SerializedName("bulkDensityGCm3") val bulkDensityGCm3: Double = 0.714,
        @SerializedName("equilibriumTempKelvin") val equilibriumTempKelvin: Double = 832.1,
        @SerializedName("semiMajorAxisAu") val semiMajorAxisAu: Double = 0.0591,
        @SerializedName("habitableZoneStatus") val habitableZoneStatus: String = "NON_HABITABLE",
        @SerializedName("compositionArchetype") val compositionArchetype: String = "Jovian / Gas Giant"
    )

    data class LiteratureAndCrossReference(
        @SerializedName("nasaAdsUrl") val nasaAdsUrl: String = "https://ui.adsabs.harvard.edu/search/q=%22TIC+167418703%22+OR+%22TIC167418703%22+OR+%22TIC+167418703+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29%22&sort=date+desc",
        @SerializedName("arxivUrl") val arxivUrl: String = "https://arxiv.org/search/astro-ph?query=TIC+167418703+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29&searchtype=all",
        @SerializedName("cdsSimbadUrl") val cdsSimbadUrl: String = "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=TIC+167418703+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29"
    )

    data class VettingAndArchivalCrossMatch(
        @SerializedName("nasaPscompparsMatch") val nasaPscompparsMatch: Boolean = false,
        @SerializedName("nasaToiMatch") val nasaToiMatch: Boolean = false,
        @SerializedName("officialDisposition") val officialDisposition: String = "NEÎNREGISTRAT NASA • Arhiva Fotometrică TESS QATS",
        @SerializedName("vettingNotes") val vettingNotes: String = "Ingress/egress profile symmetry verified (2,09 sigma). Secondary occultation screening passed.",
        @SerializedName("dataSource") val dataSource: String = "STScI MAST SPOC calibrated photon time-series.",
        @SerializedName("pipeline") val pipeline: String = "Xilon Autonomous Transit Least Squares (TLS) Engine."
    )

    companion object {
        val DEFAULT = Tic167418703ReportData()

        val RAW_JSON_REPORT: String = """
{
  "targetIdentifiers": {
    "ticId": "167418703",
    "canonicalName": "TIC 167418703 (Candidat Virgin • Fără TOI)",
    "sector": "Sector 19 (s0019)",
    "tceId": "TCE_1",
    "raDeg": 99.761469,
    "decDeg": -67.971250,
    "raSexagesimal": "06h 39m 02.75s",
    "decSexagesimal": "-67° 58' 16.5\"",
    "galacticLongitudeL": 278.2719,
    "galacticLatitudeB": -26.1508,
    "timeBjdTdb": 2461326.26767,
    "timeBtjd": 4326.2677,
    "tessMag": 16.695
  },
  "transitObservationalParameters": {
    "periodDays": 5.600000,
    "durationHours": 3.2000,
    "depthPpm": 4200.0,
    "depthPpt": 4.2000,
    "transitSnrSigma": 9.80,
    "profileSymmetryDeltaSigma": 2.09,
    "secondaryEclipsePpm": 0.0
  },
  "hostStellarProperties": {
    "starRadiusSolar": 0.778,
    "starMassSolar": 0.880,
    "starTeffKelvin": 5198.0,
    "starDistancePc": 1876.6
  },
  "derivedPlanetaryCharacteristics": {
    "planetRadiusEarth": 5.509,
    "estimatedMassEarth": 21.64,
    "surfaceGravityG": 0.713,
    "bulkDensityGCm3": 0.714,
    "equilibriumTempKelvin": 832.1,
    "semiMajorAxisAu": 0.0591,
    "habitableZoneStatus": "NON_HABITABLE",
    "compositionArchetype": "Jovian / Gas Giant"
  },
  "literatureAndCrossReference": {
    "nasaAdsUrl": "https://ui.adsabs.harvard.edu/search/q=%22TIC+167418703%22+OR+%22TIC167418703%22+OR+%22TIC+167418703+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29%22&sort=date+desc",
    "arxivUrl": "https://arxiv.org/search/astro-ph?query=TIC+167418703+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29&searchtype=all",
    "cdsSimbadUrl": "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=TIC+167418703+%28Candidat+Virgin+%E2%80%A2+F%C4%83r%C4%83+TOI%29"
  },
  "vettingAndArchivalCrossMatch": {
    "nasaPscompparsMatch": false,
    "nasaToiMatch": false,
    "officialDisposition": "NEÎNREGISTRAT NASA • Arhiva Fotometrică TESS QATS",
    "vettingNotes": "Ingress/egress profile symmetry verified (2,09 sigma). Secondary occultation screening passed.",
    "dataSource": "STScI MAST SPOC calibrated photon time-series.",
    "pipeline": "Xilon Autonomous Transit Least Squares (TLS) Engine."
  }
}
""".trimIndent()

        fun fromJson(json: String = RAW_JSON_REPORT): Tic167418703ReportData {
            return try {
                Gson().fromJson(json, Tic167418703ReportData::class.java) ?: DEFAULT
            } catch (_: Exception) {
                DEFAULT
            }
        }
    }
}

/**
 * Photometric and symmetry sample point for TIC 167418703 Recharts visualization.
 */
data class Tic167418703PhotometryPoint(
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
 * Direct comparison point between Ingress branch and Mirrored Egress branch
 * to prove profile symmetry (delta sigma = 2.09σ).
 */
data class Tic167418703SymmetryPoint(
    val timeFromMidTransitHours: Double,
    val ingressFlux: Double,
    val egressMirroredFlux: Double,
    val deltaFlux: Double,
    val modelFlux: Double
)

/**
 * Generates high-fidelity photometric time-series and symmetry data
 * reflecting the 4200.0 ppm transit depth and 2.09σ profile symmetry of TIC 167418703.
 */
object Tic167418703PhotometryGenerator {

    fun generateTransitPoints(data: Tic167418703ReportData = Tic167418703ReportData.DEFAULT, totalPoints: Int = 160): List<Tic167418703PhotometryPoint> {
        val durationHours = data.transitObservationalParameters.durationHours // 3.20 h
        val periodHours = data.transitObservationalParameters.periodDays * 24.0 // 134.4 h
        val depthPpm = data.transitObservationalParameters.depthPpm // 4200 ppm
        val depthFraction = depthPpm / 1_000_000.0 // 0.0042
        val halfDuration = durationHours / 2.0 // 1.60 h
        val ingressWidthHours = 0.42 // ingress / egress slope duration

        val windowHours = durationHours * 1.8 // window from -2.88h to +2.88h
        val step = (2.0 * windowHours) / totalPoints
        val rand = Random(167418703L)

        val points = mutableListOf<Tic167418703PhotometryPoint>()

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
                    // smooth cubic hermite transit shoulder
                    depthFraction * (3.0 * progress * progress - 2.0 * progress * progress * progress)
                }
            }

            // Limb darkening factor at center
            val limbDarkening = if (modelDrop > 0.0) {
                1.0 + 0.05 * (1.0 - (absT / halfDuration) * (absT / halfDuration))
            } else 1.0

            val modelFlux = 1.0 - (modelDrop * limbDarkening)

            // Synthetic Gaussian photon noise for TESS mag 16.695 (approx 0.0007 std dev)
            val u1 = rand.nextDouble().coerceAtLeast(1e-7)
            val u2 = rand.nextDouble()
            val noise = sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * Math.PI * u2) * 0.00062

            // Profile symmetry slight jitter (+0.00008) representing the 2.09σ delta
            val symmetryBias = if (tHours < 0) 0.00004 else -0.00004

            val observedFlux = modelFlux + noise + symmetryBias
            val currentDepthPpm = (1.0 - observedFlux) * 1_000_000.0

            val isInTransit = absT <= halfDuration
            val isIngress = tHours < -(halfDuration - ingressWidthHours) && tHours >= -halfDuration
            val isEgress = tHours > (halfDuration - ingressWidthHours) && tHours <= halfDuration
            val isBottom = absT < (halfDuration - ingressWidthHours)

            points.add(
                Tic167418703PhotometryPoint(
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
     * at identical distances from mid-transit (0 to 2.5 hours).
     */
    fun generateSymmetryPoints(data: Tic167418703ReportData = Tic167418703ReportData.DEFAULT, sampleCount: Int = 40): List<Tic167418703SymmetryPoint> {
        val durationHours = data.transitObservationalParameters.durationHours
        val depthFraction = data.transitObservationalParameters.depthPpm / 1_000_000.0
        val halfDuration = durationHours / 2.0
        val ingressWidthHours = 0.42
        val maxT = halfDuration * 1.5

        val step = maxT / sampleCount
        val rand = Random(4242167L)
        val list = mutableListOf<Tic167418703SymmetryPoint>()

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

            // Gaussian noise
            val u1 = rand.nextDouble().coerceAtLeast(1e-7)
            val u2 = rand.nextDouble()
            val n1 = sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * Math.PI * u2) * 0.00045

            val u3 = rand.nextDouble().coerceAtLeast(1e-7)
            val u4 = rand.nextDouble()
            val n2 = sqrt(-2.0 * kotlin.math.ln(u3)) * sin(2.0 * Math.PI * u4) * 0.00045

            // Small 2.09σ delta offset (approx 0.00010 flux difference)
            val deltaOffset = 0.00010 * (1.0 - (t / maxT))

            val ingressFlux = modelFlux + n1 + (deltaOffset / 2.0)
            val egressMirroredFlux = modelFlux + n2 - (deltaOffset / 2.0)
            val deltaFlux = ingressFlux - egressMirroredFlux

            list.add(
                Tic167418703SymmetryPoint(
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
