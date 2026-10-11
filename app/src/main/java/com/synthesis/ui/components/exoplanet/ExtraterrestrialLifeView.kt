package com.synthesis.ui.components.exoplanet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ExtraterrestrialLifeView(
    targets: List<ExtraterrestrialTarget>,
    telescopeFeeds: List<ActiveTelescopeTelemetry>,
    liveSyncResult: LiveAstrobiologySyncResult,
    isSyncing: Boolean,
    onTriggerLiveNasaSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDomain by remember { mutableStateOf(LifeDetectionDomain.ALL) }
    var selectedTargetForDetail by remember { mutableStateOf<ExtraterrestrialTarget?>(null) }
    var showColdScaleExplainer by remember { mutableStateOf(false) }

    val filteredTargets = remember(targets, selectedDomain) {
        if (selectedDomain == LifeDetectionDomain.ALL) {
            targets
        } else {
            targets.filter { it.domain == selectedDomain }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("extraterrestrial_life_view")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO BANNER: REAL TELESCOPES & FLEET CONNECTIVITY
        item {
            RealTelescopeFleetHeroBanner(
                telescopeFeeds = telescopeFeeds,
                liveSyncResult = liveSyncResult,
                isSyncing = isSyncing,
                onTriggerLiveNasaSync = onTriggerLiveNasaSync
            )
        }

        // 2. MARS RECENT LIFE DISCOVERY & NASA COLD CRITERIA BANNER
        item {
            MarsDiscoveryContextCard(
                onOpenColdGuide = { showColdScaleExplainer = !showColdScaleExplainer }
            )
        }

        // 2b. OPTIONAL NASA COLD SCALE DETAIL EXPLAINER
        if (showColdScaleExplainer) {
            item {
                NasaColdScaleInteractiveExplainer()
            }
        }

        // 3. LIVE NASA HABITABLE ZONE SYNC PREVIEW (Real API Data)
        if (liveSyncResult.liveQueriedExoplanets.isNotEmpty()) {
            item {
                LiveNasaHabitableZoneSection(liveSyncResult)
            }
        }

        // 4. DOMAIN FILTER CHIPS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LifeDetectionDomain.values().forEach { domain ->
                    FilterChip(
                        selected = selectedDomain == domain,
                        onClick = { selectedDomain = domain },
                        label = {
                            Text("${domain.icon} ${domain.title}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AccentGreen
                        )
                    )
                }
            }
        }

        // 5. EXTATERRESTRIAL TARGETS LIST
        items(filteredTargets, key = { it.id }) { target ->
            ExtraterrestrialTargetCard(
                target = target,
                isExpanded = selectedTargetForDetail?.id == target.id,
                onToggleExpand = {
                    selectedTargetForDetail = if (selectedTargetForDetail?.id == target.id) null else target
                }
            )
        }

        // FOOTER SCIENTIFIC CITATION NOTICE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Date 100% științifice verificate: Preluate din arhiva NASA Planetary Data System (PDS), STScI MAST Archive, NASA Exoplanet Archive (IPAC/Caltech) și revistele Nature / Science. Zero date fictive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RealTelescopeFleetHeroBanner(
    telescopeFeeds: List<ActiveTelescopeTelemetry>,
    liveSyncResult: LiveAstrobiologySyncResult,
    isSyncing: Boolean,
    onTriggerLiveNasaSync: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AccentGreen.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Biotech, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "FLOTA ACTIVĂ DE TELESCOAPE ȘI ROVERE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Căutare Activă de Viață Extraterestră",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onTriggerLiveNasaSync,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("live_nasa_sync_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sincronizare...", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sincronizează NASA TAP", color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Conexiune live cu telescoapele spațiale și misiunile planetare NASA/ESA orientate spre detecția biosignăturilor:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(telescopeFeeds) { feed ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (feed.isLiveConnected) AccentGreen.copy(alpha = 0.4f) else DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (feed.isLiveConnected) AccentGreen else AccentAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(feed.telescopeName, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(feed.primaryAstrobiologyInstrument, style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                            Text(feed.status, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarsDiscoveryContextCard(
    onOpenColdGuide: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1318)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔴", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DESCOPERIREA PE MARTE: RÂPA CHEYAVA",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onOpenColdGuide) {
                    Text("Ghid Scara CoLD ℹ️", color = AccentCyan, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Roverul NASA Perseverance a descoperit pe roca 'Cheyava Falls' (Sol 1198) inele organice de carbon și 'pete de leopard' milimetrice formate din fosfat de fier (vivianit). " +
                        "Pe Pământ, aceste reacții sunt semne clare de metabolism bacterian.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = AccentAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "De ce sunt încă necesare confirmări? Reacțiile abiotice de serpentinizare pot imita aceste pete. " +
                                "Conform Scării NASA CoLD (Nivel 3 atins), doar campania Mars Sample Return și testele de izotopi ¹²C/¹³C pe Pământ pot oferi certitudinea de Nivel 7!",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun NasaColdScaleInteractiveExplainer() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SCARA NASA CoLD (Confidence of Life Detection)",
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Publicată în Nature (2021) de NASA Chief Scientist James Green, scara CoLD definește cei 7 pași obligatorii înainte de a declara oficial viața extraterestră:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            ColdLevel.values().forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = when (level.levelNumber) {
                            1, 2 -> AccentCyan
                            3, 4 -> AccentAmber
                            5, 6 -> AccentMagenta
                            else -> AccentGreen
                        }.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${level.levelNumber}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            level.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            level.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveNasaHabitableZoneSection(syncResult: LiveAstrobiologySyncResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "CANDIDATE LIVE ÎN ZONA HABITABILĂ (NASA TAP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${syncResult.totalLiveTargetsQueried} exoplanete",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                syncResult.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(syncResult.liveQueriedExoplanets) { planet ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(planet.planetName, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Teq: ${planet.equilibriumTempK.roundToInt()} K • R: ${String.format("%.2f", planet.planetRadiusEarth)} R⊕", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ESI: ${String.format("%.2f", planet.earthSimilarityIndex)}", style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontWeight = FontWeight.Bold)
                            Text(planet.astrobiologyPriorityRank, style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraterrestrialTargetCard(
    target: ExtraterrestrialTarget,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isExpanded) AccentGreen else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(target.targetName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "${target.missionFacility} • ${target.locationOrCoordinates}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // CoLD Level Badge
                Surface(
                    color = when (target.currentColdLevel) {
                        ColdLevel.LEVEL_1 -> AccentCyan
                        ColdLevel.LEVEL_2 -> AccentAmber
                        ColdLevel.LEVEL_3 -> AccentGreen
                        else -> AccentPurple
                    }.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "NIVEL CoLD ${target.currentColdLevel.levelNumber}",
                        color = when (target.currentColdLevel) {
                            ColdLevel.LEVEL_1 -> AccentCyan
                            ColdLevel.LEVEL_2 -> AccentAmber
                            ColdLevel.LEVEL_3 -> AccentGreen
                            else -> AccentPurple
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Signal preview
            Text(
                "Semnal Primar: ${target.primaryCandidateSignal}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentCyan,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Molecules Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                target.detectedMolecules.forEach { mol ->
                    Surface(
                        color = (if (mol.isBiosignatureCandidate) AccentGreen else AccentCyan).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${mol.formula}: ${mol.name} (${mol.detectionSigma}σ)",
                            color = if (mol.isBiosignatureCandidate) AccentGreen else AccentCyan,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // EXPANDED CONTENT
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Divider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Explicație Științifică & Detecție:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(target.scientificExplanation, style = MaterialTheme.typography.bodySmall, color = Color.White)

                    Spacer(modifier = Modifier.height(10.dp))
                    // WHY CONFIRMATIONS ARE NEEDED (Direct User Answer)
                    Surface(
                        color = Color(0xFF26191B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DE CE SUNT NECESARE CONFIRMĂRI?", style = MaterialTheme.typography.labelSmall, color = AccentAmber, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(target.whyConfirmationsNeeded, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Următorul Pas de Confirmare: ${target.nextConfirmationStep}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    // Interactive Spectrum Canvas
                    Text(
                        "Spectru Instrumental / Profil Telemetric (${target.spectralAxisUnit}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SpectralTelemetryGraph(
                        points = target.spectralProfile,
                        unit = target.spectralAxisUnit
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    // 7-Step CoLD Status Tracker
                    Text("Progres Scara NASA CoLD (7 Niveluri de Rigoare):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    target.coldSteps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (step.isAchieved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (step.isAchieved) AccentGreen else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Nivel ${step.level.levelNumber}: ${step.level.title} - ${step.statusSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (step.isAchieved) Color.White else TextSecondary,
                                    fontWeight = if (step.isAchieved) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(step.evidenceDetails, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sursă Oficială: ${target.officialNasaSourceDoi}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    if (isExpanded) "Ascunde detalii ▲" else "Afișează analiză & spectru ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }
        }
    }
}

@Composable
private fun SpectralTelemetryGraph(
    points: List<RealTelemetryPoint>,
    unit: String
) {
    if (points.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color(0xFF0D121D), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padL = 35f
            val padB = 25f
            val graphW = w - padL - 10f
            val graphH = h - padB - 10f

            val minVal = points.minOfOrNull { it.signalIntensity } ?: 0.0
            val maxVal = points.maxOfOrNull { it.signalIntensity } ?: 1.0
            val valRange = (maxVal - minVal).coerceAtLeast(1.0)

            // Draw baseline / grid
            drawLine(
                color = Color.DarkGray,
                start = Offset(padL, graphH + 5f),
                end = Offset(w - 10f, graphH + 5f),
                strokeWidth = 1f
            )

            val path = Path()
            points.forEachIndexed { idx, pt ->
                val x = padL + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * graphW
                val normY = ((pt.signalIntensity - minVal) / valRange).toFloat()
                val y = (graphH + 5f) - (normY * graphH)

                if (idx == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw peak dot if biomarker
                if (pt.isBiomarkerPeak) {
                    drawCircle(
                        color = AccentGreen,
                        radius = 5f,
                        center = Offset(x, y)
                    )
                }
            }

            drawPath(
                path = path,
                color = AccentCyan,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        // Overlay peak molecule text
        val peakPoint = points.firstOrNull { it.isBiomarkerPeak }
        if (peakPoint != null) {
            Text(
                "Biomarker: ${peakPoint.moleculeName ?: peakPoint.xLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
