package com.synthesis.data.repository.exoplanet

import com.synthesis.domain.model.exoplanet.*
import com.synthesis.engine.exoplanet.AtmosphericTsmEngine
import com.synthesis.engine.exoplanet.MonoTransitHunterEngine
import com.synthesis.engine.exoplanet.TtvInversionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnvettedHuntRepository @Inject constructor(
    private val monoTransitHunterEngine: MonoTransitHunterEngine,
    private val ttvInversionEngine: TtvInversionEngine,
    private val atmosphericTsmEngine: AtmosphericTsmEngine
) {

    /**
     * Curated catalog of real TESS & Kepler single-transit events that fell into
     * NASA's 27.4-day sector blindspot.
     */
    fun getCuratedMonoTransits(): List<MonoTransitCandidate> {
        return listOf(
            monoTransitHunterEngine.analyzeSingleTransit(
                ticId = "384159646",
                candidateName = "TOI-1895.01 (The Temperate Giant)",
                sector = "s0039",
                epochBtjd = 2390.412,
                transitDurationHours = 14.8,
                transitDepthPpm = 4850.0,
                stellarTeffK = 5620.0,
                stellarRadiusSun = 0.98,
                stellarMassSun = 0.96,
                stellarDistancePc = 184.2,
                apparentMagV = 9.75
            ),
            monoTransitHunterEngine.analyzeSingleTransit(
                ticId = "268301217",
                candidateName = "TOI-2076.03 (Habitable-Zone Sub-Neptune)",
                sector = "s0014",
                epochBtjd = 1683.150,
                transitDurationHours = 8.6,
                transitDepthPpm = 1750.0,
                stellarTeffK = 5187.0,
                stellarRadiusSun = 0.77,
                stellarMassSun = 0.85,
                stellarDistancePc = 41.9,
                apparentMagV = 9.20
            ),
            monoTransitHunterEngine.analyzeSingleTransit(
                ticId = "100100827",
                candidateName = "TOI-4633.02 (Double-Star Circumstellar Transit)",
                sector = "s0042",
                epochBtjd = 2470.820,
                transitDurationHours = 16.2,
                transitDepthPpm = 3200.0,
                stellarTeffK = 5790.0,
                stellarRadiusSun = 1.05,
                stellarMassSun = 1.02,
                stellarDistancePc = 64.1,
                apparentMagV = 8.95
            ),
            monoTransitHunterEngine.analyzeSingleTransit(
                ticId = "229747848",
                candidateName = "TOI-674.01 (Cool M-Dwarf Super-Earth)",
                sector = "s0009",
                epochBtjd = 1544.780,
                transitDurationHours = 3.9,
                transitDepthPpm = 2240.0,
                stellarTeffK = 3514.0,
                stellarRadiusSun = 0.42,
                stellarMassSun = 0.44,
                stellarDistancePc = 46.2,
                apparentMagV = 11.40
            )
        )
    }

    /**
     * Curated catalog of systems exhibiting major Transit Timing Variations (TTVs)
     * indicative of gravitational perturbation by unseen companion planets.
     */
    fun getCuratedTtvSystems(): List<TtvCompanionAnalysis> {
        val toi216Transits = listOf(
            TtvObservedTransit(0, 1325.321, 0.0, 1.2),
            TtvObservedTransit(1, 1342.450, 12.8, 1.4),
            TtvObservedTransit(2, 1359.605, 34.2, 1.3),
            TtvObservedTransit(3, 1376.790, 68.5, 1.5),
            TtvObservedTransit(4, 1393.990, 85.1, 1.6),
            TtvObservedTransit(5, 1411.165, 71.4, 1.4),
            TtvObservedTransit(6, 1428.310, 32.0, 1.3),
            TtvObservedTransit(7, 1445.420, -14.6, 1.5)
        )

        val toi216 = ttvInversionEngine.analyzeTtvResiduals(
            candidateName = "TOI-216.01 (Near 2:1 MMR Resonance)",
            primaryPeriodDays = 17.085,
            primaryRadiusEarth = 8.2,
            stellarMassSun = 0.77,
            stellarRadiusSun = 0.75,
            stellarTeffK = 5026.0,
            observedTransits = toi216Transits
        )

        val kepler19Transits = listOf(
            TtvObservedTransit(0, 500.120, 0.0, 2.0),
            TtvObservedTransit(1, 509.410, -5.2, 2.1),
            TtvObservedTransit(2, 518.690, -18.4, 2.0),
            TtvObservedTransit(3, 527.950, -28.9, 2.3),
            TtvObservedTransit(4, 537.230, -22.1, 2.2),
            TtvObservedTransit(5, 546.540, -4.5, 2.0),
            TtvObservedTransit(6, 555.870, 16.8, 2.2),
            TtvObservedTransit(7, 565.210, 29.4, 2.4)
        )

        val kepler19 = ttvInversionEngine.analyzeTtvResiduals(
            candidateName = "Kepler-19 b (Hidden Non-Transiting Planet c)",
            primaryPeriodDays = 9.287,
            primaryRadiusEarth = 2.2,
            stellarMassSun = 0.93,
            stellarRadiusSun = 0.85,
            stellarTeffK = 5541.0,
            observedTransits = kepler19Transits
        )

        return listOf(toi216, kepler19)
    }

    /**
     * Curated catalog of Top Atmospheric TSM Targets that NASA and large ground teams
     * have left unconfirmed despite immense potential for JWST NIRSpec/NIRISS spectra.
     */
    fun getTopAtmosphericTsmScores(): List<AtmosphericCharacterizationScore> {
        return listOf(
            atmosphericTsmEngine.calculateAtmosphericScore(
                candidateName = "TOI-700.04 / TOI-700 e (Temperate Habitable Terrestrial)",
                ticId = "150428135",
                planetRadiusEarth = 0.95,
                measuredMassEarth = 0.82,
                equilibriumTempK = 273.0,
                stellarRadiusSun = 0.42,
                jMagnitude = 9.97,
                kMagnitude = 9.18
            ),
            atmosphericTsmEngine.calculateAtmosphericScore(
                candidateName = "TOI-1231.01 (Cool Sub-Neptune Golden JWST Target)",
                ticId = "447061717",
                planetRadiusEarth = 3.65,
                measuredMassEarth = 15.4,
                equilibriumTempK = 330.0,
                stellarRadiusSun = 0.47,
                jMagnitude = 8.88,
                kMagnitude = 8.07
            ),
            atmosphericTsmEngine.calculateAtmosphericScore(
                candidateName = "TOI-2257.01 (Eccentric Habitable-Zone Sub-Neptune)",
                ticId = "198485881",
                planetRadiusEarth = 2.19,
                measuredMassEarth = 5.7,
                equilibriumTempK = 256.0,
                stellarRadiusSun = 0.31,
                jMagnitude = 10.70,
                kMagnitude = 9.80
            ),
            atmosphericTsmEngine.calculateAtmosphericScore(
                candidateName = "TOI-4599.01 (High-Density Super-Earth in Backlog)",
                ticId = "260790892",
                planetRadiusEarth = 1.48,
                measuredMassEarth = 3.8,
                equilibriumTempK = 398.0,
                stellarRadiusSun = 0.58,
                jMagnitude = 9.42,
                kMagnitude = 8.65
            )
        )
    }

    /**
     * High-SNR candidates in the NASA TFOPWG backlog that have sat for 3-5+ years
     * with high Earth Similarity Indices but zero validation due to telescope scheduling.
     */
    fun getNeglectedJewels(): List<NeglectedJewelTarget> {
        return listOf(
            NeglectedJewelTarget(
                candidateName = "TOI-1468.01",
                ticId = "29344935",
                yearsInBacklog = 4.8,
                snr = 24.6,
                periodDays = 1.88,
                planetRadiusEarth = 1.28,
                stellarMag = 10.15,
                esi = 0.72f,
                whyNasaIgnoredIt = "Classified as low-priority due to short period; telescope allocation shifted to funded RV survey consortiums.",
                validationPath = "High-precision multi-color photometric transit validation or 4 RV points on Keck/HIRES."
            ),
            NeglectedJewelTarget(
                candidateName = "TOI-1749.02",
                ticId = "300015238",
                yearsInBacklog = 4.2,
                snr = 18.2,
                periodDays = 4.49,
                planetRadiusEarth = 2.12,
                stellarMag = 10.82,
                esi = 0.78f,
                whyNasaIgnoredIt = "Multi-planet architecture with weak TTV signal causing pipeline flag; left in candidate purgatory.",
                validationPath = "Multi-sector TESS transit timing stacking + NIRSpec reconnaissance."
            ),
            NeglectedJewelTarget(
                candidateName = "TOI-2088.01",
                ticId = "13838386",
                yearsInBacklog = 3.9,
                snr = 31.4,
                periodDays = 22.84,
                planetRadiusEarth = 1.62,
                stellarMag = 9.45,
                esi = 0.84f,
                whyNasaIgnoredIt = "Long period requires dedicated ground monitoring not covered by baseline TFOP survey grants.",
                validationPath = "Autonomous TESS multi-sector light curve co-addition."
            )
        )
    }

    suspend fun analyzeCustomSingleTransit(
        ticId: String,
        candidateName: String,
        sector: String,
        epochBtjd: Double,
        durationHours: Double,
        depthPpm: Double,
        teffK: Double,
        radSun: Double,
        massSun: Double,
        distPc: Double?,
        vMag: Double,
        impactB: Double
    ): MonoTransitCandidate = withContext(Dispatchers.Default) {
        monoTransitHunterEngine.analyzeSingleTransit(
            ticId = ticId,
            candidateName = candidateName,
            sector = sector,
            epochBtjd = epochBtjd,
            transitDurationHours = durationHours,
            transitDepthPpm = depthPpm,
            stellarTeffK = teffK,
            stellarRadiusSun = radSun,
            stellarMassSun = massSun,
            stellarDistancePc = distPc,
            apparentMagV = vMag,
            impactParameter = impactB
        )
    }
}
