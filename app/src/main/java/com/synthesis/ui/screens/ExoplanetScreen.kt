package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.domain.model.TfopwgDisposition
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.ExoplanetFilter
import com.synthesis.viewmodel.ExoplanetUiState
import com.synthesis.viewmodel.ExoplanetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoplanetScreen(
    onNavigateBack: () -> Unit,
    onViewDashboard: () -> Unit = {},
    onViewXilonDiscovery: () -> Unit = {},
    onViewDeepHunt: () -> Unit = {},
    onViewAdvancedLab: () -> Unit = {},
    viewModel: ExoplanetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EXOPLANET ARCHIVE", color = AccentCyan, style = MaterialTheme.typography.titleMedium)
                        Text("NASA Multi-Catalog Explorer", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onViewDashboard) {
                        Icon(Icons.Default.Timeline, contentDescription = "Population Dashboard", tint = AccentCyan)
                    }
                    IconButton(onClick = { viewModel.loadCandidates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Archive", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(padding)) {

            // Compact Quick-Navigation Row (De-cluttered, replaces the 4 giant banners)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onViewAdvancedLab,
                    label = { Text("🔬 Lab 2026", color = AccentGreen) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                )
                AssistChip(
                    onClick = onViewDeepHunt,
                    label = { Text("⚡ Deep Hunt", color = AccentAmber) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                )
                AssistChip(
                    onClick = onViewXilonDiscovery,
                    label = { Text("★ Xilon", color = AccentCyan) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                )
                AssistChip(
                    onClick = onViewDashboard,
                    label = { Text("📊 Recharts", color = AccentCyan) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name, TIC/KIC, or method...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCyan
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Catalog Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExoplanetFilter.values().forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                filter.title,
                                color = if (isSelected) DarkBackground else TextPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (filter) {
                                ExoplanetFilter.UNVETTED_NEW_WORLDS -> AccentGreen
                                ExoplanetFilter.HABITABLE_ZONE -> AccentCyan
                                else -> AccentAmber
                            },
                            containerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Scientific Transparency Banner
            Surface(
                color = when (selectedFilter) {
                    ExoplanetFilter.UNVETTED_NEW_WORLDS -> AccentGreen.copy(alpha = 0.12f)
                    ExoplanetFilter.HABITABLE_ZONE -> AccentCyan.copy(alpha = 0.12f)
                    else -> AccentAmber.copy(alpha = 0.10f)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                val bannerText = when (selectedFilter) {
                    ExoplanetFilter.UNVETTED_NEW_WORLDS ->
                        "Archival Discoveries: Genuine transit signals flagged by Kepler & TESS automated pipelines that have never received spectroscopic follow-up. High confidence, zero previous confirmation."
                    ExoplanetFilter.HABITABLE_ZONE ->
                        "Circumstellar Habitable Zone: Worlds whose equilibrium temperature (180K - 315K) and stellar flux permit liquid surface water under standard atmospheric models."
                    ExoplanetFilter.CONFIRMED ->
                        "Benchmark Catalog: Confirmed exoplanetary systems verified by international telescope consensus (Keck, VLT, JWST, HARPS)."
                    else ->
                        "Federated query to NASA Exoplanet Archive (TESS, Kepler KOI, K2, Confirmed). Triage ranking combines transit depth, stellar brightness, and physical radius plausibility."
                }
                Text(
                    bannerText,
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Content Area
            when (val state = uiState) {
                ExoplanetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Querying multi-catalog NASA TAP database...", color = TextSecondary)
                            Text("Connecting TESS, Kepler KOI, K2 & Confirmed archives", color = TextSecondary.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                is ExoplanetUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text("Could not reach archive endpoints", color = AccentRed, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.message, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadCandidates() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Retry Connection", color = DarkBackground)
                            }
                        }
                    }
                }
                is ExoplanetUiState.Success -> {
                    val displayed = state.displayedCandidates
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${displayed.size} candidates shown (${state.allCount} loaded)",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (state.unvettedCount > 0) {
                                    Text(
                                        "${state.unvettedCount} unvetted signals",
                                        color = AccentGreen,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        items(displayed, key = { it.toiId }) { candidate ->
                            ExoplanetCandidateCard(candidate)
                        }

                        if (displayed.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("No exoplanets match your current search or filter.", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExoplanetCandidateCard(candidate: ExoplanetCandidate) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Top Row: Name, Catalog Badge, Disposition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(candidate.toiId, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(candidate.ticId, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Catalog source badge
                    Surface(
                        color = Color(candidate.catalogSource.badgeColorHex).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            candidate.catalogSource.displayName,
                            color = Color(candidate.catalogSource.badgeColorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    DispositionBadge(candidate.disposition)
                }
            }

            // Badges row: Habitable Zone & ESI
            if (candidate.isInHabitableZone || (candidate.earthSimilarityIndex ?: 0f) >= 0.70f) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (candidate.isInHabitableZone) {
                        Surface(color = AccentCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("HABITABLE ZONE", color = AccentCyan, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    candidate.earthSimilarityIndex?.let { esi ->
                        if (esi >= 0.70f) {
                            Surface(color = AccentGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("ESI ${String.format("%.2f", esi)}", color = AccentGreen, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    if (candidate.isUnvettedCandidate) {
                        Surface(color = AccentAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("UNVETTED SIGNAL", color = AccentAmber, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                candidate.planetRadiusEarth?.let {
                    MetricColumn("RADIUS", "${String.format("%.2f", it)} R⊕", AccentCyan)
                } ?: run {
                    val estimatedR = candidate.transitDepthPpm?.let { depth ->
                        val est = 109.2 * kotlin.math.sqrt(depth / 1_000_000.0)
                        if (est in 0.3..35.0) String.format("~%.1f R⊕*", est) else null
                    }
                    MetricColumn("RADIUS", estimatedR ?: "~ În analiză", AccentAmber)
                }

                candidate.orbitalPeriodDays?.let {
                    MetricColumn("PERIOD", "${String.format("%.1f", it)} d", AccentAmber)
                } ?: MetricColumn("PERIOD", "N/A", TextSecondary)

                candidate.equilibriumTempK?.let {
                    MetricColumn("TEMP", "${it.toInt()} K", if (candidate.isInHabitableZone) AccentCyan else TextPrimary)
                } ?: MetricColumn("TEMP", "N/A", TextSecondary)

                MetricColumn("PRIORITY", "${(candidate.worthInvestigatingScore * 100).toInt()}%", AccentGreen)
            }

            // Expand / Collapse Action
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Hide Analysis" else "View Astrophysical Analysis", color = AccentCyan, style = MaterialTheme.typography.labelMedium)
                }

                candidate.stellarTessMagnitude?.let {
                    Text("Host Mag: ${String.format("%.1f", it)}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Expanded Astrophysical Details
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = TextSecondary.copy(alpha = 0.15f))

                if (candidate.planetRadiusEarth == null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("ℹ️", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "De ce raza este marcată '~ În analiză'? În cataloagele oficiale NASA (TESS/Kepler), raza se calculează ca R_p = R_* * sqrt(adâncime tranzit). Pentru candidații fără spectroscopie de înaltă rezoluție, raza stelei R_* este încă nedeterminată în arhivă, astfel încât raza exactă este în curs de validare fotometrică.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text("Astrophysical Reasoning & Vetting:", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                candidate.scoreReasoning.forEach { reason ->
                    Text("• $reason", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
                candidate.stellarDistancePc?.let {
                    Text("Distance from Earth: ${String.format("%.1f", it)} parsecs (~${(it * 3.26).toInt()} light-years)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                candidate.stellarEffTempK?.let {
                    Text("Host Star Teff: ${it.toInt()} K (${spectralTypeEstimate(it)})", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                candidate.transitDepthPpm?.let {
                    Text("Transit Depth: ${it.toInt()} ppm (dimming fraction: ${String.format("%.4f", it / 10000.0)}%)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                candidate.discoveryMethod?.let {
                    Text("Method: $it", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun DispositionBadge(disposition: TfopwgDisposition) {
    val (color, label) = when (disposition) {
        TfopwgDisposition.PC, TfopwgDisposition.CANDIDATE -> AccentCyan to "CANDIDATE"
        TfopwgDisposition.APC -> AccentAmber to "AMBIGUOUS"
        TfopwgDisposition.CONFIRMED, TfopwgDisposition.CP, TfopwgDisposition.KP -> AccentGreen to "CONFIRMED"
        TfopwgDisposition.FP, TfopwgDisposition.FA -> AccentRed to "FALSE POSITIVE"
        else -> TextSecondary to disposition.name
    }
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun spectralTypeEstimate(teff: Double): String = when {
    teff > 7500 -> "A-type star"
    teff > 6000 -> "F-type star"
    teff > 5200 -> "G-type star (Sun-like)"
    teff > 3700 -> "K-type dwarf"
    else -> "M-type red dwarf"
}
