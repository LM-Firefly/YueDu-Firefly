package io.YueDu.ui.association

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import io.YueDu.R
import io.YueDu.base.BaseViewModel
import io.YueDu.constant.AppConst
import io.YueDu.constant.AppLog
import io.YueDu.data.appDb
import io.YueDu.data.entities.DictRule
import io.YueDu.exception.NoStackTraceException
import io.YueDu.help.http.decompressed
import io.YueDu.help.http.newCallResponseBody
import io.YueDu.help.http.okHttpClient
import io.YueDu.help.http.text
import io.YueDu.utils.GSON
import io.YueDu.utils.fromJsonArray
import io.YueDu.utils.fromJsonObject
import io.YueDu.utils.isAbsUrl
import io.YueDu.utils.isJsonArray
import io.YueDu.utils.isJsonObject
import io.YueDu.utils.isUri
import io.YueDu.utils.readText
import splitties.init.appCtx

class ImportDictRuleViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<DictRule>()
    val checkSources = arrayListOf<DictRule?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val selectSource = arrayListOf<DictRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.dictRuleDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                GSON.fromJsonObject<DictRule>(text).getOrThrow().let {
                    allSources.add(it)
                }
            }

            text.isJsonArray() -> GSON.fromJsonArray<DictRule>(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }

            text.isAbsUrl() -> {
                importSourceUrl(text)
            }

            text.isUri() -> {
                importSourceAwait(text.toUri().readText(appCtx))
            }

            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.dictRuleDao.getByName(it.name)
                checkSources.add(source)
                selectStatus.add(source == null)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}