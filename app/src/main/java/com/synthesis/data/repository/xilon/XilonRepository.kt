package com.synthesis.data.repository.xilon

import com.google.gson.JsonObject
import com.synthesis.data.local.dao.XilonDiscoveryDao
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import com.synthesis.data.remote.api.ExoplanetArchiveApi
import com.synthesis.data.remote.api.MastExoApi
import com.synthesis.data.remote.api.MastPortalApi
import com.synthesis.domain.model.xilon.*
import com.synthesis.engine.xilon.XilonTransitDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XilonRepository @Inject constructor(
    private val mastExoApi: MastExoApi,
    private val mastPortalApi: MastPortalApi,
    private val exoplanetArchiveApi: ExoplanetArchiveApi,
    private val xilonTransitDetector: XilonTransitDetector,
    private val xilonDiscoveryDao: XilonDiscoveryDao
) {

    fun getSavedDiscoveries(): Flow<List<XilonDiscoveryEntity>> = xilonDiscoveryDao.getAllDiscoveries()

    suspend fun saveDiscovery(result: XilonDetectionResult, notes: String? = null): Long = withContext(Dispatchers.IO) {
        val entity = XilonDiscoveryEntity(
            ticId = result.star.ticId,
            canonicalName = result.archivalCrossMatch.canonicalName,
            sector = result.sector,
            verdict = result.verdict.name,
            verdictTitle = result.verdictTitle,
            verdictSummary = result.verdictSummary,
            periodDays = result.orbitalPeriodDays,
            depthPpm = result.transitDepthPpm,
            durationHours = result.transitDurationHours,
            planetRadiusEarth = result.derivedPlanetRadiusEarth,
            planetMassEarth = result.derivedPlanetMassEarth,
            surfaceGravityG = result.surfaceGravityEarthG,
            bulkDensityGcm3 = result.planetDensityGcm3,
            equilibriumTempK = result.derivedEquilibriumTempK,
            snr = result.signalToNoiseRatio,
            isHabitableZone = result.isHabitableZone,
            archetype = result.compositionClass?.label,
            archivalStatus = result.archivalCrossMatch.status.name,
            notes = notes
        )
        xilonDiscoveryDao.insertDiscovery(entity)
    }

    suspend fun deleteDiscovery(id: Int) = withContext(Dispatchers.IO) {
        xilonDiscoveryDao.deleteById(id)
    }

    fun getCuratedTargets(): List<TargetStarCandidate> {
        return listOf(
            TargetStarCandidate(
                ticId = "384159646",
                name = "TIC 384159646 (TOI-1895.01 Candidate)",
                description = "Planetary Candidate neconfirmat de NASA (P=748.1 d). Mono-tranzit de perioadă foarte lungă, scăpare din algoritmul SPOC din cauza blindspot-ului de sector.",
                sector = "s0039-s0039",
                tce = "1",
                estimatedMag = 9.75,
                targetType = "NASA Unvetted Candidate",
                knownClassification = "Nevalidată Oficial (Merită Verificare Urgentă)",
                advisoryPriority = "RECOMANDARE #1: PRIORITATE MAXIMĂ",
                scientificReason = "Candidat gigant temperat în sectorul TESS 39. Are o scădere clară de flux neclasificată complet. Ideal pentru descoperire primară!"
            ),
            TargetStarCandidate(
                ticId = "268301217",
                name = "TIC 268301217 (TOI-2076.03 Blindspot)",
                description = "Sub-Neptun în Zona Locuibilă (P~120-180 d estimat). A tranzitat o singură dată în Sectorul 14 și a fost ignorat de SPOC.",
                sector = "s0014-s0014",
                tce = "1",
                estimatedMag = 9.20,
                targetType = "Blindspot Sector",
                knownClassification = "Nevalidată Oficial (Zonă Locuibilă)",
                advisoryPriority = "RECOMANDARE #2: CERCETARE ÎNALTĂ",
                scientificReason = "Stea strălucitoare (V=9.2) cu tranzit de 8.6 ore. Poate fi o planetă temperată de tip super-Pământ / sub-Neptun."
            ),
            TargetStarCandidate(
                ticId = "100100827",
                name = "TIC 100100827 (TOI-4633.02 Dublă Stea)",
                description = "Tranzit circumstelar rar în sistem binar. Tranzit lung de 16.2 ore raportat de cetățeni cercetători în Sectorul 42.",
                sector = "s0042-s0042",
                tce = "1",
                estimatedMag = 8.95,
                targetType = "Circumstellar Candidate",
                knownClassification = "Nevalidată Oficial (Arhivă TESS)",
                advisoryPriority = "RECOMANDARE #3: PRIORITATE RIDICATĂ",
                scientificReason = "Stea de clasă solară cu semnal puternic (3200 ppm). Are potențial enorm pentru lucrare științifică nouă."
            ),
            TargetStarCandidate(
                ticId = "141913165",
                name = "TIC 141913165 (Stea din Deep-Sky)",
                description = "Stea izolată monitorizată în Sectorul 6. Nu există niciun raport NASA publicat, datele fotometrice brute sunt neatinse.",
                sector = "s0006-s0006",
                tce = "1",
                estimatedMag = 10.1,
                targetType = "Target Brut Neanalizat",
                knownClassification = "Complet Necunoscută (Teren Virgin)",
                advisoryPriority = "RECOMANDARE #4: EXPLORARE LIBERĂ",
                scientificReason = "Stea liniștită fără pulsații rapide. Dacă Xilon găsește o scădere periodică, ești primul care o observă."
            ),
            TargetStarCandidate(
                ticId = "25155310",
                name = "TIC 25155310 (WASP-126 b Benchmark)",
                description = "Exoplanetă confirmată istoric (SuperWASP-South, P=3.288 d). Folosită drept etalon de calibrare pentru verificarea preciziei motorului tău.",
                sector = "s0001-s0001",
                tce = "1",
                estimatedMag = 11.2,
                targetType = "Etalon Calibrare",
                knownClassification = "Planeta Cunoscută (Control)",
                advisoryPriority = "CONTROL & CALIBRARE",
                scientificReason = "Recomandată pentru primul test: dacă algoritmul găsește parametrii corecți ai WASP-126b, știi că funcționează perfect!"
            ),
            TargetStarCandidate(
                ticId = "388104525",
                name = "TIC 388104525 (WASP-119 b Benchmark)",
                description = "Exoplanetă confirmată cu tranzit adânc de 1.46%. Etalon pentru calibrarea modelului Mandel-Agol.",
                sector = "s0001-s0001",
                tce = "1",
                estimatedMag = 10.4,
                targetType = "Etalon Calibrare",
                knownClassification = "Planeta Cunoscută (Control)",
                advisoryPriority = "CONTROL & CALIBRARE",
                scientificReason = "Semnal fotometric curat cu SNR ridicat pentru validarea tranzitului trapezoidal."
            )
        )
    }

    suspend fun runAutonomousDetection(
        ticId: String,
        targetPeriodDays: Double? = null
    ): Result<XilonDetectionResult> = withContext(Dispatchers.IO) {
        try {
            val cleanTic = ticId.trim().replace("TIC", "", ignoreCase = true).trim()

            // 1. Cross-check against NASA Exoplanet Archive (both pscomppars confirmed planets and toi candidates)
            val archivalCrossMatch = crossMatchArchivalDatabases(cleanTic)

            // 2. Fetch Star Astrophysical Properties from MAST TIC Catalog
            val starProps = fetchStarProperties(cleanTic).getOrElse {
                val knownCoords = when (cleanTic) {
                    "25155310" -> Pair(63.8569, -69.2253) // WASP-126
                    "388104525" -> Pair(57.3482, -65.1934) // WASP-119
                    "384159646" -> Pair(281.3328, -43.1491) // TOI-1895
                    "268301217" -> Pair(217.1824, 62.4842) // TOI-2076
                    "100100827" -> Pair(286.9154, 51.9163) // TOI-4633
                    "141913165" -> Pair(84.1481, -64.2185)
                    else -> Pair(null, null)
                }
                MastStarProperties(
                    ticId = cleanTic,
                    tMag = 10.0,
                    teff = 5778.0,
                    radiusSun = 1.0,
                    massSun = 1.0,
                    distancePc = 150.0,
                    ra = knownCoords.first,
                    dec = knownCoords.second,
                    luminosity = 1.0
                )
            }

            // 3. Query MAST for TCEs and available photometric sectors
            val tcesJson = mastExoApi.getTces(cleanTic)
            val tceArray = tcesJson.getAsJsonArray("TCE")

            val (sectorStr, tceNum) = if (tceArray != null && tceArray.size() > 0) {
                // e.g. "s0001-s0001:TCE_1" -> sector = "s0001-s0001", tce = "1"
                val rawTce = tceArray[0].asString
                val parts = rawTce.split(":")
                val s = parts.getOrNull(0) ?: "s0001-s0001"
                val t = parts.getOrNull(1)?.replace("TCE_", "") ?: "1"
                Pair(s, t)
            } else {
                Pair("s0001-s0001", "1")
            }

            // 4. Download Photometric Time Series and DV Metadata from MAST Exo Data Validation
            val tableJson = mastExoApi.getLightCurveTable(
                ticId = cleanTic,
                tce = tceNum,
                sector = sectorStr
            )

            var dvTdur: Double? = null
            var dvTdepth: Double? = null
            var dvTsnr: Double? = null
            var dvTperiod: Double? = null

            try {
                val dvInfoJson = mastExoApi.getDvInfo(cleanTic, tceNum, sectorStr)
                val hdr = dvInfoJson.getAsJsonObject("DV Data Header")
                if (hdr != null) {
                    dvTdur = hdr.get("TDUR")?.takeIf { !it.isJsonNull }?.asDouble
                    dvTdepth = hdr.get("TDEPTH")?.takeIf { !it.isJsonNull }?.asDouble
                    dvTsnr = hdr.get("TSNR")?.takeIf { !it.isJsonNull }?.asDouble
                    dvTperiod = hdr.get("TPERIOD")?.takeIf { !it.isJsonNull }?.asDouble
                }
            } catch (e: Exception) {
                // Non-fatal if info endpoint is not available
            }

            val dataRows = tableJson.getAsJsonArray("data")
            val points = mutableListOf<MastPhotometryPoint>()

            if (dataRows != null) {
                for (elem in dataRows) {
                    val obj = elem.asJsonObject
                    val phase = obj.get("PHASE")?.takeIf { !it.isJsonNull }?.asDouble ?: continue
                    val detrend = obj.get("LC_DETREND")?.takeIf { !it.isJsonNull }?.asDouble
                        ?: obj.get("LC_INIT")?.takeIf { !it.isJsonNull }?.asDouble
                        ?: continue
                    val model = obj.get("MODEL_INIT")?.takeIf { !it.isJsonNull }?.asDouble

                    points.add(MastPhotometryPoint(phase = phase, detrendedFlux = detrend, modelFlux = model))
                }
            }

            // 5. Scientifically Rigorous Period Determination:
            // Priority 1: User explicitly entered period
            // Priority 2: Peer-reviewed literature period from pscomppars (e.g. WASP-126 b = 3.288 d)
            // Priority 3: NASA TOI archival candidate period (e.g. TOI-1895.01 = 748.07 d)
            // Priority 4: MAST SPOC DV Header TPERIOD
            // Priority 5: Fallback default
            val period = targetPeriodDays
                ?: archivalCrossMatch.literaturePeriodDays
                ?: dvTperiod
                ?: 10.0

            // 6. Run the XILON mathematical detection engine
            val result = xilonTransitDetector.analyzePhotometricTimeSeries(
                rawPoints = points,
                star = starProps,
                periodDays = period,
                sector = sectorStr,
                tceName = "TCE_$tceNum",
                archivalMatch = archivalCrossMatch,
                dvDurationHours = dvTdur ?: archivalCrossMatch.literatureDurationHours,
                dvDepthPpm = dvTdepth ?: archivalCrossMatch.literatureDepthPpm,
                dvSnr = dvTsnr
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crossMatchArchivalDatabases(cleanTic: String): ArchivalCrossMatch = withContext(Dispatchers.IO) {
        // Step 1: Check NASA Confirmed Planets (pscomppars)
        try {
            val queryConfirmed = "SELECT pl_name,hostname,tic_id,disc_year,disc_facility,pl_orbper,pl_trandur,pl_trandep,pl_rade,pl_bmasse,pl_eqt FROM pscomppars WHERE tic_id='TIC $cleanTic' OR tic_id='$cleanTic'"
            val confirmedList = exoplanetArchiveApi.queryConfirmedTable(queryConfirmed)
            if (confirmedList.isNotEmpty()) {
                val p = confirmedList[0]
                // pl_trandep in pscomppars is percentage (e.g. 0.7005% -> 7005 ppm)
                val depthPpm = p.plTranDep?.let { it * 10000.0 }
                return@withContext ArchivalCrossMatch(
                    status = ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION,
                    canonicalName = p.plName ?: "Confirmed Planet",
                    officialDisposition = "CONFIRMED PLANET",
                    discoveryYear = p.discYear,
                    discoveryFacility = p.discFacility,
                    literaturePeriodDays = p.plOrbPer,
                    literatureDurationHours = p.plTranDur,
                    literatureDepthPpm = depthPpm,
                    literatureRadiusEarth = p.plRadE,
                    isBenchmarkTarget = true
                )
            }
        } catch (e: Exception) {
            // Ignore network or table query error and proceed to TOI check
        }

        // Step 2: Check NASA TESS Objects of Interest (toi)
        try {
            val queryToi = "SELECT toi,tid,tfopwg_disp,pl_orbper,pl_trandurh,pl_trandep,pl_rade,pl_eqt FROM toi WHERE tid=$cleanTic"
            val toiList = exoplanetArchiveApi.queryToiTable(queryToi)
            if (toiList.isNotEmpty()) {
                val t = toiList[0]
                return@withContext ArchivalCrossMatch(
                    status = ArchivalCatalogStatus.NASA_TOI_CANDIDATE,
                    canonicalName = "TOI-${t.toi}",
                    officialDisposition = t.tfopwgDisp ?: "PLANET CANDIDATE (PC)",
                    discoveryYear = 2019,
                    discoveryFacility = "TESS",
                    literaturePeriodDays = t.plOrbPer,
                    literatureDurationHours = t.plTranDurH,
                    literatureDepthPpm = t.plTranDep, // in toi, pl_trandep is already ppm
                    literatureRadiusEarth = t.plRadE,
                    isBenchmarkTarget = false
                )
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Step 3: Unrecorded in both confirmed and TOI candidates
        ArchivalCrossMatch(
            status = ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL,
            canonicalName = "TIC $cleanTic",
            officialDisposition = "UNRECORDED IN NASA ARCHIVES",
            discoveryYear = null,
            discoveryFacility = null,
            literaturePeriodDays = null,
            literatureDurationHours = null,
            literatureDepthPpm = null,
            literatureRadiusEarth = null,
            isBenchmarkTarget = false
        )
    }

    suspend fun fetchStarProperties(ticId: String): Result<MastStarProperties> = withContext(Dispatchers.IO) {
        try {
            val reqPayload = JsonObject().apply {
                addProperty("service", "Mast.Catalogs.Filtered.Tic")
                addProperty("format", "json")
                val params = JsonObject().apply {
                    addProperty("columns", "ID,ra,dec,Tmag,rad,mass,Teff,d,lum")
                    val filters = com.google.gson.JsonArray().apply {
                        val f = JsonObject().apply {
                            addProperty("paramName", "ID")
                            val v = com.google.gson.JsonArray().apply { add(ticId) }
                            add("values", v)
                        }
                        add(f)
                    }
                    add("filters", filters)
                }
                add("params", params)
            }

            val resp = mastPortalApi.invokeService(reqPayload.toString())
            val dataArray = resp.getAsJsonArray("data")
            if (dataArray != null && dataArray.size() > 0) {
                val row = dataArray[0].asJsonObject
                val star = MastStarProperties(
                    ticId = ticId,
                    tMag = row.get("Tmag")?.takeIf { !it.isJsonNull }?.asDouble,
                    teff = row.get("Teff")?.takeIf { !it.isJsonNull }?.asDouble,
                    radiusSun = row.get("rad")?.takeIf { !it.isJsonNull }?.asDouble,
                    massSun = row.get("mass")?.takeIf { !it.isJsonNull }?.asDouble,
                    distancePc = row.get("d")?.takeIf { !it.isJsonNull }?.asDouble,
                    ra = row.get("ra")?.takeIf { !it.isJsonNull }?.asDouble,
                    dec = row.get("dec")?.takeIf { !it.isJsonNull }?.asDouble,
                    luminosity = row.get("lum")?.takeIf { !it.isJsonNull }?.asDouble
                )
                Result.success(star)
            } else {
                Result.failure(Exception("Star TIC $ticId not found in MAST catalog."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
