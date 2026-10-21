package io.YueDu.ui.login

import android.app.Application
import android.content.Intent
import com.script.rhino.runScriptWithContext
import io.YueDu.base.BaseViewModel
import io.YueDu.constant.AppLog
import io.YueDu.data.appDb
import io.YueDu.data.entities.BaseSource
import io.YueDu.exception.NoStackTraceException
import io.YueDu.utils.toastOnUi

class SourceLoginViewModel(application: Application) : BaseViewModel(application) {

    var source: BaseSource? = null
    var headerMap: Map<String, String> = emptyMap()

    fun initData(intent: Intent, success: (bookSource: BaseSource) -> Unit, error: () -> Unit) {
        execute {
            val sourceKey = intent.getStringExtra("key")
                ?: throw NoStackTraceException("没有参数")
            when (intent.getStringExtra("type")) {
                "bookSource" -> source = appDb.bookSourceDao.getBookSource(sourceKey)
                "rssSource" -> source = appDb.rssSourceDao.getByKey(sourceKey)
                "httpTts" -> source = appDb.httpTTSDao.get(sourceKey.toLong())
            }
            headerMap = runScriptWithContext {
                source?.getHeaderMap(true) ?: emptyMap()
            }
            source
        }.onSuccess {
            if (it != null) {
                success.invoke(it)
            } else {
                context.toastOnUi("未找到书源")
            }
        }.onError {
            error.invoke()
            AppLog.put("登录 UI 初始化失败\n$it", it, true)
        }
    }

}