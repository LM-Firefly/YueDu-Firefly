package io.legado.app.utils

import android.util.Base64
import java.io.InputStream
import java.security.MessageDigest

/**
 * 将字符串转化为MD5
 */
@Suppress("unused")
object MD5Utils {

    private fun md5Hash(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(data)
    }

    private fun md5HexString(data: ByteArray): String {
        val hash = md5Hash(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun md5Encode(str: String?): String {
        return if (str == null) "" else md5HexString(str.toByteArray())
    }

    fun md5Encode(inputStream: InputStream): String {
        val buffer = ByteArray(4096)
        val digest = MessageDigest.getInstance("MD5")
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hash = digest.digest()
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun md5Encode16(str: String): String {
        var reStr = md5Encode(str)
        reStr = reStr.substring(8, 24)
        return reStr
    }
}
