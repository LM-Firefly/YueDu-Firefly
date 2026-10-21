package io.YueDu.help.storage

import io.YueDu.help.config.LocalConfig
import io.YueDu.help.crypto.SymmetricCryptoAndroid
import io.YueDu.utils.MD5Utils

class BackupAES {
    private val crypto = SymmetricCryptoAndroid(
        "AES",
        MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
    )
    fun encryptBase64(data: String): String = crypto.encryptBase64(data)
    fun decryptStr(data: String): String = crypto.decryptStr(data)
}
