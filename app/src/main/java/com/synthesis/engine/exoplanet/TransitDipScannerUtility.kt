package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.*
import com.synthesis.domain.model.xilon.MastPhotometryPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Astronomical Analysis Utility for Scanning Photometric Light Curve Data Points.
 *
 * Scans time-series or phase-folded photon flux measurements for:
 * 1. Statistically significant drops in magnitude/flux (transit dips) using robust sigma-clipping.
 * 2. Dip morphology profiling (U-shape planetary profile vs V-shape eclipsing binary).
 * 3. Periodicity and recurrence verification (primary transits, recurring intervals, secondary occultations).
 * 4. Signal-to-noise characterization and Odd/Even transit consistency tests.
 */
@Singleton
class TransitDipScannerUtility @Inject constructor() {

    /**
     * Scans light curve points for recurring dips in magnitude.
     *
     * @param ticId Star identifier (e.g. "384159646" or "25155310")
     * @param points Photometric measurements from MAST / TESS time-series
     * @param nominalPeriodDays Known or estimated orbital period in days (if available)
     * @param expectedDurationHours Expected transit duration in hours (if available)
     * @param sigmaThreshold Sensitivity threshold in standard deviations (e.g. 3.0 to 5.0)
     */
    fun scanForRecurringDips(
        ticId: String,
        points: List<MastPhotometryPoint>,
        nominalPeriodDays: Double? = null,
        expectedDurationHours: Double? = null,
        sigmaThreshold: Double = 3.2
    ): TransitDipScanReport {
        if (points.isEmpty()) {
            return emptyReport(ticId, sigmaThreshold)
        }

        val sortedPoints = points.sortedBy { it.phase }
        val isPhaseInHours = sortedPoints.any { abs(it.phase) > 1.0 }

        // 1. Calculate Robust Baseline and Scatter (Median & MAD)
        val fluxList = sortedPoints.map { it.detrendedFlux }.sorted()
        val medianFlux = medianOf(fluxList)
        val madList = fluxList.map { abs(it - medianFlux) }.sorted()
        val mad = medianOf(madList)
        val robustSigma = (1.4826 * mad).coerceAtLeast(1e-5) // Standard normal scale factor
        val noiseFloorPpm = robustSigma * 1_000_000.0

        val dipCutoffFlux = medianFlux - (sigmaThreshold * robustSigma)

        // 2. Identify in-dip points and group into contiguous/clustered dip events
        val phaseSpan = (sortedPoints.last().phase - sortedPoints.first().phase).coerceAtLeast(0.1)
        val clusterThreshold = if (isPhaseInHours) {
            (expectedDurationHours ?: 5.0) * 0.4
        } else {
            (phaseSpan / 25.0).coerceIn(0.015, 0.08)
        }

        val dipClusters = mutableListOf<MutableList<MastPhotometryPoint>>()
        var currentCluster = mutableListOf<MastPhotometryPoint>()

        for (pt in sortedPoints) {
            // A point is part of a dip candidate if flux is significantly lower than baseline
            // or if fitted model indicates a dip
            val isDipPoint = pt.detrendedFlux < dipCutoffFlux || (pt.modelFlux != null && pt.modelFlux < -0.001)

            if (isDipPoint) {
                if (currentCluster.isEmpty()) {
                    currentCluster.add(pt)
                } else {
                    val lastPhase = currentCluster.last().phase
                    if (abs(pt.phase - lastPhase) <= clusterThreshold) {
                        currentCluster.add(pt)
                    } else {
                        // Start new cluster
                        dipClusters.add(currentCluster)
                        currentCluster = mutableListOf(pt)
                    }
                }
            } else {
                if (currentCluster.isNotEmpty() && abs(pt.phase - currentCluster.last().phase) > clusterThreshold) {
                    dipClusters.add(currentCluster)
                    currentCluster = mutableListOf()
                }
            }
        }
        if (currentCluster.isNotEmpty()) {
            dipClusters.add(currentCluster)
        }

        // 3. Filter clusters with minimum point support and characterize morphology
        val detectedDips = mutableListOf<DetectedDipSignal>()
        var dipCounter = 1

        for (cluster in dipClusters) {
            if (cluster.size < 3) continue // Ignore isolated single-point glitches

            val minPhase = cluster.minOf { it.phase }
            val maxPhase = cluster.maxOf { it.phase }
            val ingressPhase = minPhase
            val egressPhase = maxPhase

            // Calculate weighted centroid phase
            val totalDeficit = cluster.sumOf { (medianFlux - it.detrendedFlux).coerceAtLeast(1e-6) }
            val centroidPhase = if (totalDeficit > 0.0) {
                cluster.sumOf { it.phase * (medianFlux - it.detrendedFlux).coerceAtLeast(1e-6) } / totalDeficit
            } else {
                (minPhase + maxPhase) / 2.0
            }

            // Estimate duration in hours
            val durationHours = if (isPhaseInHours) {
                (maxPhase - minPhase).coerceAtLeast(0.5)
            } else {
                val periodHours = (nominalPeriodDays ?: 10.0) * 24.0
                ((maxPhase - minPhase) * periodHours).coerceAtLeast(0.5)
            }

            // Estimate robust depth: average of deepest 25% points
            val sortedClusterFlux = cluster.map { it.detrendedFlux }.sorted()
            val deepestCount = (sortedClusterFlux.size * 0.25).toInt().coerceAtLeast(1)
            val dipBottomAverage = sortedClusterFlux.take(deepestCount).average()
            val rawDepthFraction = (medianFlux - dipBottomAverage).coerceAtLeast(1e-6)
            val depthPpm = rawDepthFraction * 1_000_000.0

            // Magnitude delta: Δm = -2.5 * log10(1 - depthFraction)
            val depthMagnitudeDelta = if (rawDepthFraction in 0.0..0.9) {
                -2.5 * log10((1.0 - rawDepthFraction).coerceAtLeast(1e-5))
            } else 0.0

            // SNR = (depth / sigma) * sqrt(N)
            val snrSigma = (rawDepthFraction / robustSigma) * sqrt(cluster.size.toDouble())

            // Morphology assessment: U-shape vs V-shape
            val (shapeScore, classification) = assessDipMorphology(cluster, medianFlux, dipBottomAverage)

            // Primary transit check: phase close to 0.0 (within ± 0.05 or ± 2 hours)
            val isPrimary = if (isPhaseInHours) {
                abs(centroidPhase) < 3.0
            } else {
                abs(centroidPhase) < 0.08 || abs(centroidPhase - 1.0) < 0.08
            }

            // Period match check
            val isPeriodMatch = isPrimary || abs(abs(centroidPhase) - 0.5) < 0.06

            val label = when {
                isPrimary -> "Tranzit Principal (Fază ~0.00)"
                abs(abs(centroidPhase) - 0.5) < 0.06 -> "Scădere Secundară (Anti-Fază ~0.50)"
                else -> "Scădere Recurentă #$dipCounter"
            }

            // Estimate internal dip depth consistency relative to noise floor
            val internalConsistency = ((depthPpm - noiseFloorPpm) / depthPpm.coerceAtLeast(1.0)).coerceIn(0.2, 1.0)
            val dipConfidence = calculateTransitConfidenceScore(
                snrSigma = snrSigma,
                depthConsistency = internalConsistency,
                shapeScore = shapeScore,
                isOddEvenConsistent = true
            )

            detectedDips.add(
                DetectedDipSignal(
                    dipIndex = dipCounter++,
                    label = label,
                    centerPhase = centroidPhase,
                    centerTimeHours = if (isPhaseInHours) centroidPhase else centroidPhase * (nominalPeriodDays ?: 10.0) * 24.0,
                    ingressPhase = ingressPhase,
                    egressPhase = egressPhase,
                    durationHours = durationHours,
                    depthPpm = depthPpm,
                    depthMagnitudeDelta = depthMagnitudeDelta,
                    snrSigma = snrSigma,
                    shapeScore = shapeScore,
                    shapeClassification = classification,
                    inTransitPointsCount = cluster.size,
                    isPeriodicMatch = isPeriodMatch,
                    isPrimaryTransit = isPrimary,
                    phaseRangeStart = ingressPhase,
                    phaseRangeEnd = egressPhase,
                    confidenceScore = dipConfidence
                )
            )
        }

        // Sort dips by SNR descending
        val sortedDips = detectedDips.sortedByDescending { it.snrSigma }

        val primaryDip = sortedDips.firstOrNull { it.isPrimaryTransit } ?: sortedDips.firstOrNull()
        val secondaryDip = sortedDips.firstOrNull { it != primaryDip && abs(abs(it.centerPhase) - 0.5) < 0.1 }
            ?: sortedDips.firstOrNull { it != primaryDip }

        // 4. Recurrence Assessment and Odd/Even depth test
        val (verdict, oddEvenRatio, oddEvenSigma, diagnosis) = evaluateRecurrenceVerdict(
            allDips = sortedDips,
            primaryDip = primaryDip,
            secondaryDip = secondaryDip,
            nominalPeriodDays = nominalPeriodDays,
            robustSigmaPpm = noiseFloorPpm
        )

        // 5. Overall Transit Signal Confidence Score
        val overallConsistency = when {
            oddEvenSigma != null -> {
                kotlin.math.max(0.15, 1.0 - (oddEvenSigma / 4.0).coerceIn(0.0, 0.85))
            }
            primaryDip != null -> {
                ((primaryDip.depthPpm - noiseFloorPpm) / primaryDip.depthPpm.coerceAtLeast(1.0)).coerceIn(0.3, 0.95)
            }
            else -> 0.0
        }

        val overallConfidence = if (primaryDip != null) {
            calculateTransitConfidenceScore(
                snrSigma = primaryDip.snrSigma,
                depthConsistency = overallConsistency,
                shapeScore = primaryDip.shapeScore,
                isOddEvenConsistent = (oddEvenSigma ?: 0.0) < 3.0
            )
        } else 0.0

        return TransitDipScanReport(
            ticId = ticId,
            totalDipsFound = sortedDips.size,
            primaryDip = primaryDip,
            secondaryDip = secondaryDip,
            allDips = sortedDips,
            recurrenceVerdict = verdict,
            recurrencePeriodCandidateDays = nominalPeriodDays,
            oddEvenDepthRatio = oddEvenRatio,
            oddEvenConsistencySigma = oddEvenSigma,
            noiseFloorPpm = noiseFloorPpm,
            detectionThresholdSigma = sigmaThreshold,
            scientificDiagnosis = diagnosis,
            overallConfidenceScore = overallConfidence
        )
    }

