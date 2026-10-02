package com.synthesis.engine.exoplanet

import com.synthesis.domain.model.exoplanet.MicrolensingCurvePoint
import com.synthesis.domain.model.exoplanet.MicrolensingEvent
import com.synthesis.domain.model.exoplanet.MicrolensingInversionResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class RomanMicrolensingEngine @Inject constructor() {

    fun getBenchmarkEvents(): List<MicrolensingEvent> {
        return listOf(
            MicrolensingEvent(
                eventId = "OGLE-2016-BLG-1928",
                targetName = "OGLE-2016-BLG-1928 (Rogue Earth/Mars)",
                raDeg = 270.458,
                decDeg = -28.125,
                sourceDistanceKpc = 8.2,
                lensDistanceKpc = 4.5,
                impactParameterU0 = 0.082,
                einsteinCrossingTimeDays = 0.029, // ~41.5 minutes
                peakMagnificationA0 = 12.2,
                isFreeFloatingPlanet = true,
                planetMassEarth = 0.55, // Sub-Earth to Mars mass
                massRatioQ = null,
                separationS = null,
                lensType = "Terrestrial Rogue Planet (Free-Floating)",
                surveyOrigin = "OGLE-IV Survey & Roman Precursor",
                discoveryConfidencePercent = 99.4,
                description = "Shortest timescale microlensing event ever discovered. Demonstrates free-floating planetary mass without host star."
            ),
            MicrolensingEvent(
                eventId = "ROMAN-2026-BULGE-FFP1",
                targetName = "Roman-2026-Bulge-FFP1 (Earth-mass FFP)",
                raDeg = 268.320,
                decDeg = -29.740,
                sourceDistanceKpc = 8.0,
                lensDistanceKpc = 5.2,
                impactParameterU0 = 0.120,
                einsteinCrossingTimeDays = 0.180, // ~4.3 hours
                peakMagnificationA0 = 8.35,
                isFreeFloatingPlanet = true,
                planetMassEarth = 1.15,
                massRatioQ = null,
                separationS = null,
                lensType = "Earth-Mass Rogue Planet (Free-Floating)",
                surveyOrigin = "NASA Roman Galactic Bulge Time Domain Survey (2026 Sim)",
                discoveryConfidencePercent = 98.7,
                description = "High-cadence Roman WFI detection in Baade's Window. Single-lens isolated magnification peak."
            ),
            MicrolensingEvent(
                eventId = "KMT-2020-BLG-0414",
                targetName = "KMT-2020-BLG-0414Lb (Wide Earth)",
                raDeg = 269.112,
                decDeg = -27.890,
                sourceDistanceKpc = 7.8,
                lensDistanceKpc = 4.1,
                impactParameterU0 = 0.045,
                einsteinCrossingTimeDays = 26.5,
                peakMagnificationA0 = 22.4,
                isFreeFloatingPlanet = false,
                planetMassEarth = 1.90,
                massRatioQ = 0.000015,
                separationS = 1.25,
                lensType = "Earth-mass Planet at 1.5 AU (Host + Planet)",
                surveyOrigin = "KMTNet & Roman Synergies",
                discoveryConfidencePercent = 97.2,
                description = "Earth-mass companion causing a secondary caustic-crossing anomaly on a stellar microlensing light curve."
            ),
            MicrolensingEvent(
                eventId = "ROMAN-2026-SNOWLINE-J1",
                targetName = "Roman-2026-Snowline-J1 (Cold Gas Giant)",
                raDeg = 271.850,
                decDeg = -30.150,
                sourceDistanceKpc = 8.5,
                lensDistanceKpc = 4.8,
                impactParameterU0 = 0.095,
                einsteinCrossingTimeDays = 34.0,
                peakMagnificationA0 = 10.5,
                isFreeFloatingPlanet = false,
                planetMassEarth = 318.0, // 1 Jupiter mass
                massRatioQ = 0.0012,
                separationS = 1.42,
                lensType = "Cold Jupiter beyond Snowline (3.2 AU)",
                surveyOrigin = "NASA Roman Galactic Bulge Survey (2026 Sim)",
                discoveryConfidencePercent = 99.8,
                description = "Massive planetary caustic perturbation at t = +3.2 days demonstrating Roman sensitivity to outer systems."
            ),
            MicrolensingEvent(
                eventId = "ROMAN-2026-SUB-NEPTUNE-R4",
                targetName = "Roman-2026-Sub-Neptune-R4 (Cold Ice World)",
                raDeg = 267.950,
                decDeg = -28.920,
                sourceDistanceKpc = 8.1,
                lensDistanceKpc = 5.0,
                impactParameterU0 = 0.150,
                einsteinCrossingTimeDays = 18.2,
                peakMagnificationA0 = 6.7,
                isFreeFloatingPlanet = false,
                planetMassEarth = 8.4,
                massRatioQ = 0.000042,
                separationS = 0.88,
                lensType = "Cold Sub-Neptune at Snowline (2.1 AU)",
                surveyOrigin = "NASA Roman Galactic Bulge Survey (2026 Sim)",
                discoveryConfidencePercent = 96.5,
                description = "Resonant caustic anomaly probing the planetary mass function in the cold outer zone."
            )
        )
    }

    /**
     * Computes the Paczynski (1986) point-source point-lens magnification
     * A(u) = (u^2 + 2) / (u * sqrt(u^2 + 4))
     */
    fun paczynskiMagnification(u: Double): Double {
        val uSafe = max(1e-5, abs(u))
        val u2 = uSafe * uSafe
        return (u2 + 2.0) / (uSafe * sqrt(u2 + 4.0))
    }

    /**
     * Simulates light curve across normalized time span t in [-1.5 * t_E, +1.5 * t_E]
     */
    fun generateLightCurve(
        event: MicrolensingEvent,
        steps: Int = 100
    ): List<MicrolensingCurvePoint> {
        val points = mutableListOf<MicrolensingCurvePoint>()
        val span = max(0.1, event.einsteinCrossingTimeDays * 1.5)
        val dt = (2.0 * span) / steps.toDouble()

        // If binary lens, planetary anomaly time & width
        val anomalyTime = if (!event.isFreeFloatingPlanet && event.separationS != null) {
            span * 0.25 // slight offset after peak
        } else null

        val anomalyDuration = if (event.massRatioQ != null) {
            max(0.01, event.einsteinCrossingTimeDays * sqrt(event.massRatioQ))
        } else 0.05

        var t = -span
        while (t <= span) {
            val u = sqrt(event.impactParameterU0.pow(2.0) + (t / max(0.001, event.einsteinCrossingTimeDays)).pow(2.0))
            val baseA = paczynskiMagnification(u)

            var deltaA = 0.0
            var isCaustic = false

            if (anomalyTime != null && event.massRatioQ != null) {
                val distToAnomaly = abs(t - anomalyTime)
                if (distToAnomaly < anomalyDuration * 2.0) {
                    val gaussianAnomaly = exp(-0.5 * (distToAnomaly / (anomalyDuration * 0.5)).pow(2.0))
                    deltaA = baseA * 0.35 * gaussianAnomaly
                    if (distToAnomaly < anomalyDuration * 0.6) {
                        isCaustic = true
                    }
                }
            }

            points.add(
                MicrolensingCurvePoint(
                    timeDays = (t * 1000.0).roundToLong() / 1000.0,
                    magnification = ((baseA + deltaA) * 1000.0).roundToLong() / 1000.0,
                    planetaryAnomalyDeltaA = (deltaA * 1000.0).roundToLong() / 1000.0,
                    isCausticCrossing = isCaustic
                )
            )
            t += dt
        }

        return points
    }

    /**
     * Solves the inverse problem: inverts the Einstein ring radius theta_E and crossing time t_E
     * to determine the rogue planet or companion mass in Earth and Jupiter masses.
     */
    fun invertMicrolensingEvent(
        event: MicrolensingEvent
    ): MicrolensingInversionResult {
        val dl = event.lensDistanceKpc * 1000.0 // pc
        val ds = event.sourceDistanceKpc * 1000.0 // pc
        val piRel = (1.0 / dl) - (1.0 / ds) // relative parallax (arcsec)

        // Typical galactic bulge relative proper motion mu_rel ~ 6.5 mas/yr
        val muRelMasYr = 6.5
        val muRelMasDay = muRelMasYr / 365.25

        // theta_E = mu_rel * t_E
        val thetaEMas = muRelMasDay * event.einsteinCrossingTimeDays
        val thetaEArcsec = thetaEMas / 1000.0

        // Mass of lens: M = theta_E^2 / (kappa * pi_rel), where kappa = 4G / (c^2 * 1AU) ~ 8.144 mas / M_sun
        val kappa = 8.144 // mas / M_sun
        val massSolar = if (event.isFreeFloatingPlanet) {
            // Lens itself is the planet
            (thetaEMas.pow(2.0)) / (kappa * (piRel * 1000.0).coerceAtLeast(0.01))
        } else {
            // Primary lens is a star, planet is q * M_*
            val primaryMassSolar = (thetaEMas.pow(2.0)) / (kappa * (piRel * 1000.0).coerceAtLeast(0.01))
            primaryMassSolar * (event.massRatioQ ?: 0.001)
        }

        val massEarth = (massSolar * 333000.0).coerceAtLeast(0.1)
        val massJupiter = massEarth / 317.8

        val crossingHours = event.einsteinCrossingTimeDays * 24.0

        val causticStructure = if (event.isFreeFloatingPlanet) {
            "Punctiform Izolat (Fără structură de caustică stelară - Planetă Orfană FFP)"
        } else {
            val s = event.separationS ?: 1.0
            if (s > 1.0) "Caustice Planetare Triangulare (Separare Largă s = ${String.format("%.2f", s)})"
            else "Caustică Centrală Închisă (Separare Strânsă s = ${String.format("%.2f", s)})"
        }

        val summary = if (event.isFreeFloatingPlanet) {
            "Eveniment singular fără stea gazdă detectabilă pe o rază de >10 AU. Durata scurtă de traversare (${String.format("%.1f", crossingHours)} ore) confirmă o planetă liberă (Rogue Planet) de masă terestră (${String.format("%.2f", massEarth)} M⊕) rătăcind prin Bulbul Galactic."
        } else {
            "Sistem binar cu perturbație de tip caustică planetară. Inversia numerică Roman/Paczynski indică un companion planetar de ${String.format("%.2f", massEarth)} M⊕ (${String.format("%.3f", massJupiter)} M_Jup) la distanța de proiecție s = ${String.format("%.2f", event.separationS ?: 1.0)} θ_E."
        }

        val curve = generateLightCurve(event)

        return MicrolensingInversionResult(
            eventId = event.eventId,
            targetName = event.targetName,
            lensType = event.lensType,
            inferredMassEarth = (massEarth * 100.0).roundToLong() / 100.0,
            inferredMassJupiter = (massJupiter * 1000.0).roundToLong() / 1000.0,
            einsteinRadiusMas = (thetaEMas * 1000.0).roundToLong() / 1000.0,
            properMotionMasYr = muRelMasYr,
            peakMagnification = (event.peakMagnificationA0 * 100.0).roundToLong() / 100.0,
            crossingDurationHours = (crossingHours * 10.0).roundToLong() / 10.0,
            isFreeFloating = event.isFreeFloatingPlanet,
            causticStructure = causticStructure,
            astrophysicalSummary = summary,
            lightCurve = curve
        )
    }
}
