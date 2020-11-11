package powerdancer.dsp

object HexUtils {
    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun byteToHex(b: Byte): String {
        val hexChars = CharArray(2)
        val v = b.toInt()
        hexChars[0] = hexArray[v and 0xF0 shr 4]
        hexChars[1] = hexArray[v and 0x0F]
        return String(hexChars)
    }

    fun bytesToHex(vararg bytes: Byte): String {
        val hexChars = CharArray(2 * bytes.size)
        bytes.forEachIndexed { i, b ->
            val v = b.toInt()
            hexChars[i * 2] = hexArray[v and 0xF0 shr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}