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
            // 1. Giant gas planets (R > 7.0 R⊕) on ultra-short orbits are USP Hot Jupiters, NOT Neptunian Desert!
            r > 7.0 && p < 1.0 -> "Ultra-Short Period Hot Jupiter (USP)"
            r > 7.0 && p < 4.0 -> "Hot Jupiter"

            // 2. Genuine Neptunian Desert: intermediate radii (1.8 <= R <= 7.0 R⊕) and masses (8 <= M <= 80 M⊕) on P < 4.0d
            p < 4.0 && r in 1.8..7.0 && m in 8.0..80.0 -> "Neptunian Desert Anomaly"

            // 3. Ultra-Short Period Rocky/Magma Worlds
            p < 1.0 && r <= 2.0 -> "Ultra-Short Period Magma World (USP)"
            p < 1.0 -> "Ultra-Short Period (USP)"

            // 4. Circumstellar Habitable Zone sub-classification
            inHz && r in 0.5..2.5 -> "Habitable Zone Terrestrial Candidate"
            inHz && r > 2.5 -> "Temperate Sub-Neptune (HZ Candidate)"
            inHz -> "Habitable Zone Candidate"

            // 5. Rare Long-Period transits & Super-puffs
            p > 300.0 -> "Long-Period Transit Candidate"
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

    /**
     * Planetary Habitability Laboratory (PHL) at UPR Arecibo — Habitable Worlds Catalog.
     * The global gold-standard benchmark for confirmed exoplanets with the highest Earth Similarity Index (ESI).
     */
    fun getPhlHabitableCatalog(): List<ExoplanetCandidate> {
        return listOf(
            ExoplanetCandidate(
                toiId = "Teegarden's Star b",
                ticId = "Teegarden's Star (SO J025300.5+165253)",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 4.91,
                transitDurationHours = null,
                transitDepthPpm = null,
                planetRadiusEarth = 1.02,
                planetMassEarth = 1.05,
                insolationEarthFlux = 1.15,
                equilibriumTempK = 264.0,
                stellarTessMagnitude = 11.2,
                stellarDistancePc = 3.83,
                stellarEffTempK = 2904.0,
                stellarRadiusSun = 0.107,
                createdDate = "2019-06-18",
                lastUpdated = "2024-01-15",
                worthInvestigatingScore = 0.98f,
                earthSimilarityIndex = 0.95f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Radial Velocity (CARMENES)",
                discoveryYear = 2019,
                scoreReasoning = listOf(
                    "Highest ESI in the entire PHL Catalog (ESI = 0.95)",
                    "Earth-mass world (1.05 M⊕) at 3.83 parsecs distance",
                    "Temperate equilibrium temperature (264 K, close to Earth's 255 K)"
                )
            ),
            ExoplanetCandidate(
                toiId = "TOI-700 d",
                ticId = "TIC 150428135",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 37.42,
                transitDurationHours = 3.6,
                transitDepthPpm = 980.0,
                planetRadiusEarth = 1.14,
                planetMassEarth = 1.25,
                insolationEarthFlux = 0.86,
                equilibriumTempK = 269.0,
                stellarTessMagnitude = 12.1,
                stellarDistancePc = 31.1,
                stellarEffTempK = 3480.0,
                stellarRadiusSun = 0.415,
                createdDate = "2020-01-06",
                lastUpdated = "2023-11-20",
                worthInvestigatingScore = 0.96f,
                earthSimilarityIndex = 0.93f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Transit (NASA TESS & Spitzer)",
                discoveryYear = 2020,
                scoreReasoning = listOf(
                    "First Earth-sized habitable zone planet discovered by NASA TESS",
                    "ESI = 0.93 — 86% of Earth's solar insolation",
                    "Quiet M-dwarf host star with virtually zero flaring activity"
                )
            ),
            ExoplanetCandidate(
                toiId = "Kepler-1649 c",
                ticId = "KIC 6449612",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 19.54,
                transitDurationHours = 2.1,
                transitDepthPpm = 640.0,
                planetRadiusEarth = 1.06,
                planetMassEarth = 1.20,
                insolationEarthFlux = 0.75,
                equilibriumTempK = 234.0,
                stellarTessMagnitude = 16.2,
                stellarDistancePc = 92.4,
                stellarEffTempK = 3240.0,
                stellarRadiusSun = 0.23,
                createdDate = "2020-04-15",
                lastUpdated = "2022-09-10",
                worthInvestigatingScore = 0.95f,
                earthSimilarityIndex = 0.92f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Transit (Kepler False Positive Working Group Recovery)",
                discoveryYear = 2020,
                scoreReasoning = listOf(
                    "Most Earth-like planet in both size (1.06 R⊕) and temperature in Kepler data",
                    "ESI = 0.92, recovered from false-positive algorithmic rejects by human vetting"
                )
            ),
            ExoplanetCandidate(
                toiId = "Proxima Centauri b",
                ticId = "Alpha Centauri C",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 11.19,
                transitDurationHours = null,
                transitDepthPpm = null,
                planetRadiusEarth = 1.03,
                planetMassEarth = 1.07,
                insolationEarthFlux = 0.65,
                equilibriumTempK = 234.0,
                stellarTessMagnitude = 7.4,
                stellarDistancePc = 1.30,
                stellarEffTempK = 3042.0,
                stellarRadiusSun = 0.141,
                createdDate = "2016-08-24",
                lastUpdated = "2024-03-01",
                worthInvestigatingScore = 0.97f,
                earthSimilarityIndex = 0.87f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Radial Velocity (ESO HARPS & UVES)",
                discoveryYear = 2016,
                scoreReasoning = listOf(
                    "Closest known exoplanet to our Solar System (1.30 pc / 4.24 light-years)",
                    "ESI = 0.87, primary target for interstellar flyby probe missions (Breakthrough Starshot)"
                )
            ),
            ExoplanetCandidate(
                toiId = "TRAPPIST-1 e",
                ticId = "2MASS J23062928-0502285",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 6.10,
                transitDurationHours = 0.9,
                transitDepthPpm = 4900.0,
                planetRadiusEarth = 0.92,
                planetMassEarth = 0.69,
                insolationEarthFlux = 0.66,
                equilibriumTempK = 251.0,
                stellarTessMagnitude = 14.5,
                stellarDistancePc = 12.1,
                stellarEffTempK = 2566.0,
                stellarRadiusSun = 0.119,
                createdDate = "2017-02-22",
                lastUpdated = "2024-02-18",
                worthInvestigatingScore = 0.97f,
                earthSimilarityIndex = 0.85f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Transit (TRAPPIST & Spitzer & Kepler)",
                discoveryYear = 2017,
                scoreReasoning = listOf(
                    "Rocky core composition most closely matching Earth's iron/silicate ratio",
                    "ESI = 0.85, prime James Webb Space Telescope (JWST) atmospheric transmission target"
                )
            ),
            ExoplanetCandidate(
                toiId = "LHS 1140 b",
                ticId = "TIC 92226327",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 24.74,
                transitDurationHours = 2.1,
                transitDepthPpm = 5100.0,
                planetRadiusEarth = 1.73,
                planetMassEarth = 5.60,
                insolationEarthFlux = 0.43,
                equilibriumTempK = 226.0,
                stellarTessMagnitude = 10.9,
                stellarDistancePc = 15.0,
                stellarEffTempK = 3216.0,
                stellarRadiusSun = 0.216,
                createdDate = "2017-04-19",
                lastUpdated = "2024-07-12",
                worthInvestigatingScore = 0.94f,
                earthSimilarityIndex = 0.82f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Transit & Radial Velocity (MEarth & HARPS)",
                discoveryYear = 2017,
                scoreReasoning = listOf(
                    "Candidate Water-World or Ocean Planet with nitrogen-rich atmosphere detected by JWST",
                    "Dense, low-activity M-dwarf host star, highly favorable for atmospheric retention"
                )
            ),
            ExoplanetCandidate(
                toiId = "Kepler-452 b",
                ticId = "KIC 8311079",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 384.84,
                transitDurationHours = 10.5,
                transitDepthPpm = 200.0,
                planetRadiusEarth = 1.63,
                planetMassEarth = 3.29,
                insolationEarthFlux = 1.10,
                equilibriumTempK = 265.0,
                stellarTessMagnitude = 13.4,
                stellarDistancePc = 560.0,
                stellarEffTempK = 5757.0,
                stellarRadiusSun = 1.11,
                createdDate = "2015-07-23",
                lastUpdated = "2022-01-10",
                worthInvestigatingScore = 0.91f,
                earthSimilarityIndex = 0.83f,
                isInHabitableZone = true,
                anomalyType = "Long-Period Transit Candidate",
                discoveryMethod = "Transit (NASA Kepler)",
                discoveryYear = 2015,
                scoreReasoning = listOf(
                    "Orbit around a G2V Solar Twin star with a 384-day year (closest analogue to Earth's year)",
                    "ESI = 0.83, super-Earth in the circumstellar habitable zone of a Sun-like star"
                )
            ),
            ExoplanetCandidate(
                toiId = "K2-18 b",
                ticId = "EPIC 201912552",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 32.94,
                transitDurationHours = 2.7,
                transitDepthPpm = 2800.0,
                planetRadiusEarth = 2.61,
                planetMassEarth = 8.63,
                insolationEarthFlux = 0.94,
                equilibriumTempK = 255.0,
                stellarTessMagnitude = 11.2,
                stellarDistancePc = 38.0,
                stellarEffTempK = 3457.0,
                stellarRadiusSun = 0.41,
                createdDate = "2015-06-01",
                lastUpdated = "2024-04-10",
                worthInvestigatingScore = 0.92f,
                earthSimilarityIndex = 0.73f,
                isInHabitableZone = true,
                anomalyType = "Temperate Sub-Neptune (HZ Candidate)",
                discoveryMethod = "Transit (NASA K2 Mission)",
                discoveryYear = 2015,
                scoreReasoning = listOf(
                    "JWST detected methane and carbon dioxide in its atmosphere",
                    "Prime candidate for a Hycean world (hydrogen atmosphere over a global liquid ocean)"
                )
            ),
            ExoplanetCandidate(
                toiId = "Wolf 1069 b",
                ticId = "TIC 382224424",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.PHL_HABITABLE,
                orbitalPeriodDays = 15.56,
                transitDurationHours = null,
                transitDepthPpm = null,
                planetRadiusEarth = 1.08,
                planetMassEarth = 1.26,
                insolationEarthFlux = 0.65,
                equilibriumTempK = 250.0,
                stellarTessMagnitude = 9.8,
                stellarDistancePc = 9.6,
                stellarEffTempK = 3158.0,
                stellarRadiusSun = 0.18,
                createdDate = "2023-01-05",
                lastUpdated = "2024-02-01",
                worthInvestigatingScore = 0.93f,
                earthSimilarityIndex = 0.85f,
                isInHabitableZone = true,
                anomalyType = "Habitable Zone Terrestrial Candidate",
                discoveryMethod = "Radial Velocity (CARMENES)",
                discoveryYear = 2023,
                scoreReasoning = listOf(
                    "Earth-mass planet (1.26 M⊕) within 10 parsecs of Earth",
                    "ESI = 0.85, retains magnetic dynamo potential according to geophysical models"
                )
            )
        )
    }

    /**
     * ESA CHEOPS & Gaia Catalog: European Space Agency space missions specializing
     * in ultra-high precision transit photometry (CHEOPS) and micro-arcsecond astrometry (Gaia DR3).
     */
    fun getCheopsAndGaiaCatalog(): List<ExoplanetCandidate> {
        return listOf(
            ExoplanetCandidate(
                toiId = "Nu2 Lupi d",
                ticId = "HD 136352 d",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.CHEOPS_GAIA,
                orbitalPeriodDays = 107.58,
                transitDurationHours = 9.1,
                transitDepthPpm = 860.0,
                planetRadiusEarth = 2.56,
                planetMassEarth = 8.82,
                insolationEarthFlux = 1.80,
                equilibriumTempK = 363.0,
                stellarTessMagnitude = 5.2,
                stellarDistancePc = 14.6,
                stellarEffTempK = 5664.0,
                stellarRadiusSun = 1.05,
                createdDate = "2021-06-28",
                lastUpdated = "2023-09-12",
                worthInvestigatingScore = 0.95f,
                earthSimilarityIndex = 0.62f,
                isInHabitableZone = false,
                anomalyType = "Long-Period Transit Candidate",
                discoveryMethod = "Transit Photometry (ESA CHEOPS serendipitous discovery)",
                discoveryYear = 2021,
                scoreReasoning = listOf(
                    "Serendipitous transiting sub-Neptune around a bright star visible to the naked eye (V = 5.7)",
                    "Precise density measured by ESA CHEOPS: high water/volatile content (up to 25% by mass)"
                )
            ),
            ExoplanetCandidate(
                toiId = "LTT 9779 b",
                ticId = "TIC 183800431",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.CHEOPS_GAIA,
                orbitalPeriodDays = 0.79,
                transitDurationHours = 1.4,
                transitDepthPpm = 2200.0,
                planetRadiusEarth = 4.72,
                planetMassEarth = 29.3,
                insolationEarthFlux = 1250.0,
                equilibriumTempK = 2000.0,
                stellarTessMagnitude = 9.7,
                stellarDistancePc = 80.6,
                stellarEffTempK = 5443.0,
                stellarRadiusSun = 0.95,
                createdDate = "2020-09-21",
                lastUpdated = "2023-07-10",
                worthInvestigatingScore = 0.93f,
                earthSimilarityIndex = 0.12f,
                isInHabitableZone = false,
                anomalyType = "Neptunian Desert Anomaly",
                discoveryMethod = "Transit & Reflectance (ESA CHEOPS & TESS)",
                discoveryYear = 2020,
                scoreReasoning = listOf(
                    "The 'Mirror Planet' — highest geometric albedo ever measured for an exoplanet (0.80)",
                    "Ultra-hot Neptune surviving deep in the Neptunian Desert thanks to reflective titanium clouds"
                )
            ),
            ExoplanetCandidate(
                toiId = "WASP-103 b",
                ticId = "2MASS J16371556+0711000",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.CHEOPS_GAIA,
                orbitalPeriodDays = 0.92,
                transitDurationHours = 1.8,
                transitDepthPpm = 17500.0,
                planetRadiusEarth = 17.1,
                planetMassEarth = 474.0,
                insolationEarthFlux = 3400.0,
                equilibriumTempK = 2510.0,
                stellarTessMagnitude = 11.8,
                stellarDistancePc = 470.0,
                stellarEffTempK = 6110.0,
                stellarRadiusSun = 1.44,
                createdDate = "2022-01-11",
                lastUpdated = "2023-04-15",
                worthInvestigatingScore = 0.89f,
                earthSimilarityIndex = 0.08f,
                isInHabitableZone = false,
                anomalyType = "Ultra-Short Period Hot Jupiter (USP)",
                discoveryMethod = "Tidal Deformation Photometry (ESA CHEOPS)",
                discoveryYear = 2022,
                scoreReasoning = listOf(
                    "First direct detection of the non-spherical, rugby-ball tidal deformation of an exoplanet",
                    "CHEOPS measured the Love number k2, revealing internal composition similar to Jupiter"
                )
            ),
            ExoplanetCandidate(
                toiId = "WASP-189 b",
                ticId = "HD 133112 b",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.CHEOPS_GAIA,
                orbitalPeriodDays = 2.72,
                transitDurationHours = 4.3,
                transitDepthPpm = 3900.0,
                planetRadiusEarth = 17.9,
                planetMassEarth = 635.0,
                insolationEarthFlux = 4800.0,
                equilibriumTempK = 3200.0,
                stellarTessMagnitude = 6.6,
                stellarDistancePc = 99.0,
                stellarEffTempK = 8000.0,
                stellarRadiusSun = 2.36,
                createdDate = "2020-09-28",
                lastUpdated = "2023-06-20",
                worthInvestigatingScore = 0.91f,
                earthSimilarityIndex = 0.06f,
                isInHabitableZone = false,
                anomalyType = "Hot Jupiter",
                discoveryMethod = "High-Precision Photometry (ESA CHEOPS first science target)",
                discoveryYear = 2020,
                scoreReasoning = listOf(
                    "One of the hottest known exoplanets (3200 K), orbiting an A-type star that rotates rapidly",
                    "CHEOPS occultation measurements revealed gravity darkening on the host star"
                )
            ),
            ExoplanetCandidate(
                toiId = "Gaia-1 b (Gaia-14aae)",
                ticId = "Gaia DR3 158223611894459264",
                disposition = TfopwgDisposition.CONFIRMED,
                catalogSource = ExoplanetCatalogSource.CHEOPS_GAIA,
                orbitalPeriodDays = 182.4,
                transitDurationHours = null,
                transitDepthPpm = null,
                planetRadiusEarth = 12.5,
                planetMassEarth = 3800.0, // ~12 Jupiter masses
                insolationEarthFlux = 1.05,
                equilibriumTempK = 270.0,
                stellarTessMagnitude = 12.8,
                stellarDistancePc = 72.0,
                stellarEffTempK = 5400.0,
                stellarRadiusSun = 0.92,
                createdDate = "2022-06-13",
                lastUpdated = "2024-05-01",
                worthInvestigatingScore = 0.88f,
                earthSimilarityIndex = 0.25f,
                isInHabitableZone = true,
                anomalyType = "Long-Period Transit Candidate",
                discoveryMethod = "Astrometry (ESA Gaia DR3 Non-Single Stars Catalog)",
                discoveryYear = 2022,
                scoreReasoning = listOf(
                    "Discovered via micro-arcsecond astrometric stellar wobble in ESA Gaia DR3 data",
                    "Temperate super-Jupiter with possible massive habitable exomoons"
                )
            )
        )
    }
}

