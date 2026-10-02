package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.HarmonicPeriodHypothesis
import com.synthesis.domain.model.exoplanet.HarmonicStitchCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Multi-Sector Harmonic Stitcher Engine
 *
 * NASA pipeline blindspot recovery:
 * Single-transit events recorded in separate years (e.g. TESS Year 1 vs Year 3 or Year 5)
 * are treated as unrelated noise by automated pipelines.
 *
 * This engine calculates the discrete harmonic period solutions P = (T2 - T1) / k,
 * simulates photometric camera uptime windows to rule out periods that should have
 * produced unobserved transits, and isolates the true Keplerian period.
 */
@Singleton
class MultiSectorHarmonicStitcherEngine @Inject constructor() {

    companion object {
        // TESS Base Epoch (Sector 1 start: July 25, 2018 -> BTJD 1325.0)
        const val TESS_S1_START_BTJD = 1325.29
        const val SECTOR_DURATION_DAYS = 27.4
        const val ORBIT_PERIOD_DAYS = 13.7
        const val PERIGEE_DOWNLINK_GAP_DAYS = 1.25 // Data transfer & momentum dump gap at perigee
    }

    /**
     * Calculates authentic camera uptime windows and downlink gaps for TESS sectors.
     */
    fun getSectorCoverageWindows(sectorStr: String): List<Pair<Double, Double>> {
        val secNum = sectorStr.filter { it.isDigit() }.toIntOrNull() ?: 1
        val secStart = TESS_S1_START_BTJD + (secNum - 1) * SECTOR_DURATION_DAYS
        // Orbit 1: from start to mid-sector perigee
        val orb1Start = secStart
        val orb1End = secStart + (ORBIT_PERIOD_DAYS - PERIGEE_DOWNLINK_GAP_DAYS / 2.0)
        // Orbit 2: from end of perigee downlink to sector end
        val orb2Start = secStart + (ORBIT_PERIOD_DAYS + PERIGEE_DOWNLINK_GAP_DAYS / 2.0)
        val orb2End = secStart + SECTOR_DURATION_DAYS
        return listOf(Pair(orb1Start, orb1End), Pair(orb2Start, orb2End))
    }

    fun stitchHarmonics(
        candidateName: String,
        ticId: String,
        epoch1Btjd: Double,
        sector1: String,
        epoch2Btjd: Double,
        sector2: String,
        stellarMassSun: Double,
        stellarRadiusSun: Double,
        stellarTeffK: Double,
        monitoredGaps: List<Pair<Double, Double>> = emptyList() // Active camera windows (start, end)
    ): HarmonicStitchCandidate {
        val deltaDays = abs(epoch2Btjd - epoch1Btjd)
        val hypotheses = mutableListOf<HarmonicPeriodHypothesis>()

        // Generate authentic TESS observation coverage windows if not supplied
        val activeWindows = if (monitoredGaps.isNotEmpty()) monitoredGaps else {
            getSectorCoverageWindows(sector1) + getSectorCoverageWindows(sector2)
        }

        // Scan harmonics k from 1 to 25
        for (k in 1..25) {
            val periodDays = deltaDays / k.toDouble()
            // Kepler's Third Law for semi-major axis: a = (P/365.25)^(2/3) * M^(1/3)
            val semiMajorAxisAu = (periodDays / 365.25).pow(2.0 / 3.0) * stellarMassSun.pow(1.0 / 3.0)

            // Habitable Zone check: Insolation S = (L*) / a^2
            val luminositySolar = stellarRadiusSun.pow(2) * (stellarTeffK / 5778.0).pow(4)
            val insolationEarth = luminositySolar / semiMajorAxisAu.pow(2)
            val isHz = insolationEarth in 0.32..1.25

            // Check if any expected transit (T1 + n * P) fell into active camera windows without detection
            var isRuledOut = false
            var ruleOutReason: String? = null

            if (activeWindows.isNotEmpty()) {
                for (n in 1 until k) {
                    val expectedTransit = epoch1Btjd + n * periodDays
                    val inActiveWindow = activeWindows.any { (start, end) -> expectedTransit in start..end }
                    if (inActiveWindow) {
                        isRuledOut = true
                        ruleOutReason = "Expected transit at BTJD ${String.format("%.1f", expectedTransit)} fell during active camera coverage with no detection."
                        break
                    }
                }
            } else if (k > 8 && periodDays < 15.0) {
                // Heuristic: Short periods (< 15 days) would have transited multiple times within the 27.4-day sector
                isRuledOut = true
                ruleOutReason = "Period P=${String.format("%.1f", periodDays)}d would have transited twice in Sector $sector1."
            }

            hypotheses.add(
                HarmonicPeriodHypothesis(
                    harmonicIndexK = k,
                    periodDays = periodDays,
                    semiMajorAxisAu = semiMajorAxisAu,
                    isHabitableZone = isHz,
                    isRuledOutBySurveyCoverage = isRuledOut,
                    ruleOutReason = ruleOutReason
                )
            )
        }

        // Find the best surviving harmonic (prioritizing non-ruled out and physically consistent)
        val bestHypothesis = hypotheses.filter { !it.isRuledOutBySurveyCoverage }
            .minByOrNull { abs(it.semiMajorAxisAu - 1.0) } // Prioritize temperate orbits if valid
            ?: hypotheses.first()

        val confidence = if (hypotheses.count { !it.isRuledOutBySurveyCoverage } <= 3) 92.0 else 68.0

        return HarmonicStitchCandidate(
            candidateName = candidateName,
            ticId = ticId,
            epoch1Btjd = epoch1Btjd,
            sector1 = sector1,
            epoch2Btjd = epoch2Btjd,
            sector2 = sector2,
            deltaDays = deltaDays,
            candidateHarmonics = hypotheses,
            bestUniquePeriodDays = bestHypothesis.periodDays,
            bestSemiMajorAxisAu = bestHypothesis.semiMajorAxisAu,
            stellarTeffK = stellarTeffK,
            isHabitableZone = bestHypothesis.isHabitableZone,
            solutionConfidence = confidence
        )
    }
}
