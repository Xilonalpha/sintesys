package com.synthesis.domain.model.exoplanet

import kotlin.math.*

/**
 * Astrobiological and astrophysical category of stellar light curve variations.
 */
enum class StellarTemplateCategory(val displayName: String) {
    PLANETARY_TRANSIT("Tranzit Exoplanetar"),
    ECLIPSING_BINARY("Binară de Eclipsă"),
    STELLAR_FLARE("Flare / Activitate Coronală"),
    ROTATIONAL_VARIABILITY("Modulație Rotațională (Pete)"),
    PULSATING_VARIABLE("Variabilă Pulsantă"),
    GRAVITATIONAL_MICROLENSING("Microlentilă Gravitațională")
}

/**
 * Individual phase-folded point on a theoretical stellar template light curve.
 */
data class TemplatePhasePoint(
    val phase: Double, // Normalized phase from -0.5 to +0.5 (or time offset)
    val normalizedFlux: Double, // Normalized relative flux (1.0 = baseline unocculted star)
    val uncertainty: Double = 0.0002 // 1-sigma theoretical uncertainty / noise band
)

/**
 * Comprehensive pre-defined stellar light curve template model.
 * Provides theoretical baseline morphology and physical parameters for manual
 * and automated verification of FITS-derived transit signals by space agency analysts.
 */
