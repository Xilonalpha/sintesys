package com.synthesis.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.exoplanet.DeepHuntTab
import com.synthesis.viewmodel.exoplanet.DeepHuntViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepHuntScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeepHuntViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val monoTransits by viewModel.monoTransits.collectAsState()
    val ttvSystems by viewModel.ttvSystems.collectAsState()
    val atmosphericScores by viewModel.atmosphericScores.collectAsState()
    val neglectedJewels by viewModel.neglectedJewels.collectAsState()
    val context = LocalContext.current

    var showSolverDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DEEP HUNT: NASA PIPELINE BLINDSPOTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentCyan
                        )
                        Text(
                            "Unvetted Exoplanets & Long-Period Mono-Transits",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadData()
                        Toast.makeText(context, "Date TESS & Blindspots reîmprospătate din arhivă!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reîmprospătează date TESS",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showSolverDialog = true }) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "Single-Transit Solver",
                            tint = AccentCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = DarkSurface,
                contentColor = AccentCyan,
                edgePadding = 16.dp
            ) {
                DeepHuntTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                tab.title,
                                color = if (selectedTab == tab) AccentCyan else TextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                DeepHuntTab.MONO_TRANSITS -> MonoTransitsView(
                    candidates = monoTransits,
                    onOpenSolver = { showSolverDialog = true },
                    onSaveCandidate = { candidate ->
                        viewModel.saveCandidateToResearchArchive(candidate)
                        Toast.makeText(context, "Sinteza '${candidate.candidateName}' a fost salvată în Baza de Date Room!", Toast.LENGTH_SHORT).show()
                    }
                )
                DeepHuntTab.TTV_INVERSION -> TtvInversionView(ttvSystems)
                DeepHuntTab.ATMOSPHERIC_TSM -> AtmosphericTsmView(atmosphericScores)
                DeepHuntTab.NEGLECTED_JEWELS -> NeglectedJewelsView(neglectedJewels)
            }
        }
    }

    if (showSolverDialog) {
        SingleTransitSolverDialog(
            onDismiss = { showSolverDialog = false },
            onSolve = { tic, name, sec, epoch, dur, dep, teff, rad, mass, dist, vmag ->
                viewModel.solveCustomSingleTransit(
                    ticId = tic,
                    candidateName = name,
                    sector = sec,
                    epochBtjd = epoch,
                    durationHours = dur,
                    depthPpm = dep,
                    teffK = teff,
                    radSun = rad,
                    massSun = mass,
                    distPc = dist,
                    vMag = vmag
                )
                showSolverDialog = false
            }
        )
    }
}

@Composable
private fun MonoTransitsView(
    candidates: List<MonoTransitCandidate>,
    onOpenSolver: () -> Unit,
    onSaveCandidate: (MonoTransitCandidate) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "THE 27.4-DAY SECTOR BLINDSPOT",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "NASA's SPOC automated pipeline requires >= 2 or 3 transits in a 27.4-day sector. " +
                                "Temperate exoplanets in the Habitable Zone of solar-type stars orbit in 100–365+ days, " +
                                "meaning they transit strictly ONCE per sector and are discarded as noise. " +
                                "Below are confirmed mono-transits inverted into true orbits using Seager & Mallén-Ornelas circular dynamics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenSolver,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INVERT CUSTOM SINGLE TRANSIT", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        items(candidates) { candidate ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (candidate.isHabitableZone) AccentGreen.copy(alpha = 0.7f) else DarkBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                candidate.candidateName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                "TIC ${candidate.ticId} • Sector ${candidate.sector} • V = ${candidate.apparentMagV} mag",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (candidate.isHabitableZone) {
                            Surface(
                                color = AccentGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "★ HABITABLE ZONE",
                                    color = AccentGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Transit Duration", "${String.format("%.1f", candidate.transitDurationHours)} h")
                        MetricItem("Transit Depth", "${candidate.transitDepthPpm.roundToInt()} ppm")
                        MetricItem("Derived Radius", "${String.format("%.2f", candidate.planetRadiusEarth)} R⊕")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Estimated Period", "~${candidate.circularEstimatedPeriodDays.roundToInt()} days", highlightColor = AccentCyan)
                        MetricItem("Semi-Major Axis", "${String.format("%.3f", candidate.semiMajorAxisAu)} AU")
                        MetricItem("Equilibrium Temp", "${candidate.equilibriumTempK.roundToInt()} K")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        candidate.detectionNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onSaveCandidate(candidate) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SALVEAZĂ ÎN ROOM DB", fontSize = 11.sp, color = AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TtvInversionView(systems: List<TtvCompanionAnalysis>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AllInclusive, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GRAVITATIONAL INVERSION OF UNSEEN WORLDS",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Planets whose orbits do not align with Earth's line of sight will NEVER transit their host star. " +
                                "However, their gravity pulls on transiting inner planets, shifting their arrival times by minutes to hours (TTV). " +
                                "By analyzing O-C (Observed minus Calculated) residuals, we mathematically reconstruct the mass, orbit, and habitability of hidden companion exoplanets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(systems) { sys ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sys.primaryCandidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Primary Planet: P = ${String.format("%.2f", sys.primaryPeriodDays)} d, R = ${String.format("%.2f", sys.primaryRadiusEarth)} R⊕", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = AccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                sys.resonanceRatio,
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("TTV Amplitude", "±${String.format("%.1f", sys.ttvAmplitudeMinutes)} min", highlightColor = AccentAmber)
                        MetricItem("Super-Period", "${sys.superPeriodDays.roundToInt()} days")
                        MetricItem("Detection Confidence", "${String.format("%.1f", sys.confidenceSigma)}σ", highlightColor = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Companion Mass", "~${String.format("%.1f", sys.estimatedCompanionMassEarth)} M⊕", highlightColor = AccentCyan)
                        MetricItem("Companion Period", "${String.format("%.2f", sys.estimatedCompanionPeriodDays)} d")
                        MetricItem("Companion Teq", "${sys.companionEquilibriumTempK.roundToInt()} K")
                    }

                    if (sys.companionHabitableZone) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = AccentGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "★ UNSEEN COMPANION CONFIRMED IN CIRCUMSTELLAR HABITABLE ZONE",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(sys.theoreticalExplanation, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AtmosphericTsmView(scores: List<AtmosphericCharacterizationScore>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Biotech, contentDescription = null, tint = AccentMagenta)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ATMOSPHERIC TRANSMISSION SPECTROSCOPY (JWST / ARIEL)",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentMagenta
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Over 7,000 unconfirmed TOIs sit in NASA's backlog without transmission spectroscopy scoring. " +
                                "Using Kempton et al. (2018) formulations, we compute the Transmission Spectroscopy Metric (TSM) and " +
                                "Emission Spectroscopy Metric (ESM) to find targets where James Webb (JWST) can detect water, methane, and CO2 in 1–2 transits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(scores) { s ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (s.tsmScore >= 90) AccentMagenta else DarkBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.candidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("TIC ${s.ticId} • J = ${s.jMagnitude} mag • Teq = ${s.equilibriumTempK.roundToInt()} K", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = (if (s.tsmScore >= 90) AccentMagenta else AccentCyan).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "TSM ${s.tsmScore.roundToInt()}",
                                color = if (s.tsmScore >= 90) AccentMagenta else AccentCyan,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Planet Radius", "${String.format("%.2f", s.planetRadiusEarth)} R⊕")
                        MetricItem("Scale Height (H)", "${s.atmosphericScaleHeightKm.roundToInt()} km", highlightColor = AccentCyan)
                        MetricItem("Feature Depth", "${s.transitFeatureAmplitudePpm.roundToInt()} ppm")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(s.jwstRecommendation, style = MaterialTheme.typography.bodySmall, color = AccentMagenta)
                }
            }
        }
    }
}

