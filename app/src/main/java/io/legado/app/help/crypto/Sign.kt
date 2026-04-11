package io.legado.app.help.crypto

import androidx.annotation.Keep
import io.legado.app.utils.HexUtils
import io.legado.app.utils.isHex
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@Keep
@Suppress("unused")
class Sign(private val algorithm: String) {

    private val keyAlgorithm: String = when {
        algorithm.contains("RSA", ignoreCase = true) -> "RSA"
        algorithm.contains("EC", ignoreCase = true) -> "EC"
        algorithm.contains("DSA", ignoreCase = true) -> "DSA"
        else -> "RSA"
    }

    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    private fun ensureKeyPairIfNeeded() {
        if (privateKey != null && publicKey != null) return
        val generator = KeyPairGenerator.getInstance(keyAlgorithm)
        when (keyAlgorithm) {
            "RSA" -> generator.initialize(2048)
            "EC" -> generator.initialize(256)
            "DSA" -> generator.initialize(2048)
        }
        val keyPair = generator.generateKeyPair()
        privateKey = keyPair.private
        publicKey = keyPair.public
    }

    fun setPrivateKey(key: ByteArray): Sign {
        privateKey = KeyFactory.getInstance(keyAlgorithm)
            .generatePrivate(PKCS8EncodedKeySpec(key))
        return this
    }

    fun setPrivateKey(key: String): Sign = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): Sign {
        publicKey = KeyFactory.getInstance(keyAlgorithm)
            .generatePublic(X509EncodedKeySpec(key))
        return this
    }

    fun setPublicKey(key: String): Sign = setPublicKey(key.encodeToByteArray())

    private fun normalizeData(data: Any): ByteArray {
        return when (data) {
            is ByteArray -> data
            is InputStream -> data.readBytes()
            is String -> if (data.isHex()) HexUtils.decodeHex(data) else data.toByteArray()
            else -> throw IllegalArgumentException("Unexpected input type")
        }
    }

    fun sign(data: Any): ByteArray {
        ensureKeyPairIfNeeded()
        val signature = Signature.getInstance(algorithm)
        signature.initSign(requireNotNull(privateKey) { "Private key is not set" })
        signature.update(normalizeData(data))
        return signature.sign()
    }

    fun signHex(data: Any): String = HexUtils.encodeHexStr(sign(data))

    fun verify(data: Any, signed: ByteArray): Boolean {
        ensureKeyPairIfNeeded()
        val signature = Signature.getInstance(algorithm)
        signature.initVerify(requireNotNull(publicKey) { "Public key is not set" })
        signature.update(normalizeData(data))
        return signature.verify(signed)
    }

    fun verify(data: Any, signed: String): Boolean {
        val bytes = if (signed.isHex()) HexUtils.decodeHex(signed) else signed.toByteArray()
        return verify(data, bytes)
    }
}

