package com.synthesis.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.synthesis.R
import com.synthesis.service.*
import com.synthesis.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArSkyCameraScreen(
    arService: ArSkyTrackerService,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSlewTelescope: ((Double, Double, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionDialog by remember {
        mutableStateOf(!hasCameraPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        showPermissionDialog = false
    }

    LaunchedEffect(Unit) {
        arService.startTracking()
    }

    DisposableEffect(Unit) {
        onDispose {
            arService.stopTracking()
        }
    }

    val deviceOrientation by arService.deviceOrientation.collectAsState()

    // Filter & HUD Display States
    var selectedCategory by remember { mutableStateOf(ArTargetCategory.ALL) }
    var showConstellations by remember { mutableStateOf(true) }
    var showHorizonGrid by remember { mutableStateOf(true) }
    var useLiveCameraFeed by remember { mutableStateOf(hasCameraPermission) }
    var manualMode by remember { mutableStateOf(!deviceOrientation.sensorActive || !hasCameraPermission) }
    var showMiniRadar by remember { mutableStateOf(true) }
    var gotoFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val targets = remember(deviceOrientation, selectedCategory) {
        arService.computeProjectedTargets(
            device = deviceOrientation,
            filterCategory = selectedCategory
        )
    }

    val constellationLines = remember(deviceOrientation, showConstellations) {
        if (showConstellations) {
            ArSkyTrackerService.computeProjectedConstellations(deviceOrientation)
        } else {
            emptyList()
        }
    }

    val cardinalPoints = remember(deviceOrientation, showHorizonGrid) {
        if (showHorizonGrid) {
            ArSkyTrackerService.computeProjectedCardinalPoints(deviceOrientation)
        } else {
            emptyList()
        }
    }

    var selectedTarget by remember { mutableStateOf<ArExoplanetTarget?>(targets.firstOrNull()) }

    // Synchronize selected target with category changes
    LaunchedEffect(selectedCategory) {
        if (selectedTarget == null || !targets.any { it.id == selectedTarget?.id }) {
            selectedTarget = targets.firstOrNull()
        }
    }

    // Determine if any target is currently locked inside crosshair (< 3.5 deg)
    val lockedTarget = remember(targets) {
        targets.firstOrNull { it.isInsideCameraFov && it.angularSeparationFromCrosshairDeg <= 3.5 }
    }

    // Haptic pulse when acquiring a lock
    var lastLockedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lockedTarget?.id) {
        if (lockedTarget != null && lockedTarget.id != lastLockedId) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            lastLockedId = lockedTarget.id
            selectedTarget = lockedTarget
        } else if (lockedTarget == null) {
            lastLockedId = null
        }
    }

    // Permission Dialog
    if (showPermissionDialog && !hasCameraPermission) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                useLiveCameraFeed = false
                manualMode = true
            },
            icon = {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    stringResource(R.string.perm_camera_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    stringResource(R.string.perm_camera_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text(
                        stringResource(R.string.perm_btn_grant_camera),
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        useLiveCameraFeed = false
                        manualMode = true
                    },
                    modifier = Modifier.testTag("use_stellar_sim_button")
                ) {
                    Text(
                        stringResource(R.string.perm_btn_stellar_sim),
                        color = AccentAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.testTag("ar_camera_permission_dialog")
        )
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AR SKY CAMERA PRO",
                                color = AccentCyan,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (deviceOrientation.isTrueNorthCalibrated) AccentGreen.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "TRUE NORTH ASTRONOMIC",
                                    color = if (deviceOrientation.isTrueNorthCalibrated) AccentGreen else AccentAmber,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Vedere Augmentată • Exoplanete & Biosemnături pe Cerul Nopții",
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
                    // Toggle Live Camera vs Planetarium
                    IconButton(
                        onClick = {
                            if (hasCameraPermission) {
                                useLiveCameraFeed = !useLiveCameraFeed
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        }
                    ) {
                        Icon(
                            if (useLiveCameraFeed && hasCameraPermission) Icons.Default.CameraAlt else Icons.Default.AutoAwesome,
                            contentDescription = "Toggle Camera/Planetarium",
                            tint = if (useLiveCameraFeed && hasCameraPermission) AccentCyan else AccentAmber
                        )
                    }

                    // Toggle Constellations
                    IconButton(onClick = { showConstellations = !showConstellations }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Toggle Constellations",
                            tint = if (showConstellations) AccentCyan else TextSecondary
                        )
                    }

                    // Toggle Manual Panning vs Gyroscope
                    IconButton(onClick = { manualMode = !manualMode }) {
                        Icon(
                            if (manualMode) Icons.Default.TouchApp else Icons.Default.ScreenRotation,
                            contentDescription = "Toggle Mode",
                            tint = if (manualMode) AccentAmber else AccentGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.88f))
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .pointerInput(manualMode) {
                    if (manualMode) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newAz = deviceOrientation.compassAzimuthDeg - (dragAmount.x * 0.2)
                            val newAlt = deviceOrientation.altitudePitchDeg + (dragAmount.y * 0.2)
                            arService.setManualOrientation(newAz, newAlt)
                        }
                    }
                }
        ) {
            // LAYER 1: BACKGROUND (Camera2 Live Hardware Preview OR Cosmic Planetarium Canvas)
            if (useLiveCameraFeed && hasCameraPermission) {
                CameraHardwarePreview(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CosmicPlanetariumCanvas(
                    orientation = deviceOrientation,
                    showHorizonGrid = showHorizonGrid,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // LAYER 2: CELESTIAL OVERLAYS (Constellation Lines & Cardinal Points)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw Constellation Lines
                if (showConstellations) {
                    constellationLines.forEach { line ->
                        val start = Offset(line.startX * w, line.startY * h)
                        val end = Offset(line.endX * w, line.endY * h)
                        drawLine(
                            color = AccentCyan.copy(alpha = if (line.isVisible) 0.55f else 0.25f),
                            start = start,
                            end = end,
                            strokeWidth = if (line.isVisible) 2f else 1f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 3.5f,
                            center = start
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 3.5f,
                            center = end
                        )
                    }
                }

                // Draw Horizon Line & Ground Boundary
                if (showHorizonGrid) {
                    val horizonY = h * 0.5f + (deviceOrientation.altitudePitchDeg.toFloat() / 45f) * (h * 0.5f)
                    drawLine(
                        color = AccentCyan.copy(alpha = 0.45f),
                        start = Offset(0f, horizonY),
                        end = Offset(w, horizonY),
                        strokeWidth = 2f
                    )
                }
            }

            // LAYER 3: HUD CROSSHAIRS, RETICLES, RADAR, AND INTERACTIVE TARGETS
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Render In-FOV Exoplanet Targets
                targets.forEach { target ->
                    if (target.isInsideCameraFov) {
                        val posX = boxWidth * target.screenXFraction
                        val posY = boxHeight * target.screenYFraction
                        val isSelected = target.id == selectedTarget?.id
                        val isLocked = target.id == lockedTarget?.id

                        Box(
                            modifier = Modifier
                                .offset(x = posX - 36.dp, y = posY - 36.dp)
                                .size(72.dp)
                                .clickable {
                                    selectedTarget = target
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val targetColor = when {
                                target.category == ArTargetCategory.ASTROBIOLOGY -> AccentAmber
                                target.isHabitableZone -> AccentGreen
                                else -> AccentCyan
                            }

                            // Outer Reticle Ring
                            Box(
                                modifier = Modifier
                                    .size(if (isLocked) 58.dp else if (isSelected) 48.dp else 34.dp)
                                    .border(
                                        width = if (isLocked) 2.5.dp else if (isSelected) 2.dp else 1.dp,
                                        color = if (isLocked) AccentGreen else targetColor,
                                        shape = CircleShape
                                    )
                                    .background(
                                        targetColor.copy(alpha = if (isLocked) 0.35f else if (isSelected) 0.2f else 0.08f),
                                        CircleShape
                                    )
                            )

                            // Center Star Dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )

                            // Target Name & Badge Tag
                            Surface(
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, targetColor.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        target.name,
                                        color = targetColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (target.coldScaleLevel > 0) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            "CoLD-${target.coldScaleLevel}",
                                            color = AccentAmber,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Render Projected Cardinal Points
                cardinalPoints.forEach { cp ->
                    if (cp.isInsideFov) {
                        val posX = boxWidth * cp.screenX
                        val posY = boxHeight * cp.screenY
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .offset(x = posX - 28.dp, y = posY - 12.dp)
                                .size(56.dp, 24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    cp.label,
                                    color = AccentCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Central Targeting Reticle & Lock Indicator
                CentralTargetingCrosshair(
                    lockedTarget = lockedTarget,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Top Header Telemetry & Filter Chips
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Orientation Stats Bar with smooth horizontal scrolling
                    Surface(
                        color = Color.Black.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AZIMUT (TRUE NORTH)", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${deviceOrientation.compassAzimuthDeg.toInt()}° (${compassHeading(deviceOrientation.compassAzimuthDeg)})",
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ALTITUDINE CER", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${deviceOrientation.altitudePitchDeg.toInt()}°",
                                    color = AccentAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DECLINAȚIE MAG", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${String.format("%.1f", deviceOrientation.magneticDeclinationDeg)}°",
                                    color = AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MOD SENZOR", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (manualMode) "Manual (Drag)" else "Giroscop Activ",
                                    color = if (manualMode) AccentAmber else AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArTargetCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.labelRo, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.Black.copy(alpha = 0.6f),
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selectedCategory == cat) AccentCyan else DarkBorder
                                )
                            )
                        }
                    }
                }

                // Mini Sky Radar (Top Right)
                if (showMiniRadar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 96.dp, end = 12.dp)
                    ) {
                        ArMiniSkyRadar(
                            deviceOrientation = deviceOrientation,
                            targets = targets,
                            selectedTarget = selectedTarget,
                            onSelectTarget = { target ->
                                selectedTarget = target
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }

                // 360° Directional Guidance Badge if target is outside FOV (placed comfortably below radar)
                selectedTarget?.let { target ->
                    if (!target.isInsideCameraFov) {
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.94f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, AccentAmber),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 150.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (target.deltaAzDeg < 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Ghidează spre ${target.name}: ${if (target.deltaAzDeg < 0) "Rotește Stânga" else "Rotește Dreapta"} ${Math.abs(target.deltaAzDeg).toInt()}° • Înclinare ${target.altitudeDeg.toInt()}°",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // GoTo Feedback Banner (Toast/Snackbar alternative)
                gotoFeedbackMessage?.let { msg ->
                    Surface(
                        color = Color(0xFF064E3B).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentGreen),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom Target Detail Card (Expanded Dossier)
                selectedTarget?.let { target ->
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.96f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Target Header & CoLD Badge with Dismiss Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        target.name,
                                        color = AccentCyan,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Constelația ${target.constellation} • Misiune: ${target.missionLabel}",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = when {
                                            target.category == ArTargetCategory.ASTROBIOLOGY -> AccentAmber.copy(alpha = 0.2f)
                                            target.isHabitableZone -> AccentGreen.copy(alpha = 0.2f)
                                            else -> AccentCyan.copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (target.coldScaleLevel > 0) "SCARA CoLD: NIVEL ${target.coldScaleLevel}" else if (target.isHabitableZone) "ZONĂ HABITABILĂ" else "CONFIRMAT",
                                            color = if (target.coldScaleLevel > 0) AccentAmber else if (target.isHabitableZone) AccentGreen else AccentCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { selectedTarget = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Închide panoul", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Key Astrometry & Horizon Stats - Organized in a clear 2x2 grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TargetStatCard(
                                    label = "DISTANȚĂ",
                                    value = if (target.distanceLightYears < 0.001) "${String.format("%.1f", target.distanceLightYears * 525600.0)} min-lum." else "${String.format("%.1f", target.distanceLightYears)} ani-lumină",
                                    color = AccentAmber,
                                    modifier = Modifier.weight(1f)
                                )
                                TargetStatCard(
                                    label = "ALTITUDINE CER",
                                    value = "${target.altitudeDeg.toInt()}° (${if (target.isVisibleAboveHorizon) "Vizibil" else "Sub Orizont"})",
                                    color = if (target.isVisibleAboveHorizon) AccentGreen else TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TargetStatCard(
                                    label = "AZIMUT (TRUE NORTH)",
                                    value = "${target.azimuthDeg.toInt()}° (${compassHeading(target.azimuthDeg)})",
                                    color = AccentCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                TargetStatCard(
                                    label = "SEPARARE RETICUL",
                                    value = "${target.angularSeparationFromCrosshairDeg}°",
                                    color = if (target.angularSeparationFromCrosshairDeg <= 3.5) AccentGreen else AccentAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Biomarkers & Abiotic Details if available
                            if (target.keyBiomarkers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Biomarkeri: ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(target.keyBiomarkers.joinToString(" • "), color = Color.White, fontSize = 11.sp)
                                }
                            }

                            if (target.abioticCounterExplanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Alternativă Abiotică: ${target.abioticCounterExplanation}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons: Slew Telescope (GoTo) & Target Switching
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Target Button
                                OutlinedButton(
                                    onClick = {
                                        val idx = targets.indexOfFirst { it.id == target.id }
                                        if (idx > 0) {
                                            selectedTarget = targets[idx - 1]
                                        } else if (targets.isNotEmpty()) {
                                            selectedTarget = targets.last()
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.weight(0.25f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ținta anterioară", modifier = Modifier.size(16.dp))
                                }

                                // GoTo Slew Button
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (onSlewTelescope != null) {
                                            onSlewTelescope(target.altitudeDeg, target.azimuthDeg, target.name)
                                            gotoFeedbackMessage = "Comandă GoTo trimisă: Alt ${target.altitudeDeg.toInt()}°, Az ${target.azimuthDeg.toInt()}°"
                                        } else {
                                            gotoFeedbackMessage = "Coordonate orientate: Alt ${target.altitudeDeg.toInt()}°, Az ${target.azimuthDeg.toInt()}°"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ORIENTEAZĂ GOTO", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Next Target Button
                                OutlinedButton(
                                    onClick = {
                                        val idx = targets.indexOfFirst { it.id == target.id }
                                        if (idx in 0 until targets.lastIndex) {
                                            selectedTarget = targets[idx + 1]
                                        } else if (targets.isNotEmpty()) {
                                            selectedTarget = targets.first()
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.weight(0.25f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ținta următoare", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Camera2 Hardware Preview implementation using Android TextureView.
 * Interacts directly with physical device camera hardware and displays preview at 60 FPS.
 */
@Composable
private fun CameraHardwarePreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    var cameraDevice: CameraDevice? = null
                    var captureSession: CameraCaptureSession? = null
                    var bgThread: HandlerThread? = null
                    var bgHandler: Handler? = null

                    @SuppressLint("MissingPermission")
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        try {
                            bgThread = HandlerThread("ArCamera2Thread").apply { start() }
                            bgHandler = Handler(bgThread!!.looper)

                            val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                                cameraManager.getCameraCharacteristics(id)
                                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                            } ?: cameraManager.cameraIdList.firstOrNull()

                            if (cameraId != null) {
                                cameraManager.openCamera(
                                    cameraId,
                                    object : CameraDevice.StateCallback() {
                                        override fun onOpened(camera: CameraDevice) {
                                            cameraDevice = camera
                                            val surface = Surface(surfaceTexture)
                                            try {
                                                val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                                    addTarget(surface)
                                                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                }
                                                camera.createCaptureSession(
                                                    listOf(surface),
                                                    object : CameraCaptureSession.StateCallback() {
                                                        override fun onConfigured(session: CameraCaptureSession) {
                                                            captureSession = session
                                                            try {
                                                                session.setRepeatingRequest(requestBuilder.build(), null, bgHandler)
                                                            } catch (_: Exception) {}
                                                        }
                                                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                                                    },
                                                    bgHandler
                                                )
                                            } catch (_: Exception) {}
                                        }

                                        override fun onDisconnected(camera: CameraDevice) {
                                            camera.close()
                                            cameraDevice = null
                                        }

                                        override fun onError(camera: CameraDevice, error: Int) {
                                            camera.close()
                                            cameraDevice = null
                                        }
                                    },
                                    bgHandler
                                )
                            }
                        } catch (_: Exception) {}
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            captureSession?.close()
                            cameraDevice?.close()
                            bgThread?.quitSafely()
                        } catch (_: Exception) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}

/**
 * High-fidelity Cosmic Planetarium background canvas when camera is off or simulated.
 */
@Composable
private fun CosmicPlanetariumCanvas(
    orientation: DeviceSkyOrientation,
    showHorizonGrid: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Deep galactic radial gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF030712), Color.Black),
                center = Offset(w / 2f, h / 2f),
                radius = w.coerceAtLeast(h)
            )
        )

        // Nebula atmospheric dust haze
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentCyan.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * 0.35f, h * 0.4f),
                radius = w * 0.45f
            )
        )

        // Seeded background stars
        val rand = java.util.Random(1337)
        for (i in 0..160) {
            val sx = rand.nextFloat() * w
            val sy = rand.nextFloat() * h
            val r = rand.nextFloat() * 1.6f + 0.6f
            val alpha = rand.nextFloat() * 0.75f + 0.25f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = Offset(sx, sy)
            )
        }

        // Shaded ground below horizon
        if (showHorizonGrid) {
            val horizonY = h * 0.5f + (orientation.altitudePitchDeg.toFloat() / 45f) * (h * 0.5f)
            if (horizonY < h) {
                drawRect(
                    color = Color(0xFF020617).copy(alpha = 0.75f),
                    topLeft = Offset(0f, horizonY),
                    size = androidx.compose.ui.geometry.Size(w, h - horizonY)
                )
            }
        }
    }
}

/**
 * Central targeting HUD reticle with animated lock-on states.
 */
@Composable
private fun CentralTargetingCrosshair(
    lockedTarget: ArExoplanetTarget?,
    modifier: Modifier = Modifier
) {
    val isLocked = lockedTarget != null
    val infiniteTransition = rememberInfiniteTransition(label = "LockPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isLocked) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = modifier
            .size(90.dp)
            .border(
                width = if (isLocked) 2.dp else 1.dp,
                color = if (isLocked) AccentGreen else AccentCyan.copy(alpha = 0.45f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val crosshairLen = 10.dp.toPx()
            val reticleColor = if (isLocked) AccentGreen else AccentCyan.copy(alpha = 0.6f)

            // Tick marks
            drawLine(reticleColor, Offset(cx - crosshairLen - 12f, cy), Offset(cx - 12f, cy), strokeWidth = 2f)
            drawLine(reticleColor, Offset(cx + 12f, cy), Offset(cx + crosshairLen + 12f, cy), strokeWidth = 2f)
            drawLine(reticleColor, Offset(cx, cy - crosshairLen - 12f), Offset(cx, cy - 12f), strokeWidth = 2f)
            drawLine(reticleColor, Offset(cx, cy + 12f), Offset(cx, cy + crosshairLen + 12f), strokeWidth = 2f)
        }

        if (isLocked) {
            Surface(
                color = AccentGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, AccentGreen),
                modifier = Modifier
                    .offset(y = 56.dp)
            ) {
                Text(
                    "🔒 LOCK-ON: ${lockedTarget?.name}",
                    color = AccentGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Polar mini-radar HUD showing all targets, observer orientation, and camera FOV wedge.
 */
@Composable
private fun ArMiniSkyRadar(
    deviceOrientation: DeviceSkyOrientation,
    targets: List<ArExoplanetTarget>,
    selectedTarget: ArExoplanetTarget?,
    onSelectTarget: (ArExoplanetTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        shape = CircleShape,
        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
        modifier = modifier.size(100.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.width / 2f
            val cx = r
            val cy = r

            // Outer horizon ring
            drawCircle(
                color = DarkBorder,
                radius = r - 2f,
                center = Offset(cx, cy),
                style = Stroke(1.5f)
            )

            // 45 deg altitude ring
            drawCircle(
                color = DarkBorder.copy(alpha = 0.5f),
                radius = r * 0.5f,
                center = Offset(cx, cy),
                style = Stroke(1f)
            )

            // Center Zenith
            drawCircle(
                color = AccentCyan.copy(alpha = 0.6f),
                radius = 2.5f,
                center = Offset(cx, cy)
            )

            // Device FOV Wedge
            val headingRad = Math.toRadians(deviceOrientation.compassAzimuthDeg - 90.0)
            val halfFovRad = Math.toRadians(30.0)
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo(
                    cx + (r - 4f) * cos(headingRad - halfFovRad).toFloat(),
                    cy + (r - 4f) * sin(headingRad - halfFovRad).toFloat()
                )
                lineTo(
                    cx + (r - 4f) * cos(headingRad + halfFovRad).toFloat(),
                    cy + (r - 4f) * sin(headingRad + halfFovRad).toFloat()
                )
                close()
            }
            drawPath(path, color = AccentCyan.copy(alpha = 0.15f))

            // Plot Targets on Radar
            targets.forEach { target ->
                if (target.altitudeDeg > -10.0) {
                    val altFraction = (90.0 - target.altitudeDeg.coerceIn(0.0, 90.0)) / 90.0
                    val targetDist = (r - 6f) * altFraction.toFloat()
                    val targetAngleRad = Math.toRadians(target.azimuthDeg - 90.0)

                    val px = cx + targetDist * cos(targetAngleRad).toFloat()
                    val py = cy + targetDist * sin(targetAngleRad).toFloat()

                    val isSelected = target.id == selectedTarget?.id
                    val blipColor = when {
                        target.category == ArTargetCategory.ASTROBIOLOGY -> AccentAmber
                        target.isHabitableZone -> AccentGreen
                        else -> AccentCyan
                    }

                    drawCircle(
                        color = if (isSelected) Color.White else blipColor,
                        radius = if (isSelected) 3.5f else 2f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF070B14),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TargetStat(label: String, value: String, color: Color) {
    TargetStatCard(label = label, value = value, color = color)
}

private fun compassHeading(deg: Double): String {
    val d = (deg + 360.0) % 360.0
    return when {
        d >= 337.5 || d < 22.5 -> "N"
        d < 67.5 -> "NE"
        d < 112.5 -> "E"
        d < 157.5 -> "SE"
        d < 202.5 -> "S"
        d < 247.5 -> "SW"
        d < 292.5 -> "W"
        else -> "NW"
    }
}
