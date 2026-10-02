package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.LiveTransitTarget
import com.synthesis.domain.model.exoplanet.SmartScopeModel
import com.synthesis.domain.model.exoplanet.SmartTelescopeTelemetry
import com.synthesis.ui.theme.*

@Composable
fun SmartScopeView(
    telemetry: SmartTelescopeTelemetry,
    transits: List<LiveTransitTarget>,
    onConnect: (SmartScopeModel, String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onSlewTarget: (LiveTransitTarget) -> Unit,
    onStartSequence: (Double, Int, String) -> Unit,
    onStopSequence: () -> Unit
) {
    var selectedModel by remember { mutableStateOf(telemetry.model) }
    var ipInput by remember { mutableStateOf(telemetry.ipAddress) }
    var portInput by remember { mutableStateOf(telemetry.port.toString()) }
    var expSecInput by remember { mutableStateOf("10.0") }
    var gainInput by remember { mutableStateOf("80") }
    var filterInput by remember { mutableStateOf("UV/IR Cut") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (telemetry.isConnected) AccentGreen else AccentCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (telemetry.isConnected) AccentGreen else AccentCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SMART TELESCOPE DIRECT-SYNC (2026)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (telemetry.isConnected) AccentGreen.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                if (telemetry.isConnected) "CONECTAT" else "DECONECTAT",
                                color = if (telemetry.isConnected) AccentGreen else TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Integrare directă prin WiFi/TCP cu telescoape inteligente autonome (ZWO Seestar S50, DwarfLab DWARF II/3, Vaonis Vespera, Unistellar, ASCOM Alpaca).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Selector Chips
                    Text("Model Telescop:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmartScopeModel.values().take(3).forEach { model ->
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = {
                                    selectedModel = model
                                    portInput = model.defaultPort.toString()
                                },
                                label = { Text(model.label.split(" ").first(), fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmartScopeModel.values().drop(3).forEach { model ->
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = {
                                    selectedModel = model
                                    portInput = model.defaultPort.toString()
                                },
                                label = { Text(model.label.split(" ").first(), fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("Adresă IP WiFi") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!telemetry.isConnected) {
                        Button(
                            onClick = {
                                val port = portInput.toIntOrNull() ?: selectedModel.defaultPort
                                onConnect(selectedModel, ipInput.trim(), port)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONECTEAZĂ ${selectedModel.label.split(" ").first().uppercase()}", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DECONECTEAZĂ SMART TELESCOPE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Telemetry Bar
        if (telemetry.isConnected) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TELEMETRIE HARDWARE ÎN DIRECT", style = MaterialTheme.typography.labelMedium, color = AccentCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Baterie: ${telemetry.batteryPercent}%", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Temp. Senzor: ${telemetry.sensorTempC}°C", color = AccentAmber, style = MaterialTheme.typography.bodyMedium)
                            Text("Tracking: ${if (telemetry.isTracking) "ACTIV" else "OPRIT"}", color = if (telemetry.isTracking) AccentGreen else TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (telemetry.targetName != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Centrat pe: ${telemetry.targetName} (RA: ${telemetry.targetRa}, Dec: ${telemetry.targetDec})", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                        }
                        if (telemetry.isStacking) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Acumulare cadre FITS: Cadre ${telemetry.stackedFramesCount} (${telemetry.exposureTimeSec}s)", color = AccentCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Photometry acquisition controls
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ACHIZIȚIE FOTOMETRICĂ TRANZIT", style = MaterialTheme.typography.labelMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = expSecInput,
                                onValueChange = { expSecInput = it },
                                label = { Text("Expunere (s)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = gainInput,
                                onValueChange = { gainInput = it },
                                label = { Text("Gain") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!telemetry.isStacking) {
                            Button(
                                onClick = {
                                    val exp = expSecInput.toDoubleOrNull() ?: 10.0
                                    val gain = gainInput.toIntOrNull() ?: 80
                                    onStartSequence(exp, gain, filterInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PORNEȘTE SECVENȚA FOTOMETRICĂ (STACKING FITS)", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onStopSequence,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OPREȘTE ACHIZIȚIA CADRELOR", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Quick-Slew Targets
        item {
            Text("TRANSMITE ȚINTE TRANZIT DIRECT PE SMART SCOPE:", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }

        items(transits) { target ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(target.candidateName, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Constelație: ${target.constellation} | RA: ${target.rightAscension} | Dec: ${target.declination}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Adâncime: ${target.transitDepthPpm} ppm | Mag: ${target.apparentVisualMag}", color = AccentAmber, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSlewTarget(target) },
                        enabled = telemetry.isConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ROTEȘTE SMART TELESCOPE AICI (Plate-Solving GoTo)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Network Console Logs
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CONSOLĂ DIAGNOSTICĂ REȚEA SMART SCOPE", color = AccentCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (telemetry.liveLog.isEmpty()) {
                        Text("Nicio activitate de rețea. Conectează telescopul pentru a vedea pachetele de telemetrie.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        telemetry.liveLog.forEach { line ->
                            Text(line, color = Color.LightGray, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                        }
                    }
                }
            }
        }
    }
}
