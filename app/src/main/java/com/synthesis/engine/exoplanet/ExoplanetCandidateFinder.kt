package com.synthesis.engine.exoplanet

import com.synthesis.data.remote.dto.*
import com.synthesis.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow

/**
 * Multi-catalog astrophysical ranking, mass estimation, and triage anomaly engine.
 *
 * Implements:
 * 1. Transit signal-to-noise and stellar follow-up feasibility (TESS, Kepler, K2)
 * 2. Planetary radius physical plausibility and empirical mass-radius relations (Chen & Kipping 2017)
 * 3. Earth Similarity Index (ESI) calculation (Schulze-Makuch et al. 2011)
 * 4. Habitable Zone boundary screening (Kopparapu / Kasting et al.)
 * 5. Astrophysical anomaly detection: Neptunian Desert, Ultra-Short Period (USP),
 *    Super-Puffs, and temperate long-period candidates.
 */
@Singleton
class ExoplanetCandidateFinder @Inject constructor() {

    // 1. TESS Catalog Ranking
    fun rankCandidates(raw: List<TessObjectOfInterestDto>): List<ExoplanetCandidate> {
        return raw.mapNotNull { dto -> toTessCandidate(dto) }
            .sortedByDescending { it.worthInvestigatingScore }
    }

    // 2. Kepler KOI Catalog Ranking (including unvetted candidates)
    fun rankKeplerCandidates(raw: List<KeplerKoiDto>): List<ExoplanetCandidate> {
        return raw.mapNotNull { dto -> toKeplerCandidate(dto) }
            .sortedByDescending { it.worthInvestigatingScore }
    }

    // 3. K2 Candidates Ranking
    fun rankK2Candidates(raw: List<K2CandidateDto>): List<ExoplanetCandidate> {
        return raw.mapNotNull { dto -> toK2Candidate(dto) }
            .sortedByDescending { it.worthInvestigatingScore }
    }

    // 4. Confirmed Planets Benchmark (for scientific comparison)
    fun transformConfirmedPlanets(raw: List<ConfirmedExoplanetDto>): List<ExoplanetCandidate> {
        return raw.mapNotNull { dto -> toConfirmedPlanet(dto) }
            .sortedByDescending { it.earthSimilarityIndex ?: 0f }
    }

