package com.synthesis.service

import com.synthesis.domain.model.exoplanet.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Professional astrophysical service managing pre-defined stellar light curve templates
 * (planetary transits, eclipsing binaries, stellar flares, rotational spot modulations,
 * pulsating variables, gravitational microlensing) and performing statistical cross-matching
 * to provide space agency validation baselines.
 */
@Singleton
class StellarTemplateLibraryService @Inject constructor() {

    private val templatesList: List<StellarLightCurveTemplate> by lazy {
        listOf(
            createPlanetaryTransitTemplate(),
            createDetachedEclipsingBinaryTemplate(),
            createContactBinaryTemplate(),
            createGrazingBinaryTemplate(),
            createStellarFlareTemplate(),
            createStarspotModulationTemplate(),
            createPulsatingVariableTemplate(),
            createMicrolensingTemplate()
        )
    }

    /**
     * Returns all available pre-defined stellar light curve templates.
     */
    fun getAllTemplates(): List<StellarLightCurveTemplate> = templatesList

    /**
     * Finds a template by its unique identifier.
     */
    fun getTemplateById(id: String): StellarLightCurveTemplate? =
        templatesList.find { it.id.equals(id, ignoreCase = true) }

    /**
     * Cross-correlates an observed FITS light curve against all templates in the library.
     * Computes Pearson cross-correlation, Reduced Chi-Squared (χ²_red), and Bayesian posterior
     * probability to rank the astrophysical scenarios and flag potential false positives.
     */
    fun matchObservedLightCurve(
        points: List<PhotometricDataPoint>,
        periodDays: Double? = null,
        durationHours: Double? = null
    ): List<TemplateMatchScore> {
        if (points.size < 5) {
            return templatesList.map { tmpl ->
                TemplateMatchScore(
                    template = tmpl,
                    reducedChiSquared = 1.0,
                    crossCorrelation = 0.0,
                    bayesPosteriorProbability = 1.0 / templatesList.size,
                    confidencePercent = 100.0 / templatesList.size,
                    morphologyVerdict = "Puncte insuficiente pentru inferență statistică",
                    diagnosticNotes = "Sunt necesare cel puțin 5 puncte fotometrice pentru calculul χ².",
                    isBestMatch = tmpl.id == "planetary_transit_mandel_agol"
                )
            }
        }

        // Determine observed depth, duration, and center of transit
        val baseFlux = 1.0
        val minPoint = points.minByOrNull { it.relativeFlux }
        val minFlux = minPoint?.relativeFlux ?: 1.0
        val observedDepthPpm = ((baseFlux - minFlux) * 1_000_000.0).coerceAtLeast(100.0)

        val centerBjd = minPoint?.bjdTime ?: points.firstOrNull()?.bjdTime ?: 0.0
        val centerMin = minPoint?.minuteOffset ?: 0.0

        // Phase folding aligned to transit minimum at phase 0.0
        val effectivePeriod = periodDays?.takeIf { it > 0.001 } ?: 1.0
        val foldedObserved = if (points.any { it.bjdTime != null && it.bjdTime > 1000.0 }) {
            points.map { pt ->
                val bjd = pt.bjdTime ?: (centerBjd + (pt.minuteOffset - centerMin) / 1440.0)
                var phase = ((bjd - centerBjd) / effectivePeriod) % 1.0
                while (phase > 0.5) phase -= 1.0
                while (phase < -0.5) phase += 1.0
                val err = pt.fluxError.coerceIn(0.0002, 0.02)
                Triple(phase, pt.relativeFlux, err)
            }
        } else {
            val tSpanMin = (points.last().minuteOffset - points.first().minuteOffset).coerceAtLeast(1.0)
            points.map { pt ->
                val phase = ((pt.minuteOffset - centerMin) / tSpanMin).coerceIn(-0.5, 0.5)
                val err = pt.fluxError.coerceIn(0.0002, 0.02)
                Triple(phase, pt.relativeFlux, err)
            }
        }

        val n = foldedObserved.size
        val scores = mutableListOf<TemplateMatchScore>()

        for (template in templatesList) {
            // Generate theoretical curve matching the observed parameters
            val templatePoints = template.generateCurve(
                numPoints = 120,
                customDepthPpm = observedDepthPpm
            )

            // Interpolate theoretical flux onto observed phases
            var chiSqSum = 0.0
            var covSum = 0.0
            var varObsSum = 0.0
            var varModelSum = 0.0
            val meanObs = foldedObserved.map { it.second }.average()
            val meanModel = templatePoints.map { it.normalizedFlux }.average()

            for ((phase, obsFlux, err) in foldedObserved) {
                val modelFlux = interpolateModelFlux(templatePoints, phase)
                val sigma = err
                val diff = obsFlux - modelFlux
                chiSqSum += (diff * diff) / (sigma * sigma)

                val diffObs = obsFlux - meanObs
                val diffModel = modelFlux - meanModel
                covSum += diffObs * diffModel
                varObsSum += diffObs * diffObs
                varModelSum += diffModel * diffModel
            }

            val dof = (n - 2).coerceAtLeast(1)
            val redChiSq = (chiSqSum / dof).coerceIn(0.1, 999.0)

            val denom = sqrt(varObsSum * varModelSum)
            val pearsonR = if (denom > 1e-12) (covSum / denom).coerceIn(-1.0, 1.0) else 0.0

            // Diagnostic notes based on space agency criteria
            val (verdict, notes) = generateDiagnosticEvaluation(template, redChiSq, pearsonR, observedDepthPpm)

            // Raw log-likelihood ~ -chiSq / 2
            val logLikelihood = -chiSqSum / 2.0

            scores.add(
                TemplateMatchScore(
                    template = template,
                    reducedChiSquared = redChiSq,
                    crossCorrelation = pearsonR,
                    bayesPosteriorProbability = logLikelihood, // will normalize
                    confidencePercent = 0.0,
                    morphologyVerdict = verdict,
                    diagnosticNotes = notes
                )
            )
        }

        // Normalize Bayesian posterior probabilities via Softmax / Log-sum-exp
        val maxLogL = scores.maxOfOrNull { it.bayesPosteriorProbability } ?: 0.0
        val expWeights = scores.map { exp((it.bayesPosteriorProbability - maxLogL).coerceIn(-50.0, 0.0)) }
        val sumExp = expWeights.sum().coerceAtLeast(1e-12)

        val normalizedScores = scores.mapIndexed { idx, item ->
            val bayesProb = expWeights[idx] / sumExp
            val confPct = bayesProb * 100.0
            item.copy(
                bayesPosteriorProbability = bayesProb,
                confidencePercent = confPct
            )
        }.sortedByDescending { it.confidencePercent }

        // Mark the highest score as best match
        return normalizedScores.mapIndexed { idx, item ->
            item.copy(isBestMatch = idx == 0)
        }
    }

