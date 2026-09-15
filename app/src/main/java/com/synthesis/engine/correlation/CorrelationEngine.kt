package com.synthesis.engine.correlation

import com.synthesis.domain.model.*
import com.synthesis.engine.normalizer.DataNormalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CorrelationEngine @Inject constructor(
    private val normalizer: DataNormalizer
) {

    data class CorrelationResult(
        val sourceA: String,
        val sourceB: String,
        val correlationType: CorrelationType,
        val strength: Float,
        val description: String,
        val domains: List<String>
    )

    fun findCorrelations(observations: List<Observation>): List<Correlation> {
        if (observations.size < 4) return emptyList()

        val correlations = mutableListOf<Correlation>()
        val investigationId = observations.firstOrNull()?.investigationId ?: ""

        // Group by source
        val groupedBySource = observations.groupBy { it.sourceId }
        val sourceKeys = groupedBySource.keys.toList()

        // Cross-source correlation analysis
        for (i in sourceKeys.indices) {
            for (j in i + 1 until sourceKeys.size) {
                val sourceA = sourceKeys[i]
                val sourceB = sourceKeys[j]
                val obsA = groupedBySource[sourceA] ?: continue
                val obsB = groupedBySource[sourceB] ?: continue

                val nameA = obsA.firstOrNull()?.sourceName ?: sourceA
                val nameB = obsB.firstOrNull()?.sourceName ?: sourceB
                val domainA = obsA.firstOrNull()?.observationType?.name ?: ""
                val domainB = obsB.firstOrNull()?.observationType?.name ?: ""

                // Skip if same domain and no spatial/temporal overlap possible
                if (domainA == domainB && domainA == ObservationType.HISTORICAL.name) continue

                // Temporal correlation
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

                // Statistical correlation (for numeric measurements)
                val statStrength = calculateStatisticalCorrelation(obsA, obsB)
                if (statStrength > 0.3f) {
                    correlations.add(Correlation(
                        id = "corr_stat_${sourceA}_${sourceB}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = nameA, sourceB = nameB,
                        correlationType = CorrelationType.STATISTICAL,
                        strength = statStrength,
                        description = "Statistical relationship detected between $nameA and $nameB. Correlation detected — causality not established. Cross-domain measurement alignment suggests potential common underlying factor.",
                        causalityEstablished = false,
                        domains = listOf(domainA, domainB).filter { it.isNotBlank() }
                    ))
                }
            }
        }

        // Multi-source anomaly correlation: find if anomalies in one domain coincide with events in another
        val anomalyObs = observations.filter { it.measurement.deviation != null && abs(it.measurement.deviation!!) > 10 }
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

    private fun calculateStatisticalCorrelation(obsA: List<Observation>, obsB: List<Observation>): Float {
        val valuesA = obsA.map { it.measurement.value }.filter { it != 0.0 }
        val valuesB = obsB.map { it.measurement.value }.filter { it != 0.0 }
        if (valuesA.size < 2 || valuesB.size < 2) return 0f
        val meanA = valuesA.average()
        val meanB = valuesB.average()
        val numerator = valuesA.zip(valuesB).sumOf { (a, b) -> (a - meanA) * (b - meanB) }.toDouble()
        val denomA = kotlin.math.sqrt(valuesA.sumOf { (it - meanA) * (it - meanA) }.toDouble())
        val denomB = kotlin.math.sqrt(valuesB.sumOf { (it - meanB) * (it - meanB) }.toDouble())
        return if (denomA > 0 && denomB > 0) abs(numerator / (denomA * denomB)).toFloat().coerceIn(0f, 1f) else 0f
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
                val typeA = domainKeys[i]
                val typeB = domainKeys[j]
                val obsA = groupedByDomain[typeA] ?: continue
                val obsB = groupedByDomain[typeB] ?: continue

                // Check for temporal coincidence of anomalies across domains
                var coincident = 0
                val window = 86400000L * 7 // 7 days
                obsA.forEach { a ->
                    obsB.forEach { b ->
                        if (abs(a.timestamp - b.timestamp) < window) coincident++
                    }
                }
                if (coincident > 0) {
                    val strength = (coincident.toFloat() / (obsA.size * obsB.size).toFloat()).coerceIn(0.1f, 0.9f)
                    val nameA = obsA.firstOrNull()?.sourceName ?: typeA.name
                    val nameB = obsB.firstOrNull()?.sourceName ?: typeB.name
                    correlations.add(Correlation(
                        id = "corr_anomaly_${typeA}_${typeB}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = nameA, sourceB = nameB,
                        correlationType = CorrelationType.CAUSAL,
                        strength = strength,
                        description = "Cross-domain anomaly coincidence detected: ${obsA.size} anomalous events in $nameA temporally overlap with ${obsB.size} anomalous events in $nameB within a 7-day window. Suggest investigation into shared drivers.",
                        causalityEstablished = false,
                        domains = listOf(typeA.name, typeB.name)
                    ))
                }
            }
        }
        return correlations
    }

    private fun buildTemporalDescription(nameA: String, nameB: String, strength: Float, obsA: List<Observation>, obsB: List<Observation>): String {
        val timeA = obsA.firstOrNull()?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it.timestamp)) } ?: "unknown"
        val timeB = obsB.firstOrNull()?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it.timestamp)) } ?: "unknown"
        return "Temporal pattern detected between $nameA and $nameB. Events from both sources cluster within a 3-day window (strength: ${(strength * 100).toInt()}%). " +
                "Sample timestamps: $nameA at $timeA, $nameB at $timeB. " +
                "Correlation detected — causality not established."
    }

    private fun buildSpatialDescription(nameA: String, nameB: String, strength: Float, obsA: List<Observation>, obsB: List<Observation>): String {
        val locA = obsA.firstOrNull { it.location != null }?.location
        val locB = obsB.firstOrNull { it.location != null }?.location
        val dist = if (locA != null && locB != null) normalizer.calculateDistance(locA, locB) else null
        return "Spatial proximity detected between observations from $nameA and $nameB. " +
                (dist?.let { "Distance between representative points: ${String.format("%.1f", it)} km. " } ?: "") +
                "${(strength * 100).toInt()}% of observations fall within 500 km radius. " +
                "Correlation detected — causality not established."
    }
}
