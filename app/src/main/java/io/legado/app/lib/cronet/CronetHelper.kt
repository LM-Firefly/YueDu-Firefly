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
    // Ensure cronet artifacts are prepared
    CronetLoader.preDownload()
    disableCertificateVerify()

    // Use the public CronetEngine.Builder instead of the experimental type.
    // This avoids direct dependency on internal implementation classes.
    val builder = try {
        CronetEngine.Builder(appCtx).apply {
        // If we have a local/native library ready, set the library loader so the
        // builder prefers native provider paths. Cronet may still probe Java
        // providers internally; we add a guarded fallback below.
        if (CronetLoader.install()) {
            try {
                setLibraryLoader(CronetLoader) // 设置自定义so库加载
            } catch (e: Throwable) {
                // Some Cronet versions may not accept a custom loader here — log and continue.
                AppLog.put("setLibraryLoader failed", e)
            }
        }
        setStoragePath(appCtx.externalCache.absolutePath)//设置缓存路径
        enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50).toLong())//设置50M的磁盘缓存
        try {
            enableQuic(true)//设置支持http/3
            enableHttp2(true)  //设置支持http/2
            enablePublicKeyPinningBypassForLocalTrustAnchors(true)
            enableBrotli(true)//Brotli压缩
        } catch (_: Throwable) {
            // Some public builder variants may not expose all experimental toggles;
            // swallow and continue — the feature flags are best-effort.
        }

        try {
            // Some Cronet releases do not declare setExperimentalOptions at compile time.
            // Use reflection to invoke it when present to remain source-compatible.
            try {
                val m = this.javaClass.getMethod("setExperimentalOptions", String::class.java)
                m.invoke(this, options)
            } catch (_: NoSuchMethodException) {
                // Method not present in this Cronet API — ignore.
            }
        } catch (_: Throwable) {
            // Defensive: swallow any reflection / invocation errors and continue.
        }
        }
    } catch (e: Throwable) {
        // Constructor for CronetEngine.Builder may throw when internal Cronet
        // implementation classes are missing (NoClassDefFoundError). Catch and
        // return null so callers can fall back safely.
        AppLog.put("Cronet.Builder constructor failed", e)
        null
    }

    // Try building the engine. If the build fails due to missing internal
    // implementation classes (e.g. HttpEngineNativeProvider), attempt a safer
    // fallback builder that avoids setting a custom library loader and some
    // experimental flags. If that still fails, return null and let callers
    // fallback to OkHttp or other transport.
    if (builder == null) {
        AppLog.put("Cronet.Builder unavailable; skipping cronetEngine init")
        return@lazy null
    }

    try {
        val engine = builder.build()
        DebugLog.d("Cronet Version:", engine.versionString)
        return@lazy engine
    } catch (e: Throwable) {
        AppLog.put("Cronet build failed, attempting fallback", e)
        // Detect the specific missing-class failure which indicates an internal
        // implementation changed between Cronet versions.
        val isMissingInternal = (e is NoClassDefFoundError) || (e is ClassNotFoundException) || (e.message?.contains("HttpEngineNativeProvider") == true)
        if (isMissingInternal) {
            try {
                val fallback = CronetEngine.Builder(appCtx).apply {
                    setStoragePath(appCtx.externalCache.absolutePath)
                    try { enableHttp2(true) } catch (_: Throwable) {}
                    try { enableBrotli(true) } catch (_: Throwable) {}
                }.build()
                DebugLog.d("Cronet Fallback Version:", fallback.versionString)
                return@lazy fallback
            } catch (e2: Throwable) {
                AppLog.put("Cronet fallback build failed", e2)
            }
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
