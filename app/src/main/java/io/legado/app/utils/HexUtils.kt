package io.legado.app.utils

import java.nio.charset.Charset

object HexUtils {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    fun encodeHexStr(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out[i++] = HEX_CHARS[v ushr 4]
            out[i++] = HEX_CHARS[v and 0x0F]
        }
        return String(out)
    }

    fun encodeHexStr(text: String, charset: Charset = Charsets.UTF_8): String {
        return encodeHexStr(text.toByteArray(charset))
    }

    fun decodeHex(hex: String): ByteArray {
        val clean = hex.trim()
        if (clean.length % 2 != 0) return ByteArray(0)
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            val hi = Character.digit(clean[i], 16)
            val lo = Character.digit(clean[i + 1], 16)
            if (hi < 0 || lo < 0) return ByteArray(0)
            out[i / 2] = ((hi shl 4) + lo).toByte()
            i += 2
        }
        return out
    }

    fun decodeHexStr(hex: String, charset: Charset = Charsets.UTF_8): String {
        return decodeHex(hex).toString(charset)
    }
}
