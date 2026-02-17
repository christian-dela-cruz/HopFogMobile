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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Scanning : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

interface BleTransactionCallback {
    fun onTransactionSuccess(response: String)
    fun onTransactionFailure(error: String)
}

@SuppressLint("MissingPermission")
object BleManager {

    private const val TAG = "BleManager"
    private val HOPFOG_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val MESSAGE_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    private var bleGatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val status = _status.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private var transactionCallback: BleTransactionCallback? = null
    private var transactionJson: String? = null

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = manager.adapter
            Log.d(TAG, "BleManager Initialized.")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "GATT Connected. Discovering services...")
                        bleGatt = gatt
                        _status.value = ConnectionStatus.Connecting
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "GATT Disconnected.")
                        // If a login was in progress and we got disconnected, it failed.
                        transactionCallback?.onTransactionFailure("Disconnected unexpectedly.")
                        transactionCallback = null

                        _status.value = ConnectionStatus.Disconnected
                        bleGatt?.close()
                        bleGatt = null
                    }
                } else {
                    transactionCallback?.onTransactionFailure("Connection failed with status: $status")
                    transactionCallback = null
                    _status.value = ConnectionStatus.Error("Connection failed with status: $status")
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            handler.post {
                val service = gatt.getService(HOPFOG_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID)
                if (characteristic == null) {
                    transactionCallback?.onTransactionFailure("Characteristic not found.")
                    transactionCallback = null
                    gatt.disconnect()
                    return@post
                }
                messageCharacteristic = characteristic
                enableNotifications(gatt, characteristic)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            handler.post {
                // Connection is now fully ready for reads/writes
                _status.value = ConnectionStatus.Connected
                Log.d(TAG, "Connection fully established.")

                // If this connection was for a login transaction, send the login JSON now
                if (transactionCallback != null) {
                    messageCharacteristic?.let { char ->
                        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        char.value = transactionJson?.toByteArray(Charsets.UTF_8)
                        gatt.writeCharacteristic(char)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handler.post {
                val response = characteristic.value.toString(Charsets.UTF_8)
                // If a transaction callback is waiting, this response is for it
                if (transactionCallback != null) {
                    if (response.contains("Login OK", ignoreCase = true)) {
                        transactionCallback?.onTransactionSuccess(response)
                    } else {
                        transactionCallback?.onTransactionFailure(response)
                    }
                    // CRITICAL FIX: We consume the callback but DO NOT disconnect.
                    // The connection is handed off to become the persistent session.
                    transactionCallback = null
                    transactionJson = null
                } else {
                    // This is a regular message for the chat pages
                    _incomingMessages.tryEmit(response)
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.scanRecord?.serviceUuids?.any { it.uuid == HOPFOG_SERVICE_UUID } == true) {
                stopScan()
                Log.d(TAG, "Hub found, connecting...")
                result.device.connectGatt(appContext, false, gattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
                _status.value = ConnectionStatus.Error("Scan failed: $errorCode")
                transactionCallback?.onTransactionFailure("Scan failed: $errorCode")
                transactionCallback = null
            }
        }
    }

    fun performLoginTransaction(json: String, callback: BleTransactionCallback) {
        if (_status.value == ConnectionStatus.Connected) {
            return callback.onTransactionFailure("Already connected.")
        }
        transactionCallback = callback
        transactionJson = json
        startScan()
    }

    // Connect function is now just an alias for starting a scan
    fun connect() {
        if (_status.value == ConnectionStatus.Disconnected) {
            startScan()
        }
    }

    fun disconnect() {
        bleGatt?.disconnect()
    }

    fun sendJson(jsonString: String) {
        if (_status.value != ConnectionStatus.Connected) {
            Log.w(TAG, "Cannot send, not connected.")
            return
        }
        messageCharacteristic?.let { char ->
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            char.value = jsonString.toByteArray(Charsets.UTF_8)
            bleGatt?.writeCharacteristic(char)
        }
    }

    private fun startScan() {
        _status.value = ConnectionStatus.Scanning
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
        handler.postDelayed({ stopScan() }, 10000L)
    }

    private fun stopScan() {
        if (_status.value == ConnectionStatus.Scanning) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    fun hasPermissions(context: Context): Boolean {
        // ... (implementation is correct)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}