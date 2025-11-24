# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 这句话能够使我们的项目混淆后产生映射文件
# 包含有类名->混淆后类名的映射关系
-verbose

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*

-flattenpackagehierarchy

#############################################
#
# Android开发中一些需要保留的公共部分
#
#############################################
# 屏蔽错误Unresolved class name
#noinspection ShrinkerUnresolvedReference

# 移除Log类打印各个等级日志的代码，打正式包的时候可以做为禁log使用，这里可以作为禁止log打印的功能使用
# 记得proguard-android.txt中一定不要加-dontoptimize才起作用
# 另外的一种实现方案是通过BuildConfig.DEBUG的变量来控制
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 保持js引擎调用的java类
-keep class * extends io.legado.app.help.JsExtensions{*;}
# 数据类
-keep class **.data.entities.**{*;}
# hutool-core hutool-crypto
-keep class
!cn.hutool.core.util.RuntimeUtil,
!cn.hutool.core.util.ClassLoaderUtil,
!cn.hutool.core.util.ReflectUtil,
!cn.hutool.core.util.SerializeUtil,
!cn.hutool.core.util.ClassUtil,
cn.hutool.core.codec.**,
cn.hutool.core.util.**{*;}
-keep class cn.hutool.crypto.**{*;}
-keep interface cn.hutool.crypto.**{*;}

# 保持加密库的所有类和接口（核心修复）
# 这是关键的 ProGuard 规则，确保 BouncyCastle 提供程序在 R8 优化中不被移除或重命名
-keep class cn.hutool.crypto.GlobalBouncyCastleProvider{
    public <init>();
    public <init>(java.lang.String, int);
    *** getInstance();
    *** install();
}
-keep class cn.hutool.crypto.ProviderFactory{*;}
-keep class cn.hutool.crypto.symmetric.SymmetricCrypto{*;}

# 完全保持 BouncyCastle 库（必须）
-keep class org.bouncycastle.** {*;}
-keep interface org.bouncycastle.** {*;}
-keep enum org.bouncycastle.** {*;}

# 保持 Java 安全提供程序框架
-keep class java.security.Provider {*;}
-keep class java.security.Security {*;}
-keep class java.security.KeyStore {*;}
-keep class java.security.KeyFactory {*;}

# 防止 Hutool 库被过度优化
-keepclassmembers class cn.hutool.crypto.ProviderFactory {
    public static java.security.Provider createBouncyCastleProvider();
    public static java.security.Provider createBouncyCastleFipsProvider();
}

-dontwarn cn.hutool.**
-dontwarn org.bouncycastle.**
-dontwarn javax.security.**

# GlobalBouncyCastleProvider 在启动阶段可能失败，确保所有相关类都被保留
# 以便能够在运行时捕获和处理异常
# 缓存 Cookie
-keep class **.help.http.CookieStore{*;}
-keep class **.help.CacheManager{*;}
# StrResponse
-keep class **.help.http.StrResponse{*;}

# markwon
-dontwarn org.commonmark.ext.gfm.**

-keep class okhttp3.*{*;}
-keep class okio.*{*;}
-keep class com.jayway.jsonpath.*{*;}

# LiveEventBus
-keepclassmembers class androidx.lifecycle.LiveData {
    *** mObservers;
    *** mActiveCount;
}
-keepclassmembers class androidx.arch.core.internal.SafeIterableMap {
    *** size();
    *** putIfAbsent(...);
}

## ChangeBookSourceDialog initNavigationView
-keepclassmembers class androidx.appcompat.widget.Toolbar {
    *** mNavButtonView;
}

# MenuExtensions applyOpenTint
-keepnames class androidx.appcompat.view.menu.SubMenuBuilder
-keep class androidx.appcompat.view.menu.MenuBuilder {
    *** setOptionalIconsVisible(...);
    *** getNonActionItems();
}

# Keep JNI Zero annotations and related classes used by Cronet
-keep class internal.org.jni_zero.** { *; }
-keep @interface internal.org.jni_zero.** { *; }
-keepclasseswithmembers class * {
    @internal.org.jni_zero.CalledByNative <methods>;
}
-keepclasseswithmembers class * {
    @internal.org.jni_zero.JNINamespace <fields>;
}

# Additional rules for Cronet and JNI Zero
-keep class org.chromium.** { *; }
-keepclassmembers class org.chromium.** {
    @internal.org.jni_zero.CalledByNative *;
    @internal.org.jni_zero.JNINamespace *;
}

# Suppress warnings from missing_rules.txt
-dontwarn internal.org.jni_zero.CalledByNative
-dontwarn internal.org.jni_zero.JNINamespace

# Keep all Chromium Cronet classes
-keep class org.chromium.net.** { *; }
-keep class org.chromium.net.impl.** { *; }

# Historically we kept the removed internal class HttpEngineNativeProvider as a
# temporary compatibility shim. That shim has been removed as part of the
# Cronet migration; avoid keeping the removed class specifically so R8/ProGuard
# can optimize normally. We still keep provider/native patterns if required.
# (Removed specific -keep for HttpEngineNativeProvider)
# -keep class org.chromium.net.impl.HttpEngineNativeProvider { *; }
-keep class org.chromium.net.impl.**Provider { *; }
-keep class org.chromium.net.impl.*Native* { *; }

# Don't warn about missing Cronet classes
-dontwarn org.chromium.net.**
-dontwarn org.chromium.net.impl.**

# FileDocExtensions.kt treeDocumentFileConstructor
-keep class androidx.documentfile.provider.TreeDocumentFile {
    <init>(...);
}

# JsoupXpath
-keep,allowobfuscation class * implements org.seimicrawler.xpath.core.AxisSelector{*;}
-keep,allowobfuscation class * implements org.seimicrawler.xpath.core.NodeTest{*;}
-keep,allowobfuscation class * implements org.seimicrawler.xpath.core.Function{*;}

## JSOUP
-keep class org.jsoup.**{*;}
-dontwarn org.jspecify.annotations.NullMarked

## ExoPlayer 反射设置ua 保证该私有变量不被混淆
-keepclassmembers class androidx.media3.datasource.cache.CacheDataSource$Factory {
    *** upstreamDataSourceFactory;
}
## ExoPlayer 如果还不能播放就取消注释这个
# -keep class com.google.android.exoplayer2.** {*;}

## 对外提供api
-keep class io.legado.app.api.ReturnData{*;}

# Cronet
-keepclassmembers class org.chromium.net.X509Util {
    *** sDefaultTrustManager;
    *** sTestTrustManager;
}

# Keep internal Chromium classes required by Cronet
-keep class internal.org.chromium.** { *; }
-keep class org.chromium.base.** { *; }
-keep class org.chromium.base.version_info.** { *; }
-dontwarn internal.org.chromium.build.NativeLibraries
-dontwarn org.chromium.base.FeatureList
-dontwarn org.chromium.base.FeatureMap
-dontwarn org.chromium.base.FeatureOverrides
-dontwarn org.chromium.base.FeatureParam
-dontwarn org.chromium.base.version_info.VersionConstantsBridgeJni

# Throwable
-keepnames class * extends java.lang.Throwable
-keepclassmembernames,allowobfuscation class * extends java.lang.Throwable{*;}
