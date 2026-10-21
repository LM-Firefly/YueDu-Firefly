package io.YueDu.help.http

import io.YueDu.lib.cronet.CronetInterceptor
import okhttp3.Interceptor

object Cronet {

    val interceptor: Interceptor? by lazy {
        CronetInterceptor(cookieJar)
    }

}
