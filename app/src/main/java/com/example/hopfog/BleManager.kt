package com.example.hopfog

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.*

// Callback interface to report transaction results
interface BleTransactionCallback {
    fun onTransactionSuccess(response: String)
    fun onTransactionFailure(error: String)
}

@SuppressLint("MissingPermission")
object BleManager {

    private const val TAG = "BleManager"
    // Use your app-specific UUIDs
    private val HOPFOG_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b") // Must match ESP32
    private val MESSAGE_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8") // Must match ESP32
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val SCAN_TIMEOUT = 10000L // 10 seconds
    private const val CONNECT_TIMEOUT = 5000L // 5 seconds

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleGatt: BluetoothGatt? = null
    private var appContext: Context? = null

    // --- State Management for a single transaction ---
    private var currentCallback: BleTransactionCallback? = null
    private var dataToSend: ByteArray? = null
    private val handler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (bluetoothAdapter == null) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = manager.adapter
        }
    }

    // --- Core GATT Callback for managing the transaction lifecycle ---
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            handler.removeCallbacksAndMessages(null) // Clear any connection timeout
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "GATT Connected. Discovering services...")
                    bleGatt = gatt
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT Disconnected.")
                    // If callback is not null, it means we disconnected unexpectedly.
                    currentCallback?.let { finishTransaction(false, "Disconnected unexpectedly") }
                }
            } else {
                Log.e(TAG, "Connection failed with error: $status")
                finishTransaction(false, "Connection failed: status $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(HOPFOG_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    enableNotificationsAndWrite(gatt, characteristic)
                } else {
                    finishTransaction(false, "Characteristic not found")
                }
            } else {
                finishTransaction(false, "Service discovery failed")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Notifications enabled. Writing data...")
                val characteristic = descriptor.characteristic
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = dataToSend
                gatt.writeCharacteristic(characteristic)
            } else {
                finishTransaction(false, "Failed to enable notifications")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val response = characteristic.value.toString(Charsets.UTF_8)
            Log.d(TAG, "Received response: $response")
            finishTransaction(true, response) // Success!
        }
    }

    // --- Scan Callback ---
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Check if the found device has our specific service UUID
            if (result.scanRecord?.serviceUuids?.any { it.uuid == HOPFOG_SERVICE_UUID } == true) {
                Log.d(TAG, "HopFog Gateway found: ${result.device.name ?: "Unknown"}")
                stopScan()
                handler.postDelayed({
                    finishTransaction(false, "Connection timed out")
                }, CONNECT_TIMEOUT)
                result.device.connectGatt(appContext, false, gattCallback)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            finishTransaction(false, "BLE scan failed: code $errorCode")
        }
    }

    // --- PUBLIC METHOD TO START A TRANSACTION ---
    fun performTransaction(data: String, cb: BleTransactionCallback) {
        val context = appContext ?: run {
            cb.onTransactionFailure("BleManager not initialized")
            return
        }
        if (!hasPermissions(context)) {
            cb.onTransactionFailure("Bluetooth permissions not granted")
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            cb.onTransactionFailure("Bluetooth is not enabled")
            return
        }

        this.currentCallback = cb
        this.dataToSend = data.toByteArray(Charsets.UTF_8)

        Log.d(TAG, "Starting transaction. Scanning for HopFog Gateway...")
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)

        handler.postDelayed({
            stopScan()
            finishTransaction(false, "Device not found")
        }, SCAN_TIMEOUT)
    }

    // --- Helper & Cleanup Methods ---
    private fun enableNotificationsAndWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun stopScan() {
        handler.removeCallbacksAndMessages(null)
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun finishTransaction(success: Boolean, message: String) {
        Handler(Looper.getMainLooper()).post {
            bleGatt?.disconnect()
            bleGatt?.close()
            bleGatt = null
            if (success) {
                currentCallback?.onTransactionSuccess(message)
            } else {
                currentCallback?.onTransactionFailure(message)
            }
            currentCallback = null // Mark transaction as complete
        }
    }

    // --- Permissions Check ---
    fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // For older Android, location is required for scanning
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}