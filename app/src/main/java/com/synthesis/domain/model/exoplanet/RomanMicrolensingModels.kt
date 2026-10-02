package com.synthesis.domain.model.exoplanet

data class MicrolensingEvent(
    val eventId: String,
    val targetName: String,
    val raDeg: Double,
    val decDeg: Double,
    val sourceDistanceKpc: Double,
    val lensDistanceKpc: Double,
    val impactParameterU0: Double,
    val einsteinCrossingTimeDays: Double,
    val peakMagnificationA0: Double,
    val isFreeFloatingPlanet: Boolean,
    val planetMassEarth: Double,
    val massRatioQ: Double?,
    val separationS: Double?,
    val lensType: String,
    val surveyOrigin: String,
    val discoveryConfidencePercent: Double,
    val description: String
)

data class MicrolensingCurvePoint(
    val timeDays: Double,
    val magnification: Double,
    val planetaryAnomalyDeltaA: Double,
    val isCausticCrossing: Boolean
)

data class MicrolensingInversionResult(
    val eventId: String,
    val targetName: String,
    val lensType: String,
    val inferredMassEarth: Double,
    val inferredMassJupiter: Double,
    val einsteinRadiusMas: Double,
    val properMotionMasYr: Double,
    val peakMagnification: Double,
    val crossingDurationHours: Double,
    val isFreeFloating: Boolean,
    val causticStructure: String,
    val astrophysicalSummary: String,
    val lightCurve: List<MicrolensingCurvePoint>
)
