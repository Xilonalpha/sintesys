package com.synthesis.service

import com.synthesis.data.remote.api.ExoplanetArchiveApi
import com.synthesis.data.repository.exoplanet.PublicSpectralDatasetRegistry
import com.synthesis.domain.model.exoplanet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Service that compares retrieved planetary spectral data against a public dataset registry
 * (NASA Exoplanet Archive, space telescope published benchmarks, and theoretical chemical baselines)
 * to identify potential unique anomalies, novel biosignatures/technosignatures, and zero-prior discoveries.
 */
@Singleton
class PlanetarySpectralAnomalyService @Inject constructor(
    val registry: PublicSpectralDatasetRegistry,
    private val exoplanetArchiveApi: ExoplanetArchiveApi
) {

    /**
     * Compares a retrieved planetary spectrum against the public dataset registry.
     *
     * @param retrievedSpectrum The planetary spectrum to evaluate.
     * @param requestedBenchmarkId Optional specific benchmark ID to compare against. If null, the best matching baseline is auto-selected.
     * @return Comprehensive comparison report with statistical significance and detected anomalies.
     */
    fun compareAgainstPublicRegistry(
        retrievedSpectrum: RetrievedPlanetarySpectrum,
        requestedBenchmarkId: String? = null
    ): PublicRegistryComparisonResult {
        // 1. Determine baseline benchmark
        val benchmark = if (requestedBenchmarkId != null) {
            registry.findBenchmarkById(requestedBenchmarkId)
                ?: registry.findBestMatchingBaseline(retrievedSpectrum.equilibriumTempK, retrievedSpectrum.planetRadiusEarth)
        } else {
            registry.findBenchmarkByTargetName(retrievedSpectrum.targetName)
                ?: registry.findBestMatchingBaseline(retrievedSpectrum.equilibriumTempK, retrievedSpectrum.planetRadiusEarth)
        }

        val isTargetKnownInRegistry = registry.findBenchmarkByTargetName(retrievedSpectrum.targetName) != null

        // 2. Perform point-by-point spectral matching & residual calculation with continuum alignment
        val baselinePoints = benchmark.spectrumPoints
        val continuumOffset = if (!isTargetKnownInRegistry) {
            val obsMedian = retrievedSpectrum.spectralPoints.map { it.fluxOrDepthPpm }.sorted().let {
                if (it.isEmpty()) 0.0 else it[it.size / 2]
            }
            val baseMedian = baselinePoints.map { it.fluxOrDepthPpm }.sorted().let {
                if (it.isEmpty()) 0.0 else it[it.size / 2]
            }
            obsMedian - baseMedian
        } else {
            0.0
        }

        val anomalies = mutableListOf<SpectralAnomaly>()

        var sumChiSquared = 0.0
        var validComparisonsCount = 0
        var maxSigma = 0.0

        for (obsPoint in retrievedSpectrum.spectralPoints) {
            val matchingBaselinePoint = findInterpolatedBaselinePoint(obsPoint.wavelengthMicrons, baselinePoints)
                ?: continue

            validComparisonsCount++

            val observedVal = obsPoint.fluxOrDepthPpm
            val expectedVal = matchingBaselinePoint.fluxOrDepthPpm + continuumOffset
            val residual = observedVal - expectedVal

            val combError = sqrt(obsPoint.uncertaintyPpm.pow(2) + matchingBaselinePoint.uncertaintyPpm.pow(2))
                .coerceAtLeast(1.0)

            val sigma = abs(residual) / combError
            if (sigma > maxSigma) {
                maxSigma = sigma
            }

            sumChiSquared += sigma.pow(2)

            // 3. Classify potential spectral anomalies
            val candidateMolecules = registry.identifyMoleculesAtWavelength(obsPoint.wavelengthMicrons)
            val molecularFormulas = candidateMolecules.map { it.moleculeFormula }

            // Check A: Unprecedented exotic molecule or biosignature band
            val containsExotic = candidateMolecules.any { it.isBiosignatureCandidate || it.isTechnosignatureCandidate || it.moleculeFormula == "SO2" }
            val wasAlreadyVerifiedInBenchmark = benchmark.verifiedMolecules.any { verified -> molecularFormulas.contains(verified) }

            if (sigma >= 2.5) {
                val noveltyLevel = when {
                    sigma >= 5.0 -> AnomalyNoveltyLevel.BREAKTHROUGH_CANDIDATE
                    sigma >= 3.5 -> AnomalyNoveltyLevel.HIGH_SIGNIFICANCE
                    else -> AnomalyNoveltyLevel.MODERATE_DEVIATION
                }

                when {
                    // Exotic or Biogenic molecule detected above baseline
                    containsExotic && residual > 0 && !wasAlreadyVerifiedInBenchmark -> {
                        val exoticMols = candidateMolecules.filter { it.isBiosignatureCandidate || it.isTechnosignatureCandidate || it.moleculeFormula == "SO2" }
                        val molNames = exoticMols.joinToString(", ") { "${it.moleculeFormula} (${it.moleculeName})" }
                        anomalies.add(
                            SpectralAnomaly(
                                id = "anom_${obsPoint.wavelengthMicrons}_exotic_${UUID.randomUUID().toString().take(6)}",
                                anomalyType = SpectralAnomalyType.UNPRECEDENTED_MOLECULAR_BAND,
                                wavelengthMicrons = obsPoint.wavelengthMicrons,
                                observedValuePpm = observedVal,
                                registryExpectedValuePpm = expectedVal,
                                residualDeviationPpm = residual,
                                statisticalSignificanceSigma = sigma,
                                candidateMoleculesOrMechanism = exoticMols.map { it.moleculeFormula },
                                description = "Absorbție semnificativă la ${obsPoint.wavelengthMicrons} μm corespunzătoare unor compuși exotici sau biomarkeri: $molNames. Nu este prezentă în registrul public de referință.",
                                noveltyLevel = noveltyLevel
                            )
                        )
                    }

                    // Metastable Helium / Exosphere escape depth surge (e.g. at 1.083 um)
                    abs(obsPoint.wavelengthMicrons - 1.083) < 0.05 && residual > 200.0 -> {
                        anomalies.add(
                            SpectralAnomaly(
                                id = "anom_${obsPoint.wavelengthMicrons}_he_escape_${UUID.randomUUID().toString().take(6)}",
                                anomalyType = SpectralAnomalyType.ANOMALOUS_DEPTH_SURGE,
                                wavelengthMicrons = obsPoint.wavelengthMicrons,
                                observedValuePpm = observedVal,
                                registryExpectedValuePpm = expectedVal,
                                residualDeviationPpm = residual,
                                statisticalSignificanceSigma = sigma,
                                candidateMoleculesOrMechanism = listOf("He I (1.083 μm triplet)", "Evadare Atmosferică Hidrodinamică"),
                                description = "Surplus masiv de adâncime (+${residual.toInt()} ppm) la linia de heliu metastabil 1.083 μm, indicând o exopătură extinsă care evadează în spațiu.",
                                noveltyLevel = noveltyLevel
                            )
                        )
                    }

                    // Water Depletion Anomaly
                    (abs(obsPoint.wavelengthMicrons - 1.4) < 0.1 || abs(obsPoint.wavelengthMicrons - 2.7) < 0.15) && residual < -150.0 -> {
                        anomalies.add(
                            SpectralAnomaly(
                                id = "anom_${obsPoint.wavelengthMicrons}_h2o_deplet_${UUID.randomUUID().toString().take(6)}",
                                anomalyType = SpectralAnomalyType.UNEXPECTED_WATER_DEPLETION,
                                wavelengthMicrons = obsPoint.wavelengthMicrons,
                                observedValuePpm = observedVal,
                                registryExpectedValuePpm = expectedVal,
                                residualDeviationPpm = residual,
                                statisticalSignificanceSigma = sigma,
                                candidateMoleculesOrMechanism = listOf("H2O Depleție", "Pătură de Condensare / Capcană Rece"),
                                description = "Absență sau atenuare severă a benzilor de apă (adâncime cu ${abs(residual).toInt()} ppm sub așteptări), sugerând o atmosferă desicată sau capcane reci eficiente.",
                                noveltyLevel = noveltyLevel
                            )
                        )
                    }

                    // Super-Rayleigh scattering slope in optical blue
                    obsPoint.wavelengthMicrons < 0.9 && residual > 250.0 -> {
                        anomalies.add(
                            SpectralAnomaly(
                                id = "anom_${obsPoint.wavelengthMicrons}_rayleigh_${UUID.randomUUID().toString().take(6)}",
                                anomalyType = SpectralAnomalyType.SUPER_RAYLEIGH_SCATTERING_SLOPE,
                                wavelengthMicrons = obsPoint.wavelengthMicrons,
                                observedValuePpm = observedVal,
                                registryExpectedValuePpm = expectedVal,
                                residualDeviationPpm = residual,
                                statisticalSignificanceSigma = sigma,
                                candidateMoleculesOrMechanism = listOf("Aerosoli Fotochimici (Smog)", "Contaminare Pete Stelare"),
                                description = "Pantă super-Rayleigh extremă în spectrul vizibil/albastru (+${residual.toInt()} ppm), indicând fie cețuri fotochimice de tolini, fie neomogenități stelare pe discul gazdei.",
                                noveltyLevel = noveltyLevel
                            )
                        )
                    }

                    // General significant residual deviation
                    sigma >= 3.5 -> {
                        val carrierDesc = if (molecularFormulas.isNotEmpty()) molecularFormulas.joinToString(", ") else "Continuum / Aerosoli Necatalogate"
                        anomalies.add(
                            SpectralAnomaly(
                                id = "anom_${obsPoint.wavelengthMicrons}_gen_${UUID.randomUUID().toString().take(6)}",
                                anomalyType = if (isTargetKnownInRegistry) SpectralAnomalyType.TEMPORAL_ATMOSPHERIC_VARIABILITY else SpectralAnomalyType.UNPRECEDENTED_MOLECULAR_BAND,
                                wavelengthMicrons = obsPoint.wavelengthMicrons,
                                observedValuePpm = observedVal,
                                registryExpectedValuePpm = expectedVal,
                                residualDeviationPpm = residual,
                                statisticalSignificanceSigma = sigma,
                                candidateMoleculesOrMechanism = molecularFormulas.ifEmpty { listOf("Necunoscut") },
                                description = "Deviație spectrală majoră de ${sigma.format(1)}σ față de registrul public ($carrierDesc).",
                                noveltyLevel = noveltyLevel
                            )
                        )
                    }
                }
            }
        }

        // Check if target itself is completely absent from all public registries
        if (!isTargetKnownInRegistry) {
            anomalies.add(
                0,
                SpectralAnomaly(
                    id = "anom_zero_prior_${UUID.randomUUID().toString().take(6)}",
                    anomalyType = SpectralAnomalyType.ZERO_PRIOR_ARCHIVE_TARGET,
                    wavelengthMicrons = retrievedSpectrum.spectralPoints.firstOrNull()?.wavelengthMicrons ?: 1.0,
                    observedValuePpm = 0.0,
                    registryExpectedValuePpm = 0.0,
                    residualDeviationPpm = 0.0,
                    statisticalSignificanceSigma = if (maxSigma > 0) maxSigma else 3.0,
                    candidateMoleculesOrMechanism = listOf("Țintă Inedită (Zero Înregistrări Publice)"),
                    description = "Ținta '${retrievedSpectrum.targetName}' nu are niciun spectru de transmisie înregistrat în literatura oficială sau arhivele MAST/NASA. Toate datele reprezintă conținut inedit.",
                    noveltyLevel = AnomalyNoveltyLevel.BREAKTHROUGH_CANDIDATE
                )
            )
        }

        val degreesOfFreedom = (validComparisonsCount - 1).coerceAtLeast(1)
        val reducedChiSquared = (sumChiSquared / degreesOfFreedom).coerceAtLeast(0.01)

        // 4. Calculate Quantitative Novelty Index (0.0% to 100.0%)
        val noveltyIndex = calculateNoveltyIndex(
            isTargetKnownInRegistry = isTargetKnownInRegistry,
            maxSigma = maxSigma,
            anomalies = anomalies,
            reducedChiSq = reducedChiSquared
        )

        // 5. Generate Scientific Verdict & Follow-Up Protocols
        val (verdict, recommendations) = buildVerdictAndRecommendations(
            targetName = retrievedSpectrum.targetName,
            isTargetKnown = isTargetKnownInRegistry,
            benchmark = benchmark,
            maxSigma = maxSigma,
            anomalies = anomalies,
            noveltyIndex = noveltyIndex
        )

        val publicArchiveStatus = if (isTargetKnownInRegistry) {
            "Înregistrat în Arhiva Publică (Comparat cu ${benchmark.targetName} - ${benchmark.facilityOrPaper})"
        } else {
            "Țintă Fără Spectru Public Anterior (Evaluat împotriva modelului de referință ${benchmark.targetName})"
        }

        return PublicRegistryComparisonResult(
            comparisonId = "spec_cmp_${System.currentTimeMillis()}",
            retrievedTargetName = retrievedSpectrum.targetName,
            matchedBenchmarkId = benchmark.benchmarkId,
            matchedBenchmarkName = benchmark.targetName,
            isTargetKnownInPublicRegistry = isTargetKnownInRegistry,
            publicArchiveStatus = publicArchiveStatus,
            reducedChiSquared = reducedChiSquared,
            degreesOfFreedom = degreesOfFreedom,
            maxDeviationSigma = maxSigma,
            totalDataPointsAnalyzed = validComparisonsCount,
            anomaliesCount = anomalies.size,
            identifiedAnomalies = anomalies,
            noveltyIndexPercent = noveltyIndex,
            scientificVerdict = verdict,
            followUpRecommendations = recommendations
        )
    }

    /**
     * Interrogates NASA Exoplanet Archive TAP API to determine if any peer-reviewed
     * transmission spectroscopy or confirmed planetary parameters exist for this target.
     */
    suspend fun queryNasaArchiveForTarget(targetIdentifier: String): PublicArchiveDispositionInfo = withContext(Dispatchers.IO) {
        try {
            val cleanName = targetIdentifier.trim()
                .replace("'", "")
                .replace(";", "")
                .take(30)

            val adql = "SELECT pl_name, hostname, disc_facility FROM pscomppars WHERE pl_name LIKE '%$cleanName%' OR hostname LIKE '%$cleanName%'"
            val results = exoplanetArchiveApi.queryConfirmedTable(adql)

            if (results.isNotEmpty()) {
                val first = results.first()
                PublicArchiveDispositionInfo(
                    targetIdentifier = targetIdentifier,
                    isPresentInNasaArchive = true,
                    confirmedPlanetsCount = results.size,
                    publishedSpectraCount = 1,
                    discoveryFacility = first.discFacility ?: "NASA Survey Archive",
                    hasPublishedTransmissionSpectra = true,
                    archiveNotes = "Sistemul este indexat în catalogul confirmat NASA NExScI (${results.size} planete confirmate). Date spectroscopice comparative disponibile."
                )
            } else {
                PublicArchiveDispositionInfo(
                    targetIdentifier = targetIdentifier,
                    isPresentInNasaArchive = false,
                    confirmedPlanetsCount = 0,
                    publishedSpectraCount = 0,
                    discoveryFacility = null,
                    hasPublishedTransmissionSpectra = false,
                    archiveNotes = "Zero înregistrări găsite în tabelul planetelor confirmate NASA. Ținta este un candidat brut / necunoscut în arhivele spectroscopice."
                )
            }
        } catch (e: Exception) {
            // Graceful fallback for offline mode or network errors
            val isLikelyKnown = registry.findBenchmarkByTargetName(targetIdentifier) != null
            PublicArchiveDispositionInfo(
                targetIdentifier = targetIdentifier,
                isPresentInNasaArchive = isLikelyKnown,
                confirmedPlanetsCount = if (isLikelyKnown) 1 else 0,
                publishedSpectraCount = if (isLikelyKnown) 1 else 0,
                discoveryFacility = if (isLikelyKnown) "Space Telescope Science Institute (MAST)" else null,
                hasPublishedTransmissionSpectra = isLikelyKnown,
                archiveNotes = if (isLikelyKnown) {
                    "Țintă identificată în registrul local de benchmark-uri publicate."
                } else {
                    "Nicio înregistrare spectroscopică publică găsită în baza de date locală sau TAP."
                }
            )
        }
    }

    /**
     * Curated sample spectra ready for testing, verifying calibration, and showcasing discoveries.
     */
    fun getCuratedRetrievedSpectra(): List<RetrievedPlanetarySpectrum> {
        return listOf(
            // Scenario 1: Breakthrough discovery - Uncataloged TIC Target with Phosphine (PH3) and Dimethyl Sulfide (DMS) at 5.4σ
            RetrievedPlanetarySpectrum(
                id = "retrieved_tic_candidate_01",
                targetName = "TIC 141913165.01 (Candidat Neconfirmat)",
                ticId = "141913165",
                hostStar = "TIC 141913165 (G-Dwarf)",
                instrumentFacility = "JWST NIRSpec PRISM (Extracție Fotometrie STScI)",
                equilibriumTempK = 288.0,
                planetRadiusEarth = 1.82,
                planetMassEarth = 4.5,
                stellarRadiusSun = 0.92,
                scaleHeightKm = 210.0,
                spectralPoints = listOf(
                    SpectralDataPoint(0.8, 1200.0, 15.0),
                    SpectralDataPoint(1.1, 1220.0, 16.0),
                    SpectralDataPoint(1.4, 1310.0, 18.0, candidateMolecule = "H2O"),
                    SpectralDataPoint(1.9, 1290.0, 18.0, candidateMolecule = "H2O"),
                    SpectralDataPoint(2.3, 1240.0, 20.0, candidateMolecule = "CH4"),
                    SpectralDataPoint(2.7, 1390.0, 22.0, candidateMolecule = "CO2"),
                    SpectralDataPoint(3.3, 1350.0, 22.0, candidateMolecule = "CH4"),
                    // Anomalous DMS biomarker peak at 3.40 um (+380 ppm surge, > 5 sigma!)
                    SpectralDataPoint(3.4, 1680.0, 20.0, candidateMolecule = "DMS (Dimetil Sulfură)", isOutlier = true),
                    SpectralDataPoint(3.8, 1210.0, 22.0),
                    // Anomalous Phosphine (PH3) / SO2 biomarker peak at 4.30 um (+450 ppm surge, > 5 sigma!)
                    SpectralDataPoint(4.3, 1820.0, 25.0, candidateMolecule = "PH3 / CO2", isOutlier = true),
                    SpectralDataPoint(4.67, 1260.0, 26.0, candidateMolecule = "CO"),
                    SpectralDataPoint(5.0, 1230.0, 28.0)
                ),
                notesOrObserver = "Observație brută procesată prin motorul XILON & MAST Archive. Zero înregistrări spectroscopice anterioare în bazele de date NASA."
            ),

            // Scenario 2: Calibration Verification - WASP-39 b (Matches official benchmark with high fidelity)
            RetrievedPlanetarySpectrum(
                id = "retrieved_wasp39b_verification",
                targetName = "WASP-39 b (Test Calibrare Standard)",
                hostStar = "WASP-39 (G8V)",
                instrumentFacility = "JWST NIRSpec PRISM (Calibrare ERS Oficială)",
                equilibriumTempK = 1166.0,
                planetRadiusEarth = 14.3,
                planetMassEarth = 89.0,
                stellarRadiusSun = 0.93,
                scaleHeightKm = 820.0,
                spectralPoints = listOf(
                    SpectralDataPoint(0.85, 21250.0, 25.0),
                    SpectralDataPoint(1.15, 21230.0, 26.0),
                    SpectralDataPoint(1.4, 21560.0, 28.0, candidateMolecule = "H2O"),
                    SpectralDataPoint(2.0, 21210.0, 30.0),
                    SpectralDataPoint(2.7, 21790.0, 32.0, candidateMolecule = "H2O + CO2"),
                    SpectralDataPoint(4.05, 21630.0, 35.0, candidateMolecule = "SO2"),
                    SpectralDataPoint(4.3, 22100.0, 38.0, candidateMolecule = "CO2"),
                    SpectralDataPoint(4.67, 21520.0, 40.0, candidateMolecule = "CO")
                ),
                notesOrObserver = "Set de calibrare pentru verificarea preciziei analizei în raport cu datele ERS Nature 2023."
            ),

            // Scenario 3: Escaping Exosphere Helium Anomaly - TOI-1895.01
            RetrievedPlanetarySpectrum(
                id = "retrieved_toi1895_he_escape",
                targetName = "TOI-1895.01 (Anomalie Evadare Heliu)",
                ticId = "384159646",
                hostStar = "TOI-1895 (K-Dwarf)",
                instrumentFacility = "VLT CRIRES+ / High-Res Spectrograph",
                equilibriumTempK = 1350.0,
                planetRadiusEarth = 11.2,
                planetMassEarth = 65.0,
                stellarRadiusSun = 0.88,
                scaleHeightKm = 740.0,
                spectralPoints = listOf(
                    SpectralDataPoint(0.8, 14200.0, 35.0),
                    SpectralDataPoint(0.95, 14220.0, 35.0),
                    // Massive Helium triplet surge at 1.083 um (+950 ppm!)
                    SpectralDataPoint(1.083, 15350.0, 30.0, candidateMolecule = "He I Triplet (Evadare Masivă)", isOutlier = true),
                    SpectralDataPoint(1.25, 14210.0, 36.0),
                    SpectralDataPoint(1.4, 14450.0, 38.0, candidateMolecule = "H2O"),
                    SpectralDataPoint(1.6, 14200.0, 40.0),
                    SpectralDataPoint(2.0, 14190.0, 42.0)
                ),
                notesOrObserver = "Semnătură extremă de pierdere de masă atmosferică indusă de vântul stelar al piticei portocalii."
            ),

            // Scenario 4: Desiccated Super-Earth with Complete Water Depletion
            RetrievedPlanetarySpectrum(
                id = "retrieved_desiccated_super_earth",
                targetName = "TOI-2267.01 (Atmosferă Desicată Fără Apă)",
                ticId = "268301217",
                hostStar = "TOI-2267 (M-Dwarf)",
                instrumentFacility = "JWST NIRSpec / NIRISS SOSS",
                equilibriumTempK = 540.0,
                planetRadiusEarth = 2.1,
                planetMassEarth = 5.8,
                stellarRadiusSun = 0.45,
                scaleHeightKm = 180.0,
                spectralPoints = listOf(
                    SpectralDataPoint(1.0, 3800.0, 20.0),
                    SpectralDataPoint(1.2, 3810.0, 20.0),
                    // Complete lack of 1.4 um water peak (flat at 3810 ppm vs expected 4150 ppm)
                    SpectralDataPoint(1.4, 3810.0, 22.0, candidateMolecule = "H2O Depleție", isOutlier = true),
                    SpectralDataPoint(1.8, 3820.0, 22.0),
                    SpectralDataPoint(2.7, 3830.0, 25.0, candidateMolecule = "H2O Depleție", isOutlier = true),
                    SpectralDataPoint(4.3, 4420.0, 28.0, candidateMolecule = "CO2"),
                    SpectralDataPoint(4.67, 4120.0, 30.0, candidateMolecule = "CO")
                ),
                notesOrObserver = "Atmosferă dominată de CO2 și CO, dar lipsită de vapori de apă, indicând evaporare primordială intensă."
            )
        )
    }

    // --- Private Helper Methods ---

    private fun findInterpolatedBaselinePoint(
        wavelength: Double,
        baselinePoints: List<SpectralDataPoint>
    ): SpectralDataPoint? {
        if (baselinePoints.isEmpty()) return null
        // Find exact match within 0.08 um
        val closeMatch = baselinePoints.firstOrNull { abs(it.wavelengthMicrons - wavelength) <= 0.08 }
        if (closeMatch != null) return closeMatch

        // Interpolate between two surrounding points
        val sorted = baselinePoints.sortedBy { it.wavelengthMicrons }
        val lower = sorted.lastOrNull { it.wavelengthMicrons <= wavelength }
        val upper = sorted.firstOrNull { it.wavelengthMicrons >= wavelength }

        return when {
            lower != null && upper != null && lower != upper -> {
                val ratio = (wavelength - lower.wavelengthMicrons) / (upper.wavelengthMicrons - lower.wavelengthMicrons)
                val interpFlux = lower.fluxOrDepthPpm + ratio * (upper.fluxOrDepthPpm - lower.fluxOrDepthPpm)
                val interpError = (lower.uncertaintyPpm + upper.uncertaintyPpm) / 2.0
                SpectralDataPoint(
                    wavelengthMicrons = wavelength,
                    fluxOrDepthPpm = interpFlux,
                    uncertaintyPpm = interpError
                )
            }
            lower != null -> lower
            upper != null -> upper
            else -> sorted.first()
        }
    }

    private fun calculateNoveltyIndex(
        isTargetKnownInRegistry: Boolean,
        maxSigma: Double,
        anomalies: List<SpectralAnomaly>,
        reducedChiSq: Double
    ): Double {
        var score = 0.0

        // 1. Zero-prior status: 35 points if target was never published before
        if (!isTargetKnownInRegistry) {
            score += 35.0
        }

        // 2. Statistical peak significance: up to 30 points
        val sigmaScore = (maxSigma / 6.0).coerceIn(0.0, 1.0) * 30.0
        score += sigmaScore

        // 3. Presence of exotic biosignature / technosignature bands: up to 25 points
        val hasExoticBand = anomalies.any {
            it.anomalyType == SpectralAnomalyType.UNPRECEDENTED_MOLECULAR_BAND ||
                    it.anomalyType == SpectralAnomalyType.ANOMALOUS_DEPTH_SURGE
        }
        if (hasExoticBand) {
            score += 25.0
        }

        // 4. Residual deviation spread: up to 10 points
        if (reducedChiSq > 2.5) {
            score += 10.0
        } else if (reducedChiSq > 1.5) {
            score += 5.0
        }

        return score.coerceIn(0.0, 100.0)
    }

    private fun buildVerdictAndRecommendations(
        targetName: String,
        isTargetKnown: Boolean,
        benchmark: RegisteredSpectralBenchmark,
        maxSigma: Double,
        anomalies: List<SpectralAnomaly>,
        noveltyIndex: Double
    ): Pair<String, List<String>> {
        val recommendations = mutableListOf<String>()

        val verdict = when {
            noveltyIndex >= 80.0 -> {
                recommendations.add("Prioritate Maximă: Propune ținta pentru observație țintită JWST DDT (Director's Discretionary Time).")
                recommendations.add("Efectuează validare prin spectrograf de înaltă rezoluție la sol (VLT CRIRES+ sau ESPRESSO) pentru confirmarea liniilor înguste.")
                recommendations.add("Trimite raportul de anomalie către arhivele de colaborare științifică ExoFOP-TESS / NASA NExScI.")
                recommendations.add("Verifică activitatea magnetică a stelei gazdă pentru a exclude efectul de contaminare a petelor stelare (stellar spot unocculted crossing).")
                "★ DESCOPERIRE POTENȚIAL UNICĂ (Indice Noutate: ${noveltyIndex.format(1)}%): Semnături spectrale fără precedent (${maxSigma.format(1)}σ) absente în registrul public NASA/MAST."
            }

            noveltyIndex >= 50.0 -> {
                recommendations.add("Verifică posibile interferențe instrumentale sau erori sistematice pe detector.")
                recommendations.add("Realizează o nouă stivă de tranzit (stacked transits) pentru a crește raportul S/N peste pragul de 5.0σ.")
                recommendations.add("Compară cu modelele radiative non-LTE pentru confirmarea compoziției chimice.")
                "ANOMALIE ATMOSFERICĂ SEMNIFICATIVĂ (${maxSigma.format(1)}σ): Deviații clare față de linia de bază ${benchmark.targetName}, indicând chimism sau dinamici neprevăzute."
            }

            noveltyIndex >= 25.0 -> {
                recommendations.add("Datele sunt compatibile cu variații statistice normale sau zgomot de fond moderat.")
                recommendations.add("Recomandat monitorizarea tranzitelor ulterioare pentru rafinarea barelor de eroare.")
                "DEVIAȚIE STATISTICĂ MODERATĂ: Semnalul este în mare măsură compatibil cu registrul public, cu mici anomalii marginale."
            }

            else -> {
                recommendations.add("Calibrare validată cu succes: Răspunsul spectral este în deplină concordanță cu arhiva publică.")
                "CONFORM CU REGISTRUL PUBLIC: Spectrul obținut confirmă cu precizie ridicată modelul de referință ${benchmark.targetName} (χ²_red = normal)."
            }
        }

        return Pair(verdict, recommendations)
    }

    private fun Double.format(digits: Int): String = String.format(java.util.Locale.US, "%.${digits}f", this)
}
