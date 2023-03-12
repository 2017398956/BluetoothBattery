package personal.nfl.bluetoothbattery

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import personal.nfl.bluetoothbattery.adapter.BLEDeviceListAdapter
import personal.nfl.bluetoothbattery.broadcastreceiver.BluetoothBroadcastReceiver
import personal.nfl.bluetoothbattery.databinding.ActivityMainBinding
import personal.nfl.bluetoothbattery.util.StringUtil
import personal.nfl.permission.annotation.GetPermissions4AndroidX


private const val SCAN_PERIOD: Long = 10000

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)
    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }
    private lateinit var bleDeviceListAdapter : BLEDeviceListAdapter
    private lateinit var bluetoothBroadcastReceiver: BluetoothBroadcastReceiver

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                device.bondState
                Log.i("NFL", "device.address:${device.address}, ${device.name}")
                if (!TextUtils.isEmpty(device.name)) {
                    runOnUiThread {
                        bleDeviceListAdapter.addDevice(device)
                        bleDeviceListAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    // Device scan callback.
    @SuppressLint("MissingPermission")
    private val mLeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            if (!TextUtils.isEmpty(device.name)) {
                runOnUiThread {
                    bleDeviceListAdapter.addDevice(device)
                    bleDeviceListAdapter.notifyDataSetChanged()
                }
            }
            try {
                val strTemp = StringUtil.bytesToHexString(scanRecord)
                Log.i("NFL", "${strTemp?.length} = 长度，回调方法 -onLeScan()=" + strTemp)
                //0201061aff4c000215fda50693a4e24fb1afcfc6eb07647825000a0007c5090942523531343633310b1642523d6400cdff003ab100000000000000000000
                if (!TextUtils.isEmpty(strTemp)) {
                    val usefulData = strTemp!!.substring(86, 106) //4252 3d6400cdff003a9e
                    Log.i(
                        "NFL",
                        "${usefulData.length}= 长度，回调方法 -onLeScan() 截取有用数据 usefulData=" + usefulData
                    )
                    // String serviceID1 = usefulData.substring(2)
                    // String serviceID = usefulData.substring(2, 4)
                    // if (serviceID.equals("52")) {
                    val strVersion = usefulData.substring(4, 6)
                    val intVersion = Integer.parseInt(strVersion, 16)
                    val strPower = usefulData.substring(6, 8)
                    val intPower = Integer.parseInt(strPower, 16)
                    Log.i(
                        "NFL",
                        "$strVersion=strVersion 回调方法 -onLeScan() ，版本值 intVersion=$intVersion"
                    )
                    Log.i("NFL", "$strPower=strPower 回调方法 -onLeScan() ，电量值 intPower=$intPower")
                }
            } catch (e: Exception) {
                Log.i("NFL", " 解析蓝牙数据出错 =$e")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bleDeviceListAdapter = BLEDeviceListAdapter(bluetoothAdapter!!)
        binding.rvDevices.adapter = bleDeviceListAdapter
        obtainPermissions()
        bluetoothBroadcastReceiver = BluetoothBroadcastReceiver(bleDeviceListAdapter)
        registerReceiver()
        isLocationOpen(this)
        useBluetooth()
        binding.srlRefresh.setOnRefreshListener {
            bleDeviceListAdapter.clearDevices()
            bleDeviceListAdapter.notifyDataSetChanged()
            scanBLEDevice()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
//        if (bluetoothAdapter?.isDisabled == true){
//            bluetoothAdapter?.enable()
//        }
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(bluetoothBroadcastReceiver, intentFilter)
    }

    private fun isLocationOpen(context: Context): Boolean {
        val manager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        // gps定位
        val isGpsProvider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.i("NFL", "isGpsProvider:$isGpsProvider")
        // 网络定位
        val isNetWorkProvider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.i("NFL", "isNetWorkProvider:$isNetWorkProvider")
        return isGpsProvider || isNetWorkProvider
    }

    @SuppressLint("MissingPermission")
    private fun useBluetooth() {
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.also {
                Toast.makeText(this, "no ble", Toast.LENGTH_SHORT).show()
                finish()
            }
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            registerForActivityResult(
                StartActivityForResult()
            ) {
                Log.i("NFL", "start bluetooth ${it.resultCode}")
            }.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        scanBLEDevice()
    }

    private enum class SCAN_METHOD {
        SCAN_METHOD_NONE,SCAN_METHOD_CLASSIC, SCAN_METHOD_BLE, SCAN_METHOD_BLE_NEW, SCAN_METHOD_BLE_CUSTOM
    }

    private val scanMethod = SCAN_METHOD.SCAN_METHOD_BLE_NEW

    @SuppressLint("MissingPermission")
    private fun scanBLEDevice() {
        if (bluetoothAdapter?.isDiscovering == true && bluetoothAdapter?.isDisabled == true) {
            return
        }
        binding.srlRefresh.isRefreshing = true
        //        val devices = bluetoothManager.getDevicesMatchingConnectionStates(BluetoothProfile.GATT_SERVER,
//            intArrayOf(BluetoothProfile.STATE_CONNECTED,BluetoothProfile.STATE_CONNECTING,
//                BluetoothProfile.STATE_DISCONNECTED,BluetoothProfile.STATE_DISCONNECTING)
//        )
        val devices = bluetoothAdapter?.bondedDevices
        devices?.forEach {
            Log.i(
                "NFL",
                "state:${
                    bluetoothManager.getConnectionState(
                        it,
                        BluetoothProfile.GATT_SERVER
                    )
                },name:${it.name},uuids:${it.uuids.toList()}"
            )
        }
        devices?.let {
            bleDeviceListAdapter.addDevices(it.toList())
        }
        handler.postDelayed({
            cancelScanBluetooth()
        }, SCAN_PERIOD)
        when (scanMethod) {
            SCAN_METHOD.SCAN_METHOD_CLASSIC -> {
                bluetoothAdapter!!.startDiscovery()
            }
            SCAN_METHOD.SCAN_METHOD_BLE -> {
                bluetoothAdapter!!.startLeScan(mLeScanCallback)
            }
            SCAN_METHOD.SCAN_METHOD_BLE_NEW -> {
                bluetoothAdapter!!.bluetoothLeScanner.startScan(scanCallback)
            }
            SCAN_METHOD.SCAN_METHOD_BLE_CUSTOM -> {
                val onlyMyBluetooth = ScanFilter.Builder().setDeviceName("Bluetooth music").build()
                bluetoothAdapter!!.bluetoothLeScanner.startScan(
                    arrayListOf<ScanFilter>(onlyMyBluetooth),
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC)
                        .build(),
                    scanCallback
                )
            }
            else -> {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanBluetooth() {
        binding.srlRefresh.isRefreshing = false
        when (scanMethod) {
            SCAN_METHOD.SCAN_METHOD_CLASSIC -> {
                bluetoothAdapter!!.cancelDiscovery()
            }
            SCAN_METHOD.SCAN_METHOD_BLE -> {
                bluetoothAdapter!!.stopLeScan(mLeScanCallback)
            }
            SCAN_METHOD.SCAN_METHOD_BLE_NEW, SCAN_METHOD.SCAN_METHOD_BLE_CUSTOM -> {
                bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
            }
            else -> {}
        }
    }

    @GetPermissions4AndroidX(
        Manifest.permission.BLUETOOTH,
//        Manifest.permission.BLUETOOTH_CONNECT,
//        Manifest.permission.BLUETOOTH_SCAN,
//        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private fun obtainPermissions() {
    }

    override fun onDestroy() {
        if (bluetoothBroadcastReceiver != null) {
            unregisterReceiver(bluetoothBroadcastReceiver)
        }
        super.onDestroy()
    }
}