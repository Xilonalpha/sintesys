package com.synthesis.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

/**
 * Departamente științifice clare pentru organizarea intuitivă a meniului principal.
 */
enum class ScientificDepartment(val label: String, val icon: String, val subtitle: String) {
    EXOPLANETS("Exoplanete", "🪐", "Detecție tranzite, BLS & Arhive"),
    TELESCOPES("Observator", "🔭", "Smart Scopes, Monturi GoTo & Radar"),
    LABORATORY("Laborator", "🔬", "Spectroscopie JWST & Astrofizică"),
    COSMOLOGY("Cosmologie", "🌌", "Terra Frontier & Găuri Negre"),
    ALL("Toate", "🔍", "Căutare completă în toate modulele")
}

data class AppModuleItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val department: ScientificDepartment,
    val badge: String? = null,
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
    val discoveries by viewModel.discoveries.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val syncInfo by viewModel.syncInfo.collectAsState()
    val context = LocalContext.current

    var selectedDepartment by remember { mutableStateOf(ScientificDepartment.EXOPLANETS) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSyncInfo(context)
    }

    // Catalog organizat pe departamente științifice coerente
    val allModules = remember(onViewAdvancedLab, onViewXilonDiscovery, onViewDeepHunt, onViewExoplanets, onViewTerraFrontier, onViewResearchArchive) {
        listOf(
            // 1. DEPARTAMENTUL EXOPLANETE & TRANZIT
            AppModuleItem(
                id = "xilon_ai",
                title = "Xilon AI: Detecție Autonomă",
                subtitle = "Algoritm BLS, Mandel-Agol & Scor de Încredere tranzit",
                icon = "✨",
                department = ScientificDepartment.EXOPLANETS,
                badge = "AI LIVE",
                onClick = onViewXilonDiscovery
            ),
            AppModuleItem(
                id = "deep_hunt",
                title = "Deep Hunt: Blindspots TESS",
                subtitle = "Scanare arhive periodice, mono-tranzite cu perioadă lungă",
                icon = "⚡",
                department = ScientificDepartment.EXOPLANETS,
                badge = "TESS",
                onClick = onViewDeepHunt
            ),
            AppModuleItem(
                id = "fits_summary",
                title = "FITS Header & Curbe de Lumină",
                subtitle = "Metadate observator, fotometrie, slider SNR & export PDF",
                icon = "📊",
                department = ScientificDepartment.EXOPLANETS,
                badge = "FITS + PDF",
                onClick = { onViewAdvancedLab("FITS_SUMMARY") }
            ),
            AppModuleItem(
                id = "nasa_archive",
                title = "Catalog Exoplanete NASA (TAP)",
                subtitle = "Baze de date oficiale NASA Exoplanet Archive, Kepler & TESS",
                icon = "🪐",
                department = ScientificDepartment.EXOPLANETS,
                badge = "NASA TAP",
                onClick = onViewExoplanets
            ),
            AppModuleItem(
                id = "live_photometry",
                title = "Fotometrie Live & Protocoale NASA / CAS",
                subtitle = "Extracție flux diferențial și export oficial NASA TFOP, ESA VOTable & AAVSO",
                icon = "📉",
                department = ScientificDepartment.EXOPLANETS,
                badge = "NASA / ESA",
                onClick = { onViewAdvancedLab("LIVE_PHOTOMETRY") }
            ),
            AppModuleItem(
                id = "gaia_rescue",
                title = "Salvare Fals-Pozitive (Gaia De-Blender)",
                subtitle = "Separare fotometrică stele de fundal blenduite și validare",
                icon = "🎯",
                department = ScientificDepartment.EXOPLANETS,
                onClick = { onViewAdvancedLab("FALSE_POSITIVE_RESCUE") }
            ),
            AppModuleItem(
                id = "harmonic_stitcher",
                title = "Multi-Sector Harmonic Stitcher",
                subtitle = "Cusătură armonică între sectoare fotometrice TESS multiple",
                icon = "🔄",
                department = ScientificDepartment.EXOPLANETS,
                onClick = { onViewAdvancedLab("HARMONIC_STITCHER") }
            ),

            // 2. DEPARTAMENTUL OBSERVATOR & TELESCOAPE
            AppModuleItem(
                id = "smart_scope",
                title = "Smart Scope Sync (Seestar & DWARF)",
                subtitle = "Protocol auto-discovery rețea, ASCOM Alpaca & telemetrie",
                icon = "📡",
                department = ScientificDepartment.TELESCOPES,
                badge = "HARDWARE",
                onClick = { onViewAdvancedLab("SMART_SCOPE") }
            ),
            AppModuleItem(
                id = "telescope_goto",
                title = "Control Montură Telescop GoTo",
                subtitle = "Comenzi fizice motoare, slew rate și aliniere coordonate RA/DEC",
                icon = "🔭",
                department = ScientificDepartment.TELESCOPES,
                onClick = { onViewAdvancedLab("TELESCOPE_CONTROL") }
            ),
            AppModuleItem(
                id = "sky_radar",
                title = "Radar Cer & Numărătoare Tranzite",
                subtitle = "Ephemeride GPS locale: orare de intrare/ieșire și altitudine",
                icon = "🛰️",
                department = ScientificDepartment.TELESCOPES,
                badge = "GPS LIVE",
                onClick = { onViewAdvancedLab("SKY_RADAR") }
            ),
            AppModuleItem(
                id = "ar_sky_camera",
                title = "AR Sky Camera (Live Cer)",
                subtitle = "Hartă cerească suprapusă în timp real peste camera dispozitivului",
                icon = "📷",
                department = ScientificDepartment.TELESCOPES,
                onClick = { onViewAdvancedLab("AR_SKY_CAMERA") }
            ),
            AppModuleItem(
                id = "telescope_vision",
                title = "Viziune Artificială & Focalizare",
                subtitle = "Focalizare automată FWHM și centrare centroid stea de ghidaj",
                icon = "👁️",
                department = ScientificDepartment.TELESCOPES,
                onClick = { onViewAdvancedLab("TELESCOPE_VISION") }
            ),

            // 3. DEPARTAMENTUL LABORATOR & SPECTROSCOPIE
            AppModuleItem(
                id = "jwst_spectroscopy",
                title = "Spectroscopie Transmisie JWST",
                subtitle = "Profile de absorbție NIRSpec & NIRISS: H₂O, CO₂, CH₄ și CO",
                icon = "🌈",
                department = ScientificDepartment.LABORATORY,
                badge = "JWST",
                onClick = { onViewAdvancedLab("JWST_SPECTROSCOPY") }
            ),
            AppModuleItem(
                id = "plato_astero",
                title = "Asteroseismologie Misiunea PLATO",
                subtitle = "Oscilații p-mode stelare (Δν, νmax), determinare masă & rază",
                icon = "⭐",
                department = ScientificDepartment.LABORATORY,
                badge = "ESA PLATO",
                onClick = { onViewAdvancedLab("PLATO_ASTEROSEISMOLOGY") }
            ),
            AppModuleItem(
                id = "roman_microlensing",
                title = "Microlentile Nancy Grace Roman",
                subtitle = "Detecție planete vagaboande (rogue) prin lentile gravitaționale",
                icon = "🔍",
                department = ScientificDepartment.LABORATORY,
                badge = "ROMAN",
                onClick = { onViewAdvancedLab("ROMAN_MICROLENSING") }
            ),
            AppModuleItem(
                id = "climate_simulator",
                title = "Simulator Climateric 1D",
                subtitle = "Model radiativ-convectiv Manabe-Wetherald: insolație & albedo",
                icon = "☀️",
                department = ScientificDepartment.LABORATORY,
                onClick = { onViewAdvancedLab("CLIMATE_SIMULATOR") }
            ),
            AppModuleItem(
                id = "exomoon_tidal",
                title = "Exoluni & Oceane Mareice",
                subtitle = "Disipare termică mareică și habitabilitate în oceane subglaciare",
                icon = "🌕",
                department = ScientificDepartment.LABORATORY,
                badge = "EXOLUNI",
                onClick = { onViewAdvancedLab("EXOMOON_OCEANS") }
            ),
            AppModuleItem(
                id = "biosignatures",
                title = "Căutare Viață Extraterestră & Telescoape Live",
                subtitle = "Marte (Cheyava Falls / SAM), JWST (K2-18 b DMS), SETI & Scara NASA CoLD",
                icon = "🧬",
                department = ScientificDepartment.LABORATORY,
                badge = "LIVE TELESCOAPE",
                onClick = { onViewAdvancedLab("EXTRATERRESTRIAL_LIFE") }
            ),

            // 4. DEPARTAMENTUL COSMOLOGIE & ȘTIINȚE TERESTRE
            AppModuleItem(
                id = "terra_frontier",
                title = "TERRA INCOGNITA: Deep Earth",
                subtitle = "Anomalia Magnetică SAA, structura mantalei & fose oceanice",
                icon = "🌍",
                department = ScientificDepartment.COSMOLOGY,
                badge = "GEODINAMICĂ",
                onClick = onViewTerraFrontier
            ),
            AppModuleItem(
                id = "black_holes",
                title = "Găuri Negre & Relativitate Kerr",
                subtitle = "Metrica Kerr, orizontul evenimentelor, orbita ISCO & raza umbrei",
                icon = "🕳️",
                department = ScientificDepartment.COSMOLOGY,
                badge = "RELATIVITATE",
                onClick = { onViewAdvancedLab("BLACK_HOLES") }
            ),
            AppModuleItem(
                id = "big_bang",
                title = "Modelul Cosmologic Big Bang (FLRW)",
                subtitle = "Metrica Friedmann, radiație cosmică de fond (CMB) & reionizare",
                icon = "💥",
                department = ScientificDepartment.COSMOLOGY,
                badge = "COSMOLOGIE",
                onClick = { onViewAdvancedLab("BIG_BANG_COSMOLOGY") }
            ),
            AppModuleItem(
                id = "space_weather",
                title = "Vreme Spațială & Magnetosferă",
                subtitle = "Indici geomagnetici Kp, vânt solar și comprimare magnetopauză",
                icon = "🛡️",
                department = ScientificDepartment.COSMOLOGY,
                badge = "NOAA SWPC",
                onClick = { onViewAdvancedLab("SPACE_WEATHER") }
            ),
            AppModuleItem(
                id = "research_archive",
                title = "Arhivă Cercetări & Rapoarte (Room DB)",
                subtitle = "Bază de date locală persistentă pentru rapoarte de analiză",
                icon = "💾",
                department = ScientificDepartment.COSMOLOGY,
                badge = "ROOM DB",
                onClick = onViewResearchArchive
            )
        )
    }

    val visibleModules = remember(selectedDepartment, searchQuery, allModules) {
        if (selectedDepartment == ScientificDepartment.ALL || searchQuery.isNotBlank()) {
            if (searchQuery.isBlank()) {
                allModules
            } else {
                val q = searchQuery.trim().lowercase()
                allModules.filter {
                    it.title.lowercase().contains(q) ||
                            it.subtitle.lowercase().contains(q) ||
                            (it.badge?.lowercase()?.contains(q) ?: false)
                }
            }
        } else {
            allModules.filter { it.department == selectedDepartment }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AccentCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(AccentCyan, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "SYNTHESIS",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "ONLINE",
                                        color = AccentGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Observator & Astrofizică Computațională",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
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
                                "Sincronizare completă: NASA, MAST, Gaia, JPL actualizate!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("top_refresh_button")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sincronizează date",
                            tint = AccentCyan
                        )
                    }
                    IconButton(
                        onClick = onNewInvestigation,
                        modifier = Modifier.testTag("top_new_investigation")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Proiect nou", tint = AccentGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = AccentCyan,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Dej pe Home */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Hub Principal") },
                    label = { Text("Hub", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        indicatorColor = AccentCyan.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewExoplanets,
                    icon = { Icon(Icons.Default.Public, contentDescription = "Exoplanete", tint = TextSecondary) },
                    label = { Text("Exoplanete", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onViewAdvancedLab("SMART_SCOPE") },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Observator", tint = TextSecondary) },
                    label = { Text("Observator", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onViewAdvancedLab(null) },
                    icon = { Icon(Icons.Default.Biotech, contentDescription = "Laborator", tint = TextSecondary) },
                    label = { Text("Laborator", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewResearchArchive,
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Arhivă", tint = TextSecondary) },
                    label = { Text("Arhivă", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewInvestigation,
                containerColor = AccentCyan,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("PROIECT NOU", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("fab_new_investigation")
            )
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

            // =========================================================================
            // 1. PANOU TELEMETRIE & STATUS EXECUTIV (Simplificat, Elegant, Fără Haos)
            // =========================================================================
            item {
                ExecutiveMissionControlCard(
                    syncInfo = syncInfo,
                    onSyncNow = {
                        viewModel.triggerLiveSync(context)
                        viewModel.loadSyncInfo(context)
                        Toast.makeText(context, "Sincronizare arhive astrofizice inițiată!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // =========================================================================
            // 2. ACCES RAPID LA CELE 2 INSTRUMENTE FLAGSHIP (Direct, Fără Ocolișuri)
            // =========================================================================
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "INSTRUMENTE PRINCIPALE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: XILON AI
                        HeroQuickActionCard(
                            title = "Xilon AI",
                            subtitle = "Scaner Tranzite BLS",
                            icon = "✨",
                            badge = "AUTONOM",
                            accentColor = AccentCyan,
                            modifier = Modifier.weight(1f),
                            onClick = onViewXilonDiscovery
                        )

                        // Card 2: DEEP HUNT TESS
                        HeroQuickActionCard(
                            title = "Deep Hunt",
                            subtitle = "Blindspots TESS",
                            icon = "⚡",
                            badge = "ARHIVĂ",
                            accentColor = AccentAmber,
                            modifier = Modifier.weight(1f),
                            onClick = onViewDeepHunt
                        )
                    }
                }
            }

            // =========================================================================
            // 3. SELECTOR DEPARTAMENTE ȘTIINȚIFICE (Meniul Organizat Impecabil)
            // =========================================================================
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DEPARTAMENTE ȘTIINȚIFICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${allModules.size} Module Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pills departamente
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ScientificDepartment.values()) { dept ->
                            val isSelected = selectedDepartment == dept
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AccentCyan else DarkSurface,
                                border = BorderStroke(1.dp, if (isSelected) AccentCyan else DarkBorder),
                                modifier = Modifier.clickable {
                                    selectedDepartment = dept
                                    if (dept != ScientificDepartment.ALL) searchQuery = ""
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dept.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        dept.label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Căutare doar dacă e selectat 'Toate' sau dacă a căutat ceva
                    if (selectedDepartment == ScientificDepartment.ALL || searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Caută modul (ex: FITS, JWST, Seestar, Gaia, GoTo...)", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Caută", tint = AccentCyan, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Șterge", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // =========================================================================
            // 4. LISTA DE MODULE COERENTĂ ȘI AERISITĂ
            // =========================================================================
            if (visibleModules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Niciun modul găsit pentru căutare", color = TextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = { searchQuery = ""; selectedDepartment = ScientificDepartment.EXOPLANETS }) {
                                Text("Reveniți la Exoplanete", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(visibleModules) { module ->
                    DepartmentModuleCard(module = module)
                }
            }

            // =========================================================================
            // 5. SECȚIUNE CERCETĂRI & SARCINI ACTIVE (Sintetică, Elegantă)
            // =========================================================================
            item {
                ResearchOverviewSection(
                    activeTasks = activeTasks,
                    discoveriesCount = discoveries.size,
                    onTaskClick = { onInvestigationClick(it.id) },
                    onNewTask = onNewInvestigation,
                    onViewArchive = onViewResearchArchive
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

/**
 * Card executiv compact pentru telemetrie și starea observatorului.
 */
@Composable
private fun ExecutiveMissionControlCard(
    syncInfo: CelestialSyncInfo,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("executive_mission_control_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
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
                            "STATUS OBSERVATOR & DATE LIVE",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "WorkManager 1h • JPL, NASA TAP & Gaia DR3",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onSyncNow,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentCyan.copy(alpha = 0.15f),
                        contentColor = AccentCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("sync_now_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SYNC", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 indicatori calmi și uniformi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SURSE DATE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("15 Active", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("EPHEMERIDE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val gmst = if (syncInfo.gmstHours > 0) String.format(Locale.US, "%.1fh", syncInfo.gmstHours) else "Live"
                        Text(gmst, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("ULTIMUL SYNC", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(syncInfo.formattedLastSync, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/**
 * Card cu acțiune directă pentru instrumentele principale (Hero).
 */
@Composable
private fun HeroQuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    badge: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        badge,
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Deschide",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Card elegant și aerisit pentru fiecare modul din departament.
 */
@Composable
private fun DepartmentModuleCard(module: AppModuleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = module.onClick)
            .testTag("catalog_item_${module.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.size(42.dp)
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
                            color = AccentCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                module.badge,
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
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

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Deschide",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Secțiune compactă și clară pentru proiecte de cercetare și sarcini active.
 */
@Composable
private fun ResearchOverviewSection(
    activeTasks: List<Investigation>,
    discoveriesCount: Int,
    onTaskClick: (Investigation) -> Unit,
    onNewTask: () -> Unit,
    onViewArchive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        "PROIECTE & INVESTIGAȚII (${activeTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                TextButton(
                    onClick = onViewArchive,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Arhivă ($discoveriesCount) →",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTasks.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nicio investigație activă în curs",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Inițiază o cercetare nouă pentru sinteză de date cross-domain",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onNewTask,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crează", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeTasks.take(3).forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        task.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Status: ${task.status.name} • ${task.sourcesConsulted} surse",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
