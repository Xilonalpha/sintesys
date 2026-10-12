package com.synthesis.data.repository.exoplanet

import com.synthesis.domain.model.exoplanet.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Registry of public exoplanet spectral datasets, officially published space telescope
 * benchmarks (JWST, HST, Spitzer), and standard theoretical chemical equilibrium models.
 */
@Singleton
class PublicSpectralDatasetRegistry @Inject constructor() {

    data class MolecularBandReference(
        val moleculeFormula: String,
        val moleculeName: String,
        val centralWavelengthMicrons: Double,
        val bandwidthMicrons: Double,
        val isBiosignatureCandidate: Boolean = false,
        val isTechnosignatureCandidate: Boolean = false,
        val description: String
    )

    private val molecularBandCatalog: List<MolecularBandReference> = listOf(
        // Standard atmospheric carriers
        MolecularBandReference("H2O", "Vapori de Apă", 1.40, 0.12, description = "Bandă vibrațională H2O (NIR)"),
        MolecularBandReference("H2O", "Vapori de Apă", 1.90, 0.15, description = "Bandă de absorbție H2O"),
        MolecularBandReference("H2O", "Vapori de Apă", 2.70, 0.20, description = "Bandă fundamentală combinată H2O"),
        MolecularBandReference("H2O", "Vapori de Apă", 6.30, 0.40, description = "Bandă fundamentală de încovoiere H2O"),

        MolecularBandReference("CO2", "Dioxid de Carbon", 2.00, 0.10, description = "Supraton CO2"),
        MolecularBandReference("CO2", "Dioxid de Carbon", 2.70, 0.15, description = "Bandă combinată Fermi CO2"),
        MolecularBandReference("CO2", "Dioxid de Carbon", 4.30, 0.20, description = "Bandă fundamentală de întindere asimetrică CO2"),
        MolecularBandReference("CO2", "Dioxid de Carbon", 15.0, 1.00, description = "Bandă de încovoiere CO2 în infraroșu termic"),

        MolecularBandReference("CH4", "Metan", 1.66, 0.10, description = "Bandă de absorbție metan"),
        MolecularBandReference("CH4", "Metan", 2.30, 0.15, description = "Bandă combinată CH4"),
        MolecularBandReference("CH4", "Metan", 3.30, 0.20, description = "Bandă fundamentală de întindere C-H"),
        MolecularBandReference("CH4", "Metan", 7.70, 0.40, description = "Bandă de deformare CH4"),

        MolecularBandReference("CO", "Monoxid de Carbon", 2.35, 0.12, description = "Primul supraton CO"),
        MolecularBandReference("CO", "Monoxid de Carbon", 4.67, 0.18, description = "Bandă fundamentală 1-0 CO"),

        MolecularBandReference("SO2", "Dioxid de Sulf", 4.05, 0.12, description = "Semnătură de fotochimie indusă de raze UV stelare"),
        MolecularBandReference("SO2", "Dioxid de Sulf", 7.35, 0.30, description = "Bandă asimetrică SO2"),
        MolecularBandReference("SO2", "Dioxid de Sulf", 8.70, 0.35, description = "Bandă simetrică SO2"),

        MolecularBandReference("NH3", "Amoniac", 1.50, 0.10, description = "Bandă combinată NH3"),
        MolecularBandReference("NH3", "Amoniac", 3.00, 0.20, description = "Bandă fundamentală N-H"),
        MolecularBandReference("NH3", "Amoniac", 10.5, 0.60, description = "Bandă de inversie cuantică NH3"),

        MolecularBandReference("Na I", "Sodiu Neutru (Dublet D)", 0.589, 0.03, description = "Linie rezonantă atomică Na I"),
        MolecularBandReference("K I", "Potasiu Neutru", 0.767, 0.04, description = "Dublet atomic K I"),
        MolecularBandReference("He I", "Heliu Triplet Meta-stabil", 1.083, 0.03, description = "Indicator de evadare atmosferică hidro-dinamică"),

        // Candidate Biosignatures
        MolecularBandReference("PH3", "Fosfină", 4.30, 0.15, isBiosignatureCandidate = true, description = "Potențial biomarker anaerob (reducere biologică extremă)"),
        MolecularBandReference("PH3", "Fosfină", 8.90, 0.40, isBiosignatureCandidate = true, description = "Bandă de încovoiere fosfină"),
        MolecularBandReference("DMS", "Dimetil Sulfură (CH3SCH3)", 3.40, 0.18, isBiosignatureCandidate = true, description = "Biomarker marin oceanic / fitoplancton analog"),
        MolecularBandReference("DMS", "Dimetil Sulfură (CH3SCH3)", 9.70, 0.45, isBiosignatureCandidate = true, description = "Bandă infraroșie DMS"),
        MolecularBandReference("O3", "Ozon", 9.60, 0.50, isBiosignatureCandidate = true, description = "Fotoprodus al oxigenului biogen (O2)"),
        MolecularBandReference("N2O", "Oxid Nitos", 4.50, 0.15, isBiosignatureCandidate = true, description = "Subprodus al ciclului azotului bacterian"),
        MolecularBandReference("CH3Cl", "Clorometan", 3.35, 0.15, isBiosignatureCandidate = true, description = "Halogenură volatilă de origine biologică"),

        // Candidate Technosignatures
        MolecularBandReference("CFC-11", "Triclorofluorometan", 11.8, 0.60, isTechnosignatureCandidate = true, description = "Compus artificial industrial (fără surse geochimice naturale)"),
        MolecularBandReference("CFC-12", "Diclorodifluorometan", 9.10, 0.45, isTechnosignatureCandidate = true, description = "Poluant tehnologic refrigerant artificial"),
        MolecularBandReference("SF6", "Hexafluorură de Sulf", 10.55, 0.40, isTechnosignatureCandidate = true, description = "Gaz dielectric industrial cu stabilitate extremă")
    )