    private fun toTessCandidate(dto: TessObjectOfInterestDto): ExoplanetCandidate? {
        val toiId = dto.toi ?: return null
        val disposition = parseDisposition(dto.tfopwgDisp)
        val reasoning = mutableListOf<String>()
        var score = 0f
        var weightSum = 0f

        // Signal quality
        dto.plTranDep?.let { depth ->
            val depthScore = (depth / 5000.0).coerceIn(0.0, 1.0).toFloat()
            score += depthScore * 0.3f; weightSum += 0.3f
            if (depthScore > 0.5f) reasoning.add("Deep transit signal (${depth.toInt()} ppm) — high signal-to-noise ratio")
        }
        dto.stTmag?.let { mag ->
            val brightnessScore = ((14.0 - mag) / 8.0).coerceIn(0.0, 1.0).toFloat()
            score += brightnessScore * 0.2f; weightSum += 0.2f
            if (brightnessScore > 0.6f) reasoning.add("Bright host star (Tmag ${String.format("%.1f", mag)}) — excellent spectroscopic target")
        }

        // Physical plausibility
        dto.plRadE?.let { radius ->
            val plausibility = when {
                radius in 0.5..4.0 -> 1.0f
                radius in 4.0..15.0 -> 0.7f
                radius > 15.0 -> 0.2f
                else -> 0.4f
            }
            score += plausibility * 0.25f; weightSum += 0.25f
            if (radius in 0.5..2.0) reasoning.add("Radius (${String.format("%.2f", radius)} R⊕) is in the terrestrial/rocky planet range")
            if (radius > 15.0) reasoning.add("Radius > 15 R⊕ indicates possible eclipsing binary rather than planet")
        }

        // Mass estimation
        val estimatedMass = estimateMassFromRadius(dto.plRadE)

        // Habitable Zone / Temperate Bonus
        val inHz = isHabitableZone(dto.plEqt, dto.plInsol)
        val esi = calculateEarthSimilarityIndex(dto.plRadE, dto.plEqt)

        if (inHz) {
            score += 0.15f; weightSum += 0.15f
            reasoning.add("Insolation & temperature match the circumstellar habitable zone")
        }
        esi?.let {
            if (it >= 0.70f) reasoning.add("High Earth Similarity Index: ESI = ${String.format("%.2f", it)}")
        }

        // Time languishing in queue
        dto.toiCreated?.let { created ->
            val daysOld = daysSince(created)
            if (daysOld != null) {
                val ageScore = (daysOld / 1460.0).coerceIn(0.0, 1.0).toFloat()
                score += ageScore * 0.1f; weightSum += 0.1f
                if (daysOld > 730) reasoning.add("Flagged over ${daysOld / 365} years ago — unresolved in queue")
            }
        }

        val finalScore = if (weightSum > 0f) (score / weightSum).coerceIn(0f, 1f) else 0f
        val isUnvetted = disposition == TfopwgDisposition.PC && (finalScore >= 0.80f || (esi ?: 0f) >= 0.75f)

        val anomaly = detectAstrophysicalAnomaly(
            periodDays = dto.plOrbPer,
            radiusEarth = dto.plRadE,
            massEarth = estimatedMass,
            tempK = dto.plEqt,
            inHz = inHz
        )
        anomaly?.let { reasoning.add("Astrophysical Anomaly: $it") }

        return ExoplanetCandidate(
            toiId = "TOI-${toiId}",
            ticId = dto.tid?.toString() ?: "unknown",
            disposition = disposition,
            catalogSource = if (isUnvetted) ExoplanetCatalogSource.UNVETTED_DISCOVERY else ExoplanetCatalogSource.TESS,
            orbitalPeriodDays = dto.plOrbPer,
            transitDurationHours = dto.plTranDurH,
            transitDepthPpm = dto.plTranDep,
            planetRadiusEarth = dto.plRadE,
            planetMassEarth = estimatedMass,
            insolationEarthFlux = dto.plInsol,
            equilibriumTempK = dto.plEqt,
            stellarTessMagnitude = dto.stTmag,
            stellarDistancePc = dto.stDist,
            stellarEffTempK = dto.stTeff,
            stellarRadiusSun = dto.stRad,
            createdDate = dto.toiCreated,
            lastUpdated = dto.rowUpdate,
            worthInvestigatingScore = finalScore,
            earthSimilarityIndex = esi,
            isInHabitableZone = inHz,
            isUnvettedCandidate = isUnvetted,
            anomalyType = anomaly,
            discoveryMethod = "Transit (TESS)",
            scoreReasoning = reasoning
        )
    }

