package com.synthesis.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.synthesis.data.local.dao.PureTicDao
import com.synthesis.data.local.dao.XilonDiscoveryDao
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import com.synthesis.engine.astrometry.AstrometryEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class NasaExoFopExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val xilonDiscoveryDao: XilonDiscoveryDao,
    private val pureTicDao: PureTicDao,
    private val astrometryEngine: AstrometryEngine,
    val validationEngine: com.synthesis.engine.validation.TessHeasarcValidationEngine
) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeSpecialFloatingPointValues()
        .create()

    data class ExoFopCtoiRecord(
        val ticId: Long,
        val targetName: String,
        val sector: String,
        val raDeg: Double,
        val decDeg: Double,
        val raSexagesimal: String,
        val decSexagesimal: String,
        val galacticLongitudeDeg: Double,
        val galacticLatitudeDeg: Double,
        val coordFrame: String = "ICRS_J2000",
        val epochBjdTdb: Double,
        val epochBtjd: Double,
        val orbitalPeriodDays: Double,
        val transitDurationHours: Double,
        val transitDurationDays: Double,
        val depthPpm: Double,
        val depthPercent: Double,
        val depthFluxFraction: Double,
        val signalToNoiseRatio: Double,
        val planetRadiusEarth: Double?,
        val planetRadiusJupiter: Double?,
        val planetMassEarth: Double?,
        val semiMajorAxisAu: Double?,
        val equilibriumTempK: Double?,
        val surfaceGravityG: Double?,
        val bulkDensityGcm3: Double?,
        val isHabitableZone: Boolean,
        val ctoiDisposition: String,
        val verdictTitle: String,
        val archivalStatus: String,
        val notes: String?,
        val exportTimestampUtc: String,
        val provenance: String = "SYNTHESIS XILON-SPARK NASA EXOFOP PIPELINE v2.4"
    )

    data class NasaExoFopReport(
        val mission: String = "NASA Transiting Exoplanet Survey Satellite (TESS)",
        val facility: String = "NASA Exoplanet Science Institute (NExScI) / ExoFOP-TESS",
        val standardSpecification: String = "NASA ExoFOP-TESS CTOI Upload Specification v2.4",
        val pipelineSoftware: String = "SYNTHESIS Space Science OS (XILON Autonomous Transit Detector & SPARK Engine)",
        val generatedAtUtc: String,
        val totalDiscoveriesExported: Int,
        val discoveries: List<ExoFopCtoiRecord>
    )

    suspend fun exportAllDiscoveriesJson(): String = withContext(Dispatchers.IO) {
        val entities = xilonDiscoveryDao.getDiscoveriesList()
        val records = entities.map { convertEntityToCtoiRecord(it) }
        val nowIso = getUtcTimestampIso()

        val report = NasaExoFopReport(
            generatedAtUtc = nowIso,
            totalDiscoveriesExported = records.size,
            discoveries = records
        )

        gson.toJson(report)
    }

    suspend fun exportAllDiscoveriesCsv(): String = withContext(Dispatchers.IO) {
        val entities = xilonDiscoveryDao.getDiscoveriesList()
        val records = entities.map { convertEntityToCtoiRecord(it) }
        buildCsvFromRecords(records)
    }

    suspend fun exportSingleDiscoveryJson(id: Int): String? = withContext(Dispatchers.IO) {
        val entity = xilonDiscoveryDao.getDiscoveriesList().firstOrNull { it.id == id } ?: return@withContext null
        val record = convertEntityToCtoiRecord(entity)
        val report = NasaExoFopReport(
            generatedAtUtc = getUtcTimestampIso(),
            totalDiscoveriesExported = 1,
            discoveries = listOf(record)
        )
        gson.toJson(report)
    }

    suspend fun exportSingleDiscoveryCsv(id: Int): String? = withContext(Dispatchers.IO) {
        val entity = xilonDiscoveryDao.getDiscoveriesList().firstOrNull { it.id == id } ?: return@withContext null
        val record = convertEntityToCtoiRecord(entity)
        buildCsvFromRecords(listOf(record))
    }

    suspend fun exportDiscoveryByTicJson(ticId: String): String? = withContext(Dispatchers.IO) {
        val cleanTic = ticId.replace("TIC", "", ignoreCase = true).trim()
        val entity = xilonDiscoveryDao.getByTicId(cleanTic) ?: return@withContext null
        val record = convertEntityToCtoiRecord(entity)
        val report = NasaExoFopReport(
            generatedAtUtc = getUtcTimestampIso(),
            totalDiscoveriesExported = 1,
            discoveries = listOf(record)
        )
        gson.toJson(report)
    }

    suspend fun exportDiscoveryByTicCsv(ticId: String): String? = withContext(Dispatchers.IO) {
        val cleanTic = ticId.replace("TIC", "", ignoreCase = true).trim()
        val entity = xilonDiscoveryDao.getByTicId(cleanTic) ?: return@withContext null
        val record = convertEntityToCtoiRecord(entity)
        buildCsvFromRecords(listOf(record))
    }

    suspend fun exportDetectionResultJson(res: com.synthesis.domain.model.xilon.XilonDetectionResult): String = withContext(Dispatchers.IO) {
        val entity = XilonDiscoveryEntity(
            ticId = res.star.ticId,
            canonicalName = res.archivalCrossMatch.canonicalName,
            sector = res.sector,
            verdict = res.verdict.name,
            verdictTitle = res.verdictTitle,
            verdictSummary = res.verdictSummary,
            periodDays = res.orbitalPeriodDays,
            depthPpm = res.transitDepthPpm,
            durationHours = res.transitDurationHours,
            planetRadiusEarth = res.derivedPlanetRadiusEarth,
            planetMassEarth = res.derivedPlanetMassEarth,
            surfaceGravityG = res.surfaceGravityEarthG,
            bulkDensityGcm3 = res.planetDensityGcm3,
            equilibriumTempK = res.derivedEquilibriumTempK,
            snr = res.signalToNoiseRatio,
            isHabitableZone = res.isHabitableZone,
            archetype = res.compositionClass?.label,
            archivalStatus = res.archivalCrossMatch.status.name,
            raDeg = res.star.ra ?: res.astrometry?.raDeg,
            decDeg = res.star.dec ?: res.astrometry?.decDeg,
            epochBjd = res.astrometry?.bjdTdbCurrent
        )
        val record = convertEntityToCtoiRecord(entity)
        val report = NasaExoFopReport(
            generatedAtUtc = getUtcTimestampIso(),
            totalDiscoveriesExported = 1,
            discoveries = listOf(record)
        )
        gson.toJson(report)
    }

    suspend fun exportDetectionResultCsv(res: com.synthesis.domain.model.xilon.XilonDetectionResult): String = withContext(Dispatchers.IO) {
        val entity = XilonDiscoveryEntity(
            ticId = res.star.ticId,
            canonicalName = res.archivalCrossMatch.canonicalName,
            sector = res.sector,
            verdict = res.verdict.name,
            verdictTitle = res.verdictTitle,
            verdictSummary = res.verdictSummary,
            periodDays = res.orbitalPeriodDays,
            depthPpm = res.transitDepthPpm,
            durationHours = res.transitDurationHours,
            planetRadiusEarth = res.derivedPlanetRadiusEarth,
            planetMassEarth = res.derivedPlanetMassEarth,
            surfaceGravityG = res.surfaceGravityEarthG,
            bulkDensityGcm3 = res.planetDensityGcm3,
            equilibriumTempK = res.derivedEquilibriumTempK,
            snr = res.signalToNoiseRatio,
            isHabitableZone = res.isHabitableZone,
            archetype = res.compositionClass?.label,
            archivalStatus = res.archivalCrossMatch.status.name,
            raDeg = res.star.ra ?: res.astrometry?.raDeg,
            decDeg = res.star.dec ?: res.astrometry?.decDeg,
            epochBjd = res.astrometry?.bjdTdbCurrent
        )
        val record = convertEntityToCtoiRecord(entity)
        buildCsvFromRecords(listOf(record))
    }

    suspend fun saveReportToFile(content: String, fileName: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exofop_exports").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(exportDir, fileName)
        targetFile.writeText(content, Charsets.UTF_8)
        targetFile
    }

    suspend fun convertEntityToCtoiRecord(entity: XilonDiscoveryEntity): ExoFopCtoiRecord = withContext(Dispatchers.IO) {
        val cleanTicLong = entity.ticId.replace("TIC", "", ignoreCase = true).trim().toLongOrNull() ?: 0L
        val (raDeg, decDeg) = resolveIcrsCoordinates(entity)
        val galactic = astrometryEngine.equatorialToGalactic(raDeg, decDeg)
        val raSexagesimal = astrometryEngine.formatSexagesimalRa(raDeg)
        val decSexagesimal = astrometryEngine.formatSexagesimalDec(decDeg)

        val epochBjd = entity.epochBjd ?: astrometryEngine.unixMsToBjdTdb(entity.scannedAt)
        val epochBtjd = epochBjd - 2457000.0
        val depthPpm = entity.depthPpm
        val depthPercent = depthPpm / 10000.0
        val depthFraction = depthPpm / 1_000_000.0
        val durationDays = entity.durationHours / 24.0

        val radiusEarth = entity.planetRadiusEarth
        val radiusJupiter = radiusEarth?.div(11.209)
        val semiMajorAu = ((entity.periodDays / 365.25) * (entity.periodDays / 365.25)).let { p2 ->
            Math.cbrt(p2 * 1.0)
        }

        val ctoiDisposition = when {
            entity.verdict.contains("CONFIRMED", ignoreCase = true) || entity.archivalStatus.contains("CONFIRMED", ignoreCase = true) -> "KP"
            entity.verdict.contains("FALSE_POSITIVE", ignoreCase = true) -> "FP"
            entity.verdict.contains("ECLIPSING_BINARY", ignoreCase = true) -> "EB"
            entity.snr >= 7.1 -> "PC"
            else -> "AMB"
        }

        val pointingAudit = validationEngine.auditTessPointing(
            ticId = entity.ticId,
            raDeg = raDeg,
            decDeg = decDeg,
            claimedSectorStr = entity.sector
        )
        val sanitizedSector = if (pointingAudit.pointingMismatchFlag) {
            pointingAudit.suggestedCorrectSector
        } else {
            entity.sector
        }
        val validatedNotes = if (pointingAudit.pointingMismatchFlag) {
            "${entity.notes ?: entity.verdictSummary} | [HEASARC VALIDATED: Sector corrected from ${entity.sector} to $sanitizedSector]"
        } else {
            entity.notes ?: entity.verdictSummary
        }

        ExoFopCtoiRecord(
            ticId = cleanTicLong,
            targetName = entity.canonicalName.ifBlank { "TIC $cleanTicLong" },
            sector = sanitizedSector,
            raDeg = raDeg,
            decDeg = decDeg,
            raSexagesimal = raSexagesimal,
            decSexagesimal = decSexagesimal,
            galacticLongitudeDeg = galactic.lDeg,
            galacticLatitudeDeg = galactic.bDeg,
            epochBjdTdb = epochBjd,
            epochBtjd = epochBtjd,
            orbitalPeriodDays = entity.periodDays,
            transitDurationHours = entity.durationHours,
            transitDurationDays = durationDays,
            depthPpm = depthPpm,
            depthPercent = depthPercent,
            depthFluxFraction = depthFraction,
            signalToNoiseRatio = entity.snr,
            planetRadiusEarth = radiusEarth,
            planetRadiusJupiter = radiusJupiter,
            planetMassEarth = entity.planetMassEarth,
            semiMajorAxisAu = semiMajorAu,
            equilibriumTempK = entity.equilibriumTempK,
            surfaceGravityG = entity.surfaceGravityG,
            bulkDensityGcm3 = entity.bulkDensityGcm3,
            isHabitableZone = entity.isHabitableZone,
            ctoiDisposition = ctoiDisposition,
            verdictTitle = entity.verdictTitle,
            archivalStatus = entity.archivalStatus,
            notes = validatedNotes,
            exportTimestampUtc = getUtcTimestampIso()
        )
    }

    private fun buildCsvFromRecords(records: List<ExoFopCtoiRecord>): String {
        val sb = StringBuilder()
        val nowIso = getUtcTimestampIso()

        sb.appendLine("# ==========================================================================================")
        sb.appendLine("# NASA EXOPLANET ARCHIVE / TESS FOLLOW-UP OBSERVING PROGRAM (ExoFOP-TESS)")
        sb.appendLine("# COMMUNITY TESS OBJECT OF INTEREST (CTOI) SUBMISSION TABLE")
        sb.appendLine("# Specification: NExScI ExoFOP-TESS CTOI Standard v2.4")
        sb.appendLine("# Reduction Engine: SYNTHESIS Space Science OS (XILON Autonomous Inversion & SPARK Pipeline)")
        sb.appendLine("# Exported At (UTC): $nowIso")
        sb.appendLine("# Total Records: ${records.size}")
        sb.appendLine("# Coordinate Reference Frame: ICRS J2000 (Equatorial) / IAU 1958 (Galactic)")
        sb.appendLine("# Timing Baseline: Barycentric Julian Date in Barycentric Dynamical Time (BJD_TDB & BTJD)")
        sb.appendLine("# ==========================================================================================")

        sb.appendLine(
            listOf(
                "TIC_ID",
                "TARGET_NAME",
                "SECTOR",
                "RA_DEG",
                "DEC_DEG",
                "RA_SEXAGESIMAL",
                "DEC_SEXAGESIMAL",
                "GAL_L_DEG",
                "GAL_B_DEG",
                "EPOCH_BJD_TDB",
                "EPOCH_BTJD",
                "PERIOD_DAYS",
                "DURATION_HOURS",
                "DURATION_DAYS",
                "DEPTH_PPM",
                "DEPTH_PERCENT",
                "SNR_SIGMA",
                "PLANET_RADIUS_REARTH",
                "PLANET_RADIUS_RJUP",
                "PLANET_MASS_MEARTH",
                "SEMI_MAJOR_AXIS_AU",
                "TEQ_K",
                "SURFACE_GRAVITY_G",
                "BULK_DENSITY_GCM3",
                "HABITABLE_ZONE",
                "CTOI_DISPOSITION",
                "VERDICT_TITLE",
                "ARCHIVAL_STATUS",
                "NOTES",
                "EXPORT_TIMESTAMP_UTC"
            ).joinToString(",")
        )

        for (r in records) {
            val row = listOf(
                r.ticId.toString(),
                escapeCsv(r.targetName),
                escapeCsv(r.sector),
                String.format(Locale.US, "%.6f", r.raDeg),
                String.format(Locale.US, "%.6f", r.decDeg),
                escapeCsv(r.raSexagesimal),
                escapeCsv(r.decSexagesimal),
                String.format(Locale.US, "%.4f", r.galacticLongitudeDeg),
                String.format(Locale.US, "%.4f", r.galacticLatitudeDeg),
                String.format(Locale.US, "%.5f", r.epochBjdTdb),
                String.format(Locale.US, "%.4f", r.epochBtjd),
                String.format(Locale.US, "%.6f", r.orbitalPeriodDays),
                String.format(Locale.US, "%.3f", r.transitDurationHours),
                String.format(Locale.US, "%.5f", r.transitDurationDays),
                String.format(Locale.US, "%.1f", r.depthPpm),
                String.format(Locale.US, "%.4f", r.depthPercent),
                String.format(Locale.US, "%.2f", r.signalToNoiseRatio),
                r.planetRadiusEarth?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                r.planetRadiusJupiter?.let { String.format(Locale.US, "%.3f", it) } ?: "",
                r.planetMassEarth?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                r.semiMajorAxisAu?.let { String.format(Locale.US, "%.4f", it) } ?: "",
                r.equilibriumTempK?.let { String.format(Locale.US, "%.1f", it) } ?: "",
                r.surfaceGravityG?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                r.bulkDensityGcm3?.let { String.format(Locale.US, "%.3f", it) } ?: "",
                if (r.isHabitableZone) "TRUE" else "FALSE",
                r.ctoiDisposition,
                escapeCsv(r.verdictTitle),
                escapeCsv(r.archivalStatus),
                escapeCsv(r.notes ?: ""),
                r.exportTimestampUtc
            )
            sb.appendLine(row.joinToString(","))
        }

        return sb.toString()
    }

    private suspend fun resolveIcrsCoordinates(entity: XilonDiscoveryEntity): Pair<Double, Double> {
        if (entity.raDeg != null && entity.decDeg != null && (abs(entity.raDeg) > 0.0001 || abs(entity.decDeg) > 0.0001)) {
            return Pair(entity.raDeg, entity.decDeg)
        }

        val cleanTic = entity.ticId.replace("TIC", "", ignoreCase = true).trim()
        val pureStar = pureTicDao.getByTicId(cleanTic)
        if (pureStar != null) {
            if (pureStar.ra != null && pureStar.dec != null) {
                return Pair(pureStar.ra, pureStar.dec)
            }
        }

        return when (cleanTic) {
            "150428135" -> Pair(97.0957, -55.5786)
            "268301217" -> Pair(117.7583, -64.6147)
            "167418703" -> Pair(108.2341, -73.1256)
            "141913165" -> Pair(84.1205, -61.2184)
            "123891044" -> Pair(103.5412, -8.8524)
            "25155310" -> Pair(142.1584, 18.2351)
            else -> Pair(0.0, 0.0)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun getUtcTimestampIso(): String {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return df.format(Date())
    }
}
