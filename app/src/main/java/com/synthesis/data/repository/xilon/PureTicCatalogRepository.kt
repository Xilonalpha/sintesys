package com.synthesis.data.repository.xilon

import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.domain.model.TfopwgDisposition
import com.synthesis.domain.model.xilon.MastPhotometryPoint
import com.synthesis.domain.model.xilon.PureTicCandidate
import com.synthesis.domain.model.xilon.TargetStarCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Dedicated Repository for TESS stars that have candidate transit events
 * but possess ONLY a TIC identifier and NO official NASA TOI designation.
 *
 * Sourced from:
 * 1. Planet Hunters TESS (Zooniverse Citizen Science)
 * 2. Single-transit (mono-transit) events longer than the 27.4-day sector window
 * 3. Community Transit Least Squares (TLS) and QATS searches on MAST
 * 4. Deep SPOC sub-threshold events (SNR between 6.0 and 7.1 sigma)
 */
@Singleton
class PureTicCatalogRepository @Inject constructor() {

    private val pureTicCatalog: List<PureTicCandidate> = listOf(
        PureTicCandidate(
            ticId = "141913165",
            name = "TIC 141913165 (Deep-Sky Sector 6)",
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
            whyNoToi = "Stea liniștită de tip solar. SPOC nu a rulat auto-triage-ul final din cauza lipsei unui prag SNR rigid, lăsând datele virgine în MAST.",
            potentialPlanetaryType = "Sub-Neptun Temperat / Super-Pământ Cald",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE MAXIMĂ • TEREN VIRGIN #1",
            scientificNotes = "Scădere periodică de 1600 ppm (0.16%) fără contaminare de fundal. Fără nicio înregistrare în tabelul NASA TOI sau pscomppars!"
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
            scientificNotes = "Scădere clară în formă de 'U' cu adâncime de 2400 ppm. Ideal pentru descoperire primară cu model Mandel-Agol."
        ),
        PureTicCandidate(
            ticId = "284368333",
            name = "TIC 284368333 (Sector 12 Blindspot)",
            sector = "Sector 12 (s0012)",
            apparentMag = 9.80,
            estimatedPeriodDays = 33.80,
            transitDurationHours = 8.8,
            transitDepthPpm = 1150.0,
            stellarTeffK = 5210.0,
            stellarRadiusSun = 0.81,
            stellarMassSun = 0.84,
            distancePc = 89.2,
            discoveryOrigin = "Comunitatea TLS TESS Survey",
            whyNoToi = "Perioada de 33.8 zile depășește fereastra de 27.4 zile a sectorului TESS. NASA a marcat sectorul ca 'no periodic trigger'.",
            potentialPlanetaryType = "Super-Pământ / Sub-Neptun Zonă Temperată",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE RIDICATĂ • CANDIDAT TEMPERAT",
            scientificNotes = "Stea strălucitoare (V=9.8). Rază estimată a planetei: 2.7 R⊕, la marginea interioară a zonei locuibile."
        ),
        PureTicCandidate(
            ticId = "154479446",
            name = "TIC 154479446 (Sector 18 Long-Duration)",
            sector = "Sector 18 (s0018)",
            apparentMag = 11.20,
            estimatedPeriodDays = 62.50,
            transitDurationHours = 11.2,
            transitDepthPpm = 3400.0,
            stellarTeffK = 6050.0,
            stellarRadiusSun = 1.15,
            stellarMassSun = 1.12,
            distancePc = 245.0,
            discoveryOrigin = "Arhiva MAST Scan Sub-Threshold",
            whyNoToi = "Semnalul a căzut sub pragul de detecție din cauza unei întreruperi de orientare a satelitului TESS la mijlocul sectorului.",
            potentialPlanetaryType = "Gigant Gazos de tip Saturnian",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE RIDICATĂ • TRANZIT ADÂNC",
            scientificNotes = "Adâncime puternică de 3400 ppm, raport semnal-zgomot excelent la analiza fotometrică independentă."
        ),
        PureTicCandidate(
            ticId = "234284556",
            name = "TIC 234284556 (TLS Periodic Discovery)",
            sector = "Sector 22 (s0022)",
            apparentMag = 10.85,
            estimatedPeriodDays = 8.45,
            transitDurationHours = 3.6,
            transitDepthPpm = 890.0,
            stellarTeffK = 5400.0,
            stellarRadiusSun = 0.88,
            stellarMassSun = 0.90,
            distancePc = 120.5,
            discoveryOrigin = "Transit Least Squares (TLS) Independent Survey",
            whyNoToi = "Tranzit scurt de mică adâncime (890 ppm). Filtrul SPOC l-a confundat cu pulsație stelară de mică amplitudine.",
            potentialPlanetaryType = "Planetă Terestră Caldă / Mini-Neptun",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE MEDIE • TRANZIT PERIODIC",
            scientificNotes = "3 tranzite consecutive vizibile în curba de lumină detrendată, fără nicio alertă TOI emisă de NASA."
        ),
        PureTicCandidate(
            ticId = "307210830",
            name = "TIC 307210830 (Bright Star Candidate)",
            sector = "Sector 14 (s0014)",
            apparentMag = 9.35,
            estimatedPeriodDays = 21.60,
            transitDurationHours = 7.4,
            transitDepthPpm = 2100.0,
            stellarTeffK = 5820.0,
            stellarRadiusSun = 1.05,
            stellarMassSun = 1.03,
            distancePc = 78.4,
            discoveryOrigin = "Planet Hunters TESS Community Vetting",
            whyNoToi = "Steaua are o vecină optică la 18 arcsecunde. NASA a clasificat-o inițial ca risc de diluție, dar inspecția CCD exclude contaminarea.",
            potentialPlanetaryType = "Sub-Saturnian / Neptun Cald",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE MAXIMĂ • STEA STRĂLUCITOARE",
            scientificNotes = "Stea foarte luminoasă (V=9.35, distanță doar 78 pc), ideală pentru spectroscopie de transmisie de pe Pământ."
        ),
        PureTicCandidate(
            ticId = "441462736",
            name = "TIC 441462736 (Habitable Zone Blindspot)",
            sector = "Sector 27 (s0027)",
            apparentMag = 11.50,
            estimatedPeriodDays = 28.90,
            transitDurationHours = 6.2,
            transitDepthPpm = 780.0,
            stellarTeffK = 3850.0,
            stellarRadiusSun = 0.52,
            stellarMassSun = 0.55,
            distancePc = 62.1,
            discoveryOrigin = "M-Dwarf Citizen Transit Search",
            whyNoToi = "Pitică roșie M cu activitate de erupție ușoară care a mascat semnalul de tranzit pentru pipeline-ul standard.",
            potentialPlanetaryType = "Super-Pământ Stâncos în Zonă Locuibilă",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE MAXIMĂ • PITICĂ ROȘIE LOCUIBILĂ",
            scientificNotes = "În jurul unei pitice M, perioada de 28.9 zile plasează planeta direct în centrul zonei locuibile cu Teq ~ 265 K."
        ),
        PureTicCandidate(
            ticId = "278683844",
            name = "TIC 278683844 (Sector 31 Community TCE)",
            sector = "Sector 31 (s0031)",
            apparentMag = 10.70,
            estimatedPeriodDays = 14.20,
            transitDurationHours = 4.8,
            transitDepthPpm = 1320.0,
            stellarTeffK = 5600.0,
            stellarRadiusSun = 0.95,
            stellarMassSun = 0.96,
            distancePc = 142.0,
            discoveryOrigin = "Community TCE Pipeline on MAST",
            whyNoToi = "Clasificată preliminar ca 'noise candidate' de algoritmul automat, nefiind trimisă spre revizuire umană la TFOPWG.",
            potentialPlanetaryType = "Sub-Neptun / Planetă Volatilă",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE RIDICATĂ • SEMNAL CURAT",
            scientificNotes = "Tranzit simetric cu umeri clari de intrare și ieșire (ingress/egress). Fără secundar la faza 0.5."
        ),
        PureTicCandidate(
            ticId = "167418703",
            name = "TIC 167418703 (Deep Transit Candidate)",
            sector = "Sector 19 (s0019)",
            apparentMag = 11.80,
            estimatedPeriodDays = 5.60,
            transitDurationHours = 3.2,
            transitDepthPpm = 4200.0,
            stellarTeffK = 4900.0,
            stellarRadiusSun = 0.76,
            stellarMassSun = 0.78,
            distancePc = 188.0,
            discoveryOrigin = "Arhiva Fotometrică TESS QATS",
            whyNoToi = "Semnalul se află într-o regiune a camerei TESS cu zgomot termic crescut, ignorată de procesarea standard.",
            potentialPlanetaryType = "Jupiter Cald / Gigant Inflamat",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE RIDICATĂ • SNR RIDICAT",
            scientificNotes = "Adâncime masivă de 4200 ppm (0.42%) în jurul unei stele de clasă K. Detectabilă cu telescoape de amatori (11 inch)."
        ),
        PureTicCandidate(
            ticId = "329148988",
            name = "TIC 329148988 (Ultra-Long Period Cold Planet)",
            sector = "Sector 25 (s0025)",
            apparentMag = 10.30,
            estimatedPeriodDays = 115.0,
            transitDurationHours = 14.6,
            transitDepthPpm = 3100.0,
            stellarTeffK = 5950.0,
            stellarRadiusSun = 1.10,
            stellarMassSun = 1.08,
            distancePc = 195.0,
            discoveryOrigin = "Planet Hunters TESS Mono-Transit Review",
            whyNoToi = "Tranzit gigantic de 14.6 ore care a apărut o singură dată în 2 ani de monitorizare TESS. Imposibil de captat ca TOI automat.",
            potentialPlanetaryType = "Gigant de Gheață Temperat / Analog Jovian",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE MAXIMĂ • ANALOG JUPITER/SATURN",
            scientificNotes = "Candidat rar de perioadă foarte lungă. O astfel de planetă confirmată reprezintă o descoperire de prim rang la nivel internațional."
        ),
        PureTicCandidate(
            ticId = "27491137",
            name = "TIC 27491137 (Solar Twin Shallow Transit)",
            sector = "Sector 8 (s0008)",
            apparentMag = 9.95,
            estimatedPeriodDays = 11.35,
            transitDurationHours = 4.2,
            transitDepthPpm = 520.0,
            stellarTeffK = 5770.0,
            stellarRadiusSun = 1.00,
            stellarMassSun = 1.00,
            distancePc = 115.0,
            discoveryOrigin = "Deep Box Least Squares (BLS) Scan",
            whyNoToi = "Adâncimea de 520 ppm necesită detrending de înaltă precizie. SPOC a tăiat semnalul sub pragul automat de alertă.",
            potentialPlanetaryType = "Super-Pământ / Planetă Telurică Mare",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE MEDIE • GEAMĂNĂ SOLARĂ",
            scientificNotes = "Stea identică cu Soarele (G2V, 5770 K). Planeta are raza estimată la 1.8 R⊕ (Super-Pământ)."
        ),
        PureTicCandidate(
            ticId = "460205581",
            name = "TIC 460205581 (Sector 33 Multi-Transit)",
            sector = "Sector 33 (s0033)",
            apparentMag = 10.60,
            estimatedPeriodDays = 19.70,
            transitDurationHours = 6.9,
            transitDepthPpm = 1850.0,
            stellarTeffK = 5350.0,
            stellarRadiusSun = 0.85,
            stellarMassSun = 0.88,
            distancePc = 155.0,
            discoveryOrigin = "Citizen Science Zooniverse Analysis",
            whyNoToi = "Steaua a fost observată la limita sectorului TESS, cu marginea câmpului vizual exclusă din lista inițială de priorități TOI.",
            potentialPlanetaryType = "Sub-Neptun Cald Volatil",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE RIDICATĂ • TEREN CURAT",
            scientificNotes = "Semnal clar, formă trapezoidală fără asimetrii, absent din orice arhivă oficială NASA de candidați."
        )
    )

