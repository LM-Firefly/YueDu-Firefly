package io.legado.app.help.crypto

import android.util.Base64
import androidx.annotation.Keep
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.HexUtils
import io.legado.app.utils.isHex
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

@Keep
@Suppress("unused")
class AsymmetricCrypto(private val transformation: String) {

    private val keyAlgorithm = transformation.substringBefore('/').ifBlank { "RSA" }
    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    private fun ensureKeyPairIfNeeded() {
        if (privateKey != null && publicKey != null) return
        val generator = KeyPairGenerator.getInstance(keyAlgorithm)
        when (keyAlgorithm.uppercase()) {
            "RSA" -> generator.initialize(2048)
            "EC" -> generator.initialize(256)
            "DSA" -> generator.initialize(2048)
        }
        val keyPair = generator.generateKeyPair()
        privateKey = keyPair.private
        publicKey = keyPair.public
    }

    private fun chooseKey(usePublicKey: Boolean?): java.security.Key {
        ensureKeyPairIfNeeded()
        return if (usePublicKey != false) {
            requireNotNull(publicKey) { "Public key is not set" }
        } else {
            requireNotNull(privateKey) { "Private key is not set" }
        }
    }

    fun setPrivateKey(key: ByteArray): AsymmetricCrypto {
        privateKey = KeyFactory.getInstance(keyAlgorithm)
            .generatePrivate(PKCS8EncodedKeySpec(key))
        return this
    }

    fun setPrivateKey(key: String): AsymmetricCrypto = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): AsymmetricCrypto {
        publicKey = KeyFactory.getInstance(keyAlgorithm)
            .generatePublic(X509EncodedKeySpec(key))
        return this
    }

    fun setPublicKey(key: String): AsymmetricCrypto = setPublicKey(key.encodeToByteArray())

    private fun decodeInput(data: String): ByteArray {
        return if (data.isHex()) HexUtils.decodeHex(data) else Base64.decode(data, Base64.DEFAULT)
    }

    private fun doCipher(data: ByteArray, mode: Int, usePublicKey: Boolean?): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(mode, chooseKey(usePublicKey))
        return cipher.doFinal(data)
    }

    @JvmOverloads
    fun decrypt(data: Any, usePublicKey: Boolean? = true): ByteArray {
        val bytes = when (data) {
            is ByteArray -> data
            is String -> decodeInput(data)
            is InputStream -> data.readBytes()
            else -> throw IllegalArgumentException("Unexpected input type")
        }
        return doCipher(bytes, Cipher.DECRYPT_MODE, usePublicKey)
    }

    @JvmOverloads
    fun decryptStr(data: Any, usePublicKey: Boolean? = true): String {
        return decrypt(data, usePublicKey).toString(Charsets.UTF_8)
    }

    @JvmOverloads
    fun encrypt(data: Any, usePublicKey: Boolean? = true): ByteArray {
        val bytes = when (data) {
            is ByteArray -> data
            is String -> data.toByteArray()
            is InputStream -> data.readBytes()
            else -> throw IllegalArgumentException("Unexpected input type")
        }
        return doCipher(bytes, Cipher.ENCRYPT_MODE, usePublicKey)
    }

    @JvmOverloads
    fun encryptHex(data: Any, usePublicKey: Boolean? = true): String {
        return HexUtils.encodeHexStr(encrypt(data, usePublicKey))
    }

    @JvmOverloads
    fun encryptBase64(data: Any, usePublicKey: Boolean? = true): String {
        return EncoderUtils.base64Encode(encrypt(data, usePublicKey))
    }
}
