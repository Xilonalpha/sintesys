package com.synthesis.engine.anomaly

import com.synthesis.domain.model.*
import com.synthesis.engine.normalizer.DataNormalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow

@Singleton
class AnomalyDetector @Inject constructor(
    private val normalizer: DataNormalizer
) {

    data class AnomalyResult(
        val observation: Observation,
        val expectedValue: Double,
        val deviation: Double,
        val deviationPercent: Double,
        val anomalyScore: Float,
        val possibleCauses: List<PossibleCause>,
        val severity: AnomalySeverity
    )

    fun detectAnomalies(observations: List<Observation>): List<Anomaly> {
        if (observations.size < 3) return emptyList()

        val normalized = normalizer.normalizeBatch(observations)
        // Group by observationType AND physical unit so we don't mix temperatures with wind speeds or pressures
        val groupedByMetric = normalized.groupBy { Pair(it.observationType, it.unit) }

        val anomalies = mutableListOf<Anomaly>()

        groupedByMetric.forEach { (typeUnitPair, records) ->
            val (type, unit) = typeUnitPair
            if (records.size < 3) return@forEach

            val values = records.map { it.measurement }
            val mean = values.average()
            val stdDev = calculateStdDev(values, mean)

            records.forEachIndexed { index, record ->
                val zScore = if (stdDev > 0) abs(record.measurement - mean) / stdDev else 0.0
                val score = (zScore / 3.0).coerceIn(0.0, 1.0).toFloat()

                if (score > 0.5f) {
                    val deviation = record.measurement - mean
                    val deviationPercent = if (mean != 0.0) (deviation / mean) * 100 else 0.0

                    anomalies.add(
                        Anomaly(
                            id = "anomaly_${type}_${unit}_${index}_${System.currentTimeMillis()}",
                            investigationId = observations.firstOrNull()?.investigationId ?: "",
                            observationId = record.id,
                            expectedValue = mean,
                            observedValue = record.measurement,
                            deviation = deviation,
                            deviationPercent = deviationPercent,
                            anomalyScore = score,
                            possibleCauses = generatePossibleCauses(score, type),
                            severity = when {
                                score > 0.9 -> AnomalySeverity.CRITICAL
                                score > 0.75 -> AnomalySeverity.HIGH
                                score > 0.6 -> AnomalySeverity.MEDIUM
                                else -> AnomalySeverity.LOW
                            }
                        )
                    )
                }
            }
        }

        return anomalies.sortedByDescending { it.anomalyScore }
    }

    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        val variance = values.map { (it - mean).pow(2) }.average()
        return kotlin.math.sqrt(variance)
    }

    private fun generatePossibleCauses(score: Float, type: ObservationType): List<PossibleCause> {
        return when (type) {
            ObservationType.ASTRONOMICAL -> listOf(
                PossibleCause("Calibration error", 0.15f + (1 - score) * 0.1f),
                PossibleCause("Atmospheric interference", 0.20f + (1 - score) * 0.1f),
                PossibleCause("Previously uncatalogued object", 0.25f + score * 0.15f),
                PossibleCause("Orbital perturbation", 0.20f + score * 0.1f),
                PossibleCause("Unknown", 0.20f)
            )
            ObservationType.ATMOSPHERIC -> listOf(
                PossibleCause("Sensor malfunction", 0.20f + (1 - score) * 0.1f),
                PossibleCause("Local weather event", 0.25f + score * 0.1f),
                PossibleCause("Climate anomaly", 0.25f + score * 0.15f),
                PossibleCause("Data synchronization error", 0.15f + (1 - score) * 0.1f),
                PossibleCause("Unknown", 0.15f)
            )
            ObservationType.GEOLOGICAL -> listOf(
                PossibleCause("Measurement artifact", 0.15f + (1 - score) * 0.1f),
                PossibleCause("Tectonic activity", 0.30f + score * 0.15f),
                PossibleCause("Volcanic event", 0.20f + score * 0.1f),
                PossibleCause("Instrumentation error", 0.15f + (1 - score) * 0.1f),
                PossibleCause("Unknown", 0.20f)
            )
            else -> listOf(
                PossibleCause("Measurement artifact", 0.22f + (1 - score) * 0.1f),
                PossibleCause("Environmental effect", 0.31f + score * 0.1f),
                PossibleCause("Rare natural event", 0.28f + score * 0.1f),
                PossibleCause("Unknown", 0.19f)
            )
        }
    }
}