    private fun toKeplerCandidate(dto: KeplerKoiDto): ExoplanetCandidate? {
        val kepoiName = dto.kepoiName ?: return null
        val reasoning = mutableListOf<String>()
        var score = 0f
        var weightSum = 0f

        // Kepler disposition: CANDIDATE, FALSE POSITIVE, CONFIRMED
        val rawDisp = dto.koiDisposition?.uppercase() ?: dto.koiPdisposition?.uppercase() ?: "CANDIDATE"
        val disposition = when {
            rawDisp.contains("CONFIRM") -> TfopwgDisposition.CONFIRMED
            rawDisp.contains("FALSE") -> TfopwgDisposition.FP
            else -> TfopwgDisposition.CANDIDATE
        }

        dto.koiScore?.let { koiScore ->
            score += koiScore.toFloat() * 0.35f; weightSum += 0.35f
            if (koiScore >= 0.85) reasoning.add("High Kepler Robovetter confidence score (${String.format("%.2f", koiScore)})")
        }

        dto.koiPrad?.let { radius ->
            val plausibility = when {
                radius in 0.5..4.0 -> 1.0f
                radius in 4.0..15.0 -> 0.7f
                else -> 0.3f
            }
            score += plausibility * 0.25f; weightSum += 0.25f
            if (radius in 0.8..1.8) reasoning.add("Earth-size rocky planet radius: ${String.format("%.2f", radius)} R⊕")
        }

        dto.koiKepMag?.let { mag ->
            val brightnessScore = ((16.0 - mag) / 6.0).coerceIn(0.0, 1.0).toFloat()
            score += brightnessScore * 0.15f; weightSum += 0.15f
        }

        val estimatedMass = estimateMassFromRadius(dto.koiPrad)
        val inHz = isHabitableZone(dto.koiTeq, dto.koiInsol)
        val esi = calculateEarthSimilarityIndex(dto.koiPrad, dto.koiTeq)

        if (inHz) {
            score += 0.2f; weightSum += 0.2f
            reasoning.add("Located in the conservative circumstellar habitable zone")
        }
        esi?.let {
            if (it >= 0.75f) reasoning.add("High Earth Similarity Index: ESI = ${String.format("%.2f", it)}")
        }

        val isUnconfirmed = dto.keplerName.isNullOrBlank() && disposition == TfopwgDisposition.CANDIDATE
        if (isUnconfirmed) {
            reasoning.add("Archival Discovery: Unconfirmed Kepler signal — never assigned a confirmed planetary name")
        }

        val finalScore = if (weightSum > 0f) (score / weightSum).coerceIn(0f, 1f) else 0.5f

        val anomaly = detectAstrophysicalAnomaly(
            periodDays = dto.koiPeriod,
            radiusEarth = dto.koiPrad,
            massEarth = estimatedMass,
            tempK = dto.koiTeq,
            inHz = inHz
        )
        anomaly?.let { reasoning.add("Astrophysical Anomaly: $it") }

        return ExoplanetCandidate(
            toiId = kepoiName,
            ticId = "KIC ${dto.kepId ?: "unknown"}",
            disposition = disposition,
            catalogSource = if (isUnconfirmed && (dto.koiScore ?: 0.0) > 0.8) ExoplanetCatalogSource.UNVETTED_DISCOVERY else ExoplanetCatalogSource.KEPLER_KOI,
            orbitalPeriodDays = dto.koiPeriod,
            transitDurationHours = dto.koiDuration,
            transitDepthPpm = dto.koiDepth,
            planetRadiusEarth = dto.koiPrad,
            planetMassEarth = estimatedMass,
            insolationEarthFlux = dto.koiInsol,
            equilibriumTempK = dto.koiTeq,
            stellarTessMagnitude = dto.koiKepMag,
            stellarDistancePc = null,
            stellarEffTempK = dto.koiSteff,
            stellarRadiusSun = dto.koiSrad,
            createdDate = null,
            lastUpdated = null,
            worthInvestigatingScore = finalScore,
            earthSimilarityIndex = esi,
            isInHabitableZone = inHz,
            isUnvettedCandidate = isUnconfirmed,
            anomalyType = anomaly,
            discoveryMethod = "Transit (Kepler)",
            scoreReasoning = reasoning
        )
    }

