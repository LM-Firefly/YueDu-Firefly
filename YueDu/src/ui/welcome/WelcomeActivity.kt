package io.YueDu.ui.welcome

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.view.postDelayed
import io.YueDu.base.BaseActivity
import io.YueDu.constant.PreferKey
import io.YueDu.constant.Theme
import io.YueDu.data.appDb
import io.YueDu.databinding.ActivityWelcomeBinding
import io.YueDu.help.config.AppConfig
import io.YueDu.help.config.ThemeConfig
import io.YueDu.lib.theme.accentColor
import io.YueDu.lib.theme.backgroundColor
import io.YueDu.ui.book.read.ReadBookActivity
import io.YueDu.ui.main.MainActivity
import io.YueDu.utils.BitmapUtils
import io.YueDu.utils.fullScreen
import io.YueDu.utils.getPrefBoolean
import io.YueDu.utils.getPrefString
import io.YueDu.utils.setStatusBarColorAuto
import io.YueDu.utils.startActivity
import io.YueDu.utils.viewbindingdelegate.viewBinding
import io.YueDu.utils.visible
import io.YueDu.utils.windowSize

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            binding.root.postDelayed(600) { startMainActivity() }
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(AppConfig.welcomeShowTextDark)
                            binding.ivBook.visible(AppConfig.welcomeShowIconDark)
                            binding.tvGzh.visible(AppConfig.welcomeShowTextDark)
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }

                    else -> getPrefString(PreferKey.welcomeImage)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(AppConfig.welcomeShowText)
                            binding.ivBook.visible(AppConfig.welcomeShowIcon)
                            binding.tvGzh.visible(AppConfig.welcomeShowText)
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead) && appDb.bookDao.lastReadBook != null) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()