package com.synthesis.engine.exoplanet

import com.synthesis.data.remote.dto.TessObjectOfInterestDto
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.TfopwgDisposition
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Re-ranks TESS's own unconfirmed candidates (disposition PC / APC) using only
 * parameters NASA itself has already published for that object. This does NOT
 * detect new transits from raw light curves — it highlights which of the
 * thousands of already-flagged-but-unresolved candidates look statistically
 * most solid and most overdue for follow-up, using the same kind of reasoning
 * professional vetting teams use as a first triage pass:
 *
 *  - deep, clean transit signal relative to a bright/quiet host star
 *  - a plausible planetary radius (very large radii are usually eclipsing binaries)
 *  - how long it has sat unconfirmed (older = more likely overlooked, not just slow)
 *
 * This is a triage/prioritization score, not a confirmation. Real confirmation
 * requires follow-up observations (radial velocity, additional transits) that
 * only professional telescope time can provide.
 */
@Singleton
class ExoplanetCandidateFinder @Inject constructor() {

    fun rankCandidates(raw: List<TessObjectOfInterestDto>): List<ExoplanetCandidate> {
        return raw.mapNotNull { dto -> toCandidate(dto) }
            .sortedByDescending { it.worthInvestigatingScore }
    }

    private fun toCandidate(dto: TessObjectOfInterestDto): ExoplanetCandidate? {
        val toiId = dto.toi ?: return null
        val disposition = parseDisposition(dto.tfopwgDisp)

        val reasoning = mutableListOf<String>()
        var score = 0f
        var weightSum = 0f

        // 1. Signal quality: deeper transit + brighter star = easier, more trustworthy signal
        dto.plTranDep?.let { depth ->
            val depthScore = (depth / 5000.0).coerceIn(0.0, 1.0).toFloat() // 5000ppm+ is a very deep, obvious transit
            score += depthScore * 0.3f; weightSum += 0.3f
            if (depthScore > 0.5f) reasoning.add("Deep transit signal (${depth.toInt()} ppm) — hard to miss, easy to confirm")
        }
        dto.stTmag?.let { mag ->
            // TESS magnitude: lower number = brighter star = better signal-to-noise
            val brightnessScore = ((14.0 - mag) / 8.0).coerceIn(0.0, 1.0).toFloat()
            score += brightnessScore * 0.2f; weightSum += 0.2f
            if (brightnessScore > 0.6f) reasoning.add("Bright host star (Tmag ${String.format("%.1f", mag)}) — good follow-up target")
        }

        // 2. Physical plausibility: radii far outside typical planet range often mean eclipsing binary
        dto.plRadE?.let { radius ->
            val plausibility = when {
                radius in 0.5..4.0 -> 1.0f   // rocky to sub-Neptune — very plausible
                radius in 4.0..15.0 -> 0.7f  // gas giant range — plausible
                radius > 15.0 -> 0.2f        // likely eclipsing binary, not a planet
                else -> 0.4f
            }
            score += plausibility * 0.25f; weightSum += 0.25f
            if (radius in 0.5..2.0) reasoning.add("Radius (${String.format("%.2f", radius)} R⊕) is in the rocky-planet range")
            if (radius > 15.0) reasoning.add("Radius unusually large for a planet — elevated eclipsing-binary risk")
        }

        // 3. Temperate zone bonus — genuinely more scientifically interesting, not more "valuable"
        dto.plEqt?.let { eqt ->
            if (eqt in 180.0..320.0) {
                score += 0.1f; weightSum += 0.1f
                reasoning.add("Equilibrium temperature (${eqt.toInt()}K) falls in a temperate range")
            } else {
                weightSum += 0.1f
            }
        }

        // 4. Age in the unconfirmed queue — older PC/APC entries are more likely to have been
        // overlooked simply due to limited follow-up telescope time, not because they're weak signals
        dto.toiCreated?.let { created ->
            val daysOld = daysSince(created)
            if (daysOld != null) {
                val ageScore = (daysOld / 1460.0).coerceIn(0.0, 1.0).toFloat() // caps around 4 years
                score += ageScore * 0.15f; weightSum += 0.15f
                if (daysOld > 730) reasoning.add("Flagged over ${daysOld / 365} years ago and still unresolved — likely stuck behind higher-priority targets, not disqualified")
            }
        }

        val finalScore = if (weightSum > 0f) (score / weightSum).coerceIn(0f, 1f) else 0f
        if (reasoning.isEmpty()) reasoning.add("Limited published parameters available — worth a manual look at the raw TESS light curve")

        return ExoplanetCandidate(
            toiId = "TOI-${toiId}",
            ticId = dto.tid?.toString() ?: "unknown",
            disposition = disposition,
            orbitalPeriodDays = dto.plOrbPer,
            transitDurationHours = dto.plTranDurH,
            transitDepthPpm = dto.plTranDep,
            planetRadiusEarth = dto.plRadE,
            insolationEarthFlux = dto.plInsol,
            equilibriumTempK = dto.plEqt,
            stellarTessMagnitude = dto.stTmag,
            stellarDistancePc = dto.stDist,
            stellarEffTempK = dto.stTeff,
            stellarRadiusSun = dto.stRad,
            createdDate = dto.toiCreated,
            lastUpdated = dto.rowUpdate,
            worthInvestigatingScore = finalScore,
            scoreReasoning = reasoning
        )
    }

    private fun parseDisposition(raw: String?): TfopwgDisposition =
        try { TfopwgDisposition.valueOf(raw?.trim()?.uppercase() ?: "") } catch (e: Exception) { TfopwgDisposition.UNKNOWN }

    private fun daysSince(isoDate: String): Long? = try {
        val date = LocalDate.parse(isoDate.take(10))
        ChronoUnit.DAYS.between(date, LocalDate.now())
    } catch (e: Exception) { null }
}
