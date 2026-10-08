package com.synthesis.service

import com.synthesis.domain.model.exoplanet.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Box-fitting Least Squares (BLS) Periodic Transit Detection Engine.
 *
 * Implements the Kovács, Zucker & Mazeh (2002) BLS algorithm (A&A 391, 369-377)
 * to search for periodic box-shaped or trapezoidal brightness decrements in
 * photometric time series extracted from FITS image sequences or video sensors.
 *
 * Includes:
 * 1. Robust iterative sigma-clipping and median baseline normalization
 * 2. Optimal frequency grid generation tailored to the observational baseline
 * 3. Weighted BLS power spectrum computation (Signal Residue SR)
 * 4. Signal Detection Efficiency (SDE) and Transit Signal-to-Noise Ratio (SNR)
 * 5. Phase-folding with adaptive binning
 * 6. Astrophysical False-Positive Vetting (Odd/Even depth comparison & Secondary eclipse check)
 * 7. Keplerian exoplanet physical parameter inversion (Rp, a, Teq, Habitable Zone)
 */
@Singleton
class PeriodicTransitAnalysisEngine @Inject constructor() {

    // Physical Constants (SI & Astronomical)
    private val G = 6.67430e-11        // Gravitational constant (m^3 kg^-1 s^-2)
    private val M_SUN = 1.989e30       // Solar mass in kg
    private val R_SUN = 6.9634e8       // Solar radius in meters
    private val R_EARTH = 6.3781e6     // Earth radius in meters
    private val R_JUPITER = 7.1492e7   // Jupiter radius in meters
    private val AU_METERS = 1.4959787e11
    private val SECONDS_PER_DAY = 86400.0

    /**
     * Executes the complete BLS periodic transit search on FramePhotometrySample data
     * produced by the telescope vision / FITS ingestion pipeline.
     */
    fun analyzeSamples(
        samples: List<FramePhotometrySample>,
        minPeriodDays: Double? = null,
        maxPeriodDays: Double? = null,
        hostStarRadiusSolar: Double = 1.0,
        hostStarMassSolar: Double = 1.0,
        hostStarTeffK: Double = 5778.0
    ): PeriodicTransitDetectionResult {
        if (samples.size < 8) {
            return fallbackNoDetection("Date insuficiente (${samples.size} cadre). Sunt necesare minim 8 cadre FITS.")
        }

        // Extract time in days, normalized flux, and flux error
        val firstBjd = samples.first().bjd
        val rawTime = samples.map {
            if (it.bjd > 2400000.0) it.bjd else firstBjd + (it.timeMinutes / 1440.0)
        }
        val rawFlux = samples.map { it.normalizedFlux }
        val rawErrors = samples.map { it.fluxError.coerceIn(0.0001, 0.05) }

        return runBlsPipeline(
            times = rawTime,
            fluxes = rawFlux,
            errors = rawErrors,
            minPeriodDays = minPeriodDays,
            maxPeriodDays = maxPeriodDays,
            hostStarRadiusSolar = hostStarRadiusSolar,
            hostStarMassSolar = hostStarMassSolar,
            hostStarTeffK = hostStarTeffK
        )
    }

    /**
     * Executes the BLS periodic transit search on general PhotometricDataPoint list.
     */
    fun analyzeDataPoints(
        points: List<PhotometricDataPoint>,
        minPeriodDays: Double? = null,
        maxPeriodDays: Double? = null,
        hostStarRadiusSolar: Double = 1.0,
        hostStarMassSolar: Double = 1.0,
        hostStarTeffK: Double = 5778.0
    ): PeriodicTransitDetectionResult {
        if (points.size < 8) {
            return fallbackNoDetection("Date insuficiente (${points.size} puncte). Sunt necesare minim 8 măsurători fotometrice.")
        }

        val firstBjd = points.first().bjdTime
        val rawTime = points.map {
            if (it.bjdTime > 2400000.0) it.bjdTime else firstBjd + (it.minuteOffset / 1440.0)
        }
        val rawFlux = points.map { it.relativeFlux }
        val rawErrors = points.map { it.fluxError.coerceIn(0.0001, 0.05) }

        return runBlsPipeline(
            times = rawTime,
            fluxes = rawFlux,
            errors = rawErrors,
            minPeriodDays = minPeriodDays,
            maxPeriodDays = maxPeriodDays,
            hostStarRadiusSolar = hostStarRadiusSolar,
            hostStarMassSolar = hostStarMassSolar,
            hostStarTeffK = hostStarTeffK
        )
    }

