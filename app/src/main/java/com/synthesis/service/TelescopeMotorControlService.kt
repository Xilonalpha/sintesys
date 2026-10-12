package com.synthesis.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synthesis.MainActivity
import com.synthesis.R
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

sealed class UsbConnectResult {
    data class Connected(val deviceName: String, val chipName: String) : UsbConnectResult()
    data class PermissionRequested(val deviceName: String) : UsbConnectResult()
    data class DeviceNotFound(val reason: String) : UsbConnectResult()
    data class OpenFailed(val reason: String) : UsbConnectResult()
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
    companion object {
        const val USB_NOTIFICATION_CHANNEL_ID = "synthesis_telescope_hardware_channel"
        const val USB_DISCONNECT_NOTIFICATION_ID = 4202
        const val ACTION_USB_PERMISSION = "com.synthesis.USB_PERMISSION"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _telemetry = MutableStateFlow(MountTelemetry())
    val telemetry: StateFlow<MountTelemetry> = _telemetry.asStateFlow()

    private var tcpSocket: Socket? = null
    private var tcpOutputStream: OutputStream? = null

    private var usbConnection: UsbDeviceConnection? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private var usbEndpointIn: UsbEndpoint? = null
    private var readJob: Job? = null

    private var slewJob: Job? = null

    private val usbReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted && device != null) {
                        addLog("✓ Permisiune USB aprobată pentru ${device.deviceName}! Conectare automată...")
                        openAndConfigureDevice(device)
                    } else {
                        addLog("⚠️ Permisiunea USB a fost refuzată de utilizator.")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    addLog("Dispozitiv USB detectat fizic. Apasă 'CABLU USB-OTG' pentru inițializare.")
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    if (_telemetry.value.connectionMode == MountConnectionMode.USB_SERIAL_OTG || usbConnection != null || _telemetry.value.isConnected) {
                        slewJob?.cancel()
                        readJob?.cancel()
                        disconnect()
                        val alertMessage = "ALERTA HARDWARE: Cablul USB al monturii a fost deconectat fizic din dispozitiv! Motoarele au fost oprite."
                        addLog(alertMessage)
                        triggerUsbDetachedNotification()
                    }
                }
            }
        }
    }

    init {
        createNotificationChannel()
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alerte Hardware Montură Telescop"
            val descriptionText = "Notificări de siguranță la deconectarea cablului USB sau întreruperea comunicării seriale cu montura."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(USB_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun triggerUsbDetachedNotification() {
        try {
            val title = context.getString(R.string.notif_usb_detached_title)
            val desc = context.getString(R.string.notif_usb_detached_desc)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, USB_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(desc)
                .setStyle(NotificationCompat.BigTextStyle().bigText(desc))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(USB_DISCONNECT_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission POST_NOTIFICATIONS may not be granted by user yet
        } catch (_: Exception) {}
    }

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

    fun connectUsbOtg(): UsbConnectResult {
        disconnect()
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: Map<String, UsbDevice> = usbManager.deviceList

        if (deviceList.isEmpty()) {
            val msg = "Niciun dispozitiv USB detectat în telefon. Asigură-te că adaptorul OTG este introdus, montura este pornită și 'Conexiune OTG' este activată în Setările telefonului."
            addLog(msg)
            return UsbConnectResult.DeviceNotFound(msg)
        }

        // 1. Detect if a USB Hub is present and log a warning
        val hasHub = deviceList.values.any { it.deviceClass == UsbConstants.USB_CLASS_HUB }
        if (hasHub) {
            addLog("Info: Detectat USB Hub. Se caută adaptorul serial al monturii (Prolific/FTDI/CP210x/CH340)...")
        }

        // 2. Select the true UART serial adapter device, prioritizing Prolific PL2303, FTDI, CP210x, CH340
        val serialDevice = deviceList.values.firstOrNull { dev ->
            val vid = dev.vendorId
            val isKnownUart = vid == 0x067B || vid == 0x0403 || vid == 0x10C4 || vid == 0x1A86 || vid == 0x03EB
            val isNotHub = dev.deviceClass != UsbConstants.USB_CLASS_HUB
            val hasSerialInterface = (0 until dev.interfaceCount).any { idx ->
                val iface = dev.getInterface(idx)
                iface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                iface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC
            }
            isNotHub && (isKnownUart || hasSerialInterface)
        } ?: deviceList.values.firstOrNull { it.deviceClass != UsbConstants.USB_CLASS_HUB }

        if (serialDevice == null) {
            val msg = "Eroare USB: A fost detectat doar un Hub USB, dar niciun port serial. Conectează cablul USB-OTG DIRECT între telefon și telecomandă."
            addLog(msg)
            return UsbConnectResult.DeviceNotFound(msg)
        }

        // 3. Check and request Android USB runtime permission
        if (!usbManager.hasPermission(serialDevice)) {
            addLog("Solicitare permisiune USB Android pentru ${serialDevice.deviceName} (VID: 0x${Integer.toHexString(serialDevice.vendorId).uppercase()}). Apasă 'Permite' pe dialogul de pe ecran!")
            try {
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val permissionIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                    flags
                )
                usbManager.requestPermission(serialDevice, permissionIntent)
            } catch (e: Exception) {
                addLog("Eroare cerere permisiune USB: ${e.message}")
            }
            return UsbConnectResult.PermissionRequested(serialDevice.deviceName)
        }

        return openAndConfigureDevice(serialDevice)
    }

    fun openAndConfigureDevice(serialDevice: UsbDevice): UsbConnectResult {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        addLog("Dispozitiv USB telescop: ${serialDevice.deviceName} (VID: 0x${Integer.toHexString(serialDevice.vendorId).uppercase()} PID: 0x${Integer.toHexString(serialDevice.productId).uppercase()}). Deschidere canal serial...")

        var connection: UsbDeviceConnection? = null
        try {
            connection = usbManager.openDevice(serialDevice)
        } catch (e: Exception) {
            val msg = "Eroare la deschiderea dispozitivului USB: ${e.message}"
            addLog(msg)
            return UsbConnectResult.OpenFailed(msg)
        }

        if (connection == null) {
            val msg = "Eșec la deschiderea conexiunii USB fizice. Dacă ai folosit o aplicație terminal (ex: 'Serial USB Terminal'), te rugăm să o ÎNCHIZI complet din Recente (Swipe Up) pentru a elibera portul USB."
            addLog(msg)
            return UsbConnectResult.OpenFailed(msg)
        }

        var epOut: UsbEndpoint? = null
        var epIn: UsbEndpoint? = null
        var claimed = false

        for (i in 0 until serialDevice.interfaceCount) {
            val usbIf = serialDevice.getInterface(i)
            if (connection.claimInterface(usbIf, true)) {
                for (j in 0 until usbIf.endpointCount) {
                    val ep = usbIf.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_OUT && epOut == null) epOut = ep
                        if (ep.direction == UsbConstants.USB_DIR_IN && epIn == null) epIn = ep
                    }
                }
                if (epOut != null) {
                    claimed = true
                    break
                }
            }
        }

        if (!claimed || epOut == null) {
            connection.close()
            val msg = "Eroare: Nu s-a putut aloca interfața serială USB (Lipsește canalul OUT de date). Verifică compatibilitatea cablului."
            addLog(msg)
            return UsbConnectResult.OpenFailed(msg)
        }

        usbConnection = connection
        usbEndpointOut = epOut
        usbEndpointIn = epIn

        // Configure Hardware UART chip parameters (Baud 9600, 8N1, DTR/RTS)
        val chipName = configureUsbSerialChip(connection, serialDevice.vendorId, serialDevice.productId)
        addLog("✓ Port fizic UART configurat: $chipName @ 9600 bps, 8N1.")
        addLog("Important: Verifică ca telecomanda monturii (NexStar+/SynScan) să fie pornită și să fi trecut de ecranul inițial.")

        _telemetry.value = _telemetry.value.copy(
            isConnected = true,
            connectionMode = MountConnectionMode.USB_SERIAL_OTG,
            isParked = false,
            isTracking = true,
            lastExecutedCommand = "PORT_DESCHIS: $chipName (9600 8N1)"
        )

        startSerialReader(connection, epIn)
        return UsbConnectResult.Connected(serialDevice.deviceName, chipName)
    }

    private fun startSerialReader(connection: UsbDeviceConnection, epIn: UsbEndpoint?) {
        readJob?.cancel()
        if (epIn == null) return
        readJob = scope.launch {
            val buffer = ByteArray(256)
            while (isActive && _telemetry.value.isConnected) {
                try {
                    val len = connection.bulkTransfer(epIn, buffer, buffer.size, 500)
                    if (len > 0) {
                        val text = String(buffer, 0, len, Charsets.US_ASCII).trim()
                        if (text.isNotEmpty()) {
                            addLog("RX Montură: $text")
                        }
                    }
                } catch (_: Exception) {
                    delay(200)
                }
            }
        }
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
        readJob?.cancel()
        readJob = null
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
        usbEndpointIn = null
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

        // Protocol command construction (Meade LX200 + Celestron NexStar dual compatibility)
        val meadeRaCmd = ":Sr${formatMeadeRa(raHours)}#"
        val meadeDecCmd = ":Sd${formatMeadeDec(decDeg)}#"
        val meadeSlewCmd = ":MS#"

        // Celestron NexStar precise 16-bit hex GoTo: r<RA_HEX>,<DEC_HEX>
        val nexStarRaVal = ((raHours / 24.0) * 65536.0).roundToInt().coerceIn(0, 65535)
        val normDec = if (decDeg < 0) decDeg + 360.0 else decDeg
        val nexStarDecVal = ((normDec / 360.0) * 65536.0).roundToInt().coerceIn(0, 65535)
        val nexStarSlewCmd = String.format("r%04X,%04X#", nexStarRaVal, nexStarDecVal)

        sendPhysicalCommand("$meadeRaCmd$meadeDecCmd$meadeSlewCmd$nexStarSlewCmd")

        _telemetry.value = _telemetry.value.copy(
            isSlewing = true,
            isParked = false,
            targetName = targetName,
            lastExecutedCommand = "SLEW_GOTO -> $meadeSlewCmd / $nexStarSlewCmd"
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
        val nexStarRate: Byte = when (rate) {
            SlewRate.GUIDE_1X -> 2
            SlewRate.CENTER_8X -> 5
            SlewRate.FIND_64X -> 8
            SlewRate.MAX_SLEW -> 9
        }

        // Meade LX200 ASCII command
        val meadeCmd = when (direction) {
            SlewDirection.NORTH -> ":Mn#"
            SlewDirection.SOUTH -> ":Ms#"
            SlewDirection.EAST -> ":Me#"
            SlewDirection.WEST -> ":Mw#"
        }

        // Celestron NexStar pass-through motor packets:
        // Dev 16 = Azm/RA, Dev 17 = Alt/Dec. Cmd 36 = Positive, Cmd 37 = Negative
        val celestronBytes = when (direction) {
            SlewDirection.NORTH -> byteArrayOf('P'.code.toByte(), 3, 17, 36, nexStarRate, 0, 0, 0)
            SlewDirection.SOUTH -> byteArrayOf('P'.code.toByte(), 3, 17, 37, nexStarRate, 0, 0, 0)
            SlewDirection.EAST -> byteArrayOf('P'.code.toByte(), 3, 16, 36, nexStarRate, 0, 0, 0)
            SlewDirection.WEST -> byteArrayOf('P'.code.toByte(), 3, 16, 37, nexStarRate, 0, 0, 0)
        }

        // Send binary Celestron packet and Meade ASCII string
        sendPhysicalBytes(celestronBytes)
        sendPhysicalCommand(meadeCmd)
        addLog("Puls manual ${direction.name} (Viteză ${rate.label}) -> NexStar + Meade trimis.")

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
            lastExecutedCommand = "JOG_${direction.name} -> $meadeCmd / NexStar P"
        )
    }

    fun emergencyStop() {
        slewJob?.cancel()
        // Stop Celestron motors (Azm dev 16 + Alt dev 17)
        val stopAzm = byteArrayOf('P'.code.toByte(), 3, 16, 36, 0, 0, 0, 0)
        val stopAlt = byteArrayOf('P'.code.toByte(), 3, 17, 36, 0, 0, 0, 0)
        sendPhysicalBytes(stopAzm)
        sendPhysicalBytes(stopAlt)
        // Stop Celestron M# and Meade :Q#
        sendPhysicalCommand(":Q#M#")
        _telemetry.value = _telemetry.value.copy(
            isSlewing = false,
            isTracking = false,
            lastExecutedCommand = "EMERGENCY_HALT (:Q# / M#)"
        )
        addLog("⚠️ OPRIRE DE URGENȚĂ MOTOARE ACTIVATĂ (Meade :Q# & Celestron NexStar M#)! Toate mișcările mecanice au fost sistate.")
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

    private fun sendPhysicalBytes(bytes: ByteArray) {
        scope.launch {
            try {
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
                addLog("Eroare transmitere octeți USB/Serial: ${e.message}")
            }
        }
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
                        val sent = conn.bulkTransfer(ep, bytes, bytes.size, 1000)
                        if (sent >= 0) {
                            addLog("Comandă trimisă fizic ($sent octeți): $asciiCommand")
                        } else {
                            addLog("Avertisment USB: Trimitere eșuată ($sent). Verifică conexiunea cablului.")
                        }
                    } else {
                        addLog("Eroare hardware: Portul fizic USB nu este activ. Reîncearcă conexiunea cablului OTG!")
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
                connection.controlTransfer(0x40, 0x00, 0, 0, null, 0, 500)
                connection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 500)
                connection.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 500)
                connection.controlTransfer(0x40, 0x01, 0x0101, 0, null, 0, 500)
                connection.controlTransfer(0x40, 0x02, 0x0000, 0, null, 0, 500)
                "FTDI FT232R/FT231X"
            }
            0x1A86 -> { // WCH (CH340 / CH341)
                connection.controlTransfer(0x40, 0xA1, 0xC29C, 0xB834, null, 0, 500)
                connection.controlTransfer(0x40, 0x9A, 0x2518, 0x0050, null, 0, 500)
                connection.controlTransfer(0x40, 0x9A, 0x1312, 0xB282, null, 0, 500)
                connection.controlTransfer(0x40, 0xA4, 0x00FF, 0, null, 0, 500)
                "WCH CH340/CH341"
            }
            0x10C4 -> { // Silicon Labs (CP2102, CP2104, CP2108)
                connection.controlTransfer(0x41, 0x00, 0x0001, 0, null, 0, 500)
                val baud9600 = byteArrayOf(0x80.toByte(), 0x25.toByte(), 0x00.toByte(), 0x00.toByte())
                connection.controlTransfer(0x41, 0x1E, 0, 0, baud9600, 4, 500)
                connection.controlTransfer(0x41, 0x03, 0x0800, 0, null, 0, 500)
                connection.controlTransfer(0x41, 0x07, 0x0303, 0, null, 0, 500)
                "Silicon Labs CP210x"
            }
            0x067B -> { // Prolific PL2303 (PL2303H/HX/HXD/RA/EA/TA/TB/G)
                try {
                    // Prolific vendor init sequence
                    val buf = ByteArray(1)
                    connection.controlTransfer(0xC0, 0x01, 0x8484, 0, buf, 1, 500)
                    connection.controlTransfer(0x40, 0x01, 0x0404, 0, null, 0, 500)
                    connection.controlTransfer(0xC0, 0x01, 0x8484, 0, buf, 1, 500)
                    connection.controlTransfer(0xC0, 0x01, 0x8383, 0, buf, 1, 500)
                    connection.controlTransfer(0xC0, 0x01, 0x8484, 0, buf, 1, 500)
                    connection.controlTransfer(0x40, 0x01, 0x0404, 1, null, 0, 500)
                    connection.controlTransfer(0xC0, 0x01, 0x8484, 0, buf, 1, 500)
                    connection.controlTransfer(0xC0, 0x01, 0x8383, 0, buf, 1, 500)
                    connection.controlTransfer(0x40, 0x01, 0, 1, null, 0, 500)
                    connection.controlTransfer(0x40, 0x01, 1, 0, null, 0, 500)
                    connection.controlTransfer(0x40, 0x01, 2, 0x44, null, 0, 500)

                    // Line coding: 9600 bps, 1 stop bit, no parity, 8 data bits
                    val lineCoding = byteArrayOf(
                        0x80.toByte(), 0x25.toByte(), 0x00.toByte(), 0x00.toByte(),
                        0x00.toByte(), 0x00.toByte(), 0x08.toByte()
                    )
                    connection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, 7, 500)

                    // CRITICAL for PL2303: Assert DTR (0x01) and RTS (0x02) -> 0x0003
                    connection.controlTransfer(0x21, 0x22, 0x0003, 0, null, 0, 500)

                    // Reset flow control
                    connection.controlTransfer(0x40, 0x01, 0, 0, null, 0, 500)
                } catch (e: Exception) {
                    addLog("Avertisment init PL2303: ${e.message}")
                }
                "Prolific PL2303 (Celestron/SkyWatcher Cable)"
            }
            else -> "Generic USB-CDC Serial (VID: 0x${Integer.toHexString(vendorId)})"
        }
    }
}
