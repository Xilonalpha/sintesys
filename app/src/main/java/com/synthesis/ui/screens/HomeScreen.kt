package com.synthesis.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.Investigation
import com.synthesis.domain.model.InvestigationStatus
import com.synthesis.service.worker.CelestialSyncInfo
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.HomeViewModel
import java.util.Locale

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
    val systemStatus by viewModel.systemStatus.collectAsState()
    val syncInfo by viewModel.syncInfo.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ModuleCategory.ALL) }

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

    // Filter modules by category and query
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
                                "Astrophysics & Ephemeris Sync Live",
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
                            android.widget.Toast.makeText(
                                context,
                                "Sincronizare date externe (NASA, MAST, Gaia, JPL) actualizată!",
                                android.widget.Toast.LENGTH_SHORT
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
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Acasă", style = MaterialTheme.typography.labelSmall) },
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
                shape = RoundedCornerShape(16.dp)
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. DYNAMIC CELESTIAL WORKMANAGER SYNC BAR (PRIMARY STATUS)
            item {
                CelestialSyncLiveBar(
                    syncInfo = syncInfo,
                    onTriggerSync = { viewModel.triggerLiveSync(context) }
                )
            }

            // 2. SLEEK SYSTEM STATUS ROW
            item {
                ModernSystemStatusBar(status = systemStatus)
            }

            // 3. CATALOG & QUICK DISCOVERY DIRECT ACCESS (SOLVES COMPLEX NAVIGATION)
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

            // 4. DISPLAY FILTERED MODULE CARDS
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

            // 5. ACTIVE INVESTIGATIONS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "INVESTIGAȚII ȘI ANALIZE ACTIVE",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (investigations.isNotEmpty()) {
                        Text(
                            "${investigations.size} Active",
                            color = AccentCyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (investigations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentCyan.copy(alpha = 0.1f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Nicio investigație în desfășurare",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Apasă pe butonul de mai jos pentru a iniția căutarea automată de exoplanete sau simularea unei curbe fotometrice.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onNewInvestigation,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentCyan),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inițiază o Investigație Nouă", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                items(investigations) { investigation ->
                    InvestigationCard(investigation = investigation, onClick = { onInvestigationClick(investigation.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun ModuleCatalogItemCard(module: AppModuleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = module.onClick),
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
            StatusPill(label = "ANALIZE", value = status.activeAnalyses.toString(), color = AccentAmber)
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
fun InvestigationCard(investigation: Investigation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (investigation.status) {
                            InvestigationStatus.ACTIVE -> AccentGreen
                            InvestigationStatus.COMPLETED -> AccentCyan
                            InvestigationStatus.FAILED -> AccentRed
                            else -> AccentAmber
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    investigation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    investigation.query,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Surse: ${investigation.sourcesConsulted} • Anomalii: ${investigation.anomaliesFound} • Scor: ${(investigation.confidenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
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
