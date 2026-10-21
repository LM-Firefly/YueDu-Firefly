package io.YueDu.ui.book.read.config

import android.app.Application
import android.os.Bundle
import io.YueDu.base.BaseViewModel
import io.YueDu.data.appDb
import io.YueDu.data.entities.HttpTTS
import io.YueDu.exception.NoStackTraceException
import io.YueDu.model.ReadAloud
import io.YueDu.utils.getClipText
import io.YueDu.utils.isJsonArray
import io.YueDu.utils.isJsonObject
import io.YueDu.utils.toastOnUi

class HttpTtsEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    fun initData(arguments: Bundle?, success: (httpTTS: HttpTTS) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    return@execute appDb.httpTTSDao.get(argumentId)
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(httpTTS: HttpTTS, success: (() -> Unit)? = null) {
        id = httpTTS.id
        execute {
            appDb.httpTTSDao.insert(httpTTS)
            if (ReadAloud.ttsEngine == httpTTS.id.toString()) ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

    fun importFromClip(onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    HttpTTS.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    HttpTTS.fromJsonArray(text1).getOrThrow().first()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            onSuccess.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}