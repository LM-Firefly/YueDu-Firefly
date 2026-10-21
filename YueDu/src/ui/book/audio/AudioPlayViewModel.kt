package io.YueDu.ui.book.audio

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.YueDu.R
import io.YueDu.base.BaseViewModel
import io.YueDu.constant.AppLog
import io.YueDu.constant.BookType
import io.YueDu.constant.EventBus
import io.YueDu.data.appDb
import io.YueDu.data.entities.Book
import io.YueDu.data.entities.BookChapter
import io.YueDu.data.entities.BookSource
import io.YueDu.help.book.getBookSource
import io.YueDu.help.book.removeType
import io.YueDu.help.book.simulatedTotalChapterNum
import io.YueDu.model.AudioPlay
import io.YueDu.model.webBook.WebBook
import io.YueDu.utils.postEvent
import io.YueDu.utils.toastOnUi

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {
    val titleData = MutableLiveData<String>()
    val coverData = MutableLiveData<String>()

    fun initData(intent: Intent) = AudioPlay.apply {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl") ?: book?.bookUrl ?: return@execute
            val book = appDb.bookDao.getBook(bookUrl) ?: return@execute
            inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            initBook(book)
        }.onFinally {
            saveRead()
        }
    }

    private suspend fun initBook(book: Book) {
        val isSameBook = AudioPlay.book?.bookUrl == book.bookUrl
        if (isSameBook) {
            AudioPlay.upData(book)
        } else {
            AudioPlay.resetData(book)
        }
        titleData.postValue(book.name)
        coverData.postValue(book.getDisplayCover())
        if (book.tocUrl.isEmpty() && !loadBookInfo(book)) {
            return
        }
        if (AudioPlay.chapterSize == 0 && !loadChapterList(book)) {
            return
        }
    }

    private suspend fun loadBookInfo(book: Book): Boolean {
        val bookSource = AudioPlay.bookSource ?: return true
        try {
            WebBook.getBookInfoAwait(bookSource, book)
            return true
        } catch (e: Exception) {
            AppLog.put("详情页出错: ${e.localizedMessage}", e, true)
            return false
        }
    }

    private suspend fun loadChapterList(book: Book): Boolean {
        val bookSource = AudioPlay.bookSource ?: return true
        try {
            val oldBook = book.copy()
            val cList = WebBook.getChapterListAwait(bookSource, book).getOrThrow()
            if (oldBook.bookUrl == book.bookUrl) {
                appDb.bookDao.update(book)
            } else {
                appDb.bookDao.replace(oldBook, book)
            }
            appDb.bookChapterDao.delByBook(book.bookUrl)
            appDb.bookChapterDao.insert(*cList.toTypedArray())
            AudioPlay.chapterSize = cList.size
            AudioPlay.simulatedChapterSize = book.simulatedTotalChapterNum()
            AudioPlay.upDurChapter()
            return true
        } catch (e: Exception) {
            context.toastOnUi(R.string.error_load_toc)
            return false
        }
    }

    fun upSource() {
        execute {
            val book = AudioPlay.book ?: return@execute
            AudioPlay.bookSource = book.getBookSource()
        }
    }

    fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        execute {
            AudioPlay.book?.migrateTo(book, toc)
            book.removeType(BookType.updateError)
            AudioPlay.book?.delete()
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            AudioPlay.upDurChapter()
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}