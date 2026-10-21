package org.chromium.net.impl

/**
 * Cronet NativeCronetProvider.isEnabled() 通过 `Class.forName("org.chromium.net.impl.NativeCronetProviderSentinel")` 反射探测 native provider 是否可用
 * （官方 cronet_impl_native_proguard.cfg 亦要求 keep 该类，见 YueDu/cronet-proguard-rules.pro）。
 *
 * 但 Cronet 155.0.8059.84 发布的 cronet_impl_native_java.jar 并未包含此类 （jar 内 186 个 org.chromium.net.impl.* 类中无任何 Sentinel 类），
 * 导致 Class.forName 必然抛 ClassNotFoundException，isEnabled() 恒返回 false， 叠加 JavaCronetProvider 被 R8 剥离后所有 Cronet provider 均不可用，
 * 抛出 "All available Cronet providers are disabled"，全部网络请求失败。
 *
 * 此处补齐该空标记类（保持类名不被混淆由 proguard keep 规则保证）。
 */
class NativeCronetProviderSentinel
