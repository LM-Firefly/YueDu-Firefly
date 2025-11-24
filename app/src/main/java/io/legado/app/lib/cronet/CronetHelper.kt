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
    // 若 so 未安装，先同步下载以便 native 路径可用
    try { CronetLoader.installSync() } catch (e: Throwable) { AppLog.put("Cronet.installSync failed (pre-build)", e) }

    // so 就绪时直接走 NativeCronetEngineBuilderImpl（platform jar 已移除，ServiceLoader 无 provider 注册）
    if (CronetLoader.install()) {
        try {
            val implClass = Class.forName("org.chromium.net.impl.NativeCronetEngineBuilderImpl")
            val ctor = implClass.getConstructor(android.content.Context::class.java)
            val nb = ctor.newInstance(appCtx)
            try { implClass.getMethod("setLibraryLoader", org.chromium.net.CronetEngine.Builder.LibraryLoader::class.java).invoke(nb, CronetLoader) } catch (e: Throwable) { DebugLog.d("CronetHelper", "setLibraryLoader: ${e.message}") }
            try { implClass.getMethod("setStoragePath", String::class.java).invoke(nb, appCtx.externalCache.absolutePath) } catch (e: Throwable) { DebugLog.d("CronetHelper", "setStoragePath: ${e.message}") }
            try { implClass.getMethod("enableHttpCache", Int::class.javaPrimitiveType, Long::class.javaPrimitiveType).invoke(nb, HTTP_CACHE_DISK, 1024L * 1024 * 50) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enableHttpCache: ${e.message}") }
            try { implClass.getMethod("enableQuic", Boolean::class.javaPrimitiveType).invoke(nb, true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enableQuic: ${e.message}") }
            try { implClass.getMethod("enableHttp2", Boolean::class.javaPrimitiveType).invoke(nb, true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enableHttp2: ${e.message}") }
            try { implClass.getMethod("enablePublicKeyPinningBypassForLocalTrustAnchors", Boolean::class.javaPrimitiveType).invoke(nb, true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enablePKPBypass: ${e.message}") }
            try { implClass.getMethod("enableBrotli", Boolean::class.javaPrimitiveType).invoke(nb, true) } catch (e: Throwable) { DebugLog.d("CronetHelper", "enableBrotli: ${e.message}") }
            try { implClass.getMethod("setExperimentalOptions", String::class.java).invoke(nb, options) } catch (e: Throwable) { DebugLog.d("CronetHelper", "setExperimentalOptions: ${e.message}") }
            val engine = implClass.getMethod("build").invoke(nb)
            if (engine is CronetEngine) {
                DebugLog.d("Cronet Version (native):", engine.versionString)
                return@lazy engine
            }
        } catch (e: Throwable) {
            AppLog.put("Cronet native reflection 初始化失败", e)
        }
        AppLog.put("Cronet 初始化失败，回退到 OkHttp")
        return@lazy null
    }

    // so 不可用：尝试 ServiceLoader 路径（系统 / GMS Cronet）
    val builder = try {
        CronetEngine.Builder(appCtx)
    } catch (e: Throwable) {
        AppLog.put("Cronet.Builder ctor failed (no system provider)", e)
        null
    }?.apply {
        setStoragePath(appCtx.externalCache.absolutePath)
        enableHttpCache(HTTP_CACHE_DISK, 1024L * 1024 * 50)
        enableQuic(true)
        enableHttp2(true)
        enablePublicKeyPinningBypassForLocalTrustAnchors(true)
        enableBrotli(true)
        try {
            val m = this.javaClass.getMethod("setExperimentalOptions", String::class.java)
            m.invoke(this, options)
        } catch (e: Throwable) { DebugLog.d("CronetHelper", "setExperimentalOptions: ${e.message}") }
    }
    if (builder == null) {
        AppLog.put("Cronet 初始化失败，回退到 OkHttp")
        return@lazy null
    }
    try {
        val engine = builder.build()
        DebugLog.d("Cronet Version:", engine.versionString)
        return@lazy engine
    } catch (e: Throwable) {
        AppLog.put("Cronet system build 失败", e)
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
