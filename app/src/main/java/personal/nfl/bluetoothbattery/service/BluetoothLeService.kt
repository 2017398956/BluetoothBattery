package personal.nfl.bluetoothbattery.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

private val TAG = BluetoothLeService::class.java.simpleName
private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
val UUID_HEART_RATE_MEASUREMENT = UUID.fromString("SampleGattAttributes.HEART_RATE_MEASUREMENT")
class BluetoothLeService(private var bluetoothGatt: BluetoothGatt?) : Service() {

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    private val Battery_Service_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    private val Battery_Level_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private var connectionState = STATE_DISCONNECTED
    // Various callback methods defined by the BLE API.
//    private val gattCallback = object : BluetoothGattCallback() {
//        @SuppressLint("MissingPermission")
//        override fun onConnectionStateChange(
//            gatt: BluetoothGatt,
//            status: Int,
//            newState: Int
//        ) {
//            val intentAction: String
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    intentAction = ACTION_GATT_CONNECTED
//                    connectionState = STATE_CONNECTED
//                    broadcastUpdate(intentAction)
//                    Log.i(TAG, "Connected to GATT server.")
//                    // Log.i(TAG, "Attempting to start service discovery: " + bluetoothGatt?.discoverServices())
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    intentAction = ACTION_GATT_DISCONNECTED
//                    connectionState = STATE_DISCONNECTED
//                    Log.i(TAG, "Disconnected from GATT server.")
//                    broadcastUpdate(intentAction)
//                }
//            }
//        }
//
//        // New services discovered
//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            when (status) {
//                BluetoothGatt.GATT_SUCCESS -> broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
//                else -> Log.w(TAG, "onServicesDiscovered received: $status")
//            }
//        }
//
//        // Result of a characteristic read operation
//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            when (status) {
//                BluetoothGatt.GATT_SUCCESS -> {
//                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//                }
//            }
//        }
//    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    fun onCharacteristicRead(
//        gatt: BluetoothGatt?,
//        characteristic: BluetoothGattCharacteristic,
//        status: Int
//    ) {
//        if (status == BluetoothGatt.GATT_SUCCESS) {
////            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//        }
//    }
//
//
//    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
//        val intent = Intent(action)
//        Log.v(
//            TAG,
//            "characteristic.getStringValue(0) = " + characteristic.getIntValue(
//                BluetoothGattCharacteristic.FORMAT_UINT8,
//                0
//            )
//        )
//        intent.putExtra(
//            DeviceControl.EXTRAS_DEVICE_BATTERY,
//            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
//        )
//        sendBroadcast(intent)
//    }
//
//    fun getBattery() {
//        val batteryService: BluetoothGattService = mBluetoothGatt.getService(Battery_Service_UUID)
//        if (batteryService == null) {
//            Log.d(TAG, "Battery service not found!")
//            return
//        }
//        val batteryLevel = batteryService.getCharacteristic(Battery_Level_UUID)
//        if (batteryLevel == null) {
//            Log.d(TAG, "Battery level not found!")
//            return
//        }
//        mBluetoothGatt.readCharacteristic(batteryLevel)
//        Log.v(TAG, "batteryLevel = " + mBluetoothGatt.readCharacteristic(batteryLevel))
//    }
}