data class StellarLightCurveTemplate(
    val id: String,
    val name: String,
    val category: StellarTemplateCategory,
    val astrophysicalClass: String,
    val badgeColorHex: String,
    val morphology: String,
    val physicsDescription: String,
    val diagnosticVerificationFeatures: List<String>,
    val falsePositiveRisk: String,
    val typicalPeriod: String,
    val typicalDepthPpm: String,
    val formulaDescription: String,
    val isPeriodic: Boolean = true,
    val defaultDepthPpm: Double = 1000.0,
    val defaultDurationPhase: Double = 0.08,
    val sampleCurve: List<TemplatePhasePoint> = emptyList()
) {
    /**
     * Generates high-resolution synthetic flux points across phase [-0.5, +0.5]
     * based on the template's analytical astrophysical formula.
     */
    fun generateCurve(
        numPoints: Int = 120,
        customDepthPpm: Double? = null,
        customDurationPhase: Double? = null
    ): List<TemplatePhasePoint> {
        val depth = (customDepthPpm ?: defaultDepthPpm) / 1_000_000.0
        val duration = customDurationPhase ?: defaultDurationPhase
        val halfDur = duration / 2.0
        val points = mutableListOf<TemplatePhasePoint>()

        for (i in 0 until numPoints) {
            val phase = -0.5 + (i.toDouble() / (numPoints - 1).toDouble()) * 1.0
            val flux = evaluateFluxAtPhase(phase, depth, halfDur)
            points.add(TemplatePhasePoint(phase = phase, normalizedFlux = flux))
        }
        return points
    }

    private fun evaluateFluxAtPhase(phase: Double, depth: Double, halfDur: Double): Double {
        val absPhase = abs(phase)
        return when (id) {
            "planetary_transit_mandel_agol" -> {
                // Mandel & Agol (2002) U-Shape profile with quadratic limb-darkening
                if (absPhase > halfDur) {
                    1.0
                } else {
                    val ingressFraction = halfDur * 0.25
                    val ingressStart = halfDur - ingressFraction
                    if (absPhase > ingressStart) {
                        // Smooth polynomial ingress/egress transition
                        val tau = (halfDur - absPhase) / ingressFraction
                        1.0 - depth * (3.0 * tau * tau - 2.0 * tau * tau * tau)
                    } else {
                        // Flat bottom with gentle limb-darkening curvature (u1=0.3, u2=0.2)
                        val rNorm = absPhase / ingressStart
                        val limbCurvature = 1.0 - 0.05 * (1.0 - cos(rNorm * PI / 2.0))
                        1.0 - depth * limbCurvature
                    }
                }
            }
            "eclipsing_binary_detached_ea" -> {
                // Detached Algol-type eclipsing binary (EA)
                // Primary eclipse at phase 0.0, secondary eclipse at phase 0.5
                val primDepth = depth.coerceAtLeast(0.15)
                val secDepth = primDepth * 0.28
                val primDur = halfDur * 1.3
                val secPhase = abs(absPhase - 0.5)

                if (absPhase <= primDur) {
                    // Deep primary V/U-shaped eclipse
                    val f = absPhase / primDur
                    1.0 - primDepth * (1.0 - f * f).coerceAtLeast(0.0)
                } else if (secPhase <= primDur * 0.9) {
                    // Secondary occultation at phase 0.5
                    val f = secPhase / (primDur * 0.9)
                    1.0 - secDepth * (1.0 - f * f).coerceAtLeast(0.0)
                } else {
                    // Reflection / gravity darkening effect
                    1.0 + 0.002 * cos(2.0 * PI * absPhase)
                }
            }
            "eclipsing_binary_contact_ew" -> {
                // Contact W UMa-type binary (EW)
                // Continuous double-wave sinusoidal variation due to tidal ellipsoidal distortion
                val amp1 = depth.coerceAtLeast(0.12)
                val amp2 = amp1 * 0.88
                1.0 - (amp1 * 0.5 * (1.0 + cos(2.0 * PI * 2.0 * phase))) -
                        (amp2 * 0.15 * (1.0 - cos(2.0 * PI * phase)))
            }
            "grazing_eclipsing_binary_v_shape" -> {
                // Grazing binary without full disk occultation (b > 1 - Rp/Rs)
                // Sharp V-shaped profile with ZERO flat bottom
                val vDepth = depth.coerceAtLeast(0.02)
                val vDur = halfDur * 1.5
                if (absPhase <= vDur) {
                    val x = absPhase / vDur
                    1.0 - vDepth * (1.0 - x)
                } else {
                    1.0
                }
            }
            "stellar_flare_davenport" -> {
                // Davenport (2014) empirical stellar flare template
                // Fast cubic rise, double-exponential cooling decay
                val flareAmp = depth.coerceAtLeast(0.03) * 5.0
                val flareCenter = 0.0
                val delta = phase - flareCenter
                if (delta < -0.04) {
                    1.0
                } else if (delta < 0.0) {
                    // Impulsive cubic rise
                    val tNorm = (delta + 0.04) / 0.04 // 0 to 1
                    1.0 + flareAmp * (tNorm * tNorm * tNorm)
                } else {
                    // Two-component exponential thermal decay
                    val t = delta * 40.0
                    val decay = 0.689 * exp(-1.6 * t) + 0.311 * exp(-0.27 * t)
                    1.0 + flareAmp * decay
                }
            }
            "starspot_rotational_bydra" -> {
                // Rotational starspot modulation (BY Draconis type)
                // Quasi-sinusoidal wave with harmonics
                val spotAmp = depth.coerceAtLeast(0.015)
                1.0 - spotAmp * (0.8 * cos(2.0 * PI * phase) + 0.2 * cos(4.0 * PI * phase + 0.4))
            }
            "pulsating_rr_lyrae" -> {
                // RR Lyrae / Cepheid pulsating variable
                // Asymmetric saw-tooth: steep rise, gradual decline
                val pulsAmp = depth.coerceAtLeast(0.20)
                val shifted = (phase + 0.5) % 1.0 // 0 to 1
                if (shifted < 0.18) {
                    // Rapid expansion / rise to maximum
                    val t = shifted / 0.18
                    1.0 - pulsAmp * 0.5 + pulsAmp * t
                } else {
                    // Slower cooling decay
                    val t = (shifted - 0.18) / 0.82
                    1.0 + pulsAmp * 0.5 - pulsAmp * t
                }
            }
            "gravitational_microlensing_paczynski" -> {
                // Paczyński (1986) point-source point-lens amplification
                // A(u) = (u^2 + 2) / (u * sqrt(u^2 + 4))
                val u0 = 0.3 // Impact parameter
                val tE = 0.15 // Einstein crossing duration
                val u = sqrt(u0 * u0 + (phase / tE) * (phase / tE))
                val amp = (u * u + 2.0) / (u * sqrt(u * u + 4.0))
                1.0 + (amp - 1.0) * 0.25 // Scaled relative amplification
            }
            else -> 1.0
        }
    }
}

/**
 * Result of cross-correlating and fitting an observed FITS light curve against
 * a pre-defined stellar light curve template.
 */
data class TemplateMatchScore(
    val template: StellarLightCurveTemplate,
    val reducedChiSquared: Double,
    val crossCorrelation: Double, // Pearson r (-1.0 to 1.0)
    val bayesPosteriorProbability: Double, // 0.0 to 1.0
    val confidencePercent: Double, // 0.0% to 100.0%
    val morphologyVerdict: String,
    val diagnosticNotes: String,
    val isBestMatch: Boolean = false
)