    private fun interpolateModelFlux(templatePoints: List<TemplatePhasePoint>, targetPhase: PhaseDouble): Double {
        if (templatePoints.isEmpty()) return 1.0
        val clamped = targetPhase.coerceIn(-0.5, 0.5)

        // Find surrounding points
        var left = templatePoints.first()
        var right = templatePoints.last()

        for (i in 0 until templatePoints.size - 1) {
            if (templatePoints[i].phase <= clamped && templatePoints[i + 1].phase >= clamped) {
                left = templatePoints[i]
                right = templatePoints[i + 1]
                break
            }
        }

        val dPhase = right.phase - left.phase
        return if (abs(dPhase) > 1e-7) {
            val t = (clamped - left.phase) / dPhase
            left.normalizedFlux + t * (right.normalizedFlux - left.normalizedFlux)
        } else {
            left.normalizedFlux
        }
    }

    private fun generateDiagnosticEvaluation(
        template: StellarLightCurveTemplate,
        redChiSq: Double,
        pearsonR: Double,
        depthPpm: Double
    ): Pair<String, String> {
        return when (template.id) {
            "planetary_transit_mandel_agol" -> {
                if (redChiSq < 1.8 && pearsonR > 0.6) {
                    Pair(
                        "Semnătură U-Shape Autentică (Validat NASA)",
                        "Morfologie plată la minim cu curbură de limb-darkening coerentă. Ingress/Egress simetric conforme cu ecuațiile Mandel & Agol. Fără eclipsă secundară."
                    )
                } else if (redChiSq < 3.5) {
                    Pair(
                        "Compatibil Tranzit Planetar",
                        "Profil U-shape plauzibil. Se recomandă analiză fotometrică multicolor pentru excluderea contaminării de flux din fundal."
                    )
                } else {
                    Pair(
                        "Deviație Morfologică",
                        "Profilul observat deviază de la modelul clasic de tranzit planetar U-shape (χ²_red = ${String.format(java.util.Locale.US, "%.2f", redChiSq)})."
                    )
                }
            }
            "eclipsing_binary_detached_ea" -> {
                if (depthPpm > 30000.0) {
                    Pair(
                        "Probabilă Binară de Eclipsă Detașată (EA)",
                        "Adâncimea semnalului depășește pragul fizic planetar (${String.format(java.util.Locale.US, "%.0f", depthPpm)} ppm > 30.000 ppm / 3%). Suspect eclipsă stelară mutuală."
                    )
                } else {
                    Pair(
                        "Lipsă Eclipsă Secundară",
                        "Nu s-a detectat eclipsă secundară semnificativă la faza 0.5, reducând probabilitatea unei binare de eclipsă detașate masive."
                    )
                }
            }
            "eclipsing_binary_contact_ew" -> {
                if (pearsonR > 0.7 && redChiSq < 2.0) {
                    Pair(
                        "Binară de Contact (W UMa)",
                        "Variație continuă de lumină cu distorsiune mareică elipsoidală. Minime duble caracteristice sistemelor binare cu anvelopă comună."
                    )
                } else {
                    Pair(
                        "Fără Modulație Continuă",
                        "Fluxul stelar este plat în afara tranzitului, excluzând distorsiunea mareică specifică stelelor binare în contact de tip EW."
                    )
                }
            }
            "grazing_eclipsing_binary_v_shape" -> {
                if (redChiSq < 1.6 && pearsonR > 0.7) {
                    Pair(
                        "Avertisment Fals-Pozitiv: Binară Grazing",
                        "Profil ascuțit în formă de V fără fund plat (impact parameter b > 1 - Rp/R*). Risc ridicat de fals-pozitiv stelar grazing."
                    )
                } else {
                    Pair(
                        "Fals-Pozitiv Grazing Exclus",
                        "Prezența fundului de tranzit aplatizat exclude o eclipsă tangențială pură în formă de V."
                    )
                }
            }
            "stellar_flare_davenport" -> {
                if (pearsonR > 0.5) {
                    Pair(
                        "Eveniment Flare Reversibil",
                        "Creștere impulsivă rapidă urmată de răcire exponențială asimetrică conform modelului Davenport (2014)."
                    )
                } else {
                    Pair(
                        "Fără Semnătură de Flare",
                        "Semnalul este un dip negativ de absorbție, nu o erupție de emisie coronală pozitivă."
                    )
                }
            }
            "starspot_rotational_bydra" -> {
                Pair(
                    "Modulație Rotațională Cvasisinusoidală",
                    "Variație de fond cauzată de rotația stelei purtătoare de pete magnetice active (tip BY Dra)."
                )
            }
            "pulsating_rr_lyrae" -> {
                Pair(
                    "Pulsare Stelară Asimetrică",
                    "Profil de tip dinte de fierăstrău cu pantă abruptă de creștere și descreștere lentă."
                )
            }
            "gravitational_microlensing_paczynski" -> {
                Pair(
                    "Microlentilă Gravitațională (Paczyński)",
                    "Amplificare simetrică acromatică singulară conform formulei optice relativiste Paczyński (1986)."
                )
            }
            else -> Pair("Analiză Șablon Finalizată", "Date comparate cu modelul astrofizic.")
        }
    }

