package io.YueDu.help.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import io.YueDu.BuildConfig
import io.YueDu.help.config.AppConfig
import java.io.File
import java.io.InputStream


// 注意：本类不加 @GlideModule 注解。glide-ksp 处理器内部用 KotlinPoet ClassName.bestGuess()
// 解析 @GlideModule 类的全限定名，该函数假设"包名段全小写开头、类名段大写开头"，
// 无法解析 io.YueDu 这种含大写包名段的包（YueDu 段被误判为类名，后续段校验失败，
// 抛 IllegalArgumentException: couldn't make a guess），导致构建崩溃。
// 改由同目录的 GeneratedAppGlideModuleImpl（手写的 glide-ksp 生成物等价实现）在
// Glide 初始化时反射加载并委托到本类，行为与注解处理器生成的产物一致。
@Suppress("unused")
class LegadoGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpModeLoaderFactory
        )
        registry.prepend(
            String::class.java,
            InputStream::class.java,
            LegadoDataUrlLoader.Factory()
        )
        registry.prepend(
            String::class.java,
            File::class.java,
            FilePathLoader.Factory()
        )
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        val calculator = MemorySizeCalculator.Builder(context).build()
        val bitmapPool = AsyncRecycleBitmapPool(calculator.bitmapPoolSize)
        builder.setMemorySizeCalculator(calculator)
        builder.setBitmapPool(bitmapPool)
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 1024 * 1024 * 1000))
        if (!BuildConfig.DEBUG && !AppConfig.recordLog) {
            builder.setLogLevel(Log.ERROR)
        }
    }
}
