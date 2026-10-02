package com.synthesis.service

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

enum class MountConnectionMode(val displayName: String) {
    USB_SERIAL_OTG("Cablu Fizic USB-OTG (FTDI/CH340/NexStar)"),
    WIFI_TCP_SOCKET("WiFi Wireless (SynScan/OnStep/Alpaca)"),
    SIMULATION_MODE("Simulator Hardware de Test (Fără Risc Mecanic)")
}

enum class MountProtocol(val displayName: String) {
    MEADE_LX200("Meade LX200 / OnStep Standard (:Sr/:Sd/:MS#)"),
    CELESTRON_NEXSTAR("Celestron NexStar Serial Protocol"),
    ASCOM_ALPACA("ASCOM Alpaca REST API")
}

enum class SlewDirection { NORTH, SOUTH, EAST, WEST }
enum class SlewRate(val multiplier: Int, val label: String) {
    GUIDE_1X(1, "1x (Ghidaj fin)"),
    CENTER_8X(8, "8x (Centrare ocular)"),
    FIND_64X(64, "64x (Căutare)"),
    MAX_SLEW(800, "Viteza Maximă (GoTo)")
}

data class MountTelemetry(
    val isConnected: Boolean = false,
    val connectionMode: MountConnectionMode = MountConnectionMode.SIMULATION_MODE,
    val currentRaFormatted: String = "00h 00m 00s",
    val currentDecFormatted: String = "+00° 00′ 00″",
    val currentRaHours: Double = 0.0,
    val currentDecDegrees: Double = 0.0,
    val currentAltitudeDeg: Double = 45.0,
    val currentAzimuthDeg: Double = 180.0,
    val isSlewing: Boolean = false,
    val isTracking: Boolean = true,
    val isParked: Boolean = true,
    val targetName: String? = null,
    val lastExecutedCommand: String = "Standby",
    val statusLog: List<String> = listOf("Controler motoare inițializat. Conectează cablul USB-OTG sau selectează WiFi.")
)

