package com.synthesis.engine.correlation

import com.synthesis.domain.model.*
import com.synthesis.engine.normalizer.DataNormalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow

@Singleton
class CorrelationEngine @Inject constructor(
    private val normalizer: DataNormalizer
) {

    fun findCorrelations(observations: List<Observation>): List<Correlation> {
        return correlate(observations, "")
    }

    fun correlate(observations: List<Observation>, investigationId: String): List<Correlation> {
        val correlations = mutableListOf<Correlation>()
        val groupedBySource = observations.groupBy { it.sourceId }
        val sources = groupedBySource.keys.toList()

        for (i in sources.indices) {
            for (j in i + 1 until sources.size) {
                val sourceA = sources[i]
                val sourceB = sources[j]
                val obsA = groupedBySource[sourceA] ?: emptyList()
                val obsB = groupedBySource[sourceB] ?: emptyList()

                val nameA = obsA.firstOrNull()?.sourceName ?: sourceA
                val nameB = obsB.firstOrNull()?.sourceName ?: sourceB
                val domainA = obsA.firstOrNull()?.observationType?.name ?: ""
                val domainB = obsB.firstOrNull()?.observationType?.name ?: ""

                // Temporal correlation: do events cluster in time?
                val temporalStrength = calculateTemporalCorrelation(obsA, obsB)
                if (temporalStrength > 0.2f) {
                    correlations.add(Correlation(
                        id = "corr_temp_${sourceA}_${sourceB}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = nameA, sourceB = nameB,
                        correlationType = CorrelationType.TEMPORAL,
                        strength = temporalStrength,
                        description = buildTemporalDescription(nameA, nameB, temporalStrength, obsA, obsB),
                        causalityEstablished = false,
                        domains = listOf(domainA, domainB).filter { it.isNotBlank() }
                    ))
                }

                // Spatial correlation (only if both have locations)
                val spatialStrength = calculateSpatialCorrelation(obsA, obsB)
                if (spatialStrength > 0.2f) {
                    correlations.add(Correlation(
                        id = "corr_spatial_${sourceA}_${sourceB}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = nameA, sourceB = nameB,
                        correlationType = CorrelationType.SPATIAL,
                        strength = spatialStrength,
                        description = buildSpatialDescription(nameA, nameB, spatialStrength, obsA, obsB),
                        causalityEstablished = false,
                        domains = listOf(domainA, domainB).filter { it.isNotBlank() }
                    ))
                }

                // Statistical correlation (for numeric measurements, time-aligned with signed Pearson r and Student's t test)
                val stat = calculateStatisticalCorrelation(obsA, obsB)
                if (abs(stat.r) > 0.3f) {
                    val relationType = if (stat.r > 0) "Direct co-variation" else "Inverse relationship (anti-correlation)"
                    correlations.add(Correlation(
                        id = "corr_stat_${sourceA}_${sourceB}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = nameA, sourceB = nameB,
                        correlationType = CorrelationType.STATISTICAL,
                        strength = abs(stat.r),
                        description = "Statistical relationship detected between $nameA and $nameB ($domainA vs $domainB, Pearson r=${String.format("%.2f", stat.r)}, t=${String.format("%.2f", stat.t)}, df=${stat.df}, ${stat.pString}). $relationType confirmed across time-aligned data.",
                        causalityEstablished = false,
                        domains = listOf(domainA, domainB).filter { it.isNotBlank() }
                    ))
                }
            }
        }

        // Multi-source anomaly correlation: find if anomalies in one domain coincide with events in another
        val anomalyObs = observations.filter { isSignificantDeviation(it, 0.10) }
        if (anomalyObs.size >= 2) {
            val crossDomainAnomalies = findCrossDomainAnomalyCorrelations(anomalyObs, investigationId)
            correlations.addAll(crossDomainAnomalies)
        }

        return correlations.sortedByDescending { it.strength }
    }

    private fun calculateTemporalCorrelation(obsA: List<Observation>, obsB: List<Observation>): Float {
        if (obsA.isEmpty() || obsB.isEmpty()) return 0f
        val timeWindow = 86400000L * 3 // 3 days in ms
        var matches = 0
        obsA.forEach { a ->
            obsB.forEach { b ->
                if (abs(a.timestamp - b.timestamp) < timeWindow) matches++
            }
        }
        val maxPossible = obsA.size * obsB.size
        return if (maxPossible > 0) (matches.toFloat() / maxPossible.toFloat()).coerceIn(0f, 1f) else 0f
    }

    private fun calculateSpatialCorrelation(obsA: List<Observation>, obsB: List<Observation>): Float {
        if (obsA.isEmpty() || obsB.isEmpty()) return 0f
        var matches = 0
        val distanceThreshold = 500.0 // km
        val obsAWithLoc = obsA.filter { it.location != null }
        val obsBWithLoc = obsB.filter { it.location != null }
        if (obsAWithLoc.isEmpty() || obsBWithLoc.isEmpty()) return 0f

        obsAWithLoc.forEach { a ->
            a.location?.let { locA ->
                obsBWithLoc.forEach { b ->
                    b.location?.let { locB ->
                        if (normalizer.calculateDistance(locA, locB) < distanceThreshold) matches++
                    }
                }
            }
        }
        val maxPossible = obsAWithLoc.size * obsBWithLoc.size
        return if (maxPossible > 0) (matches.toFloat() / maxPossible.toFloat()).coerceIn(0f, 1f) else 0f
    }

    data class StatisticalResult(
        val r: Float,
        val n: Int,
        val df: Int,
        val t: Double,
        val pString: String
    )

    private fun calculateStatisticalCorrelation(obsA: List<Observation>, obsB: List<Observation>): StatisticalResult {
        // Bin into 24-hour intervals to ensure true temporal synchronicity between independent time series
        val binSizeMs = 86400000L
        val binsA = obsA.filter { it.measurement.value != 0.0 }.groupBy { it.timestamp / binSizeMs }
        val binsB = obsB.filter { it.measurement.value != 0.0 }.groupBy { it.timestamp / binSizeMs }
        val commonBins = binsA.keys.intersect(binsB.keys).sorted()

        if (commonBins.size >= 3) {
            val valuesA = commonBins.map { bin -> binsA[bin]!!.map { it.measurement.value }.average() }
            val valuesB = commonBins.map { bin -> binsB[bin]!!.map { it.measurement.value }.average() }
            val meanA = valuesA.average()
            val meanB = valuesB.average()
            val numerator = valuesA.indices.sumOf { (valuesA[it] - meanA) * (valuesB[it] - meanB) }
            val denomA = kotlin.math.sqrt(valuesA.sumOf { (it - meanA).pow(2) })
            val denomB = kotlin.math.sqrt(valuesB.sumOf { (it - meanB).pow(2) })
            val r = if (denomA > 0 && denomB > 0) (numerator / (denomA * denomB)).toFloat().coerceIn(-1f, 1f) else 0f
            return buildStatisticalResult(r, commonBins.size)
        }

        // Fallback for sparse discrete events: 1-to-1 nearest neighbor matching without replacement
        val timeWindowMs = 86400000L * 3
        val usedB = mutableSetOf<String>()
        val paired = mutableListOf<Pair<Double, Double>>()
        obsA.filter { it.measurement.value != 0.0 }.sortedBy { it.timestamp }.forEach { a ->
            val matchB = obsB.filter { it.measurement.value != 0.0 && it.id !in usedB && abs(it.timestamp - a.timestamp) <= timeWindowMs }
                .minByOrNull { abs(it.timestamp - a.timestamp) }
            if (matchB != null) {
                usedB.add(matchB.id)
                paired.add(a.measurement.value to matchB.measurement.value)
            }
        }
        if (paired.size < 3) return StatisticalResult(0f, 0, 0, 0.0, "insufficient data")

        val valuesA = paired.map { it.first }
        val valuesB = paired.map { it.second }
        val meanA = valuesA.average()
        val meanB = valuesB.average()
        val numerator = paired.sumOf { (a, b) -> (a - meanA) * (b - meanB) }
        val denomA = kotlin.math.sqrt(valuesA.sumOf { (it - meanA).pow(2) })
        val denomB = kotlin.math.sqrt(valuesB.sumOf { (it - meanB).pow(2) })
        val r = if (denomA > 0 && denomB > 0) (numerator / (denomA * denomB)).toFloat().coerceIn(-1f, 1f) else 0f
        return buildStatisticalResult(r, paired.size)
    }

    private fun buildStatisticalResult(r: Float, n: Int): StatisticalResult {
        val df = (n - 2).coerceAtLeast(1)
        val rDouble = r.toDouble().coerceIn(-0.9999, 0.9999)
        val t = rDouble * kotlin.math.sqrt(df.toDouble() / (1.0 - rDouble * rDouble))
        val absT = abs(t)
        val pLabel = when {
            absT >= 4.5 -> "p < 0.001 (Extremely Significant)"
            absT >= 3.0 -> "p < 0.01 (Highly Significant)"
            absT >= 2.1 -> "p < 0.05 (Statistically Significant)"
            else -> "p >= 0.05 (Advisory / Sub-threshold)"
        }
        return StatisticalResult(r, n, df, t, pLabel)
    }

    private fun isSignificantDeviation(obs: Observation, thresholdPct: Double): Boolean {
        val dev = obs.measurement.deviation ?: return false
        val expected = obs.measurement.expectedValue
        val unit = obs.measurement.unit.lowercase()

        if (unit == "mw" || unit == "magnitude") return abs(dev) >= 0.8
        if (unit == "kpa") return abs(dev) >= 1.5
        if (unit == "°c" || unit == "c") return abs(dev) >= 3.0
        if (expected != null && abs(expected) > 1e-4) return (abs(dev) / abs(expected)) >= thresholdPct
        return abs(dev) >= (thresholdPct * 100.0)
    }

    private fun findCrossDomainAnomalyCorrelations(
        anomalyObs: List<Observation>,
        investigationId: String
    ): List<Correlation> {
        val correlations = mutableListOf<Correlation>()
        val groupedByDomain = anomalyObs.groupBy { it.observationType }
        val domainKeys = groupedByDomain.keys.toList()

        for (i in domainKeys.indices) {
            for (j in i + 1 until domainKeys.size) {
                val domainA = domainKeys[i]
                val domainB = domainKeys[j]
                val listA = groupedByDomain[domainA] ?: emptyList()
                val listB = groupedByDomain[domainB] ?: emptyList()

                // Check for temporal clustering of anomalies across domains
                val windowMs = 86400000L * 2 // 48-hour window
                var crossDomainEvents = 0
                listA.forEach { a ->
                    listB.forEach { b ->
                        if (abs(a.timestamp - b.timestamp) < windowMs) crossDomainEvents++
                    }
                }

                if (crossDomainEvents > 0) {
                    val strength = (crossDomainEvents.toFloat() / (listA.size + listB.size)).coerceIn(0.1f, 1.0f)
                    correlations.add(Correlation(
                        id = "corr_anomaly_${domainA.name}_${domainB.name}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = listA.firstOrNull()?.sourceName ?: domainA.name,
                        sourceB = listB.firstOrNull()?.sourceName ?: domainB.name,
                        correlationType = CorrelationType.STATISTICAL,
                        strength = strength,
                        description = "Simultaneous anomalous events detected across ${domainA.name} and ${domainB.name}. Co-occurring within 48h. Possible shared underlying driver.",
                        causalityEstablished = false,
                        domains = listOf(domainA.name, domainB.name)
                    ))
                }
            }
        }
        return correlations
    }

    private fun buildTemporalDescription(nameA: String, nameB: String, strength: Float, obsA: List<Observation>, obsB: List<Observation>): String {
        val pct = (strength * 100).toInt()
        return "$pct% of observations between $nameA and $nameB occurred within 72 hours of each other. Temporal clustering detected. Causality not established."
    }

    private fun buildSpatialDescription(nameA: String, nameB: String, strength: Float, obsA: List<Observation>, obsB: List<Observation>): String {
        val pct = (strength * 100).toInt()
        return "$pct% of geolocated events between $nameA and $nameB occurred within 500 km. Spatial proximity detected."
    }
}
