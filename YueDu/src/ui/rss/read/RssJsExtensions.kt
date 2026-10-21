package io.YueDu.ui.rss.read

import io.YueDu.data.entities.BaseSource
import io.YueDu.help.JsExtensions
import io.YueDu.ui.association.AddToBookshelfDialog
import io.YueDu.ui.book.search.SearchActivity
import io.YueDu.utils.showDialogFragment

@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity) : JsExtensions {

    override fun getSource(): BaseSource? {
        return activity.getSource()
    }

    fun searchBook(key: String) {
        SearchActivity.start(activity, key)
    }

    fun addBook(bookUrl: String) {
        activity.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

}
