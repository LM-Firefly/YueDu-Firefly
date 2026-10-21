package io.YueDu.lib.mobi.decompress

interface Decompressor {

    fun decompress(data: ByteArray): ByteArray

}