    /**
     * Core Mathematical Box-fitting Least Squares (BLS) Execution Pipeline.
     */
    fun runBlsPipeline(
        times: List<Double>,
        fluxes: List<Double>,
        errors: List<Double>,
        minPeriodDays: Double? = null,
        maxPeriodDays: Double? = null,
        hostStarRadiusSolar: Double = 1.0,
        hostStarMassSolar: Double = 1.0,
        hostStarTeffK: Double = 5778.0,
        nPhaseBins: Int = 50,
        periodSteps: Int = 180
    ): PeriodicTransitDetectionResult {
        val n = times.size
        if (n < 8) {
            return fallbackNoDetection("Număr insuficient de puncte fotometrice ($n).")
        }

        // 1. Sort temporally
        val sortedIndices = times.indices.sortedBy { times[it] }
        val tSorted = sortedIndices.map { times[it] }
        val ySorted = sortedIndices.map { fluxes[it] }
        val errSorted = sortedIndices.map { errors[it] }

        val t0 = tSorted.first()
        val tSpan = (tSorted.last() - t0).coerceAtLeast(0.01) // Duration of baseline in days

        // 2. Iterative Sigma-Clipping for Outliers (Clip positive flares/cosmics; preserve transit dips)
        val cleanIndices = mutableListOf<Int>()
        val medianFlux = computeMedian(ySorted)
        val mad = computeMad(ySorted, medianFlux).coerceAtLeast(0.0005)

        for (i in ySorted.indices) {
            val dev = ySorted[i] - medianFlux
            val isPositiveFlare = dev > 4.5 * mad
            val isExtremeExtinction = dev < -0.15 // > 15% drop is non-planetary
            if (!isPositiveFlare && !isExtremeExtinction) {
                cleanIndices.add(i)
            }
        }

        val useIndices = if (cleanIndices.size >= 8) cleanIndices else ySorted.indices.toList()
        val tClean = useIndices.map { tSorted[it] }
        val yClean = useIndices.map { ySorted[it] / medianFlux } // Normalize to baseline 1.0
        val errClean = useIndices.map { errSorted[it] / medianFlux }
        val nClean = tClean.size

        // 3. Compute weights: w_i = (1 / sigma_i^2) / sum(1 / sigma_j^2)
        val rawWeights = errClean.map { 1.0 / (it * it).coerceAtLeast(1e-8) }
        val weightSum = rawWeights.sum()
        val wNorm = rawWeights.map { it / weightSum }

        // Mean weighted flux
        val yWeightedMean = tClean.indices.sumOf { wNorm[it] * yClean[it] }

        // 4. Period Grid Determination
        // For short local observations, search periods from fraction of hour up to 2x baseline
        val pMin = minPeriodDays?.coerceAtLeast(0.02) ?: (tSpan * 0.15).coerceIn(0.05, 1.5)
        val pMax = maxPeriodDays?.coerceAtLeast(pMin * 1.2) ?: (tSpan * 2.5).coerceIn(pMin * 1.5, 30.0)

        val fMin = 1.0 / pMax
        val fMax = 1.0 / pMin
        val freqStep = (fMax - fMin) / periodSteps.coerceAtLeast(20)

        // Trial duty cycles (duration fractions q = tau / P)
        val trialQList = listOf(0.01, 0.02, 0.035, 0.05, 0.08, 0.12)

        val periodogram = mutableListOf<BlsPeriodogramPoint>()
        var bestPower = -1.0
        var bestPeriod = pMin
        var bestEpoch = t0
        var bestDepthFraction = 0.0
        var bestDurationDays = 0.05
        var bestQ = 0.03

        // 5. BLS Periodogram Scanning Loop
        for (step in 0..periodSteps) {
            val freq = fMin + (step * freqStep)
            val period = 1.0 / freq

            // Bin data into phase bins [0, 1)
            val binS = DoubleArray(nPhaseBins) { 0.0 } // weighted flux residuals sum
            val binR = DoubleArray(nPhaseBins) { 0.0 } // weight sum
            val binCount = IntArray(nPhaseBins) { 0 }

            for (i in 0 until nClean) {
                val phase = ((tClean[i] - t0) * freq).let { it - floor(it) } // [0, 1)
                val binIdx = (phase * nPhaseBins).toInt().coerceIn(0, nPhaseBins - 1)
                val residual = yClean[i] - yWeightedMean
                binS[binIdx] += wNorm[i] * residual
                binR[binIdx] += wNorm[i]
                binCount[binIdx]++
            }

            var localBestPower = 0.0
            var localBestDepth = 0.0
            var localBestQ = 0.03
            var localBestPhaseCenter = 0.0

            for (q in trialQList) {
                val windowBins = (q * nPhaseBins).roundToInt().coerceIn(1, nPhaseBins / 3)

                for (startBin in 0 until nPhaseBins) {
                    var sSum = 0.0
                    var rSum = 0.0

                    for (k in 0 until windowBins) {
                        val b = (startBin + k) % nPhaseBins
                        sSum += binS[b]
                        rSum += binR[b]
                    }

                    // A transit is a BRIGHTNESS DROP: sSum must be NEGATIVE
                    if (rSum in 0.001..0.999 && sSum < 0.0) {
                        val power = (sSum * sSum) / (rSum * (1.0 - rSum))
                        if (power > localBestPower) {
                            localBestPower = power
                            localBestDepth = (-sSum) / (rSum * (1.0 - rSum))
                            localBestQ = q
                            val centerBin = (startBin + windowBins / 2.0) % nPhaseBins
                            localBestPhaseCenter = centerBin / nPhaseBins.toDouble()
                        }
                    }
                }
            }

            // Approximate SNR at this period
            val snrAtPeriod = (localBestDepth / mad) * sqrt((localBestQ * nClean).coerceAtLeast(1.0))

            val pPoint = BlsPeriodogramPoint(
                periodDays = period,
                periodHours = period * 24.0,
                power = localBestPower,
                depthPpm = localBestDepth * 1_000_000.0,
                durationHours = (period * localBestQ) * 24.0,
                snr = snrAtPeriod
            )
            periodogram.add(pPoint)

            if (localBestPower > bestPower) {
                bestPower = localBestPower
                bestPeriod = period
                bestDepthFraction = localBestDepth
                bestQ = localBestQ
                bestDurationDays = period * localBestQ
                bestEpoch = t0 + (localBestPhaseCenter * period)
            }
        }

        // 6. Signal Detection Efficiency (SDE)
        val powers = periodogram.map { it.power }
        val meanPower = powers.average()
        val stdPower = sqrt(powers.map { (it - meanPower).pow(2) }.average()).coerceAtLeast(1e-6)
        val sde = (bestPower - meanPower) / stdPower

        // 7. Phase Folding at Best Period and Epoch
        val phaseFoldedPoints = mutableListOf<PhaseFoldedPoint>()
        val inTransitHalfWidth = (bestQ * 0.5)

        for (i in 0 until nClean) {
            val t = tClean[i]
            val cycle = floor((t - bestEpoch) / bestPeriod)
            var phase = ((t - bestEpoch) / bestPeriod) - cycle // [0, 1)
            if (phase > 0.5) phase -= 1.0 // [-0.5, 0.5)

            val isInTransit = abs(phase) <= inTransitHalfWidth

            phaseFoldedPoints.add(
                PhaseFoldedPoint(
                    phase = phase,
                    relativeFlux = yClean[i],
                    fluxError = errClean[i],
                    timeMinutes = (t - t0) * 1440.0,
                    bjd = t,
                    epochIndex = cycle.toLong(),
                    isInTransit = isInTransit
                )
            )
        }
        phaseFoldedPoints.sortBy { it.phase }

        // 8. Binned Phase Curve (50 bins across [-0.5, 0.5])
        val binnedCurve = computePhaseBins(phaseFoldedPoints, binCount = 40)

        // 9. Generate Trapezoid Transit Model Curve
        val modelCurve = generateTransitModelCurve(
            depthFraction = bestDepthFraction,
            halfWidthPhase = inTransitHalfWidth,
            ingressFraction = 0.15
        )

        // 10. Odd-Even Depth Vetting (Binary Star Discrimination)
        val oddTransits = phaseFoldedPoints.filter { it.isInTransit && (it.epochIndex % 2L != 0L) }
        val evenTransits = phaseFoldedPoints.filter { it.isInTransit && (it.epochIndex % 2L == 0L) }
        val outOfTransit = phaseFoldedPoints.filter { !it.isInTransit }

        val baselineMean = if (outOfTransit.isNotEmpty()) outOfTransit.map { it.relativeFlux }.average() else 1.0
        val oddDepth = if (oddTransits.isNotEmpty()) (baselineMean - oddTransits.map { it.relativeFlux }.average()).coerceAtLeast(0.0) else bestDepthFraction
        val evenDepth = if (evenTransits.isNotEmpty()) (baselineMean - evenTransits.map { it.relativeFlux }.average()).coerceAtLeast(0.0) else bestDepthFraction

        val oddErr = if (oddTransits.isNotEmpty()) mad / sqrt(oddTransits.size.toDouble()) else mad
        val evenErr = if (evenTransits.isNotEmpty()) mad / sqrt(evenTransits.size.toDouble()) else mad
        val oddEvenDiffSigma = abs(oddDepth - evenDepth) / sqrt(oddErr * oddErr + evenErr * evenErr).coerceAtLeast(1e-6)
        val isOddEvenConsistent = oddEvenDiffSigma < 3.0 // < 3 sigma confirms identical depths (Planet, not EB)

        // 11. Secondary Eclipse Vetting (Searches for companion occultation at phase 0.5)
        val secEclipseWindow = phaseFoldedPoints.filter { abs(abs(it.phase) - 0.5) < (inTransitHalfWidth * 1.2) }
        val secDepth = if (secEclipseWindow.isNotEmpty()) {
            (baselineMean - secEclipseWindow.map { it.relativeFlux }.average()).coerceAtLeast(0.0)
        } else 0.0
        val secErr = mad / sqrt(secEclipseWindow.size.coerceAtLeast(1).toDouble())
        val secSigma = secDepth / secErr.coerceAtLeast(1e-6)
        val isSecDetected = secSigma >= 3.0 && (secDepth > bestDepthFraction * 0.25)

        // 12. Chi-Squared Fit Evaluation vs Flat Line
        var chiSqFlat = 0.0
        var chiSqTransit = 0.0
        for (pt in phaseFoldedPoints) {
            val err = pt.fluxError.coerceAtLeast(0.0005)
            val resFlat = pt.relativeFlux - baselineMean
            chiSqFlat += (resFlat * resFlat) / (err * err)

            val modelVal = evaluateModelAtPhase(pt.phase, bestDepthFraction, inTransitHalfWidth, 0.15)
            val resTransit = pt.relativeFlux - modelVal
            chiSqTransit += (resTransit * resTransit) / (err * err)
        }
        val deltaChiSq = (chiSqFlat - chiSqTransit).coerceAtLeast(0.0)

        // 13. Exoplanet Physical Parameters Inversion
        val physicalParams = deriveExoplanetPhysicalParameters(
            periodDays = bestPeriod,
            depthFraction = bestDepthFraction,
            durationDays = bestDurationDays,
            hostStarRadiusSolar = hostStarRadiusSolar,
            hostStarMassSolar = hostStarMassSolar,
            hostStarTeffK = hostStarTeffK
        )

        // 14. Detection Significance & Vetting Decisions
        val depthPpm = bestDepthFraction * 1_000_000.0
        val inTransitCount = phaseFoldedPoints.count { it.isInTransit }.coerceAtLeast(1)
        val transitSnr = (bestDepthFraction / mad) * sqrt(inTransitCount.toDouble())

        // Detection Criteria:
        // SDE >= 3.8, Depth >= 300 ppm, SNR >= 3.0
        val isDetected = sde >= 3.8 && depthPpm >= 300.0 && transitSnr >= 3.0

        val maxTheoreticalDurationDays = (hostStarRadiusSolar * bestPeriod) / (Math.PI * physicalParams.semiMajorAxisAu * 215.0)
        val durationRatio = bestDurationDays / maxTheoreticalDurationDays.coerceAtLeast(0.001)
        val isDurationPhysical = durationRatio in 0.05..1.4

        val vettingMetrics = TransitVettingMetrics(
            oddTransitDepthPpm = oddDepth * 1_000_000.0,
            evenTransitDepthPpm = evenDepth * 1_000_000.0,
            oddEvenSigmaDiff = oddEvenDiffSigma,
            isOddEvenConsistent = isOddEvenConsistent,
            secondaryEclipseDepthPpm = secDepth * 1_000_000.0,
            secondaryEclipseSigma = secSigma,
            isSecondaryEclipseDetected = isSecDetected,
            durationKeplerianRatio = durationRatio,
            isDurationPhysical = isDurationPhysical,
            chiSquaredFlat = chiSqFlat / nClean,
            chiSquaredTransit = chiSqTransit / nClean,
            deltaChiSquared = deltaChiSq
        )

        val status = buildString {
            if (isDetected) {
                append("★ SEMNAL PERIODIC VALIDAT [BLS SDE = ${String.format("%.2f", sde)}, SNR = ${String.format("%.1f", transitSnr)}]. ")
                append("P = ${String.format("%.3f", bestPeriod)} d (${String.format("%.1f", bestPeriod * 24.0)} h), ")
                append("Adâncime = ${String.format("%.0f", depthPpm)} ppm. ")
                if (!isOddEvenConsistent) {
                    append("⚠️ Alertă Binară: Discrepanță adâncime Impar/Par (${String.format("%.1f", oddEvenDiffSigma)}σ). ")
                } else if (isSecDetected) {
                    append("⚠️ Alertă Binară: Eclipsă secundară detectată la faza 0.5. ")
                } else {
                    append("Candidat exoplanetă robust (${physicalParams.planetClassification}, Rp = ${String.format("%.2f", physicalParams.planetRadiusEarth)} R⊕). ")
                    if (physicalParams.isHabitableZone) {
                        append("🌍 În Zona Locuibilă (Teq = ${physicalParams.equilibriumTempK.roundToInt()} K)!")
                    }
                }
            } else {
                append("Niciun semnal periodic semnificativ peste pragul SDE (SDE = ${String.format("%.2f", sde)} < 4.5, SNR = ${String.format("%.1f", transitSnr)}).")
            }
        }

        return PeriodicTransitDetectionResult(
            isDetected = isDetected,
            bestPeriodDays = bestPeriod,
            bestPeriodHours = bestPeriod * 24.0,
            epochBjd = bestEpoch,
            transitDepthPpm = depthPpm,
            transitDepthFraction = bestDepthFraction,
            transitDurationHours = bestDurationDays * 24.0,
            dutyCycleQ = bestQ,
            signalDetectionEfficiencySde = sde,
            transitSnr = transitSnr,
            vetting = vettingMetrics,
            physicalParameters = physicalParams,
            periodogram = periodogram,
            phaseFoldedPoints = phaseFoldedPoints,
            binnedPhaseCurve = binnedCurve,
            transitModelCurve = modelCurve,
            statusSummary = status
        )
    }

