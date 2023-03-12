package personal.nfl.bluetoothbattery.broadcastreceiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import personal.nfl.bluetoothbattery.adapter.BLEDeviceListAdapter

class BluetoothBroadcastReceiver(private val bluetoothAdapter: BLEDeviceListAdapter) :
    BroadcastReceiver() {
    private val names: MutableList<String> = ArrayList()

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent == null) return
        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let {
            Log.e("NFL", "getAddress: " + it.address + " name: " + it.name)
            if (!TextUtils.isEmpty(it.name)) {
                names.add(it.name)
                bluetoothAdapter.addDevice(it)
                bluetoothAdapter.notifyDataSetChanged()
            }
        }

        if (TextUtils.equals(intent.action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            if (names.size == 0) {
                Toast.makeText(context, "未发现可连接的蓝牙设备", Toast.LENGTH_SHORT).show()
            }
        }
    }
}