    private val publicBenchmarks: List<RegisteredSpectralBenchmark> = listOf(
        // 1. WASP-39 b (The JWST ERS Golden Benchmark)
        RegisteredSpectralBenchmark(
            benchmarkId = "NASA-JWST-WASP-39b-PRISM",
            targetName = "WASP-39 b",
            benchmarkType = SpectralBenchmarkType.PUBLISHED_SPACE_TELESCOPE,
            facilityOrPaper = "JWST Transiting Exoplanet Community Early Release Science (Nature 2023)",
            publicationDoi = "10.1038/s41586-022-05269-w",
            equilibriumTempK = 1166.0,
            verifiedMolecules = listOf("H2O", "CO2", "SO2", "CO", "Na I", "K I"),
            spectrumPoints = generateWasp39bReference(),
            description = "Primul spectru complet 0.5-5.5 μm obținut cu JWST NIRSpec PRISM; a demonstrat pentru prima dată fotochimia dioxidului de sulf (SO2 la 4.05 μm) și absorbția de dioxid de carbon (CO2 la 4.3 μm)."
        ),

        // 2. K2-18 b (Sub-Neptune / Hycean World Candidate)
        RegisteredSpectralBenchmark(
            benchmarkId = "NASA-JWST-K2-18b-NIRSPEC",
            targetName = "K2-18 b",
            benchmarkType = SpectralBenchmarkType.PUBLISHED_SPACE_TELESCOPE,
            facilityOrPaper = "Madhusudhan et al. 2023 (ApJL 956:L13)",
            publicationDoi = "10.3847/2041-8213/acf577",
            equilibriumTempK = 255.0,
            verifiedMolecules = listOf("CH4", "CO2"),
            spectrumPoints = generateK218bReference(),
            description = "Planeta candidat Hycean cu atmosferă bogată în metan și dioxid de carbon, cu absență marcantă de amoniac (NH3) și indicii tentative de dimetil sulfură (DMS)."
        ),

        // 3. WASP-96 b (Cloud-free Water Vapor Giant)
        RegisteredSpectralBenchmark(
            benchmarkId = "NASA-JWST-WASP-96b-NIRISS",
            targetName = "WASP-96 b",
            benchmarkType = SpectralBenchmarkType.PUBLISHED_SPACE_TELESCOPE,
            facilityOrPaper = "JWST NIRISS SOSS Commissioning Release (NASA ERS 2022)",
            publicationDoi = "10.17909/exoplanet-wasp96b",
            equilibriumTempK = 1285.0,
            verifiedMolecules = listOf("H2O", "Na I", "K I"),
            spectrumPoints = generateWasp96bReference(),
            description = "Gigant gazos cald cu profil clasic de vapori de apă fără estompare semnificativă de la nori în banda 0.8-2.8 μm."
        ),

        // 4. HD 209458 b (Classic Historical Hot Jupiter)
        RegisteredSpectralBenchmark(
            benchmarkId = "NASA-HST-HD209458b-STIS-WFC3",
            targetName = "HD 209458 b (Osiris)",
            benchmarkType = SpectralBenchmarkType.PUBLISHED_SPACE_TELESCOPE,
            facilityOrPaper = "Sing et al. 2016 / Vidal-Madjar et al. 2003",
            publicationDoi = "10.1038/nature16068",
            equilibriumTempK = 1450.0,
            verifiedMolecules = listOf("Na I", "H2O", "He I"),
            spectrumPoints = generateHd209458bReference(),
            description = "Benchmark istoric Hubble STIS / WFC3; detectarea inițială a sodiului la 0.589 μm și a atmosferei extinse în evadare (linia de heliu metastabil 1.083 μm)."
        ),

        // 5. TRAPPIST-1 e (Temperate Terrestrial Secondary Atmosphere Target)
        RegisteredSpectralBenchmark(
            benchmarkId = "NASA-JWST-TRAPPIST-1e-MIRI",
            targetName = "TRAPPIST-1 e",
            benchmarkType = SpectralBenchmarkType.PUBLISHED_SPACE_TELESCOPE,
            facilityOrPaper = "Gillon et al. / JWST GTO Cycle 1-3 Atmospheric Constraints",
            publicationDoi = "10.1038/nature21360",
            equilibriumTempK = 251.0,
            verifiedMolecules = listOf("CO2"),
            spectrumPoints = generateTrappist1eReference(),
            description = "Planetă terestră din zona locuibilă în jurul unei pitice roșii M8V. Baseline teoretic restrâns prin observații MIRI LRS și NIRSpec."
        ),

        // 6. GJ 1214 b (High-Altitude Photochemical Haze Benchmark)
        RegisteredSpectralBenchmark(
            benchmarkId = "NASA-JWST-GJ-1214b-MIRI",
            targetName = "GJ 1214 b",
            benchmarkType = SpectralBenchmarkType.HIGH_ALTITUDE_HAZE_DECK,
            facilityOrPaper = "Kempton et al. 2023 (Nature 620:67-72)",
            publicationDoi = "10.1038/s41586-023-06159-5",
            equilibriumTempK = 596.0,
            verifiedMolecules = emptyList(), // Obscured by thick aerosols
            spectrumPoints = generateGj1214bReference(),
            description = "Sub-Neptunian cu o pătură masivă de aerosoli și cețuri dense la mare altitudine, care produce o curbă de transmisie aproape plată și o reflectanță ridicată."
        ),

        // 7. Theoretical Solar Equilibrium Chemistry Baseline (1x Solar Metallicity)
        RegisteredSpectralBenchmark(
            benchmarkId = "THEORETICAL-SOLAR-EQ-1X",
            targetName = "Model Teoretic Echilibru Solar (1x)",
            benchmarkType = SpectralBenchmarkType.THEORETICAL_SOLAR_EQUILIBRIUM,
            facilityOrPaper = "Exo-Transmit Grid (Kempton et al. 2017)",
            publicationDoi = "10.1088/1538-3873/aa61ef",
            equilibriumTempK = 1000.0,
            verifiedMolecules = listOf("H2O", "CO", "CH4", "CO2"),
            spectrumPoints = generateTheoreticalSolarEqReference(),
            description = "Model standard de chimie în echilibru pentru o compoziție de hidrogen/heliu cu metalicitate solară la 1000 K."
        ),

        // 8. Theoretical Super-Solar High-Metallicity (100x Solar)
        RegisteredSpectralBenchmark(
            benchmarkId = "THEORETICAL-SUPER-SOLAR-100X",
            targetName = "Model Teoretic Super-Solar (100x)",
            benchmarkType = SpectralBenchmarkType.SUPER_SOLAR_ENRICHED,
            facilityOrPaper = "Fortney & Marley Grid (ApJ)",
            publicationDoi = "10.1086/589942",
            equilibriumTempK = 800.0,
            verifiedMolecules = listOf("CO2", "H2O", "CO"),
            spectrumPoints = generateTheoreticalSuperSolarReference(),
            description = "Atmosferă puternic îmbogățită în elemente grele, tipică planetelor sub-Neptun și mini-Neptun volatile."
        )
    )