    /**
     * Inverts transit parameters into physical orbital and planetary properties.
     */
    private fun deriveExoplanetPhysicalParameters(
        periodDays: Double,
        depthFraction: Double,
        durationDays: Double,
        hostStarRadiusSolar: Double,
        hostStarMassSolar: Double,
        hostStarTeffK: Double
    ): DerivedExoplanetParameters {
        val rStarMeters = hostStarRadiusSolar * R_SUN
        val mStarKg = hostStarMassSolar * M_SUN
        val periodSeconds = periodDays * SECONDS_PER_DAY

        // 1. Planet Radius: Rp = R* * sqrt(depth)
        val radiusRatio = sqrt(depthFraction.coerceAtLeast(1e-8))
        val planetRadiusMeters = rStarMeters * radiusRatio
        val planetRadiusEarth = planetRadiusMeters / R_EARTH
        val planetRadiusJupiter = planetRadiusMeters / R_JUPITER

        // 2. Semi-Major Axis via Kepler's 3rd Law:
        // a = (G * M* * P^2 / (4 * pi^2))^(1/3)
        val aMeters = ( (G * mStarKg * periodSeconds.pow(2)) / (4.0 * Math.PI.pow(2)) ).pow(1.0 / 3.0)
        val semiMajorAxisAu = aMeters / AU_METERS

        // 3. Orbital velocity: v = 2 * pi * a / P
        val vOrbitalMps = (2.0 * Math.PI * aMeters) / periodSeconds
        val vOrbitalKmS = vOrbitalMps / 1000.0

        // 4. Impact parameter b and inclination i
        val tauSeconds = durationDays * SECONDS_PER_DAY
        val chordFraction = (vOrbitalMps * tauSeconds) / (2.0 * rStarMeters)
        val bClamped = if (chordFraction < 1.0) sqrt(1.0 - chordFraction.pow(2)).coerceIn(0.0, 0.98) else 0.0
        val inclinationRad = acos((bClamped * rStarMeters / aMeters).coerceIn(0.0, 1.0))
        val inclinationDeg = Math.toDegrees(inclinationRad)

        // 5. Stellar Luminosity & Insolation
        // L / L_sun = (R / R_sun)^2 * (Teff / 5778)^4
        val luminositySolar = hostStarRadiusSolar.pow(2) * (hostStarTeffK / 5778.0).pow(4)
        val insolationEarth = luminositySolar / semiMajorAxisAu.pow(2)

        // 6. Equilibrium Temperature (assuming Bond albedo A = 0.3)
        // Teq = Teff * sqrt(R* / (2 * a)) * (1 - A)^0.25
        val rStarAu = rStarMeters / AU_METERS
        val teqK = hostStarTeffK * sqrt(rStarAu / (2.0 * semiMajorAxisAu)) * (1.0 - 0.3).pow(0.25)

        // 7. Habitable Zone criteria (Kopparapu et al. 2013)
        val isHz = insolationEarth in 0.32..1.25

        // 8. Geometric Transit Probability: p = (R* + Rp) / a
        val prob = ((rStarMeters + planetRadiusMeters) / aMeters).coerceIn(0.001, 0.5)

        // 9. Planet classification
        val classification = when {
            planetRadiusEarth < 0.8 -> "Sub-Terestră"
            planetRadiusEarth in 0.8..1.25 -> "Planeta Terestră (Dimensiune Pământ)"
            planetRadiusEarth in 1.25..2.0 -> "Super-Pământ"
            planetRadiusEarth in 2.0..4.0 -> "Sub-Neptuniană"
            planetRadiusEarth in 4.0..8.0 -> "Gigant de Gheață (Tip Neptun)"
            periodDays < 10.0 -> "Jupiter Fierbinte (Hot Jupiter)"
            else -> "Gigant Gazos (Tip Jupiter)"
        }

        return DerivedExoplanetParameters(
            planetRadiusEarth = planetRadiusEarth,
            planetRadiusJupiter = planetRadiusJupiter,
            radiusRatioRpRs = radiusRatio,
            semiMajorAxisAu = semiMajorAxisAu,
            orbitalVelocityKmS = vOrbitalKmS,
            orbitalInclinationDeg = inclinationDeg,
            impactParameterB = bClamped,
            equilibriumTempK = teqK,
            insolationSolar = insolationEarth,
            isHabitableZone = isHz,
            planetClassification = classification,
            geometricTransitProbability = prob
        )
    }

