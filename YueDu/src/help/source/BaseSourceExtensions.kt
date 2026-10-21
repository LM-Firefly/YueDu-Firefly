package io.YueDu.help.source

import io.YueDu.constant.SourceType
import io.YueDu.data.entities.BaseSource
import io.YueDu.data.entities.BookSource
import io.YueDu.data.entities.RssSource
import io.YueDu.model.SharedJsScope
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

fun BaseSource.getShareScope(coroutineContext: CoroutineContext? = null): Scriptable? {
    return SharedJsScope.getScope(jsLib, coroutineContext)
}

fun BaseSource.getSourceType(): Int {
    return when (this) {
        is BookSource -> SourceType.book
        is RssSource -> SourceType.rss
        else -> error("unknown source type: ${this::class.simpleName}.")
    }
}
