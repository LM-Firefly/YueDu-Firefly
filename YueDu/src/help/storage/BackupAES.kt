package io.YueDu.help.storage

import cn.hutool.crypto.symmetric.AES
import io.YueDu.help.config.LocalConfig
import io.YueDu.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)