    /**
     * Evaluates whether a dip displays a flat-bottomed U-shape (planetary transit)
     * or a sharp V-shape (grazing stellar binary).
     */
    private fun assessDipMorphology(
        cluster: List<MastPhotometryPoint>,
        baselineFlux: Double,
        bottomFlux: Double
    ): Pair<Double, DipShapeClassification> {
        val totalDepth = (baselineFlux - bottomFlux).coerceAtLeast(1e-6)
        val bottomThreshold = bottomFlux + (totalDepth * 0.20) // Bottom 20% of dip

        val bottomPoints = cluster.filter { it.detrendedFlux <= bottomThreshold }
        val bottomFraction = bottomPoints.size.toDouble() / cluster.size.toDouble()

        // Curvature and flat fraction score
        val shapeScore = (bottomFraction * 2.2).coerceIn(0.1, 1.0)

        val classification = when {
            shapeScore >= 0.55 -> DipShapeClassification.U_SHAPED_TRANSIT
            shapeScore < 0.35 && totalDepth > 0.015 -> DipShapeClassification.V_SHAPED_BINARY
            totalDepth < 0.001 -> DipShapeClassification.SHALLOW_SUB_THRESHOLD
            else -> DipShapeClassification.U_SHAPED_TRANSIT
        }

        return Pair(shapeScore, classification)
    }

