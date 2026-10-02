package com.synthesis.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class AlpacaTelescopeStatus(
    val isConnected: Boolean,
    val isSlewing: Boolean,
    val isTracking: Boolean,
    val rightAscensionHours: Double,
    val declinationDegrees: Double,
    val altitudeDegrees: Double,
    val azimuthDegrees: Double,
    val errorMessage: String? = null
)

/**
 * Native ASCOM Alpaca REST API Client.
 * Connects directly over WiFi/LAN to ASCOM Alpaca telescope drivers (Windows ASCOM Remote,
 * INDI-Alpaca, or embedded ESP32 OnStep Alpaca controllers).
 */
@Singleton
class AlpacaTelescopeService @Inject constructor() {

    private var clientTransactionId = 1

    suspend fun getConnected(host: String, port: Int = 11123, deviceNumber: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val endpoint = "http://$host:$port/api/v1/telescope/$deviceNumber/connected?ClientID=1&ClientTransactionID=${clientTransactionId++}"
        val response = executeGet(endpoint)
        response.optBoolean("Value", false)
    }

    suspend fun setConnected(host: String, port: Int = 11123, deviceNumber: Int = 0, connected: Boolean): Boolean = withContext(Dispatchers.IO) {
        val endpoint = "http://$host:$port/api/v1/telescope/$deviceNumber/connected"
        val body = "Connected=$connected&ClientID=1&ClientTransactionID=${clientTransactionId++}"
        val response = executePut(endpoint, body)
        response.optInt("ErrorNumber", -1) == 0
    }

    suspend fun getStatus(host: String, port: Int = 11123, deviceNumber: Int = 0): AlpacaTelescopeStatus = withContext(Dispatchers.IO) {
        try {
            val base = "http://$host:$port/api/v1/telescope/$deviceNumber"
            val connResp = executeGet("$base/connected?ClientID=1&ClientTransactionID=${clientTransactionId++}")
            val isConn = connResp.optBoolean("Value", false)

            if (!isConn) {
                return@withContext AlpacaTelescopeStatus(
                    isConnected = false,
                    isSlewing = false,
                    isTracking = false,
                    rightAscensionHours = 0.0,
                    declinationDegrees = 90.0,
                    altitudeDegrees = 0.0,
                    azimuthDegrees = 0.0
                )
            }

            val raResp = executeGet("$base/rightascension?ClientID=1&ClientTransactionID=${clientTransactionId++}")
            val decResp = executeGet("$base/declination?ClientID=1&ClientTransactionID=${clientTransactionId++}")
            val slewResp = executeGet("$base/slewing?ClientID=1&ClientTransactionID=${clientTransactionId++}")
            val trackResp = executeGet("$base/tracking?ClientID=1&ClientTransactionID=${clientTransactionId++}")

            AlpacaTelescopeStatus(
                isConnected = true,
                isSlewing = slewResp.optBoolean("Value", false),
                isTracking = trackResp.optBoolean("Value", true),
                rightAscensionHours = raResp.optDouble("Value", 0.0),
                declinationDegrees = decResp.optDouble("Value", 90.0),
                altitudeDegrees = 45.0,
                azimuthDegrees = 180.0
            )
        } catch (e: Exception) {
            AlpacaTelescopeStatus(
                isConnected = false,
                isSlewing = false,
                isTracking = false,
                rightAscensionHours = 0.0,
                declinationDegrees = 90.0,
                altitudeDegrees = 0.0,
                azimuthDegrees = 0.0,
                errorMessage = e.message
            )
        }
    }

    suspend fun slewToCoordinates(
        host: String,
        port: Int = 11123,
        deviceNumber: Int = 0,
        raHours: Double,
        decDegrees: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val endpoint = "http://$host:$port/api/v1/telescope/$deviceNumber/slewtocoordinatesasync"
        val body = "RightAscension=$raHours&Declination=$decDegrees&ClientID=1&ClientTransactionID=${clientTransactionId++}"
        val response = executePut(endpoint, body)
        response.optInt("ErrorNumber", -1) == 0
    }

    suspend fun abortPosition(host: String, port: Int = 11123, deviceNumber: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val endpoint = "http://$host:$port/api/v1/telescope/$deviceNumber/abortposition"
        val body = "ClientID=1&ClientTransactionID=${clientTransactionId++}"
        val response = executePut(endpoint, body)
        response.optInt("ErrorNumber", -1) == 0
    }

    suspend fun parkMount(host: String, port: Int = 11123, deviceNumber: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val endpoint = "http://$host:$port/api/v1/telescope/$deviceNumber/park"
        val body = "ClientID=1&ClientTransactionID=${clientTransactionId++}"
        val response = executePut(endpoint, body)
        response.optInt("ErrorNumber", -1) == 0
    }

    private fun executeGet(urlStr: String): JSONObject {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        return JSONObject(sb.toString())
    }

    private fun executePut(urlStr: String, urlEncodedBody: String): JSONObject {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.doOutput = true
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(urlEncodedBody)
        writer.flush()
        writer.close()

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        return JSONObject(sb.toString())
    }
}
