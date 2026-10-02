package com.synthesis.domain.model.exoplanet

data class PhotometricDataPoint(
    val minuteOffset: Double,      // Minutes from observation start
    val bjdTime: Double,          // Barycentric Julian Date
    val relativeFlux: Double,      // Normalized stellar flux (baseline = 1.000)
    val fluxError: Double = 0.0012 // Photometric uncertainty
)

data class LiveObservationSession(
    val candidateName: String = "TOI-1895.01",
    val hostStar: String = "TIC 384159821",
    val observerName: String = "Zagorka Lynovi",
    val aavsoObserverCode: String = "LYNO",
    val observatoryLocation: String = "44.432° N, 26.103° E (București)",
    val telescopeSpecs: String = "200mm f/5 Newtonian Reflector",
    val cameraModel: String = "ZWO ASI533MM Pro (Gain 100, -10°C)",
    val filterBand: String = "Sloan r' (Clear Red)",
    val comparisonStarName: String = "TYC 3412-1102-1",
    val comparisonStarMag: Double = 10.450,
    val checkStarName: String = "TYC 3412-0943-1",
    val checkStarMag: Double = 10.610,
    val expectedDepthPpm: Double = 3200.0,
    val expectedDurationMinutes: Double = 180.0,
    val dataPoints: List<PhotometricDataPoint> = emptyList(),
    val isObserving: Boolean = false,
    val isRealImportedData: Boolean = false,
    val dataSourceTag: String = "DEMO BENCHMARK (Simulat)",
    val currentPhase: String = "Pre-Ingress Baseline",
    val calculatedMidTransitBjd: Double = 2460218.4215,
    val measuredDepthPpm: Double = 0.0,
    val measuredRadiusEarth: Double = 0.0,
    val signalToNoiseRatio: Double = 0.0,
    val chiSquaredPerDof: Double = 0.98
)
