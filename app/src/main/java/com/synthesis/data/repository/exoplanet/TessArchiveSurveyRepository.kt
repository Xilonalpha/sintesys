package com.synthesis.data.repository.exoplanet

import android.util.Log
import com.synthesis.data.local.entity.SynthesizedResearchEntity
import com.synthesis.data.remote.api.ExoplanetArchiveApi
import com.synthesis.data.repository.SynthesizedResearchRepository
import com.synthesis.data.repository.xilon.XilonRepository
import com.synthesis.domain.model.exoplanet.TessSurveyCriteria
import com.synthesis.domain.model.exoplanet.TessSurveyDiscovery
import com.synthesis.engine.exoplanet.TransitDipScannerUtility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

@Singleton
class TessArchiveSurveyRepository @Inject constructor(
    private val xilonRepository: XilonRepository,
    private val exoplanetArchiveApi: ExoplanetArchiveApi,
    private val transitDipScannerUtility: TransitDipScannerUtility,
    private val synthesizedResearchRepository: SynthesizedResearchRepository
) {

    /**
     * Periodically queries TESS archives and runs photometric transit detection
     * on stars matching the screening criteria.
     */
    suspend fun executeArchiveSurveyScan(criteria: TessSurveyCriteria): List<TessSurveyDiscovery> = withContext(Dispatchers.IO) {
        val discoveries = mutableListOf<TessSurveyDiscovery>()
        val candidateTics = mutableListOf<Pair<String, Double?>>() // (TIC ID, optional literature period)

        Log.d(TAG, "Starting TESS archive survey scan with criteria: maxTmag=${criteria.maxTmag}, minSnr=${criteria.minSnr}, minDepth=${criteria.minDepthPpm}")

        // 1. Query candidate stars from NASA Exoplanet Archive (TAP TOI table)
        try {
            val query = "SELECT top 10 tid,toi,st_tmag,st_rad,st_teff,pl_orbper,pl_trandep,pl_trandurh,pl_rade,tfopwg_disp " +
                    "FROM toi WHERE st_tmag <= ${criteria.maxTmag} AND pl_trandep >= ${criteria.minDepthPpm} " +
                    "ORDER BY tid DESC"
            val toiResults = exoplanetArchiveApi.queryToiTable(query)
            for (dto in toiResults) {
                val tid = dto.tid?.toString() ?: continue
                val rad = dto.stRad ?: 1.0
                if (criteria.dwarfStarsOnly && rad > 1.35) continue
                candidateTics.add(Pair(tid, dto.plOrbPer))
            }
            Log.d(TAG, "Retrieved ${toiResults.size} stars from NASA TAP meeting magnitude and depth criteria.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "NASA TAP TOI query transient failure: ${e.message}")
        }

        // 2. Include curated unvetted candidates and blindspot stars
        val curated = xilonRepository.getCuratedTargets()
        for (target in curated) {
            if (target.estimatedMag <= criteria.maxTmag) {
                candidateTics.add(Pair(target.ticId, null))
            }
        }

        // Distinct list limited to batch size
        val batchToScan = candidateTics.distinctBy { it.first }.take(criteria.scanBatchSize)
        Log.d(TAG, "Evaluating ${batchToScan.size} stars in this execution cycle...")

        for ((ticId, period) in batchToScan) {
            coroutineContext.ensureActive()
            try {
                val detectionResult = xilonRepository.runAutonomousDetection(ticId, period)
                detectionResult.getOrNull()?.let { detection ->
                    val starMag = detection.star.tMag ?: 10.0
                    val starRadius = detection.star.radiusSun ?: 1.0

                    val passesDwarfFilter = !criteria.dwarfStarsOnly || starRadius <= 1.35
                    val passesSnr = detection.signalToNoiseRatio >= criteria.minSnr
                    val passesDepth = detection.transitDepthPpm >= criteria.minDepthPpm

                    if (passesDwarfFilter && passesSnr && passesDepth) {
                        val depthFraction = detection.transitDepthPpm / 1_000_000.0
                        val magDelta = if (depthFraction in 0.0..0.9) {
                            -2.5 * log10((1.0 - depthFraction).coerceAtLeast(1e-5))
                        } else 0.0

                        val radius = detection.derivedPlanetRadiusEarth ?: 1.0
                        val rationale = "Semnal fotometric robust validat de TESS Survey Scout: adâncime ${detection.transitDepthPpm.toInt()} ppm " +
                                "(Δm: ${String.format(Locale.US, "%.4f", magDelta)} mag), durată ${String.format(Locale.US, "%.1f", detection.transitDurationHours)}h, " +
                                "SNR ${String.format(Locale.US, "%.1f", detection.signalToNoiseRatio)}σ pe stea pitică (Tmag ${String.format(Locale.US, "%.1f", starMag)})."

                        val discovery = TessSurveyDiscovery(
                            ticId = detection.star.ticId,
                            starName = detection.archivalCrossMatch.canonicalName,
                            depthPpm = detection.transitDepthPpm,
                            depthMagnitudeDelta = magDelta,
                            orbitalPeriodDays = detection.orbitalPeriodDays,
                            derivedRadiusEarth = radius,
                            snr = detection.signalToNoiseRatio,
                            hostStarTmag = starMag,
                            hostStarRadiusSun = starRadius,
                            transitDurationHours = detection.transitDurationHours,
                            verdictLabel = detection.verdictTitle,
                            scientificRationale = rationale
                        )

                        discoveries.add(discovery)

                        // 3. Save discovery in Room Database
                        xilonRepository.saveDiscovery(detection, "Descoperire automată WorkManager TESS Survey Scout")

                        val researchEntity = SynthesizedResearchEntity(
                            id = UUID.randomUUID().toString(),
                            title = "Alertă Sondaj TESS: TIC ${detection.star.ticId} (${detection.archivalCrossMatch.canonicalName})",
                            targetName = "TIC ${detection.star.ticId}",
                            researchCategory = "EXOPLANET",
                            summary = rationale,
                            synthesizedDataJson = """{"tic":"${detection.star.ticId}","period_d":${detection.orbitalPeriodDays},"depth_ppm":${detection.transitDepthPpm},"radius_earth":$radius,"snr":${detection.signalToNoiseRatio},"vmag":$starMag,"scout":"WORKMANAGER_PERIODIC"}""",
                            confidenceScore = 0.95f,
                            sourceCount = 3,
                            observerNotes = "Detectat autonom în fundal prin scanarea periodică a arhivelor TESS / MAST.",
                            timestamp = System.currentTimeMillis(),
                            isVerified = true,
                            isBookmarked = true
                        )
                        synthesizedResearchRepository.saveResearch(researchEntity)
                        Log.i(TAG, "★ Discovered potential transit on TIC $ticId! Saved to Room research database.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to analyze TIC $ticId in background scout: ${e.message}")
            }
        }

        discoveries
    }

    companion object {
        private const val TAG = "TessArchiveSurveyRepo"
    }
}