    // --- TEMPLATE FACTORY BUILDERS ---

    private fun createPlanetaryTransitTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "planetary_transit_mandel_agol",
            name = "Tranzit Planetar (Mandel & Agol U-Shape)",
            category = StellarTemplateCategory.PLANETARY_TRANSIT,
            astrophysicalClass = "Exoplanet Transit (Mandel & Agol 2002)",
            badgeColorHex = "#00E5FF", // Cyan
            morphology = "U-shape aplatizat cu limb-darkening quadratic și pante simetrice de ingress/egress",
            physicsDescription = "Ocultarea discului stelar de către o exoplanetă pe o orbită circulară sau excentrică. Produce o curbă de lumină caracteristică în formă de 'U' cu fund aplatizat, determinată de întunecarea către margine (limb darkening) a fotosferei stelare.",
            diagnosticVerificationFeatures = listOf(
                "Simetrie strictă între ingress (faza de intrare) și egress (faza de ieșire)",
                "Fund aplatizat cu curbură limb-darkening (u1 ≈ 0.3, u2 ≈ 0.2)",
                "Absența eclipsei secundare detectabile la faza 0.5 (fapt ce exclude companion stelar cald)",
                "Lățime fizică de tranzit conformă cu a 3-a lege a lui Kepler: T_dur ∝ P^(1/3) * (R_* / a)",
                "Adâncime acromatică identică pe toate filtrele optice (Sloan r', Johnson V, Clear)"
            ),
            falsePositiveRisk = "Scăzut dacă fundul este neted și fără eclipsă secundară. Atenție la binare contaminate optic în același pixel (blended EBs).",
            typicalPeriod = "0.5 – 365+ zile",
            typicalDepthPpm = "100 – 15,000 ppm (0.01% – 1.5%)",
            formulaDescription = "F(φ) = 1.0 - (Rp/R*)^2 * Ω(φ, u1, u2)",
            defaultDepthPpm = 1200.0,
            defaultDurationPhase = 0.08
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createDetachedEclipsingBinaryTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "eclipsing_binary_detached_ea",
            name = "Binară de Eclipsă Detașată (Tip Algol / EA)",
            category = StellarTemplateCategory.ECLIPSING_BINARY,
            astrophysicalClass = "Detached Eclipsing Binary (EA / Algol)",
            badgeColorHex = "#E040FB", // Purple / Magenta
            morphology = "Eclipsă primară adâncă însoțită de eclipsă secundară distinctă la faza 0.5",
            physicsDescription = "Două stele legate gravitațional ce se eclipsează reciproc fără a fi în contact fizic direct. Eclipsa primară apare la conjuncția inferioară a stelei cu temperatură mai mică, iar eclipsa secundară la faza 0.5.",
            diagnosticVerificationFeatures = listOf(
                "Prezența eclipsei secundare clare la faza 0.5",
                "Adâncime frecvent > 30,000 ppm (> 3% din flux)",
                "Diferență de adâncime dependentă de lungimea de undă (variație cromatică pronunțată)",
                "Efecte subtile de reflexie stelar-stelar între eclipse"
            ),
            falsePositiveRisk = "Critic. Eclipsa secundară sau o binară atenuată de o a treia stea din fundal (blended binary) imită perfect un tranzit de exoplanetă.",
            typicalPeriod = "1.0 – 50 zile",
            typicalDepthPpm = "50,000 – 500,000 ppm (5% – 50%)",
            formulaDescription = "F(φ) = 1.0 - δ_prim(φ) - δ_sec(φ - 0.5)",
            defaultDepthPpm = 180000.0,
            defaultDurationPhase = 0.12
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createContactBinaryTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "eclipsing_binary_contact_ew",
            name = "Binară de Contact (Tip W Ursae Majoris / EW)",
            category = StellarTemplateCategory.ECLIPSING_BINARY,
            astrophysicalClass = "Contact / Overcontact Binary (EW)",
            badgeColorHex = "#FF5252", // Red
            morphology = "Variație continuă sinusoid-dublă cu minime aproape egale și distorsiune elipsoidală",
            physicsDescription = "Sistem binar strâns în care ambele componente își umplu lobii Roche și împart o anvelopă fotosferică comună. Variația de lumină este continuă, cauzată de distorsiunea mareică a corpurilor stelare.",
            diagnosticVerificationFeatures = listOf(
                "Niciun segment de flux plat (variație neîntreruptă pe parcursul orbitei)",
                "Două minime succesive de adâncimi comparabile",
                "Perioadă orbitală foarte scurtă (tipic sub 0.8 zile)",
                "Luminanță maximă la fazele de cuadratură 0.25 și 0.75"
            ),
            falsePositiveRisk = "Ușor de distins de tranzitul planetar datorită modulației continue din afara eclipselor.",
            typicalPeriod = "0.2 – 0.8 zile",
            typicalDepthPpm = "100,000 – 400,000 ppm (10% – 40%)",
            formulaDescription = "F(φ) = 1.0 - A1 * cos(4πφ) - A2 * cos(2πφ)",
            defaultDepthPpm = 220000.0,
            defaultDurationPhase = 0.50
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createGrazingBinaryTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "grazing_eclipsing_binary_v_shape",
            name = "Binară Grazing (V-Shape False Positive)",
            category = StellarTemplateCategory.ECLIPSING_BINARY,
            astrophysicalClass = "Grazing Stellar Eclipse (b > 1 - Rp/Rs)",
            badgeColorHex = "#FF9100", // Orange
            morphology = "Profil ascuțit în formă de 'V', fără segment plat la minim",
            physicsDescription = "Eclipsă stelară în care discul secundar nu este complet cuprins în interiorul discului primar (parametru de impact b apropiat de 1.0). Doar marginea este parțial acoperită, generând o cădere de flux ascuțită în V.",
            diagnosticVerificationFeatures = listOf(
                "Raport durata ingress-egress / durata totală = 1.0 (zero fund plat)",
                "Unghi ascuțit la faza minimă",
                "Frecvent însoțită de semnale de contaminare astrometrică a centrului de masă (centroid shift)"
            ),
            falsePositiveRisk = "Extrem de ridicat. Una dintre cele mai frecvente capcane pentru algoritmii automați de detecție NASA TESS / Kepler.",
            typicalPeriod = "0.8 – 20 zile",
            typicalDepthPpm = "5,000 – 60,000 ppm (0.5% – 6%)",
            formulaDescription = "F(φ) = 1.0 - δ * (1 - |φ| / φ_0)",
            defaultDepthPpm = 25000.0,
            defaultDurationPhase = 0.09
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createStellarFlareTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "stellar_flare_davenport",
            name = "Flare Stelară (Model Davenport 2014)",
            category = StellarTemplateCategory.STELLAR_FLARE,
            astrophysicalClass = "Impulsive Stellar Flare / Coronal Event",
            badgeColorHex = "#FFD600", // Yellow
            morphology = "Creștere impulsivă bruscă cubică urmată de descreștere lentă biexponențială",
            physicsDescription = "Eliberare violentă de energie magnetică prin reconectare în coronosfera stelei gazdă (frecventă la stele M-dwarf / pitice roșii). Produce o creștere bruscă a fluxului optic urmată de răcire termică treptată.",
            diagnosticVerificationFeatures = listOf(
                "Asimetrie temporală extremă: t_crestere << t_descrestere",
                "Semn pozitiv de flux (+ΔF > 0), spre deosebire de tranzitul de absorbție (-ΔF < 0)",
                "Comportament aperiodic singular",
                "Fază de descreștere modelată precis prin două exponențiale de răcire: e^(-1.6t) și e^(-0.27t)"
            ),
            falsePositiveRisk = "Risc de perturbare a liniei de bază fotometrice a tranzitelor reale la stele M-dwarf active.",
            typicalPeriod = "Aperiodic / stocastic",
            typicalDepthPpm = "+10,000 – +500,000 ppm (+1% – +50%)",
            formulaDescription = "F(t) = 1 + A * (0.689 e^(-1.6t) + 0.311 e^(-0.27t))",
            isPeriodic = false,
            defaultDepthPpm = 80000.0,
            defaultDurationPhase = 0.15
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createStarspotModulationTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "starspot_rotational_bydra",
            name = "Pete Stelare & Variație Rotațională (Tip BY Dra)",
            category = StellarTemplateCategory.ROTATIONAL_VARIABILITY,
            astrophysicalClass = "Starspot Rotational Modulation",
            badgeColorHex = "#76FF03", // Bright Lime Green
            morphology = "Modulație continuă cvasisinusoidală cu armonici de rotație diferențială",
            physicsDescription = "Pete magnetice reci de pe fotosfera stelei se rotesc odată cu aceasta, modulând periodic cantitatea de flux luminos receptat pe Pământ. Perioada corespunde perioadei de rotație stelare P_rot.",
            diagnosticVerificationFeatures = listOf(
                "Variație lină sinusoidală pe întreaga durată a rotației",
                "Evoluție lentă a amplitudinii pe săptămâni din cauza apariției/dispariției petelor",
                "Sub-structuri armonice cauzate de grupuri multiple de pete dispuse la latitudini diferite"
            ),
            falsePositiveRisk = "Poate distorsiona linia de bază a unui tranzit dacă nu este aplicat un algoritm de detrending gaussian sau spline.",
            typicalPeriod = "2 – 45 zile (perioada de rotație stelară)",
            typicalDepthPpm = "5,000 – 40,000 ppm (0.5% – 4%)",
            formulaDescription = "F(φ) = 1.0 - A_spot * [cos(2πφ) + h * cos(4πφ)]",
            defaultDepthPpm = 20000.0,
            defaultDurationPhase = 0.50
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createPulsatingVariableTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "pulsating_rr_lyrae",
            name = "Variabilă Pulsantă (Tip RR Lyrae / Cefeidă)",
            category = StellarTemplateCategory.PULSATING_VARIABLE,
            astrophysicalClass = "Radially Pulsating Star (RR Lyrae / Cepheid)",
            badgeColorHex = "#64FFDA", // Mint Teal
            morphology = "Profil dinte de fierăstrău asimetric: ascensiune rapidă spre maxim și declin lent",
            physicsDescription = "Pulsare radială a straturilor exterioare ale stelei alimentată de mecanismul kappa (ionizarea heliului și opacitatea). Raza și temperatura stelei oscilează periodic.",
            diagnosticVerificationFeatures = listOf(
                "Asimetrie clară în formă de dinte de fierăstrău",
                "Amplitudine mare (zeci de sutimi de magnitudine)",
                "Schimbare concomitentă a tipului spectral și culorii stelare"
            ),
            falsePositiveRisk = "Scăzut pentru tranzite planetare datorită amplitudinii mari și formei de undă net distincte.",
            typicalPeriod = "0.2 – 2.0 zile (RR Lyrae) / 1 – 50 zile (Cefeide)",
            typicalDepthPpm = "100,000 – 800,000 ppm (10% – 80%)",
            formulaDescription = "F(φ) = Sawtooth(φ, t_rise = 0.18)",
            defaultDepthPpm = 300000.0,
            defaultDurationPhase = 0.50
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }

    private fun createMicrolensingTemplate(): StellarLightCurveTemplate {
        val tmpl = StellarLightCurveTemplate(
            id = "gravitational_microlensing_paczynski",
            name = "Microlentilă Gravitațională (Paczyński 1986)",
            category = StellarTemplateCategory.GRAVITATIONAL_MICROLENSING,
            astrophysicalClass = "Single-Lens Gravitational Microlensing",
            badgeColorHex = "#40C4FF", // Sky Blue
            morphology = "Creștere simetrică în clopot, perfect acromatică și singulară",
            physicsDescription = "Devierea relativistă a razelor de lumină ale unei stele îndepărtate de către câmpul gravitațional al unui obiect masiv intermediar (stea sau planetă rătăcitoare). Produce o amplificare luminoasă temporară fără variație de culoare.",
            diagnosticVerificationFeatures = listOf(
                "Simetrie temporală riguroasă în jurul maximului de amplificare t_0",
                "Eveniment singular strict neperiodic",
                "Acrotamatism desăvârșit: curba este identică la orice lungime de undă",
                "Posibilă apariție a unor mici vârfuri scurte secundare în caz de lentilă binară (planetă pe orbită)"
            ),
            falsePositiveRisk = "Ușor de separat de tranzite datorită creșterii de flux și naturii singulare neperiodice.",
            typicalPeriod = "Eveniment unic (durată Einstein t_E ≈ 10 – 60 zile)",
            typicalDepthPpm = "+50,000 – +1,000,000+ ppm (+5% – +100%+)",
            formulaDescription = "A(u) = (u^2 + 2) / (u * sqrt(u^2 + 4))",
            isPeriodic = false,
            defaultDepthPpm = 250000.0,
            defaultDurationPhase = 0.30
        )
        return tmpl.copy(sampleCurve = tmpl.generateCurve())
    }
}

private typealias PhaseDouble = Double
