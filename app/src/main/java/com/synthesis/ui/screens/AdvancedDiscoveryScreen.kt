package com.synthesis.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.synthesis.R
import com.synthesis.ui.screens.cosmology.BigBangUniverseScreen
import com.synthesis.ui.screens.cosmology.BlackHoleRelativityScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.exoplanet.*
import com.synthesis.service.*
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.exoplanet.AdvancedDiscoveryViewModel
import com.synthesis.viewmodel.exoplanet.AdvancedTab
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedDiscoveryScreen(
    onNavigateBack: () -> Unit,
    initialTab: AdvancedTab? = null,
    viewModel: AdvancedDiscoveryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.selectTab(initialTab)
        }
    }

    var showAllModulesDialog by remember { mutableStateOf(false) }
    val biosignatures by viewModel.biosignatures.collectAsState()
    val technosignatures by viewModel.technosignatures.collectAsState()
    val dilutionRescues by viewModel.dilutionRescues.collectAsState()
    val harmonicStitches by viewModel.harmonicStitches.collectAsState()
    val liveTransits by viewModel.liveTransits.collectAsState()
    val climateResult by viewModel.climateResult.collectAsState()

    val isPro by viewModel.isPro.collectAsState()
    val remainingTrialSeconds by viewModel.remainingTrialSeconds.collectAsState()
    val exportModalContent by viewModel.exportModalContent.collectAsState()
    val alertSuccessMessage by viewModel.alertSuccessMessage.collectAsState()
    val mountTelemetry by viewModel.mountTelemetry.collectAsState()
    val liveSession by viewModel.liveSession.collectAsState()

    // 2026 Modules State
    val smartScopeTelemetry by viewModel.smartScopeTelemetry.collectAsState()
    val platoTargets by viewModel.platoTargets.collectAsState()
    val jwstSpectra by viewModel.jwstSpectra.collectAsState()
    val exomoons by viewModel.exomoons.collectAsState()
    val spaceWeatherAssessments by viewModel.spaceWeatherAssessments.collectAsState()
    val telescopeVisionState by viewModel.telescopeVisionState.collectAsState()
    val telescopeViewfinderBitmap by viewModel.telescopeViewfinderBitmap.collectAsState()
    val periodicTransitResult by viewModel.periodicTransitResult.collectAsState()
    val exportedPdfFile by viewModel.exportedPdfFile.collectAsState()

    var showDeblendDialog by remember { mutableStateOf(false) }

    var pendingAlertTarget by remember { mutableStateOf<LiveTransitTarget?>(null) }
    var showGpsAlertRationaleDialog by remember { mutableStateOf(false) }

    val gpsAlertPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        pendingAlertTarget?.let { target ->
            viewModel.triggerGpsTransitNotification(target)
        }
        pendingAlertTarget = null
        showGpsAlertRationaleDialog = false
    }

    val remainingMinutes = remainingTrialSeconds / 60

    if (showGpsAlertRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showGpsAlertRationaleDialog = false
                pendingAlertTarget?.let { viewModel.triggerGpsTransitNotification(it) }
                pendingAlertTarget = null
            },
            icon = {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    stringResource(R.string.perm_gps_notif_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    stringResource(R.string.perm_gps_notif_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val permsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        gpsAlertPermissionLauncher.launch(permsToRequest.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    modifier = Modifier.testTag("grant_gps_notif_button")
                ) {
                    Text(
                        stringResource(R.string.perm_btn_grant_gps_notif),
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGpsAlertRationaleDialog = false
                        pendingAlertTarget?.let { viewModel.triggerGpsTransitNotification(it) }
                        pendingAlertTarget = null
                    },
                    modifier = Modifier.testTag("use_estimated_coords_button")
                ) {
                    Text(
                        stringResource(R.string.perm_btn_use_estimated_coords),
                        color = TextSecondary
                    )
                }
            },
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.testTag("gps_transit_alert_dialog")
        )
    }

    BackHandler(enabled = selectedTab != AdvancedTab.SMART_SCOPE) {
        viewModel.selectTab(AdvancedTab.SMART_SCOPE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "DISCOVERY & GOTO LAB",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isPro) {
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "PRO",
                                        color = AccentGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Surface(
                                    color = AccentAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "TRIAL: ${remainingMinutes}m",
                                        color = AccentAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "Biosignatures, De-blender, GPS Radar & Physical Telescope GoTo",
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
                    IconButton(
                        onClick = {
                            viewModel.refreshActiveModuleData()
                            Toast.makeText(context, "Modulul '${selectedTab.title}' a fost reîmprospătat!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reîmprospătează date modul",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { showAllModulesDialog = true }
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Catalog Toate Modulele",
                            tint = AccentCyan
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.subscriptionManager.forceExpireTrialForTesting()
                            Toast.makeText(context, "Perioada de test a fost setată pe expirat. Ecranul de blocare este activ!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.HourglassBottom,
                            contentDescription = "Test 1H Lockout",
                            tint = AccentAmber
                        )
                    }
                    IconButton(onClick = { showDeblendDialog = true }) {
                        Icon(
                            Icons.Default.FilterCenterFocus,
                            contentDescription = "Rescue FP Dialog",
                            tint = AccentGreen
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentModuleToResearchArchive(selectedTab)
                            Toast.makeText(context, "Sinteza modulului '${selectedTab.title}' a fost salvată în Baza de Date Room!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Salvează în Arhivă Room",
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
            var showOnly2026 by remember { mutableStateOf(false) }
            val displayedTabs = remember(showOnly2026) {
                if (showOnly2026) {
                    AdvancedTab.values().filter { it.isYear2026 }
                } else {
                    AdvancedTab.values().toList()
                }
            }

            // 2026 Category Toggle Bar with Direct Module Menu Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAllModulesDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan.copy(alpha = 0.2f),
                        contentColor = AccentCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MENIU TOATE MODULELE (18)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilterChip(
                    selected = showOnly2026,
                    onClick = {
                        showOnly2026 = true
                        if (!selectedTab.isYear2026) {
                            viewModel.selectTab(AdvancedTab.SMART_SCOPE)
                        }
                    },
                    label = { Text("🚀 Noi 2026/27", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan,
                        selectedLabelColor = Color.Black
                    )
                )

                FilterChip(
                    selected = !showOnly2026,
                    onClick = { showOnly2026 = false },
                    label = { Text("🔬 Toate (18)", fontSize = 11.sp) }
                )
            }

            val currentTabIndex = displayedTabs.indexOf(selectedTab).coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = currentTabIndex,
                containerColor = DarkSurface,
                contentColor = AccentCyan,
                edgePadding = 16.dp
            ) {
                displayedTabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                tab.title,
                                color = if (selectedTab == tab) AccentCyan else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                AdvancedTab.BIOSIGNATURES -> AstrobiologyView(biosignatures, technosignatures)
                AdvancedTab.FALSE_POSITIVE_RESCUE -> FalsePositiveRescueView(
                    rescues = dilutionRescues,
                    onOpenDeblender = { showDeblendDialog = true }
                )
                AdvancedTab.HARMONIC_STITCHER -> HarmonicStitcherView(harmonicStitches)
                AdvancedTab.CLIMATE_SIMULATOR -> ClimateSimulatorView(
                    result = climateResult,
                    onSimulate = { name, insolation, co2, press ->
                        viewModel.updateClimate(name, insolation, co2, press)
                    }
                )
                AdvancedTab.SKY_RADAR -> SkyRadarView(
                    transits = liveTransits,
                    onSendGpsAlert = { target ->
                        val hasLoc = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true

                        if (!hasLoc || !hasNotif) {
                            pendingAlertTarget = target
                            showGpsAlertRationaleDialog = true
                        } else {
                            viewModel.triggerGpsTransitNotification(target)
                        }
                    },
                    onExport = { target, format ->
                        viewModel.exportTargetFormat(target, format)
                    },
                    onSlewMount = { target ->
                        viewModel.selectTab(AdvancedTab.TELESCOPE_CONTROL)
                        if (!mountTelemetry.isConnected) {
                            val usbConnected = viewModel.connectMountUsb()
                            if (!usbConnected) {
                                viewModel.connectMountSim()
                            }
                        }
                        viewModel.slewMountToTarget(target)
                        val modeLabel = if (mountTelemetry.connectionMode == MountConnectionMode.USB_SERIAL_OTG) "Hardware USB" else "Goto"
                        Toast.makeText(context, "Comandă $modeLabel trimisă: Orientare motoare către ${target.candidateName}...", Toast.LENGTH_SHORT).show()
                    }
                )
                AdvancedTab.FITS_SUMMARY -> FitsHeaderSummaryScreen(
                    header = telescopeVisionState.lastLoadedFitsHeader,
                    visionState = telescopeVisionState,
                    session = liveSession,
                    periodicResult = periodicTransitResult,
                    onIngestFitsBytes = { bytes -> viewModel.ingestTelescopeFitsBytes(bytes) },
                    onLoadPresetTarget = { preset -> viewModel.loadEmpiricalWasp12bDataset() },
                    onRunPeriodicBlsScan = { pMin, pMax, rStar, mStar ->
                        viewModel.runPeriodicBlsScanOnVision(pMin, pMax, rStar, mStar)
                    },
                    onNavigateToVisionEngine = { viewModel.selectTab(AdvancedTab.TELESCOPE_VISION) },
                    onTransferToOfficialReport = {
                        viewModel.transferVisionDataToOfficialSession()
                        viewModel.selectTab(AdvancedTab.LIVE_PHOTOMETRY)
                    },
                    onAdjustSnrThreshold = { threshold ->
                        viewModel.updateSnrFilterThreshold(threshold)
                    },
                    onExportPdfReport = {
                        viewModel.exportFitsSummaryPdf(context)
                    },
                    onSharePdfReport = {
                        viewModel.shareFitsSummaryPdf(context)
                    },
                    onOpenPdfReport = {
                        viewModel.openFitsSummaryPdf(context)
                    },
                    exportedPdfFile = exportedPdfFile
                )
                AdvancedTab.TELESCOPE_VISION -> TelescopeCameraVisionView(
                    visionState = telescopeVisionState,
                    viewfinderBitmap = telescopeViewfinderBitmap,
                    onStartPipeline = { src -> viewModel.startTelescopeVision(src) },
                    onStopPipeline = { viewModel.stopTelescopeVision() },
                    onResetPhotometry = { viewModel.resetTelescopeVisionPhotometry() },
                    onSelectStarAt = { x, y, isTarget -> viewModel.selectTelescopeVisionStar(x, y, isTarget) },
                    onIngestFitsBytes = { bytes -> viewModel.ingestTelescopeFitsBytes(bytes) },
                    onUpdateApertureRadii = { ap, skyIn, skyOut -> viewModel.updateTelescopeApertureRadii(ap, skyIn, skyOut) },
                    onTransferToOfficialSession = {
                        viewModel.transferVisionDataToOfficialSession()
                        viewModel.selectTab(AdvancedTab.LIVE_PHOTOMETRY)
                    },
                    onRunPeriodicBlsScan = { pMin, pMax, rStar, mStar ->
                        viewModel.runPeriodicBlsScanOnVision(pMin, pMax, rStar, mStar)
                    },
                    onOpenFitsSummary = { viewModel.selectTab(AdvancedTab.FITS_SUMMARY) }
                )
                AdvancedTab.TELESCOPE_CONTROL -> TelescopeControlView(
                    telemetry = mountTelemetry,
                    transits = liveTransits,
                    onConnectUsb = {
                        val ok = viewModel.connectMountUsb()
                        if (!ok) {
                            Toast.makeText(context, "Niciun cablu USB detectat. Treci pe simulator pentru test.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onConnectWifi = { ip, port -> viewModel.connectMountWifi(ip, port) },
                    onConnectSim = { viewModel.connectMountSim() },
                    onDisconnect = { viewModel.disconnectMount() },
                    onSlewTarget = { target -> viewModel.slewMountToTarget(target) },
                    onJog = { dir, rate -> viewModel.jogMount(dir, rate) },
                    onEmergencyStop = { viewModel.emergencyStopMount() },
                    onPark = { viewModel.parkMount() },
                    onToggleTracking = { viewModel.toggleMountTracking() }
                )
                AdvancedTab.LIVE_PHOTOMETRY -> LivePhotometryView(
                    session = liveSession,
                    onImportRealData = { text, target, host -> viewModel.importRealPhotometricFile(text, target, host) },
                    onLoadEmpiricalDataset = { viewModel.loadEmpiricalWasp12bDataset() },
                    onUpdateObserver = { name, code, loc, tel, cam, filt, cStar, cMag, kStar, kMag ->
                        viewModel.updateObserverSettings(name, code, loc, tel, cam, filt, cStar, cMag, kStar, kMag)
                    },
                    onReset = { viewModel.resetPhotometrySession() },
                    onGenerateEtd = { viewModel.generateOfficialEtdReport() },
                    onGenerateAavso = { viewModel.generateOfficialAavsoReport() },
                    onOpenVisionEngine = { viewModel.selectTab(AdvancedTab.TELESCOPE_VISION) },
                    periodicResult = periodicTransitResult,
                    onRunPeriodicBlsScan = { pMin, pMax, rStar, mStar ->
                        viewModel.runBlsAnalysisOnCurrentData(pMin, pMax, rStar, mStar)
                    }
                )
                AdvancedTab.SMART_SCOPE -> SmartScopeView(
                    telemetry = smartScopeTelemetry,
                    transits = liveTransits,
                    onConnect = { model, ip, port -> viewModel.connectSmartScope(model, ip, port) },
                    onDisconnect = { viewModel.disconnectSmartScope() },
                    onSlewTarget = { target -> viewModel.slewSmartScopeToTarget(target) },
                    onStartSequence = { exp, gain, filter -> viewModel.startSmartScopeSequence(exp, gain, filter) },
                    onStopSequence = { viewModel.stopSmartScopeSequence() },
                    onOpenVisionEngine = { viewModel.selectTab(AdvancedTab.TELESCOPE_VISION) }
                )
                AdvancedTab.BIG_BANG_COSMOLOGY -> BigBangUniverseScreen(
                    engine = viewModel.bigBangCosmologyEngine
                )
                AdvancedTab.BLACK_HOLES -> BlackHoleRelativityScreen(
                    engine = viewModel.blackHoleRelativityEngine
                )
                AdvancedTab.PLATO_ASTEROSEISMOLOGY -> PlatoAsteroseismologyView(
                    targets = platoTargets
                )
                AdvancedTab.JWST_SPECTROSCOPY -> JwstTransmissionSpectroscopyView(
                    spectra = jwstSpectra
                )
                AdvancedTab.ROMAN_MICROLENSING -> RomanMicrolensingView(
                    engine = viewModel.romanMicrolensingEngine
                )
                AdvancedTab.AR_SKY_CAMERA -> ArSkyCameraScreen(
                    arService = viewModel.arSkyTrackerService,
                    onNavigateBack = { viewModel.selectTab(AdvancedTab.SMART_SCOPE) }
                )
                AdvancedTab.EXOMOON_OCEANS -> ExomoonTidalOceanView(
                    exomoons = exomoons
                )
                AdvancedTab.SPACE_WEATHER -> SpaceWeatherMagnetosphereView(
                    assessments = spaceWeatherAssessments
                )
            }
        }
    }

    if (showDeblendDialog) {
        CustomDeblendDialog(
            onDismiss = { showDeblendDialog = false },
            onDeblend = { name, tic, depth, tMag, nMag, sep, rad ->
                viewModel.rescueCustomCandidate(
                    name = name,
                    ticId = tic,
                    status = "False Positive (FP - Blended)",
                    depthPpm = depth,
                    targetMag = tMag,
                    neighborMag = nMag,
                    sepArcsec = sep,
                    radSun = rad
                )
                showDeblendDialog = false
            }
        )
    }

    if (exportModalContent != null) {
        val (formatTitle, content) = exportModalContent!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportModal() },
            title = { Text(formatTitle, color = AccentCyan) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Telescope Target", content))
                        Toast.makeText(context, "Fișier copiat în clipboard!", Toast.LENGTH_SHORT).show()
                        viewModel.dismissExportModal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPIAZĂ ÎN CLIPBOARD")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExportModal() }) {
                    Text("ÎNCHIDE", color = TextSecondary)
                }
            }
        )
    }

    if (showAllModulesDialog) {
        AllModulesCatalogDialog(
            currentTab = selectedTab,
            onSelectTab = { tab ->
                viewModel.selectTab(tab)
                showAllModulesDialog = false
            },
            onDismiss = { showAllModulesDialog = false }
        )
    }

    if (alertSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAlertMessage() },
            icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentAmber) },
            title = { Text("Alertă Tranzit Live Transmisă", color = AccentAmber) },
            text = { Text(alertSuccessMessage!!, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAlertMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black)
                ) {
                    Text("AM ÎNȚELES")
                }
            }
        )
    }
}