@Composable
private fun NeglectedJewelsView(jewels: List<NeglectedJewelTarget>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "THE TFOP BACKLOG PURGATORY",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "NASA's TESS Follow-up Observing Program (TFOP) currently has thousands of planet candidates (PC) " +
                                "that have languished unconfirmed for 3 to 5+ years. " +
                                "Ground observatories lack the hours to follow up all targets, leaving high-SNR, high-ESI worlds abandoned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(jewels) { j ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(j.candidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("TIC ${j.ticId} • V = ${j.stellarMag} mag • ESI = ${j.esi}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = AccentAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${j.yearsInBacklog} YRS IN BACKLOG",
                                color = AccentAmber,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Signal-to-Noise", "${j.snr}σ", highlightColor = AccentGreen)
                        MetricItem("Period", "${String.format("%.2f", j.periodDays)} d")
                        MetricItem("Radius", "${String.format("%.2f", j.planetRadiusEarth)} R⊕")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Why NASA delayed it: ${j.whyNasaIgnoredIt}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Validation Path: ${j.validationPath}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, highlightColor: Color = Color.White) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = highlightColor)
    }
}

@Composable
private fun SingleTransitSolverDialog(
    onDismiss: () -> Unit,
    onSolve: (tic: String, name: String, sec: String, epoch: Double, dur: Double, dep: Double, teff: Double, rad: Double, mass: Double, dist: Double?, vmag: Double) -> Unit
) {
    var tic by remember { mutableStateOf("999000123") }
    var name by remember { mutableStateOf("Custom Mono-Transit Target") }
    var duration by remember { mutableStateOf("12.5") }
    var depth by remember { mutableStateOf("2500") }
    var teff by remember { mutableStateOf("5700") }
    var rad by remember { mutableStateOf("1.0") }
    var mass by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Single-Transit Inversion Solver", color = AccentCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Inverts single transit duration and depth into semi-major axis, period, and habitability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Target Name") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (h)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = depth,
                        onValueChange = { depth = it },
                        label = { Text("Depth (ppm)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = teff,
                        onValueChange = { teff = it },
                        label = { Text("Teff (K)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rad,
                        onValueChange = { rad = it },
                        label = { Text("R* (R☉)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dur = duration.toDoubleOrNull() ?: 12.0
                    val dep = depth.toDoubleOrNull() ?: 2000.0
                    val t = teff.toDoubleOrNull() ?: 5778.0
                    val r = rad.toDoubleOrNull() ?: 1.0
                    val m = mass.toDoubleOrNull() ?: 1.0
                    onSolve(tic, name, "s0045", 2400.0, dur, dep, t, r, m, 120.0, 10.5)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
            ) {
                Text("RUN INVERSION")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        }
    )
}