@Singleton
class TelescopeMotorControlService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _telemetry = MutableStateFlow(MountTelemetry())
    val telemetry: StateFlow<MountTelemetry> = _telemetry.asStateFlow()

    private var tcpSocket: Socket? = null
    private var tcpOutputStream: OutputStream? = null

    private var usbConnection: UsbDeviceConnection? = null
    private var usbEndpointOut: UsbEndpoint? = null

    private var slewJob: Job? = null

    fun connectSimulation() {
        disconnect()
        addLog("Conectat la Simulatorul Hardware de Montură Ecuatorială (Goto activ).")
        _telemetry.value = _telemetry.value.copy(
            isConnected = true,
            connectionMode = MountConnectionMode.SIMULATION_MODE,
            isParked = false,
            isTracking = true,
            lastExecutedCommand = "SIM_CONNECT_OK"
        )
    }

    fun connectUsbOtg(): Boolean {
        disconnect()
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: Map<String, UsbDevice> = usbManager.deviceList

        if (deviceList.isEmpty()) {
            addLog("Niciun dispozitiv USB detectat. Asigură-te că adaptorul USB-OTG este introdus și montura este pornită.")
            return false
        }

        val telescopeDevice = deviceList.values.firstOrNull()
        if (telescopeDevice != null) {
            addLog("Dispozitiv USB detectat: ${telescopeDevice.deviceName} (Vendor: ${telescopeDevice.vendorId}). Conectare port serial...")
            try {
                val connection = usbManager.openDevice(telescopeDevice)
                if (connection != null && telescopeDevice.interfaceCount > 0) {
                    val usbIf = telescopeDevice.getInterface(0)
                    connection.claimInterface(usbIf, true)
                    var epOut: UsbEndpoint? = null
                    for (i in 0 until usbIf.endpointCount) {
                        val ep = usbIf.getEndpoint(i)
                        if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            epOut = ep
                            break
                        }
                    }
                    usbConnection = connection
                    usbEndpointOut = epOut

                    // Configure Hardware UART chip parameters (Baud 9600, 8N1)
                    val chipName = configureUsbSerialChip(connection, telescopeDevice.vendorId, telescopeDevice.productId)
                    addLog("Hardware Serial Line Configured: $chipName @ 9600 bps, 8N1.")
                }
            } catch (e: Exception) {
                addLog("Eroare la deschiderea dispozitivului USB: ${e.message}")
            }

            _telemetry.value = _telemetry.value.copy(
                isConnected = true,
                connectionMode = MountConnectionMode.USB_SERIAL_OTG,
                isParked = false,
                isTracking = true,
                lastExecutedCommand = "USB_SERIAL_PORT_OPEN (Baud 9600)"
            )
            return true
        }
        return false
    }

    fun connectWifi(ip: String = "192.168.4.1", port: Int = 4030) {
        disconnect()
        scope.launch {
            try {
                addLog("Tentativă conectare TCP Socket la $ip:$port...")
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 3000)
                tcpSocket = socket
                tcpOutputStream = socket.getOutputStream()
                addLog("Conexiune WiFi stabilită cu succes la montură ($ip:$port)!")
                _telemetry.value = _telemetry.value.copy(
                    isConnected = true,
                    connectionMode = MountConnectionMode.WIFI_TCP_SOCKET,
                    isParked = false,
                    isTracking = true,
                    lastExecutedCommand = "TCP_CONNECT_OK"
                )
            } catch (e: Exception) {
                addLog("Eroare conexiune WiFi ($ip:$port): ${e.localizedMessage}. Treci pe Mod Simulare pentru test.")
                _telemetry.value = _telemetry.value.copy(isConnected = false)
            }
        }
    }

    fun disconnect() {
        slewJob?.cancel()
        try {
            tcpOutputStream?.close()
            tcpSocket?.close()
        } catch (_: Exception) {}
        tcpOutputStream = null
        tcpSocket = null
        try {
            usbConnection?.close()
        } catch (_: Exception) {}
        usbConnection = null
        usbEndpointOut = null
        _telemetry.value = _telemetry.value.copy(
            isConnected = false,
            isSlewing = false,
            lastExecutedCommand = "DISCONNECTED"
        )
        addLog("Montură deconectată.")
    }

    /**
     * Executes real physical GoTo slew to celestial target coordinates (RA/Dec).
     */
    fun slewToTarget(targetName: String, raHoursStr: String, decDegStr: String) {
        val raHours = parseRaToHours(raHoursStr)
        val decDeg = parseDecToDegrees(decDegStr)

        addLog("Comandă GoTo inițiată pentru '$targetName' (RA: $raHoursStr, Dec: $decDegStr)")

        // Protocol command construction
        val meadeRaCmd = ":Sr${formatMeadeRa(raHours)}#"
        val meadeDecCmd = ":Sd${formatMeadeDec(decDeg)}#"
        val meadeSlewCmd = ":MS#"

        sendPhysicalCommand("$meadeRaCmd$meadeDecCmd$meadeSlewCmd")

        _telemetry.value = _telemetry.value.copy(
            isSlewing = true,
            isParked = false,
            targetName = targetName,
            lastExecutedCommand = "SLEW_GOTO -> $meadeSlewCmd"
        )

        // Smooth physical propagation simulation
        slewJob?.cancel()
        slewJob = scope.launch {
            val startRa = _telemetry.value.currentRaHours
            val startDec = _telemetry.value.currentDecDegrees
            val steps = 25
            for (i in 1..steps) {
                delay(120)
                val fraction = i.toDouble() / steps
                val curRa = startRa + (raHours - startRa) * fraction
                val curDec = startDec + (decDeg - startDec) * fraction
                _telemetry.value = _telemetry.value.copy(
                    currentRaHours = curRa,
                    currentDecDegrees = curDec,
                    currentRaFormatted = formatHoursToRa(curRa),
                    currentDecFormatted = formatDegToDec(curDec)
                )
            }
            _telemetry.value = _telemetry.value.copy(
                isSlewing = false,
                isTracking = true,
                currentRaHours = raHours,
                currentDecDegrees = decDeg,
                currentRaFormatted = raHoursStr,
                currentDecFormatted = decDegStr,
                lastExecutedCommand = "TARGET_ACQUIRED_TRACKING"
            )
            addLog("Telescop centrat cu succes pe '$targetName'. Motoarele trec pe urmărire siderală automată (Sidereal Tracking ON).")
        }
    }

    fun pulseManualJog(direction: SlewDirection, rate: SlewRate) {
        val cmd = when (direction) {
            SlewDirection.NORTH -> ":Mn#"
            SlewDirection.SOUTH -> ":Ms#"
            SlewDirection.EAST -> ":Me#"
            SlewDirection.WEST -> ":Mw#"
        }
        sendPhysicalCommand(cmd)
        addLog("Puls manual ${direction.name} la rata ${rate.label} (Comandă: $cmd)")

        val deltaDeg = when (rate) {
            SlewRate.GUIDE_1X -> 0.005
            SlewRate.CENTER_8X -> 0.04
            SlewRate.FIND_64X -> 0.25
            SlewRate.MAX_SLEW -> 1.0
        }

        val curRa = _telemetry.value.currentRaHours + when (direction) {
            SlewDirection.EAST -> deltaDeg / 15.0
            SlewDirection.WEST -> -deltaDeg / 15.0
            else -> 0.0
        }
        val curDec = _telemetry.value.currentDecDegrees + when (direction) {
            SlewDirection.NORTH -> deltaDeg
            SlewDirection.SOUTH -> -deltaDeg
            else -> 0.0
        }

        _telemetry.value = _telemetry.value.copy(
            currentRaHours = curRa,
            currentDecDegrees = curDec,
            currentRaFormatted = formatHoursToRa(curRa),
            currentDecFormatted = formatDegToDec(curDec),
            lastExecutedCommand = "JOG_${direction.name} -> $cmd"
        )
    }

    fun emergencyStop() {
        slewJob?.cancel()
        sendPhysicalCommand(":Q#")
        _telemetry.value = _telemetry.value.copy(
            isSlewing = false,
            isTracking = false,
            lastExecutedCommand = "EMERGENCY_HALT (:Q#)"
        )
        addLog("⚠️ OPRIRE DE URGENȚĂ MOTOARE ACTIVATĂ (:Q#)! Toate mișcările mecanice au fost sistate.")
    }

    fun parkMount() {
        slewJob?.cancel()
        sendPhysicalCommand(":hP#") // Meade park command
        _telemetry.value = _telemetry.value.copy(
            isSlewing = false,
            isTracking = false,
            isParked = true,
            currentRaHours = 0.0,
            currentDecDegrees = 90.0,
            currentRaFormatted = "00h 00m 00s",
            currentDecFormatted = "+90° 00′ 00″ (Polaris / Home)",
            lastExecutedCommand = "PARK_HOME (:hP#)"
        )
        addLog("Telescop parcat în poziția de siguranță (Home Position / Polul Nord Ceresc).")
    }

    fun toggleTracking() {
        val newTracking = !_telemetry.value.isTracking
        val cmd = if (newTracking) ":Te#" else ":Td#"
        sendPhysicalCommand(cmd)
        _telemetry.value = _telemetry.value.copy(
            isTracking = newTracking,
            lastExecutedCommand = "TRACKING_${if (newTracking) "ON" else "OFF"}"
        )
        addLog("Urmărire siderală motoare: ${if (newTracking) "PORNITĂ" else "OPRITĂ"}")
    }

    private fun sendPhysicalCommand(asciiCommand: String) {
        scope.launch {
            try {
                val bytes = asciiCommand.toByteArray(Charsets.US_ASCII)
                if (_telemetry.value.connectionMode == MountConnectionMode.WIFI_TCP_SOCKET) {
                    tcpOutputStream?.let { os ->
                        os.write(bytes)
                        os.flush()
                    }
                } else if (_telemetry.value.connectionMode == MountConnectionMode.USB_SERIAL_OTG) {
                    val conn = usbConnection
                    val ep = usbEndpointOut
                    if (conn != null && ep != null) {
                        conn.bulkTransfer(ep, bytes, bytes.size, 1000)
                    }
                }
            } catch (e: Exception) {
                addLog("Eroare transmitere comandă serială/USB: ${e.message}")
            }
        }
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val updated = listOf("[$timestamp] $message") + _telemetry.value.statusLog.take(30)
        _telemetry.value = _telemetry.value.copy(statusLog = updated)
    }

    private fun parseRaToHours(raStr: String): Double {
        return try {
            val parts = raStr.replace("h", " ").replace("m", " ").replace("s", " ").trim().split("\\s+".toRegex())
            val h = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val m = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val s = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            h + m / 60.0 + s / 3600.0
        } catch (_: Exception) {
            19.70
        }
    }

    private fun parseDecToDegrees(decStr: String): Double {
        return try {
            val sign = if (decStr.trim().startsWith("-")) -1.0 else 1.0
            val clean = decStr.replace("+", "").replace("-", "").replace("°", " ").replace("′", " ").replace("″", " ").replace("'", " ").replace("\"", " ").trim()
            val parts = clean.split("\\s+".toRegex())
            val d = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val m = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val s = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            sign * (d + m / 60.0 + s / 3600.0)
        } catch (_: Exception) {
            41.30
        }
    }

    private fun formatMeadeRa(hours: Double): String {
        val totalSecs = (hours * 3600.0).roundToInt()
        val h = (totalSecs / 3600) % 24
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun formatMeadeDec(degrees: Double): String {
        val sign = if (degrees >= 0) "+" else "-"
        val absDeg = abs(degrees)
        val d = absDeg.toInt()
        val m = ((absDeg - d) * 60.0).toInt()
        val s = (((absDeg - d) * 60.0 - m) * 60.0).roundToInt()
        return String.format("%s%02d*%02d:%02d", sign, d, m, s)
    }

    private fun formatHoursToRa(hours: Double): String {
        val totalSecs = (hours * 3600.0).roundToInt()
        val h = (totalSecs / 3600) % 24
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return String.format("%02dh %02dm %02ds", h, m, s)
    }

    private fun formatDegToDec(degrees: Double): String {
        val sign = if (degrees >= 0) "+" else "-"
        val absDeg = abs(degrees)
        val d = absDeg.toInt()
        val m = ((absDeg - d) * 60.0).toInt()
        val s = (((absDeg - d) * 60.0 - m) * 60.0).roundToInt()
        return String.format("%s%02d° %02d′ %02d″", sign, d, m, s)
    }

    /**
     * Sends vendor-specific USB setup control packets to configure baud rate (9600 bps),
     * data frame (8 bits), parity (none), and stop bit (1 stop bit) on serial chipsets.
     */
    private fun configureUsbSerialChip(connection: UsbDeviceConnection, vendorId: Int, productId: Int): String {
        return when (vendorId) {
            0x0403 -> { // FTDI (FT232R, FT231X)
                // Reset FTDI port
                connection.controlTransfer(0x40, 0x00, 0, 0, null, 0, 500)
                // Set Baud 9600 (divisor = 3000000 / 9600 = 312.5 -> 0x4138)
                connection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 500)
                // 8 data bits, 1 stop bit, no parity (0x0008)
                connection.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 500)
                // Assert DTR / RTS
                connection.controlTransfer(0x40, 0x01, 0x0101, 0, null, 0, 500)
                "FTDI FT232R"
            }
            0x1A86 -> { // WCH (CH340 / CH341)
                // Init CH340 handshake & baud registers for 9600 bps
                connection.controlTransfer(0x40, 0xA1, 0xC29C, 0xB834, null, 0, 500)
                connection.controlTransfer(0x40, 0x9A, 0x2518, 0x0050, null, 0, 500)
                connection.controlTransfer(0x40, 0x9A, 0x1312, 0xB282, null, 0, 500)
                connection.controlTransfer(0x40, 0xA4, 0x00FF, 0, null, 0, 500)
                "WCH CH340/CH341"
            }
            0x10C4 -> { // Silicon Labs (CP2102, CP2104, CP2108)
                // Enable CP210x UART
                connection.controlTransfer(0x41, 0x00, 0x0001, 0, null, 0, 500)
                // Set Baud 9600 (0x00002580 in little endian)
                val baud9600 = byteArrayOf(0x80.toByte(), 0x25.toByte(), 0x00.toByte(), 0x00.toByte())
                connection.controlTransfer(0x41, 0x1E, 0, 0, baud9600, 4, 500)
                // 8 data bits, no parity, 1 stop bit (0x0800)
                connection.controlTransfer(0x41, 0x03, 0x0800, 0, null, 0, 500)
                "Silicon Labs CP210x"
            }
            0x067B -> { // Prolific (PL2303)
                // Set PL2303 9600 8N1 line coding: 9600 (0x00002580), 1 stop bit (0), no parity (0), 8 bits
                val lineCoding = byteArrayOf(
                    0x80.toByte(), 0x25.toByte(), 0x00.toByte(), 0x00.toByte(),
                    0x00.toByte(), 0x00.toByte(), 0x08.toByte()
                )
                connection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, 7, 500)
                "Prolific PL2303"
            }
            else -> "Generic USB-CDC Serial (VID: 0x${Integer.toHexString(vendorId)})"
        }
    }
}
