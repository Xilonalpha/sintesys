package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.MonoTransitCandidate
import com.synthesis.domain.model.exoplanet.PipelineBlindspotCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Mono-Transit Inversion Engine
 *
 * NASA SPOC pipeline blindspot recovery:
 * SPOC automated Box Least Squares (BLS) requires >= 2 or 3 transits in a 27.4-day TESS sector.
 * Temperate planets in the Habitable Zone around Solar-type stars (P > 100-365 days) only transit ONCE
 * per sector and are systematically discarded as instrumental or attitude-control anomalies.
 *
 * This engine applies the Seager & Mallen-Ornelas (2003) circular orbit inversion to recover
 * semi-major axis, orbital period, insolation, and habitability from a single transit profile.
 */
@Singleton
class MonoTransitHunterEngine @Inject constructor(
    private val exoplanetCandidateFinder: ExoplanetCandidateFinder
) {
    // Physical Constants (SI)
    private val G = 6.67430e-11        // Gravitational constant (m^3 kg^-1 s^-2)
    private val M_SUN = 1.989e30       // Solar mass in kg
    private val R_SUN = 6.9634e8       // Solar radius in meters
    private val R_EARTH = 6.3781e6     // Earth radius in meters
    private val AU_METERS = 1.4959787e11
    private val SECONDS_PER_DAY = 86400.0

    /**
     * Inverts a single transit profile into complete orbital and planetary parameters.
     * Assumes circular orbit (eccentricity e = 0) as minimum-energy baseline.
     */
    fun analyzeSingleTransit(
        ticId: String,
        candidateName: String,
        sector: String,
        epochBtjd: Double,
        transitDurationHours: Double,
        transitDepthPpm: Double,
        stellarTeffK: Double,
        stellarRadiusSun: Double,
        stellarMassSun: Double,
        stellarDistancePc: Double?,
        apparentMagV: Double,
        impactParameter: Double = 0.0 // Default central transit (b = 0)
    ): MonoTransitCandidate {
        val tauSeconds = transitDurationHours * 3600.0
        val depthFraction = (transitDepthPpm / 1_000_000.0).coerceAtLeast(1e-7)

        val mStarKg = stellarMassSun * M_SUN
        val rStarMeters = stellarRadiusSun * R_SUN

        // 1. Planetary Radius: Rp = R* * sqrt(depth)
        val planetRadiusMeters = rStarMeters * sqrt(depthFraction)
        val planetRadiusEarth = planetRadiusMeters / R_EARTH

        // 2. Circular Semi-Major Axis Inversion:
        // Orbital velocity: v_c = sqrt(G * M* / a)
        // Central transit duration: tau = 2 * R* * sqrt(1 - b^2) / v_c
        // => sqrt(a) = tau * sqrt(G * M*) / (2 * R* * sqrt(1 - b^2))
        val bClamped = impactParameter.coerceIn(0.0, 0.95)
        val sqrtChordCorrection = sqrt(1.0 - bClamped.pow(2))
        val sqrtA = (tauSeconds * sqrt(G * mStarKg)) / (2.0 * rStarMeters * sqrtChordCorrection)
        val semiMajorAxisMeters = sqrtA.pow(2)
        val semiMajorAxisAu = semiMajorAxisMeters / AU_METERS

        // 3. Orbital Period via Kepler's Third Law: P = 2*pi * sqrt(a^3 / (G * M*))
        val periodSeconds = 2.0 * Math.PI * sqrt(semiMajorAxisMeters.pow(3) / (G * mStarKg))
        val circularPeriodDays = periodSeconds / SECONDS_PER_DAY

        // Minimum possible period based on stellar boundary crossing
        val minPeriodDays = (circularPeriodDays * 0.70).coerceAtLeast(transitDurationHours / 24.0 * 2.0)

        // 4. Stellar Luminosity & Insolation:
        // L* / L_sun = (R* / R_sun)^2 * (Teff / 5778)^4
        val stellarLuminositySolar = stellarRadiusSun.pow(2) * (stellarTeffK / 5778.0).pow(4)
        val insolationEarth = stellarLuminositySolar / semiMajorAxisAu.pow(2)

        // 5. Equilibrium Temperature (assuming Bond albedo A = 0.3):
        // Teq = Teff * sqrt(R* / (2 * a)) * (1 - A)^0.25
        val rStarAu = rStarMeters / AU_METERS
        val teqK = stellarTeffK * sqrt(rStarAu / (2.0 * semiMajorAxisAu)) * (1.0 - 0.3).pow(0.25)

        // 6. Circumstellar Habitable Zone Evaluation (Kopparapu et al. 2013 / Kasting)
        // Conservative HZ insolation limits: ~0.35 S_earth (maximum greenhouse) to ~1.15 S_earth (runaway greenhouse)
        val isHz = insolationEarth in 0.32..1.25

        // 7. Earth Similarity Index
        val esi = exoplanetCandidateFinder.calculateEarthSimilarityIndex(planetRadiusEarth, teqK)

        val notes = buildString {
            append("SPOC single-transit recovery: duration=${String.format("%.2f", transitDurationHours)}h, depth=${String.format("%.0f", transitDepthPpm)}ppm. ")
            append("Inverted orbit: a=${String.format("%.3f", semiMajorAxisAu)} AU, estimated P_circ=${String.format("%.1f", circularPeriodDays)} d. ")
            if (isHz) {
                append("★ LOCATED IN CIRCUMSTELLAR HABITABLE ZONE (S=${String.format("%.2f", insolationEarth)} S⊕, Teq=${teqK.roundToInt()} K). ")
            }
            append("Missed by NASA due to single transit occurrence within 27.4-day sector window.")
        }

        return MonoTransitCandidate(
            ticId = ticId,
            candidateName = candidateName,
            sector = sector,
            epochBtjd = epochBtjd,
            transitDurationHours = transitDurationHours,
            transitDepthPpm = transitDepthPpm,
            stellarTeffK = stellarTeffK,
            stellarRadiusSun = stellarRadiusSun,
            stellarMassSun = stellarMassSun,
            stellarDistancePc = stellarDistancePc,
            apparentMagV = apparentMagV,
            planetRadiusEarth = planetRadiusEarth,
            minimumPeriodDays = minPeriodDays,
            circularEstimatedPeriodDays = circularPeriodDays,
            semiMajorAxisAu = semiMajorAxisAu,
            stellarInsolationEarth = insolationEarth,
            equilibriumTempK = teqK,
            isHabitableZone = isHz,
            earthSimilarityIndex = esi,
            blindspotCategory = PipelineBlindspotCategory.MONO_TRANSIT_LONG_PERIOD,
            detectionNotes = notes
        )
    }
}
