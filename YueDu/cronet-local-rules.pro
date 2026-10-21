# ============================================================================
# Cronet 本地补丁规则 —— 本文件永不被脚本覆盖，请在此维护手工规则
# ----------------------------------------------------------------------------
# YueDu/cronet-proguard-rules.pro 由 .github/scripts/cronet.sh 的 sync_proguard_rules() 从 Chromium 官方 golden cfg 整文件覆盖（rm -f + curl），
# 因此任何手工补充的 keep 规则都必须放在本文件，否则会在下次 cronet 更新 PR 中丢失。
# ============================================================================

# -------- 1. protobuf 消息类字段反射保留 --------
# 修复 release 崩溃：java.lang.RuntimeException: Field flags_ for il.n not found.
# Chromium 内嵌 protobuf（org.chromium.net.internal.com.google.protobuf）lite 运行时在 MessageSchema.newSchemaForRawMessageInfo 中
# 通过反射 getDeclaredField("<field>_") 定位消息类的 map/oneof 字段（如 httpflags.Flags 的 flags_: MapFieldLite）。
# R8 重命名字段后反射失败，令 CronetEngine 初始化崩溃，WebDav 同步/正文/目录等全部网络请求失败。
# 保留 protobuf 消息类及其 Builder 的全部字段名（类名混淆无碍，反射按 Class 对象查找）。
-keepclassmembers class * extends org.chromium.net.internal.com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-keepclassmembers class * extends org.chromium.net.internal.com.google.protobuf.GeneratedMessageLite$Builder {
    <fields>;
}

# -------- 2. Cronet provider 反射链 --------
# 修复 release 崩溃：All available Cronet providers are disabled.
# CronetProvider.getEnabledProviders 按类名反射（Class.forName + getConstructor(Context)） 加载候选 provider。
# JavaCronetProvider 是纯 Java 兜底实现（isEnabled 恒 true、不依赖 native so），若被 R8 剥离，则 native so 加载失败时无任何可用 provider。
# 保名并保住 Context 构造器；HttpEngineNativeProvider 同理由反射实例化。
-keep class org.chromium.net.impl.JavaCronetProvider {
    public <init>(android.content.Context);
}
-keep class org.chromium.net.impl.HttpEngineNativeProvider {
    public <init>(android.content.Context);
}

# -------- 3. Sentinel 标记类（官方 cfg 已含此 keep，此处双保险） --------
# NativeCronetProvider.isEnabled() 通过 Class.forName("org.chromium.net.impl.NativeCronetProviderSentinel") 反射探测。
# Cronet 155 发布 jar 缺失此类，由 app 源码补齐： `YueDu/src/org/chromium/net/impl/NativeCronetProviderSentinel.kt`
# 注意：若未来新版 cronet jar 内置了该类，请删除 app 侧补丁类，避免 duplicate class。
-keep class org.chromium.net.impl.NativeCronetProviderSentinel
