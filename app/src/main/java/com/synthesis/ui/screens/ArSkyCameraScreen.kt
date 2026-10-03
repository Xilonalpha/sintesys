package com.synthesis.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.synthesis.R
import com.synthesis.service.ArExoplanetTarget
import com.synthesis.service.ArSkyTrackerService
import com.synthesis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArSkyCameraScreen(
    arService: ArSkyTrackerService,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    val targets = remember(deviceOrientation) {
        arService.computeProjectedTargets(deviceOrientation)
    }

    var selectedTarget by remember { mutableStateOf<ArExoplanetTarget?>(targets.firstOrNull()) }
    var manualMode by remember { mutableStateOf(!deviceOrientation.sensorActive || !hasCameraPermission) }

    if (showPermissionDialog && !hasCameraPermission) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                manualMode = true
            },
            icon = {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(32.dp))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AR SKY CAMERA", color = AccentCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Vedere Augmentată Exoplanete Cerul Nopții", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { manualMode = !manualMode }) {
                        Icon(
                            if (manualMode) Icons.Default.TouchApp else Icons.Default.ScreenRotation,
                            contentDescription = "Toggle Manual Mode",
                            tint = if (manualMode) AccentAmber else AccentGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.85f))
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
            // Immersive Starry Background if Camera is offline/simulated
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Dark radial gradient sky
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF020617), Color.Black),
                        center = Offset(w / 2f, h / 2f),
                        radius = w.coerceAtLeast(h)
                    )
                )

                // Background stars
                val rand = java.util.Random(42)
                for (i in 0..120) {
                    val sx = rand.nextFloat() * w
                    val sy = rand.nextFloat() * h
                    val r = rand.nextFloat() * 1.5f + 0.5f
                    drawCircle(color = Color.White.copy(alpha = rand.nextFloat() * 0.8f + 0.2f), radius = r, center = Offset(sx, sy))
                }

                // Horizon line
                val horizonY = h * 0.5f + (deviceOrientation.altitudePitchDeg.toFloat() / 45f) * (h * 0.5f)
                drawLine(
                    color = AccentCyan.copy(alpha = 0.35f),
                    start = Offset(0f, horizonY),
                    end = Offset(w, horizonY),
                    strokeWidth = 1.5f
                )
            }

            // HUD Crosshairs & Telemetry Bar
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Central targeting reticle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .border(1.dp, AccentCyan.copy(alpha = 0.4f), CircleShape)
                )

                // Render In-FOV Exoplanet Targets
                targets.forEach { target ->
                    if (target.isInsideCameraFov) {
                        val posX = boxWidth * target.screenXFraction
                        val posY = boxHeight * target.screenYFraction
                        val isSelected = target.id == selectedTarget?.id

                        Box(
                            modifier = Modifier
                                .offset(x = posX - 36.dp, y = posY - 36.dp)
                                .size(72.dp)
                                .clickable { selectedTarget = target },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 56.dp else 40.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (target.isHabitableZone) AccentGreen else AccentCyan,
                                        shape = CircleShape
                                    )
                                    .background(
                                        (if (target.isHabitableZone) AccentGreen else AccentCyan).copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            )

                            // Target label tag
                            Surface(
                                color = Color.Black.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 20.dp)
                            ) {
                                Text(
                                    target.name,
                                    color = if (target.isHabitableZone) AccentGreen else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Top Telemetry Header
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AZIMUT", color = TextSecondary, fontSize = 9.sp)
                            Text("${deviceOrientation.compassAzimuthDeg.toInt()}° (${compassHeading(deviceOrientation.compassAzimuthDeg)})", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ALTITUDINE", color = TextSecondary, fontSize = 9.sp)
                            Text("${deviceOrientation.altitudePitchDeg.toInt()}°", color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MOD", color = TextSecondary, fontSize = 9.sp)
                            Text(if (manualMode) "Trage Ecran" else "Giroscop Activ", color = if (manualMode) AccentAmber else AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Directional Guidance Indicator if target is outside FOV
                selectedTarget?.let { target ->
                    if (!target.isInsideCameraFov) {
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 70.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (target.deltaAzDeg < 0) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Ghidează spre ${target.name}: ${if (target.deltaAzDeg < 0) "Rotire Stânga" else "Rotire Dreapta"} ${Math.abs(target.deltaAzDeg).toInt()}°, Înclinare ${target.altitudeDeg.toInt()}°",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Bottom Selected Target Detail Card
                selectedTarget?.let { target ->
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(target.name, color = AccentCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Constelația ${target.constellation} • Stea: ${target.hostStar}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(
                                    color = if (target.isHabitableZone) AccentGreen.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        if (target.isHabitableZone) "ZONĂ HABITABILĂ" else "CONFIRMAT",
                                        color = if (target.isHabitableZone) AccentGreen else AccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TargetStat("DISTANȚĂ", "${String.format("%.1f", target.distanceLightYears)} ani-lumină", AccentAmber)
                                TargetStat("ALTITUDINE", "${target.altitudeDeg.toInt()}° (${if (target.isVisibleAboveHorizon) "Vizibil pe Cer" else "Sub Orizont"})", if (target.isVisibleAboveHorizon) AccentGreen else TextSecondary)
                                TargetStat("AZIMUT COORDONAT", "${target.azimuthDeg.toInt()}°", AccentCyan)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tip Lume: ${target.planetType} | Metodă: ${target.discoveryMethod}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetStat(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextSecondary, fontSize = 9.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
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
