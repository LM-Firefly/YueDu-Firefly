package io.YueDu.ui.config

import android.app.Application
import android.content.Context
import io.YueDu.R
import io.YueDu.base.BaseViewModel
import io.YueDu.data.appDb
import io.YueDu.help.AppWebDav
import io.YueDu.help.book.BookHelp
import io.YueDu.utils.FileUtils
import io.YueDu.utils.restart
import io.YueDu.utils.toastOnUi
import kotlinx.coroutines.delay
import splitties.init.appCtx

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }

    fun clearWebViewData() {
        execute {
            FileUtils.delete(context.getDir("webview", Context.MODE_PRIVATE))
            FileUtils.delete(context.getDir("hws_webview", Context.MODE_PRIVATE), true)
            context.toastOnUi(R.string.clear_webview_data_success)
            delay(3000)
            appCtx.restart()
        }
    }

    fun shrinkDatabase() {
        execute {
            appDb.openHelper.writableDatabase.execSQL("VACUUM")
        }.onSuccess {
            context.toastOnUi(R.string.success)
        }
    }

}
