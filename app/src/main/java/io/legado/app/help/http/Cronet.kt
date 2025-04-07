package io.legado.app.help.http

import io.legado.app.lib.cronet.CronetLoader
import okhttp3.CookieJar
import okhttp3.Interceptor

object Cronet {

    val loader: LoaderInterface? by lazy {
        CronetLoader
    }

    fun preDownload() {
        loader?.preDownload()
    }

    val interceptor: Interceptor? by lazy {
        kotlin.runCatching {
            val iClass = Class.forName("io.legado.app.lib.cronet.CronetInterceptor")
            iClass.getDeclaredConstructor(CookieJar::class.java)
                .newInstance(cookieJar) as Interceptor
        }.getOrNull()
    }

    interface LoaderInterface {

        fun install(): Boolean

        fun preDownload()

    }

}