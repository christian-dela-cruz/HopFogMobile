package com.example.hopfog

import android.content.Context
import android.net.wifi.WifiManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object ESP32ConnectionManager {

    const val ESP32_SSID = "HopFog-Network"
    private const val STATUS_URL = "http://hopfog.com/status"
    private const val PING_TIMEOUT_MS = 3000L

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val pingClient = HttpClient(CIO) {
        engine {
            requestTimeout = PING_TIMEOUT_MS
        }
    }

    @Suppress("DEPRECATION")
    suspend fun isConnectedToESP32(context: Context): Boolean {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
        val wifiInfo = wifiManager.connectionInfo ?: return false
        val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: return false
        return ssid == ESP32_SSID
    }

    suspend fun checkESP32Reachable(): Boolean {
        return try {
            val response: HttpResponse = pingClient.get(STATUS_URL)
            val json = JSONObject(response.bodyAsText())
            val reachable = json.optBoolean("online", false)
            _connectionState.value = reachable
            reachable
        } catch (e: Exception) {
            _connectionState.value = false
            false
        }
    }

    fun getConnectionStatus(): StateFlow<Boolean> = connectionState

    /**
     * Checks if the device is connected to the HopFog-Network WiFi.
     * Returns true if connected to the correct SSID, false otherwise.
     */
    suspend fun ensureWifiConnection(context: Context): Boolean {
        val connected = isConnectedToESP32(context)
        if (!connected) {
            _connectionState.value = false
        }
        return connected
    }
}
