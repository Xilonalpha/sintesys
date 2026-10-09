package com.synthesis.ui.screens.terra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.terra.*
import com.synthesis.ui.components.terra.AntarcticaSubglacialDashboard
import com.synthesis.ui.components.terra.TerraDiscoveryPipelineDashboard
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.terra.TerraFrontierViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerraFrontierScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerraFrontierViewModel = hiltViewModel()
) {
    val selectedDomain by viewModel.selectedDomain.collectAsState()
    val mysteries by viewModel.filteredMysteries.collectAsState()
    val trenches by viewModel.hadalTrenches.collectAsState()
    val selectedTrench by viewModel.selectedTrench.collectAsState()
    val mantleWaterWt by viewModel.mantleWaterWtPercent.collectAsState()
    val mantleState by viewModel.mantleHydrationState.collectAsState()
    val simYear by viewModel.simulationYear.collectAsState()
    val geomagneticState by viewModel.geomagneticState.collectAsState()
    val deepEarthquakeCount by viewModel.deepSubductionEventsCount.collectAsState()

    val pipelineStages by viewModel.pipelineStages.collectAsState()
    val telemetryFeeds by viewModel.telemetryFeeds.collectAsState()
    val frontierMetrics by viewModel.frontierMetrics.collectAsState()
    val pipelineOverview by viewModel.pipelineOverview.collectAsState()

    val subglacialLakes by viewModel.subglacialLakes.collectAsState()
    val selectedLake by viewModel.selectedSubglacialLake.collectAsState()
    val bedrockFeatures by viewModel.bedrockFeatures.collectAsState()
    val selectedFeature by viewModel.selectedBedrockFeature.collectAsState()
    val polarAltimetry by viewModel.polarAltimetry.collectAsState()
    val iceMeltFraction by viewModel.iceMeltFraction.collectAsState()
    val isostaticRebound by viewModel.isostaticRebound.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Pipeline, 1: Antarctica, 2: Mituri, 3: Hadal, 4: Manta

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌍", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "TERRA INCOGNITA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                        Text(
                            "Frontierele Neștiute ale Pământului & Oceanelor Abisale",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reîmprospătează telemetrie",
                            tint = AccentCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. HERO CARD: CE NU ȘTIM DESPRE PLANETA TERRA
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(AccentCyan, AccentGreen, AccentAmber))),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF0F2B48), Color(0xFF061426))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = AccentCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "PLANETARY FRONTIER LAB",
                                    color = AccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                "Seisme Adânci: $deepEarthquakeCount",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "Paradoxul Terrei: Cunoaștem mai bine suprafața Lunii decât adâncurile propriilor noastre oceane",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Peste 80% din relieful fundului oceanic este necartografiat la rezoluție înaltă, iar în mantaua Pământului la 500 km adâncime este stocat un ocean de 2 ori mai mare decât toate mările de la suprafață. Acest modul dezvăluie fenomenele reale, formulele și percepțiile greșite despre planeta noastră.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metric Pills Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF021024),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("FUNDUL OCEANIC", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                    Text("81.4% Necartografiat", color = AccentAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF021024),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("APA DIN MANTA", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                    Text("~2x Toate Oceanele", color = AccentGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF021024),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("SCUT GEOMAGNETIC", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                    Text("-10% în 150 Ani", color = AccentRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. PRIMARY TAB SELECTOR
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = DarkSurface,
                    contentColor = AccentCyan,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Pipeline", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("🧊 Antarctica", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Mituri", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("Oceane Hadale", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        text = { Text("Manta & SAA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // TAB 0: PIPELINE DE SINTEZĂ ȘI DESCOPERIRE TERRA
            if (activeTab == 0) {
                item {
                    TerraDiscoveryPipelineDashboard(
                        overview = pipelineOverview,
                        stages = pipelineStages,
                        telemetryFeeds = telemetryFeeds,
                        frontierMetrics = frontierMetrics,
                        onTriggerSynthesis = { viewModel.triggerSynthesisScan() }
                    )
                }
            }

            // TAB 1: ANTARCTICA SUBGLACIARĂ (BEDMACHINE & LACURI SUBGLACIARE)
            if (activeTab == 1) {
                item {
                    AntarcticaSubglacialDashboard(
                        subglacialLakes = subglacialLakes,
                        selectedLake = selectedLake,
                        bedrockFeatures = bedrockFeatures,
                        selectedFeature = selectedFeature,
                        polarAltimetry = polarAltimetry,
                        iceMeltFraction = iceMeltFraction,
                        isostaticRebound = isostaticRebound,
                        onSelectLake = { viewModel.selectSubglacialLake(it) },
                        onSelectFeature = { viewModel.selectBedrockFeature(it) },
                        onIceMeltChange = { viewModel.updateIceMeltSlider(it) }
                    )
                }
            }

            // TAB 2: MITURI VS REALITĂȚI ȘTIINȚIFICE
            if (activeTab == 2) {
                // Domain Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDomain == null,
                            onClick = { viewModel.selectDomain(null) },
                            label = { Text("Toate Frontierele (${viewModel.allMysteries.value.size})") }
                        )
                        TerraDomain.values().forEach { domain ->
                            FilterChip(
                                selected = selectedDomain == domain,
                                onClick = { viewModel.selectDomain(if (selectedDomain == domain) null else domain) },
                                label = { Text(domain.displayName) }
                            )
                        }
                    }
                }

                items(mysteries) { mystery ->
                    MysteryCard(mystery = mystery)
                }
            }

            // TAB 3: OCEANELE HADALE & ADÂNCIMILE NECARTOGRAFIATE
            if (activeTab == 3) {
                item {
                    Text(
                        "GROPILE HADALE (> 6.000M ADÂNCIME) & PRESIUNEA EXTREMĂ",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    // Horizontal Trench Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trenches.forEach { trench ->
                            val isSelected = selectedTrench?.name == trench.name
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else DarkSurface,
                                border = BorderStroke(1.dp, if (isSelected) AccentCyan else Color(0xFF1E293B)),
                                modifier = Modifier.clickable { viewModel.selectTrench(trench) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isSelected) "🌊 " else "⚓ ", fontSize = 14.sp)
                                    Text(
                                        trench.name.take(24),
                                        color = if (isSelected) AccentCyan else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                selectedTrench?.let { trench ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(trench.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(trench.location, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Surface(color = Color(0xFF0F2B48), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "-${trench.maxDepthMeters} m",
                                            color = AccentCyan,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Trench Metrics Grid
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = Color(0xFF0B192C), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("PRESIUNE HIDROSTATICĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                            Text("${trench.pressureAtmospheres.roundToInt()} atm", color = AccentAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("~${(trench.pressureAtmospheres * 1.013).roundToInt()} bar", color = TextSecondary, fontSize = 9.sp)
                                        }
                                    }
                                    Surface(color = Color(0xFF0B192C), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("NECARTOGRAFIAT", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                            Text("${trench.unmappedFloorPercent}%", color = AccentRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("Rezoluție sonică fină", color = TextSecondary, fontSize = 9.sp)
                                        }
                                    }
                                    Surface(color = Color(0xFF0B192C), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("TEMPERATURĂ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                            Text("${trench.temperatureCelsius}°C", color = AccentCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text("Ape polare adânci", color = TextSecondary, fontSize = 9.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Sursă de Energie Chimică (Fără Lumină):", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text(trench.chemicalEnergySource, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Organisme Endemice de Presiune Extremă (Piezofile):", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                trench.primaryEndemicOrganisms.forEach { organism ->
                                    Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(organism, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 4: SIMULATOR MANTA TERRESTRĂ & GEODINAM SAA
            if (activeTab == 4) {
                // Simulator 1: Ringwoodite Mantle Water
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💎", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "SIMULATOR: OCEANUL ASCUNS DIN MANTA",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Modifică concentrația masică de apă stocată în rețeaua cristalină a mineralului Ringwoodit (Zona de Tranziție 410-660 km):",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Apă în Ringwoodit: ${(mantleWaterWt * 10).roundToInt() / 10.0} wt%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentGreen)
                                Surface(color = Color(0xFF042F1A), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        "${mantleState.ratioToSurfaceOceans}x Toate Oceanele Terrei",
                                        color = AccentGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Slider(
                                value = mantleWaterWt.toFloat(),
                                onValueChange = { viewModel.updateMantleWaterSlider(it.toDouble()) },
                                valueRange = 0.2f..2.5f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentGreen,
                                    activeTrackColor = AccentGreen,
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Minim: 0.2 wt%", color = TextSecondary, fontSize = 9.sp)
                                Text("Probabil: 1.5 wt%", color = TextSecondary, fontSize = 9.sp)
                                Text("Saturație: 2.5 wt%", color = TextSecondary, fontSize = 9.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Pompă Tectonică de Subducție: ~${mantleState.subductionWaterPumpRateMtYear.roundToInt()} Megatone de apă pompate anual în adâncime de plăcile oceanice.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Simulator 2: SAA Geomagnetic Field Decay
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧭", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "SIMULATOR GEOMAGNETIC: ANOMALIA SAA",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Evoluția slăbirii scutului magnetic în Anomalia Atlanticului de Sud și viteza de derivă a Polului Nord Magnetic spre Siberia:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("An Proiecție: $simYear", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentAmber)
                                Text("Intensitate: ${geomagneticState.currentIntensityMicroTesla} µT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentRed)
                            }

                            Slider(
                                value = simYear.toFloat(),
                                onValueChange = { viewModel.updateSimulationYear(it.roundToInt()) },
                                valueRange = 1990f..2060f,
                                steps = 70,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentRed,
                                    activeTrackColor = AccentRed,
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = Color(0xFF2B0A0D), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("DERIVĂ POL NORD", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                        Text("${geomagneticState.northMagneticDriftKmPerYear} km/an", color = AccentAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(color = Color(0xFF2B0A0D), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("RISC SATELIȚI LEO", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                        Text(if (simYear >= 2030) "CRITIC" else "RIDICAT", color = AccentRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Status Radiație: ${geomagneticState.lowEarthOrbitRadiationRisk}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun MysteryCard(mystery: TerraFrontierMystery) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Domain Badge + Readiness Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(mystery.domain.badgeColorHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        mystery.domain.displayName.uppercase(),
                        color = Color(mystery.domain.badgeColorHex),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    "Explorat: ${mystery.explorationReadinessPercent}%",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                mystery.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // MISCONCEPTION (Percepție Eronată)
            Surface(
                color = Color(0xFF2A1517),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("❌", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "PERCEPȚIE ERONATĂ COMUNĂ",
                            color = AccentRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        mystery.misconception,
                        color = Color(0xFFFFCDD2),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SCIENTIFIC DISCOVERY (Realitate Științifică)
            Surface(
                color = Color(0xFF07271D),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔬", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "DESCOPERIRE & REALITATE ȘTIINȚIFICĂ",
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        mystery.scientificDiscovery,
                        color = Color(0xFFC8E6C9),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Formula Pill
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📐 ", fontSize = 12.sp)
                    Text(
                        mystery.physicalPrinciple,
                        color = AccentCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Implication for Life & Astrobiology
            Row(verticalAlignment = Alignment.Top) {
                Text("🌱 ", fontSize = 12.sp)
                Column {
                    Text("Implicație pentru Viață & Astrobiologie:", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                    Text(mystery.implicationsForLife, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                }
            }
        }
    }
}
