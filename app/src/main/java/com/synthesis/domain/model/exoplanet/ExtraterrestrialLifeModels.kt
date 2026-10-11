package com.synthesis.domain.model.exoplanet

/**
 * Domain models for the Extraterrestrial Life Discovery System.
 * Connects to real space telescopes, planetary rovers, and radio observatories:
 * - NASA Perseverance Rover (SHERLOC / PIXL / Mastcam-Z) in Jezero Crater
 * - NASA Curiosity Rover (SAM / Tunable Laser Spectrometer) in Gale Crater
 * - James Webb Space Telescope (JWST NIRSpec / MIRI) transmission spectroscopy
 * - Transiting Exoplanet Survey Satellite (TESS) habitable zone targets
 * - Green Bank & Breakthrough Listen radio technosignatures
 * - NASA Europa Clipper ocean plume spectrometry
 *
 * Implements the official NASA CoLD (Confidence of Life Detection) 7-Level Scale (Green et al., Nature 2021).
 */

enum class LifeDetectionDomain(val title: String, val icon: String) {
    ALL("Toate Observațiile", "🌌"),
    MARS_ROVERS("Marte - Rovere & Sateliți", "🔴"),
    JWST_SPECTROSCOPY("JWST - Biosignături Atmosferice", "🔭"),
    OCEAN_WORLDS("Oceane Subglaciare (Europa/Enceladus)", "🌊"),
    SETI_RADIO("SETI - Radio & Tehnosemnături", "📡"),
    HABITABLE_EXOPLANETS("Exoplanete în Zona Habitabilă", "🌍")
}

/**
 * Official NASA Confidence of Life Detection (CoLD) Scale
 * Framework published by NASA Chief Scientist James Green et al. in Nature (2021).
 */
enum class ColdLevel(
    val levelNumber: Int,
    val title: String,
    val description: String,
    val requirement: String
) {
    LEVEL_1(
        1,
        "Semnal / Biomarker Detectat",
        "Detecție inițială a unui semnal compatibil cu activitatea biologică (ex. gaz atmosferic, moleculă organică, semnal radio).",
        "Sensibilitate instrumentală peste pragul de zgomot (SNR > 3σ)."
    ),
    LEVEL_2(
        2,
        "Contaminare Exclusă",
        "S-a dovedit că semnalul nu provine din contaminare terestră, zgomot instrumental sau interferențe RFI locale.",
        "Teste oarbe de calibrare și linii de bază instrumentale validate."
    ),
    LEVEL_3(
        3,
        "Mediu Plauzibil Biologic",
        "Condițiile locale de mediu (apă, temperatură, nutrienți, presiune) sunt compatibile cu biologia și supraviețuirea vieții.",
        "Modelare geochimică și astrobiologică a habitabilității locale."
    ),
    LEVEL_4(
        4,
        "Mecanisme Abiotice Evaluate & Excluse",
        "S-a demonstrat că sursele ne-biologice (reacții Fischer-Tropsch, serpentinizare, fotoliză UV, meteoriți) nu pot explica abundența observată.",
        "Modele termodinamice care infirmă generarea abiotică pură."
    ),
    LEVEL_5(
        5,
        "Biosemnătură Independentă Suplimentară",
        "Detectarea unui al doilea semnal independent biologic (ex. pereche de dezechilibru CH4 + O3, fracționare izotopică δ¹³C sau chiralitate).",
        "Corelație temporală sau spațială a două biomarkere distincte."
    ),
    LEVEL_6(
        6,
        "Ipoteze Alternative Eliminate",
        "Ipoteze fizice concurente au fost testate riguros și respinse prin predicții și măsurători suplimentare.",
        "Consens al comunității astrobiologice și revizuire inter-colegială extinsă."
    ),
    LEVEL_7(
        7,
        "Confirmare Independentă Finală",
        "Verificare independentă realizată de un instrument/observator complet separat sau prin analiză de laborator a eșantioanelor aduse pe Pământ (Sample Return).",
        "Analiză multi-tehnologie în laboratoare ultra-curate pe Pământ (ex. Mars Sample Return)."
    )
}

data class ColdStepStatus(
    val level: ColdLevel,
    val isAchieved: Boolean,
    val statusSummary: String,
    val evidenceDetails: String
)

data class RealTelemetryPoint(
    val xLabel: String,
    val xValue: Double, // Wavelength in microns, Raman shift in cm-1, or Frequency in GHz
    val signalIntensity: Double, // Flux, Relative Raman intensity, or Power spectral density
    val baselineNoise: Double,
    val isBiomarkerPeak: Boolean = false,
    val moleculeName: String? = null
)

data class ExtraterrestrialTarget(
    val id: String,
    val targetName: String,
    val domain: LifeDetectionDomain,
    val missionFacility: String, // e.g. "NASA Mars 2020 Perseverance Rover (SHERLOC)"
    val locationOrCoordinates: String, // e.g. "Craterul Jezero, Neretva Vallis (Marte)"
    val detectedMolecules: List<AtmosphericMolecule>,
    val primaryCandidateSignal: String, // e.g. "Inele aromatice organice cu reacții de reducere a fierului și fosfatului (Pete Leopard)"
    val currentColdLevel: ColdLevel,
    val confidencePercent: Double, // 0 - 100%
    val detectionSigma: Double, // e.g. 5.2σ
    val scientificExplanation: String,
    val whyConfirmationsNeeded: String, // Explicit answers to user's question about why confirmations are required
    val nextConfirmationStep: String, // e.g. "Campania Mars Sample Return (NASA/ESA) - recuperarea carotei #24"
    val officialNasaSourceDoi: String, // Real official NASA announcement / DOI / peer-reviewed paper
    val observationDateOrSol: String, // e.g. "Sol 1198 (21 Iulie 2024)"
    val coldSteps: List<ColdStepStatus>,
    val spectralProfile: List<RealTelemetryPoint>,
    val spectralAxisUnit: String // "μm" for IR, "cm⁻¹" for Raman, "GHz" for Radio
)

data class ActiveTelescopeTelemetry(
    val telescopeName: String,
    val agency: String,
    val status: String,
    val currentTarget: String,
    val primaryAstrobiologyInstrument: String,
    val activeWavelengthBand: String,
    val isLiveConnected: Boolean
)

data class LiveAstrobiologySyncResult(
    val isSyncing: Boolean = false,
    val totalLiveTargetsQueried: Int = 0,
    val apiEndpointUsed: String = "NASA Exoplanet Archive TAP (TAP/sync)",
    val httpStatusCode: Int = 200,
    val statusMessage: String = "Gata pentru interogare live a telescoapelor și arhivelor NASA / MAST.",
    val lastSyncTimestampMs: Long = 0L,
    val liveQueriedExoplanets: List<HabitableZoneLivePlanet> = emptyList()
)

data class HabitableZoneLivePlanet(
    val planetName: String,
    val hostStar: String,
    val equilibriumTempK: Double,
    val planetRadiusEarth: Double,
    val insolationEarthUnits: Double,
    val discoveryFacility: String,
    val distanceParsecs: Double,
    val earthSimilarityIndex: Double,
    val astrobiologyPriorityRank: String
)