@Composable
private fun TelescopeControlView(
    telemetry: MountTelemetry,
    transits: List<LiveTransitTarget>,
    onConnectUsb: () -> Unit,
    onConnectWifi: (String, Int) -> Unit,
    onConnectSim: () -> Unit,
    onDisconnect: () -> Unit,
    onSlewTarget: (LiveTransitTarget) -> Unit,
    onJog: (SlewDirection, SlewRate) -> Unit,
    onEmergencyStop: () -> Unit,
    onPark: () -> Unit,
    onToggleTracking: () -> Unit
) {
    var selectedRate by remember { mutableStateOf(SlewRate.CENTER_8X) }
    var wifiIp by remember { mutableStateOf("192.168.4.1") }
    var wifiPort by remember { mutableStateOf("4030") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PANOU CONEXIUNE & PROTOCOL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (telemetry.isConnected) AccentGreen else AccentCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (telemetry.isConnected) AccentGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (telemetry.isConnected) "MONTURĂ CONECTATĂ: ${telemetry.connectionMode.displayName}"
                                else "MONTURĂ DECONECTATĂ",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (telemetry.isConnected) AccentGreen else Color.White
                            )
                        }
                        if (telemetry.isConnected) {
                            TextButton(onClick = onDisconnect) {
                                Text("DECONECTEAZĂ", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Compatibil nativ cu Celestron NexStar 6SE / 8SE / Evolution (telecomanda NexStar+ prin USB-OTG sau WiFi SkyPortal 4030), " +
                                "precum și monturi Meade LX200, Sky-Watcher SynScan și OnStep. Oferă rezoluție sub-arcsecundă (comenzi hexazecimale r/e/s) pentru deep-sky.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConnectUsb,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CABLU USB-OTG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val p = wifiPort.toIntOrNull() ?: 4030
                                onConnectWifi(wifiIp, p)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WIFI (4030)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onConnectSim,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SIMULATOR", fontSize = 11.sp, color = AccentGreen)
                        }
                    }
                }
            }
        }

        // TELEMETRIE COORDONATE CURENTE & OPRIRE DE URGENȚĂ
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (telemetry.isSlewing) AccentAmber else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("POZIȚIE CURENTĂ TELESCOP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("RA: ${telemetry.currentRaFormatted}", style = MaterialTheme.typography.titleMedium, color = AccentCyan)
                            Text("Dec: ${telemetry.currentDecFormatted}", style = MaterialTheme.typography.titleMedium, color = AccentCyan)
                        }

                        Surface(
                            color = (if (telemetry.isSlewing) AccentAmber else if (telemetry.isTracking) AccentGreen else Color.Gray).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (telemetry.isSlewing) "⚡ ROTAȚIE MOTOARE (GoTo)"
                                else if (telemetry.isTracking) "★ URMARE SIDERALE ACTIVĂ"
                                else "MOTOARE OPRITE",
                                color = if (telemetry.isSlewing) AccentAmber else if (telemetry.isTracking) AccentGreen else Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BUTON MARE OPRIRE DE URGENȚĂ
                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚠️ OPRIRE DE URGENȚĂ MOTOARE (:Q#)", fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPark,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PARCHEAZĂ (HOME)")
                        }

                        OutlinedButton(
                            onClick = onToggleTracking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (telemetry.isTracking) "OPREȘTE TRACKING" else "PORNEȘTE TRACKING")
                        }
                    }
                }
            }
        }

        // TELECOMANDĂ VIRTUALĂ D-PAD (MANUAL JOGGING PAD)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "TELECOMANDĂ MANUALĂ MOTOARE (VIRTUAL D-PAD)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Mișcă manual axele RA (Est/Vest) și Dec (Nord/Sud) pentru centrarea stelei în câmpul vizual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SELECTOR VITEZA MOTOARE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SlewRate.values().forEach { rate ->
                            FilterChip(
                                selected = selectedRate == rate,
                                onClick = { selectedRate = rate },
                                label = { Text(rate.label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // D-PAD CONTROLLER
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { onJog(SlewDirection.NORTH, selectedRate) },
                            modifier = Modifier.size(54.dp).background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "North", tint = AccentCyan, modifier = Modifier.size(32.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            IconButton(
                                onClick = { onJog(SlewDirection.WEST, selectedRate) },
                                modifier = Modifier.size(54.dp).background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "West", tint = AccentCyan, modifier = Modifier.size(32.dp))
                            }

                            Surface(
                                shape = CircleShape,
                                color = DarkBorder,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("GoTo", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            IconButton(
                                onClick = { onJog(SlewDirection.EAST, selectedRate) },
                                modifier = Modifier.size(54.dp).background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "East", tint = AccentCyan, modifier = Modifier.size(32.dp))
                            }
                        }

                        IconButton(
                            onClick = { onJog(SlewDirection.SOUTH, selectedRate) },
                            modifier = Modifier.size(54.dp).background(AccentCyan.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "South", tint = AccentCyan, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        // ȚINTE EXOPLANETARE PENTRU ORIENTARE AUTOMATĂ (GoTo SLEW)
        item {
            Text("ORIENTARE AUTOMATĂ LA ȚINTE ACTIVE (GoTo):", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
        }

        items(transits) { t ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.candidateName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text("RA: ${t.rightAscension} • Dec: ${t.declination} • V = ${t.apparentVisualMag} mag", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    Button(
                        onClick = { onSlewTarget(t) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ROTEȘTE AICI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TERMINAL SERIAL COMMAND LOG
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030508)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TERMINAL PROTOCOL SERIAL (Meade/LX200/NexStar):", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        Text("Baud: 9600 8N1", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    telemetry.statusLog.take(8).forEach { log ->
                        Text(
                            log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (log.contains("⚠️") || log.contains("Eroare")) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AstrobiologyView(
    biosignatures: List<BiosignatureAssessment>,
    technosignatures: List<TechnosignatureCandidate>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Biotech, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ATMOSPHERIC CHEMICAL DISEQUILIBRIUM",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Life maintains planetary atmospheres out of chemical equilibrium (Krissansen-Totton 2018). " +
                                "Simultaneous detection of methane (CH4) and oxidants (O2/O3) or marine volatile biomarkers like Dimethyl Sulfide (DMS) " +
                                "provides the strongest empirical evidence of active extraterrestrial biospheres.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(biosignatures) { bio ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bio.planetName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Host: ${bio.hostStar} • Teq = ${bio.equilibriumTempK.roundToInt()} K", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "BIO-SCORE ${bio.biosignatureScore.roundToInt()}%",
                                color = AccentGreen,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Thermodynamic Disequilibrium (ΔG): ${String.format("%.1f", bio.thermodynamicDisequilibriumMetric)} kJ/mol",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Detected Molecular Bands:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bio.detectedMolecules.forEach { mol ->
                            Surface(
                                color = (if (mol.isBiosignatureCandidate) AccentGreen else AccentCyan).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "${mol.formula} (${mol.detectionSigma}σ)",
                                    color = if (mol.isBiosignatureCandidate) AccentGreen else AccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(bio.biologicalExplanation, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, tint = AccentMagenta)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "TECHNO-SIGNATURES & DYSON MEGASTRUCTURES",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentMagenta
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Spherical planets generate strictly symmetric transit profiles. " +
                                "Artificial megastructures (Dyson swarms, solar sails) produce asymmetric transit slopes, " +
                                "while advanced planetary civilizations emit synthetic industrial pollution bands like Chlorofluorocarbons (CFC-11/12).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(technosignatures) { tech ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tech.candidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Host Star: ${tech.hostStar}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = AccentMagenta.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "TECHNO-PROB ${tech.artificialMegastructureProbability.roundToInt()}%",
                                color = AccentMagenta,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AstroMetricItem("Transit Asymmetry", "${String.format("%.2f", tech.transitAsymmetryIndex * 100)}%")
                        AstroMetricItem("Ingress vs Egress", "${tech.ingressDurationHours}h / ${tech.egressDurationHours}h")
                        AstroMetricItem("Depth Variance", "${tech.depthVariancePpm.roundToInt()} ppm")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tech.physicalDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Natural Hypothesis: ${tech.naturalPhenomenaHypothesis}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                }
            }
        }
    }
}

@Composable
private fun FalsePositiveRescueView(
    rescues: List<DilutionRescueCandidate>,
    onOpenDeblender: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterCenterFocus, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GAIA DR3 DILUTION DE-BLENDER (UNLIMITED)",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "TESS pixels are large (21\"). Automated algorithms mislabel shallow transits contaminated by background stars as " +
                                "False Positives (grazing eclipsing binaries). By calculating the exact Gaia flux dilution factor, " +
                                "we invert the true transit depth and rescue genuine rocky Super-Earths!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenDeblender,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DE-BLEND CUSTOM GAIA CANDIDATE")
                    }
                }
            }
        }

        items(rescues) { res ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (res.isRescuedToRockyPlanet) AccentGreen else DarkBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(res.candidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("TIC ${res.ticId} • Original: ${res.originalNasaStatus}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        if (res.isRescuedToRockyPlanet) {
                            Surface(
                                color = AccentGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "★ RESCUED ROCKY PLANET",
                                    color = AccentGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AstroMetricItem("Observed Depth", "${res.observedTransitDepthPpm.roundToInt()} ppm")
                        AstroMetricItem("Dilution Factor", "${String.format("%.2f", res.dilutionCorrectionFactor)}x", highlightColor = AccentAmber)
                        AstroMetricItem("True De-blended Depth", "${res.trueTransitDepthPpm.roundToInt()} ppm", highlightColor = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AstroMetricItem("Apparent Radius", "${String.format("%.2f", res.apparentRadiusEarth)} R⊕")
                        AstroMetricItem("True Radius", "${String.format("%.2f", res.trueCorrectedRadiusEarth)} R⊕", highlightColor = AccentCyan)
                        AstroMetricItem("Gaia Separation", "${String.format("%.1f", res.angularSeparationArcsec)}\"")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(res.rescueVerdict, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun HarmonicStitcherView(stitches: List<HarmonicStitchCandidate>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CROSS-YEAR MULTI-SECTOR STITCHING",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "When TESS records a single transit in Sector 14 and another in Sector 44 (separated by years), " +
                                "they are treated as unrelated anomalies. This engine scans all discrete harmonic periods P = ΔT / k, " +
                                "cross-references them with survey camera uptime, and isolates the true orbital ephemeris.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(stitches) { stitch ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stitch.candidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("TIC ${stitch.ticId} • Sector ${stitch.sector1} to Sector ${stitch.sector2} (ΔT = ${String.format("%.1f", stitch.deltaDays)} days)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AstroMetricItem("Best Solved Period", "${String.format("%.2f", stitch.bestUniquePeriodDays)} days", highlightColor = AccentCyan)
                        AstroMetricItem("Semi-Major Axis", "${String.format("%.3f", stitch.bestSemiMajorAxisAu)} AU")
                        AstroMetricItem("Confidence", "${stitch.solutionConfidence.roundToInt()}%", highlightColor = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Harmonic Period Aliases (P = ΔT / k):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    stitch.candidateHarmonics.take(6).forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("k=${h.harmonicIndexK}: P = ${String.format("%.1f", h.periodDays)} d (${String.format("%.2f", h.semiMajorAxisAu)} AU)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            if (h.isRuledOutBySurveyCoverage) {
                                Text("RULED OUT", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text("★ VIABLE", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClimateSimulatorView(
    result: ClimateSimulationResult?,
    onSimulate: (name: String, insolation: Double, co2: Double, press: Double) -> Unit
) {
    var insolation by remember { mutableStateOf(1.0f) }
    var co2Ppm by remember { mutableStateOf(400f) }
    var pressureBar by remember { mutableStateOf(1.0f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1D BUDYKO-SELLERS ENERGY BALANCE SIMULATOR",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Simulates ice-albedo feedback, greenhouse warming, and runaway climate bifurcations in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Stellar Insolation: ${String.format("%.2f", insolation)} S☉", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = insolation,
                        onValueChange = {
                            insolation = it
                            onSimulate("Custom Exoplanet", insolation.toDouble(), (co2Ppm * 1e-6).toDouble(), pressureBar.toDouble())
                        },
                        valueRange = 0.4f..1.8f
                    )

                    Text("Atmospheric CO2: ${co2Ppm.roundToInt()} ppm", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = co2Ppm,
                        onValueChange = {
                            co2Ppm = it
                            onSimulate("Custom Exoplanet", insolation.toDouble(), (co2Ppm * 1e-6).toDouble(), pressureBar.toDouble())
                        },
                        valueRange = 10f..50000f
                    )

                    Text("Surface Pressure: ${String.format("%.1f", pressureBar)} bar", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = pressureBar,
                        onValueChange = {
                            pressureBar = it
                            onSimulate("Custom Exoplanet", insolation.toDouble(), (co2Ppm * 1e-6).toDouble(), pressureBar.toDouble())
                        },
                        valueRange = 0.1f..10.0f
                    )
                }
            }
        }

        if (result != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when (result.climateRegime) {
                            ClimateRegime.TROPICAL_SUPER_HABITABLE, ClimateRegime.TEMPERATE_EARTH_LIKE -> AccentGreen
                            ClimateRegime.RUNAWAY_GREENHOUSE -> Color(0xFFEF4444)
                            ClimateRegime.SNOWBALL_GLACIATED -> AccentCyan
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(result.climateRegime.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Surface(
                                color = AccentCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "${String.format("%.1f", result.meanSurfaceTempC)}°C",
                                    color = AccentCyan,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AstroMetricItem("Ice Line Latitude", "${result.polarIceLatitudeDeg.roundToInt()}°")
                            AstroMetricItem("Liquid Ocean Cover", "${result.liquidOceanCoveragePercent.roundToInt()}%", highlightColor = AccentGreen)
                            AstroMetricItem("Bond Albedo", "${String.format("%.2f", result.bondAlbedo)}")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(result.climateSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyRadarView(
    transits: List<LiveTransitTarget>,
    onSendGpsAlert: (LiveTransitTarget) -> Unit,
    onExport: (LiveTransitTarget, String) -> Unit,
    onSlewMount: (LiveTransitTarget) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "LIVE TRANSIT SKY RADAR & ALERTE GPS",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Propagare orbitală în timp real cu predicția ferestrelor de tranzit deasupra coordonatelor tale GPS. " +
                                "Apasă pe butonul de alertă pentru notificare pe telefon sau pe butonul de orientare fizică pentru a roti telescopul la țintă.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(transits) { t ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(t.candidateName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Constelație: ${t.constellation} • V = ${t.apparentVisualMag} mag", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(
                            color = AccentAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "IN ~${t.hoursUntilIngress.roundToInt()}h",
                                color = AccentAmber,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AstroMetricItem("Ascensie Dreaptă", t.rightAscension)
                        AstroMetricItem("Declinație", t.declination)
                        AstroMetricItem("Adâncime Tranzit", "${t.transitDepthPpm.roundToInt()} ppm")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fezabilitate Telescop: ${t.amateurTelescopeFeasibility}", style = MaterialTheme.typography.bodySmall, color = AccentGreen)

                    Spacer(modifier = Modifier.height(14.dp))

                    // BUTON ORIENTARE FIZICĂ MOTOARE (GoTo)
                    Button(
                        onClick = { onSlewMount(t) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ ROTEȘTE TELESCOPUL FIZIC AICI (GoTo Slew)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // BUTON ALERTA GPS
                    OutlinedButton(
                        onClick = { onSendGpsAlert(t) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TRIMITE ALERTĂ GPS PE TELEFON", color = AccentAmber, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // BUTOANE EXPORT TELESCOP
                    Text("Export fișiere de telescop:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onExport(t, "NINA") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("N.I.N.A.", fontSize = 11.sp, color = AccentCyan)
                        }
                        OutlinedButton(
                            onClick = { onExport(t, "STELLARIUM") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Stellarium", fontSize = 11.sp, color = AccentCyan)
                        }
                        OutlinedButton(
                            onClick = { onExport(t, "ASTROIMAGEJ") },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("AstroImageJ", fontSize = 11.sp, color = AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroMetricItem(label: String, value: String, highlightColor: Color = Color.White) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = highlightColor)
    }
}

@Composable
private fun CustomDeblendDialog(
    onDismiss: () -> Unit,
    onDeblend: (name: String, tic: String, depth: Double, tMag: Double, nMag: Double, sep: Double, rad: Double) -> Unit
) {
    var name by remember { mutableStateOf("Gaia Deblended Candidate") }
    var depth by remember { mutableStateOf("450") }
    var targetMag by remember { mutableStateOf("10.8") }
    var neighborMag by remember { mutableStateOf("10.2") }
    var sepArcsec by remember { mutableStateOf("5.4") }
    var radSun by remember { mutableStateOf("0.95") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gaia Dilution De-Blender (Nelimitat)", color = AccentGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Candidate Name") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = depth,
                        onValueChange = { depth = it },
                        label = { Text("Obs Depth (ppm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sepArcsec,
                        onValueChange = { sepArcsec = it },
                        label = { Text("Separation (\")") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetMag,
                        onValueChange = { targetMag = it },
                        label = { Text("Target Gaia Mag") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = neighborMag,
                        onValueChange = { neighborMag = it },
                        label = { Text("Neighbor Gaia Mag") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = depth.toDoubleOrNull() ?: 400.0
                    val tm = targetMag.toDoubleOrNull() ?: 10.5
                    val nm = neighborMag.toDoubleOrNull() ?: 10.0
                    val s = sepArcsec.toDoubleOrNull() ?: 5.0
                    val r = radSun.toDoubleOrNull() ?: 1.0
                    onDeblend(name, "999888111", d, tm, nm, s, r)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black)
            ) {
                Text("RESCUE PLANET")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        }
    )
}

@Composable
fun AllModulesCatalogDialog(
    currentTab: AdvancedTab,
    onSelectTab: (AdvancedTab) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CATALOG MODULE LABORATOARE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Surface(
                    color = AccentCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "18 MODULE",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Selectează direct oricare dintre cele 18 instrumente științifice fără a mai naviga orizontal prin tab-uri:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Group 1: Observații & Hardware
                item {
                    Text(
                        "🔭 OBSERVAȚII & HARDWARE TELESCOP",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                val obsTabs = listOf(
                    AdvancedTab.SMART_SCOPE,
                    AdvancedTab.FITS_SUMMARY,
                    AdvancedTab.TELESCOPE_VISION,
                    AdvancedTab.SKY_RADAR,
                    AdvancedTab.TELESCOPE_CONTROL,
                    AdvancedTab.LIVE_PHOTOMETRY,
                    AdvancedTab.AR_SKY_CAMERA
                )
                items(obsTabs) { tab ->
                    ModuleItemRow(
                        tab = tab,
                        isSelected = tab == currentTab,
                        onClick = { onSelectTab(tab) }
                    )
                }

                // Group 2: Spectroscopie & Laborator
                item {
                    Text(
                        "🔬 SPECTROSCOPIE & ASTROFIZICĂ PLANETARĂ",
                        color = AccentCyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                val specTabs = listOf(
                    AdvancedTab.JWST_SPECTROSCOPY,
                    AdvancedTab.PLATO_ASTEROSEISMOLOGY,
                    AdvancedTab.ROMAN_MICROLENSING,
                    AdvancedTab.EXOMOON_OCEANS,
                    AdvancedTab.SPACE_WEATHER,
                    AdvancedTab.BIOSIGNATURES,
                    AdvancedTab.FALSE_POSITIVE_RESCUE,
                    AdvancedTab.HARMONIC_STITCHER,
                    AdvancedTab.CLIMATE_SIMULATOR
                )
                items(specTabs) { tab ->
                    ModuleItemRow(
                        tab = tab,
                        isSelected = tab == currentTab,
                        onClick = { onSelectTab(tab) }
                    )
                }

                // Group 3: Cosmologie & Relativitate
                item {
                    Text(
                        "🌌 COSMOLOGIE & FIZICĂ RELATIVISTĂ",
                        color = AccentMagenta,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                val cosmoTabs = listOf(
                    AdvancedTab.BIG_BANG_COSMOLOGY,
                    AdvancedTab.BLACK_HOLES
                )
                items(cosmoTabs) { tab ->
                    ModuleItemRow(
                        tab = tab,
                        isSelected = tab == currentTab,
                        onClick = { onSelectTab(tab) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ÎNCHIDE", color = AccentCyan)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
private fun ModuleItemRow(
    tab: AdvancedTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) AccentCyan else Color(0xFF334155)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tab.title,
                    color = if (isSelected) AccentCyan else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
                if (tab.isYear2026) {
                    Text(
                        "Modul Avansat 2026/27",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selectat",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
