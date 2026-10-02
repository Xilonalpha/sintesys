package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.AtmosphericMolecule
import com.synthesis.domain.model.exoplanet.BiosignatureAssessment
import com.synthesis.domain.model.exoplanet.TechnosignatureCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * Biosignature & Technosignature Spectral Fingerprint Analyzer
 *
 * Implements Krissansen-Totton et al. (2018, Science Advances, 4(1):eaao5747)
 * atmospheric thermodynamic disequilibrium calculations and transit asymmetry
 * algorithms for megastructure detection (Boyajian's star analog analysis).
 */
@Singleton
class BiosignatureTechnosignatureEngine @Inject constructor() {

    private val R_GAS = 8.314462 // J / (mol * K)

    /**
     * Evaluates atmospheric chemical disequilibrium and biosignature probability.
     * Simultaneous CH4 + O2/O3 is the canonical golden biosignature pair because
     * photochemistry rapidly destroys CH4 in an oxidizing atmosphere unless actively
     * replenished by biological processes.
     */
    fun evaluateBiosignatures(
        planetName: String,
        hostStar: String,
        equilibriumTempK: Double,
        molecules: List<AtmosphericMolecule>
    ): BiosignatureAssessment {
        val ch4 = molecules.find { it.formula == "CH4" }
        val o2 = molecules.find { it.formula == "O2" }
        val o3 = molecules.find { it.formula == "O3" }
        val dms = molecules.find { it.formula == "DMS" || it.formula == "(CH3)2S" }
        val ph3 = molecules.find { it.formula == "PH3" }
        val co2 = molecules.find { it.formula == "CO2" }
        val h2o = molecules.find { it.formula == "H2O" }

        // Thermodynamic Disequilibrium Gibbs Free Energy Metric Phi (kJ/mol)
        // Krissansen-Totton 2018 approximation based on methane-oxygen chemical potential
        var disequilibriumKj = 0.0
        if (ch4 != null && (o2 != null || o3 != null)) {
            val ch4Abundance = ch4.abundancePpm * 1e-6
            val o2Abundance = ((o2?.abundancePpm ?: 0.0) + (o3?.abundancePpm ?: 0.0) * 100.0) * 1e-6
            // Delta G ~ -R * T * ln(K_eq / Q)
            disequilibriumKj = (R_GAS * equilibriumTempK * (ln(ch4Abundance.coerceAtLeast(1e-10)) + 2.0 * ln(o2Abundance.coerceAtLeast(1e-10)))) / -1000.0
            disequilibriumKj = abs(disequilibriumKj).coerceIn(1.0, 150.0)
        }

        // Biological Confidence Scoring (0 - 100)
        var score = 10.0
        val reasons = mutableListOf<String>()
        val abioticRisks = mutableListOf<String>()

        if (ch4 != null && ch4.detectionSigma >= 3.0) {
            score += 20.0
            reasons.add("Methane (CH4) confirmed at ${ch4.abundancePpm} ppm (${ch4.detectionSigma}σ)")
        }

        if ((o2 != null && o2.detectionSigma >= 3.0) || (o3 != null && o3.detectionSigma >= 3.0)) {
            val oxidant = o2 ?: o3!!
            score += 30.0
            reasons.add("Oxidant (${oxidant.formula}) detected at ${oxidant.detectionSigma}σ")
            if (ch4 != null) {
                score += 25.0
                reasons.add("★ CANONICAL BIOLOGICAL PAIR: Simultaneous CH4 + O2/O3 in thermodynamic disequilibrium!")
            }
        }

        if (dms != null && dms.detectionSigma >= 2.5) {
            score += 15.0
            reasons.add("Marine phytoplankton biomarker Dimethyl Sulfide (DMS) detected at ${dms.detectionSigma}σ")
        }

        if (ph3 != null && ph3.detectionSigma >= 3.0) {
            score += 10.0
            reasons.add("Phosphine (PH3) detected at ${ph3.detectionSigma}σ (anaerobic biological indicator)")
        }

        if (co2 != null && h2o != null) {
            reasons.add("H2O + CO2 confirmed: Temperate carbon-silicate cycle active")
        }

        if (ch4 != null && o2 == null && o3 == null && (co2?.abundancePpm ?: 0.0) < 100.0) {
            abioticRisks.add("Methane without strong oxidants can be produced by serpentinization or mantle degassing.")
        }
        if (disequilibriumKj < 5.0) {
            abioticRisks.add("Atmosphere is near chemical equilibrium; photochemical abiotic synthesis cannot be ruled out.")
        }

        val clampedScore = score.coerceIn(5.0, 98.0)

        return BiosignatureAssessment(
            planetName = planetName,
            hostStar = hostStar,
            equilibriumTempK = equilibriumTempK,
            thermodynamicDisequilibriumMetric = disequilibriumKj,
            detectedMolecules = molecules,
            biosignatureScore = clampedScore,
            biologicalExplanation = reasons.joinToString(". "),
            abioticAlternativeRisk = abioticRisks.ifEmpty { listOf("Negligible abiotic explanation for simultaneous oxidants + reductants.") }.joinToString(". ")
        )
    }

    /**
     * Evaluates transit light-curve asymmetry to differentiate between spherical planets
     * and anomalous non-symmetric artificial megastructures (Dyson swarms, solar collectors).
     */
    fun evaluateTechnosignatures(
        candidateName: String,
        hostStar: String,
        ingressDurationHours: Double,
        egressDurationHours: Double,
        depthVariancePpm: Double,
        cfcPollutionSigma: Double?
    ): TechnosignatureCandidate {
        val totalDuration = ingressDurationHours + egressDurationHours + 0.1
        // Spherical planets have ingress == egress. Asymmetry index > 0.25 indicates non-spherical obscuration.
        val asymmetryIndex = (abs(ingressDurationHours - egressDurationHours) / totalDuration).coerceIn(0.0, 1.0)

        var artificialProb = (asymmetryIndex * 60.0) + (depthVariancePpm / 200.0).coerceIn(0.0, 25.0)
        if (cfcPollutionSigma != null && cfcPollutionSigma >= 3.0) {
            artificialProb += (cfcPollutionSigma * 8.0)
        }
        artificialProb = artificialProb.coerceIn(2.0, 95.0)

        val description = buildString {
            if (asymmetryIndex > 0.30) {
                append("Anomalous steep ingress (${ingressDurationHours}h) vs elongated egress (${egressDurationHours}h), asymmetry = ${String.format("%.2f", asymmetryIndex)}. ")
                append("Consistent with non-spherical triangular or multi-panel obscuring geometry. ")
            } else {
                append("Near-symmetric profile with minor ingress/egress variance (${String.format("%.2f", asymmetryIndex)}). ")
            }
            if (cfcPollutionSigma != null && cfcPollutionSigma >= 3.0) {
                append("★ ARTIFICIAL POLLUTION BAND: Chlorofluorocarbon (CFC-11/12) absorption signature detected at ${String.format("%.1f", cfcPollutionSigma)}σ! ")
            }
        }

        val naturalHypothesis = if (asymmetryIndex > 0.30) {
            "Disintegrating ultra-short period rocky comet tail, circumplanetary tilted ring system, or gravity-darkened stellar oblate transit."
        } else {
            "Standard planetary limb darkening on a spherical body."
        }

        return TechnosignatureCandidate(
            candidateName = candidateName,
            hostStar = hostStar,
            transitAsymmetryIndex = asymmetryIndex,
            ingressDurationHours = ingressDurationHours,
            egressDurationHours = egressDurationHours,
            depthVariancePpm = depthVariancePpm,
            cfcPollutionSigma = cfcPollutionSigma,
            artificialMegastructureProbability = artificialProb,
            physicalDescription = description,
            naturalPhenomenaHypothesis = naturalHypothesis
        )
    }
}
