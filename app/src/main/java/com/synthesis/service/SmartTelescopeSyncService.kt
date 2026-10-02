package com.synthesis.service

import android.content.Context
import com.synthesis.domain.model.exoplanet.LiveTransitTarget
import com.synthesis.domain.model.exoplanet.SmartScopeModel
import com.synthesis.domain.model.exoplanet.SmartTelescopeTelemetry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredSmartScope(
    val model: SmartScopeModel,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val latencyMs: Long,
    val discoveryMethod: String,
    val isVirtualFallback: Boolean = false
)

/**
 * Autonomous Smart Telescope Direct-Sync & Auto-Discovery Protocol (2026 Edition)
 *
 * Direct WiFi/Ethernet TCP and HTTP REST integration with consumer smart telescopes:
 * - ZWO Seestar S50 / S30 (TCP 4700 / REST API)
 * - DwarfLab DWARF II / 3 (WebSocket / REST 9988)
 * - Vaonis Vespera / Stellina (Port 8080)
 * - Unistellar eVscope 2 / eQuinox / Odyssey (Port 5000)
 * - ASCOM Alpaca Universal Hub (Port 11112 + UDP 32227 Discovery)
 */
@Singleton
class SmartTelescopeSyncService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _telemetry = MutableStateFlow(SmartTelescopeTelemetry())
    val telemetry: StateFlow<SmartTelescopeTelemetry> = _telemetry.asStateFlow()

    // AUTO-DISCOVERY STATEFLOWS
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredScopes = MutableStateFlow<List<DiscoveredSmartScope>>(emptyList())
    val discoveredScopes: StateFlow<List<DiscoveredSmartScope>> = _discoveredScopes.asStateFlow()

    private val _scanStatus = MutableStateFlow("Pregătit pentru scanare automată")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private var scanJob: Job? = null
    private var activeSocket: Socket? = null
    private var socketWriter: PrintWriter? = null
    private var telemetryJob: Job? = null

    /**
     * Starts comprehensive multi-protocol network auto-discovery:
     * 1. ASCOM Alpaca UDP Broadcast on port 32227
     * 2. Direct Hotspot AP gateway check (Seestar 192.168.110.1, DWARF 192.168.88.1, etc.)
     * 3. Parallel local subnet probe for open ports (4700, 9988, 11112)
     */
    fun startNetworkDiscovery() {
        scanJob?.cancel()
        _isScanning.value = true
        _discoveredScopes.value = emptyList()
        _scanStatus.value = "Identificare rețea locală WiFi..."
        addLog("Scanare automată inițiată pentru Seestar, DWARF și ASCOM Alpaca...")

        scanJob = scope.launch {
            try {
                val foundList = mutableListOf<DiscoveredSmartScope>()

                // 1. Detect Local Subnets
                val localIps = getLocalIpAddresses()
                addLog("Adrese IP locale detectate: ${localIps.ifEmpty { listOf("N/A (se utilizează 192.168.1.x)") }.joinToString()}")

                // 2. STAGE A: ASCOM Alpaca UDP 32227 Discovery
                _scanStatus.value = "Transmisie pachete UDP Alpaca Discovery (Port 32227)..."
                runAlpacaUdpDiscovery(foundList)

                // 3. STAGE B: Known Smart Telescope Hotspot Gateways
                _scanStatus.value = "Verificare Hotspot-uri directe (Seestar, DWARF, Vespera)..."
                probeHotspotGateways(foundList)

                // 4. STAGE C: Subnet Rapid Sweep
                if (localIps.isNotEmpty()) {
                    for (ip in localIps) {
                        val prefix = ip.substringBeforeLast(".") + "."
                        _scanStatus.value = "Scanare subrețea $prefix 0/24 pe porturile 4700, 9988, 11112..."
                        sweepSubnet(prefix, foundList)
                    }
                } else {
                    _scanStatus.value = "Scanare subrețea implicită 192.168.1.0/24..."
                    sweepSubnet("192.168.1.", foundList)
                }

                _scanStatus.value = if (foundList.isNotEmpty()) {
                    "Scanare finalizată: ${foundList.size} dispozitiv(e) detectat(e)!"
                } else {
                    "Niciun telescop fizic detectat. Poți adăuga un profil virtual sau introduce IP manual."
                }
                addLog(_scanStatus.value)
            } catch (e: CancellationException) {
                _scanStatus.value = "Scanare anulată."
            } catch (e: Exception) {
                _scanStatus.value = "Eroare la scanare: ${e.message}"
                addLog("Eroare scanare: ${e.message}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun cancelNetworkDiscovery() {
        scanJob?.cancel()
        _isScanning.value = false
        _scanStatus.value = "Scanare oprită de utilizator."
        addLog("Scanarea rețelei a fost întreruptă.")
    }

    /**
     * Adds an on-device virtual telescope instance for offline testing.
     */
    fun addVirtualTestScope(model: SmartScopeModel) {
        val virtual = DiscoveredSmartScope(
            model = model,
            name = "${model.label} (Emulator Virtual)",
            ipAddress = "127.0.0.1",
            port = model.defaultPort,
            latencyMs = 1,
            discoveryMethod = "Simulare Locală (Offline Test)",
            isVirtualFallback = true
        )
        val current = _discoveredScopes.value.filter { it.ipAddress != "127.0.0.1" }
        _discoveredScopes.value = listOf(virtual) + current
        addLog("Telescop virtual ${model.label} adăugat pentru testare offline.")
    }

    private suspend fun runAlpacaUdpDiscovery(foundList: MutableList<DiscoveredSmartScope>) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 1200

            val message = "alpacadiscovery1".toByteArray(Charsets.US_ASCII)
            val packet = DatagramPacket(message, message.size, InetAddress.getByName("255.255.255.255"), 32227)
            socket.send(packet)

            val receiveBuffer = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 1200) {
                try {
                    socket.receive(receivePacket)
                    val responderIp = receivePacket.address.hostAddress ?: continue
                    val responseText = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                    val latency = System.currentTimeMillis() - startTime

                    // extract port from response, e.g. {"AlpacaPort": 11112}
                    val port = Regex("\"AlpacaPort\"\\s*:\\s*(\\d+)").find(responseText)?.groupValues?.get(1)?.toIntOrNull() ?: 11112

                    val device = DiscoveredSmartScope(
                        model = SmartScopeModel.ASCOM_ALPACA,
                        name = "ASCOM Alpaca Hub ($responderIp)",
                        ipAddress = responderIp,
                        port = port,
                        latencyMs = latency,
                        discoveryMethod = "ASCOM Alpaca UDP Broadcast (32227)"
                    )
                    synchronized(foundList) {
                        if (foundList.none { it.ipAddress == responderIp && it.port == port }) {
                            foundList.add(device)
                            _discoveredScopes.value = foundList.toList()
                            addLog("✨ ASCOM Alpaca Hub descoperit automat la $responderIp:$port (ping: ${latency}ms)!")
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            // UDP broadcast might be constrained in some networks
        } finally {
            socket?.close()
        }
    }

    private suspend fun probeHotspotGateways(foundList: MutableList<DiscoveredSmartScope>) = withContext(Dispatchers.IO) {
        val hotspotTargets = listOf(
            Triple(SmartScopeModel.ZWO_SEESTAR_S50, "192.168.110.1", 4700),
            Triple(SmartScopeModel.DWARF_3, "192.168.88.1", 9988),
            Triple(SmartScopeModel.VAONIS_VESPERA, "192.168.42.1", 8080),
            Triple(SmartScopeModel.UNISTELLAR_EVSCOPE, "192.168.14.1", 5000)
        )

        for ((model, ip, port) in hotspotTargets) {
            val latency = probeTcpPort(ip, port, timeoutMs = 350)
            if (latency != null) {
                val device = DiscoveredSmartScope(
                    model = model,
                    name = "${model.label} (Hotspot Direct)",
                    ipAddress = ip,
                    port = port,
                    latencyMs = latency,
                    discoveryMethod = "Smart Telescope Hotspot Gateway"
                )
                synchronized(foundList) {
                    if (foundList.none { it.ipAddress == ip && it.port == port }) {
                        foundList.add(device)
                        _discoveredScopes.value = foundList.toList()
                        addLog("✨ ${model.label} detectat la $ip:$port!")
                    }
                }
            }
        }
    }

    private suspend fun sweepSubnet(prefix: String, foundList: MutableList<DiscoveredSmartScope>) = coroutineScope {
        val semaphore = Semaphore(25)
        val jobs = mutableListOf<Job>()

        // Common ports to test: 4700 (Seestar), 9988 (DWARF), 11112 (Alpaca)
        val portModelMap = listOf(
            Pair(4700, SmartScopeModel.ZWO_SEESTAR_S50),
            Pair(9988, SmartScopeModel.DWARF_3),
            Pair(11112, SmartScopeModel.ASCOM_ALPACA)
        )

        // Scan common host address range (1 to 254)
        for (host in 1..254) {
            val ip = "$prefix$host"
            for ((port, model) in portModelMap) {
                jobs.add(
                    launch(Dispatchers.IO) {
                        semaphore.withPermit {
                            val latency = probeTcpPort(ip, port, timeoutMs = 280)
                            if (latency != null) {
                                val device = DiscoveredSmartScope(
                                    model = model,
                                    name = "${model.label} ($ip)",
                                    ipAddress = ip,
                                    port = port,
                                    latencyMs = latency,
                                    discoveryMethod = "Subnet TCP Port Scanner"
                                )
                                synchronized(foundList) {
                                    if (foundList.none { it.ipAddress == ip && it.port == port }) {
                                        foundList.add(device)
                                        _discoveredScopes.value = foundList.toList()
                                        addLog("✨ ${model.label} detectat pe rețeaua locală la $ip:$port (${latency}ms)!")
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
        jobs.joinAll()
    }

    private fun probeTcpPort(ip: String, port: Int, timeoutMs: Int): Long? {
        val start = System.currentTimeMillis()
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                System.currentTimeMillis() - start
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        ips.add(addr.hostAddress ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return ips.filter { it.isNotBlank() }
    }

    /**
     * Attempts a real TCP handshake with the smart telescope at [ipAddress]:[port].
     * If device is unreachable on the current network, logs the diagnostic network error.
     */
    fun connectSmartTelescope(
        model: SmartScopeModel,
        ipAddress: String,
        port: Int = model.defaultPort
    ): Boolean {
        disconnect()
        addLog("Inițiere conexiune la ${model.label} pe $ipAddress:$port...")

        return try {
            val socket = Socket()
            // Real network handshake with 2.5s timeout
            socket.connect(InetSocketAddress(ipAddress, port), 2500)
            socket.soTimeout = 3000
            activeSocket = socket
            socketWriter = PrintWriter(socket.getOutputStream(), true)

            _telemetry.value = _telemetry.value.copy(
                model = model,
                ipAddress = ipAddress,
                port = port,
                isConnected = true,
                liveLog = _telemetry.value.liveLog + "Conexiune TCP stabilită cu succes cu ${model.label}."
            )

            // Send handshake probe
            val probeCmd = when (model) {
                SmartScopeModel.ZWO_SEESTAR_S50 -> "{\"id\":1,\"method\":\"scope_get_status\"}\r\n"
                SmartScopeModel.DWARF_3 -> "{\"cmd\":\"get_system_state\"}\r\n"
                SmartScopeModel.ASCOM_ALPACA -> "GET /api/v1/telescope/0/connected HTTP/1.1\r\nHost: $ipAddress\r\n\r\n"
                else -> ":GR#\r\n"
            }
            sendNetworkCommand(probeCmd)
            startTelemetryMonitor()
            true
        } catch (e: Exception) {
            addLog("⚠️ Conexiune eșuată la $ipAddress:$port (${e.javaClass.simpleName}: ${e.message}). Verifică dacă ești pe rețeaua WiFi a telescopului.")
            _telemetry.value = _telemetry.value.copy(
                model = model,
                ipAddress = ipAddress,
                port = port,
                isConnected = false
            )
            false
        }
    }

    /**
     * Sends coordinates of an exoplanet transit target to the smart telescope
     * and commands it to auto-center (Plate Solving & GoTo).
     */
    fun slewToTransitTarget(target: LiveTransitTarget) {
        if (!_telemetry.value.isConnected) {
            addLog("Eroare: Niciun smart telescope conectat. Conectează mai întâi dispozitivul.")
            return
        }

        val model = _telemetry.value.model
        addLog("Transmitere coordonate ${target.candidateName} (RA: ${target.rightAscension}, Dec: ${target.declination}) către ${model.label}...")

        val gotoCommand = when (model) {
            SmartScopeModel.ZWO_SEESTAR_S50 -> {
                // Parse RA hours to decimal degrees
                val raDeg = parseRaToDegrees(target.rightAscension)
                val decDeg = parseDecToDegrees(target.declination)
                "{\"id\":2,\"method\":\"scope_goto\",\"params\":{\"ra\":$raDeg,\"dec\":$decDeg,\"target\":\"${target.candidateName}\"}}\r\n"
            }
            SmartScopeModel.DWARF_3 -> {
                val raDeg = parseRaToDegrees(target.rightAscension)
                val decDeg = parseDecToDegrees(target.declination)
                "{\"cmd\":\"goto\",\"ra\":$raDeg,\"dec\":$decDeg}\r\n"
            }
            SmartScopeModel.ASCOM_ALPACA -> {
                val raHours = parseRaToDegrees(target.rightAscension) / 15.0
                val decDeg = parseDecToDegrees(target.declination)
                "PUT /api/v1/telescope/0/slewtocoordinatesasync HTTP/1.1\r\nHost: ${_telemetry.value.ipAddress}\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\nRightAscension=$raHours&Declination=$decDeg\r\n\r\n"
            }
            else -> {
                ":Sr${target.rightAscension}#:Sd${target.declination}#:MS#\r\n"
            }
        }

        sendNetworkCommand(gotoCommand)

        _telemetry.value = _telemetry.value.copy(
            targetName = target.candidateName,
            targetRa = target.rightAscension,
            targetDec = target.declination,
            isTracking = true
        )
    }

    /**
     * Starts an autonomous exoplanet photometric observation sequence:
     * e.g. 10-second sub-exposures with auto-stacking and FITS recording for the transit duration.
     */
    fun startPhotometricSequence(
        exposureSec: Double = 10.0,
        gain: Int = 80,
        filter: String = "UV/IR Cut"
    ) {
        if (!_telemetry.value.isConnected) {
            addLog("Nu se poate porni expunerea: telescopul nu este conectat.")
            return
        }

        addLog("Pornire secvență fotometrică: Expunere ${exposureSec}s, Gain $gain, Filtru: $filter. Înregistrare cadre FITS...")

        val captureCmd = when (_telemetry.value.model) {
            SmartScopeModel.ZWO_SEESTAR_S50 ->
                "{\"id\":3,\"method\":\"start_stack\",\"params\":{\"exp_time\":$exposureSec,\"gain\":$gain,\"filter\":\"$filter\"}}\r\n"
            SmartScopeModel.DWARF_3 ->
                "{\"cmd\":\"start_capture\",\"exp\":$exposureSec,\"gain\":$gain}\r\n"
            else ->
                ":START_EXPOSURE_$exposureSec#\r\n"
        }
        sendNetworkCommand(captureCmd)

        _telemetry.value = _telemetry.value.copy(
            isStacking = true,
            exposureTimeSec = exposureSec
        )
    }

    fun stopObservation() {
        addLog("Oprire achiziție cadre. Salvare sesiune FITS.")
        val stopCmd = when (_telemetry.value.model) {
            SmartScopeModel.ZWO_SEESTAR_S50 -> "{\"id\":4,\"method\":\"stop_stack\"}\r\n"
            SmartScopeModel.DWARF_3 -> "{\"cmd\":\"stop_capture\"}\r\n"
            else -> ":STOP#\r\n"
        }
        sendNetworkCommand(stopCmd)
        _telemetry.value = _telemetry.value.copy(isStacking = false)
    }

    fun disconnect() {
        telemetryJob?.cancel()
        try {
            socketWriter?.close()
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
        socketWriter = null
        _telemetry.value = _telemetry.value.copy(
            isConnected = false,
            isTracking = false,
            isStacking = false
        )
        addLog("Smart Telescope deconectat.")
    }

    private fun startTelemetryMonitor() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            val reader = activeSocket?.let { BufferedReader(InputStreamReader(it.getInputStream())) }
            while (isActive && _telemetry.value.isConnected) {
                try {
                    val line = reader?.readLine()
                    if (line != null) {
                        addLog("Răspuns Smart Scope: $line")
                    } else {
                        delay(2000)
                    }
                } catch (e: Exception) {
                    delay(2000)
                }
            }
        }
    }

    private fun sendNetworkCommand(cmd: String) {
        scope.launch {
            try {
                socketWriter?.print(cmd)
                socketWriter?.flush()
            } catch (e: Exception) {
                addLog("Eroare la transmiterea comenzii de rețea: ${e.message}")
            }
        }
    }

    private fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$timestamp] $msg"
        val updated = (_telemetry.value.liveLog + entry).takeLast(40)
        _telemetry.value = _telemetry.value.copy(liveLog = updated)
    }

    private fun parseRaToDegrees(raStr: String): Double {
        // format: "19h 42m 12s"
        return try {
            val parts = raStr.replace("h", " ").replace("m", " ").replace("s", " ").trim().split("\\s+".toRegex())
            val h = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val m = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val s = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            (h + m / 60.0 + s / 3600.0) * 15.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun parseDecToDegrees(decStr: String): Double {
        // format: "+41° 18′ 29″"
        return try {
            val sign = if (decStr.startsWith("-")) -1.0 else 1.0
            val clean = decStr.replace("+", "").replace("-", "")
                .replace("°", " ").replace("′", " ").replace("″", " ")
                .replace("'", " ").replace("\"", " ").trim()
            val parts = clean.split("\\s+".toRegex())
            val d = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val m = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val s = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            sign * (d + m / 60.0 + s / 3600.0)
        } catch (e: Exception) {
            0.0
        }
    }
}
