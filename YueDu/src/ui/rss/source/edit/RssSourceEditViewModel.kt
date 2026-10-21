package io.YueDu.ui.rss.source.edit

import android.app.Application
import android.content.Intent
import io.YueDu.R
import io.YueDu.base.BaseViewModel
import io.YueDu.data.appDb
import io.YueDu.data.entities.RssSource
import io.YueDu.exception.NoStackTraceException
import io.YueDu.help.AppCacheManager
import io.YueDu.help.RuleComplete
import io.YueDu.help.http.CookieStore
import io.YueDu.help.source.removeSortCache
import io.YueDu.model.SharedJsScope
import io.YueDu.utils.GSON
import io.YueDu.utils.fromJsonObject
import io.YueDu.utils.getClipText
import io.YueDu.utils.printOnDebug
import io.YueDu.utils.stackTraceStr
import io.YueDu.utils.toastOnUi
import kotlinx.coroutines.Dispatchers


class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {
    var autoComplete = false
    var rssSource: RssSource? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("sourceUrl")
            if (key != null) {
                appDb.rssSourceDao.getByKey(key)?.let {
                    rssSource = it
                }
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: RssSource, success: ((RssSource) -> Unit)) {
        execute {
            if (source.sourceUrl.isBlank() || source.sourceName.isBlank()) {
                throw NoStackTraceException(context.getString(R.string.non_null_name_url))
            }
            val oldSource = rssSource ?: RssSource()
            if (!source.equal(oldSource)) {
                source.lastUpdateTime = System.currentTimeMillis()
                if (oldSource.sortUrl != source.sortUrl) {
                    oldSource.removeSortCache()
                }
                if (oldSource.jsLib != source.jsLib) {
                    SharedJsScope.remove(oldSource.jsLib)
                }
            }
            rssSource?.let {
                appDb.rssSourceDao.delete(it)
                //更新收藏的源地址
                if (it.sourceUrl != source.sourceUrl) {
                    appDb.rssStarDao.updateOrigin(source.sourceUrl, it.sourceUrl)
                    appDb.rssArticleDao.updateOrigin(source.sourceUrl, it.sourceUrl)
                    appDb.cacheDao.deleteSourceVariables(it.sourceUrl)
                    AppCacheManager.clearSourceVariables()
                }
            }
            appDb.rssSourceDao.insert(source)
            rssSource = source
            source
        }.onSuccess {
            success(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printOnDebug()
        }
    }

    fun pasteSource(onSuccess: (source: RssSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: RssSource? = null
            context.getClipText()?.let { json ->
                source = GSON.fromJsonObject<RssSource>(json).getOrThrow()
            }
            source
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                context.toastOnUi("格式不对")
            }
        }
    }

    fun importSource(text: String, finally: (source: RssSource) -> Unit) {
        execute {
            val text1 = text.trim()
            GSON.fromJsonObject<RssSource>(text1).getOrThrow().let {
                finally.invoke(it)
            }
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun clearCookie(url: String) {
        execute {
            CookieStore.removeCookie(url)
        }
    }

    fun ruleComplete(rule: String?, preRule: String? = null, type: Int = 1): String? {
        if (autoComplete) {
            return RuleComplete.autoComplete(rule, preRule, type)
        }
        return rule
    }

}