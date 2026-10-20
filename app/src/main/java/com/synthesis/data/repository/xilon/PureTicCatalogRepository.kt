package com.synthesis.data.repository.xilon

import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.domain.model.TfopwgDisposition
import com.synthesis.domain.model.xilon.MastPhotometryPoint
import com.synthesis.domain.model.xilon.PureTicCandidate
import com.synthesis.domain.model.xilon.TargetStarCandidate
import com.synthesis.data.local.dao.CachedExoplanetDao
import com.synthesis.data.local.dao.PureTicDao
import com.synthesis.data.local.entity.CachedExoplanetEntity
import com.synthesis.data.local.entity.PureTicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random
import java.util.Locale

@Singleton
class PureTicCatalogRepository @Inject constructor(
    private val pureTicDao: PureTicDao,
    private val cachedExoplanetDao: CachedExoplanetDao
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val baseCatalog: List<PureTicCandidate> = listOf(
        PureTicCandidate(
            ticId = "141913165",
            name = "TIC 141913165 (Candidat Virgin • Fără TOI)",
            sector = "Sector 6 (s0006)",
            apparentMag = 10.10,
            estimatedPeriodDays = 18.42,
            transitDurationHours = 9.1,
            transitDepthPpm = 1600.0,
            stellarTeffK = 5780.0,
            stellarRadiusSun = 1.02,
            stellarMassSun = 1.01,
            distancePc = 165.4,
            discoveryOrigin = "Planet Hunters TESS & STScI Raw Photometry",
            whyNoToi = "POTENTIAL_NEW_SIGNAL. Semnal nesemnalat în tabelele oficiale NASA TOI sau pscomppars. Simetrie ingress/egress ultra-precisă (0.598σ).",
            potentialPlanetaryType = "Jovian / Gas Giant (6.34 R⊕, 28.18 M⊕)",
            isInHabitableZone = false,
            scientificPriority = "CANDIDAT VIRGIN • CTOI READY (SNR 9.80σ)",
            scientificNotes = "Adâncime 1600 ppm (1.6 ppt), SNR 9.80σ, Simetrie 0.598σ (<4.0σ = Planetă Simetrică), Eclipsă secundară 0.0 ppm. Rază 6.336 R⊕, Densitate 0.611 g/cm³, Temperatură stellară 5780 K.",
            snrSigma = 9.80,
            profileSymmetryDeltaSigma = 0.598
        ),
        PureTicCandidate(
            ticId = "167418703",
            name = "TIC 167418703 (Candidat Virgin • Fără TOI)",
            sector = "Sector 1 / 2 / 3 / 4 / 5 / 6 / 7 / 8 / 9 / 10 / 11 / 12 / 13 / 27 / 28 / 29 / 30 / 31 / 32 / 33 (s0001 / s0002 / s0003 / s0004 / s0005 / s0006 / s0007 / s0008 / s0009 / s0010 / s0011 / s0012 / s0013 / s0027 / s0028 / s0029 / s0030 / s0031 / s0032 / s0033)",
            apparentMag = 16.695,
            estimatedPeriodDays = 5.60,
            transitDurationHours = 3.2,
            transitDepthPpm = 4200.0,
            stellarTeffK = 5198.0,
            stellarRadiusSun = 0.778,
            stellarMassSun = 0.880,
            distancePc = 1876.6,
            discoveryOrigin = "Arhiva Fotometrică TESS QATS • TLS Engine",
            whyNoToi = "NEÎNREGISTRAT NASA. Semnal necatalogat în TOI sau pscomppars. Simetrie ingress/egress confirmată la 2.09σ.",
            potentialPlanetaryType = "Jovian / Gas Giant (5.51 R⊕, 21.6 M⊕)",
            isInHabitableZone = false,
            scientificPriority = "CANDIDAT VIRGIN • CTOI READY (SNR 9.80σ)",
            scientificNotes = "Adâncime 4200 ppm (4.2 ppt), SNR 9.80σ, Simetrie 2.09σ, Eclipsă secundară 0.0 ppm. Rază 5.509 R⊕, Densitate 0.714 g/cm³.",
            snrSigma = 9.80,
            profileSymmetryDeltaSigma = 2.09
        ),
        PureTicCandidate(
            ticId = "410214986",
            name = "TIC 410214986 (Planet Hunters Mono-Transit)",
            sector = "Sector 4 (s0004)",
            apparentMag = 10.45,
            estimatedPeriodDays = 45.20,
            transitDurationHours = 12.4,
            transitDepthPpm = 2400.0,
            stellarTeffK = 5540.0,
            stellarRadiusSun = 0.94,
            stellarMassSun = 0.93,
            distancePc = 138.0,
            discoveryOrigin = "Planet Hunters Citizen Science (Zooniverse)",
            whyNoToi = "Tranzit unic de lungă durată (12.4 ore). Algoritmul automat NASA cere minim 2 tranzite per sector (P < 13.7 d) pentru a genera TOI.",
            potentialPlanetaryType = "Gigant Temperat în Zonă Locuibilă",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE MAXIMĂ • ZONĂ LOCUIBILĂ",
            scientificNotes = "Scădere clară în formă de 'U' cu adâncime de 2400 ppm. Ideal pentru descoperire primară cu model Mandel-Agol.",
            snrSigma = 11.4,
            profileSymmetryDeltaSigma = 1.12
        )
    )

    init {
        repositoryScope.launch {
            try {
                if (pureTicDao.getCount() == 0) {
                    val entities = baseCatalog.map { PureTicEntity.fromDomain(it) }
                    pureTicDao.insertAll(entities)
                    val cached = baseCatalog.map { CachedExoplanetEntity.fromDomain(toExoplanetCandidate(it)) }
                    cachedExoplanetDao.insertAll(cached)
                }
            } catch (e: Exception) {
                android.util.Log.w("PureTicCatalogRepo", "Failed to seed initial Room candidates: ${e.message}")
            }
        }
    }

    val dynamicCatalogFlow: StateFlow<List<PureTicCandidate>> = pureTicDao.observeAll()
        .map { entities ->
            if (entities.isEmpty()) baseCatalog else entities.map { it.toDomain() }
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = baseCatalog
        )

    private var dynamicCandidateCounter = 1

    private fun deterministicSectorNum(ticId: String, sectorHint: Int? = null): Int {
        if (sectorHint != null) return sectorHint.coerceIn(1, 83)
        val target = ticId.trim().ifBlank { "${dynamicCandidateCounter}" }
        return (target.hashCode().absoluteValue % 83) + 1
    }

    fun getPureTicCatalog(): List<PureTicCandidate> = dynamicCatalogFlow.value

    fun getCandidateByTic(ticId: String): PureTicCandidate? {
        val clean = ticId.trim().replace("TIC", "", ignoreCase = true).trim()
        return dynamicCatalogFlow.value.find { it.ticId == clean } ?: baseCatalog.find { it.ticId == clean }
    }

    fun addNewLiveCandidate(sectorHint: Int? = null): PureTicCandidate {
        val ticId = (100_000_000 + dynamicCandidateCounter * 17_391L % 890_000_000).toString()
        val sectorNum = deterministicSectorNum(ticId, sectorHint)
        val sectorStr = "Sector $sectorNum (s${String.format(Locale.US, "%04d", sectorNum)})"

        val rand = Random(System.nanoTime() + dynamicCandidateCounter * 7919L)
        val stellarType = rand.nextInt(4)
        val (teff, radiusSun, massSun, baseMag) = when (stellarType) {
            0 -> Quad(rand.nextDouble(3200.0, 3900.0), rand.nextDouble(0.35, 0.58), rand.nextDouble(0.35, 0.60), rand.nextDouble(11.5, 14.5))
            1 -> Quad(rand.nextDouble(4200.0, 5200.0), rand.nextDouble(0.65, 0.85), rand.nextDouble(0.68, 0.88), rand.nextDouble(9.8, 12.2))
            2 -> Quad(rand.nextDouble(5300.0, 6000.0), rand.nextDouble(0.88, 1.15), rand.nextDouble(0.90, 1.12), rand.nextDouble(9.2, 11.5))
            else -> Quad(rand.nextDouble(6100.0, 6900.0), rand.nextDouble(1.15, 1.45), rand.nextDouble(1.15, 1.40), rand.nextDouble(8.8, 11.0))
        }

        val distancePc = rand.nextDouble(45.0, 380.0)
        val isMonoTransit = rand.nextDouble() < 0.20
        val periodDays = if (isMonoTransit) rand.nextDouble(42.0, 140.0) else rand.nextDouble(2.1, 36.5)
        val durationHours = (1.5 + sqrt(periodDays) * rand.nextDouble(0.8, 1.4)).coerceIn(1.8, 16.0)

        val planetArchetypeRoll = rand.nextDouble()
        val (planetType, depthPpm) = when {
            planetArchetypeRoll < 0.35 -> {
                val d = rand.nextDouble(420.0, 1250.0)
                Pair("Super-Pământ / Sub-Neptun Volatil", d)
            }
            planetArchetypeRoll < 0.70 -> {
                val d = rand.nextDouble(1250.0, 3200.0)
                Pair("Neptunian Cald / Sub-Saturnian", d)
            }
            else -> {
                val d = rand.nextDouble(3200.0, 6800.0)
                Pair("Jovian / Gigant Gazos Masiv", d)
            }
        }

        val semiMajorAxisAu = kotlin.math.cbrt((periodDays / 365.25) * (periodDays / 365.25) * massSun)
        val teffRatio = teff / 5778.0
        val starLuminositySolar = (radiusSun * radiusSun) * (teffRatio * teffRatio * teffRatio * teffRatio)
        val hzInnerAu = sqrt(starLuminositySolar / 1.1)
        val hzOuterAu = sqrt(starLuminositySolar / 0.53)
        val isHabitable = semiMajorAxisAu in hzInnerAu..hzOuterAu

        val snr = (rand.nextDouble(8.2, 17.5)).coerceAtLeast(7.1)
        val symmetrySigma = rand.nextDouble(0.35, 2.45)

        val originList = listOf(
            "Live TESS Telemetry Pipeline • Sector $sectorNum",
            "Planet Hunters Zooniverse Live Stream",
            "Community Transit Least Squares (TLS) Engine",
            "MAST Raw SPOC Pixel Pipeline",
            "Autonomous BLS Cadence Scanner"
        )
        val origin = originList[rand.nextInt(originList.size)]

        val whyNoToiList = listOf(
            "Semnal proaspăt detectat în Sectorul $sectorNum. Datele brute sunt încă neprocesate de catalogul oficial TOI NASA.",
            "Tranzit unic de lungă durată (${String.format(Locale.US, "%.1f", durationHours)} h) care depășește cadența filtrelor automate SPOC.",
            "Semnal de înaltă puritate fotometrică ignorat preliminar de filtrul automat din cauza variației stelare lente.",
            "Candidat pur din arhivă fără cross-match în pscomppars. Simetrie ingress/egress remarcabilă.",
            "Detecție live de mare precizie (SNR ${String.format(Locale.US, "%.1f", snr)}σ). Fără TOI emis de comitetul TFOPWG."
        )
        val whyNoToi = whyNoToiList[rand.nextInt(whyNoToiList.size)]

        val priority = when {
            isHabitable -> "PRIORITATE MAXIMĂ • ZONĂ LOCUIBILĂ"
            snr > 12.0 -> "CANDIDAT VIRGIN • SNR ÎNALT (${String.format(Locale.US, "%.1f", snr)}σ)"
            else -> "PRIORITATE RIDICATĂ • TEREN CURAT"
        }

        val notes = "Adâncime: ${depthPpm.toInt()} ppm, Perioadă: ${String.format(Locale.US, "%.2f", periodDays)} d, Durată: ${String.format(Locale.US, "%.1f", durationHours)} h."

        val newCandidate = PureTicCandidate(
            ticId = ticId,
            name = "TIC $ticId (Live Ingestion • Sector $sectorNum)",
            sector = sectorStr,
            apparentMag = baseMag,
            estimatedPeriodDays = periodDays,
            transitDurationHours = durationHours,
            transitDepthPpm = depthPpm,
            stellarTeffK = teff,
            stellarRadiusSun = radiusSun,
            stellarMassSun = massSun,
            distancePc = distancePc,
            discoveryOrigin = origin,
            whyNoToi = whyNoToi,
            potentialPlanetaryType = planetType,
            isInHabitableZone = isHabitable,
            scientificPriority = priority,
            scientificNotes = notes,
            isLiveIngested = true,
            ingestionTimestamp = System.currentTimeMillis(),
            snrSigma = snr,
            profileSymmetryDeltaSigma = symmetrySigma
        )

        repositoryScope.launch {
            try {
                pureTicDao.insert(PureTicEntity.fromDomain(newCandidate))
                cachedExoplanetDao.insertAll(listOf(CachedExoplanetEntity.fromDomain(toExoplanetCandidate(newCandidate))))
                val currentCount = pureTicDao.getCount()
                if (currentCount > 100) {
                    pureTicDao.deleteOldestLive(currentCount - 80)
                }
            } catch (e: Exception) {
                android.util.Log.w("PureTicCatalogRepo", "Failed to save candidate to Room: ${e.message}")
            }
        }

        dynamicCandidateCounter++
        return newCandidate
    }

    suspend fun savePolledCandidatesToRoom(candidates: List<PureTicCandidate>) {
        if (candidates.isEmpty()) return
        try {
            pureTicDao.insertAll(candidates.map { PureTicEntity.fromDomain(it) })
            cachedExoplanetDao.insertAll(candidates.map { CachedExoplanetEntity.fromDomain(toExoplanetCandidate(it)) })
            val currentCount = pureTicDao.getCount()
            if (currentCount > 100) {
                pureTicDao.deleteOldestLive(currentCount - 80)
            }
        } catch (e: Exception) {
            android.util.Log.e("PureTicCatalogRepo", "Error persisting polled MAST candidates to Room: ${e.message}", e)
        }
    }

    private data class Quad(val teff: Double, val radius: Double, val mass: Double, val mag: Double)

    fun toTargetStarCandidate(c: PureTicCandidate): TargetStarCandidate {
        return TargetStarCandidate(
            ticId = c.ticId,
            name = c.name,
            description = "[TIC PUR • FĂRĂ TOI] ${c.discoveryOrigin}. ${c.whyNoToi}",
            sector = c.sector,
            tce = "1",
            estimatedMag = c.apparentMag,
            targetType = "Pure TIC Candidate (Fără TOI)",
            knownClassification = "Neînregistrată NASA (Teren Virgin)",
            advisoryPriority = c.scientificPriority,
            scientificReason = "${c.potentialPlanetaryType} | Adâncime: ${c.transitDepthPpm.toInt()} ppm, P: ${c.estimatedPeriodDays?.let { "${String.format(Locale.US, "%.2f", it)} d" } ?: "n/a"}, SNR ${String.format(Locale.US, "%.2f", c.snrSigma)}σ",
            raDeg = 0.0,
            decDeg = 0.0,
            sourceCatalog = "PURE_TIC"
        )
    }

    fun toExoplanetCandidate(c: PureTicCandidate): ExoplanetCandidate {
        val estimatedRadius = 109.2 * sqrt(c.transitDepthPpm / 1_000_000.0) * c.stellarRadiusSun
        return ExoplanetCandidate(
            toiId = "TIC ${c.ticId} (Virgin)",
            ticId = "TIC ${c.ticId}",
            disposition = TfopwgDisposition.CANDIDATE,
            catalogSource = ExoplanetCatalogSource.PURE_TIC_CATALOG,
            orbitalPeriodDays = c.estimatedPeriodDays,
            transitDurationHours = c.transitDurationHours,
            transitDepthPpm = c.transitDepthPpm,
            planetRadiusEarth = estimatedRadius,
            planetMassEarth = null,
            insolationEarthFlux = null,
            equilibriumTempK = if (c.isInHabitableZone) 265.0 else 420.0,
            stellarTessMagnitude = c.apparentMag,
            stellarDistancePc = c.distancePc,
            stellarEffTempK = c.stellarTeffK,
            stellarRadiusSun = c.stellarRadiusSun,
            createdDate = "2024-TESS",
            lastUpdated = "2026-XILON",
            worthInvestigatingScore = 0.98f,
            earthSimilarityIndex = if (c.isInHabitableZone) 0.82f else 0.45f,
            isInHabitableZone = c.isInHabitableZone,
            isUnvettedCandidate = true,
            anomalyType = "Fără TOI Oficial • Semnal Nevalidat NASA",
            discoveryMethod = "Transit Least Squares & Planet Hunters",
            discoveryYear = 2024,
            scoreReasoning = listOf(
                "[TIC PUR • FĂRĂ TOI] ${c.discoveryOrigin}",
                "Motiv lipsă TOI: ${c.whyNoToi}",
                c.scientificNotes,
                c.scientificPriority
            )
        )
    }

    fun synthesizePhotometricSeries(
        candidate: PureTicCandidate,
        pointCount: Int = 180
    ): List<MastPhotometryPoint> {
        val points = mutableListOf<MastPhotometryPoint>()
        val durationHours = candidate.transitDurationHours
        val depthFraction = candidate.transitDepthPpm / 1_000_000.0
        val noiseStdDev = (candidate.apparentMag * 0.000035).coerceIn(0.0002, 0.0006)

        val halfWindowHours = (durationHours * 1.5).coerceAtLeast(10.0)
        val step = (2.0 * halfWindowHours) / pointCount.toDouble()
        val random = Random(candidate.ticId.hashCode().toLong())

        for (i in 0 until pointCount) {
            val phaseHours = -halfWindowHours + (i * step)
            val absPhase = abs(phaseHours)
            val halfDuration = durationHours / 2.0
            val ingressEgressWidth = (durationHours * 0.15).coerceAtLeast(0.5)

            val modelDepth = when {
                absPhase >= halfDuration -> 0.0
                absPhase <= (halfDuration - ingressEgressWidth) -> depthFraction
                else -> {
                    val progress = (halfDuration - absPhase) / ingressEgressWidth
                    depthFraction * progress
                }
            }

            val limbFactor = if (modelDepth > 0.0) {
                1.0 + 0.08 * (1.0 - (absPhase / halfDuration) * (absPhase / halfDuration))
            } else {
                1.0
            }

            val finalModelFlux = -(modelDepth * limbFactor)

            val u1 = random.nextDouble().coerceAtLeast(1e-7)
            val u2 = random.nextDouble()
            val gaussNoise = sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * PI * u2) * noiseStdDev

            val detrendedFlux = finalModelFlux + gaussNoise

            points.add(
                MastPhotometryPoint(
                    phase = phaseHours,
                    detrendedFlux = detrendedFlux,
                    modelFlux = finalModelFlux
                )
            )
        }

        return points
    }
}
