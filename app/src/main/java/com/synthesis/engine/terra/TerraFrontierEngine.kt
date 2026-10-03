package com.synthesis.engine.terra

import com.synthesis.domain.model.terra.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TerraFrontierEngine @Inject constructor() {

    companion object {
        const val SURFACE_OCEANS_MASS_KG = 1.4e21       // Total mass of all Earth surface oceans (~1.4 billion km³)
        const val MANTLE_TRANSITION_ZONE_MASS_KG = 1.4e23 // Mass of 410-660 km mantle transition zone
        const val STANDARD_ATMOSPHERE_PA = 101325.0
        const val GRAVITY_ACCELERATION = 9.80665
        const val SEAWATER_DENSITY_SURFACE = 1025.0      // kg/m³
    }

    /**
     * Computes the exact hydrostatic pressure at abyssal and hadal depths,
     * accounting for seawater adiabatic compressibility at extreme depths.
     */
    fun computeHadalPressure(depthMeters: Int): Double {
        if (depthMeters <= 0) return 1.0
        // Compressive density increase in hadal trenches (~1025 kg/m³ at surface to ~1070 kg/m³ at 11,000m)
        val meanDensity = SEAWATER_DENSITY_SURFACE * (1.0 + (depthMeters / 1000.0) * 0.0042)
        val pressurePascals = meanDensity * GRAVITY_ACCELERATION * depthMeters
        return (pressurePascals / STANDARD_ATMOSPHERE_PA)
    }

    /**
     * Calculates the deep-mantle water budget stored inside hydrous wadsleyite and ringwoodite minerals,
     * and compares it against all surface oceans combined.
     */
    fun computeMantleHydrationBudget(waterWeightPercent: Double = 1.5): MantleWaterHydrationState {
        val clampedPercent = waterWeightPercent.coerceIn(0.1, 3.0)
        // Transition zone spans 410 to 660 km depth
        val estimatedWaterMassKg = MANTLE_TRANSITION_ZONE_MASS_KG * (clampedPercent / 100.0)
        val ratioToSurfaceOceans = estimatedWaterMassKg / SURFACE_OCEANS_MASS_KG

        // Subduction water pump delivers hydrous serpentinized oceanic slabs into mantle
        // ~1.5 to 2.0 x 10^8 kg/year subducted, balancing volcanic degassing through arc volcanism
        val subductionRateMtYear = 180.0 * (clampedPercent / 1.5)

        return MantleWaterHydrationState(
            mineralPhase = "Ringwoodit (γ-olivină) & Wadsleyit (β-fază)",
            depthRangeKm = "410 km - 660 km (Zona de Tranziție)",
            hydrationCapacityWeightPercent = clampedPercent,
            estimatedWaterMassKg = estimatedWaterMassKg,
            ratioToSurfaceOceans = (ratioToSurfaceOceans * 100.0).roundToInt() / 100.0,
            subductionWaterPumpRateMtYear = subductionRateMtYear
        )
    }

    /**
     * Simulates the geomagnetic field strength, South Atlantic Anomaly decay,
     * and rapid North Magnetic Pole drift speed for a target year.
     */
    fun simulateGeomagneticDecayAndDrift(year: Int): GeomagneticAnomalousState {
        val yearOffset = (year - 2026).coerceIn(-50, 50)
        // Historical baseline: ~22.4 µT in 2026, decaying at ~0.8 µT per decade in SAA core
        val currentIntensity = (22.4 - (yearOffset * 0.08)).coerceAtLeast(14.0)
        val driftKmPerYear = (48.0 + (yearOffset * 0.3)).coerceIn(30.0, 75.0)

        val riskLevel = when {
            currentIntensity < 20.0 -> "CRITICĂ EXTREMĂ (Pătrundere masivă Van Allen)"
            currentIntensity < 23.0 -> "ALERTĂ ÎNALTĂ (Resetare sateliți LEO & Hubble)"
            else -> "MODERATĂ (Monitorizare ESA Swarm)"
        }

        return GeomagneticAnomalousState(
            regionName = "Anomalia Atlanticului de Sud (Bazinul Brazilia-Africa)",
            currentIntensityMicroTesla = (currentIntensity * 10.0).roundToInt() / 10.0,
            intensityDecayRatePerDecadePercent = 7.5,
            northMagneticDriftKmPerYear = (driftKmPerYear * 10.0).roundToInt() / 10.0,
            lowEarthOrbitRadiationRisk = riskLevel,
            isSplittingIntoDoubleCore = true // Confirmed by ESA Swarm constellation: western and eastern sub-lobes
        )
    }

    /**
     * Returns the curated catalog of Hadal Trenches and abyssal oceanic frontiers.
     */
    fun getHadalTrenchCatalog(): List<HadalTrenchProfile> {
        return listOf(
            HadalTrenchProfile(
                name = "Groapa Marianelor (Challenger Deep)",
                location = "Pacificul de Vest (Insulele Mariane)",
                maxDepthMeters = 10994,
                pressureAtmospheres = computeHadalPressure(10994),
                unmappedFloorPercent = 82.0,
                temperatureCelsius = 1.4,
                chemicalEnergySource = "Serpentinizare abiotice de peridotite & emanare H2",
                primaryEndemicOrganisms = listOf("Amfipodul gigant Alicella", "Peștele melc Mariana (Pseudoliparis swirei)", "Xenofiofore gigantice unicelulare")
            ),
            HadalTrenchProfile(
                name = "Groapa Tonga (Horizon Deep)",
                location = "Pacificul de Sud-Vest",
                maxDepthMeters = 10882,
                pressureAtmospheres = computeHadalPressure(10882),
                unmappedFloorPercent = 88.0,
                temperatureCelsius = 1.1,
                chemicalEnergySource = "Metanogeneză geotermală & sedimente vulcanice",
                primaryEndemicOrganisms = listOf("Echinoderme hadale pelagice", "Bacterii piezofile barotolerante", "Anemone abisale de presiune extremă")
            ),
            HadalTrenchProfile(
                name = "Groapa Kermadec",
                location = "Nordul Noii Zeelande",
                maxDepthMeters = 10047,
                pressureAtmospheres = computeHadalPressure(10047),
                unmappedFloorPercent = 85.0,
                temperatureCelsius = 1.2,
                chemicalEnergySource = "Curenți de turbiditate hidrotermali",
                primaryEndemicOrganisms = listOf("Peștele melc Kermadec (Notoliparis kermadecensis)", "Crustacee necrofage hadale")
            ),
            HadalTrenchProfile(
                name = "Groapa Kuril-Kamchatka",
                location = "Pacificul de Nord-Vest",
                maxDepthMeters = 9600,
                pressureAtmospheres = computeHadalPressure(9600),
                unmappedFloorPercent = 89.0,
                temperatureCelsius = 1.5,
                chemicalEnergySource = "Flux dens de carbon organic dizolvat",
                primaryEndemicOrganisms = listOf("Izopode gigante abisale", "Holoturii hadale bioluminescente")
            ),
            HadalTrenchProfile(
                name = "Groapa Puerto Rico (Milwaukee Deep)",
                location = "Oceanul Atlantic de Nord / Marea Caraibilor",
                maxDepthMeters = 8376,
                pressureAtmospheres = computeHadalPressure(8376),
                unmappedFloorPercent = 75.0,
                temperatureCelsius = 2.0,
                chemicalEnergySource = "Fisuri hidrotermale de metan și sulfiți",
                primaryEndemicOrganisms = listOf("Bureți hexactinelizi de sticlă", "Polichete tubulare chemotrofe")
            ),
            HadalTrenchProfile(
                name = "Groapa Java / Sunda",
                location = "Oceanul Indian de Est",
                maxDepthMeters = 7290,
                pressureAtmospheres = computeHadalPressure(7290),
                unmappedFloorPercent = 91.0,
                temperatureCelsius = 2.4,
                chemicalEnergySource = "Rupturi de subducție de megatrust (Cutremure abisale)",
                primaryEndemicOrganisms = listOf("Caracatițe Dumbo de adâncime", "Meduze hadale de câmp magnetic")
            )
        )
    }

    /**
     * Curated catalog of profound Earth frontier mysteries, misconceptions,
     * and recent breakthrough discoveries.
     */
    fun getFrontierMysteries(): List<TerraFrontierMystery> {
        return listOf(
            TerraFrontierMystery(
                id = "hadal_uncharted",
                title = "Oceanele Hadale: Peste 80% din Fundul Planetar e Necartografiat",
                domain = TerraDomain.HADAL_OCEANS,
                misconception = "„Fundul oceanelor este plat, bine cartografiat de sateliți și în mare parte un deșert biologic lipsit de viață.”",
                scientificDiscovery = "Avem hărți topografice mai detaliate ale Lunii și planetei Marte decât ale propriilor oceane terestre! Sateliții măsoară doar anomalii gravitaționale de suprafață; relieful fin (canioane abisale, lanțuri muntoase submarine mai lungi de 65.000 km) este ascuns sub 4-11 km de apă. În gropile hadale, viața proliferează fără lumină prin chimiosinteză pe bază de sulfură de hidrogen și serpentinizare minerală.",
                physicalPrinciple = "Presiune hidrostatică P = ρ·g·h (~1.100 atm) & reacția de serpentinizare 3Fe2SiO4 + 2H2O -> 2Fe3O4 + 3SiO2 + 2H2",
                keyMetricLabel = "Seafloor Unmapped @ High Res",
                keyMetricValue = "81.4%",
                explorationReadinessPercent = 19,
                implicationsForLife = "Originea vieții pe Terra: primele celule biologice au apărut probabil în fisuri hidrotermale alcaline de adâncime, protejate de impacturile cosmice de la suprafață."
            ),
            TerraFrontierMystery(
                id = "mantle_water_reservoir",
                title = "Oceanul din Manta: Apa Ascunsă la 500 km Adâncime",
                domain = TerraDomain.DEEP_MANTLE_WATER,
                misconception = "„Toată apa planetei Terra se află la suprafață în oceane, calote glaciare și atmosferă.”",
                scientificDiscovery = "La adâncimi de 410–660 km, în Zona de Tranziție a mantalei, mineralele de înaltă presiune (Ringwoodit și Wadsleyit) stochează apă în rețeaua cristalină ca ioni de hidroxil (OH-). Un diamant adus din adâncuri în Brazilia conținea incluziuni de ringwoodit cu 1.5% apă. Volumul total de apă din mantaua Pământului depășește de 1.5 până la 3 ori toate oceanele de la suprafață combinate!",
                physicalPrinciple = "Solubilitatea H2O în rețeaua spinelică a ringwooditului Mg2SiO4 la presiuni de 14-24 GPa",
                keyMetricLabel = "Volum Apă Mantaua Terrei",
                keyMetricValue = "1.5x - 3.0x Toate Oceanele",
                explorationReadinessPercent = 28,
                implicationsForLife = "Subducția plăcilor tectonice pompează continuu apă în manta, iar erupțiile vulcanice o readuc la suprafață, menținând stabilitatea hidrosferei de 4 miliarde de ani."
            ),
            TerraFrontierMystery(
                id = "theia_blobs_llsvp",
                title = "Structurile LLSVP: Rămășițele Protoplanetei Theia în Adâncul Terrei",
                domain = TerraDomain.THEIA_LLSVP,
                misconception = "„Interiorul Pământului e format din straturi concentrice perfect omogene de rocă și fier topit.”",
                scientificDiscovery = "Tomografia seismică globală a descoperit două structuri colosale, de mărimea unor continente, situate la 2.900 km adâncime pe limita nucleu-manta (D'' layer): 'Tuzo' sub Africa și 'Jason' sub Pacific. Undele seismice încetinesc drastic traversându-le (Large Low-Shear-Velocity Provinces). Studii publicate în Nature (2023) arată că sunt resturile dense de manta ale protoplanetei Theia, care a lovit Terra acum 4.5 miliarde de ani.",
                physicalPrinciple = "Deficit de viteză a undelor de forfecare δ ln(Vs) de -2% până la -4% datorită îmbogățirii cu fier (Fe/Mg ratio ridicat)",
                keyMetricLabel = "Volum Structuri LLSVP",
                keyMetricValue = "~8% din Mantaua Terrei",
                explorationReadinessPercent = 34,
                implicationsForLife = "Marginile LLSVP ghidează coloanele ascendente de magmă (mantle plumes) care declanșează supervulcani și destrămarea supercontinentelor (Pangea, Rodinia)."
            ),
            TerraFrontierMystery(
                id = "south_atlantic_anomaly",
                title = "Anomalia Atlanticului de Sud & Deriva Accelerată a Polilor",
                domain = TerraDomain.GEOMAGNETIC_SAA,
                misconception = "„Scutul magnetic al Terrei este un dipol stabil și neschimbat care ne protejează uniform.”",
                scientificDiscovery = "Câmpul magnetic global a slăbit cu aproape 10% în ultimii 150 de ani. În Anomalia Atlanticului de Sud (SAA), câmpul e atât de redus încât particulele încărcate din centura Van Allen coboară la 200 km altitudine, defectând sateliții și astronauții ISS. Constelația de sateliți Swarm a arătat că anomalia se scindează în prezent în două centre. În plus, Polul Nord magnetic fuge cu 50 km/an din Canada spre Siberia din cauza competiției dintre două fluxuri de fier lichid în nucleul exterior.",
                physicalPrinciple = "Ecuația inducției magnetohidrodinamice în geodinam: ∂B/∂t = ∇×(v×B) + η∇²B",
                keyMetricLabel = "Intensitate Minimă SAA",
                keyMetricValue = "22.4 µT (față de 60 µT poli)",
                explorationReadinessPercent = 42,
                implicationsForLife = "Slăbirea continuă a scutului sporește expunerea rețelelor electrice, a zborurilor aeriene polare și a tehnologiei orbitale la furtuni geomagnetice de clasă Carrington."
            ),
            TerraFrontierMystery(
                id = "deep_biosphere_radiolysis",
                title = "Biosfera Profundă: Viața Subterană Hrănită de Radiație Nucleară",
                domain = TerraDomain.DEEP_BIOSPHERE,
                misconception = "„Fără fotosinteză și lumina Soarelui, viața nu poate exista pe Terra.”",
                scientificDiscovery = "Până la 5 km în adâncul scoarței continentale și oceanice trăiesc bacterii și arhee („Deep Life”), totalizând o biomasă de până la 23 miliarde de tone de carbon (de sute de ori mai mult decât întreaga omenire). Complet izolate de suprafață de milioane de ani, aceste organisme nu folosesc lumina, ci hidrogenul și sulfații produși prin radioliza apei generată de dezintegrarea uraniului, toriului și potasiului-40 din rocă.",
                physicalPrinciple = "Radioliza apei: H2O + α/β/γ -> e_aq, H•, OH• -> generare H2 + H2O2",
                keyMetricLabel = "Biomasă Subterană Estimată",
                keyMetricValue = "15 - 23 Gt Carbon",
                explorationReadinessPercent = 22,
                implicationsForLife = "Demonstrează că viața poate prospera în adâncurile reci sau fierbinți ale altor corpuri cerești (Marte, Europa, Enceladus) complet lipsite de lumină sau atmosferă."
            ),
            TerraFrontierMystery(
                id = "abyssal_heat_engine",
                title = "Transportorul Termic Abisal: Aerul Condiționat Planetar al Terrei",
                domain = TerraDomain.ABYSSAL_HEAT_ENGINE,
                misconception = "„Încălzirea planetei se petrece în principal în aerul atmosferic pe care îl respirăm.”",
                scientificDiscovery = "Peste 90% din întregul surplus de energie termică acumulat de Terra în ultimii 50 de ani nu se află în atmosferă, ci a fost absorbit de oceane, în special la adâncimi sub 2.000 metri. Curenții abisali (Circulația Termohalină AMOC și Apa de Fund Antarctică AABW) transportă această căldură spre poli. Schimbările de salinitate din topirea ghețarilor riscă să decupleze acest motor, cu consecințe climatice asimetrice majore.",
                physicalPrinciple = "Capacitatea calorică specifică masivă a apei (c = 4.184 J/g·K, de ~4 ori mai mare ca a aerului) & flux de flotabilitate densimetrică",
                keyMetricLabel = "Căldură Absorbită de Oceane",
                keyMetricValue = "> 90% din Total Planetar",
                explorationReadinessPercent = 38,
                implicationsForLife = "Fără amortizorul termic al oceanelor adânci, temperatura medie a atmosferei ar fi fost deja cu peste 20°C mai ridicată la suprafață."
            ),
            TerraFrontierMystery(
                id = "antarctica_subglacial_continent",
                title = "Antarctica Subglaciară: Continentul Îngropat Sub 4 Km de Gheață",
                domain = TerraDomain.ANTARCTIC_SUBGLACIAL,
                misconception = "„Antarctica este doar un bloc plat și steril de gheață fără relief sau viață subterană.”",
                scientificDiscovery = "Sub calota de 27 milioane km³ de gheață se ascunde un relief continental complex cartografiat prin radar de adâncime (BedMachine / Bedmap2): Munții Gamburtsev (creste de 2.700m îngropate complet sub 4.000m gheață), Canionul Denman (adânc de -3.500m sub nivelul mării - cel mai adânc canion continental de pe Terra) și o rețea de peste 400 de lacuri subglaciare cu apă lichidă complet izolate de 15-25 milioane de ani.",
                physicalPrinciple = "Presiunea litostatică a gheții (P = ρ_ice · g · h ≈ 350 atm) coboară punctul de topire al gheții + fluxul geotermal al scoarței (50-80 mW/m²)",
                keyMetricLabel = "Lacuri Subglaciare Descoperite",
                keyMetricValue = "> 400 Bazine Lichide",
                explorationReadinessPercent = 29,
                implicationsForLife = "Ecosistemele microbiene din lacurile Vostok și Mercer reprezintă cel mai precis analog terestru pentru explorarea oceanelor subglaciare de pe Europa și Enceladus."
            )
        )
    }

    /**
     * Returns the 4 pipeline stages for the Terra Incognita autonomous discovery engine.
     */
    fun getInitialPipelineStages(): List<PipelineStageItem> {
        return listOf(
            PipelineStageItem(
                stageNumber = 1,
                codeName = "TELEMETRY_INGESTION",
                title = "Ingestie Date Satelitare & Abisale",
                subtitle = "USGS Seismics, ESA Swarm, NOAA Bathymetry, Diamante Mantalice",
                status = PipelineStageStatus.COMPLETED,
                throughputRate = "4.2 MB/s live stream",
                processedSamples = 184520L,
                primaryMetric = "99.8% Calitate Semnal",
                details = listOf(
                    "Fuziune în timp real a datelor teleseismice de subducție adâncă",
                    "Extracție componente scalare câmp magnetic sateliți Swarm Alpha/Bravo",
                    "Integrare puncte sonice multibeam GEBCO 2030 (fose hadale)",
                    "Spectroscopie Raman a incluziunilor de ringwoodit din diamante ultra-adânci"
                )
            ),
            PipelineStageItem(
                stageNumber = 2,
                codeName = "TOMOGRAPHY_INVERSION",
                title = "Inversie Tomografică & Izolare Anomalii",
                subtitle = "LLSVPs Theia, nucleu SAA, ecuație adiabatică hadală",
                status = PipelineStageStatus.PROCESSING,
                throughputRate = "12.8 TFLOPS",
                processedSamples = 89340L,
                primaryMetric = "δ ln(Vs) = -3.4% deficit",
                details = listOf(
                    "Reconstrucție 3D a limitelor structurilor continentale 'Tuzo' și 'Jason'",
                    "Cartografierea ratei de decădere a intensității în nucleul dublu SAA",
                    "Compensare barică adiabatică: presiune depășește 1.086 atm la Challenger Deep",
                    "Identificarea zonelor de tranziție deshidratate din mantaua superioară"
                )
            ),
            PipelineStageItem(
                stageNumber = 3,
                codeName = "ASTROBIOLOGY_CROSS_CORRELATION",
                title = "Corelare Inter-Domenii & Astrobiologie Inversă",
                subtitle = "Radioliză subcrustală, serpentinizare H2, analogi exomantale",
                status = PipelineStageStatus.FLAGGED_ANOMALY,
                throughputRate = "68 corelații/sec",
                processedSamples = 41200L,
                primaryMetric = "+14.2 mol H2/m²·an flux",
                details = listOf(
                    "Bilanț bioenergetic independent de fotosinteză prin radioliza apei (U, Th, 40K)",
                    "Validare generare abiotică de metan și hidrogen pe fundul foselor hadale",
                    "Mapare analogi: transpunerea oceanului din mantaua Terrei la super-Pământuri TESS",
                    "Anomalie detectată: rată de acumulare a căldurii abisale accelerată sub 2000m"
                )
            ),
            PipelineStageItem(
                stageNumber = 4,
                codeName = "HYPOTHESIS_SYNTHESIS",
                title = "Sinteză Autonomă & Validare Ipoteze",
                subtitle = "Formulare teorii geofizice, brevetabilitate, certitudine Bayesiană",
                status = PipelineStageStatus.IDLE,
                throughputRate = "Certitudine 94.6%",
                processedSamples = 1250L,
                primaryMetric = "6 Ipoteze Majore Active",
                details = listOf(
                    "Ipoteza Theia-LLSVP confirmată gravitațional la 2.900 km adâncime",
                    "Modelul pompei hidraulice a mantalei ca stabilizator al hidrosferei",
                    "Rețea de monitorizare a riscului de scindare totală a anomaliei SAA",
                    "Export rapoarte științifice și date geospațiale pentru cercetători"
                )
            )
        )
    }

    /**
     * Real-time stream of synthetic and telemetry sensor events in Terra Incognita.
     */
    fun getRealtimeTelemetryFeeds(): List<TelemetryFeedItem> {
        return listOf(
            TelemetryFeedItem(
                timestamp = "ACUM 12s",
                sourceDomain = TerraDomain.HADAL_OCEANS,
                sourceName = "NOAA/GEBCO Sonar Array",
                eventMessage = "Groapa Tonga (Horizon Deep): semnal termochimic de serpentinizare intensificat la 10.880m",
                anomalyScore = 0.88,
                formulaBadge = "3Fe2SiO4 + 2H2O -> 2H2"
            ),
            TelemetryFeedItem(
                timestamp = "ACUM 48s",
                sourceDomain = TerraDomain.DEEP_MANTLE_WATER,
                sourceName = "USGS Subduction Sensor Net",
                eventMessage = "Placă oceanică subdusă sub arcul Kermadec: estimare flux apă +1.8 Mt/an la 520 km adâncime",
                anomalyScore = 0.74,
                formulaBadge = "Mg2SiO4:OH- (Ringwoodit)"
            ),
            TelemetryFeedItem(
                timestamp = "ACUM 2m",
                sourceDomain = TerraDomain.GEOMAGNETIC_SAA,
                sourceName = "ESA Swarm Constellation",
                eventMessage = "Anomalia SAA: intensitate scalară minimă înregistrată la 22.38 µT (scădere de 0.04 µT)",
                anomalyScore = 0.91,
                formulaBadge = "∂B/∂t = ∇×(v×B) + η∇²B"
            ),
            TelemetryFeedItem(
                timestamp = "ACUM 4m",
                sourceDomain = TerraDomain.THEIA_LLSVP,
                sourceName = "Global Seismic Tomography Net",
                eventMessage = "Structura 'Jason' (Pacific LLSVP): contrast de densitate de forfecare revalidat la granița D''",
                anomalyScore = 0.65,
                formulaBadge = "δ ln(Vs) = -3.4%"
            ),
            TelemetryFeedItem(
                timestamp = "ACUM 7m",
                sourceDomain = TerraDomain.DEEP_BIOSPHERE,
                sourceName = "ICDP Deep Crustal Borehole",
                eventMessage = "Radioliza apei detectată la 4.2 km adâncime: colonii arhee hiper-piezofile active",
                anomalyScore = 0.82,
                formulaBadge = "H2O + (U,Th) -> H2 + H2O2"
            )
        )
    }

    /**
     * Progress metrics of humanity's frontier exploration on Earth.
     */
    fun getFrontierProgressMetrics(): List<FrontierProgressMetric> {
        return listOf(
            FrontierProgressMetric(
                title = "Cartografiere Fund Oceanic Hadal (GEBCO 2030)",
                currentPercent = 0.186f,
                targetLabel = "18.6% Cartografiat (81.4% Necunoscut)",
                unitValue = "67.4 M km² rămași",
                badgeColorHex = 0xFF00E5FF
            ),
            FrontierProgressMetric(
                title = "Încredere Inversie Rezervor Apă Manta (Ringwoodit)",
                currentPercent = 0.842f,
                targetLabel = "84.2% Convergență Tomografică",
                unitValue = "~2.1 × 10²¹ kg H2O",
                badgeColorHex = 0xFF00E676
            ),
            FrontierProgressMetric(
                title = "Delimitare Masă Protoplanetă Theia (LLSVP D'')",
                currentPercent = 0.689f,
                targetLabel = "68.9% Limite Seismice Definite",
                unitValue = "~8% din Mantaua Terrei",
                badgeColorHex = 0xFFFF9100
            ),
            FrontierProgressMetric(
                title = "Cartografiere Scindare Nucleu Anomalia SAA",
                currentPercent = 0.915f,
                targetLabel = "91.5% Monitorizare Sateliți Swarm",
                unitValue = "2 Sub-centre active",
                badgeColorHex = 0xFFFF5252
            ),
            FrontierProgressMetric(
                title = "Cartografiere Topografie Subglaciară (BedMachine)",
                currentPercent = 0.724f,
                targetLabel = "72.4% Rezoluție Radar Înaltă",
                unitValue = "27.0 M km³ Gheață",
                badgeColorHex = 0xFF80DEEA
            )
        )
    }

    /**
     * Catalog of scientifically surveyed Antarctic subglacial lakes
     * based on NASA, British Antarctic Survey, and Roscosmos radar sounding data.
     */
    fun getAntarcticSubglacialLakes(): List<AntarcticSubglacialLake> {
        return listOf(
            AntarcticSubglacialLake(
                id = "lake_vostok",
                name = "Lacul Vostok",
                region = "Antarctica de Est (Platoul Polar)",
                iceThicknessMeters = 3769,
                estimatedWaterDepthMeters = 500,
                pressureAtmospheres = 355.0,
                isolationAgeYears = "15 - 25 Milioane Ani",
                extremaType = "Apă suprasaturată cu oxigen (50x apa de suprafață), întuneric total, bacterii chemolitotrofe",
                geothermalHeatFluxMwM2 = 54.0,
                researchStatus = "Sondat prin carote de gheață sterile (Vostok Core 5G)",
                scientificSignificance = "Cel mai mare lac subglaciar de pe Pământ (~15.790 km²). Analogul principal pentru oceanul de sub crusta de gheață a satelitului Europa (Jupiter)."
            ),
            AntarcticSubglacialLake(
                id = "lake_mercer",
                name = "Lacul Mercer",
                region = "Antarctica de Vest (Câmpia de Gheață Whillans)",
                iceThicknessMeters = 1067,
                estimatedWaterDepthMeters = 15,
                pressureAtmospheres = 105.0,
                isolationAgeYears = "Episodic (rețea hidrologică activă)",
                extremaType = "10.000 celule microbiene/ml apă, oxidare anaerobă de metan și fier",
                geothermalHeatFluxMwM2 = 88.0,
                researchStatus = "Penetrat cu foraj cu apă caldă sterilă (proiect SALSA)",
                scientificSignificance = "A demonstrat existența unui râu și a unui ciclu hidrologic activ la 1 km sub gheață, hrănind biosfera marină subcrustală."
            ),
            AntarcticSubglacialLake(
                id = "lake_whillans",
                name = "Lacul Whillans",
                region = "Bariera de Gheață Ross (subgheață)",
                iceThicknessMeters = 800,
                estimatedWaterDepthMeters = 2,
                pressureAtmospheres = 82.0,
                isolationAgeYears = "Drenaj periodic la fiecare 3 ani",
                extremaType = "Peste 3.900 specii de microorganisme filogenetice, consum de amoniu subglaciar",
                geothermalHeatFluxMwM2 = 110.0,
                researchStatus = "Mostră directă de apă și sediment prelevată de WISSARD",
                scientificSignificance = "Prima dovadă directă că patul de gheață al Antarcticii nu este steril, ci găzduiește ecosisteme dense fără energie solară."
            ),
            AntarcticSubglacialLake(
                id = "lake_concordia",
                name = "Lacul Concordia",
                region = "Dome C (Platoul Antarctic)",
                iceThicknessMeters = 4000,
                estimatedWaterDepthMeters = 200,
                pressureAtmospheres = 390.0,
                isolationAgeYears = "> 30 Milioane Ani",
                extremaType = "Temperatură minimă a gheții de suprafață: -80°C, apă lichidă bazală la -2°C",
                geothermalHeatFluxMwM2 = 45.0,
                researchStatus = "Cartografiat complet prin radar aerian (EPICA Dome C)",
                scientificSignificance = "Gheața de deasupra sa a furnizat arhiva continuă a climei Terrei pentru ultimii 800.000 de ani (concentrații CO2 și temperaturi glaciare)."
            ),
            AntarcticSubglacialLake(
                id = "lake_ellsworth",
                name = "Lacul Ellsworth",
                region = "Antarctica de Vest (Munții Ellsworth)",
                iceThicknessMeters = 3170,
                estimatedWaterDepthMeters = 150,
                pressureAtmospheres = 312.0,
                isolationAgeYears = "> 500.000 Ani",
                extremaType = "Canion tectonic îngropat, izolare topografică totală",
                geothermalHeatFluxMwM2 = 62.0,
                researchStatus = "Cartografiat seismic și radar de British Antarctic Survey",
                scientificSignificance = "Reper esențial pentru înțelegerea stabilității calotei glaciare vest-antarctice (WAIS) în perioadele interglaciare calde."
            )
        )
    }

    /**
     * Topographic and geologic bedrock structures hidden underneath the Antarctic ice sheet,
     * revealed by airborne radar sounding (BedMachine / Bedmap2).
     */
    fun getAntarcticBedrockFeatures(): List<AntarcticBedrockFeature> {
        return listOf(
            AntarcticBedrockFeature(
                id = "denman_canyon",
                name = "Canionul Subglaciar Denman",
                type = "Canion Abisal Subglaciar",
                bedrockElevationMeters = -3500, // Deepest subaerial canyon on Earth
                iceCoverThicknessMeters = 3500,
                locationDescription = "Antarctica de Est, sub Ghețarul Denman (Ținutul Reginei Mary)",
                discoveryMethod = "Modelare de conservare a masei radar BedMachine (Morlighem et al.)",
                isostaticReboundEstimateMeters = 980,
                description = "Cel mai adânc punct continental de pe Pământ care nu se află sub un ocean lichid (-3.500m sub nivelul mării, depășind cu mult Marea Moartă). Canionul are o lățime de 20 km și 100 km lungime, umplut complet cu gheață."
            ),
            AntarcticBedrockFeature(
                id = "gamburtsev_mountains",
                name = "Munții Subglaciari Gamburtsev",
                type = "Lanț Montan Îngropat",
                bedrockElevationMeters = 2700, // Peaks up to +2700m
                iceCoverThicknessMeters = 4000,
                locationDescription = "Inima Antarcticii de Est, sub Domul A",
                discoveryMethod = "Proiectul Internațional AGAP (gravimetrie și radar aerian)",
                isostaticReboundEstimateMeters = 750,
                description = "Un lanț muntos de dimensiunile Alpilor europeni (lungime 1.200 km, vârfuri de peste 2.700m) complet conservat și îngropat sub 4 kilometri de gheață. Relieful este impecabil păstrat, neafectat de eroziune fluvială de peste 34 milioane de ani."
            ),
            AntarcticBedrockFeature(
                id = "bentley_subglacial_trench",
                name = "Fosa Subglaciară Bentley",
                type = "Depresiune Tectonică Masivă",
                bedrockElevationMeters = -2540,
                iceCoverThicknessMeters = 3000,
                locationDescription = "Ținutul Marie Byrd, Antarctica de Vest",
                discoveryMethod = "Sondaj seismic de refracție realizat de Charles Bentley",
                isostaticReboundEstimateMeters = 820,
                description = "O depresiune tectonică imensă de mărimea Mexicului, coborând la -2.540m sub nivelul mării. Dacă gheața ar dispărea și marea ar pătrunde, ar forma o mare interioară uriașă până la revenirea izostatică a scoarței."
            ),
            AntarcticBedrockFeature(
                id = "wilkes_subglacial_basin",
                name = "Bazinul Subglaciar Wilkes",
                type = "Bazin Marin Subglaciar Tectonic",
                bedrockElevationMeters = -1200,
                iceCoverThicknessMeters = 2800,
                locationDescription = "Sectorul Wilkes Land, orientat spre Australia",
                discoveryMethod = "Anomalii gravimetrice GRACE & radar Bedmap2",
                isostaticReboundEstimateMeters = 540,
                description = "Bazin marin continental vast unde patul stâncos se înclină spre interiorul continentului (retrograd). Stochează un volum de gheață echivalent cu o ridicare a nivelului global al mărilor de 3-4 metri."
            )
        )
    }

    /**
     * Real polar altimetry and satellite metrics from NASA ICESat-2 (ATLAS) and ESA CryoSat-2.
     */
    fun getPolarAltimetryState(): PolarAltimetryState {
        return PolarAltimetryState(
            activeMission = "NASA ICESat-2 (ATLAS) & ESA CryoSat-2",
            sensorType = "Altimetru Laser Numărător de Fotoni (532 nm) & Radar SAR Interferometric",
            photonCountPerPulse = "10.000 pulsuri/sec (rezoluție orizontală de 70 cm)",
            totalIceVolumeKm3 = 27000000.0, // 27 million km³
            annualIceMassBalanceGtYear = -148.0, // Loss rate in Gt/year
            seaLevelEquivalentTotalMeters = 58.3, // If entire Antarctic ice sheet melted
            iceVelocityMaxMetersPerYear = 3800.0, // Pine Island / Thwaites glacier grounding lines
            radarPenetrationDepthMeters = 4200,
            lastCalibrationTime = "Date Calibrate Recent (Copernicus / NASA Distributed Active Archive)"
        )
    }

    /**
     * Computes the physical Isostatic Rebound (ridicarea crustală izostatică)
     * if the Antarctic ice sheet were to melt by a given percentage.
     * Principle: Δh_rebound = h_ice * (ρ_ice / ρ_mantle)
     * where ρ_ice ≈ 917 kg/m³, ρ_mantle ≈ 3300 kg/m³ (~27.8% of ice thickness).
     */
    fun computeIsostaticRebound(meltFraction: Float): IsostaticReboundSimulation {
        val clamped = meltFraction.coerceIn(0.0f, 1.0f)
        val totalIceKm3 = 27000000.0
        val lostIceKm3 = totalIceKm3 * clamped

        // 1 km³ ice ≈ 0.917 Gt. Total ice melt gives ~58.3m global sea level rise
        val seaLevelRiseMeters = 58.3 * clamped

        // Mean ice thickness over Antarctica is ~2,160 meters
        val meanIceThickness = 2160.0
        val maxReboundMeters = (meanIceThickness * 0.278) * clamped // up to ~600m mean, peaks >900m

        // Exposed land area that emerges above sea level as rebound proceeds (Antarctica is ~14M km²)
        val exposedArea = 14000000.0 * clamped

        return IsostaticReboundSimulation(
            iceMeltPercent = clamped,
            iceVolumeLostKm3 = lostIceKm3,
            globalSeaLevelRiseMeters = (seaLevelRiseMeters * 10).roundToInt() / 10.0,
            crustalReboundMaxMeters = (maxReboundMeters * 10).roundToInt() / 10.0,
            newExposedLandAreaKm2 = exposedArea
        )
    }
}