    fun getAllBenchmarks(): List<RegisteredSpectralBenchmark> = publicBenchmarks

    fun findBenchmarkById(benchmarkId: String): RegisteredSpectralBenchmark? =
        publicBenchmarks.firstOrNull { it.benchmarkId.equals(benchmarkId, ignoreCase = true) }

    fun findBenchmarkByTargetName(name: String): RegisteredSpectralBenchmark? =
        publicBenchmarks.firstOrNull {
            it.targetName.contains(name, ignoreCase = true) || name.contains(it.targetName, ignoreCase = true)
        }

    /**
     * Finds the closest baseline theoretical or empirical model based on planet physical parameters.
     */
    fun findBestMatchingBaseline(tempK: Double, radiusEarth: Double): RegisteredSpectralBenchmark {
        return when {
            radiusEarth <= 1.5 -> findBenchmarkById("NASA-JWST-TRAPPIST-1e-MIRI") ?: publicBenchmarks.first()
            radiusEarth in 1.5..3.5 && tempK < 400.0 -> findBenchmarkById("NASA-JWST-K2-18b-NIRSPEC") ?: publicBenchmarks.first()
            radiusEarth in 1.5..3.5 && tempK >= 400.0 -> findBenchmarkById("THEORETICAL-SUPER-SOLAR-100X") ?: publicBenchmarks.first()
            tempK > 1100.0 -> findBenchmarkById("NASA-JWST-WASP-39b-PRISM") ?: publicBenchmarks.first()
            else -> findBenchmarkById("THEORETICAL-SOLAR-EQ-1X") ?: publicBenchmarks.first()
        }
    }

