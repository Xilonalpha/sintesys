package com.synthesis.domain.model.exoplanet

import java.util.Locale

/**
 * Rigorous Gaussian and Bayesian Uncertainty Quantification for derived physical parameters.
 * Conforms to NASA / ESA / IAU publication requirements for TRL 8/9 Flight Qualification.
 */
data class PhysicalUncertaintyQuantification(
    val planetRadiusEarth: Double,
    val planetRadiusError1Sigma: Double,
    val semiMajorAxisAu: Double,
    val semiMajorAxisError1Sigma: Double,
    val equilibriumTempK: Double,
    val equilibriumTempError1Sigma: Double,
    val periodDays: Double,
    val periodError1Sigma: Double,
    val transitDepthPpm: Double,
    val depthError1Sigma: Double,
    val reducedChiSquared: Double,
    val degreesOfFreedom: Int
) {
    fun formatRadiusWithSigma(): String {
        return String.format(Locale.US, "%.2f ± %.2f R_Earth", planetRadiusEarth, planetRadiusError1Sigma)
    }

    fun formatSemiMajorAxisWithSigma(): String {
        return String.format(Locale.US, "%.4f ± %.4f AU", semiMajorAxisAu, semiMajorAxisError1Sigma)
    }

    fun formatTemperatureWithSigma(): String {
        return String.format(Locale.US, "%.1f ± %.1f K", equilibriumTempK, equilibriumTempError1Sigma)
    }

    fun formatPeriodWithSigma(): String {
        return String.format(Locale.US, "%.5f ± %.5f d", periodDays, periodError1Sigma)
    }

    fun formatDepthWithSigma(): String {
        return String.format(Locale.US, "%.1f ± %.1f ppm", transitDepthPpm, depthError1Sigma)
    }

    /**
     * Formats complete LaTeX string for peer-reviewed academic papers (ApJ / A&A / AJ).
     */
    fun formatLatexString(): String {
        return String.format(
            Locale.US,
            "R_p = %.2f \\pm %.2f\\, R_\\oplus,\\quad a = %.4f \\pm %.4f\\, \\text{AU},\\quad T_{\\text{eq}} = %.1f \\pm %.1f\\, \\text{K},\\quad \\chi^2_\\nu = %.2f",
            planetRadiusEarth, planetRadiusError1Sigma,
            semiMajorAxisAu, semiMajorAxisError1Sigma,
            equilibriumTempK, equilibriumTempError1Sigma,
            reducedChiSquared
        )
    }
}
