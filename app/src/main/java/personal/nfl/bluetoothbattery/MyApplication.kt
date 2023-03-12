package personal.nfl.bluetoothbattery

import android.app.Application
import personal.nfl.permission.support.util.AbcPermission

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AbcPermission.install(this)
    }
}