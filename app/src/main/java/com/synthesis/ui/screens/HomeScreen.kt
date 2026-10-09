package com.synthesis.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.*
import com.synthesis.service.worker.CelestialSyncInfo
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.util.Locale

enum class DashboardSectionTab(val label: String, val icon: String) {
    CENTRAL_DASHBOARD("Panou Central & Descoperiri", "🚀"),
    MODULE_CATALOG("Catalog Toate Modulele (23)", "🔬")
}

enum class ModuleCategory(val label: String, val icon: String) {
    ALL("Toate", "🔬"),
    TELESCOPES("Telescoape & Cer", "🔭"),
    EXOPLANETS("Exoplanete & Tranzit", "🪐"),
    SPECTROSCOPY("Spectroscopie & Fizică", "🧪"),
    COSMOLOGY("Cosmologie & Terra", "🌌")
}

data class AppModuleItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val category: ModuleCategory,
    val badge: String? = null,
    val is2026New: Boolean = false,
    val onClick: () -> Unit
)

data class ResearchProjectPreset(
    val title: String,
    val query: String,
    val domain: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onInvestigationClick: (String) -> Unit,
    onNewInvestigation: () -> Unit,
    onViewExoplanets: () -> Unit,
    onViewXilonDiscovery: () -> Unit = {},
    onViewDeepHunt: () -> Unit = {},
    onViewAdvancedLab: (String?) -> Unit = {},
    onViewTerraFrontier: () -> Unit = {},
    onViewResearchArchive: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val investigations by viewModel.investigations.collectAsState()
    val discoveries by viewModel.discoveries.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val syncInfo by viewModel.syncInfo.collectAsState()
    val context = LocalContext.current

    var selectedSectionTab by remember { mutableStateOf(DashboardSectionTab.CENTRAL_DASHBOARD) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ModuleCategory.ALL) }

    // Initiation card local state
    var projectTitleInput by remember { mutableStateOf("") }
    var projectQueryInput by remember { mutableStateOf("") }
    var selectedProjectDomain by remember { mutableStateOf("Astrophysics") }
    var isInitiationExpanded by remember { mutableStateOf(false) }

    val researchPresets = remember {
        listOf(
            ResearchProjectPreset(
                title = "WASP-39 b: CO₂ & H₂O Inversiune Spectroscopică",
                query = "Ce corelații există între banda CO₂ la 4.3µm pe JWST NIRSpec și îmbogățirea cu metale grele a atmosferei?",
                domain = "Astrophysics",
                icon = "🪐"
            ),
            ResearchProjectPreset(
                title = "TESS Sector 74: Scanare Mono-Tranzit BLS",
                query = "Identifică semnale mono-tranzit cu durată >12h și estimează semiaxa mare minimă a candidaților.",
                domain = "Exoplanet Science",
                icon = "⚡"
            ),
            ResearchProjectPreset(
                title = "Anomalia SAA: Câmp Magnetic vs. Radiație LEO",
                query = "Analizează corelația dintre scăderea intensității dipolului terestru în Atlanticul de Sud și fluxul de protoni de înaltă energie.",
                domain = "Geomagnetism",
                icon = "🌍"
            ),
            ResearchProjectPreset(
                title = "Gaură Neagră Kerr: Frecvențe QPO & Rază ISCO",
                query = "Modelează oscilațiile cvasipolifrecvență (QPO) în discul de acreție și derivă parametrul de spin adimensional a*.",
                domain = "Relativity",
                icon = "🕳️"
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadSyncInfo(context)
    }

    // Complete catalog of all modules in the application
    val allModules = remember(onViewAdvancedLab, onViewXilonDiscovery, onViewDeepHunt, onViewExoplanets, onViewTerraFrontier, onViewResearchArchive) {
        listOf(
            // 1. EXOPLANET DISCOVERY & TRANSIT
            AppModuleItem(
                id = "xilon_ai",
                title = "Xilon AI: Detecție Autonomă",
                subtitle = "Algoritm BLS, modelare Mandel-Agol și chat astrofizic Gemini 3.5 Flash",
                icon = "✨",
                category = ModuleCategory.EXOPLANETS,
                badge = "AI LIVE",
                is2026New = true,
                onClick = onViewXilonDiscovery
            ),
            AppModuleItem(
                id = "deep_hunt",
                title = "Deep Hunt: Blindspots TESS",
                subtitle = "Mono-tranzite cu perioadă lungă & exoplanete din sectoare neacoperite",
                icon = "⚡",
                category = ModuleCategory.EXOPLANETS,
                badge = "TESS",
                is2026New = true,
                onClick = onViewDeepHunt
            ),
            AppModuleItem(
                id = "fits_summary",
                title = "FITS Header & Curbe de Lumină",
                subtitle = "Metadate observator, slider SNR dinamic în timp real și export rapoarte PDF",
                icon = "📊",
                category = ModuleCategory.EXOPLANETS,
                badge = "FITS + PDF",
                is2026New = true,
                onClick = { onViewAdvancedLab("FITS_HEADER_SUMMARY") }
            ),
            AppModuleItem(
                id = "nasa_archive",
                title = "Catalog Exoplanete NASA (TAP)",
                subtitle = "Explorare baze de date NASA Exoplanet Archive, Kepler, K2 și TESS",
                icon = "🪐",
                category = ModuleCategory.EXOPLANETS,
                badge = "NASA TAP",
                onClick = onViewExoplanets
            ),
            AppModuleItem(
                id = "live_photometry",
                title = "Fotometrie Live & Raportare Oficială",
                subtitle = "Extracție flux fotometric, calibrare diferențială și export AAVSO / CAS (ETD)",
                icon = "📉",
                category = ModuleCategory.EXOPLANETS,
                onClick = { onViewAdvancedLab("LIVE_PHOTOMETRY") }
            ),
            AppModuleItem(
                id = "gaia_rescue",
                title = "Salvare Fals-Pozitive (Gaia De-Blender)",
                subtitle = "Separare fotometrică stele de fundal blenduite și validare tranzite",
                icon = "🎯",
                category = ModuleCategory.EXOPLANETS,
                onClick = { onViewAdvancedLab("GAIA_RESCUE") }
            ),
            AppModuleItem(
                id = "harmonic_stitcher",
                title = "Multi-Sector Harmonic Stitcher",
                subtitle = "Cusătură armonică între sectoare fotometrice TESS multiple",
                icon = "🔄",
                category = ModuleCategory.EXOPLANETS,
                onClick = { onViewAdvancedLab("HARMONIC_STITCHER") }
            ),

            // 2. TELESCOPES & SKY
            AppModuleItem(
                id = "smart_scope",
                title = "Smart Scope Sync (Seestar & DWARF)",
                subtitle = "Protocol auto-discovery rețea, ASCOM Alpaca și telemetrie montură",
                icon = "📡",
                category = ModuleCategory.TELESCOPES,
                badge = "HARDWARE",
                is2026New = true,
                onClick = { onViewAdvancedLab("SMART_SCOPE") }
            ),
            AppModuleItem(
                id = "telescope_goto",
                title = "Control Montură Telescop GoTo",
                subtitle = "Comenzi fizice motoare pas-cu-pas, slew rate și sincronizare coordonate RA/DEC",
                icon = "🔭",
                category = ModuleCategory.TELESCOPES,
                onClick = { onViewAdvancedLab("TELESCOPE_GOTO") }
            ),
            AppModuleItem(
                id = "sky_radar",
                title = "Radar Cer & Numărătoare Tranzite",
                subtitle = "Ephemeride GPS locale: orare de intrare/ieșire din tranzit și vizibilitate",
                icon = "🛰️",
                category = ModuleCategory.TELESCOPES,
                badge = "GPS LIVE",
                onClick = { onViewAdvancedLab("SKY_RADAR") }
            ),
            AppModuleItem(
                id = "ar_sky_camera",
                title = "AR Sky Camera (Realitate Augmentată)",
                subtitle = "Harta cerului suprapusă peste camera foto folosind giroscopul și busola",
                icon = "📷",
                category = ModuleCategory.TELESCOPES,
                onClick = { onViewAdvancedLab("AR_SKY_CAMERA") }
            ),
            AppModuleItem(
                id = "telescope_vision",
                title = "Viziune Artificială Ghidaj Telescop",
                subtitle = "Focalizare automată (FWHM), detectare centroid stea de ghidaj pe senzor",
                icon = "👁️",
                category = ModuleCategory.TELESCOPES,
                onClick = { onViewAdvancedLab("TELESCOPE_VISION") }
            ),

            // 3. SPECTROSCOPY & PLANETARY PHYSICS
            AppModuleItem(
                id = "jwst_spectroscopy",
                title = "Spectroscopie Transmisie JWST",
                subtitle = "Spectre NIRSpec & NIRISS: profile de absorbție H₂O, CO₂, CH₄ și CO",
                icon = "🌈",
                category = ModuleCategory.SPECTROSCOPY,
                badge = "JWST",
                is2026New = true,
                onClick = { onViewAdvancedLab("JWST_SPECTROSCOPY") }
            ),
            AppModuleItem(
                id = "climate_simulator",
                title = "Simulator Climateric 1D Exoplanete",
                subtitle = "Model radiativ-convectiv Manabe-Wetherald: insolație, CO₂ și albedo",
                icon = "☀️",
                category = ModuleCategory.SPECTROSCOPY,
                onClick = { onViewAdvancedLab("CLIMATE_SIMULATOR") }
            ),
            AppModuleItem(
                id = "biosignatures",
                title = "Astrobiologie & Tehnosemnături",
                subtitle = "Analiză biosfere, compoziție atmosferică disonantă și semnale narrowband",
                icon = "🧬",
                category = ModuleCategory.SPECTROSCOPY,
                onClick = { onViewAdvancedLab("BIOSIGNATURES") }
            ),
            AppModuleItem(
                id = "plato_astero",
                title = "Asteroseismologie Misiunea PLATO",
                subtitle = "Oscilații p-mode stelare (Δν, νmax), determinare masă, rază și vârstă stea",
                icon = "⭐",
                category = ModuleCategory.SPECTROSCOPY,
                badge = "ESA PLATO",
                is2026New = true,
                onClick = { onViewAdvancedLab("PLATO_ASTEROSEISMOLOGY") }
            ),
            AppModuleItem(
                id = "roman_microlensing",
                title = "Microlentile Nancy Grace Roman",
                subtitle = "Detecție planete vagaboande (rogue) prin curbarea gravitațională a luminii",
                icon = "🔍",
                category = ModuleCategory.SPECTROSCOPY,
                badge = "ROMAN",
                is2026New = true,
                onClick = { onViewAdvancedLab("ROMAN_MICROLENSING") }
            ),
            AppModuleItem(
                id = "exomoon_tidal",
                title = "Exoluni & Oceane Mareice Subglaciare",
                subtitle = "Disipare flux termic mareic și habitabilitate în oceane subglaciare",
                icon = "🌕",
                category = ModuleCategory.SPECTROSCOPY,
                badge = "EXOLUNI",
                is2026New = true,
                onClick = { onViewAdvancedLab("EXOMOON_TIDAL") }
            ),

            // 4. COSMOLOGY & EARTH SCIENCES
            AppModuleItem(
                id = "big_bang",
                title = "Modelul Cosmologic Big Bang (FLRW)",
                subtitle = "Metrica Friedmann-Lemaître, nucleosinteză primordială, CMB și epoca reionizării",
                icon = "💥",
                category = ModuleCategory.COSMOLOGY,
                badge = "COSMOLOGIE",
                is2026New = true,
                onClick = { onViewAdvancedLab("BIG_BANG_COSMOLOGY") }
            ),
            AppModuleItem(
                id = "black_holes",
                title = "Găuri Negre & Relativitate Kerr",
                subtitle = "Metrica Kerr rotativă, orizontul evenimentelor, orbita ISCO & raza umbrei",
                icon = "🕳️",
                category = ModuleCategory.COSMOLOGY,
                badge = "RELATIVITATE",
                is2026New = true,
                onClick = { onViewAdvancedLab("BLACK_HOLES") }
            ),
            AppModuleItem(
                id = "terra_frontier",
                title = "TERRA INCOGNITA: Deep Earth & Oceans",
                subtitle = "Anomalia Magnetică SAA, structura mantalei terestre și fosele oceanice hadale",
                icon = "🌍",
                category = ModuleCategory.COSMOLOGY,
                badge = "GEODINAMICĂ",
                is2026New = true,
                onClick = onViewTerraFrontier
            ),
            AppModuleItem(
                id = "space_weather",
                title = "Vreme Spațială & Magnetosferă",
                subtitle = "Indici Kp, vânt solar, furtuni geomagnetice și comprimare magnetosferică",
                icon = "🛡️",
                category = ModuleCategory.COSMOLOGY,
                badge = "NOAA SWPC",
                is2026New = true,
                onClick = { onViewAdvancedLab("SPACE_WEATHER") }
            ),
            AppModuleItem(
                id = "research_archive",
                title = "Arhivă Cercetări Offline (Room DB)",
                subtitle = "Bază de date locală persistentă pentru rapoarte de analiză și sinteze",
                icon = "💾",
                category = ModuleCategory.COSMOLOGY,
                badge = "ROOM DB",
                onClick = onViewResearchArchive
            )
        )
    }

    val filteredModules = remember(searchQuery, selectedCategory, allModules) {
        allModules.filter { item ->
            val matchesCategory = (selectedCategory == ModuleCategory.ALL) || (item.category == selectedCategory)
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                item.title.lowercase().contains(q) ||
                        item.subtitle.lowercase().contains(q) ||
                        (item.badge?.lowercase()?.contains(q) ?: false)
            }
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(AccentCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "SYNTHESIS",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = AccentCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "v1.6.3",
                                        color = AccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Multi-Domain Scientific Discovery & Synthesis Hub",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.triggerLiveSync(context)
                            viewModel.loadSyncInfo(context)
                            Toast.makeText(
                                context,
                                "Sincronizare date externe (NASA, MAST, Gaia, JPL) actualizată!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("top_refresh_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reîmprospătează date externe",
                            tint = AccentCyan
                        )
                    }
                    IconButton(
                        onClick = { onViewAdvancedLab(null) },
                        modifier = Modifier.testTag("top_lab_button")
                    ) {
                        Icon(Icons.Default.Biotech, contentDescription = "Discovery Lab", tint = AccentGreen)
                    }
                    IconButton(
                        onClick = onViewResearchArchive,
                        modifier = Modifier.testTag("top_archive_button")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Arhivă Cercetări", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = AccentCyan
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* already on home */ },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        indicatorColor = AccentCyan.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onViewAdvancedLab(null) },
                    icon = { Icon(Icons.Default.Biotech, contentDescription = "Lab", tint = AccentGreen) },
                    label = { Text("Lab & Cosmo", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewDeepHunt,
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Deep Hunt", tint = AccentAmber) },
                    label = { Text("Deep Hunt", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewXilonDiscovery,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Xilon", tint = AccentCyan) },
                    label = { Text("Xilon AI", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewExoplanets,
                    icon = { Icon(Icons.Default.Public, contentDescription = "Exoplanete", tint = TextSecondary) },
                    label = { Text("Exoplanete", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewInvestigation,
                containerColor = AccentCyan,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_new_investigation")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Investigation", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. DYNAMIC CELESTIAL WORKMANAGER SYNC BAR
            item {
                CelestialSyncLiveBar(
                    syncInfo = syncInfo,
                    onTriggerSync = { viewModel.triggerLiveSync(context) }
                )
            }

            // 2. SYSTEM TELEMETRY & LIVE CONNECTION COUNTERS
            item {
                ModernSystemStatusBar(status = systemStatus)
            }

            // 3. MAIN SECTION TAB SELECTOR: CENTRAL DASHBOARD vs MODULE CATALOG
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DashboardSectionTab.values().forEach { tab ->
                        val isSelected = selectedSectionTab == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSectionTab = tab },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AccentCyan else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tab.icon, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tab.label,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // VIEW A: CENTRAL DASHBOARD (INITIATE PROJECT + ACTIVE TASKS + DISCOVERIES)
            // =========================================================================
            if (selectedSectionTab == DashboardSectionTab.CENTRAL_DASHBOARD) {

                // A.1 INITIATE NEW RESEARCH PROJECT SECTION
                item {
                    InitiateResearchProjectCard(
                        isExpanded = isInitiationExpanded,
                        onToggleExpand = { isInitiationExpanded = !isInitiationExpanded },
                        titleInput = projectTitleInput,
                        onTitleChange = { projectTitleInput = it },
                        queryInput = projectQueryInput,
                        onQueryChange = { projectQueryInput = it },
                        selectedDomain = selectedProjectDomain,
                        onDomainChange = { selectedProjectDomain = it },
                        presets = researchPresets,
                        onSelectPreset = { preset ->
                            projectTitleInput = preset.title
                            projectQueryInput = preset.query
                            selectedProjectDomain = preset.domain
                            isInitiationExpanded = true
                        },
                        onLaunchProject = {
                            if (projectTitleInput.isNotBlank() && projectQueryInput.isNotBlank()) {
                                viewModel.initiateResearchProject(
                                    title = projectTitleInput.trim(),
                                    query = projectQueryInput.trim(),
                                    domain = selectedProjectDomain
                                ) { newId ->
                                    Toast.makeText(context, "Proiectul de cercetare a fost inițiat cu succes!", Toast.LENGTH_SHORT).show()
                                    projectTitleInput = ""
                                    projectQueryInput = ""
                                    isInitiationExpanded = false
                                    onInvestigationClick(newId)
                                }
                            } else {
                                Toast.makeText(context, "Completează titlul și întrebarea științifică a cercetării.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // A.2 ACTIVE SYNTHESIS TASKS SECTION
                item {
                    ActiveSynthesisTasksSection(
                        activeTasks = activeTasks,
                        onTaskClick = { onInvestigationClick(it.id) },
                        onNewTaskClick = { isInitiationExpanded = true }
                    )
                }

                // A.3 RECENT SCIENTIFIC DISCOVERIES SECTION
                item {
                    RecentDiscoveriesSection(
                        discoveries = discoveries,
                        onSeedBenchmark = {
                            viewModel.seedBenchmarkDiscoveries()
                            Toast.makeText(context, "Descoperiri de referință generate în baza de date!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

            } else {
                // =====================================================================
                // VIEW B: MODULE CATALOG & DIRECT NAVIGATION (23 MODULES)
                // =====================================================================
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "CATALOG COMPLET MODULE (${allModules.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    "Apasă pe orice modul pentru deschidere directă imediată",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Instant Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Caută modul (ex: FITS, JWST, Seestar, Gaia, GoTo, Kerr...)", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Caută", tint = AccentCyan, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Șterge", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ModuleCategory.values()) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text("${cat.icon} ${cat.label}", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentCyan,
                                        selectedLabelColor = Color.Black,
                                        containerColor = DarkSurface,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) AccentCyan else Color(0xFF1E293B)
                                    )
                                )
                            }
                        }
                    }
                }

                // Filtered Module Cards
                if (filteredModules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Niciun modul găsit pentru \"$searchQuery\"", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { searchQuery = ""; selectedCategory = ModuleCategory.ALL }) {
                                    Text("Afișează toate modulele", color = AccentCyan)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredModules) { module ->
                        ModuleCatalogItemCard(module = module)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// =============================================================================
// COMPONENT 1: INITIATE NEW RESEARCH PROJECT CARD
// =============================================================================
@Composable
fun InitiateResearchProjectCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    queryInput: String,
    onQueryChange: (String) -> Unit,
    selectedDomain: String,
    onDomainChange: (String) -> Unit,
    presets: List<ResearchProjectPreset>,
    onSelectPreset: (ResearchProjectPreset) -> Unit,
    onLaunchProject: () -> Unit
) {
    val domains = listOf("Astrophysics", "Exoplanet Science", "Atmospheric Spectroscopy", "Geomagnetism", "Relativity", "Astrobiology")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("initiate_research_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(AccentCyan, AccentGreen, AccentAmber))),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .padding(16.dp)
        ) {
            // Header Row with Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentCyan.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔬", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "INIȚIERE PROIECT DE CERCETARE",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Corelare cross-domain & sinteză de date din 15 surse",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onToggleExpand,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isExpanded) AccentAmber.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f),
                        contentColor = if (isExpanded) AccentAmber else AccentCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_initiate_button")
                ) {
                    Text(
                        if (isExpanded) "RESTRÂNGE ▲" else "DESCHIDE FORMULAR ▼",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Preset Chips (Always visible for instantaneous 1-tap initiation)
            Text(
                "ȘABLOANE RAPIDE DE CERCETARE (1-TAP):",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable { onSelectPreset(preset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(preset.icon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                preset.title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Expandable Detailed Form
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Title Field
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = onTitleChange,
                        label = { Text("Titlul Proiectului de Cercetare", fontSize = 12.sp) },
                        placeholder = { Text("ex: WASP-39 b: Inversiune Spectroscopică CO₂", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_project_title"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Query Field
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = onQueryChange,
                        label = { Text("Întrebare / Ipoteză Științifică de Investigat", fontSize = 12.sp) },
                        placeholder = { Text("Ce corelații există între spectroscopia JWST și compoziția atmosferică?", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_project_query"),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Domain Selector
                    Text("DOMENIU PRINCIPAL:", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(domains) { domain ->
                            val isSelected = selectedDomain == domain
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDomainChange(domain) },
                                label = { Text(domain, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Launch Button
                    Button(
                        onClick = onLaunchProject,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_launch_project")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "LANSEAZĂ PROIECTUL & RULARE PIPELINE SINTEZĂ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPONENT 2: ACTIVE SYNTHESIS TASKS SECTION
// =============================================================================
@Composable
fun ActiveSynthesisTasksSection(
    activeTasks: List<Investigation>,
    onTaskClick: (Investigation) -> Unit,
    onNewTaskClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SARCINI ACTIVE DE SINTEZĂ (${activeTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                "Monitorizare în Timp Real",
                color = AccentCyan,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTasks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("😴 Nicio sarcină de sinteză activă în acest moment", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Motoarele de analiză sunt în standby. Inițiază un proiect nou folosind formularul de mai sus.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNewTaskClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inițiază o Sarcină Nouă", fontSize = 11.sp)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeTasks.forEach { task ->
                    ActiveSynthesisTaskCard(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

@Composable
fun ActiveSynthesisTaskCard(
    task: Investigation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_item_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Task Status & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (task.status) {
                            InvestigationStatus.ACTIVE -> AccentGreen.copy(alpha = 0.2f)
                            InvestigationStatus.ANALYZING -> AccentAmber.copy(alpha = 0.2f)
                            InvestigationStatus.CORRELATING -> AccentMagenta.copy(alpha = 0.2f)
                            InvestigationStatus.COMPLETED -> AccentCyan.copy(alpha = 0.2f)
                            else -> Color(0xFF334155)
                        }
                    ) {
                        Text(
                            task.status.name,
                            color = when (task.status) {
                                InvestigationStatus.ACTIVE -> AccentGreen
                                InvestigationStatus.ANALYZING -> AccentAmber
                                InvestigationStatus.CORRELATING -> AccentMagenta
                                InvestigationStatus.COMPLETED -> AccentCyan
                                else -> TextSecondary
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                task.query,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            val progressVal = if (task.confidenceScore > 0f) task.confidenceScore else 0.65f
            LinearProgressIndicator(
                progress = { progressVal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentCyan,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🛰️ ${if (task.sourcesConsulted > 0) task.sourcesConsulted else 6} Surse Consultate",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "⚡ ${if (task.anomaliesFound > 0) task.anomaliesFound else 2} Anomalii",
                    color = AccentAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "🎯 ${(progressVal * 100).toInt()}% Încredere",
                    color = AccentGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =============================================================================
// COMPONENT 3: RECENT SCIENTIFIC DISCOVERIES SECTION
// =============================================================================
@Composable
fun RecentDiscoveriesSection(
    discoveries: List<Discovery>,
    onSeedBenchmark: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = AccentGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✨", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "DESCOPERIRI ȘTIINȚIFICE RECENTE (${discoveries.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            if (discoveries.isNotEmpty()) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Clasificate după Impact",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (discoveries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nicio descoperire înregistrată încă în arhivă", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rulați o sarcină de sinteză nouă sau generați descoperirile de referință benchmark din baza de date.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSeedBenchmark,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generează Descoperiri Demonstrative (Benchmark)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                discoveries.take(8).forEach { discovery ->
                    RecentDiscoveryDashboardCard(discovery = discovery)
                }
            }
        }
    }
}

@Composable
fun RecentDiscoveryDashboardCard(discovery: Discovery) {
    var isExpanded by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovery_card_${discovery.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type Badge + Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (discovery.discoveryType) {
                        DiscoveryType.NOVEL_CORRELATION -> AccentCyan.copy(alpha = 0.2f)
                        DiscoveryType.CAUSAL_LINK -> AccentGreen.copy(alpha = 0.2f)
                        DiscoveryType.PREDICTIVE_PATTERN -> AccentAmber.copy(alpha = 0.2f)
                        DiscoveryType.ANOMALY_CLUSTER -> AccentMagenta.copy(alpha = 0.2f)
                        else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        discovery.discoveryType.name.replace('_', ' '),
                        color = when (discovery.discoveryType) {
                            DiscoveryType.NOVEL_CORRELATION -> AccentCyan
                            DiscoveryType.CAUSAL_LINK -> AccentGreen
                            DiscoveryType.PREDICTIVE_PATTERN -> AccentAmber
                            DiscoveryType.ANOMALY_CLUSTER -> AccentMagenta
                            else -> Color(0xFF38BDF8)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = AccentGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Valoare: ${currencyFormat.format(discovery.estimatedValueUsd)}",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                discovery.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                discovery.description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Score Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScorePill(label = "NOUTATE", value = "${(discovery.noveltyScore * 100).toInt()}%", color = AccentCyan)
                Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0xFF1E293B)))
                ScorePill(label = "ÎNCREDERE", value = "${(discovery.confidenceScore * 100).toInt()}%", color = AccentAmber)
                Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0xFF1E293B)))
                ScorePill(label = "IMPACT", value = "${(discovery.impactScore * 100).toInt()}%", color = AccentGreen)
                Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0xFF1E293B)))
                ScorePill(label = "BREVET", value = "${(discovery.patentabilityScore * 100).toInt()}%", color = AccentMagenta)
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Domains
                    if (discovery.domains.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Domenii: ", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(discovery.domains.joinToString(", "), color = AccentCyan, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Hidden Pattern
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "PATTERN ASCUNS IDENTIFICAT:",
                                color = AccentAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                discovery.hiddenPattern.description,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Recommended actions
                    if (discovery.recommendedActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ACȚIUNI RECOMANDATE:",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        discovery.recommendedActions.forEach { act ->
                            Text("• $act", color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Expand / Collapse Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isExpanded) "Restrânge detaliile ▲" else "Explorează dovezi & acțiuni ▼",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScorePill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
    }
}

// =============================================================================
// REUSABLE AUXILIARY COMPONENTS
// =============================================================================
@Composable
fun ModuleCatalogItemCard(module: AppModuleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = module.onClick)
            .testTag("catalog_item_${module.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(module.icon, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        module.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (module.badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = AccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                module.badge,
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Deschide",
                tint = AccentCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ModernSystemStatusBar(status: HomeViewModel.SystemStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(label = "SURSE DATE", value = status.dataSources.toString(), color = AccentCyan)
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E293B)))
            StatusPill(label = "CONN. LIVE", value = status.liveConnections.toString(), color = AccentGreen)
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E293B)))
            StatusPill(label = "SARCINI", value = status.activeAnalyses.toString(), color = AccentAmber)
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E293B)))
            StatusPill(label = "ANOMALII", value = status.anomalies.toString(), color = AccentRed)
        }
    }
}

@Composable
fun StatusPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun CelestialSyncLiveBar(
    syncInfo: CelestialSyncInfo,
    onTriggerSync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("celestial_sync_live_bar"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.75f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛰️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "CELESTIAL & EPHEMERIS DYNAMIC SYNC",
                            color = AccentCyan,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "WorkManager Periodic (1h) • JPL & NASA TAP",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onTriggerSync,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentCyan.copy(alpha = 0.15f),
                        contentColor = AccentCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("sync_celestial_now_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SYNC NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text("LAST SYNC", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        Text(syncInfo.formattedLastSync, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text("SOLAR BODIES", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        Text("${syncInfo.syncedBodies} (Moon, Mars, Jup, Sat)", color = AccentGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text("GMST TIME", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        val gmstStr = if (syncInfo.gmstHours > 0) String.format(Locale.US, "%.2fh", syncInfo.gmstHours) else "Realtime"
                        Text(gmstStr, color = AccentAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
