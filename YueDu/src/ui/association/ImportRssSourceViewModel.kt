package io.YueDu.ui.association

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import io.YueDu.R
import io.YueDu.base.BaseViewModel
import io.YueDu.constant.AppConst
import io.YueDu.constant.AppLog
import io.YueDu.constant.AppPattern
import io.YueDu.data.appDb
import io.YueDu.data.entities.RssSource
import io.YueDu.exception.NoStackTraceException
import io.YueDu.help.config.AppConfig
import io.YueDu.help.http.decompressed
import io.YueDu.help.http.newCallResponseBody
import io.YueDu.help.http.okHttpClient
import io.YueDu.help.source.SourceHelp
import io.YueDu.utils.GSON
import io.YueDu.utils.fromJsonArray
import io.YueDu.utils.fromJsonObject
import io.YueDu.utils.isAbsUrl
import io.YueDu.utils.isJsonArray
import io.YueDu.utils.isJsonObject
import io.YueDu.utils.isUri
import io.YueDu.utils.jsonPath
import io.YueDu.utils.readText
import io.YueDu.utils.splitNotBlank
import splitties.init.appCtx

class ImportRssSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<RssSource>()
    val checkSources = arrayListOf<RssSource?>()
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
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val keepGroup = AppConfig.importKeepGroup
            val keepEnable = AppConfig.importKeepEnable
            val selectSource = arrayListOf<RssSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    checkSources[index]?.let {
                        if (keepName) {
                            source.sourceName = it.sourceName
                        }
                        if (keepGroup) {
                            source.sourceGroup = it.sourceGroup
                        }
                        if (keepEnable) {
                            source.enabled = it.enabled
                        }
                        source.customOrder = it.customOrder
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.sourceGroup = groups.joinToString(",")
                        } else {
                            source.sourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertRssSource(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text)
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        val mText = text.trim()
        when {
            mText.isJsonObject() -> kotlin.runCatching {
                val json = JsonPath.parse(mText)
                val urls = json.read<List<String>>("$.sourceUrls")
                if (!urls.isNullOrEmpty()) {
                    urls.forEach {
                        importSourceUrl(it)
                    }
                }
            }.onFailure {
                GSON.fromJsonArray<RssSource>(mText).getOrThrow().let {
                    val source = it.firstOrNull() ?: return@let
                    if (source.sourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是订阅源")
                    }
                    allSources.addAll(it)
                }
            }

            mText.isJsonArray() -> {
                GSON.fromJsonArray<RssSource>(mText).getOrThrow().let {
                    val source = it.firstOrNull() ?: return@let
                    if (source.sourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是订阅源")
                    }
                    allSources.addAll(it)
                }
            }

            mText.isAbsUrl() -> {
                importSourceUrl(mText)
            }

            mText.isUri() -> {
                importSourceAwait(mText.toUri().readText(appCtx))
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
        }.decompressed().byteStream().use { body ->
            val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
            for (item in items) {
                if (!item.containsKey("sourceUrl")) {
                    throw NoStackTraceException("不是订阅源")
                }
                val jsonItem = jsonPath.parse(item)
                GSON.fromJsonObject<RssSource>(jsonItem.jsonString()).getOrThrow().let { source ->
                    allSources.add(source)
                }
            }
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val has = appDb.rssSourceDao.getByKey(it.sourceUrl)
                checkSources.add(has)
                selectStatus.add(has == null || has.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}