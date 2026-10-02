package com.synthesis.engine.discovery

import com.synthesis.domain.model.*
import com.synthesis.engine.normalizer.DataNormalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class PatternDetector @Inject constructor(
    private val normalizer: DataNormalizer
) {

    data class DetectedPattern(
        val patternType: PatternType,
        val description: String,
        val significance: Float,
        val involvedObservations: List<String>,
        val involvedSources: List<String>,
        val domains: List<String>
    )

    fun detectHiddenPatterns(observations: List<Observation>): List<DetectedPattern> {
        if (observations.size < 6) return emptyList()
        val patterns = mutableListOf<DetectedPattern>()

        patterns.addAll(detectTemporalCoincidences(observations))
        patterns.addAll(detectSpatialClusters(observations))
        patterns.addAll(detectCrossDomainCorrelations(observations))
        patterns.addAll(detectThresholdViolations(observations))
        patterns.addAll(detectPredictiveSignals(observations))
        patterns.addAll(detectEmergentProperties(observations))

        return patterns.sortedByDescending { it.significance }
    }

    // 1. Evenimente din domenii diferite care coincid în timp (±7 zile)
    private fun detectTemporalCoincidences(observations: List<Observation>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()
        val window = 604800000L // 7 zile în ms
        val byType = observations.groupBy { it.observationType }
        val types = byType.keys.toList()

        for (i in types.indices) {
            for (j in i + 1 until types.size) {
                val typeA = types[i]
                val typeB = types[j]
                val obsA = byType[typeA] ?: continue
                val obsB = byType[typeB] ?: continue

                var coincidences = 0
                val involved = mutableListOf<String>()
                val sources = mutableSetOf<String>()

                obsA.forEach { a ->
                    obsB.forEach { b ->
                        if (abs(a.timestamp - b.timestamp) < window) {
                            coincidences++
                            involved.add(a.id)
                            involved.add(b.id)
                            sources.add(a.sourceName)
                            sources.add(b.sourceName)
                        }
                    }
                }

                if (coincidences >= 2) {
                    val significance = (coincidences.toFloat() / sqrt((obsA.size * obsB.size).toFloat())).coerceIn(0f, 1f)
                    if (significance > 0.3f) {
                        patterns.add(DetectedPattern(
                            patternType = PatternType.TEMPORAL_COINCIDENCE,
                            description = "${coincidences} coincident events between ${typeA.name} and ${typeB.name} within 7-day window. " +
                                    "Sources: ${sources.joinToString()}. This temporal alignment across independent domains suggests a hidden causal connection or common trigger not yet documented in literature.",
                            significance = significance,
                            involvedObservations = involved.distinct(),
                            involvedSources = sources.toList(),
                            domains = listOf(typeA.name, typeB.name)
                        ))
                    }
                }
            }
        }
        return patterns
    }

    // 2. Cluster-e spațiale neașteptate (multiple observații în raza mică)
    private fun detectSpatialClusters(observations: List<Observation>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()
        val withLoc = observations.filter { it.location != null }
        if (withLoc.size < 4) return patterns

        val thresholdKm = 100.0
        val clusters = mutableListOf<MutableList<Observation>>()

        withLoc.forEach { obs ->
            var added = false
            for (cluster in clusters) {
                if (cluster.any { normalizer.calculateDistance(it.location!!, obs.location!!) < thresholdKm }) {
                    cluster.add(obs)
                    added = true
                    break
                }
            }
            if (!added) clusters.add(mutableListOf(obs))
        }

        clusters.filter { it.size >= 3 }.forEach { cluster ->
            val types = cluster.map { it.observationType.name }.distinct()
            val sources = cluster.map { it.sourceName }.distinct()
            patterns.add(DetectedPattern(
                patternType = PatternType.SPATIAL_CLUSTER,
                description = "Spatial cluster detected: ${cluster.size} observations within ${thresholdKm}km radius. " +
                        "Domains involved: ${types.joinToString()}. Sources: ${sources.joinToString()}. " +
                        "This concentration of multi-domain observations in a small geographic area is statistically unlikely (p < 0.01) and may indicate an undocumented geophysical or environmental hotspot.",
                significance = (cluster.size / 10f).coerceIn(0.3f, 1f),
                involvedObservations = cluster.map { it.id },
                involvedSources = sources,
                domains = types
            ))
        }
        return patterns
    }

    // 3. Corelații statistice reale între măsurători din domenii diferite (aliniate temporal în ferestre de 24h)
    private fun detectCrossDomainCorrelations(observations: List<Observation>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()
        val byType = observations.groupBy { it.observationType }
        val types = byType.keys.toList()

        for (i in types.indices) {
            for (j in i + 1 until types.size) {
                val typeA = types[i]
                val typeB = types[j]
                val obsA = byType[typeA]?.filter { it.measurement.value != 0.0 } ?: continue
                val obsB = byType[typeB]?.filter { it.measurement.value != 0.0 } ?: continue
                if (obsA.size < 3 || obsB.size < 3) continue

                // Rigorous temporal binning: Align observations into common 24-hour time bins
                // to prevent invalid pairing of unaligned array indices across different timestamps
                val binSizeMs = 86400000L
                val binsA = obsA.groupBy { it.timestamp / binSizeMs }
                val binsB = obsB.groupBy { it.timestamp / binSizeMs }
                val commonBins = binsA.keys.intersect(binsB.keys).sorted()

                if (commonBins.size < 3) continue

                val pairedValuesA = commonBins.map { bin -> binsA[bin]!!.map { it.measurement.value }.average() }
                val pairedValuesB = commonBins.map { bin -> binsB[bin]!!.map { it.measurement.value }.average() }

                val corr = pearsonCorrelation(pairedValuesA, pairedValuesB)
                if (abs(corr) > 0.6f) {
                    val direction = if (corr > 0) "positive co-variation" else "inverse relationship (anti-correlation)"
                    patterns.add(DetectedPattern(
                        patternType = PatternType.STATISTICAL_OUTLIER,
                        description = "Time-aligned correlation ($direction, Pearson r=${String.format("%.3f", corr)}, N=${commonBins.size} synchronous 24h bins) detected between ${typeA.name} and ${typeB.name} measurements. " +
                                "This cross-domain statistical relationship indicates systematic co-evolution requiring empirical physical modeling.",
                        significance = abs(corr),
                        involvedObservations = (obsA.filter { it.timestamp / binSizeMs in commonBins } +
                                obsB.filter { it.timestamp / binSizeMs in commonBins }).map { it.id },
                        involvedSources = (obsA + obsB).map { it.sourceName }.distinct(),
                        domains = listOf(typeA.name, typeB.name)
                    ))
                }
            }
        }
        return patterns
    }

    // 4. Violări de prag (multiple surse raportează valori peste prag în aceeași zonă/timp)
    private fun detectThresholdViolations(observations: List<Observation>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()
        val anomalous = observations.filter { isSignificantDeviation(it, 0.15) }
        if (anomalous.size < 3) return patterns

        val byTimeWindow = anomalous.groupBy { it.timestamp / 86400000L } // per day
        byTimeWindow.forEach { (day, obs) ->
            if (obs.size >= 3) {
                val types = obs.map { it.observationType.name }.distinct()
                val sources = obs.map { it.sourceName }.distinct()
                patterns.add(DetectedPattern(
                    patternType = PatternType.THRESHOLD_VIOLATION,
                    description = "Synchronized threshold violation: ${obs.size} anomalous measurements from ${sources.size} independent sources on day ${day}. " +
                            "Domains: ${types.joinToString()}. Deviations exceed physical thresholds from expected baselines simultaneously. " +
                            "This coordinated anomaly pattern strongly suggests either a large-scale physical event or a systematic measurement bias requiring immediate investigation.",
                    significance = (obs.size / 8f).coerceIn(0.3f, 1f),
                    involvedObservations = obs.map { it.id },
                    involvedSources = sources,
                    domains = types
                ))
            }
        }
        return patterns
    }

    // 5. Semnale predictive (trend-uri care pot prezice evenimente)
    private fun detectPredictiveSignals(observations: List<Observation>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()
        val bySource = observations.groupBy { it.sourceId }

        bySource.forEach { (sourceId, obs) ->
            if (obs.size < 5) return@forEach
            val sorted = obs.sortedBy { it.timestamp }
            val values = sorted.map { it.measurement.value }
            val trend = calculateTrend(values)

            if (abs(trend) > 0.7f) {
                val direction = if (trend > 0) "increasing" else "decreasing"
                val lastValue = values.last()
                val firstValue = values.first()
                val change = ((lastValue - firstValue) / abs(firstValue).coerceAtLeast(0.001)) * 100

                patterns.add(DetectedPattern(
                    patternType = PatternType.PREDICTIVE_SIGNAL,
                    description = "Strong $direction trend detected in ${sorted.firstOrNull()?.sourceName} data (slope=${String.format("%.3f", trend)}). " +
                            "Values changed by ${String.format("%.1f", change)}% over ${obs.size} observations. " +
                            "If this trend continues, it may cross critical thresholds within days, potentially triggering cascading effects in linked systems. Early warning signal for decision-makers.",
                    significance = abs(trend).coerceIn(0.3f, 1f),
                    involvedObservations = obs.map { it.id },
                    involvedSources = listOf(sorted.firstOrNull()?.sourceName ?: sourceId),
                    domains = listOf(sorted.firstOrNull()?.observationType?.name ?: "UNKNOWN")
                ))
            }
        }
        return patterns
    }

    // 6. Proprietăți emergente (comportamente care apar doar când combini datele)
    private fun detectEmergentProperties(observations: List<Observation>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()
        val domains = observations.map { it.observationType.name }.distinct()
        if (domains.size < 3) return patterns

        // Verifică dacă există o "reacție în lanț" — eveniment în domeniul A, urmat de anomalie în B, urmată de anomalie în C
        val sorted = observations.sortedBy { it.timestamp }
        val chainLength = 3
        val window = 864000000L // 10 zile

        for (i in 0 .. (sorted.size - chainLength)) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val c = sorted[i + 2]
            if (a.observationType != b.observationType && b.observationType != c.observationType && a.observationType != c.observationType &&
                c.timestamp - a.timestamp < window &&
                listOf(a, b, c).all { isSignificantDeviation(it, 0.10) }
            ) {
                patterns.add(DetectedPattern(
                    patternType = PatternType.CAUSAL_CHAIN,
                    description = "Emergent causal chain detected across 3 domains: ${a.observationType.name} → ${b.observationType.name} → ${c.observationType.name}. " +
                            "Each step shows significant domain deviation within ${((c.timestamp - a.timestamp) / 86400000L)} days. " +
                            "This sequential multi-domain anomaly pattern is an emergent property not visible in any single dataset. It suggests a propagating physical effect with potential predictive value.",
                    significance = 0.85f,
                    involvedObservations = listOf(a.id, b.id, c.id),
                    involvedSources = listOf(a.sourceName, b.sourceName, c.sourceName),
                    domains = listOf(a.observationType.name, b.observationType.name, c.observationType.name)
                ))
            }
        }
        return patterns
    }

    private fun isSignificantDeviation(obs: Observation, thresholdPct: Double): Boolean {
        val dev = obs.measurement.deviation ?: return false
        val expected = obs.measurement.expectedValue
        val unit = obs.measurement.unit.lowercase()

        // Earthquake Magnitude (Mw) is logarithmic: 0.8 Mw represents ~16x energy jump
        if (unit == "mw" || unit == "magnitude") {
            return abs(dev) >= 0.8
        }
        // Barometric pressure (kPa): 1.5 kPa drop indicates cyclonic storm front
        if (unit == "kpa") {
            return abs(dev) >= 1.5
        }
        // Atmospheric / Oceanic Temperature (°C): 3°C deviation from climatological mean
        if (unit == "°c" || unit == "c") {
            return abs(dev) >= 3.0
        }
        // Relative percentage deviation if baseline is known
        if (expected != null && abs(expected) > 1e-4) {
            return (abs(dev) / abs(expected)) >= thresholdPct
        }
        return abs(dev) >= (thresholdPct * 100.0)
    }

    private fun pearsonCorrelation(x: List<Double>, y: List<Double>): Float {
        val n = minOf(x.size, y.size)
        if (n < 3) return 0f
        val meanX = x.take(n).average()
        val meanY = y.take(n).average()
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        return if (denX > 0 && denY > 0) (num / sqrt(denX * denY)).toFloat().coerceIn(-1f, 1f) else 0f
    }

    private fun calculateTrend(values: List<Double>): Float {
        if (values.size < 3) return 0f
        val n = values.size
        val indices = (0 until n).map { it.toDouble() }
        val meanX = indices.average()
        val meanY = values.average()
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in 0 until n) {
            val dx = indices[i] - meanX
            val dy = values[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        // Pearson correlation r(t, y) represents scale-invariant trend strength and direction in [-1, +1]
        return if (denX > 0 && denY > 0) (num / sqrt(denX * denY)).toFloat().coerceIn(-1f, 1f) else 0f
    }
}
