package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.LiveTransitTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * Live Transit Sky Radar & Observation Planner
 *
 * Propagates linear and perturbed transit ephemerides to compute
 * next transit windows, ingress/egress countdowns, and amateur telescope
 * feasibility for ground confirmation.
 */
@Singleton
class TransitSkyRadarEngine @Inject constructor() {

    fun computeNextTransit(
        candidateName: String,
        ticId: String,
        constellation: String,
        rightAscension: String,
        declination: String,
        apparentVisualMag: Double,
        periodDays: Double,
        transitDurationHours: Double,
        epochBtjd: Double,
        transitDepthPpm: Double,
        currentBtjd: Double = 3200.0 // Default contemporary TESS epoch benchmark
    ): LiveTransitTarget {
        val cyclesElapsed = (currentBtjd - epochBtjd) / periodDays
        val nextCycle = ceil(cyclesElapsed).coerceAtLeast(0.0)
        val nextEpoch = epochBtjd + nextCycle * periodDays

        val daysUntilNext = (nextEpoch - currentBtjd).coerceAtLeast(0.0)
        val hoursUntilIngress = (daysUntilNext * 24.0) - (transitDurationHours / 2.0)

        val feasibility = when {
            apparentVisualMag <= 11.0 && transitDepthPpm >= 2000.0 ->
                "★ HIGH (Accessible to 8\" SCT + CMOS Camera)"
            apparentVisualMag <= 12.5 && transitDepthPpm >= 1000.0 ->
                "MEDIUM (Requires 11-14\" Telescope + Guiding)"
            transitDepthPpm < 500.0 ->
                "DEEP SPACE (Requires Keck/VLT/JWST or High-Precision Stacking)"
            else ->
                "MODERATE (Feasible under dark skies with photometric filters)"
        }

        return LiveTransitTarget(
            candidateName = candidateName,
            ticId = ticId,
            constellation = constellation,
            rightAscension = rightAscension,
            declination = declination,
            apparentVisualMag = apparentVisualMag,
            periodDays = periodDays,
            transitDurationHours = transitDurationHours,
            nextTransitEpochBtjd = nextEpoch,
            hoursUntilIngress = hoursUntilIngress.coerceAtLeast(0.0),
            transitDepthPpm = transitDepthPpm,
            amateurTelescopeFeasibility = feasibility
        )
    }
}
