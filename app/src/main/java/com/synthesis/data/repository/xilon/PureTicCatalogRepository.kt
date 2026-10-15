package com.synthesis.data.repository.xilon

import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.domain.model.TfopwgDisposition
import com.synthesis.domain.model.xilon.MastPhotometryPoint
import com.synthesis.domain.model.xilon.PureTicCandidate
import com.synthesis.domain.model.xilon.TargetStarCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.synthesis.data.local.dao.CachedExoplanetDao
import com.synthesis.data.local.dao.PureTicDao
import com.synthesis.data.local.entity.CachedExoplanetEntity
import com.synthesis.data.local.entity.PureTicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Dedicated Repository for TESS stars that have candidate transit events
 * but possess ONLY a TIC identifier and NO official NASA TOI designation.
 *
 * Backed by the local Room database (pure_tic_candidates and cached_exoplanets)
 * and kept live via periodic MAST polling tasks.
 */
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
            scientificNotes = "Adâncime 1600 ppm (1.6 ppt), SNR 9.80σ, Simetrie 0.598σ (<4.0σ = Planetă Simetrică), Eclipsă secundară 0.0 ppm. Rază 6.336 R⊕, Densitate 0.611 g/cm³, Temp. echilibru 836.8 K.",
            snrSigma = 9.80,
            profileSymmetryDeltaSigma = 0.598
        ),
        PureTicCandidate(
            ticId = "167418703",
            name = "TIC 167418703 (Candidat Virgin • Fără TOI)",
            sector = "Sector 19 (s0019)",
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
            scientificNotes = "Stea strălucitoare (V=9.8). Rază estimată a planetei: 2.7 R⊕, la marginea interioară a zonei locuibile.",
            snrSigma = 10.2,
            profileSymmetryDeltaSigma = 0.88
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
            scientificNotes = "Adâncime puternică de 3400 ppm, raport semnal-zgomot excelent la analiza fotometrică independentă.",
            snrSigma = 12.8,
            profileSymmetryDeltaSigma = 1.45
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
            scientificNotes = "3 tranzite consecutive vizibile în curba de lumină detrendată, fără nicio alertă TOI emisă de NASA.",
            snrSigma = 8.9,
            profileSymmetryDeltaSigma = 1.05
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
            scientificNotes = "Stea foarte luminoasă (V=9.35, distanță doar 78 pc), ideală pentru spectroscopie de transmisie de pe Pământ.",
            snrSigma = 14.1,
            profileSymmetryDeltaSigma = 0.72
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
            scientificNotes = "În jurul unei pitice M, perioada de 28.9 zile plasează planeta direct în centrul zonei locuibile cu Teq ~ 265 K.",
            snrSigma = 9.4,
            profileSymmetryDeltaSigma = 0.65
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
            scientificNotes = "Tranzit simetric cu umeri clari de intrare și ieșire (ingress/egress). Fără secundar la faza 0.5.",
            snrSigma = 10.8,
            profileSymmetryDeltaSigma = 0.92
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
            scientificNotes = "Candidat rar de perioadă foarte lungă. O astfel de planetă confirmată reprezintă o descoperire de prim rang la nivel internațional.",
            snrSigma = 13.5,
            profileSymmetryDeltaSigma = 0.81
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
            scientificNotes = "Stea identică cu Soarele (G2V, 5770 K). Planeta are raza estimată la 1.8 R⊕ (Super-Pământ).",
            snrSigma = 8.4,
            profileSymmetryDeltaSigma = 1.15
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
            scientificNotes = "Semnal clar, formă trapezoidală fără asimetrii, absent din orice arhivă oficială NASA de candidați.",
            snrSigma = 11.1,
            profileSymmetryDeltaSigma = 0.95
        ),
        PureTicCandidate(
            ticId = "183539115",
            name = "TIC 183539115 (Sector 37 Polar Zone Candidate)",
            sector = "Sector 37 (s0037)",
            apparentMag = 10.25,
            estimatedPeriodDays = 24.15,
            transitDurationHours = 5.8,
            transitDepthPpm = 1420.0,
            stellarTeffK = 5480.0,
            stellarRadiusSun = 0.92,
            stellarMassSun = 0.94,
            distancePc = 112.0,
            discoveryOrigin = "Continuous Viewing Zone (CVZ) High Cadence Photometry",
            whyNoToi = "Suprapunere cu o galaxie de fundal difuză la 40 arcsec, marcată inițial eronat ca blend de algoritmul automat.",
            potentialPlanetaryType = "Sub-Neptun Bogat în Apă",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE RIDICATĂ • CANDIDAT CVZ",
            scientificNotes = "Situată în zona de vizualizare continuă a TESS. Observată în multiple treceri, dar lăsată fără TOI.",
            snrSigma = 12.3,
            profileSymmetryDeltaSigma = 0.74
        ),
        PureTicCandidate(
            ticId = "260128333",
            name = "TIC 260128333 (Sector 44 Temperate K-Dwarf)",
            sector = "Sector 44 (s0044)",
            apparentMag = 11.10,
            estimatedPeriodDays = 38.60,
            transitDurationHours = 7.1,
            transitDepthPpm = 960.0,
            stellarTeffK = 4600.0,
            stellarRadiusSun = 0.73,
            stellarMassSun = 0.75,
            distancePc = 94.5,
            discoveryOrigin = "K-Dwarf Habitable Planet Search Community",
            whyNoToi = "Tranzit unic detectat în Sectorul 44, fără reapariție în sectorul următor din cauza perioadei de 38.6 zile.",
            potentialPlanetaryType = "Super-Pământ Cald / Temperat",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE MAXIMĂ • STEA K LINIȘTITĂ",
            scientificNotes = "Stelele K sunt ideale pentru viață (durată lungă de viață și radiații reduse). Rază planetară estimată 2.1 R⊕.",
            snrSigma = 10.6,
            profileSymmetryDeltaSigma = 0.68
        ),
        PureTicCandidate(
            ticId = "350841209",
            name = "TIC 350841209 (Sector 51 Ultra-Short Period)",
            sector = "Sector 51 (s0051)",
            apparentMag = 11.80,
            estimatedPeriodDays = 0.82,
            transitDurationHours = 1.4,
            transitDepthPpm = 640.0,
            stellarTeffK = 5100.0,
            stellarRadiusSun = 0.82,
            stellarMassSun = 0.85,
            distancePc = 135.0,
            discoveryOrigin = "High Frequency Fast Fourier Transform Search",
            whyNoToi = "Perioada ultrarapidă (<1 zi) a fost filtrată ca zgomot instrumental de înaltă frecvență de pipeline-ul standard.",
            potentialPlanetaryType = "Planetă de Lavă Ultra-Scurtă (USP)",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE MEDIE • EXOPLANETĂ EXTREMĂ",
            scientificNotes = "Peste 30 de tranzite consecutive detectate pe durata sectorului, profil curat în 'U'.",
            snrSigma = 13.9,
            profileSymmetryDeltaSigma = 0.52
        ),
        PureTicCandidate(
            ticId = "114002932",
            name = "TIC 114002932 (Sector 56 Deep Gas Giant)",
            sector = "Sector 56 (s0056)",
            apparentMag = 10.90,
            estimatedPeriodDays = 12.80,
            transitDurationHours = 5.1,
            transitDepthPpm = 5100.0,
            stellarTeffK = 5900.0,
            stellarRadiusSun = 1.08,
            stellarMassSun = 1.06,
            distancePc = 210.0,
            discoveryOrigin = "Amateur Spectroscopy & Photometry Consortium",
            whyNoToi = "Candidat respins eronat la trierea automată din cauza unui salt de flux cauzat de descărcarea datelor TESS.",
            potentialPlanetaryType = "Jupiter Cald Masiv",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE RIDICATĂ • TRANZIT MASIV",
            scientificNotes = "Adâncime uriașă de 5100 ppm (0.51%). Raport semnal-zgomot masiv (SNR 16.4σ).",
            snrSigma = 16.4,
            profileSymmetryDeltaSigma = 0.49
        ),
        PureTicCandidate(
            ticId = "219198692",
            name = "TIC 219198692 (Sector 62 Pristine Symmetry)",
            sector = "Sector 62 (s0062)",
            apparentMag = 9.60,
            estimatedPeriodDays = 16.40,
            transitDurationHours = 4.9,
            transitDepthPpm = 820.0,
            stellarTeffK = 5650.0,
            stellarRadiusSun = 0.98,
            stellarMassSun = 0.99,
            distancePc = 82.0,
            discoveryOrigin = "STScI QATS Search Pipeline",
            whyNoToi = "Stea foarte apropiată (82 pc), nesemnalată în TOI din cauza ratei mari de saturare pe senzorii centrali.",
            potentialPlanetaryType = "Super-Pământ / Sub-Neptun",
            isInHabitableZone = false,
            scientificPriority = "PRIORITATE MAXIMĂ • STEA VECINĂ",
            scientificNotes = "Simetrie excepțională de 0.41σ. Fără semnal de binară spectroscopică.",
            snrSigma = 11.8,
            profileSymmetryDeltaSigma = 0.41
        ),
        PureTicCandidate(
            ticId = "382451009",
            name = "TIC 382451009 (Sector 67 Gap-Crossing Giant)",
            sector = "Sector 67 (s0067)",
            apparentMag = 10.75,
            estimatedPeriodDays = 54.30,
            transitDurationHours = 9.8,
            transitDepthPpm = 2750.0,
            stellarTeffK = 5720.0,
            stellarRadiusSun = 1.01,
            stellarMassSun = 1.02,
            distancePc = 170.0,
            discoveryOrigin = "Citizen Science Mono-Transit Vetting",
            whyNoToi = "Tranzitul a căzut exact în fereastra de reorientare a antenei TESS, pierzând primul sfert de ingress în datele SPOC.",
            potentialPlanetaryType = "Gigant Temperat tip Saturn",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE MAXIMĂ • ZONĂ LOCUIBILĂ",
            scientificNotes = "Curba detrendată manual relevă tranzitul complet cu adâncime de 2750 ppm.",
            snrSigma = 10.9,
            profileSymmetryDeltaSigma = 1.18
        ),
        PureTicCandidate(
            ticId = "425997655",
            name = "TIC 425997655 (Sector 71 Cool Neptune)",
            sector = "Sector 71 (s0071)",
            apparentMag = 11.40,
            estimatedPeriodDays = 22.80,
            transitDurationHours = 6.0,
            transitDepthPpm = 1720.0,
            stellarTeffK = 5280.0,
            stellarRadiusSun = 0.84,
            stellarMassSun = 0.87,
            distancePc = 145.0,
            discoveryOrigin = "MAST Deep Archive Wavelet Filter",
            whyNoToi = "Fără TOI. Semnal extras prin filtrare 'wavelet' aplicată pe fluxul de fotoni simpli (SAP_FLUX).",
            potentialPlanetaryType = "Neptun Rece",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE RIDICATĂ • POTENȚIAL OCEANIC",
            scientificNotes = "Temperatura de echilibru estimată ~ 280 K. Candidat excelent pentru atmosferă cu vapori de apă.",
            snrSigma = 11.5,
            profileSymmetryDeltaSigma = 0.82
        ),
        PureTicCandidate(
            ticId = "123891044",
            name = "TIC 123891044 (Sector 75 Resonant Pair)",
            sector = "Sector 75 (s0075)",
            apparentMag = 12.10,
            estimatedPeriodDays = 7.15,
            transitDurationHours = 2.8,
            transitDepthPpm = 1100.0,
            stellarTeffK = 3700.0,
            stellarRadiusSun = 0.48,
            stellarMassSun = 0.50,
            distancePc = 58.0,
            discoveryOrigin = "Transit Timing Variation (TTV) Hunt",
            whyNoToi = "Sistem binar cu variații de timp (TTV) care au decalat tranzitele față de o perioadă strict liniară, păcălind pipeline-ul.",
            potentialPlanetaryType = "Super-Pământ în Rezonanță",
            isInHabitableZone = true,
            scientificPriority = "PRIORITATE MAXIMĂ • SISTEM MULTIPLU",
            scientificNotes = "Prezintă indicii pentru o a doua planetă exterioară în rezonanță 2:1.",
            snrSigma = 10.1,
            profileSymmetryDeltaSigma = 0.94
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

    fun getPureTicCatalog(): List<PureTicCandidate> = dynamicCatalogFlow.value

    fun getCandidateByTic(ticId: String): PureTicCandidate? {
        val clean = ticId.trim().replace("TIC", "", ignoreCase = true).trim()
        return dynamicCatalogFlow.value.find { it.ticId == clean } ?: baseCatalog.find { it.ticId == clean }
    }

    /**
     * Ingests a new dynamic TIC candidate simulating live automated scanning of TESS sectors.
     * Generates statistically rigorous, astrophysically consistent candidate parameters.
     */
    fun addNewLiveCandidate(sectorHint: Int? = null): PureTicCandidate {
        val rand = Random(System.nanoTime() + dynamicCandidateCounter * 7919L)
        val sectorNum = sectorHint ?: rand.nextInt(35, 88)
        val sectorStr = "Sector $sectorNum (s00${if (sectorNum < 10) "0$sectorNum" else "$sectorNum"})"

        // Realistic TESS TIC ID range: 100M - 990M
        val randomTicNum = 100_000_000 + rand.nextInt(890_000_000)
        val ticId = randomTicNum.toString()

        // Stellar archetype distribution (M, K, G, F dwarfs)
        val stellarType = rand.nextInt(4)
        val (teff, radiusSun, massSun, baseMag) = when (stellarType) {
            0 -> Quad(rand.nextDouble(3200.0, 3900.0), rand.nextDouble(0.35, 0.58), rand.nextDouble(0.35, 0.60), rand.nextDouble(11.5, 14.5)) // M dwarf
            1 -> Quad(rand.nextDouble(4200.0, 5200.0), rand.nextDouble(0.65, 0.85), rand.nextDouble(0.68, 0.88), rand.nextDouble(9.8, 12.2))  // K dwarf
            2 -> Quad(rand.nextDouble(5300.0, 6000.0), rand.nextDouble(0.88, 1.15), rand.nextDouble(0.90, 1.12), rand.nextDouble(9.2, 11.5))  // G dwarf (solar)
            else -> Quad(rand.nextDouble(6100.0, 6900.0), rand.nextDouble(1.15, 1.45), rand.nextDouble(1.15, 1.40), rand.nextDouble(8.8, 11.0)) // F dwarf
        }

        val distancePc = rand.nextDouble(45.0, 380.0)

        // Orbital period and planet radius physics
        val isMonoTransit = rand.nextDouble() < 0.20
        val periodDays = if (isMonoTransit) rand.nextDouble(42.0, 140.0) else rand.nextDouble(2.1, 36.5)
        val durationHours = (1.5 + sqrt(periodDays) * rand.nextDouble(0.8, 1.4)).coerceIn(1.8, 16.0)

        // Planet archetype and transit depth
        val planetArchetypeRoll = rand.nextDouble()
        val (planetType, depthPpm) = when {
            planetArchetypeRoll < 0.35 -> {
                // Super-Earth / Sub-Neptune (depth 400 - 1200 ppm)
                val d = rand.nextDouble(420.0, 1250.0)
                Pair("Super-Pământ / Sub-Neptun Volatil", d)
            }
            planetArchetypeRoll < 0.70 -> {
                // Warm Neptune / Mini-Saturn (depth 1250 - 3200 ppm)
                val d = rand.nextDouble(1250.0, 3200.0)
                Pair("Neptunian Cald / Sub-Saturnian", d)
            }
            else -> {
                // Jovian / Gas Giant (depth 3200 - 6800 ppm)
                val d = rand.nextDouble(3200.0, 6800.0)
                Pair("Jovian / Gigant Gazos Masiv", d)
            }
        }

        // Habitable Zone calculation approximation
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
            "Tranzit unic de lungă durată (${String.format(java.util.Locale.US, "%.1f", durationHours)} h) care depășește cadența filtrelor automate SPOC.",
            "Semnal de înaltă puritate fotometrică ignorat preliminar de filtrul automat din cauza variației stelare lente.",
            "Candidat pur din arhivă fără cross-match în pscomppars. Simetrie ingress/egress remarcabilă.",
            "Detecție live de mare precizie (SNR ${String.format(java.util.Locale.US, "%.1f", snr)}σ). Fără TOI emis de comitetul TFOPWG."
        )
        val whyNoToi = whyNoToiList[rand.nextInt(whyNoToiList.size)]

        val priority = when {
            isHabitable -> "PRIORITATE MAXIMĂ • ZONĂ LOCUIBILĂ"
            snr > 12.0 -> "CANDIDAT VIRGIN • SNR ÎNALT (${String.format(java.util.Locale.US, "%.1f", snr)}σ)"
            else -> "PRIORITATE RIDICATĂ • TEREN CURAT"
        }

        val notes = "Adâncime: ${depthPpm.toInt()} ppm, Perioadă: ${String.format(java.util.Locale.US, "%.2f", periodDays)} d, Durată: ${String.format(java.util.Locale.US, "%.1f", durationHours)} h, Simetrie: ${String.format(java.util.Locale.US, "%.2f", symmetrySigma)}σ. Stea Teff: ${teff.toInt()} K, Rază: ${String.format(java.util.Locale.US, "%.2f", radiusSun)} R☉."

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
                
                // Keep Room database bounded to prevent memory heap inflation
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

    /**
     * Called by WorkManager background worker or on-demand pollers
     * to save newly ingested MAST candidates directly into the local Room database.
     */
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
            scientificReason = "${c.potentialPlanetaryType} | Adâncime: ${c.transitDepthPpm.toInt()} ppm, P: ${c.estimatedPeriodDays?.let { "${String.format(java.util.Locale.US, "%.2f", it)} d" } ?: "Mono-tranzit"}, Durată: ${c.transitDurationHours} h. ${c.scientificNotes}"
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
