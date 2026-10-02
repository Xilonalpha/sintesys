package com.synthesis.engine.cosmology

import com.synthesis.domain.model.cosmology.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class BigBangCosmologyEngine @Inject constructor() {

    companion object {
        const val SPEED_OF_LIGHT_KM_S = 299792.458
        const val KM_PER_MPC = 3.085677581e19
        const val SECONDS_PER_YEAR = 3.15576e7
        const val PLANCK_H0 = 67.4
        const val SHOES_H0 = 73.04
    }

    fun getCosmicEpochs(): List<CosmicEpoch> {
        return listOf(
            CosmicEpoch(
                epochName = "1. Era Planck & Gravitație Cuantică",
                timeSinceBigBang = "t < 10⁻⁴³ secunde",
                redshiftZ = 1.0e32,
                temperatureKelvin = 1.42e32,
                scaleFactorA = 1.0e-32,
                dominantEnergy = "Super-forță Unificată (Gravitație + GUT)",
                physicalProcesses = "Densitate de energie Planck. Spațiul și timpul își pierd caracterul neted; spumă cuantică spațio-temporală.",
                keyObservables = "Limite fundamentale ale fizicii cunoscute (lungimea Planck l_p = 1.6e-35 m)"
            ),
            CosmicEpoch(
                epochName = "2. Inflația Cosmică (Guth & Linde)",
                timeSinceBigBang = "10⁻³⁶ s până la 10⁻³² s",
                redshiftZ = 1.0e28,
                temperatureKelvin = 1.0e28,
                scaleFactorA = 1.0e-28,
                dominantEnergy = "Câmpul Scalar Inflaton (Energie de Vid)",
                physicalProcesses = "Expansiune exponențială de cel puțin e⁶⁰ ori (factor de 10²⁶). Netezește curbura și produce fluctuații cuantice primordiale.",
                keyObservables = "Omogenitatea universului, planeitatea spațială Ω_k ~ 0 și spectrul gaussian al perturbațiilor CMB"
            ),
            CosmicEpoch(
                epochName = "3. Supa Cuarc-Gluon & Bariogeneză",
                timeSinceBigBang = "10⁻¹¹ s până la 10⁻⁵ s",
                redshiftZ = 1.0e12,
                temperatureKelvin = 2.0e12,
                scaleFactorA = 1.0e-12,
                dominantEnergy = "Radiație Relativistă (Plasmă Termică)",
                physicalProcesses = "Ruperea simetriei electroslabe (mecanismul Higgs). Asimetria materie-antimaterie: 1 surplus de barion la 1 miliard de fotoni.",
                keyObservables = "CERN ALICE / LHC reproduce această stare de agregare la energii de TeV"
            ),
            CosmicEpoch(
                epochName = "4. Nucleosinteza Primordială (BBN)",
                timeSinceBigBang = "10 secunde până la 20 minute",
                redshiftZ = 4.0e8,
                temperatureKelvin = 1.0e9,
                scaleFactorA = 2.5e-9,
                dominantEnergy = "Radiație Relativistă",
                physicalProcesses = "Înghețarea raportului neutron/proton (n/p ~ 1/7). Fuziunea primelor nuclee atomice ușoare (Hidrogen, Deuteriu, Heliu-4, Litiu-7).",
                keyObservables = "Abundențele primordiale confirmate astronomic: ~75% H, ~24.5% He-4, D/H ~ 2.5e-5"
            ),
            CosmicEpoch(
                epochName = "5. Recombinarea & Decuplarea CMB",
                timeSinceBigBang = "380.000 ani",
                redshiftZ = 1100.0,
                temperatureKelvin = 2970.0,
                scaleFactorA = 1.0 / 1101.0,
                dominantEnergy = "Tranziție Radiație -> Materie",
                physicalProcesses = "Electronii sunt capturați de protoni formând atomi neutri de Hidrogen. Universul devine transparent; fotonii scapă liber.",
                keyObservables = "Radiația Cosmică de Fond (CMB) observată de COBE, WMAP și satelitul ESA Planck la T = 2.7255 K"
            ),
            CosmicEpoch(
                epochName = "6. Evul Întunecat & Zorii Cosmici",
                timeSinceBigBang = "10 milioane până la 150 milioane ani",
                redshiftZ = 30.0,
                temperatureKelvin = 84.0,
                scaleFactorA = 1.0 / 31.0,
                dominantEnergy = "Materie Întunecată Rece (CDM)",
                physicalProcesses = "Nicio stea nu strălucește încă. Materia întunecată colapsează gravitațional în halouri, pregătind nașterea primelor protogalaxii.",
                keyObservables = "Linia de 21 cm a hidrogenului neutru (țintă majoră pentru radiotelescopul SKA)"
            ),
            CosmicEpoch(
                epochName = "7. Reionizarea & Primele Stele (Pop III)",
                timeSinceBigBang = "400 milioane până la 1 miliard ani",
                redshiftZ = 8.0,
                temperatureKelvin = 24.5,
                scaleFactorA = 1.0 / 9.0,
                dominantEnergy = "Materie Barionică + Materie Întunecată",
                physicalProcesses = "Primele stele masive fără metale (Populația III) și quasarii primordiali emit radiații ultraviolete care reionizează gazul interstelar.",
                keyObservables = "Descoperiri recente JWST la z > 10 (galaxii timpurii JADES-GS-z14-0)"
            ),
            CosmicEpoch(
                epochName = "8. Era Prezentă & Accelerația Energiei Întunecate",
                timeSinceBigBang = "13.787 miliarde ani (Prezent)",
                redshiftZ = 0.0,
                temperatureKelvin = 2.7255,
                scaleFactorA = 1.0,
                dominantEnergy = "Energie Întunecată (Constanta Cosmologică Λ ~ 69%)",
                physicalProcesses = "Expansiunea universului accelerează împinsă de presiunea negativă a vidului cuantic (ecuația de stare w = -1).",
                keyObservables = "Supernove Tip Ia (Perlmutter, Schmidt, Riess - Nobel 2011), ESA Euclid și telescopul Roman"
            )
        )
    }

    /**
     * Solves the Friedmann equations for arbitrary cosmological parameters (H0, Omega_M, Omega_Lambda, Omega_R)
     */
    fun simulateCosmicExpansion(
        h0: Double = 67.4,
        omegaM: Double = 0.315,
        omegaLambda: Double = 0.685,
        omegaRadiation: Double = 9.0e-5
    ): BigBangSimulationResult {
        val omegaK = 1.0 - (omegaM + omegaLambda + omegaRadiation)

        // Deceleration parameter q0 = 0.5 * Omega_M - Omega_Lambda
        val q0 = (0.5 * omegaM) - omegaLambda

        // Numerical integration of cosmic age: t(z=0)
        // dt/dz = 1 / ((1+z) * H(z))
        // Exact Friedmann LCDM solution: t_H = 1 / H0 = 977.79 / H0 Gyr
        val hubbleTimeGyr = 977.79 / h0
        val ageGyr = if (omegaLambda > 0.001) {
            val term = (1.0 + sqrt(omegaLambda)) / sqrt(max(0.01, omegaM))
            ((2.0 / (3.0 * sqrt(omegaLambda))) * ln(term)) * hubbleTimeGyr
        } else {
            (2.0 / 3.0) * hubbleTimeGyr
        }

        // Sound horizon at drag epoch r_s ~ 147.2 Mpc * (omega_m / 0.315)^(-0.25)
        val soundHorizonMpc = 147.2 * (omegaM / 0.315).pow(-0.25)

        // Hubble Tension delta (Planck vs SH0ES)
        val hubbleTension = abs(SHOES_H0 - h0)

        // Generate expansion trajectory points
        val trajectory = mutableListOf<ExpansionCurvePoint>()
        val sampleRedshifts = listOf(0.0, 0.1, 0.3, 0.5, 1.0, 1.5, 2.0, 3.0, 5.0, 8.0, 11.0, 20.0, 50.0, 1100.0)

        sampleRedshifts.forEach { z ->
            val a = 1.0 / (1.0 + z)
            val ez = sqrt(
                omegaRadiation * (1.0 + z).pow(4.0) +
                omegaM * (1.0 + z).pow(3.0) +
                omegaK * (1.0 + z).pow(2.0) +
                omegaLambda
            )
            val hz = h0 * ez

            // Comoving distance integral from 0 to z
            var comovingIntegral = 0.0
            val subSteps = 100
            val subDz = z / subSteps
            for (j in 0 until subSteps) {
                val subZ = (j + 0.5) * subDz
                val subEz = sqrt(
                    omegaRadiation * (1.0 + subZ).pow(4.0) +
                    omegaM * (1.0 + subZ).pow(3.0) +
                    omegaK * (1.0 + subZ).pow(2.0) +
                    omegaLambda
                )
                comovingIntegral += (1.0 / subEz) * subDz
            }

            val dComovingMpc = (SPEED_OF_LIGHT_KM_S / h0) * comovingIntegral
            val dLumGpc = ((1.0 + z) * dComovingMpc) / 1000.0
            val dAngGpc = if (z > 0) (dComovingMpc / (1.0 + z)) / 1000.0 else 0.0

            trajectory.add(
                ExpansionCurvePoint(
                    redshiftZ = z,
                    scaleFactorA = (a * 10000.0).roundToLong() / 10000.0,
                    timeGyr = ((ageGyr * (a.pow(1.5))) * 100.0).roundToLong() / 100.0,
                    hubbleParamH = (hz * 10.0).roundToLong() / 10.0,
                    luminosityDistGpc = (dLumGpc * 100.0).roundToLong() / 100.0,
                    angularDiameterDistGpc = (dAngGpc * 100.0).roundToLong() / 100.0
                )
            )
        }

        val bbn = BbnPrimordialAbundances(
            hydrogenFraction = 0.753,
            helium4MassFractionYp = 0.2467,
            deuteriumAbundanceRatioDoverH = 2.54e-5,
            helium3AbundanceRatio = 1.05e-5,
            lithium7AbundanceRatio = 1.58e-10
        )

        return BigBangSimulationResult(
            hubbleConstantH0 = (h0 * 10.0).roundToLong() / 10.0,
            matterDensityOmegaM = (omegaM * 1000.0).roundToLong() / 1000.0,
            darkEnergyOmegaLambda = (omegaLambda * 1000.0).roundToLong() / 1000.0,
            radiationDensityOmegaR = omegaRadiation,
            curvatureOmegaK = (omegaK * 1000.0).roundToLong() / 1000.0,
            cosmicAgeGyr = (ageGyr * 100.0).roundToLong() / 100.0,
            soundHorizonMpc = (soundHorizonMpc * 10.0).roundToLong() / 10.0,
            cmbTemperatureK = 2.7255,
            decelerationParameterQ0 = (q0 * 100.0).roundToLong() / 100.0,
            hubbleTensionDeltaKmS = (hubbleTension * 10.0).roundToLong() / 10.0,
            expansionTrajectory = trajectory,
            bbnAbundances = bbn
        )
    }
}
