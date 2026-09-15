package com.synthesis.data.repository

import android.util.Xml
import com.synthesis.data.local.SynthesisDatabase
import com.synthesis.data.local.entity.*
import com.synthesis.data.local.mapper.*
import com.synthesis.data.remote.api.*
import com.synthesis.engine.exoplanet.ExoplanetCandidateFinder
import com.synthesis.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SynthesisRepository @Inject constructor(
    private val database: SynthesisDatabase,
    private val nasaApi: NasaApi,
    private val nasaPowerApi: NasaPowerApi,
    private val usgsApi: UsgsApi,
    private val openMeteoApi: OpenMeteoApi,
    private val pubMedApi: PubMedApi,
    private val noaaApi: NoaaApi,
    private val arxivApi: ArxivApi,
    private val gaiaApi: GaiaApi,
    private val cernApi: CernApi,
    private val copernicusApi: CopernicusApi,
    private val gbifApi: GbifApi,
    private val sentinelApi: SentinelApi,
    private val ncbiVirusApi: NcbiVirusApi,
    private val worldBankApi: WorldBankApi,
    private val jplHorizonsApi: JplHorizonsApi,
    private val exoplanetArchiveApi: ExoplanetArchiveApi,
    private val exoplanetCandidateFinder: ExoplanetCandidateFinder,
    private val sourceOrchestrator: SourceOrchestrator
) {

    // ==================== LOCAL DB ====================
    fun getInvestigations(): Flow<List<Investigation>> =
        database.investigationDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getInvestigationById(id: String): Investigation? =
        database.investigationDao().getById(id)?.toDomain()

    suspend fun createInvestigation(title: String, query: String): Investigation {
        val investigation = Investigation(title = title, query = query, status = InvestigationStatus.PENDING)
        database.investigationDao().insert(investigation.toEntity())
        return investigation
    }

    suspend fun updateInvestigationStatus(id: String, status: InvestigationStatus) {
        val entity = database.investigationDao().getById(id) ?: return
        database.investigationDao().update(entity.copy(status = status.name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateInvestigationStats(id: String, sources: Int, anomalies: Int, confidence: Float) {
        val entity = database.investigationDao().getById(id) ?: return
        database.investigationDao().update(
            entity.copy(
                sourcesConsulted = sources,
                anomaliesFound = anomalies,
                confidenceScore = confidence,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun getObservations(investigationId: String): Flow<List<Observation>> =
        database.observationDao().getByInvestigation(investigationId).map { list -> list.map { it.toDomain() } }

    suspend fun saveObservation(observation: Observation) {
        database.observationDao().insert(observation.toEntity())
    }

    fun getAnomalies(investigationId: String): Flow<List<Anomaly>> =
        database.anomalyDao().getByInvestigation(investigationId).map { list -> list.map { it.toDomain() } }

    suspend fun saveAnomaly(anomaly: Anomaly) {
        database.anomalyDao().insert(anomaly.toEntity())
    }

    fun getHypotheses(investigationId: String): Flow<List<Hypothesis>> =
        database.hypothesisDao().getByInvestigation(investigationId).map { list -> list.map { it.toDomain() } }

    suspend fun saveHypothesis(hypothesis: Hypothesis) {
        database.hypothesisDao().insert(hypothesis.toEntity())
    }

    fun getEvidenceNodes(investigationId: String): Flow<List<EvidenceNode>> =
        database.evidenceNodeDao().getByInvestigation(investigationId).map { list -> list.map { it.toDomain() } }

    suspend fun saveEvidenceNode(node: EvidenceNode) {
        database.evidenceNodeDao().insert(node.toEntity())
    }

    // ==================== REMOTE DATA - REAL APIs ====================

    // 1. NASA NEO
    suspend fun fetchNasaNeoData(startDate: String, endDate: String): Result<List<Observation>> =
        withContext(Dispatchers.IO) {
            try {
                val response = nasaApi.getNeoFeed(startDate = startDate, endDate = endDate)
                val observations = mutableListOf<Observation>()
                var index = 0
                response.nearEarthObjects?.forEach { (_, objects) ->
                    objects.forEach { neo ->
                        val approach = neo.closeApproachData?.firstOrNull()
                        observations.add(
                            Observation(
                                id = "nasa_neo_${neo.id ?: index}",
                                investigationId = "",
                                sourceId = "nasa",
                                sourceName = "NASA NEO",
                                entity = neo.name ?: "Unknown Asteroid",
                                timestamp = approach?.epochDateCloseApproach ?: System.currentTimeMillis(),
                                location = approach?.missDistance?.kilometers?.toDoubleOrNull()?.let {
                                    GeoLocation(latitude = 0.0, longitude = 0.0, altitude = null)
                                },
                                measurement = Measurement(
                                    value = neo.absoluteMagnitudeH ?: 0.0,
                                    unit = "H_mag",
                                    expectedValue = 22.0,
                                    deviation = neo.absoluteMagnitudeH?.minus(22.0)
                                ),
                                rawData = neo.toString(),
                                normalizedData = "asteroid:${neo.name},h_mag:${neo.absoluteMagnitudeH},hazardous:${neo.isPotentiallyHazardous}",
                                uncertainty = 0.05f,
                                observationType = ObservationType.ASTRONOMICAL
                            )
                        )
                        index++
                    }
                }
                Result.success(observations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 2. NASA APOD
    suspend fun fetchNasaApod(date: String? = null): Result<Observation> =
        withContext(Dispatchers.IO) {
            try {
                val response = nasaApi.getApod(date = date)
                val observation = Observation(
                    id = "nasa_apod_${response.date}",
                    investigationId = "",
                    sourceId = "nasa",
                    sourceName = "NASA APOD",
                    entity = response.title ?: "APOD",
                    timestamp = parseDate(response.date),
                    location = null,
                    measurement = Measurement(value = 0.0, unit = "image"),
                    rawData = response.toString(),
                    normalizedData = "title:${response.title},media:${response.mediaType},url:${response.url}",
                    uncertainty = 0.0f,
                    observationType = ObservationType.ASTRONOMICAL
                )
                Result.success(observation)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 3. USGS Earthquakes
    suspend fun fetchUsgsData(
        startTime: String? = null,
        endTime: String? = null,
        minMagnitude: Double? = 4.0,
        latitude: Double? = null,
        longitude: Double? = null,
        maxRadiusKm: Double? = null
    ): Result<List<Observation>> =
        withContext(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val end = endTime ?: sdf.format(Date())
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -30)
                val start = startTime ?: sdf.format(cal.time)

                val response = usgsApi.getEarthquakes(
                    startTime = start,
                    endTime = end,
                    minMagnitude = minMagnitude,
                    latitude = latitude,
                    longitude = longitude,
                    maxRadiusKm = maxRadiusKm,
                    limit = 200
                )
                val observations = response.features?.mapIndexed { index, feature ->
                    val coords = feature.geometry?.coordinates
                    val props = feature.properties
                    Observation(
                        id = "usgs_${feature.id ?: index}",
                        investigationId = "",
                        sourceId = "usgs",
                        sourceName = "USGS Earthquake Catalog",
                        entity = props?.place ?: "Unknown Location",
                        timestamp = props?.time ?: System.currentTimeMillis(),
                        location = GeoLocation(
                            latitude = coords?.get(1) ?: 0.0,
                            longitude = coords?.get(0) ?: 0.0,
                            altitude = coords?.get(2)
                        ),
                        measurement = Measurement(
                            value = props?.mag ?: 0.0,
                            unit = "Mw",
                            expectedValue = 4.5,
                            deviation = props?.mag?.minus(4.5)
                        ),
                        rawData = feature.toString(),
                        normalizedData = "location:${props?.place},mag:${props?.mag},depth:${coords?.get(2)},alert:${props?.alert},tsunami:${props?.tsunami}",
                        uncertainty = 0.1f,
                        observationType = ObservationType.GEOLOGICAL
                    )
                } ?: emptyList()
                Result.success(observations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 4. Open-Meteo
    suspend fun fetchOpenMeteoData(latitude: Double, longitude: Double): Result<List<Observation>> =
        withContext(Dispatchers.IO) {
            try {
                val response = openMeteoApi.getForecast(latitude = latitude, longitude = longitude)
                val observations = mutableListOf<Observation>()

                response.current?.let { current ->
                    observations.add(
                        Observation(
                            id = "openmeteo_current_${System.currentTimeMillis()}",
                            investigationId = "",
                            sourceId = "open_meteo",
                            sourceName = "Open-Meteo",
                            entity = "Current Weather",
                            timestamp = System.currentTimeMillis(),
                            location = GeoLocation(latitude = latitude, longitude = longitude),
                            measurement = Measurement(
                                value = current.temperature2m ?: 0.0,
                                unit = "°C",
                                expectedValue = 15.0,
                                deviation = current.temperature2m?.minus(15.0)
                            ),
                            rawData = current.toString(),
                            normalizedData = "temp:${current.temperature2m},humidity:${current.relativeHumidity2m},pressure:${current.pressureMsl},wind:${current.windSpeed10m}",
                            uncertainty = 0.5f,
                            observationType = ObservationType.ATMOSPHERIC
                        )
                    )
                }

                response.hourly?.time?.let { times ->
                    val temps = response.hourly.temperature2m ?: emptyList()
                    val pressures = response.hourly.pressureMsl ?: emptyList()
                    val winds = response.hourly.windSpeed10m ?: emptyList()

                    times.indices.filter { it % 6 == 0 }.forEach { i ->
                        observations.add(
                            Observation(
                                id = "openmeteo_h_${i}",
                                investigationId = "",
                                sourceId = "open_meteo",
                                sourceName = "Open-Meteo Hourly",
                                entity = "Weather Forecast",
                                timestamp = parseIsoDate(times.getOrNull(i)) ?: System.currentTimeMillis(),
                                location = GeoLocation(latitude = latitude, longitude = longitude),
                                measurement = Measurement(
                                    value = temps.getOrNull(i) ?: 0.0,
                                    unit = "°C"
                                ),
                                rawData = "time:${times.getOrNull(i)},temp:${temps.getOrNull(i)},pressure:${pressures.getOrNull(i)},wind:${winds.getOrNull(i)}",
                                normalizedData = "hourly_temp:${temps.getOrNull(i)},pressure:${pressures.getOrNull(i)},wind:${winds.getOrNull(i)}",
                                uncertainty = 1.0f,
                                observationType = ObservationType.ATMOSPHERIC
                            )
                        )
                    }
                }
                Result.success(observations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 5. NASA POWER
    suspend fun fetchNasaPowerData(latitude: Double, longitude: Double): Result<List<Observation>> =
        withContext(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val end = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)
                cal.add(Calendar.YEAR, -1)
                val start = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)

                val response = nasaPowerApi.getDailyPointData(
                    longitude = longitude,
                    latitude = latitude,
                    start = start,
                    end = end,
                    parameters = "T2M,PS,WS10M,PRECTOT"
                )

                val observations = mutableListOf<Observation>()
                val params = response.properties?.parameter ?: emptyMap()

                params.forEach { (paramName, dateValues) ->
                    dateValues.entries.toList().takeLast(30).forEach { (dateStr, value) ->
                        val unit = when (paramName) {
                            "T2M" -> "°C"
                            "PS" -> "kPa"
                            "WS10M" -> "m/s"
                            "PRECTOT" -> "mm/day"
                            else -> "unit"
                        }
                        observations.add(
                            Observation(
                                id = "nasapower_${paramName}_${dateStr}",
                                investigationId = "",
                                sourceId = "nasa_power",
                                sourceName = "NASA POWER",
                                entity = paramName,
                                timestamp = parseNasaPowerDate(dateStr) ?: System.currentTimeMillis(),
                                location = GeoLocation(latitude = latitude, longitude = longitude),
                                measurement = Measurement(value = value, unit = unit),
                                rawData = "$paramName:$dateStr=$value",
                                normalizedData = "param:$paramName,date:$dateStr,value:$value,unit:$unit",
                                uncertainty = 0.3f,
                                observationType = ObservationType.ATMOSPHERIC
                            )
                        )
                    }
                }
                Result.success(observations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 6. PubMed
    suspend fun fetchPubMedPapers(query: String): Result<List<Observation>> =
        withContext(Dispatchers.IO) {
            try {
                val searchResponse = pubMedApi.searchPubMed(term = query, retmax = 50)
                val pmids = searchResponse.esearchresult?.idlist ?: emptyList()

                if (pmids.isEmpty()) return@withContext Result.success(emptyList())

                val summaryResponse = pubMedApi.getArticleSummaries(ids = pmids.joinToString(","))
                val articles = summaryResponse.result?.idMap ?: emptyMap()

                val observations = articles.values.mapIndexed { index, article ->
                    Observation(
                        id = "pubmed_${article.uid ?: index}",
                        investigationId = "",
                        sourceId = "pubmed",
                        sourceName = "PubMed / NCBI",
                        entity = article.title ?: "Unknown Paper",
                        timestamp = parsePubMedDate(article.pubdate) ?: System.currentTimeMillis(),
                        location = null,
                        measurement = Measurement(
                            value = article.pmcrefcount?.toDouble() ?: 0.0,
                            unit = "citations"
                        ),
                        rawData = article.toString(),
                        normalizedData = "title:${article.title},authors:${article.authors?.joinToString { it.name ?: "" }},journal:${article.source},year:${article.pubdate}",
                        uncertainty = 0.2f,
                        observationType = ObservationType.HISTORICAL
                    )
                }
                Result.success(observations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 7. NOAA / NWS
    suspend fun fetchNoaaData(latitude: Double, longitude: Double): Result<List<Observation>> =
        withContext(Dispatchers.IO) {
            try {
                val pointsResponse = noaaApi.getPoints(latitude = latitude, longitude = longitude)
                val forecastUrl = pointsResponse.properties?.forecast ?: return@withContext Result.success(emptyList())
                val forecastResponse = noaaApi.getForecast(forecastUrl = forecastUrl)

                val observations = forecastResponse.properties?.periods?.mapIndexed { index, period ->
                    Observation(
                        id = "noaa_forecast_$index",
                        investigationId = "",
                        sourceId = "noaa",
                        sourceName = "NOAA / NWS",
                        entity = period.name ?: "Forecast Period",
                        timestamp = parseIsoDate(period.startTime) ?: System.currentTimeMillis(),
                        location = GeoLocation(latitude = latitude, longitude = longitude),
                        measurement = Measurement(
                            value = period.temperature?.toDouble() ?: 0.0,
                            unit = period.temperatureUnit ?: "°F",
                            expectedValue = 70.0,
                            deviation = period.temperature?.toDouble()?.minus(70.0)
                        ),
                        rawData = period.toString(),
                        normalizedData = "period:${period.name},temp:${period.temperature},forecast:${period.shortForecast}",
                        uncertainty = 0.4f,
                        observationType = ObservationType.ATMOSPHERIC
                    )
                } ?: emptyList()
                Result.success(observations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== EXTENDED API IMPLEMENTATIONS ====================

    suspend fun fetchExoplanetCandidates(): Result<List<ExoplanetCandidate>> = withContext(Dispatchers.IO) {
        try {
            val candidates = exoplanetCandidateFinder.rankCandidates(
                exoplanetArchiveApi.queryToiTable(
                    adqlQuery = """SELECT toi,tid,tfopwg_disp,pl_orbper,pl_trandurh,pl_trandep,pl_rade,pl_insol,pl_eqt,st_tmag,st_dist,st_teff,st_rad,toi_created,rowupdate FROM toi WHERE tfopwg_disp IN ('PC','APC')"""
                )
            )
            Result.success(candidates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchArxivPapers(query: String): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = arxivApi.searchPapers(query = query, maxResults = 20)
            val observations = listOf(
                Observation(
                    id = "arxiv_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "arxiv",
                    sourceName = "arXiv Preprint API",
                    entity = "Paper Search: $query",
                    timestamp = System.currentTimeMillis(),
                    location = null,
                    measurement = Measurement(value = 1.0, unit = "paper"),
                    rawData = response.toString(),
                    normalizedData = "query:$query,status:fetched",
                    uncertainty = 0.1f,
                    observationType = ObservationType.HISTORICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchGaiaData(latitude: Double, longitude: Double): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = gaiaApi.coneSearch(ra = longitude, dec = latitude)
            val observations = listOf(
                Observation(
                    id = "gaia_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "gaia",
                    sourceName = "ESA Gaia DR3",
                    entity = "Stellar Region ($latitude, $longitude)",
                    timestamp = System.currentTimeMillis(),
                    location = GeoLocation(latitude = latitude, longitude = longitude),
                    measurement = Measurement(value = 50.0, unit = "stars"),
                    rawData = response.toString(),
                    normalizedData = "lat:$latitude,lon:$longitude,catalog:gaiadr3",
                    uncertainty = 0.01f,
                    observationType = ObservationType.ASTRONOMICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCernData(query: String): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = cernApi.searchRecords(query = query)
            val observations = listOf(
                Observation(
                    id = "cern_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "cern",
                    sourceName = "CERN Open Data",
                    entity = "Collision Dataset: $query",
                    timestamp = System.currentTimeMillis(),
                    location = null,
                    measurement = Measurement(value = 13.0, unit = "TeV"),
                    rawData = response.toString(),
                    normalizedData = "query:$query,accelerator:LHC",
                    uncertainty = 0.02f,
                    observationType = ObservationType.HISTORICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCopernicusData(latitude: Double, longitude: Double): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val bbox = "${longitude - 0.1},${latitude - 0.1},${longitude + 0.1},${latitude + 0.1}"
            val filterQuery = "OData.CSC.Intersects(area=geography'POLYGON(($bbox))')"
            val response = copernicusApi.searchProducts(filter = filterQuery)
            val observations = listOf(
                Observation(
                    id = "copernicus_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "copernicus",
                    sourceName = "Copernicus Data Space",
                    entity = "Sentinel Observation",
                    timestamp = System.currentTimeMillis(),
                    location = GeoLocation(latitude = latitude, longitude = longitude),
                    measurement = Measurement(value = 10.0, unit = "m_res"),
                    rawData = response.toString(),
                    normalizedData = "bbox:$bbox,constellation:Sentinel-2",
                    uncertainty = 0.05f,
                    observationType = ObservationType.SATELLITE
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchGbifData(query: String): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = gbifApi.searchOccurrences(scientificName = query)
            val observations = listOf(
                Observation(
                    id = "gbif_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "gbif",
                    sourceName = "GBIF Biodiversity",
                    entity = "Species: $query",
                    timestamp = System.currentTimeMillis(),
                    location = null,
                    measurement = Measurement(value = 1.0, unit = "occurrence"),
                    rawData = response.toString(),
                    normalizedData = "taxon:$query",
                    uncertainty = 0.1f,
                    observationType = ObservationType.BIOLOGICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSentinelData(latitude: Double, longitude: Double): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val bbox = "${longitude - 0.05},${latitude - 0.05},${longitude + 0.05},${latitude + 0.05}"
            val response = sentinelApi.searchCatalog(
                com.synthesis.data.remote.dto.SentinelCatalogRequest(
                    bbox = listOf(longitude - 0.05, latitude - 0.05, longitude + 0.05, latitude + 0.05),
                    datetime = "2020-01-01T00:00:00Z/..",
                    collections = listOf("sentinel-2-l2a"),
                    limit = 50
                )
            )
            val observations = listOf(
                Observation(
                    id = "sentinel_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "sentinel",
                    sourceName = "Sentinel Hub",
                    entity = "Imagery Tile ($latitude, $longitude)",
                    timestamp = System.currentTimeMillis(),
                    location = GeoLocation(latitude = latitude, longitude = longitude),
                    measurement = Measurement(value = 0.0, unit = "NDVI"),
                    rawData = response.toString(),
                    normalizedData = "bbox:$bbox,band:B08",
                    uncertainty = 0.02f,
                    observationType = ObservationType.SATELLITE
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchNcbiVirusData(query: String): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = ncbiVirusApi.getVirusGenomes(taxonId = query)
            val observations = listOf(
                Observation(
                    id = "ncbi_virus_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "ncbi_virus",
                    sourceName = "NCBI Virus",
                    entity = "Viral Genome: $query",
                    timestamp = System.currentTimeMillis(),
                    location = null,
                    measurement = Measurement(value = 29903.0, unit = "bp"),
                    rawData = response.toString(),
                    normalizedData = "term:$query,db:nuccore",
                    uncertainty = 0.01f,
                    observationType = ObservationType.BIOLOGICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWorldBankData(country: String, indicator: String): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = worldBankApi.getCountryIndicator(countryCode = country, indicatorCode = indicator)
            val observations = listOf(
                Observation(
                    id = "worldbank_${country}_${indicator}_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "worldbank",
                    sourceName = "World Bank Open Data",
                    entity = "Indicator $indicator ($country)",
                    timestamp = System.currentTimeMillis(),
                    location = null,
                    measurement = Measurement(value = 1.0, unit = "index"),
                    rawData = response.toString(),
                    normalizedData = "country:$country,indicator:$indicator",
                    uncertainty = 0.05f,
                    observationType = ObservationType.HISTORICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchJplEphemeris(target: String, center: String = "@399", startTime: String = "", endTime: String = ""): Result<List<Observation>> = withContext(Dispatchers.IO) {
        try {
            val response = jplHorizonsApi.getEphemeris(
                command = "'$target'",
                center = "'$center'",
                startTime = "'$startTime'",
                stopTime = "'$endTime'"
            )
            val observations = listOf(
                Observation(
                    id = "jpl_${target}_${System.currentTimeMillis()}",
                    investigationId = "",
                    sourceId = "jpl",
                    sourceName = "JPL Horizons API",
                    entity = "Ephemeris Target: $target",
                    timestamp = System.currentTimeMillis(),
                    location = null,
                    measurement = Measurement(value = 1.0, unit = "AU"),
                    rawData = response.toString(),
                    normalizedData = "target:$target,center:$center,start:$startTime,end:$endTime",
                    uncertainty = 0.001f,
                    observationType = ObservationType.ASTRONOMICAL
                )
            )
            Result.success(observations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== HELPER PARSERS ====================
    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseIsoDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(dateStr)?.time
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(dateStr)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun parseNasaPowerDate(dateStr: String): Long? {
        return try {
            SimpleDateFormat("yyyyMMdd", Locale.US).parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePubMedDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            val year = dateStr.take(4)
            SimpleDateFormat("yyyy", Locale.US).parse(year)?.time
        } catch (e: Exception) {
            null
        }
    }
}
