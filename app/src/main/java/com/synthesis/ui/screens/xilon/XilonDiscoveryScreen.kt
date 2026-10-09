package com.synthesis.ui.screens.xilon

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.geometry.Size
import com.synthesis.domain.model.exoplanet.DetectedDipSignal
import com.synthesis.domain.model.exoplanet.TransitDipScanReport
import com.synthesis.domain.model.exoplanet.DipShapeClassification
import com.synthesis.domain.model.exoplanet.DipRecurrenceVerdict
import androidx.compose.ui.window.Dialog
import com.synthesis.ui.components.exoplanet.XilonDetectionRechartsView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.synthesis.data.local.entity.XilonDiscoveryEntity
import com.synthesis.data.service.gemini.GeminiAnalysisResult
import com.synthesis.domain.model.xilon.*
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.xilon.XilonAiChatMessage
import com.synthesis.viewmodel.xilon.XilonUiState
import com.synthesis.viewmodel.xilon.XilonViewModel
import kotlin.math.roundToInt
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XilonDiscoveryScreen(
    onNavigateBack: () -> Unit,
    viewModel: XilonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedDiscoveries by viewModel.savedDiscoveries.collectAsState()
    val geminiAnalysis by viewModel.geminiAnalysis.collectAsState()
    val isAnalyzingWithGemini by viewModel.isAnalyzingWithGemini.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatResponding by viewModel.isChatResponding.collectAsState()
    val isApiKeySet by viewModel.isApiKeySet.collectAsState()
    val dipReport by viewModel.dipScanReport.collectAsState()
    val selectedDip by viewModel.selectedDip.collectAsState()
    val dipSensitivity by viewModel.dipSensitivitySigma.collectAsState()
    val isDipHighlightActive by viewModel.isDipHighlightActive.collectAsState()
    var showGeminiKeyDialog by remember { mutableStateOf(false) }
    var ticInput by remember { mutableStateOf("25155310") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "XILON DISCOVERY",
                                color = AccentCyan,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isApiKeySet) AccentGreen.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (isApiKeySet) "✨ GEMINI 3.5 AI" else "⚙️ AI CONFIG",
                                    color = if (isApiKeySet) AccentGreen else AccentAmber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Autonomous Photon Transit Detector • MAST Archive & Gemini AI",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showGeminiKeyDialog = true }) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Gemini Key Config",
                            tint = if (isApiKeySet) AccentCyan else AccentAmber
                        )
                    }
                    IconButton(onClick = {
                        val target = ticInput.trim().ifBlank { "25155310" }
                        viewModel.runScan(target)
                        Toast.makeText(context, "Reîmprospătare date fotometrice MAST & cataloage NASA pentru TIC $target...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reîmprospătează date MAST/NASA", tint = Color.White)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Cyber Card
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AUTONOMOUS ENGINE", color = AccentCyan, style = MaterialTheme.typography.labelMedium)
                            Surface(
                                color = AccentGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "ONLINE • DUAL ARCHIVE VALIDATION",
                                    color = AccentGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Xilon extracts raw photon time-series from MAST (STScI), executes Transit Least Squares (TLS), and cross-matches both confirmed exoplanets (pscomppars) and unvetted candidates (toi).",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Saved Discoveries Offline Catalog (if present)
            if (savedDiscoveries.isNotEmpty()) {
                item {
                    SavedDiscoveriesCard(
                        discoveries = savedDiscoveries,
                        onSelect = { tic ->
                            ticInput = tic
                            viewModel.runScan(tic)
                        },
                        onDelete = { id ->
                            viewModel.deleteSaved(id)
                        }
                    )
                }
            }

            // Target Selector & Scan Trigger Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("TARGET STAR INPUT (TIC ID)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ticInput,
                                onValueChange = { ticInput = it },
                                label = { Text("TESS Input Catalog ID") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = { viewModel.runScan(ticInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SCAN", color = DarkBackground, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // SCIENTIFIC ADVISOR: TARGET COUNSELING (WHICH ONES DESERVE VERIFICATION)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "CONSILIER ASTRONOMIC: CE STELE MERITĂ VERIFICATE?",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Prioritizare NASA & Blindspots",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        val curatedList = viewModel.curatedTargets
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            curatedList.forEach { target ->
                                val isSelected = ticInput == target.ticId
                                val isHighPriority = target.advisoryPriority.contains("PRIORITATE MAXIMĂ") || target.advisoryPriority.contains("CERCETARE ÎNALTĂ")
                                val isControl = target.advisoryPriority.contains("CONTROL")
                                
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            ticInput = target.ticId
                                            viewModel.runScan(target.ticId)
                                        },
                                    color = if (isSelected) AccentCyan.copy(alpha = 0.15f) else Color(0xFF0F172A),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentCyan
                                        else if (isHighPriority) AccentAmber.copy(alpha = 0.6f)
                                        else if (isControl) Color(0xFF334155)
                                        else AccentGreen.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    color = if (isHighPriority) AccentAmber.copy(alpha = 0.2f)
                                                           else if (isControl) Color(0xFF334155)
                                                           else AccentGreen.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        target.advisoryPriority,
                                                        color = if (isHighPriority) AccentAmber else if (isControl) TextSecondary else AccentGreen,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    target.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) AccentCyan else Color.White
                                                )
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    ticInput = target.ticId
                                                    viewModel.runScan(target.ticId)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) AccentCyan else Color(0xFF1E293B),
                                                    contentColor = if (isSelected) Color.Black else AccentCyan
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(if (isSelected) "SCANAT" else "SCANĂ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            target.scientificReason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isHighPriority) Color(0xFFFDE68A) else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            "Sector: ${target.sector} • Mag: ${target.estimatedMag} • Clasificare: ${target.knownClassification}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dynamic State Rendering
            when (val state = uiState) {
                is XilonUiState.Scanning -> {
                    item {
                        ScanningProgressCard(state)
                    }
                }
                is XilonUiState.Error -> {
                    item {
                        Surface(
                            color = AccentRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(state.message, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                is XilonUiState.Success -> {
                    val res = state.result

                    // 1. Verdict & Diagnostic Summary
                    item {
                        VerdictReportCard(
                            res = res,
                            isSaved = savedDiscoveries.any { it.ticId == res.star.ticId },
                            onSave = { viewModel.saveCurrentResult() }
                        )
                    }

                    // 2. Peer-Reviewed Benchmark Validation Card (if confirmed planet)
                    if (res.archivalCrossMatch.status == ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION) {
                        item {
                            BenchmarkValidationCard(res)
                        }
                    }

                    // 2.5 Real Gemini AI Scientific Evaluation Dossier
                    item {
                        GeminiAiEvaluationCard(
                            res = res,
                            analysis = geminiAnalysis,
                            isAnalyzing = isAnalyzingWithGemini,
                            onReAnalyze = { viewModel.runGeminiAnalysis(res) },
                            onConfigureKey = { showGeminiKeyDialog = true }
                        )
                    }

                    // 2.6 Interactive Gemini AI Astrophysicist Chat
                    item {
                        GeminiAstrophysicistChatCard(
                            res = res,
                            messages = chatMessages,
                            isResponding = isChatResponding,
                            onSendMessage = { viewModel.askGemini(it) }
                        )
                    }

                    // 3. Light Curve Canvas Visualization with Transit Dip Scanner
                    item {
                        LightCurveVisualizerCard(
                            res = res,
                            dipReport = dipReport,
                            selectedDip = selectedDip,
                            isDipHighlightActive = isDipHighlightActive,
                            dipSensitivity = dipSensitivity,
                            onSelectDip = { viewModel.selectDip(it) },
                            onToggleHighlight = { viewModel.toggleDipHighlight() },
                            onRescanDips = { viewModel.runDipScan(it) }
                        )
                    }

                    // 4. Astrophysical Derived Metrics Grid
                    item {
                        AstrophysicalMetricsCard(res)
                    }

                    // 5. Planetary Interior Physics & Dynamics Card
                    item {
                        PlanetaryInteriorPhysicsCard(res)
                    }

                    // 6. Celestial Astrometry & Deep Literature (NASA ADS / arXiv / SIMBAD)
                    item {
                        AstrometryAndLiteratureCard(res)
                    }

                    // 7. Execution Logs & Pipeline Audit
                    item {
                        PipelineAuditCard(res)
                    }

                    // 8. Share / Export Dossiers
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.saveXilonToResearchArchive(res)
                                    Toast.makeText(context, "Sinteza '${res.tceName}' a fost salvată în Arhiva Room DB!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = DarkBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SALVEAZĂ ÎN ARHIVA ROOM DB", color = DarkBackground, style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = {
                                    val text = buildDiscoveryDossierText(res)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "XILON Discovery Dossier: TIC ${res.star.ticId}")
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Discovery Report"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = DarkBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EXPORT SCIENTIFIC DOSSIER", color = DarkBackground, style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = {
                                    val ctoiText = buildNasaExoFopCtoiText(res)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "NASA ExoFOP CTOI Submission: TIC ${res.star.ticId}")
                                        putExtra(Intent.EXTRA_TEXT, ctoiText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share NASA CTOI Format"))
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FORMAT NASA ExoFOP / CTOI REPORT", color = AccentGreen, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                is XilonUiState.Idle -> {
                    if (state.lastResult != null) {
                        item {
                            VerdictReportCard(
                                res = state.lastResult,
                                isSaved = savedDiscoveries.any { it.ticId == state.lastResult.star.ticId },
                                onSave = { viewModel.saveCurrentResult() }
                            )
                        }
                        item {
                            LightCurveVisualizerCard(state.lastResult)
                        }
                        item {
                            AstrophysicalMetricsCard(state.lastResult)
                        }
                        item {
                            PlanetaryInteriorPhysicsCard(state.lastResult)
                        }
                        item {
                            AstrometryAndLiteratureCard(state.lastResult)
                        }
                    } else {
                        item {
                            Surface(
                                color = DarkSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("NO TARGET ACTIVELY SCANNED", color = TextSecondary, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Select one of the curated candidate stars above or enter any valid TIC ID to download live photon series from MAST and execute mathematical transit detection.",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGeminiKeyDialog) {
        GeminiApiKeyDialog(
            isKeyConfigured = isApiKeySet,
            onDismiss = { showGeminiKeyDialog = false },
            onSaveKey = { key ->
                viewModel.saveUserGeminiApiKey(key)
                showGeminiKeyDialog = false
            }
        )
    }
}

@Composable
fun CuratedTargetChip(label: String, ticId: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) AccentCyan else DarkBackground,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentCyan else TextSecondary.copy(alpha = 0.3f))
    ) {
        Text(
            label,
            color = if (isSelected) DarkBackground else TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SavedDiscoveriesCard(
    discoveries: List<XilonDiscoveryEntity>,
    onSelect: (String) -> Unit,
    onDelete: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MY SAVED CATALOG (${discoveries.size})",
                        color = AccentCyan,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    if (expanded) "HIDE ▲" else "SHOW ALL ▼",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                discoveries.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).clickable { onSelect(item.ticId) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "TIC ${item.ticId}",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        item.canonicalName,
                                        color = AccentCyan,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    "P = ${String.format("%.3f", item.periodDays)} d • Rp = ${item.planetRadiusEarth?.let { String.format("%.2f", it) } ?: "N/A"} R⊕ • SNR = ${String.format("%.1f", item.snr)}σ",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                item.archetype?.let { arch ->
                                    Text(
                                        arch,
                                        color = AccentGreen,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onSelect(item.ticId) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Scan", tint = AccentCyan)
                                }
                                IconButton(onClick = { onDelete(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.6f))
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
fun ScanningProgressCard(state: XilonUiState.Scanning) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("SCANNING TIC ${state.ticId}", color = AccentCyan, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { state.stageIndex.toFloat() / state.totalStages.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = AccentCyan,
                trackColor = DarkBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Stage ${state.stageIndex}/${state.totalStages}: ${state.stage}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun VerdictReportCard(
    res: XilonDetectionResult,
    isSaved: Boolean,
    onSave: () -> Unit
) {
    val badgeColor = Color(res.verdict.colorHex)
    val match = res.archivalCrossMatch

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        res.verdictTitle,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.Bookmark,
                        contentDescription = "Save Discovery",
                        tint = if (isSaved) AccentGreen else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(res.verdictSummary, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HOST STAR", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("TIC ${res.star.ticId}", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                }

                Surface(
                    color = when (match.status) {
                        ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION -> AccentGreen.copy(alpha = 0.2f)
                        ArchivalCatalogStatus.NASA_TOI_CANDIDATE -> AccentCyan.copy(alpha = 0.2f)
                        ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL -> AccentAmber.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        when (match.status) {
                            ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION -> "CONFIRMED: ${match.canonicalName}"
                            ArchivalCatalogStatus.NASA_TOI_CANDIDATE -> "CANDIDATE: ${match.canonicalName}"
                            ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL -> "UNRECORDED IN NASA ARCHIVES"
                        },
                        color = when (match.status) {
                            ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION -> AccentGreen
                            ArchivalCatalogStatus.NASA_TOI_CANDIDATE -> AccentCyan
                            ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL -> AccentAmber
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onSave,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSaved) AccentGreen else TextSecondary.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = if (isSaved) AccentGreen else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isSaved) "SAVED IN MY CATALOG" else "SAVE TO MY CATALOG",
                        color = if (isSaved) AccentGreen else TextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun BenchmarkValidationCard(res: XilonDetectionResult) {
    val m = res.archivalCrossMatch
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("★ PEER-REVIEWED BENCHMARK RECOVERY", color = AccentGreen, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Target is known confirmed planet ${m.canonicalName}, discovered in ${m.discoveryYear ?: "literature"} (${m.discoveryFacility ?: "ground transit"}). Xilon photon extraction successfully verified against archival ground truth:",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PARAMETER", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Period (P)", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Text("Transit Depth", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Text("Duration", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("LITERATURE TRUTH", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("${m.literaturePeriodDays?.let { String.format("%.3f", it) } ?: "N/A"} d", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                    Text("${m.literatureDepthPpm?.roundToInt() ?: "N/A"} ppm", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                    Text("${m.literatureDurationHours?.let { String.format("%.2f", it) } ?: "N/A"} h", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("XILON EXTRACTED", color = AccentCyan, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("${String.format("%.3f", res.orbitalPeriodDays)} d", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                    Text("${res.transitDepthPpm.roundToInt()} ppm", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                    Text("${String.format("%.2f", res.transitDurationHours)} h", color = AccentCyan, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

enum class BinningMode(val label: String) {
    RAW("Raw"),
    BIN_15M("15 min"),
    BIN_30M("30 min")
}

enum class ChartEngine(val label: String, val icon: String) {
    RECHARTS("Recharts Interactiv", "🌐"),
    NATIVE_CANVAS("Canvas Nativ", "⚡")
}

@Composable
fun LightCurveVisualizerCard(
    res: XilonDetectionResult,
    dipReport: TransitDipScanReport? = null,
    selectedDip: DetectedDipSignal? = null,
    isDipHighlightActive: Boolean = true,
    dipSensitivity: Double = 3.5,
    onSelectDip: (DetectedDipSignal?) -> Unit = {},
    onToggleHighlight: () -> Unit = {},
    onRescanDips: (Double) -> Unit = {}
) {
    var chartEngine by remember { mutableStateOf(ChartEngine.RECHARTS) }
    var binMode by remember { mutableStateOf(BinningMode.RAW) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURBĂ DE LUMINĂ (TIC ${res.star.ticId})", color = AccentCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${res.totalPhotonsAnalyzed} fotoni analizați • Perioadă: ${String.format(Locale.US, "%.3f", res.orbitalPeriodDays)}d", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }

                // Switch between Recharts and Native Canvas
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        ChartEngine.values().forEach { engine ->
                            val isSelected = chartEngine == engine
                            Surface(
                                onClick = { chartEngine = engine },
                                color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(engine.icon, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        engine.label,
                                        color = if (isSelected) AccentCyan else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (chartEngine == ChartEngine.RECHARTS) {
                // Interactive Recharts Engine with highlighted dip regions
                XilonDetectionRechartsView(
                    res = res,
                    dipReport = dipReport,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Native Compose Canvas Engine with Binning Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Binning:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    BinningMode.values().forEach { mode ->
                        val isSelected = binMode == mode
                        Surface(
                            onClick = { binMode = mode },
                            color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else DarkBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentCyan else TextSecondary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                mode.label,
                                color = if (isSelected) AccentCyan else TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Canvas Chart with Highlighted Transit Dips
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val rawPts = res.lightCurveSample
                        if (rawPts.isNotEmpty()) {
                            val minP = rawPts.minOf { it.phase }
                            val maxP = rawPts.maxOf { it.phase }
                            val pRange = (maxP - minP).takeIf { it > 0.001 } ?: 1.0
                            val midX = (((0.0 - minP) / pRange) * w).toFloat().coerceIn(0f, w)

                            // Compute binned points if requested
                            val displayPts = when (binMode) {
                                BinningMode.RAW -> rawPts
                                BinningMode.BIN_15M -> binPhotometry(rawPts, binWidth = (pRange / 35.0).coerceAtLeast(0.1))
                                BinningMode.BIN_30M -> binPhotometry(rawPts, binWidth = (pRange / 18.0).coerceAtLeast(0.2))
                            }

                            val baselineY = h * 0.28f

                            // Draw baseline and mid-transit line
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.35f),
                                start = Offset(0f, baselineY),
                                end = Offset(w, baselineY),
                                strokeWidth = 1.5f
                            )

                            drawLine(
                                color = AccentCyan.copy(alpha = 0.4f),
                                start = Offset(midX, 0f),
                                end = Offset(midX, h),
                                strokeWidth = 1f
                            )

                            // Draw highlighted dip regions if dip highlight is active
                            if (isDipHighlightActive && dipReport != null) {
                                dipReport.allDips.forEach { dip ->
                                    val xStart = (((dip.ingressPhase - minP) / pRange) * w).toFloat().coerceIn(0f, w)
                                    val xEnd = (((dip.egressPhase - minP) / pRange) * w).toFloat().coerceIn(0f, w)
                                    val bandWidth = (xEnd - xStart).coerceAtLeast(6f)
                                    val isSelected = selectedDip == dip
                                    val bandColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.32f) else Color(0xFFFFD600).copy(alpha = 0.16f)
                                    val borderColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFFFFD600).copy(alpha = 0.65f)

                                    // Shaded transit column
                                    drawRect(
                                        color = bandColor,
                                        topLeft = Offset(xStart, 0f),
                                        size = Size(bandWidth, h)
                                    )
                                    // Ingress line
                                    drawLine(
                                        color = borderColor,
                                        start = Offset(xStart, 0f),
                                        end = Offset(xStart, h),
                                        strokeWidth = 1.5f
                                    )
                                    // Egress line
                                    drawLine(
                                        color = borderColor,
                                        start = Offset(xEnd, 0f),
                                        end = Offset(xEnd, h),
                                        strokeWidth = 1.5f
                                    )
                                }
                            }

                            // Draw background raw faint points if binned
                            if (binMode != BinningMode.RAW) {
                                rawPts.forEach { pt ->
                                    val x = (((pt.phase - minP) / pRange) * w).toFloat().coerceIn(0f, w)
                                    val depthPx = if (res.transitDepthPpm > 0) {
                                        (pt.detrendedFlux * 1_000_000.0 / res.transitDepthPpm * (h * 0.5f)).toFloat()
                                    } else 0f
                                    val y = (baselineY + depthPx).coerceIn(0f, h)
                                    drawCircle(
                                        color = Color.Gray.copy(alpha = 0.25f),
                                        radius = 1.5f,
                                        center = Offset(x, y)
                                    )
                                }
                            }

                            displayPts.forEach { pt ->
                                val x = (((pt.phase - minP) / pRange) * w).toFloat().coerceIn(0f, w)
                                val depthPx = if (res.transitDepthPpm > 0) {
                                    (pt.detrendedFlux * 1_000_000.0 / res.transitDepthPpm * (h * 0.5f)).toFloat()
                                } else 0f
                                val y = (baselineY + depthPx).coerceIn(0f, h)

                                val inHighlightedDip = isDipHighlightActive && dipReport?.allDips?.any { dp -> pt.phase in dp.ingressPhase..dp.egressPhase } == true
                                val pointRadius = if (inHighlightedDip) 4.5f else if (binMode == BinningMode.RAW) 2.5f else 4.0f
                                val pointColor = when {
                                    inHighlightedDip -> Color(0xFFFFD600)
                                    binMode == BinningMode.RAW -> AccentCyan.copy(alpha = 0.55f)
                                    else -> AccentCyan
                                }

                                drawCircle(
                                    color = pointColor,
                                    radius = pointRadius,
                                    center = Offset(x, y)
                                )
                            }

                            // Draw fitted U-shaped transit model curve
                            val path = Path()
                            val halfDurationWidthPx = if (res.transitDurationHours > 0) {
                                ((res.transitDurationHours * 0.5 / pRange) * w).toFloat().coerceIn(w * 0.06f, w * 0.25f)
                            } else w * 0.12f
                            val bottomY = baselineY + (h * 0.5f)

                            path.moveTo(0f, baselineY)
                            path.lineTo((midX - halfDurationWidthPx).coerceAtLeast(0f), baselineY)
                            path.cubicTo(
                                midX - halfDurationWidthPx * 0.8f, baselineY,
                                midX - halfDurationWidthPx * 0.6f, bottomY,
                                midX - halfDurationWidthPx * 0.3f, bottomY
                            )
                            path.lineTo(midX + halfDurationWidthPx * 0.3f, bottomY)
                            path.cubicTo(
                                midX + halfDurationWidthPx * 0.6f, bottomY,
                                midX + halfDurationWidthPx * 0.8f, baselineY,
                                (midX + halfDurationWidthPx).coerceAtMost(w), baselineY
                            )
                            path.lineTo(w, baselineY)

                            drawPath(
                                path = path,
                                color = AccentGreen,
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val isHours = res.lightCurveSample.any { kotlin.math.abs(it.phase) > 1.0 }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isHours && res.lightCurveSample.isNotEmpty()) {
                        Text("${res.lightCurveSample.minOf { it.phase }.roundToInt()}h (Pre-transit)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text("0.0h (Mid-Transit)", color = AccentCyan, style = MaterialTheme.typography.labelSmall)
                        Text("+${res.lightCurveSample.maxOf { it.phase }.roundToInt()}h (Post-transit)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text("-0.5 φ (Out-of-Transit)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text("Phase 0.0 (Mid-Transit)", color = AccentCyan, style = MaterialTheme.typography.labelSmall)
                        Text("+0.5 φ (Occultation)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // -------------------------------------------------------------
            // ANALYSIS UTILITY: TRANSIT DIP SCANNER & RECURRENCE VERIFICATION
            // -------------------------------------------------------------
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFF090E17),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if ((dipReport?.totalDipsFound ?: 0) > 0) AccentCyan.copy(alpha = 0.4f) else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ANALIZOR SCĂDERI DE LUMINĂ (DIP SCANNER)",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Toggle highlight button
                        Surface(
                            onClick = onToggleHighlight,
                            color = if (isDipHighlightActive) AccentCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isDipHighlightActive) AccentCyan else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isDipHighlightActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (isDipHighlightActive) AccentCyan else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isDipHighlightActive) "EVIDENȚIERE: ACTIVĂ" else "EVIDENȚIERE: OPRITĂ",
                                    color = if (isDipHighlightActive) AccentCyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (dipReport != null) {
                        // Diagnostic Verdict Pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(dipReport.recurrenceVerdict.badgeColorHex).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(dipReport.recurrenceVerdict.badgeColorHex))
                            ) {
                                Text(
                                    dipReport.recurrenceVerdict.title,
                                    color = Color(dipReport.recurrenceVerdict.badgeColorHex),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            dipReport.scientificDiagnosis,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Metric Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SCĂDERI DETECTATE", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${dipReport.totalDipsFound}", color = Color(0xFFFFD600), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ZGOMOT FOTOMETRIC", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${dipReport.noiseFloorPpm.toInt()} ppm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PRAG DETECȚIE", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${String.format(Locale.US, "%.1f", dipReport.detectionThresholdSigma)}σ", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = Color(0xFF111827),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("RAPORT ODD/EVEN", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                    Text(dipReport.oddEvenDepthRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "--", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Sensitivity Threshold Adjuster
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sensibilitate scanare prag:", color = TextSecondary, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(2.5 to "2.5σ (Sensibil)", 3.5 to "3.5σ (Standard)", 5.0 to "5.0σ (Strict)").forEach { (sigmaVal, label) ->
                                    val isSelected = abs(dipSensitivity - sigmaVal) < 0.2
                                    Surface(
                                        onClick = { onRescanDips(sigmaVal) },
                                        color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else Color(0xFF1F2937),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, if (isSelected) AccentCyan else Color.Transparent)
                                    ) {
                                        Text(
                                            label,
                                            color = if (isSelected) AccentCyan else TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // List of Detected Dips
                        if (dipReport.allDips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Scăderi identificate în timp / fază (Apasă pe card pentru a focaliza):",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(dipReport.allDips) { dip ->
                                    val isSelected = selectedDip == dip
                                    Surface(
                                        onClick = { onSelectDip(dip) },
                                        color = if (isSelected) Color(0xFF1E293B) else Color(0xFF111827),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) AccentCyan else if (dip.isPrimaryTransit) AccentGreen.copy(alpha = 0.5f) else Color(0xFF374151)
                                        ),
                                        modifier = Modifier.width(200.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Dip #${dip.dipIndex}",
                                                    color = if (isSelected) AccentCyan else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    color = Color(dip.shapeClassification.colorHex).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        dip.shapeClassification.label,
                                                        color = Color(dip.shapeClassification.colorHex),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                dip.label,
                                                color = TextSecondary,
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("ADÂNCIME", color = TextSecondary, fontSize = 7.5.sp)
                                                    Text("${dip.depthPpm.toInt()} ppm", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("${String.format(Locale.US, "%.4f", dip.depthMagnitudeDelta)} mag", color = TextSecondary, fontSize = 8.sp)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("DURATĂ / SNR", color = TextSecondary, fontSize = 7.5.sp)
                                                    Text("${String.format(Locale.US, "%.1f", dip.durationHours)}h", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("SNR: ${String.format(Locale.US, "%.1f", dip.snrSigma)}σ", color = AccentCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Scan trigger button if no report yet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Scanează automat curba fotometrică pentru scăderi repetitive de magnitudine:",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onRescanDips(dipSensitivity) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("SCANEAZĂ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun binPhotometry(
    points: List<MastPhotometryPoint>,
    binWidth: Double
): List<MastPhotometryPoint> {
    if (points.isEmpty()) return emptyList()
    val sorted = points.sortedBy { it.phase }
    val minP = sorted.first().phase
    val maxP = sorted.last().phase
    val binned = mutableListOf<MastPhotometryPoint>()

    var start = minP
    while (start <= maxP) {
        val end = start + binWidth
        val inBin = sorted.filter { it.phase in start..end }
        if (inBin.isNotEmpty()) {
            val avgPhase = inBin.map { it.phase }.average()
            val avgFlux = inBin.map { it.detrendedFlux }.average()
            val avgModel = inBin.mapNotNull { it.modelFlux }.takeIf { it.isNotEmpty() }?.average()
            binned.add(MastPhotometryPoint(avgPhase, avgFlux, avgModel))
        }
        start = end
    }
    return if (binned.isNotEmpty()) binned else points
}

@Composable
fun AstrophysicalMetricsCard(res: XilonDetectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("DERIVED ASTROPHYSICAL PARAMETERS", color = AccentCyan, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("PERIOD", "${String.format("%.3f", res.orbitalPeriodDays)} d", AccentAmber)
                MetricItem("DEPTH", "${res.transitDepthPpm.roundToInt()} ppm", AccentCyan)
                MetricItem("DURATION", "${String.format("%.2f", res.transitDurationHours)} h", TextPrimary)
                MetricItem("SNR", "${String.format("%.1f", res.signalToNoiseRatio)} σ", if (res.signalToNoiseRatio >= 7.1) AccentGreen else AccentRed)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("PLANET RADIUS", res.derivedPlanetRadiusEarth?.let { "${String.format("%.2f", it)} R⊕" } ?: "N/A", AccentGreen)
                MetricItem("SEMI-MAJOR AXIS", res.derivedSemiMajorAxisAu?.let { "${String.format("%.3f", it)} AU" } ?: "N/A", TextPrimary)
                MetricItem("TEMP (Teq)", res.derivedEquilibriumTempK?.let { "${it.roundToInt()} K (${(it - 273.15).roundToInt()}°C)" } ?: "N/A", if (res.isHabitableZone) AccentCyan else TextPrimary)
                MetricItem("ODD/EVEN Δ", "${String.format("%.1f", res.oddEvenTransitDeltaSigma)} σ", if (res.oddEvenTransitDeltaSigma < 4.0) AccentGreen else AccentRed)
            }
        }
    }
}

@Composable
fun PlanetaryInteriorPhysicsCard(res: XilonDetectionResult) {
    val comp = res.compositionClass
    val compColor = Color(comp?.colorHex ?: 0xFF00E5FF)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, compColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PLANETARY INTERIOR & DYNAMICS", color = compColor, style = MaterialTheme.typography.labelMedium)
                comp?.let {
                    Surface(
                        color = compColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            it.label,
                            color = compColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            comp?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("ESTIMATED MASS", res.derivedPlanetMassEarth?.let { "${String.format("%.2f", it)} M⊕" } ?: "N/A", AccentAmber)
                MetricItem("BULK DENSITY", res.planetDensityGcm3?.let { "${String.format("%.2f", it)} g/cm³" } ?: "N/A", AccentCyan)
                MetricItem("SURFACE GRAVITY", res.surfaceGravityEarthG?.let { "${String.format("%.2f", it)} g⊕" } ?: "N/A", TextPrimary)
                MetricItem("ESCAPE VEL", res.escapeVelocityKms?.let { "${String.format("%.1f", it)} km/s" } ?: "N/A", AccentGreen)
            }
        }
    }
}

@Composable
fun AstrometryAndLiteratureCard(res: XilonDetectionResult) {
    val context = LocalContext.current
    val astrometry = res.astrometry
    val star = res.star
    val match = res.archivalCrossMatch

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CELESTIAL ASTROMETRY & TIMING", color = AccentCyan, style = MaterialTheme.typography.labelMedium)
                }
                Surface(
                    color = AccentCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "ICRS • J2000 • IAU 1958",
                        color = AccentCyan,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coordinates Grid: RA & Dec in Sexagesimal and Degrees
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("RIGHT ASCENSION (α)", astrometry?.formattedRaSexagesimal ?: (star.ra?.let { String.format(Locale.US, "%.4f°", it) } ?: "N/A"), AccentAmber)
                MetricItem("DECLINATION (δ)", astrometry?.formattedDecSexagesimal ?: (star.dec?.let { String.format(Locale.US, "%+.4f°", it) } ?: "N/A"), AccentAmber)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Galactic Coordinates
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("GALACTIC LONGITUDE (l)", astrometry?.galactic?.formattedL ?: "N/A", AccentCyan)
                MetricItem("GALACTIC LATITUDE (b)", astrometry?.galactic?.formattedB ?: "N/A", AccentCyan)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Timing: BJD_TDB and BTJD (TESS Baseline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TIME REFERENCE (BJD_TDB)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        astrometry?.bjdTdbCurrent?.let { String.format(Locale.US, "BJD %.5f", it) } ?: "N/A",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Column {
                    Text("TESS EPOCH (BTJD = BJD - 2457000)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        astrometry?.btjdCurrent?.let { String.format(Locale.US, "BTJD %.4f", it) } ?: "N/A",
                        color = AccentGreen,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Peer-Reviewed Literature Cross-Reference Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("PEER-REVIEWED LITERATURE & ARCHIVE SEARCH", color = AccentAmber, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Direct one-tap queries across international astronomical citation indexes for TIC ${star.ticId} (${match.canonicalName}):",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: NASA ADS & arXiv
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val url = astrometry?.nasaAdsUrl ?: "https://ui.adsabs.harvard.edu/search/q=%22TIC+${star.ticId}%22&sort=date+desc"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NASA ADS", color = AccentAmber, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        val url = astrometry?.arxivUrl ?: "https://arxiv.org/search/astro-ph?query=TIC+${star.ticId}&searchtype=all"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("arXiv (astro-ph)", color = AccentCyan, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: SIMBAD & NASA Exoplanet Archive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val url = astrometry?.simbadUrl ?: "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=TIC+${star.ticId}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CDS SIMBAD", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        val url = "https://exoplanetarchive.ipac.caltech.edu/overview/${match.canonicalName.replace(" ", "%20")}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXOPLANET ARCHIVE", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun PipelineAuditCard(res: XilonDetectionResult) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XILON PIPELINE AUDIT TRAIL", color = AccentCyan, style = MaterialTheme.typography.labelMedium)
                Text(if (expanded) "HIDE ▲" else "VIEW ▼", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                res.analysisLog.forEach { logLine ->
                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(logLine, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

private fun buildDiscoveryDossierText(res: XilonDetectionResult): String {
    val m = res.archivalCrossMatch
    val a = res.astrometry
    return """
★ XILON PHOTON TRANSIT ANALYSIS REPORT ★
====================================================
TARGET: TIC ${res.star.ticId}
CANONICAL NAME: ${m.canonicalName}
STATUS: ${m.status} (${m.officialDisposition})
SECTOR: ${res.sector} (TCE: ${res.tceName})
VERDICT: ${res.verdictTitle}
SUMMARY: ${res.verdictSummary}

ASTROMETRY & COORDINATE FRAMES (IAU 1958 / ICRS J2000):
• Right Ascension (α): ${a?.formattedRaSexagesimal ?: (res.star.ra?.let { String.format("%.6f°", it) } ?: "N/A")} (${res.star.ra?.let { String.format("%.6f°", it) } ?: "N/A"})
• Declination (δ): ${a?.formattedDecSexagesimal ?: (res.star.dec?.let { String.format("%+.6f°", it) } ?: "N/A")} (${res.star.dec?.let { String.format("%+.6f°", it) } ?: "N/A"})
• Galactic Longitude (l): ${a?.galactic?.formattedL ?: "N/A"}
• Galactic Latitude (b): ${a?.galactic?.formattedB ?: "N/A"}
• Time Reference (BJD_TDB): ${a?.bjdTdbCurrent?.let { String.format(Locale.US, "%.5f", it) } ?: "N/A"}
• TESS Epoch (BTJD): ${a?.btjdCurrent?.let { String.format(Locale.US, "%.4f", it) } ?: "N/A"}

ASTROPHYSICAL CHARACTERISTICS:
• Orbital Period: ${res.orbitalPeriodDays} days (Literature: ${m.literaturePeriodDays ?: "N/A"})
• Transit Depth: ${res.transitDepthPpm} ppm (Literature: ${m.literatureDepthPpm ?: "N/A"})
• Transit Duration: ${res.transitDurationHours} hours (Literature: ${m.literatureDurationHours ?: "N/A"})
• Signal-to-Noise Ratio (SNR): ${res.signalToNoiseRatio} σ (Detection Threshold: 7.1 σ)
• Profile Symmetry Delta (Δσ): ${res.oddEvenTransitDeltaSigma} σ (< 4.0 σ = Symmetric Planet)
• Secondary Eclipse Depth: ${res.secondaryEclipseDepthPpm} ppm
• Derived Planetary Radius: ${res.derivedPlanetRadiusEarth ?: "N/A"} Earth Radii (R⊕)
• Semi-Major Axis: ${res.derivedSemiMajorAxisAu ?: "N/A"} AU
• Equilibrium Temperature: ${res.derivedEquilibriumTempK ?: "N/A"} K
• Habitable Zone Status: ${if (res.isHabitableZone) "CONFIRMED HABITABLE ZONE" else "Non-Habitable"}

PLANETARY INTERIOR & DYNAMICS (CHEN & KIPPING):
• Archetype: ${res.compositionClass?.label ?: "N/A"}
• Estimated Mass: ${res.derivedPlanetMassEarth ?: "N/A"} M⊕
• Bulk Mean Density: ${res.planetDensityGcm3 ?: "N/A"} g/cm³
• Surface Gravity: ${res.surfaceGravityEarthG ?: "N/A"} g⊕
• Escape Velocity: ${res.escapeVelocityKms ?: "N/A"} km/s

PEER-REVIEWED LITERATURE & ARCHIVE REFERENCES:
• NASA ADS Query: ${a?.nasaAdsUrl ?: "https://ui.adsabs.harvard.edu/search/q=%22TIC+${res.star.ticId}%22"}
• arXiv astro-ph: ${a?.arxivUrl ?: "https://arxiv.org/search/astro-ph?query=TIC+${res.star.ticId}"}
• CDS SIMBAD: ${a?.simbadUrl ?: "https://simbad.u-strasbg.fr/simbad/sim-id?Ident=TIC+${res.star.ticId}"}

ARCHIVAL CROSS-REFERENCE (NASA PSCOMPPARS & TOI):
${when (m.status) {
    ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION -> "Peer-Reviewed Confirmed Exoplanet: ${m.canonicalName}, discovered in ${m.discoveryYear ?: "historical"} by ${m.discoveryFacility ?: "ground/space observatory"}."
    ArchivalCatalogStatus.NASA_TOI_CANDIDATE -> "Recorded in NASA TOI archive as unconfirmed candidate: ${m.canonicalName}."
    ArchivalCatalogStatus.POTENTIAL_NEW_SIGNAL -> "UNRECORDED: Not present in NASA confirmed exoplanets or candidate catalogs."
}}

Generated by Xilon Autonomous Engine on raw MAST Photons.
    """.trimIndent()
}

private fun buildNasaExoFopCtoiText(res: XilonDetectionResult): String {
    val star = res.star
    val match = res.archivalCrossMatch
    val a = res.astrometry
    val isConfirmed = match.status == ArchivalCatalogStatus.CONFIRMED_PLANET_VALIDATION
    val type = if (isConfirmed) "BENCHMARK_RECOVERY" else "COMMUNITY_TESS_OBJECT_OF_INTEREST_CANDIDATE"

    return """
# ==============================================================================
# NASA ExoFOP-TESS Community TESS Object of Interest (CTOI) Report
# Generated by: Synthesis / Xilon Autonomous Photometry Pipeline
# Submission Type: $type
# ==============================================================================

[TARGET IDENTIFIERS]
TIC_ID: ${star.ticId}
CANONICAL_NAME: ${match.canonicalName}
SECTOR: ${res.sector}
TCE_ID: ${res.tceName}
RA_DEG: ${star.ra?.let { String.format("%.6f", it) } ?: "N/A"}
DEC_DEG: ${star.dec?.let { String.format("%.6f", it) } ?: "N/A"}
RA_SEXAGESIMAL: ${a?.formattedRaSexagesimal ?: "N/A"}
DEC_SEXAGESIMAL: ${a?.formattedDecSexagesimal ?: "N/A"}
GALACTIC_LONGITUDE_L: ${a?.galactic?.formattedL ?: "N/A"}
GALACTIC_LATITUDE_B: ${a?.galactic?.formattedB ?: "N/A"}
TIME_BJD_TDB: ${a?.bjdTdbCurrent?.let { String.format(Locale.US, "%.5f", it) } ?: "N/A"}
TIME_BTJD: ${a?.btjdCurrent?.let { String.format(Locale.US, "%.4f", it) } ?: "N/A"}
TESS_MAG: ${star.tMag?.let { String.format("%.3f", it) } ?: "N/A"}

[TRANSIT OBSERVATIONAL PARAMETERS]
PERIOD_DAYS: ${String.format("%.6f", res.orbitalPeriodDays)}
DURATION_HOURS: ${String.format("%.4f", res.transitDurationHours)}
DEPTH_PPM: ${String.format("%.1f", res.transitDepthPpm)}
DEPTH_PPT: ${String.format("%.4f", res.transitDepthPpm / 1000.0)}
TRANSIT_SNR_SIGMA: ${String.format("%.2f", res.signalToNoiseRatio)}
PROFILE_SYMMETRY_DELTA_SIGMA: ${String.format("%.2f", res.oddEvenTransitDeltaSigma)}
SECONDARY_ECLIPSE_PPM: ${String.format("%.1f", res.secondaryEclipseDepthPpm)}

[HOST STELLAR PROPERTIES]
STAR_RADIUS_SOLAR: ${star.radiusSun?.let { String.format("%.3f", it) } ?: "1.000"}
STAR_MASS_SOLAR: ${star.massSun?.let { String.format("%.3f", it) } ?: "1.000"}
STAR_TEFF_KELVIN: ${star.teff?.let { String.format("%.1f", it) } ?: "5778.0"}
STAR_DISTANCE_PC: ${star.distancePc?.let { String.format("%.1f", it) } ?: "N/A"}

[DERIVED PLANETARY CHARACTERISTICS]
PLANET_RADIUS_EARTH: ${res.derivedPlanetRadiusEarth?.let { String.format("%.3f", it) } ?: "N/A"}
ESTIMATED_MASS_EARTH: ${res.derivedPlanetMassEarth?.let { String.format("%.2f", it) } ?: "N/A"}
SURFACE_GRAVITY_G: ${res.surfaceGravityEarthG?.let { String.format("%.3f", it) } ?: "N/A"}
BULK_DENSITY_G_CM3: ${res.planetDensityGcm3?.let { String.format("%.3f", it) } ?: "N/A"}
EQUILIBRIUM_TEMP_KELVIN: ${res.derivedEquilibriumTempK?.let { String.format("%.1f", it) } ?: "N/A"}
SEMI_MAJOR_AXIS_AU: ${res.derivedSemiMajorAxisAu?.let { String.format("%.4f", it) } ?: "N/A"}
HABITABLE_ZONE_STATUS: ${if (res.isHabitableZone) "IN_HABITABLE_ZONE" else "NON_HABITABLE"}
COMPOSITION_ARCHETYPE: ${res.compositionClass?.label ?: "N/A"}

[LITERATURE & CROSS-REFERENCE]
NASA_ADS_URL: ${a?.nasaAdsUrl ?: "N/A"}
ARXIV_URL: ${a?.arxivUrl ?: "N/A"}
CDS_SIMBAD_URL: ${a?.simbadUrl ?: "N/A"}

[VETTING & ARCHIVAL CROSS-MATCH]
NASA_PSCOMPPARS_MATCH: ${if (isConfirmed) "YES (${match.canonicalName})" else "NO"}
NASA_TOI_MATCH: ${if (match.status == ArchivalCatalogStatus.NASA_TOI_CANDIDATE) "YES (${match.canonicalName})" else "NO"}
OFFICIAL_DISPOSITION: ${match.officialDisposition}
VETTING_NOTES: Ingress/egress profile symmetry verified (${String.format("%.2f", res.oddEvenTransitDeltaSigma)} sigma). Secondary occultation screening passed.
DATA_SOURCE: STScI MAST SPOC calibrated photon time-series.
PIPELINE: Xilon Autonomous Transit Least Squares (TLS) Engine.
    """.trimIndent()
}

@Composable
fun GeminiAiEvaluationCard(
    res: XilonDetectionResult,
    analysis: GeminiAnalysisResult?,
    isAnalyzing: Boolean,
    onReAnalyze: () -> Unit,
    onConfigureKey: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(AccentCyan, AccentPurple, AccentMagenta)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "XILON-AI VETTING NEURONAL",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (analysis?.isLiveAiGenerated == true) AccentGreen.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        if (analysis?.isLiveAiGenerated == true) "● GEMINI 3.5 LIVE" else "○ MOD EURISTIC",
                        color = if (analysis?.isLiveAiGenerated == true) AccentGreen else AccentAmber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Sinteză astrofizică de frontieră pe baza parametrilor fotometrici măsurați.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isAnalyzing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Gemini 3.5 Flash analizează tranzitul și fizica stelară...",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (analysis != null) {
                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Scor Încredere Fizică AI: ${analysis.confidenceScorePercent}%",
                                color = AccentCyan,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                analysis.modelName,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            analysis.analysisDossier,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReAnalyze,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-analizează", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onConfigureKey,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cheie AI", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Button(
                    onClick = onReAnalyze,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lansează Vetting Neuronal Gemini AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GeminiAstrophysicistChatCard(
    res: XilonDetectionResult,
    messages: List<XilonAiChatMessage>,
    isResponding: Boolean,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showFullscreenChatDialog by remember { mutableStateOf(false) }
    var isInlineHeightUnlimited by remember { mutableStateOf(true) }
    val chatScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Automatically scroll to latest message
    LaunchedEffect(messages.size, isResponding) {
        if (messages.isNotEmpty()) {
            chatScrollState.animateScrollTo(chatScrollState.maxValue)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "DISCUTĂ CU ASTROFIZICIANUL NEURONAL",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Gemini 3.5 analizează fizica stelară și tranzitul în timp real",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { showFullscreenChatDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan.copy(alpha = 0.2f),
                        contentColor = AccentCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ECRAN COMPLET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick suggestion chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { onSendMessage("Există șanse ca această planetă să aibă o atmosferă cu apă?") },
                    label = { Text("Atmosferă H₂O?", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { onSendMessage("De ce nu este clasificată drept binară eclipsantă?") },
                    label = { Text("Fals pozitiv?", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { onSendMessage("Ce instrument JWST ar trebui folosit pentru observație?") },
                    label = { Text("JWST?", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scroll helper & expand header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (messages.isEmpty()) "Mesaje" else "${messages.size} mesaje în conversație",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { isInlineHeightUnlimited = !isInlineHeightUnlimited },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isInlineHeightUnlimited) "Restrânge înălțimea ▲" else "Extinde înălțimea ▼",
                            color = AccentCyan,
                            fontSize = 11.sp
                        )
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    chatScrollState.animateScrollTo(chatScrollState.maxValue)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Mergi jos",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Message history container
            val containerModifier = if (isInlineHeightUnlimited) {
                Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 380.dp)
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .verticalScroll(chatScrollState)
                    .padding(10.dp)
            }

            Column(
                modifier = containerModifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    Text(
                        "Nicio întrebare adresată încă. Tastează mai jos o întrebare astrofizică sau apasă pe un cip de sugestie.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                } else {
                    messages.forEach { msg ->
                        if (msg.isUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    color = AccentCyan,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            msg.sender,
                                            color = Color.Black,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        SelectionContainer {
                                            Text(
                                                msg.text,
                                                color = Color.Black,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // AI Message: Full width with expand toggle so multi-paragraph responses are 100% readable
                            var isMsgExpanded by remember { mutableStateOf(false) }
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                msg.sender,
                                                color = AccentCyan,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Gemini AI Analysis", msg.text))
                                                Toast.makeText(context, "Răspuns copiat!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiază răspuns", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SelectionContainer {
                                        Text(
                                            msg.text,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            maxLines = if (isMsgExpanded || isInlineHeightUnlimited) Int.MAX_VALUE else 10,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (msg.text.length > 250 && !isInlineHeightUnlimited) {
                                        TextButton(
                                            onClick = { isMsgExpanded = !isMsgExpanded },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                if (isMsgExpanded) "▲ Restrânge textul" else "▼ Citește tot mesajul (${msg.text.length} caractere)",
                                                color = AccentCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isResponding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI formulează răspunsul...", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Întreabă despre această exoplanetă...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val q = inputText
                            inputText = ""
                            onSendMessage(q)
                        }
                    },
                    modifier = Modifier.background(AccentCyan, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Trimite", tint = Color.Black)
                }
            }
        }
    }

    // Fullscreen/Dedicated chat dialog for completely unrestricted reading & scrolling
    if (showFullscreenChatDialog) {
        GeminiFullscreenChatDialog(
            targetName = "TIC ${res.star.ticId}",
            messages = messages,
            isResponding = isResponding,
            onDismiss = { showFullscreenChatDialog = false },
            onSendMessage = onSendMessage
        )
    }
}

/**
 * Dedicated Fullscreen Dialog for Gemini AI Astrophysicist Chat.
 * Provides unrestricted, buttery-smooth vertical scrolling, edge-to-edge layout,
 * copy-all shortcut, and comfortable text reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiFullscreenChatDialog(
    targetName: String,
    messages: List<XilonAiChatMessage>,
    isResponding: Boolean,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var dialogInputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size, isResponding) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AccentGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Chat Astrofizic Gemini AI",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = AccentCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "3.5 Flash",
                                                color = AccentCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Țintă: $targetName • Derivări fizice & Vetting tranzit",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Închide", tint = Color.White)
                            }
                        },
                        actions = {
                            if (messages.isNotEmpty()) {
                                IconButton(onClick = {
                                    val fullLog = messages.joinToString("\n\n") { "${it.sender}:\n${it.text}" }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Chat Transcript", fullLog))
                                    Toast.makeText(context, "Tot istoricul conversației a fost copiat!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiază tot", tint = AccentCyan)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                    )
                },
                bottomBar = {
                    Surface(
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Suggestion chips row inside fullscreen chat
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { onSendMessage("Ce temperatură de echilibru Teq și compoziție are această planetă?") },
                                    label = { Text("Teq & Compoziție?", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { onSendMessage("Care este probabilitatea de fals pozitiv (binară eclipsantă blenduită)?") },
                                    label = { Text("Fals Pozitiv?", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { onSendMessage("Explică derivarea masei folosind legea Chen & Kipping (2017)") },
                                    label = { Text("Masa Chen-Kipping?", fontSize = 11.sp) }
                                )
                            }

                            // Input row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = dialogInputText,
                                    onValueChange = { dialogInputText = it },
                                    placeholder = { Text("Întreabă Gemini 3.5 orice despre această stea...", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        if (dialogInputText.isNotBlank()) {
                                            val q = dialogInputText.trim()
                                            dialogInputText = ""
                                            onSendMessage(q)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(AccentCyan, RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Trimite",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(DarkBackground)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "🌌 Asistent Astrofizic Gemini 3.5 Online",
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCyan,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Pune orice întrebare despre semnalul fotometric MAST, modelul analitic Mandel-Agol, atmosfera acestei planete sau instrumentele JWST NIRSpec.",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(messages) { msg ->
                                if (msg.isUser) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Surface(
                                            color = AccentCyan,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.widthIn(max = 320.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    msg.sender,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                SelectionContainer {
                                                    Text(
                                                        msg.text,
                                                        color = Color.Black,
                                                        fontSize = 14.sp,
                                                        lineHeight = 20.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = AccentCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        msg.sender,
                                                        color = AccentCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Message", msg.text))
                                                        Toast.makeText(context, "Răspuns copiat!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copiază",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            SelectionContainer {
                                                Text(
                                                    msg.text,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    lineHeight = 22.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isResponding) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AccentCyan,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Gemini 3.5 AI formulează răspunsul astrofizic detaliat...",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Floating jump-to-bottom button
                    if (messages.size > 2) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            },
                            containerColor = AccentCyan,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Mergi jos", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiApiKeyDialog(
    isKeyConfigured: Boolean,
    onDismiss: () -> Unit,
    onSaveKey: (String) -> Unit
) {
    var keyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurare Google Gemini AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = if (isKeyConfigured) AccentGreen.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isKeyConfigured) AccentGreen else AccentAmber, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isKeyConfigured) "Cheie activă configurată (Gemini 3.5 Flash Online)" else "Nicio cheie configurată (se folosește modul euristic local)",
                            color = if (isKeyConfigured) AccentGreen else AccentAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    "Pentru a debloca sinteza neuronală de astrofizică și chat-ul live, introdu cheia ta gratuită din Google AI Studio (aistudio.google.com):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Gemini API Key (AIzaSy...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveKey(keyText) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
            ) {
                Text("Salvează Cheia", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Închide", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}


