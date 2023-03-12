package personal.nfl.bluetoothbattery.adapter

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.coroutines.*
import personal.nfl.bluetoothbattery.databinding.ItemBleDeviceBinding
import personal.nfl.bluetoothbattery.util.StringUtil
import java.util.*

class BLEDeviceListAdapter(bluetoothAdapter: BluetoothAdapter) : RecyclerView.Adapter<BLEDeviceListAdapter.MyViewHolder>() {

    private val data = mutableListOf<BluetoothDevice>()
    private lateinit var context: Context
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter = bluetoothAdapter

    class MyViewHolder(binding: ItemBleDeviceBinding) : ViewHolder(binding.root) {
        val binding = binding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        if (!this::context.isInitialized) {
            context = parent.context
        }
        return MyViewHolder(
            ItemBleDeviceBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.tvName.text = data[position].name
        if (BluetoothDevice.BOND_BONDED == data[position].bondState) {
            holder.binding.tvName.setTextColor(Color.RED)
        } else {
            holder.binding.tvName.setTextColor(Color.WHITE)
        }
        holder.binding.root.setOnClickListener {
            if ("Bluetooth music" == data[position].name) {
                Log.i("NFL", "start Connect Bluetooth music")
                val autoConnect = true
                val device = bluetoothAdapter.getRemoteDevice(data[position].address)
                if (device == null){
                    Toast.makeText(context, "获取蓝牙失败", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                bluetoothGatt = device.connectGatt(context, autoConnect,
                    object : BluetoothGattCallback() {

                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt?,
                            status: Int,
                            newState: Int
                        ) {
                            super.onConnectionStateChange(gatt, status, newState)
                            Log.i("NFL", "onConnectionStateChange:$status, $newState")
                            Log.i(
                                "NFL", "onConnectionStateChange:hex:${
                                    StringUtil.bytesToHexString(
                                        byteArrayOf(status.toByte())
                                    )
                                }, ${StringUtil.bytesToHexString(byteArrayOf(newState.toByte()))}"
                            )
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    // 连接成功，调用发现服务的方法
                                    val service = gatt?.getService(UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb"))
                                    Log.i("NFL", "getService:$service")
                                    if(gatt?.discoverServices() == true){
                                        gatt?.services?.forEach {
                                            Log.i("NFL", "service:${it.uuid}")
                                        }
                                        gatt?.let {
                                            // getBattery(it)
                                        }
                                    }else{
                                        Log.i("NFL", "discoverServices failed.")
                                    }
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    Log.i("NFL", "断开连接")
                                    gatt?.disconnect()
                                    gatt?.close()
                                }
                            } else {
                                Log.i("NFL", "连接失败");
                                gatt?.close();
                            }
                        }

                        override fun onServiceChanged(gatt: BluetoothGatt) {
                            super.onServiceChanged(gatt)
                            Log.i("NFL", "onServiceChanged")
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            super.onServicesDiscovered(gatt, status)
                            Log.i("NFL", "onServicesDiscovered:$status")
                        }

                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt?,
                            characteristic: BluetoothGattCharacteristic?,
                            status: Int
                        ) {
                            super.onCharacteristicRead(gatt, characteristic, status)
                            Log.i(
                                "NFL", "read:${
                                    characteristic?.getIntValue(
                                        BluetoothGattCharacteristic.FORMAT_UINT8,
                                        0
                                    )
                                }"
                            )
                        }
                    }, BluetoothDevice.TRANSPORT_BREDR)

                if (!autoConnect){
                    bluetoothGatt?.connect()
                }
            }
        }
    }

    fun addDevice(device: BluetoothDevice) {
        for (temp in data) {
            if (temp.address == device.address) {
                return
            }
        }
        data.add(device)
    }

    fun addDevices(devices: List<BluetoothDevice>) {
        devices?.forEach {
            addDevice(it)
        }
    }

    fun clearDevices() {
        data.clear()
    }

    private val Battery_Service_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    private val Battery_Level_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun getBattery(bluetoothGatt: BluetoothGatt) {
        Log.i("NFL", "gatt:$bluetoothGatt")
        val batteryService: BluetoothGattService = bluetoothGatt.getService(Battery_Service_UUID)
        if (batteryService == null) {
            Log.d("NFL", "Battery service not found!")
            return
        }
        val batteryLevel = batteryService.getCharacteristic(Battery_Level_UUID)
        if (batteryLevel == null) {
            Log.d("NFL", "Battery level not found!")
            return
        }
        bluetoothGatt.readCharacteristic(batteryLevel)
        Log.v("NFL", "batteryLevel = " + bluetoothGatt.readCharacteristic(batteryLevel))
    }
}