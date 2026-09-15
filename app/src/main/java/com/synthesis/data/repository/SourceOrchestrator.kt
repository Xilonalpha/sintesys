package com.synthesis.data.repository

import com.synthesis.data.remote.api.*
import com.synthesis.domain.model.DataSource
import com.synthesis.domain.model.SourceStatus
import com.synthesis.domain.model.SourceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceOrchestrator @Inject constructor(
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
    private val jplHorizonsApi: JplHorizonsApi
) {
    private val _sources = MutableStateFlow<List<DataSource>>(emptyList())
    val sources: StateFlow<List<DataSource>> = _sources.asStateFlow()

    private val sourceConfigs = listOf(
        SourceConfig("nasa", "NASA NEO & APOD", SourceType.NASA, "https://api.nasa.gov"),
        SourceConfig("nasa_power", "NASA POWER Climate", SourceType.NASA, "https://power.larc.nasa.gov"),
        SourceConfig("usgs", "USGS Earthquake Catalog", SourceType.USGS, "https://earthquake.usgs.gov"),
        SourceConfig("open_meteo", "Open-Meteo Weather", SourceType.OPEN_METEO, "https://api.open-meteo.com"),
        SourceConfig("pubmed", "PubMed / NCBI", SourceType.PUBMED, "https://eutils.ncbi.nlm.nih.gov"),
        SourceConfig("noaa", "NOAA / NWS", SourceType.NOAA, "https://api.weather.gov"),
        SourceConfig("arxiv", "arXiv Papers", SourceType.ARXIV, "http://export.arxiv.org"),
        SourceConfig("gaia", "ESA Gaia DR3", SourceType.ASTRONOMICAL_CATALOG, "https://gaia.aip.de"),
        SourceConfig("cern", "CERN Open Data", SourceType.CERN, "https://opendata.cern.ch"),
        SourceConfig("copernicus", "Copernicus Data Space", SourceType.COPERNICUS, "https://catalogue.dataspace.copernicus.eu"),
        SourceConfig("gbif", "GBIF Biodiversity", SourceType.BIOLOGICAL, "https://api.gbif.org"),
        SourceConfig("sentinel", "ESA Sentinel Hub", SourceType.SATELLITE_CATALOG, "https://services.sentinel-hub.com"),
        SourceConfig("ncbi_virus", "NCBI Virus Genomes", SourceType.BIOLOGICAL, "https://api.ncbi.nlm.nih.gov"),
        SourceConfig("worldbank", "World Bank Open Data", SourceType.GEOLOGICAL, "https://api.worldbank.org"),
        SourceConfig("jpl", "JPL Horizons", SourceType.ASTRONOMICAL_CATALOG, "https://ssd.jpl.nasa.gov")
    )

    init {
        _sources.value = sourceConfigs.map { it.toDataSource(SourceStatus.MAINTENANCE, 0, 0f) }
    }

    suspend fun runHealthChecks() {
        val results = coroutineScope {
            sourceConfigs.map { config ->
                async(Dispatchers.IO) {
                    val startTime = System.currentTimeMillis()
                    val status = when (config.id) {
                        "nasa" -> checkNasaHealth()
                        "nasa_power" -> checkNasaPowerHealth()
                        "usgs" -> checkUsgsHealth()
                        "open_meteo" -> checkOpenMeteoHealth()
                        "pubmed" -> checkPubMedHealth()
                        "noaa" -> checkNoaaHealth()
                        "arxiv" -> checkArxivHealth()
                        "gaia" -> checkGaiaHealth()
                        "cern" -> checkCernHealth()
                        "copernicus" -> checkCopernicusHealth()
                        "gbif" -> checkGbifHealth()
                        "sentinel" -> checkSentinelHealth()
                        "ncbi_virus" -> checkNcbiVirusHealth()
                        "worldbank" -> checkWorldBankHealth()
                        "jpl" -> checkJplHealth()
                        else -> SourceStatus.OFFLINE
                    }
                    val latency = (System.currentTimeMillis() - startTime).toInt()
                    config.toDataSource(status, latency, if (status == SourceStatus.ONLINE) 0.95f else 0f)
                }
            }.awaitAll()
        }
        _sources.value = results
    }

    fun getOnlineSources(): List<DataSource> = _sources.value.filter { it.status == SourceStatus.ONLINE }
    fun getSourceByType(type: SourceType): DataSource? = _sources.value.find { it.type == type && it.status == SourceStatus.ONLINE }

    private suspend fun checkNasaHealth(): SourceStatus = safeCheck { nasaApi.getApod(apiKey = "DEMO_KEY").title != null }
    private suspend fun checkNasaPowerHealth(): SourceStatus = safeCheck { nasaPowerApi.getDailyPointData(longitude = 0.0, latitude = 0.0, start = "20240101", end = "20240102").properties != null }
    private suspend fun checkUsgsHealth(): SourceStatus = safeCheck { usgsApi.getEarthquakes(limit = 1).features != null }
    private suspend fun checkOpenMeteoHealth(): SourceStatus = safeCheck { openMeteoApi.getForecast(latitude = 52.52, longitude = 13.41, forecastDays = 1).current != null }
    private suspend fun checkPubMedHealth(): SourceStatus = safeCheck { pubMedApi.searchPubMed(term = "test", retmax = 1).esearchresult?.idlist != null }
    private suspend fun checkNoaaHealth(): SourceStatus = safeCheck { noaaApi.getPoints(latitude = 39.7456, longitude = -97.0892).properties != null }
    private suspend fun checkArxivHealth(): SourceStatus = safeCheck { arxivApi.searchPapers(query = "physics", maxResults = 1).contentLength() > 0 }
    private suspend fun checkGaiaHealth(): SourceStatus = safeCheck { gaiaApi.coneSearch(ra = 266.41683, dec = -29.00781, sr = 0.01, maxRec = 1).data != null }
    private suspend fun checkCernHealth(): SourceStatus = safeCheck { cernApi.searchRecords(query = "CMS", size = 1).hits?.hits != null }
    private suspend fun checkCopernicusHealth(): SourceStatus = safeCheck { copernicusApi.searchProducts(filter = "Collection/Name eq 'SENTINEL-2' and Online eq true", top = 1).value != null }
    private suspend fun checkGbifHealth(): SourceStatus = safeCheck { gbifApi.searchOccurrences(query = "Panthera leo", limit = 1).results != null }
    private suspend fun checkSentinelHealth(): SourceStatus = safeCheck {
        sentinelApi.searchCatalog(com.synthesis.data.remote.dto.SentinelCatalogRequest(
            bbox = listOf(12.0, 41.0, 13.0, 42.0),
            datetime = "2024-01-01T00:00:00Z/2024-01-02T00:00:00Z",
            collections = listOf("sentinel-2-l2a"),
            limit = 1
        )).features != null
    }
    private suspend fun checkNcbiVirusHealth(): SourceStatus = safeCheck { ncbiVirusApi.getVirusGenomes(taxonId = "2697049", pageSize = 1).reports != null }
    private suspend fun checkWorldBankHealth(): SourceStatus = safeCheck { worldBankApi.getIndicatorData(indicatorCode = "SP.POP.TOTL", perPage = 1).size() >= 2 }
    private suspend fun checkJplHealth(): SourceStatus = safeCheck {
        jplHorizonsApi.getEphemeris(
            command = "'499'", startTime = "2026-08-11", stopTime = "2026-08-12",
            center = "@399", stepSize = "1d"
        ).contentLength() > 0
    }

    private inline fun safeCheck(check: () -> Boolean): SourceStatus {
        return try {
            if (check()) SourceStatus.ONLINE else SourceStatus.DEGRADED
        } catch (e: Exception) {
            when (e) { is UnknownHostException, is SocketTimeoutException -> SourceStatus.OFFLINE else -> SourceStatus.DEGRADED }
        }
    }

    private data class SourceConfig(val id: String, val name: String, val type: SourceType, val endpoint: String) {
        fun toDataSource(status: SourceStatus, latency: Int, quality: Float) = DataSource(
            id = id, name = name, type = type, status = status,
            lastUpdate = System.currentTimeMillis(), apiLatency = latency,
            dataQuality = quality, coverage = "Global",
            license = "Public Domain / Open Access", endpoint = endpoint
        )
    }
}
