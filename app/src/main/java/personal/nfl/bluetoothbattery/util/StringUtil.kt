package personal.nfl.bluetoothbattery.util


class StringUtil {
    companion object {
        fun bytesToHexString(bytes: ByteArray): String? {
            val stringBuilder = StringBuilder()
            if (bytes.isEmpty()) {
                return null
            }
            for (element in bytes) {
                val v = element.toInt() and 0xFF
                val hv = Integer.toHexString(v)
                if (hv.length < 2) {
                    stringBuilder.append(0)
                }
                stringBuilder.append(hv)
            }
            return stringBuilder.toString()
        }
    }
}