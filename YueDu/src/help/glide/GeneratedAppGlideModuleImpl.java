package com.bumptech.glide;

import android.content.Context;
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule;
import com.bumptech.glide.module.AppGlideModule;
import ch.qoqa.glide.svg.SvgLibraryGlideModule;
import io.YueDu.help.glide.LegadoGlideModule;

/**
 * 手写的 glide-ksp 生成物等价实现（正常由 {@code com.github.bumptech.glide:ksp} 注解处理器生成）。
 *
 * <p>不走注解处理器的原因：glide-ksp 内部用 KotlinPoet {@code ClassName.bestGuess()} 解析
 * {@code @GlideModule} 类的全限定名，该函数假设"包名段全小写开头、类名段全大写开头"，
 * 遇到 {@code io.YueDu.*} 这类含大写包名段的包会误判边界（{@code YueDu} 被当成类名，
 * 后续小写段校验失败），直接抛
 * {@code IllegalArgumentException: couldn't make a guess for io.YueDu....}，构建崩溃。
 *
 * <p>运行时发现机制不变：Glide 初始化时反射
 * {@code Class.forName("com.bumptech.glide.GeneratedAppGlideModuleImpl")}
 * 并以 {@code Context} 构造，本类的类名、包名、构造签名与生成物完全一致。
 *
 * <p>{@link #registerComponents} 中 LibraryGlideModule 先注册、AppGlideModule 最后注册，
 * 与注解处理器生成代码的顺序一致（AppGlideModule 的 replace 覆盖库模块的默认注册）。
 */
public final class GeneratedAppGlideModuleImpl extends GeneratedAppGlideModule {

  private final AppGlideModule appGlideModule = new LegadoGlideModule();

  /** 由 Glide 通过反射以 application context 构造，参数本身不需要使用。 */
  @SuppressWarnings("unused")
  public GeneratedAppGlideModuleImpl(Context context) {}

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    appGlideModule.applyOptions(context, builder);
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    new OkHttpLibraryGlideModule().registerComponents(context, glide, registry);
    new SvgLibraryGlideModule().registerComponents(context, glide, registry);
    appGlideModule.registerComponents(context, glide, registry);
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