    private fun toK2Candidate(dto: K2CandidateDto): ExoplanetCandidate? {
        val name = dto.epicCandName ?: dto.epicName ?: return null
        val reasoning = mutableListOf<String>()
        val inHz = isHabitableZone(dto.plEqt, dto.plInsol)
        val esi = calculateEarthSimilarityIndex(dto.plRadE, dto.plEqt)

        val disposition = if (dto.k2cDisp?.uppercase()?.contains("CONFIRM") == true) {
            TfopwgDisposition.CONFIRMED
        } else {
            TfopwgDisposition.CANDIDATE
        }

        val estimatedMass = estimateMassFromRadius(dto.plRadE)
        dto.plRadE?.let {
            if (it in 0.5..4.0) reasoning.add("Radius (${String.format("%.2f", it)} R⊕) within planetary regime")
        }
        if (inHz) reasoning.add("Equilibrium temperature fits circumstellar habitable zone")
        esi?.let { if (it > 0.7f) reasoning.add("Earth Similarity Index: ESI = ${String.format("%.2f", it)}") }

        val anomaly = detectAstrophysicalAnomaly(
            periodDays = dto.plOrbPer,
            radiusEarth = dto.plRadE,
            massEarth = estimatedMass,
            tempK = dto.plEqt,
            inHz = inHz
        )

        return ExoplanetCandidate(
            toiId = name,
            ticId = dto.epicName ?: "EPIC unknown",
            disposition = disposition,
            catalogSource = ExoplanetCatalogSource.K2,
            orbitalPeriodDays = dto.plOrbPer,
            transitDurationHours = dto.plTranDur,
            transitDepthPpm = dto.plTranDep,
            planetRadiusEarth = dto.plRadE,
            planetMassEarth = estimatedMass,
            insolationEarthFlux = dto.plInsol,
            equilibriumTempK = dto.plEqt,
            stellarTessMagnitude = dto.stOptMag,
            stellarDistancePc = null,
            stellarEffTempK = dto.stTeff,
            stellarRadiusSun = dto.stRad,
            createdDate = null,
            lastUpdated = null,
            worthInvestigatingScore = 0.75f,
            earthSimilarityIndex = esi,
            isInHabitableZone = inHz,
            isUnvettedCandidate = disposition == TfopwgDisposition.CANDIDATE,
            anomalyType = anomaly,
            discoveryMethod = "Transit (K2 Mission)",
            scoreReasoning = reasoning
        )
    }

    private fun toConfirmedPlanet(dto: ConfirmedExoplanetDto): ExoplanetCandidate? {
        val plName = dto.plName ?: return null
        val inHz = isHabitableZone(dto.plEqt, dto.plInsol)
        val esi = calculateEarthSimilarityIndex(dto.plRadE, dto.plEqt)
        val reasoning = mutableListOf<String>()

        reasoning.add("Confirmed exoplanet verified by international consensus")
        dto.discoveryMethod?.let { reasoning.add("Discovered via $it in ${dto.discYear ?: "archive"}") }
        dto.discFacility?.let { reasoning.add("Discovery facility: $it") }
        if (inHz) reasoning.add("Orbital distance lies in the host star's habitable zone")
        esi?.let { if (it >= 0.70f) reasoning.add("High Earth Similarity: ESI = ${String.format("%.2f", it)}") }

        val resolvedMass = dto.plBmassE ?: estimateMassFromRadius(dto.plRadE)

        val anomaly = detectAstrophysicalAnomaly(
            periodDays = dto.plOrbPer,
            radiusEarth = dto.plRadE,
            massEarth = resolvedMass,
            tempK = dto.plEqt,
            inHz = inHz
        )

        return ExoplanetCandidate(
            toiId = plName,
            ticId = dto.hostName ?: "Host Star",
            disposition = TfopwgDisposition.CONFIRMED,
            catalogSource = ExoplanetCatalogSource.CONFIRMED_BENCHMARK,
            orbitalPeriodDays = dto.plOrbPer,
            transitDurationHours = null,
            transitDepthPpm = null,
            planetRadiusEarth = dto.plRadE,
            planetMassEarth = resolvedMass,
            insolationEarthFlux = dto.plInsol,
            equilibriumTempK = dto.plEqt,
            stellarTessMagnitude = dto.syVmag,
            stellarDistancePc = dto.syDist,
            stellarEffTempK = dto.stTeff,
            stellarRadiusSun = dto.stRad,
            createdDate = dto.discYear?.toString(),
            lastUpdated = null,
            worthInvestigatingScore = 1.0f,
            earthSimilarityIndex = esi,
            isInHabitableZone = inHz,
            isUnvettedCandidate = false,
            anomalyType = anomaly,
            discoveryMethod = dto.discoveryMethod,
            discoveryYear = dto.discYear,
            scoreReasoning = reasoning
        )
    }