    /**
     * Cross-checks a given wavelength against known molecular bands to list potential carriers.
     */
    fun identifyMoleculesAtWavelength(wavelengthMicrons: Double, toleranceFactor: Double = 1.0): List<MolecularBandReference> {
        return molecularBandCatalog.filter { band ->
            val effectiveBandwidth = band.bandwidthMicrons * toleranceFactor
            abs(wavelengthMicrons - band.centralWavelengthMicrons) <= (effectiveBandwidth / 2.0)
        }
    }

    fun isExoticOrBiosignatureMolecule(candidate: String): Boolean {
        val upper = candidate.uppercase()
        return molecularBandCatalog.any {
            (it.isBiosignatureCandidate || it.isTechnosignatureCandidate || it.moleculeFormula == "SO2") &&
                    (upper.contains(it.moleculeFormula) || upper.contains(it.moleculeName.uppercase()))
        }
    }

    // --- Synthetic Reference Spectra Generators based on Published Data ---

    private fun generateWasp39bReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(
            0.6, 0.75, 0.85, 1.0, 1.15, 1.25, 1.4, 1.6, 1.85, 2.0, 2.3, 2.7,
            3.0, 3.4, 3.8, 4.05, 4.3, 4.67, 5.0, 5.3
        )
        val baseDepth = 21200.0 // ppm (~2.12%)
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 1.4) < 0.1 -> 350.0 // H2O
                abs(wl - 2.7) < 0.15 -> 580.0 // H2O + CO2
                abs(wl - 4.05) < 0.08 -> 420.0 // SO2 photochemistry
                abs(wl - 4.3) < 0.12 -> 890.0 // CO2 fundamental
                abs(wl - 4.67) < 0.1 -> 310.0 // CO
                wl < 0.9 -> 180.0 * (1.0 / wl) // Rayleigh slope
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 25.0 + (wl * 4.0),
                candidateMolecule = when {
                    abs(wl - 4.05) < 0.08 -> "SO2"
                    abs(wl - 4.3) < 0.12 -> "CO2"
                    abs(wl - 1.4) < 0.1 || abs(wl - 2.7) < 0.15 -> "H2O"
                    abs(wl - 4.67) < 0.1 -> "CO"
                    else -> null
                }
            )
        }
    }

    private fun generateK218bReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(
            0.9, 1.15, 1.4, 1.66, 1.9, 2.3, 2.7, 3.0, 3.3, 3.4, 3.8, 4.3, 4.8, 5.0
        )
        val baseDepth = 3050.0 // ppm
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 1.66) < 0.08 || abs(wl - 2.3) < 0.1 || abs(wl - 3.3) < 0.12 -> 220.0 // CH4
                abs(wl - 4.3) < 0.12 -> 310.0 // CO2
                abs(wl - 3.4) < 0.08 -> 80.0 // DMS tentative
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 30.0,
                candidateMolecule = when {
                    abs(wl - 4.3) < 0.12 -> "CO2"
                    abs(wl - 2.3) < 0.1 || abs(wl - 3.3) < 0.12 -> "CH4"
                    abs(wl - 3.4) < 0.08 -> "DMS?"
                    else -> null
                }
            )
        }
    }

    private fun generateWasp96bReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(0.8, 1.0, 1.15, 1.4, 1.6, 1.9, 2.1, 2.4, 2.7)
        val baseDepth = 15400.0
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 1.15) < 0.08 -> 180.0
                abs(wl - 1.4) < 0.1 -> 340.0
                abs(wl - 1.9) < 0.1 -> 290.0
                abs(wl - 2.7) < 0.15 -> 410.0
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 35.0,
                candidateMolecule = if (modulation > 50.0) "H2O" else null
            )
        }
    }

    private fun generateHd209458bReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(0.55, 0.589, 0.7, 0.767, 1.0, 1.083, 1.25, 1.4, 1.6, 1.9, 2.2, 3.6, 4.5)
        val baseDepth = 14600.0
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 0.589) < 0.02 -> 310.0 // Na I line
                abs(wl - 0.767) < 0.02 -> 160.0 // K I
                abs(wl - 1.083) < 0.02 -> 450.0 // He I escaping exosphere
                abs(wl - 1.4) < 0.1 -> 240.0 // H2O
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 40.0,
                candidateMolecule = when {
                    abs(wl - 0.589) < 0.02 -> "Na I"
                    abs(wl - 1.083) < 0.02 -> "He I (Exosphere)"
                    abs(wl - 1.4) < 0.1 -> "H2O"
                    else -> null
                }
            )
        }
    }

    private fun generateTrappist1eReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(2.0, 2.7, 3.5, 4.3, 4.8, 6.0, 7.5, 9.6, 11.0, 15.0)
        val baseDepth = 480.0 // ppm for Earth-sized planet around M-dwarf
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 4.3) < 0.15 -> 95.0 // CO2
                abs(wl - 15.0) < 0.5 -> 120.0 // CO2 15 um
                abs(wl - 9.6) < 0.3 -> 35.0 // Ozone tentative
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 18.0,
                candidateMolecule = when {
                    abs(wl - 4.3) < 0.15 || abs(wl - 15.0) < 0.5 -> "CO2"
                    abs(wl - 9.6) < 0.3 -> "O3"
                    else -> null
                }
            )
        }
    }

    private fun generateGj1214bReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(1.0, 1.4, 1.8, 2.2, 2.7, 3.3, 4.0, 4.5, 5.0, 7.0, 9.0, 11.0)
        val baseDepth = 13500.0
        return wlGrid.map { wl ->
            // Almost completely flat line with minute 10-20 ppm variation due to dense photochemical tholin hazes
            val modulation = (kotlin.math.sin(wl * 1.5) * 12.0)
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 20.0,
                candidateMolecule = "Aerosol / Tholin Haze"
            )
        }
    }

    private fun generateTheoreticalSolarEqReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.3, 2.7, 3.3, 4.0, 4.3, 4.67, 5.0)
        val baseDepth = 10000.0
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 1.4) < 0.1 -> 250.0 // H2O
                abs(wl - 1.9) < 0.1 -> 220.0 // H2O
                abs(wl - 2.3) < 0.1 -> 180.0 // CH4/CO
                abs(wl - 2.7) < 0.15 -> 320.0 // H2O
                abs(wl - 4.3) < 0.15 -> 390.0 // CO2
                abs(wl - 4.67) < 0.1 -> 210.0 // CO
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 20.0,
                candidateMolecule = when {
                    abs(wl - 1.4) < 0.1 || abs(wl - 1.9) < 0.1 || abs(wl - 2.7) < 0.15 -> "H2O"
                    abs(wl - 4.3) < 0.15 -> "CO2"
                    abs(wl - 4.67) < 0.1 -> "CO"
                    else -> null
                }
            )
        }
    }

    private fun generateTheoreticalSuperSolarReference(): List<SpectralDataPoint> {
        val wlGrid = doubleArrayOf(0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.3, 2.7, 3.3, 4.0, 4.3, 4.67, 5.0)
        val baseDepth = 8000.0
        return wlGrid.map { wl ->
            val modulation = when {
                abs(wl - 1.4) < 0.1 -> 180.0
                abs(wl - 2.7) < 0.15 -> 340.0
                abs(wl - 4.3) < 0.15 -> 580.0 // Prominent CO2 in high-Z atmospheres
                abs(wl - 4.67) < 0.1 -> 290.0 // CO
                else -> 0.0
            }
            SpectralDataPoint(
                wavelengthMicrons = wl,
                fluxOrDepthPpm = baseDepth + modulation,
                uncertaintyPpm = 25.0,
                candidateMolecule = if (abs(wl - 4.3) < 0.15) "CO2 (100x Solar)" else "H2O/CO"
            )
        }
    }
}
