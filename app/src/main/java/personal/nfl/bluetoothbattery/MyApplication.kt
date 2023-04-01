package personal.nfl.bluetoothbattery

import android.app.Application
import android.content.Context
import me.weishu.reflection.Reflection
import personal.nfl.permission.support.util.AbcPermission

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AbcPermission.install(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }
}