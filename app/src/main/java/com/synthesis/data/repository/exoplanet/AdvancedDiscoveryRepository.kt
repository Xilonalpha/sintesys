package com.synthesis.data.repository.exoplanet

import com.synthesis.domain.model.exoplanet.*
import com.synthesis.engine.exoplanet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedDiscoveryRepository @Inject constructor(
    private val biosignatureEngine: BiosignatureTechnosignatureEngine,
    private val dilutionEngine: DilutionDeBlenderEngine,
    private val harmonicStitcherEngine: MultiSectorHarmonicStitcherEngine,
    private val climateEngine: EnergyBalanceClimateEngine,
    private val skyRadarEngine: TransitSkyRadarEngine,
    private val platoEngine: PlatoAsteroseismologyEngine,
    private val jwstEngine: JwstTransmissionSpectroscopyEngine,
    private val exomoonEngine: ExomoonTidalOceanEngine,
    private val spaceWeatherEngine: SpaceWeatherMagnetosphereEngine
) {

    fun getCuratedPlatoAsteroseismicTargets(): List<PlatoAsteroseismicResult> {
        return listOf(
            platoEngine.solveStellarAndPlanetaryProperties(
                starName = "HD 219134 (PLATO Bright Solar Analogue Benchmark)",
                ticId = "300015239",
                nuMaxMicroHz = 3280.0,
                deltaNuMicroHz = 141.2,
                teffK = 5740.0,
                metallicityFeH = 0.08,
                observedTransitDepthPpm = 295.0
            ),
            platoEngine.solveStellarAndPlanetaryProperties(
                starName = "TOI-197 / TIC 441462736 (Oscillating Subgiant Planet Host)",
                ticId = "441462736",
                nuMaxMicroHz = 430.0,
                deltaNuMicroHz = 28.5,
                teffK = 5080.0,
                metallicityFeH = -0.05,
                observedTransitDepthPpm = 1120.0
            )
        )
    }

    fun getCuratedJwstSpectra(): List<JwstTransmissionSpectrumResult> {
        return listOf(
            jwstEngine.synthesizeJwstSpectrum(
                planetName = "WASP-39 b (JWST Flagship NIRSpec Benchmark)",
                hostStar = "WASP-39 (G8V)",
                planetRadiusEarth = 14.3,
                planetMassEarth = 89.0, // ~ 0.28 M_Jup
                stellarRadiusSun = 0.93,
                equilibriumTempK = 1166.0,
                stellarTeffK = 5485.0,
                jMagnitude = 10.6,
                kMagnitude = 10.2,
                hasPhotochemicalSo2 = true // Iconic 4.05 um discovery!
            ),
            jwstEngine.synthesizeJwstSpectrum(
                planetName = "TRAPPIST-1 e (JWST Cycle 4 MIRI Terrestrial Search)",
                hostStar = "TRAPPIST-1 (M8V)",
                planetRadiusEarth = 0.92,
                planetMassEarth = 0.69,
                stellarRadiusSun = 0.119,
                equilibriumTempK = 251.0,
                stellarTeffK = 2566.0,
                jMagnitude = 11.3,
                kMagnitude = 10.3,
                hasPhotochemicalSo2 = false
            )
        )
    }

    fun getCuratedExomoons(): List<ExomoonTidalOceanResult> {
        return listOf(
            exomoonEngine.analyzeExomoonAndTidalOcean(
                moonName = "Kepler-1625 b-I (First Candidate Exomoon)",
                hostPlanet = "Kepler-1625 b (Jovian Gas Giant)",
                hostStar = "Kepler-1625 (G0V)",
                planetMassJupiter = 3.0,
                stellarMassSun = 1.08,
                planetSemiMajorAxisAu = 0.87,
                moonMassEarth = 10.2, // Neptune-mass moon candidate
                moonRadiusEarth = 3.8,
                semiMajorAxisMoonKm = 3_000_000.0,
                orbitalEccentricity = 0.05,
                surfaceTemperatureK = 240.0
            ),
            exomoonEngine.analyzeExomoonAndTidalOcean(
                moonName = "Kepler-1708 b-I (Temperate Tidal Ocean Moon)",
                hostPlanet = "Kepler-1708 b (Cool Gas Giant)",
                hostStar = "Kepler-1708 (F8V)",
                planetMassJupiter = 2.0,
                stellarMassSun = 1.15,
                planetSemiMajorAxisAu = 1.64,
                moonMassEarth = 1.0, // Earth-sized exomoon
                moonRadiusEarth = 1.0,
                semiMajorAxisMoonKm = 1_450_000.0,
                orbitalEccentricity = 0.012,
                surfaceTemperatureK = 190.0
            )
        )
    }

    fun getCuratedSpaceWeatherAssessments(): List<MagnetosphereShieldingResult> {
        return listOf(
            spaceWeatherEngine.evaluateMagnetosphereAndShielding(
                planetName = "TRAPPIST-1 e (M-Dwarf High-XUV Space Weather)",
                hostStar = "TRAPPIST-1 (Ultra-Cool Dwarf)",
                semiMajorAxisAu = 0.029,
                planetMassEarth = 0.69,
                planetRadiusEarth = 0.92,
                rotationPeriodHours = 146.0, // Tidally locked
                coreMassFraction = 0.38,
                stellarWindVelocityKms = 600.0,
                stellarWindDensityProtonsCm3 = 2500.0,
                stellarXuvFluxEarthUnits = 120.0
            ),
            spaceWeatherEngine.evaluateMagnetosphereAndShielding(
                planetName = "Proxima Centauri b (Habitable Zone Solar Flare Test)",
                hostStar = "Proxima Centauri (M5.5V)",
                semiMajorAxisAu = 0.048,
                planetMassEarth = 1.17,
                planetRadiusEarth = 1.07,
                rotationPeriodHours = 268.0,
                coreMassFraction = 0.35,
                stellarWindVelocityKms = 550.0,
                stellarWindDensityProtonsCm3 = 1800.0,
                stellarXuvFluxEarthUnits = 85.0
            )
        )
    }

    fun getCuratedBiosignatures(): List<BiosignatureAssessment> {
        val k2_18bMolecules = listOf(
            AtmosphericMolecule("CH4", "Methane", 12000.0, 5.2, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("CO2", "Carbon Dioxide", 15000.0, 4.8, isBiosignatureCandidate = false, isTechnosignatureCandidate = false),
            AtmosphericMolecule("DMS", "Dimethyl Sulfide ((CH3)2S)", 25.0, 2.9, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("H2O", "Water Vapor", 350.0, 3.1, isBiosignatureCandidate = false, isTechnosignatureCandidate = false)
        )

        val trappist1eMolecules = listOf(
            AtmosphericMolecule("O2", "Molecular Oxygen", 210000.0, 3.8, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("O3", "Ozone", 12.0, 4.1, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("CH4", "Methane", 18.0, 3.6, isBiosignatureCandidate = true, isTechnosignatureCandidate = false),
            AtmosphericMolecule("H2O", "Water Vapor", 4500.0, 4.5, isBiosignatureCandidate = false, isTechnosignatureCandidate = false),
            AtmosphericMolecule("CO2", "Carbon Dioxide", 420.0, 6.0, isBiosignatureCandidate = false, isTechnosignatureCandidate = false)
        )

        return listOf(
            biosignatureEngine.evaluateBiosignatures(
                planetName = "TRAPPIST-1 e (Temperate Terrestrial Bio-Candidate)",
                hostStar = "TRAPPIST-1 (M8V)",
                equilibriumTempK = 251.0,
                molecules = trappist1eMolecules
            ),
            biosignatureEngine.evaluateBiosignatures(
                planetName = "K2-18 b (Hycean Ocean Candidate - JWST Detection)",
                hostStar = "K2-18 (M2.5V)",
                equilibriumTempK = 279.0,
                molecules = k2_18bMolecules
            )
        )
    }

    fun getCuratedTechnosignatures(): List<TechnosignatureCandidate> {
        return listOf(
            biosignatureEngine.evaluateTechnosignatures(
                candidateName = "TIC 400499414 (Boyajian Analogue - Jagged Ingress)",
                hostStar = "KIC 8462852 Twin (F3V)",
                ingressDurationHours = 18.4,
                egressDurationHours = 6.2,
                depthVariancePpm = 22000.0,
                cfcPollutionSigma = null
            ),
            biosignatureEngine.evaluateTechnosignatures(
                candidateName = "TOI-5612.01 (Industrial CFC Pollution Signature)",
                hostStar = "HD 101805 (G2V)",
                ingressDurationHours = 2.4,
                egressDurationHours = 2.6,
                depthVariancePpm = 180.0,
                cfcPollutionSigma = 3.6
            )
        )
    }

    fun getCuratedDilutionRescues(): List<DilutionRescueCandidate> {
        return listOf(
            dilutionEngine.deblendCandidate(
                candidateName = "TOI-1785.01",
                ticId = "300015239",
                originalNasaStatus = "False Positive (FP - Grazing Binary)",
                observedTransitDepthPpm = 340.0,
                targetStarMagGaia = 10.45,
                blendingStarMagGaia = 10.82,
                angularSeparationArcsec = 4.2,
                stellarRadiusSun = 0.88
            ),
            dilutionEngine.deblendCandidate(
                candidateName = "TOI-2341.01",
                ticId = "415982311",
                originalNasaStatus = "False Positive (FP - Stellar Blend)",
                observedTransitDepthPpm = 490.0,
                targetStarMagGaia = 11.20,
                blendingStarMagGaia = 9.95,
                angularSeparationArcsec = 7.8,
                stellarRadiusSun = 1.02
            )
        )
    }

    fun getCuratedHarmonicStitches(): List<HarmonicStitchCandidate> {
        return listOf(
            harmonicStitcherEngine.stitchHarmonics(
                candidateName = "TOI-4633.02 (Multi-Year Year 1 vs Year 3 Bridge)",
                ticId = "100100827",
                epoch1Btjd = 1683.15,
                sector1 = "s0014",
                epoch2Btjd = 2496.15,
                sector2 = "s0044",
                stellarMassSun = 1.02,
                stellarRadiusSun = 1.05,
                stellarTeffK = 5790.0,
                monitoredGaps = listOf(Pair(1900.0, 1927.0), Pair(2150.0, 2177.0))
            )
        )
    }

    fun simulateClimate(
        planetName: String,
        insolation: Double,
        co2Bar: Double,
        pressureBar: Double
    ): ClimateSimulationResult {
        return climateEngine.simulateClimate(
            planetName = planetName,
            stellarInsolationSolar = insolation,
            co2LevelBar = co2Bar,
            surfacePressureBar = pressureBar
        )
    }

    private fun getCurrentBtjd(): Double {
        val nowMs = System.currentTimeMillis()
        val jd = (nowMs / 86400000.0) + 2440587.5
        return jd - 2457000.0
    }

    fun getCuratedLiveTransits(): List<LiveTransitTarget> {
        val currentBtjd = getCurrentBtjd()
        return listOf(
            skyRadarEngine.computeNextTransit(
                candidateName = "TOI-700 e (Habitable Zone Terrestrial)",
                ticId = "150428135",
                constellation = "Dorado",
                rightAscension = "06h 28m 23s",
                declination = "-55° 34′ 43″",
                apparentVisualMag = 13.1,
                periodDays = 27.81,
                transitDurationHours = 2.4,
                epochBtjd = 1500.24,
                transitDepthPpm = 860.0,
                currentBtjd = currentBtjd
            ),
            skyRadarEngine.computeNextTransit(
                candidateName = "TOI-1231 b (Golden JWST Sub-Neptune)",
                ticId = "447061717",
                constellation = "Vela",
                rightAscension = "09h 15m 58s",
                declination = "-52° 28′ 13″",
                apparentVisualMag = 12.3,
                periodDays = 24.24,
                transitDurationHours = 3.6,
                epochBtjd = 1600.41,
                transitDepthPpm = 3400.0,
                currentBtjd = currentBtjd
            ),
            skyRadarEngine.computeNextTransit(
                candidateName = "TOI-1895.01 (Recovered Temperate Mono-Transit)",
                ticId = "384159646",
                constellation = "Cygnus",
                rightAscension = "19h 42m 12s",
                declination = "+41° 18′ 29″",
                apparentVisualMag = 9.75,
                periodDays = 194.5,
                transitDurationHours = 14.8,
                epochBtjd = 2390.412,
                transitDepthPpm = 4850.0,
                currentBtjd = currentBtjd
            )
        )
    }

    suspend fun runCustomDilutionDeblend(
        candidateName: String,
        ticId: String,
        status: String,
        depthPpm: Double,
        targetMag: Double,
        neighborMag: Double,
        sepArcsec: Double,
        radSun: Double
    ): DilutionRescueCandidate = withContext(Dispatchers.Default) {
        dilutionEngine.deblendCandidate(
            candidateName = candidateName,
            ticId = ticId,
            originalNasaStatus = status,
            observedTransitDepthPpm = depthPpm,
            targetStarMagGaia = targetMag,
            blendingStarMagGaia = neighborMag,
            angularSeparationArcsec = sepArcsec,
            stellarRadiusSun = radSun
        )
    }
}
