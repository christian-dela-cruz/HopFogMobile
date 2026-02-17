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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Scanning : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

@SuppressLint("MissingPermission")
object BleManager {

    private const val TAG = "BleManager"
    private val HOPFOG_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val MESSAGE_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleGatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null // NEW: Hold a reference to the characteristic
    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val status = _status.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
    }

    // --- Core GATT Callback ---
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "GATT Connected. Discovering services...")
                    _status.value = ConnectionStatus.Connecting
                    bleGatt = gatt
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT Disconnected.")
                    // Don't report an error on intentional disconnect.
                    if (_status.value != ConnectionStatus.Disconnected) {
                        _status.value = ConnectionStatus.Disconnected
                    }
                    bleGatt?.close()
                    bleGatt = null
                }
            } else {
                Log.e(TAG, "Connection failed with error: $status")
                _status.value = ConnectionStatus.Error("Connection failed: status $status")
                bleGatt?.close()
                bleGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(HOPFOG_SERVICE_UUID)
                messageCharacteristic = service?.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID)
                if (messageCharacteristic != null) {
                    enableNotifications(gatt, messageCharacteristic!!)
                } else {
                    _status.value = ConnectionStatus.Error("Characteristic not found")
                    disconnect()
                }
            } else {
                _status.value = ConnectionStatus.Error("Service discovery failed")
                disconnect()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Notifications enabled. Connection fully established.")
                _status.value = ConnectionStatus.Connected // We are now fully connected!
            } else {
                _status.value = ConnectionStatus.Error("Failed to enable notifications")
                disconnect()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val response = characteristic.value.toString(Charsets.UTF_8)
            Log.d(TAG, "Received notification: $response")

            // --- THIS IS THE FIX ---
            // The BLE callback runs on a background thread.
            // We must post the work to the main thread before emitting to the flow
            // that the UI is observing.
            handler.post {
                _incomingMessages.tryEmit(response)
            }
            // --- END OF FIX ---
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful.")
            } else {
                Log.w(TAG, "Write failed with status: $status")
            }
        }
    }

    // --- Scan Callback ---
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.scanRecord?.serviceUuids?.any { it.uuid == HOPFOG_SERVICE_UUID } == true) {
                Log.d(TAG, "HopFog Hub found: ${result.device.name ?: "Unknown"}")
                stopScan()
                _status.value = ConnectionStatus.Connecting
                result.device.connectGatt(appContext, false, gattCallback)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            _status.value = ConnectionStatus.Error("BLE scan failed: code $errorCode")
        }
    }

    // --- NEW PUBLIC METHODS for connection management ---

    fun connect() {
        val context = appContext ?: run {
            _status.value = ConnectionStatus.Error("BleManager not initialized")
            return
        }
        if (!hasPermissions(context) || bluetoothAdapter?.isEnabled != true) {
            _status.value = ConnectionStatus.Error("Permissions or Bluetooth not enabled")
            return
        }

        Log.d(TAG, "Starting scan for HopFog Hub...")
        _status.value = ConnectionStatus.Scanning
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)

        handler.postDelayed({
            if (_status.value == ConnectionStatus.Scanning) {
                stopScan()
                _status.value = ConnectionStatus.Error("Device not found")
            }
        }, 10000L) // 10 second scan timeout
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        _status.value = ConnectionStatus.Disconnected
        bleGatt?.disconnect()
    }

    // --- NEW PUBLIC METHODS for sending data ---

    fun sendJson(jsonString: String) {
        if (_status.value != ConnectionStatus.Connected) {
            Log.w(TAG, "Not connected, cannot send data.")
            return
        }
        messageCharacteristic?.let { char ->
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            char.value = jsonString.toByteArray(Charsets.UTF_8)
            bleGatt?.writeCharacteristic(char)
        } ?: run {
            Log.w(TAG, "Characteristic not available, cannot send data.")
        }
    }

    // --- Helper & Cleanup Methods ---
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun stopScan() {
        handler.removeCallbacksAndMessages(null)
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}