    /**
     * Standard Earth Similarity Index (ESI) calculation:
     * ESI = (1 - |(r - 1)/(r + 1)|)^0.57 * (1 - |(t - 288)/(t + 288)|)^0.70
     *
     * IMPORTANT SCIENTIFIC RULE: ESI is physically defined ONLY when BOTH radius
     * (or mass) and surface/equilibrium temperature are known.
     * If temperature is missing (null), global ESI MUST be null. It is scientifically
     * invalid to return 1.0 simply because radius is 1.0 when the planet orbits in 2 days
     * at 1000K!
     */
    fun calculateEarthSimilarityIndex(radiusEarth: Double?, tempK: Double?): Float? {
        if (radiusEarth == null || tempK == null) return null
        if (radiusEarth <= 0.0 || tempK <= 0.0) return null

        // Schulze-Makuch et al. (2011) 2-parameter formulation:
        // Weight normalization ensures proper geometric mean with exponent sum = 1.0
        // (wr = 0.57, wt = 1.07 -> wr_norm = 0.35, wt_norm = 0.65)
        val esiR = (1.0 - abs((radiusEarth - 1.0) / (radiusEarth + 1.0))).coerceIn(0.0, 1.0).pow(0.35)
        val esiT = (1.0 - abs((tempK - 288.0) / (tempK + 288.0))).coerceIn(0.0, 1.0).pow(0.65)

        val combinedEsi = (esiR * esiT).toFloat().coerceIn(0f, 1f)
        return combinedEsi
    }

    /**
     * Empirical mass-radius relation for exoplanets (Chen & Kipping 2017 / Weiss & Marcy 2014)
     * returns estimated mass in Earth masses (M⊕).
     */
    fun estimateMassFromRadius(radiusEarth: Double?): Double? {
        if (radiusEarth == null || radiusEarth <= 0.0) return null
        return when {
            radiusEarth < 1.5 -> radiusEarth.pow(3.28)
            radiusEarth in 1.5..4.0 -> 2.69 * radiusEarth.pow(1.7)
            radiusEarth in 4.0..14.0 -> (0.86 * radiusEarth.pow(1.89)).coerceAtMost(3178.0)
            else -> (318.0 * (radiusEarth / 11.2).pow(0.5)).coerceAtMost(5000.0)
        }
    }

    /**
     * Detects physical and population anomalies in the exoplanet distribution:
     * 1. Neptunian Desert: extreme atmospheric stripping zone for P < 4d, 2 < R < 7 R⊕
     * 2. Ultra-Short Period (USP): orbital period < 1 day (ultra-hot magma worlds)
     * 3. Habitable Zone Candidate: temperate temperature & insolation
     * 4. Long-Period Transit: P > 300 days (rare TESS/Kepler single/double transiters)
     */
    fun detectAstrophysicalAnomaly(
        periodDays: Double?,
        radiusEarth: Double?,
        massEarth: Double?,
        tempK: Double?,
        inHz: Boolean
    ): String? {
        val p = periodDays ?: return null
        val r = radiusEarth ?: return null
        val m = massEarth ?: estimateMassFromRadius(r) ?: return null

        return when {
            p < 4.0 && ((r in 2.0..7.0) || (m in 10.0..100.0)) -> "Neptunian Desert Anomaly"
            p < 1.0 -> "Ultra-Short Period (USP)"
            inHz -> "Habitable Zone Candidate"
            p > 300.0 -> "Long-Period Transit"
            r > 6.0 && m < 15.0 -> "Super-Puff Anomaly"
            else -> null
        }
    }

    /**
     * Checks if temperature (Kelvin) or stellar insolation (Earth Flux)
     * places the world in the circumstellar habitable zone (Kasting-Kopparapu model).
     */
    fun isHabitableZone(tempK: Double?, insolationEarthFlux: Double?): Boolean {
        if (tempK != null && tempK in 180.0..315.0) return true
        if (insolationEarthFlux != null && insolationEarthFlux in 0.35..1.75) return true
        return false
    }

    private fun parseDisposition(raw: String?): TfopwgDisposition =
        try { TfopwgDisposition.valueOf(raw?.trim()?.uppercase() ?: "") } catch (e: Exception) { TfopwgDisposition.UNKNOWN }

    private fun daysSince(isoDate: String): Long? = try {
        val date = LocalDate.parse(isoDate.take(10))
        ChronoUnit.DAYS.between(date, LocalDate.now())
    } catch (e: Exception) { null }
}
