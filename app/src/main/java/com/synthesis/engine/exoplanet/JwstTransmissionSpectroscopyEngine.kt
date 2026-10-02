package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.JwstTransmissionSpectrumResult
import com.synthesis.domain.model.exoplanet.SpectralBandPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * JWST Atmospheric Transmission Spectroscopy Synthesizer (Cycle 4/5 - 2026 Edition)
 *
 * Implements:
 * 1. Physical scale height and transit depth modulation (Seager & Sasselov 2000)
 * 2. Molecular cross-section modeling (H2O, CO2, CH4, SO2 photochemistry, CO) across 0.6 - 12.0 um
 * 3. Kempton et al. (2018, PASP 130:114401) Transmission Spectroscopy Metric (TSM)
 *    and Emission Spectroscopy Metric (ESM) priority ranking.
 */
@Singleton
class JwstTransmissionSpectroscopyEngine @Inject constructor() {

    companion object {
        const val G_CONST = 6.67430e-11 // m^3 / (kg * s^2)
        const val K_BOLTZMANN = 1.380649e-23 // J / K
        const val M_EARTH_KG = 5.972e24
        const val R_EARTH_M = 6.371e6
        const val R_SUN_M = 6.9634e8
        const val AMU_KG = 1.660539e-27
    }

    /**
     * Synthesizes transmission spectrum and calculates JWST priority metrics.
     */
    fun synthesizeJwstSpectrum(
        planetName: String,
        hostStar: String,
        planetRadiusEarth: Double,
        planetMassEarth: Double,
        stellarRadiusSun: Double,
        equilibriumTempK: Double,
        stellarTeffK: Double,
        jMagnitude: Double = 9.5,
        kMagnitude: Double = 9.0,
        hasPhotochemicalSo2: Boolean = true
    ): JwstTransmissionSpectrumResult {
        // Physical surface gravity g (m/s^2)
        val mPlanetKg = planetMassEarth * M_EARTH_KG
        val rPlanetM = planetRadiusEarth * R_EARTH_M
        val rStarM = stellarRadiusSun * R_SUN_M
        val gSurface = (G_CONST * mPlanetKg) / (rPlanetM.pow(2))

        // Mean molecular weight mu (amu)
        val muAmu = when {
            planetRadiusEarth >= 3.0 -> 2.3 // H2 / He dominated gas giant / sub-Neptune
            planetRadiusEarth >= 1.6 -> 8.0 // Water-rich / Volatile-rich mini-Neptune
            else -> 28.0 // Heavy N2 / CO2 / H2O secondary terrestrial atmosphere
        }
        val muKg = muAmu * AMU_KG

        // Atmospheric scale height H = (k_B * T) / (mu * g)
        val scaleHeightM = (K_BOLTZMANN * equilibriumTempK) / (muKg * gSurface)
        val scaleHeightKm = scaleHeightM / 1000.0

        // Geometric baseline transit depth: (R_p / R_*)^2
        val baselineDepthFraction = (rPlanetM / rStarM).pow(2)
        val baselineDepthPpm = baselineDepthFraction * 1_000_000.0

        // Modulation per scale height: delta_depth = (2 * R_p * H) / R_*^2
        val deltaDepthPerScaleHeightPpm = ((2.0 * rPlanetM * scaleHeightM) / (rStarM.pow(2))) * 1_000_000.0

        // Transmission Spectroscopy Metric (TSM - Kempton et al. 2018)
        val scaleFactor = when {
            planetRadiusEarth < 1.5 -> 0.190
            planetRadiusEarth < 2.75 -> 1.26
            planetRadiusEarth < 4.0 -> 1.28
            else -> 1.15
        }
        val tsm = scaleFactor * ((planetRadiusEarth.pow(3) * equilibriumTempK) / (planetMassEarth * stellarRadiusSun.pow(2))) * 10.0.pow(-jMagnitude / 5.0)

        // Emission Spectroscopy Metric (ESM - Kempton et al. 2018)
        val dayTempK = 1.10 * equilibriumTempK
        val b75Planet = planckFunction75(dayTempK)
        val b75Star = planckFunction75(stellarTeffK)
        val esm = 4.29e6 * (b75Planet / b75Star) * (rPlanetM / rStarM).pow(2) * 10.0.pow(-kMagnitude / 5.0)

        val jwstPriority = when {
            tsm >= 80.0 -> "★ TIER 1 (JWST Cycle 4/5 Golden Target - S/N > 10 in 1 transit)"
            tsm >= 40.0 -> "TIER 2 (High-Priority Target - Clear atmospheric detections in 2-3 transits)"
            tsm >= 15.0 -> "MODERATE (Requires 4-8 transits or MIRI phase curves)"
            else -> "LOW (Dense cloud deck or faint host star; requires stacked JWST visits)"
        }

        // Generate wavelength grid across NIRSpec (0.6 - 5.0 um) & MIRI (5.0 - 12.0 um)
        val wavelengths = doubleArrayOf(
            0.8, 1.1, 1.25, 1.4, 1.65, 1.9, 2.1, 2.3, 2.7, 3.0, 3.3, 3.8,
            4.05, 4.3, 4.67, 5.0, 6.3, 7.7, 8.5, 9.5, 10.5, 12.0
        )

        val detectedFeatures = mutableListOf<String>()
        val spectrumPoints = mutableListOf<SpectralBandPoint>()

        for (wl in wavelengths) {
            var nHeights = 3.0 // Base absorption continuum (Rayleigh + collision-induced)
            var absorbingMol: String? = null

            // Molecular cross-section peaks
            when {
                // H2O absorption bands
                absDiff(wl, 1.4) < 0.1 || absDiff(wl, 1.9) < 0.1 || absDiff(wl, 2.7) < 0.15 || absDiff(wl, 6.3) < 0.3 -> {
                    nHeights += 3.8
                    absorbingMol = "H2O (Vapori de Apă)"
                    if (!detectedFeatures.contains("H2O")) detectedFeatures.add("H2O")
                }
                // CO2 fundamental asymmetric stretch (4.3 um) & overtone (2.7 um)
                absDiff(wl, 4.3) < 0.15 || absDiff(wl, 2.7) < 0.1 -> {
                    nHeights += 5.5
                    absorbingMol = "CO2 (Dioxid de Carbon)"
                    if (!detectedFeatures.contains("CO2")) detectedFeatures.add("CO2")
                }
                // SO2 Photochemical peak at 4.05 um (WASP-39 b JWST flagship discovery)
                hasPhotochemicalSo2 && absDiff(wl, 4.05) < 0.08 -> {
                    nHeights += 4.2
                    absorbingMol = "SO2 (Fotochimie UV Activă)"
                    if (!detectedFeatures.contains("SO2")) detectedFeatures.add("SO2")
                }
                // CH4 methane features
                absDiff(wl, 2.3) < 0.1 || absDiff(wl, 3.3) < 0.15 || absDiff(wl, 7.7) < 0.3 -> {
                    nHeights += 3.5
                    absorbingMol = "CH4 (Metan)"
                    if (!detectedFeatures.contains("CH4")) detectedFeatures.add("CH4")
                }
                // CO carbon monoxide
                absDiff(wl, 4.67) < 0.1 -> {
                    nHeights += 2.6
                    absorbingMol = "CO (Monoxid de Carbon)"
                    if (!detectedFeatures.contains("CO")) detectedFeatures.add("CO")
                }
            }

            val depth = baselineDepthPpm + (nHeights * deltaDepthPerScaleHeightPpm)
            val instError = (8.0 + (wl * 1.5)).coerceAtLeast(6.0) // JWST NIRSpec/MIRI instrument noise floor ~ 10 ppm

            spectrumPoints.add(
                SpectralBandPoint(
                    wavelengthMicrons = wl,
                    transitDepthPpm = depth,
                    absorbingMolecule = absorbingMol,
                    instrumentalErrorPpm = instError
                )
            )
        }

        val summary = "Sinteză spectrală JWST (NIRSpec/MIRI): H = ${String.format("%.1f", scaleHeightKm)} km, " +
                "TSM = ${String.format("%.1f", tsm)}, ESM = ${String.format("%.1f", esm)}. " +
                "Semnătură atmosferică dominată de: ${detectedFeatures.joinToString(", ")}."

        return JwstTransmissionSpectrumResult(
            planetName = planetName,
            hostStar = hostStar,
            equilibriumTempK = equilibriumTempK,
            surfaceGravityMs2 = gSurface,
            scaleHeightKm = scaleHeightKm,
            transmissionSpectroscopyMetric = tsm,
            emissionSpectroscopyMetric = esm,
            jwstFollowUpPriority = jwstPriority,
            detectedAbsorptionFeatures = detectedFeatures,
            spectrumPoints = spectrumPoints,
            photochemistryDetected = hasPhotochemicalSo2,
            atmosphericSummary = summary
        )
    }

    private fun absDiff(a: Double, b: Double): Double = kotlin.math.abs(a - b)

    private fun planckFunction75(tK: Double): Double {
        // Planck blackbody function approximation at 7.5 um (MIRI band)
        val c1 = 1.191e8 // W * um^4 / (m^2 * sr)
        val c2 = 14387.7 // um * K
        val wl = 7.5
        val exponent = (c2 / (wl * tK)).coerceIn(0.1, 50.0)
        return (c1 / (wl.pow(5) * (exp(exponent) - 1.0))).coerceAtLeast(1e-10)
    }
}