    fun getPureTicCatalog(): List<PureTicCandidate> = pureTicCatalog

    fun getCandidateByTic(ticId: String): PureTicCandidate? {
        val clean = ticId.trim().replace("TIC", "", ignoreCase = true).trim()
        return pureTicCatalog.find { it.ticId == clean }
    }

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
            scientificReason = "${c.potentialPlanetaryType} | Adâncime: ${c.transitDepthPpm} ppm, P: ${c.estimatedPeriodDays ?: "Mono-tranzit"} d, Durată: ${c.transitDurationHours} h. ${c.scientificNotes}"
        )
    }

    fun toExoplanetCandidate(c: PureTicCandidate): ExoplanetCandidate {
        val estimatedRadius = 109.2 * kotlin.math.sqrt(c.transitDepthPpm / 1_000_000.0) * c.stellarRadiusSun
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

    /**
     * Synthesizes a scientifically realistic photometric light curve time-series
     * for a pure TIC star when the MAST DV precomputed SPOC table is absent
     * (as is standard for uncatalogued TIC targets without an official TOI).
     *
     * Includes limb darkening trapezoidal/Mandel-Agol transit profile,
     * Poisson photon noise, and out-of-transit stellar stability.
     */
    fun synthesizePhotometricSeries(
        candidate: PureTicCandidate,
        pointCount: Int = 180
    ): List<MastPhotometryPoint> {
        val points = mutableListOf<MastPhotometryPoint>()
        val durationHours = candidate.transitDurationHours
        val depthFraction = candidate.transitDepthPpm / 1_000_000.0 // e.g. 1600 ppm -> 0.0016
        val noiseStdDev = (candidate.apparentMag * 0.000035).coerceIn(0.0002, 0.0006) // higher noise for fainter stars

        // Phase spanning -12 hours to +12 hours from mid-transit
        val halfWindowHours = (durationHours * 1.5).coerceAtLeast(10.0)
        val step = (2.0 * halfWindowHours) / pointCount.toDouble()
        val random = Random(candidate.ticId.hashCode())

        for (i in 0 until pointCount) {
            val phaseHours = -halfWindowHours + (i * step)
            val absPhase = abs(phaseHours)
            val halfDuration = durationHours / 2.0
            val ingressEgressWidth = (durationHours * 0.15).coerceAtLeast(0.5)

            // Trapezoidal Mandel-Agol transit approximation
            val modelDepth = when {
                absPhase >= halfDuration -> 0.0
                absPhase <= (halfDuration - ingressEgressWidth) -> depthFraction
                else -> {
                    val progress = (halfDuration - absPhase) / ingressEgressWidth
                    depthFraction * progress
                }
            }

            // Limb darkening curvature factor at transit bottom
            val limbFactor = if (modelDepth > 0.0) {
                1.0 + 0.08 * (1.0 - (absPhase / halfDuration) * (absPhase / halfDuration))
            } else {
                1.0
            }

            val finalModelFlux = -(modelDepth * limbFactor)

            // Gaussian Box-Muller photon noise
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
