package com.example.hopfog

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

// These UUIDs must match the ones on your BLE device's firmware.
// You can generate your own unique UUIDs online.
private const val HOPFOG_SERVICE_UUID = "0000abf0-0000-1000-8000-00805f9b34fb"
private const val MESSAGE_CHARACTERISTIC_UUID = "0000abf1-0000-1000-8000-00805f9b34fb"

@SuppressLint("MissingPermission") // We will handle permissions explicitly before calling these functions.
object BleManager {

    private const val TAG = "BleManager"

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleGatt: BluetoothGatt? = null

    // --- Connection State Flow ---
    // UI can observe this to show "Connecting...", "Connected", "Disconnected"
    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState = _connectionState.asStateFlow()

    fun initialize(context: Context): Boolean {
        if (bluetoothAdapter == null) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = manager.adapter
        }
        return bluetoothAdapter != null && bluetoothAdapter!!.isEnabled
    }

    fun startBleScan(context: Context) {
        if (!hasPermissions(context)) {
            Log.e(TAG, "BLE Scan requires permissions.")
            return
        }

        if (bluetoothAdapter?.bluetoothLeScanner == null) {
            Log.e(TAG, "Bluetooth not initialized or not supported.")
            return
        }

        Log.d(TAG, "Starting BLE Scan...")
        _connectionState.value = "Scanning..."
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // We'll connect to the first device we find that has a name.
            // In a real app, you'd show a list of devices to the user.
            if (!result.device.name.isNullOrEmpty()) {
                Log.d(TAG, "Found BLE device: ${result.device.name} (${result.device.address})")
                stopBleScan()
                connectToDevice(result.device)
            }
        }
    }

    fun stopBleScan() {
        Log.d(TAG, "Stopping BLE Scan.")
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to ${device.name}...")
        _connectionState.value = "Connecting to ${device.name}"
        bleGatt = device.connectGatt(null, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Successfully connected to GATT server.")
                _connectionState.value = "Connected"
                // Discover services after successful connection
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Disconnected from GATT server.")
                _connectionState.value = "Disconnected"
                bleGatt?.close()
                bleGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered.")
                // You can log the services here to debug
                // gatt.services.forEach { service -> Log.d(TAG, "Service: ${service.uuid}") }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }
    }

    fun sendMessage(message: String): Boolean {
        if (bleGatt == null || _connectionState.value != "Connected") {
            Log.e(TAG, "Not connected to a BLE device.")
            return false
        }

        val service = bleGatt?.getService(UUID.fromString(HOPFOG_SERVICE_UUID))
        if (service == null) {
            Log.e(TAG, "HopFog service not found.")
            return false
        }

        val characteristic = service.getCharacteristic(UUID.fromString(MESSAGE_CHARACTERISTIC_UUID))
        if (characteristic == null) {
            Log.e(TAG, "Message characteristic not found.")
            return false
        }

        Log.d(TAG, "Sending message: $message")
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = message.toByteArray(Charsets.UTF_8)
        return bleGatt?.writeCharacteristic(characteristic) ?: false
    }

    // --- Helper for checking permissions ---
    private fun hasPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}