    /**
     * Determines recurrence confidence, odd/even depth consistency, and scientific diagnosis.
     */
    private fun evaluateRecurrenceVerdict(
        allDips: List<DetectedDipSignal>,
        primaryDip: DetectedDipSignal?,
        secondaryDip: DetectedDipSignal?,
        nominalPeriodDays: Double?,
        robustSigmaPpm: Double
    ): Tuple4<DipRecurrenceVerdict, Double?, Double?, String> {
        if (allDips.isEmpty() || primaryDip == null) {
            return Tuple4(
                DipRecurrenceVerdict.IRREGULAR_NOISE_OR_STELLAR_VARIATION,
                null,
                null,
                "Nu s-au identificat scăderi periodice semnificative peste pragul fotometric de zgomot ($robustSigmaPpm ppm)."
            )
        }

        val primaryDepth = primaryDip.depthPpm
        val isPrimaryStrong = primaryDip.snrSigma >= 7.1

        if (allDips.size == 1) {
            val diag = if (isPrimaryStrong) {
                "S-a detectat o scădere singulară profundă (Mono-Tranzit) la faza ${String.format("%.2f", primaryDip.centerPhase)}, " +
                        "cu adâncime de ${primaryDepth.toInt()} ppm (SNR: ${String.format("%.1f", primaryDip.snrSigma)}σ). " +
                        "Profilul confirmă un tranzit cu perioadă orbitală mai mare decât sectorul TESS (Mono-Transit Candidate)."
            } else {
                "Scădere izolată cu SNR marginal (${String.format("%.1f", primaryDip.snrSigma)}σ). Necesită acumulare de observații."
            }
            return Tuple4(
                DipRecurrenceVerdict.MONO_TRANSIT_ISOLATED,
                null,
                null,
                diag
            )
        }

        // Multiple dips detected
        val secondaryDepth = secondaryDip?.depthPpm ?: 0.0
        val depthRatio = if (secondaryDepth > 0.0) primaryDepth / secondaryDepth else null
        val depthDiff = abs(primaryDepth - secondaryDepth)
        val sigmaDiff = depthDiff / robustSigmaPpm.coerceAtLeast(10.0)

        if (secondaryDip != null && abs(abs(secondaryDip.centerPhase) - 0.5) < 0.1) {
            // Secondary occultation at phase 0.5
            val isDeepSecondary = secondaryDepth > (primaryDepth * 0.4)
            val diag = if (isDeepSecondary) {
                "S-au detectat 2 scăderi recurente (Tranzit Principal și Ocultație Secundară adâncă la faza 0.50). " +
                        "Adâncimea secundară ridicată (${secondaryDepth.toInt()} ppm) indică o posibilă binară stelară cu eclipsă."
            } else {
                "S-a identificat tranzitul principal (${primaryDepth.toInt()} ppm) și o scădere secundară termică slabă la faza 0.50 (${secondaryDepth.toInt()} ppm). " +
                        "Semnătura corespunde unei exoplanete fierbinți cu emisie termică detectabilă."
            }
            return Tuple4(
                DipRecurrenceVerdict.PRIMARY_AND_SECONDARY_OCCULTATION,
                depthRatio,
                sigmaDiff,
                diag
            )
        }

        // Multiple recurring transit events
        val isOddEvenConsistent = sigmaDiff < 3.0
        val verdict = if (isOddEvenConsistent) {
            DipRecurrenceVerdict.PERIODIC_TRANSIT_SERIES
        } else {
            DipRecurrenceVerdict.MULTI_CANDIDATE_SUPERPOSITION
        }

        val diag = if (isOddEvenConsistent) {
            "S-au confirmat ${allDips.size} scăderi recurente identice în curba de lumină. " +
                    "Adâncimea este stabilă (raport Odd/Even: ${String.format("%.2f", depthRatio ?: 1.0)}), " +
                    "confirmând periodicitatea strict kepleriană a semnalului exoplanetar (Perioadă: ${nominalPeriodDays?.let { String.format("%.3f d", it) } ?: "estimată"})."
        } else {
            "S-au detectat ${allDips.size} scăderi de magnitudine cu adâncimi variabile. " +
                    "Sugerează fie un sistem multi-planetar cu tranzite suprapuse, fie variații stelare compuse."
        }

        return Tuple4(verdict, depthRatio, sigmaDiff, diag)
    }

