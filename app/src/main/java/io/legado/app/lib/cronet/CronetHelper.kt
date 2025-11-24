@file:Keep
@file:Suppress("DEPRECATION")

package io.legado.app.lib.cronet

import androidx.annotation.Keep
import io.legado.app.constant.AppLog
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.SSLHelper
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.DebugLog
import io.legado.app.utils.externalCache
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.CronetEngine
import org.chromium.net.UploadDataProvider
import org.chromium.net.UrlRequest
// import org.chromium.net.X509Util // 已在Cronet 140中移除
import org.json.JSONObject
import splitties.init.appCtx

internal const val BUFFER_SIZE = 32 * 1024

val cronetEngine: CronetEngine? by lazy {
    CronetLoader.preDownload()
    disableCertificateVerify()
    // 若 so 未安装，先同步下载安装以便 native builder 能正确加载
    try { CronetLoader.installSync() } catch (e: Throwable) { AppLog.put("Cronet.installSync failed (pre-build)", e) }
    val builder = try {
        CronetEngine.Builder(appCtx)
    } catch (e: Throwable) {
        val msg = e.message ?: ""
        if (msg.contains("HttpEngineNativeProvider") || e is NoClassDefFoundError || e is ClassNotFoundException) { AppLog.put("Cronet.Builder ctor failed — missing internal Cronet class (HttpEngineNativeProvider); skipping Cronet", e) }
        else { AppLog.put("Cronet.Builder ctor failed", e) }
        null
    }?.apply {
        if (CronetLoader.install()) {
            setLibraryLoader(CronetLoader)//设置自定义so库加载
        }
        setStoragePath(appCtx.externalCache.absolutePath)//设置缓存路径
        enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50).toLong())//设置50M的磁盘缓存
        enableQuic(true)//设置支持http/3
        enableHttp2(true)  //设置支持http/2
        enablePublicKeyPinningBypassForLocalTrustAnchors(true)
        enableBrotli(true)//Brotli压缩
        val m = this.javaClass.getMethod("setExperimentalOptions", String::class.java)
        m.invoke(this, options)
    }
    if (builder == null) {
        AppLog.put("Cronet.Builder unavailable; attempting NativeCronetEngineBuilderImpl via reflection")
        try {
            val implClass = Class.forName("org.chromium.net.impl.NativeCronetEngineBuilderImpl")
            val ctor = implClass.getConstructor(android.content.Context::class.java)
            val nativeBuilder = ctor.newInstance(appCtx)
            try {
                val setLibraryLoader = implClass.getMethod("setLibraryLoader", org.chromium.net.CronetEngine.Builder.LibraryLoader::class.java)
                setLibraryLoader.invoke(nativeBuilder, CronetLoader)
            } catch (e: Throwable) { DebugLog.d("CronetHelper", "setLibraryLoader reflection: ${e.message}")
            }
            try { implClass.getMethod("setStoragePath", String::class.java).invoke(nativeBuilder, appCtx.externalCache.absolutePath) } catch (e: Throwable) { DebugLog.d("CronetHelper", "setStoragePath reflection: ${e.message}") }
            try { implClass.getMethod("enableHttp2", Boolean::class.javaPrimitiveType).invoke(nativeBuilder, true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enableHttp2 reflection: ${e.message}") }
            try { implClass.getMethod("enableBrotli", Boolean::class.javaPrimitiveType).invoke(nativeBuilder, true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enableBrotli reflection: ${e.message}") }
            try { implClass.getMethod("setExperimentalOptions", String::class.java).invoke(nativeBuilder, options) } catch (e: Throwable) { DebugLog.d("CronetHelper", "setExperimentalOptions reflection: ${e.message}") }
            val buildMethod = implClass.getMethod("build")
            val engine = buildMethod.invoke(nativeBuilder)
            if (engine is CronetEngine) {
                DebugLog.d("Cronet Version (native impl):", engine.versionString)
                return@lazy engine
            }
        } catch (e: Throwable) { AppLog.put("Native builder reflection failed", e) }
        AppLog.put("Cronet.Builder unavailable; skipping cronetEngine init")
        return@lazy null
    }
    try {
        val engine = builder.build()
        DebugLog.d("Cronet Version:", engine.versionString)
        return@lazy engine
    } catch (e: Throwable) {
        AppLog.put("Cronet build failed, attempting fallback", e)
        val isMissingInternal = (e is NoClassDefFoundError) || (e is ClassNotFoundException) || (e.message?.contains("HttpEngineNativeProvider") == true)
        if (isMissingInternal) {
            try {
                val fallback = CronetEngine.Builder(appCtx).apply {
                    setStoragePath(appCtx.externalCache.absolutePath)
                    try { enableHttp2(true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "fallback enableHttp2: ${e.message}") }
                    try { enableBrotli(true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "fallback enableBrotli: ${e.message}") }
                }.build()
                DebugLog.d("Cronet Fallback Version:", fallback.versionString)
                return@lazy fallback
            } catch (e2: Throwable) { AppLog.put("Cronet fallback build failed", e2) }
        }
        AppLog.put("初始化cronetEngine出错", e)
        return@lazy null
    }
}

val options by lazy {
    val options = JSONObject()

    //设置域名映射规则
    //MAP hostname ip,MAP hostname ip
//    val host = JSONObject()
//    host.put("host_resolver_rules","")
//    options.put("HostResolverRules", host)

    //启用DnsHttpsSvcb更容易迁移到http3
    val dnsSvcb = JSONObject()
    dnsSvcb.put("enable", true)
    dnsSvcb.put("enable_insecure", true)
    dnsSvcb.put("use_alpn", true)
    options.put("UseDnsHttpsSvcb", dnsSvcb)
    options.put("AsyncDNS", JSONObject("{'enable':true}"))

    options.toString()
}

fun buildRequest(request: Request, callback: UrlRequest.Callback): UrlRequest? {
    val url = request.url.toString()
    val headers: Headers = request.headers
    val requestBody = request.body
    return cronetEngine?.newUrlRequestBuilder(
        url,
        callback,
        okHttpClient.dispatcher.executorService
    )?.apply {
        setHttpMethod(request.method)//设置
        allowDirectExecutor()
        headers.forEachIndexed { index, _ ->
            if (headers.name(index) == cookieJarHeader) return@forEachIndexed
            addHeader(headers.name(index), headers.value(index))
        }
        if (requestBody != null) {
            val contentType: MediaType? = requestBody.contentType()
            if (contentType != null) {
                addHeader("Content-Type", contentType.toString())
            } else {
                addHeader("Content-Type", "text/plain")
            }
            val provider: UploadDataProvider = if (requestBody.contentLength() > BUFFER_SIZE) {
                LargeBodyUploadProvider(requestBody, okHttpClient.dispatcher.executorService)
            } else {
                BodyUploadProvider(requestBody)
            }
            provider.use {
                this.setUploadDataProvider(it, okHttpClient.dispatcher.executorService)
            }

        }
    }?.build()

}

private fun disableCertificateVerify() {
    // X509Util 在 Cronet 140 中已被移除，证书验证已被重构
    // 新版本中证书验证机制已更新，不需要手动禁用
    // 在 Cronet 141 中，HttpEngineNativeProvider 类也已被移除
    // 所有相关代码已被移除，避免运行时错误
}
