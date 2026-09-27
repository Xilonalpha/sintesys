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

                // Statistical correlation (for numeric measurements, time-aligned)
                val statStrength = calculateStatisticalCorrelation(obsA, obsB)
                if (statStrength > 0.3f) {
                    correlations.add(Correlation(
                        id = "corr_stat_${sourceA}_${sourceB}_${System.currentTimeMillis()}",
                        investigationId = investigationId,
                        sourceA = nameA, sourceB = nameB,
                        correlationType = CorrelationType.STATISTICAL,
                        strength = statStrength,
                        description = "Statistical relationship detected between $nameA and $nameB ($domainA vs $domainB, Pearson r=${String.format("%.2f", statStrength)}). Co-variation detected — causality requires discriminant testing.",
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
        // Pair observations by temporal proximity within a 3-day window
        val timeWindowMs = 86400000L * 3
        val paired = mutableListOf<Pair<Double, Double>>()
        obsA.filter { it.measurement.value != 0.0 }.forEach { a ->
            val closestB = obsB.filter { it.measurement.value != 0.0 }.minByOrNull { abs(it.timestamp - a.timestamp) }
            if (closestB != null && abs(closestB.timestamp - a.timestamp) <= timeWindowMs) {
                paired.add(a.measurement.value to closestB.measurement.value)
            }
        }
        if (paired.size < 3) return 0f

        val valuesA = paired.map { it.first }
        val valuesB = paired.map { it.second }
        val meanA = valuesA.average()
        val meanB = valuesB.average()
        val numerator = paired.sumOf { (a, b) -> (a - meanA) * (b - meanB) }
        val denomA = kotlin.math.sqrt(valuesA.sumOf { (it - meanA).pow(2) })
        val denomB = kotlin.math.sqrt(valuesB.sumOf { (it - meanB).pow(2) })
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