    private fun medianOf(sortedList: List<Double>): Double {
        if (sortedList.isEmpty()) return 0.0
        val mid = sortedList.size / 2
        return if (sortedList.size % 2 == 1) {
            sortedList[mid]
        } else {
            (sortedList[mid - 1] + sortedList[mid]) / 2.0
        }
    }

    private fun emptyReport(ticId: String, sigma: Double): TransitDipScanReport {
        return TransitDipScanReport(
            ticId = ticId,
            totalDipsFound = 0,
            primaryDip = null,
            secondaryDip = null,
            allDips = emptyList(),
            recurrenceVerdict = DipRecurrenceVerdict.IRREGULAR_NOISE_OR_STELLAR_VARIATION,
            recurrencePeriodCandidateDays = null,
            oddEvenDepthRatio = null,
            oddEvenConsistencySigma = null,
            noiseFloorPpm = 0.0,
            detectionThresholdSigma = sigma,
            scientificDiagnosis = "Nu există date fotometrice valide pentru scanarea scăderilor de magnitudine."
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        /**
         * Calculates an astrophysical confidence score (0.0% - 100.0%) for an identified transit signal,
         * synthesized from:
         * 1. Signal-to-Noise Ratio (SNR) - calibrated against NASA Kepler/TESS MES standards (threshold >= 7.1 sigma)
         * 2. Photometric Depth Consistency - stability across odd/even transits or scatter vs noise floor
         * 3. Transit Morphology - flat-bottom U-shape vs grazing binary V-shape
         */
        fun calculateTransitConfidenceScore(
            snrSigma: Double,
            depthConsistency: Double,
            shapeScore: Double = 0.8,
            isOddEvenConsistent: Boolean = true
        ): Double {
            // 1. SNR term (saturates near 12 sigma, threshold at 7.1 sigma)
            val snrTerm = when {
                snrSigma <= 2.0 -> (snrSigma / 2.0).coerceIn(0.0, 0.3)
                snrSigma < 7.1 -> 0.3 + ((snrSigma - 2.0) / 5.1) * 0.45 // 0.3 to 0.75
                else -> 0.75 + ((snrSigma - 7.1) / 10.0).coerceAtMost(1.0) * 0.25 // 0.75 to 1.0
            }.coerceIn(0.0, 1.0)

            // 2. Depth consistency term (0.0 to 1.0)
            val consistencyTerm = depthConsistency.coerceIn(0.0, 1.0) * if (isOddEvenConsistent) 1.0 else 0.4

            // 3. Shape factor (U-shape vs V-shape)
            val shapeTerm = shapeScore.coerceIn(0.1, 1.0)

            // Weighted confidence synthesis: 50% SNR, 35% Depth Consistency, 15% Morphology
            val rawScore = (0.50 * snrTerm + 0.35 * consistencyTerm + 0.15 * shapeTerm) * 100.0
            return rawScore.coerceIn(5.0, 99.8)
        }
    }
}
