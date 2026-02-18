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
    object Ready : ConnectionStatus() // <-- THIS IS THE CORRECT STATE
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
                        Log.d(TAG, "GATT Connected. Requesting MTU...")
                        _status.value = ConnectionStatus.Connecting
                        bleGatt = gatt
                        gatt.requestMtu(512)
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "GATT Disconnected.")
                        transactionCallback?.onTransactionFailure("Disconnected.")
                        transactionCallback = null
                        _status.value = ConnectionStatus.Disconnected
                        bleGatt?.close()
                        bleGatt = null
                    }
                } else {
                    Log.e(TAG, "onConnectionStateChange failed with status: $status")
                    transactionCallback?.onTransactionFailure("Connection failed.")
                    transactionCallback = null
                    _status.value = ConnectionStatus.Error("Connection failed")
                    gatt.close()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "MTU changed to $mtu. Discovering services.")
                    gatt.discoverServices()
                } else {
                    Log.w(TAG, "MTU change failed. Disconnecting.")
                    transactionCallback?.onTransactionFailure("MTU negotiation failed.")
                    gatt.disconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered. Enabling notifications.")
                    val service = gatt.getService(HOPFOG_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID)
                    if (characteristic == null) {
                        transactionCallback?.onTransactionFailure("HopFog characteristic not found.")
                        gatt.disconnect()
                        return@post
                    }
                    messageCharacteristic = characteristic
                    enableNotifications(gatt, characteristic)
                } else {
                    Log.w(TAG, "onServicesDiscovered failed with status: $status")
                    transactionCallback?.onTransactionFailure("Service discovery failed.")
                    gatt.disconnect()
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Notifications enabled. Connection is now ready.")
                    _status.value = ConnectionStatus.Ready // <-- THE CORRECT STATE
                    if (transactionCallback != null && transactionJson != null) {
                        sendJson(transactionJson!!)
                    }
                } else {
                    Log.e(TAG, "Descriptor write failed. Disconnecting.")
                    transactionCallback?.onTransactionFailure("Failed to enable notifications.")
                    gatt.disconnect()
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handler.post {
                val response = value.toString(Charsets.UTF_8)
                Log.d(TAG, "Received data: $response")
                if (transactionCallback != null) {
                    if (response.contains("login", ignoreCase = true) && response.contains("ok", ignoreCase = true)) {
                        transactionCallback?.onTransactionSuccess(response)
                    } else {
                        transactionCallback?.onTransactionFailure(response)
                    }
                    transactionCallback = null
                    transactionJson = null
                } else {
                    _incomingMessages.tryEmit(response)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.scanRecord?.serviceUuids?.any { it.uuid == HOPFOG_SERVICE_UUID } == true) {
                stopScan()
                Log.d(TAG, "Hub found, connecting...")
                _status.value = ConnectionStatus.Connecting
                result.device.connectGatt(appContext, false, gattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
                Log.e(TAG, "Scan failed with error code: $errorCode")
                _status.value = ConnectionStatus.Error("Scan failed")
                transactionCallback?.onTransactionFailure("Scan failed.")
                transactionCallback = null
            }
        }
    }

    fun performLoginTransaction(json: String, callback: BleTransactionCallback) {
        if (_status.value !is ConnectionStatus.Disconnected) {
            return callback.onTransactionFailure("A connection is already active.")
        }
        transactionCallback = callback
        transactionJson = json
        startScan()
    }

    fun connect() {
        if (_status.value is ConnectionStatus.Disconnected) {
            startScan()
        }
    }

    fun disconnect() {
        bleGatt?.disconnect()
    }

    fun sendJson(jsonString: String) {
        if (_status.value != ConnectionStatus.Ready) { // <-- CHECK FOR READY
            Log.w(TAG, "Cannot send, connection not fully ready.")
            return
        }
        messageCharacteristic?.let { char ->
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            char.value = jsonString.toByteArray(Charsets.UTF_8)
            bleGatt?.writeCharacteristic(char)
        } ?: Log.e(TAG, "Cannot send, characteristic is null.")
    }

    private fun startScan() {
        if (!hasPermissions(appContext!!)) {
            transactionCallback?.onTransactionFailure("Bluetooth permissions not granted.")
            _status.value = ConnectionStatus.Error("Permissions missing.")
            return
        }
        _status.value = ConnectionStatus.Scanning
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
        handler.postDelayed({ stopScan(didFindDevice = false) }, 10000L)
    }

    private fun stopScan(didFindDevice: Boolean = true) {
        if (_status.value == ConnectionStatus.Scanning) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            if (!didFindDevice && transactionCallback != null) {
                _status.value = ConnectionStatus.Error("Hub not found.")
                transactionCallback?.onTransactionFailure("Hub not found.")
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
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