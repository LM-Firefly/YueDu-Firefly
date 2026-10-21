package org.chromium.net.impl

/**
 * 空标记类，供 Cronet `NativeCronetProvider.isEnabled()` 反射探测（Cronet 156.0.8078.48 实测）：
 * ```java
 * Class.forName(getClass().getName() + "Sentinel", false, getClassLoader());
 * ```
 *
 * 即以 `NativeCronetProvider` 类名拼接 "Sentinel" 探测本类：找到则 native provider 可用，抛 `ClassNotFoundException` 则 `isEnabled()` 恒返回 false。
 * 但 cronet 发布的 cronet_impl_native_java.jar 并未包含本类（156.0.8078.48 仍缺），故由 app 源码补齐，否则 native provider 永远不可用，
 * 叠加 JavaCronetProvider 被剥离时抛出 "All available Cronet providers are disabled"，全部网络请求失败。
 * 探测依赖 `NativeCronetProvider` 与本类同包同名前缀，故两者类名均不得混淆 （keep 规则见 YueDu/cronet-proguard-rules.pro 与 YueDu/cronet-local-rules.pro）。
 */
class NativeCronetProviderSentinel
