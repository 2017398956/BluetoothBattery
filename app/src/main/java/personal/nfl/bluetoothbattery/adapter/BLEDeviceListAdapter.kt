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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class BLEDeviceListAdapter(bluetoothAdapter: BluetoothAdapter) :
    RecyclerView.Adapter<BLEDeviceListAdapter.MyViewHolder>() {

    private val data = mutableListOf<BluetoothDevice>()
    private lateinit var context: Context
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter = bluetoothAdapter
    private val connectClassicBluetooth = true

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
                try {
                    val method = BluetoothDevice::class.java.getDeclaredMethod(
                        "getMetadata",
                        Int::class.java
                    )
                    method.isAccessible = true
                    for (i in 0 until 20) {
                        val bytes: ByteArray = method.invoke(data[position], 0) as ByteArray
                        Log.i("NFL", "getMetadata($i):${String(bytes)}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val autoConnect = true
                val device = bluetoothAdapter.getRemoteDevice(data[position].address)
                if (device == null) {
                    Toast.makeText(context, "获取蓝牙失败", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (connectClassicBluetooth) {
                    connectClassicBluetoothDevice(device)
                } else {
                    bluetoothGatt?.disconnect()
                    bluetoothGatt = device.connectGatt(
                        context, autoConnect,
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
                                        val service =
                                            gatt?.getService(UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb"))
                                        Log.i("NFL", "getService:$service")
                                        if (gatt?.discoverServices() == true) {
                                            gatt?.services?.forEach {
                                                Log.i("NFL", "service:${it.uuid}")
                                            }
                                            gatt?.let {
                                                // getBattery(it)
                                            }
                                        } else {
                                            Log.i("NFL", "discoverServices failed.")
                                        }
                                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                        Log.i("NFL", "断开连接")
                                        gatt?.disconnect()
                                        gatt?.close()
                                    }
                                } else {
                                    Log.i("NFL", "连接失败")
                                    gatt?.close()
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
                        }, BluetoothDevice.TRANSPORT_LE
                    )
                    if (!autoConnect) {
                        bluetoothGatt?.connect()
                    }
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
        devices.forEach {
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

    private enum class BluetoothState {
        STATE_CONNECTING, STATE_CONNECTED
    }

    private var mState: BluetoothState? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mConnectThread: ConnectThread? = null

    private fun getBattery2(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getDeclaredMethod("getMetadata", Int.javaClass)
            method.isAccessible = true
            val batteryLevel = method.invoke(
                device,
                18
            )
            Log.i("NFL", "battery level:$batteryLevel")
        } catch (e: Exception) {
            Log.i("NFL", "get battery level failed.")
            getBattery3(device)
        }
    }

    private fun getBattery3(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getDeclaredMethod("getBatteryLevel")
            method.isAccessible = true
            val batteryLevel = method.invoke(device)
            Log.i("NFL", "battery level:$batteryLevel")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectClassicBluetoothDevice(device: BluetoothDevice) {
        getBattery2(device)
        //  扫描是比较耗费资源的,在进行连接的时候要先将扫描停掉
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        //如果设备还没有绑定过，要先绑定设备,绑定成功后会收到绑定成功的通知，然后我们再次调用这个连接方法
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            device.createBond()
            return
        }
        if (mState == BluetoothState.STATE_CONNECTING || mState == BluetoothState.STATE_CONNECTED) return
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        /**
         *
         */
        mConnectThread = ConnectThread(device)
        mConnectThread!!.start()
    }

//---------------------------------------------------------------------------------------------
    /**
     * 执行连接操作的线程
     *    注：连接会阻塞，需要在子线程中执行
     */
    private inner class ConnectThread
    @SuppressLint("MissingPermission") constructor(device: BluetoothDevice) : Thread() {
        private var mSocket: BluetoothSocket? = null
        private val currentDevice = device

        init {
            try {
                var SPP_UUID = UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb")
                SPP_UUID = UUID.fromString("0000110c-0000-1000-8000-00805f9b34fb")
                SPP_UUID = UUID.fromString("0000110d-0000-1000-8000-00805f9b34fb")
                SPP_UUID = UUID.fromString("0000110e-0000-1000-8000-00805f9b34fb")
                // receive data:41542b425253463d3534330d
                SPP_UUID = UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb")
//                SPP_UUID = UUID.fromString("00001124-0000-1000-8000-00805f9b34fb")
//                SPP_UUID = UUID.fromString("00001200-0000-1000-8000-00805f9b34fb")
//                SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                // 加密传输，Android 强制执行配对，弹窗显示输入配对码，推荐使用这种
//                SPP_UUID = device.uuids[0].uuid
//                0000110b-0000-1000-8000-00805f9b34fb 音频输出
//                0000110c-0000-1000-8000-00805f9b34fb 遥控目标
//                0000110d-0000-1000-8000-00805f9b34fb 高级音频
//                0000110e-0000-1000-8000-00805f9b34fb 遥控
//                0000111e-0000-1000-8000-00805f9b34fb 免提
//                00001124-0000-1000-8000-00805f9b34fb 输入设备服务 (HID)
//                00001200-0000-1000-8000-00805f9b34fb 即插即用信息
                mSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                /**
                 * temp = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                 * 明文传输(不安全)，无需配对，通常使用于蓝牙2.1设备，因为对于蓝牙2.1设备,
                 * 如果有任何设备没有具有输入和输出能力或显示数字键，无法进行安全套接字连接
                 */
            } catch (e: IOException) {
                Log.e("NFL", "create socket failed:${e.message}")
                e.printStackTrace()
            }
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                mSocket?.let {
                    // 建立socket连接
                    it.connect()
                }
            } catch (e: Exception) {
                Log.e("NFL", "连接异常：${e.message}")
                // 针对连接异常的处理
                try {
                    currentDevice.javaClass.getDeclaredMethod("createRfcommSocket", Int.javaClass)
                        ?.let {
                            it.isAccessible = true
                            mSocket = it.invoke(currentDevice, 1) as BluetoothSocket
                            mSocket!!.connect()
                        }
                } catch (e2: Exception) {
                    try {
                        mSocket?.close()
                    } catch (e1: IOException) {
                        e1.printStackTrace()
                    }
                    return
                }
            }
            mSocket?.let {
                mConnectedThread = ConnectedThread(it)
                mConnectedThread!!.start()
            }
        }

        fun cancel() {
            mConnectedThread?.cancel()
            try {
                mSocket?.close()
            } catch (e: IOException) {
            }
        }
    }

//---------------------------------------------------------------------------------------------
    /**
     * 	进行 发送/接收 数据的线程
     *    注：连接会阻塞，需要在子线程中执行
     */
    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private var isSending = false// 是否正在发送数据
        private var mSocket: BluetoothSocket = socket
        private var mInStream: InputStream? = null
        private var mOutStream: OutputStream? = null
        private var destroy = false

        init {
            try {
                // 获取输入输出流
                mInStream = socket.inputStream
                mOutStream = socket.outputStream
            } catch (e: IOException) {
            }
            // 到这里可以认为已经成功的建立了连接
            mState = BluetoothState.STATE_CONNECTED
        }

        override fun run() {
            var num = 0 // 从流中读取到的有效字节数
            // 用一个1024字节的数据接收数据，可根据需要调整大小
            val buffer = ByteArray(1024)
            var buffer_new: ByteArray? = null //实际收到的数据
            Log.i("NFL", "connect bluetooth success.")
            // 对输入流保持监听直至一个异常发生
            while (!destroy) {
                try {
                    num = mInStream?.read(buffer) ?: 0
                    buffer_new = ByteArray(num)
                    //截取有效数据
                    System.arraycopy(buffer, 0, buffer_new, 0, num)
                    Log.i("NFL", "receive data:${StringUtil.bytesToHexString(buffer_new)}")
                } catch (e: IOException) {
                    Log.e("NFL", "receive data failed:${e.message}")
                }
            }
        }

        /**
         * 向蓝牙模块发射数据
         * @param bytes
         */
        fun write(bytes: ByteArray) {
            Log.i("NFL", "write()发送内容：${StringUtil.bytesToHexString(bytes)}")
            if (isSending || bytes.isEmpty()) return
            isSending = true
            try {
                mOutStream?.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            isSending = false
        }

        fun cancel() {
            destroy = true
            try {
                mSocket.close()
            } catch (e: IOException) {
            }
        }
    }

}