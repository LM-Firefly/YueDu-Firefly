package io.legado.app.help

import android.util.Log
import io.legado.app.constant.AppLog
import java.security.Provider
import java.security.Security

/**
 * 安全提供程序初始化器
 * 
 * 用于在应用启动时主动初始化 BouncyCastle SecurityProvider，
 * 避免后续进行加密操作时才首次初始化导致的延迟和可能的失败。
 * 
 * 问题背景：
 * - BouncyCastle 提供程序在首次使用时进行静态初始化
 * - 在某些 Android 版本或特定设备上可能会失败
 * - 失败后 JVM 会缓存失败状态，后续初始化会被拒绝
 * 
 * 解决方案：
 * - 在应用启动早期主动初始化，捕获异常
 * - 如果初始化失败，至少在日志中有记录便于调试
 * - 不会导致应用启动失败（异常被捕获）
 */
object SecurityInitializer {
    private const val TAG = "SecurityInit"
    
    /**
     * 初始化 BouncyCastle 提供程序
     * 
     * 应在应用启动时（Application.onCreate()）调用一次
     * 建议在 BaseApplication 或类似的地方调用
     * 
     * 示例：
     * ```kotlin
     * override fun onCreate() {
     *     super.onCreate()
     *     SecurityInitializer.initBouncyCastleProvider()
     * }
     * ```
     */
    fun initBouncyCastleProvider() {
        try {
            AppLog.put("初始化 BouncyCastle 安全提供程序...")
            
            // 使用可选的 BouncyCastle 提供程序（若存在则注册）
            try {
                val bcProvider = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
                    .getConstructor().newInstance() as? Provider
                
                if (bcProvider != null) {
                    val installed = Security.getProvider(bcProvider.name)
                    if (installed == null) {
                        Security.addProvider(bcProvider)
                        AppLog.put("✓ BouncyCastleProvider 已初始化并注册 (版本: ${bcProvider.version})")
                    } else {
                        AppLog.put("✓ BouncyCastleProvider 已存在 (版本: ${installed.version})")
                    }
                } else {
                    AppLog.put("⚠ BouncyCastleProvider 无法实例化")
                }
            } catch (e: Exception) {
                AppLog.put("直接注册 BouncyCastleProvider 失败: ${e.message}")
            }
            
        } catch (e: Exception) {
            // 捕获所有异常，确保不会导致应用启动失败
            AppLog.put("⚠ 安全提供程序初始化发生异常: ${e.javaClass.simpleName} - ${e.message}")
            Log.w(TAG, "SecurityInitializer exception", e)
        }
    }
    
    /**
     * 获取当前已注册的所有加密相关提供程序信息
     * 用于调试和诊断
     */
    fun reportSecurityProviders() {
        try {
            val providers = Security.getProviders()
            val cryptoProviders = providers.filter { 
                it.name.contains("Crypto", ignoreCase = true) ||
                it.name.contains("BouncyCastle", ignoreCase = true) ||
                it.name.contains("Sun", ignoreCase = true)
            }
            
            if (cryptoProviders.isNotEmpty()) {
                AppLog.put("已注册的加密提供程序:")
                cryptoProviders.forEach { provider ->
                    AppLog.put("  - ${provider.name} (v${provider.version})")
                }
            } else {
                AppLog.put("⚠ 未发现任何加密提供程序")
            }
        } catch (e: Exception) {
            AppLog.put("获取提供程序列表失败: ${e.message}")
        }
    }
    
    /**
     * 验证加密库是否可用
     * 返回 true 表示加密库正常工作，false 表示可能存在问题
     */
    fun verifyCryptoAvailable(): Boolean {
        return try {
            // 尝试创建一个简单的加密实例
            val crypto = io.legado.app.help.crypto.SymmetricCryptoAndroid("AES", "1234567890123456".toByteArray())
            crypto != null
        } catch (e: Exception) {
            AppLog.put("加密库验证失败: ${e.javaClass.simpleName}")
            false
        }
    }
}
