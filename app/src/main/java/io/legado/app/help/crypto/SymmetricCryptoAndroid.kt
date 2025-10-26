package io.legado.app.help.crypto

import androidx.annotation.Keep
import cn.hutool.core.codec.Base64
import cn.hutool.core.util.HexUtil
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.isHex
import java.io.InputStream
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 对称加解密，使用 Android 原生 javax.crypto，不依赖 hutool 的 BouncyCastle 提供程序。
 */
@Keep
class SymmetricCryptoAndroid(
    private val transformation: String,
    key: ByteArray?,
) {
    private val secretKey: SecretKey
    private var iv: ByteArray? = null

    init {
        val algorithm = transformation.split("/").first().let { normalizeAlgorithm(it) }
        secretKey = generateSecretKey(algorithm, key)
    }

    private fun normalizeAlgorithm(algorithm: String): String {
        return when (algorithm.uppercase()) {
            "3DES" -> "DESede"
            else -> algorithm
        }
    }

    private fun generateSecretKey(algorithm: String, key: ByteArray?): SecretKey {
        val upperAlgo = algorithm.uppercase()
        return when {
            key == null -> KeyGenerator.getInstance(algorithm).generateKey()
            upperAlgo == "DES" -> {
                val keyBytes = if (key.size >= 8) key.copyOf(8) else key.copyOf(8).also { key.copyInto(it) }
                SecretKeyFactory.getInstance("DES").generateSecret(DESKeySpec(keyBytes))
            }
            upperAlgo == "DESEDE" -> {
                val keyBytes = when {
                    key.size >= 24 -> key.copyOf(24)
                    key.size >= 16 -> key.copyOf(16)
                    else -> {
                        // 不足 24 字节时循环填充
                        ByteArray(24).also { buf ->
                            for (i in buf.indices) buf[i] = key[i % key.size]
                        }
                    }
                }
                SecretKeyFactory.getInstance("DESede").generateSecret(DESedeKeySpec(keyBytes))
            }
            else -> SecretKeySpec(key, algorithm)
        }
    }

    fun setIv(iv: ByteArray): SymmetricCryptoAndroid {
        this.iv = iv
        return this
    }

    private fun createCipher(mode: Int): Cipher {
        val cipher = Cipher.getInstance(transformation)
        val ivBytes = this.iv
        if (ivBytes != null && ivBytes.isNotEmpty()) {
            cipher.init(mode, secretKey, IvParameterSpec(ivBytes))
        } else {
            cipher.init(mode, secretKey)
        }
        return cipher
    }

    fun encrypt(data: ByteArray): ByteArray = createCipher(Cipher.ENCRYPT_MODE).doFinal(data)

    fun encrypt(data: String, charset: String?): ByteArray =
        encrypt(data.toByteArray(if (charset != null) Charset.forName(charset) else Charsets.UTF_8))

    fun encrypt(data: String, charset: Charset?): ByteArray =
        encrypt(data.toByteArray(charset ?: Charsets.UTF_8))

    fun encrypt(data: String): ByteArray = encrypt(data.toByteArray(Charsets.UTF_8))

    fun encrypt(data: InputStream): ByteArray = encrypt(data.readBytes())

    fun encryptBase64(data: ByteArray): String = EncoderUtils.base64Encode(encrypt(data))

    fun encryptBase64(data: String, charset: String?): String =
        EncoderUtils.base64Encode(encrypt(data, charset))

    fun encryptBase64(data: String, charset: Charset?): String =
        EncoderUtils.base64Encode(encrypt(data, charset))

    fun encryptBase64(data: String): String = EncoderUtils.base64Encode(encrypt(data))

    fun encryptBase64(data: InputStream): String = EncoderUtils.base64Encode(encrypt(data))

    fun encryptHex(data: ByteArray): String = HexUtil.encodeHexStr(encrypt(data))

    fun encryptHex(data: String): String = HexUtil.encodeHexStr(encrypt(data))

    fun decrypt(data: ByteArray): ByteArray = createCipher(Cipher.DECRYPT_MODE).doFinal(data)

    fun decrypt(data: String): ByteArray {
        val bytes = if (data.isHex()) {
            HexUtil.decodeHex(data)
        } else {
            Base64.decode(data)
        }
        return decrypt(bytes)
    }

    fun decryptStr(data: ByteArray): String = decrypt(data).toString(Charsets.UTF_8)

    fun decryptStr(data: ByteArray, charset: Charset?): String =
        decrypt(data).toString(charset ?: Charsets.UTF_8)

    fun decryptStr(data: String): String = decrypt(data).toString(Charsets.UTF_8)

    fun decryptStr(data: String, charset: Charset?): String =
        decrypt(data).toString(charset ?: Charsets.UTF_8)

}
