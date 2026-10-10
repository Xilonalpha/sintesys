package com.synthesis.domain.model.exoplanet

/**
 * Rigorous statistical representation of a Bayesian Credible Interval / Uncertainty distribution
 * for transit signal verification, compliant with NASA / ESA / IAU publication criteria.
 */
data class BayesianUncertaintyMetrics(
    val medianConfidence: Double, // Posterior median (%)
    val modeConfidence: Double,   // Maximum A Posteriori (MAP) (%)
    val lowerBound68: Double,     // 1-sigma lower (16th percentile) (%)
    val upperBound68: Double,     // 1-sigma upper (84th percentile) (%)
    val lowerBound95: Double,     // 2-sigma lower (2.5th percentile) (%)
    val upperBound95: Double,     // 2-sigma upper (97.5th percentile) (%)
    val minusError1Sigma: Double, // -Δ (1-sigma error bar)
    val plusError1Sigma: Double,  // +Δ (1-sigma error bar)
    val effectiveSampleSize: Int, // MCMC/Posterior draws or synthetic posterior count
    val bayesFactorVsFlat: Double,// Evidence ratio B_10 (Transit hypothesis vs Flat baseline)
    val falseAlarmProbabilityFap: Double, // FAP probability (0.0 to 1.0)
    val posteriorStandardDeviation: Double,
    val alphaPosterior: Double,   // Beta posterior shape parameter α = α_prior + k
    val betaPosterior: Double     // Beta posterior shape parameter β = β_prior + (n - k)
) {
    /**
     * Formats the confidence score with asymmetric Bayesian 1-sigma error bars.
     * Example: "88.4% (+2.1 / -2.4)"
     */
    fun formatWithAsymmetricErrors(): String {
        return "${String.format("%.1f", medianConfidence)}% (+${String.format("%.1f", plusError1Sigma)} / -${String.format("%.1f", minusError1Sigma)})"
    }

    /**
     * Formats as LaTeX / Scientific standard string for papers and agency reports:
     * C = 88.4%_{-2.4}^{+2.1} (68% CI [86.0%, 90.5%])
     */
    fun formatScientificString(): String {
        return "${String.format("%.1f", medianConfidence)}% (-${String.format("%.1f", minusError1Sigma)}/+${String.format("%.1f", plusError1Sigma)}) [68% CI: ${String.format("%.1f", lowerBound68)}% - ${String.format("%.1f", upperBound68)}%]"
    }
}