    /**
     * Bins phase-folded points into uniformly spaced intervals across [-0.5, +0.5].
     */
    private fun computePhaseBins(
        points: List<PhaseFoldedPoint>,
        binCount: Int = 40
    ): List<PhaseBinPoint> {
        val binWidth = 1.0 / binCount
        val bins = mutableListOf<PhaseBinPoint>()

        for (b in 0 until binCount) {
            val phaseStart = -0.5 + (b * binWidth)
            val phaseEnd = phaseStart + binWidth
            val phaseCenter = phaseStart + (binWidth * 0.5)

            val inBin = points.filter { it.phase in phaseStart..phaseEnd }
            if (inBin.isNotEmpty()) {
                val meanFlux = inBin.map { it.relativeFlux }.average()
                val variance = inBin.map { (it.relativeFlux - meanFlux).pow(2) }.average()
                val error = (sqrt(variance) / sqrt(inBin.size.toDouble())).coerceAtLeast(0.0002)

                bins.add(
                    PhaseBinPoint(
                        phaseCenter = phaseCenter,
                        meanFlux = meanFlux,
                        fluxError = error,
                        pointCount = inBin.size
                    )
                )
            }
        }
        return bins
    }

    /**
     * Generates a 100-point trapezoidal/limb-darkened transit model curve over phase [-0.5, 0.5].
     */
    private fun generateTransitModelCurve(
        depthFraction: Double,
        halfWidthPhase: Double,
        ingressFraction: Double = 0.15
    ): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val step = 0.01 // 100 points
        for (i in -50..50) {
            val phase = i * step
            val modelFlux = evaluateModelAtPhase(phase, depthFraction, halfWidthPhase, ingressFraction)
            points.add(phase to modelFlux)
        }
        return points
    }

    private fun evaluateModelAtPhase(
        phase: Double,
        depthFraction: Double,
        halfWidthPhase: Double,
        ingressFraction: Double = 0.15
    ): Double {
        val absP = abs(phase)
        val fullInWidth = halfWidthPhase * (1.0 - ingressFraction)

        return when {
            absP <= fullInWidth -> 1.0 - depthFraction
            absP <= halfWidthPhase -> {
                // Linear ingress / egress ramp
                val ramp = (halfWidthPhase - absP) / (halfWidthPhase - fullInWidth).coerceAtLeast(1e-5)
                1.0 - (depthFraction * ramp)
            }
            else -> 1.0
        }
    }

    private fun computeMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 1.0
        val sorted = values.sorted()
        return if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
    }

    private fun computeMad(values: List<Double>, median: Double): Double {
        if (values.isEmpty()) return 0.001
        val deviations = values.map { abs(it - median) }
        val madRaw = computeMedian(deviations)
        return (1.4826 * madRaw).coerceAtLeast(0.0001) // Normal distribution consistent scale
    }

    private fun fallbackNoDetection(message: String): PeriodicTransitDetectionResult {
        val dummyVetting = TransitVettingMetrics(
            oddTransitDepthPpm = 0.0,
            evenTransitDepthPpm = 0.0,
            oddEvenSigmaDiff = 0.0,
            isOddEvenConsistent = true,
            secondaryEclipseDepthPpm = 0.0,
            secondaryEclipseSigma = 0.0,
            isSecondaryEclipseDetected = false,
            durationKeplerianRatio = 0.0,
            isDurationPhysical = false,
            chiSquaredFlat = 1.0,
            chiSquaredTransit = 1.0,
            deltaChiSquared = 0.0
        )
        val dummyParams = DerivedExoplanetParameters(
            planetRadiusEarth = 0.0,
            planetRadiusJupiter = 0.0,
            radiusRatioRpRs = 0.0,
            semiMajorAxisAu = 0.0,
            orbitalVelocityKmS = 0.0,
            orbitalInclinationDeg = 90.0,
            impactParameterB = 0.0,
            equilibriumTempK = 0.0,
            insolationSolar = 0.0,
            isHabitableZone = false,
            planetClassification = "Nedeterminat",
            geometricTransitProbability = 0.0
        )
        return PeriodicTransitDetectionResult(
            isDetected = false,
            bestPeriodDays = 0.0,
            bestPeriodHours = 0.0,
            epochBjd = 0.0,
            transitDepthPpm = 0.0,
            transitDepthFraction = 0.0,
            transitDurationHours = 0.0,
            dutyCycleQ = 0.0,
            signalDetectionEfficiencySde = 0.0,
            transitSnr = 0.0,
            vetting = dummyVetting,
            physicalParameters = dummyParams,
            periodogram = emptyList(),
            phaseFoldedPoints = emptyList(),
            binnedPhaseCurve = emptyList(),
            transitModelCurve = emptyList(),
            statusSummary = message
        )
    }
}
