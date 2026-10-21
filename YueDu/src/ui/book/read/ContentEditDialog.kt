package io.YueDu.ui.book.read

import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import io.YueDu.R
import io.YueDu.base.BaseDialogFragment
import io.YueDu.base.BaseViewModel
import io.YueDu.data.appDb
import io.YueDu.data.entities.BookChapter
import io.YueDu.databinding.DialogContentEditBinding
import io.YueDu.databinding.DialogEditTextBinding
import io.YueDu.help.book.BookHelp
import io.YueDu.help.book.ContentProcessor
import io.YueDu.help.book.isLocal
import io.YueDu.help.coroutine.Coroutine
import io.YueDu.lib.dialogs.alert
import io.YueDu.lib.theme.primaryColor
import io.YueDu.model.ReadBook
import io.YueDu.model.webBook.WebBook
import io.YueDu.utils.applyTint
import io.YueDu.utils.sendToClip
import io.YueDu.utils.setLayout
import io.YueDu.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 内容编辑
 */
class ContentEditDialog : BaseDialogFragment(R.layout.dialog_content_edit) {

    val binding by viewBinding(DialogContentEditBinding::bind)
    val viewModel by viewModels<ContentEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = ReadBook.curTextChapter?.title
        initMenu()
        binding.toolBar.setOnClickListener {
            lifecycleScope.launch {
                val book = ReadBook.book ?: return@launch
                val chapter = withContext(IO) {
                    appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
                } ?: return@launch
                editTitle(chapter)
            }
        }
        viewModel.loadStateLiveData.observe(viewLifecycleOwner) {
            if (it) {
                binding.rlLoading.visible()
            } else {
                binding.rlLoading.gone()
            }
        }
        viewModel.initContent {
            binding.contentView.setText(it)
            binding.contentView.post {
                binding.contentView.apply {
                    val lineIndex = layout.getLineForOffset(ReadBook.durChapterPos)
                    val lineHeight = layout.getLineTop(lineIndex)
                    scrollTo(0, lineHeight)
                }
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.content_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_save -> {
                    save()
                    dismiss()
                }
                R.id.menu_reset -> viewModel.initContent(true) { content ->
                    binding.contentView.setText(content)
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
                R.id.menu_copy_all -> requireContext()
                    .sendToClip("${binding.toolBar.title}\n${binding.contentView.text}")
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun editTitle(chapter: BookChapter) {
        alert {
            setTitle(R.string.edit)
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            alertBinding.editView.setText(chapter.title)
            setCustomView(alertBinding.root)
            okButton {
                chapter.title = alertBinding.editView.text.toString()
                lifecycleScope.launch {
                    withContext(IO) {
                        appDb.bookChapterDao.update(chapter)
                    }
                    binding.toolBar.title = chapter.getDisplayTitle()
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        save()
    }

    private fun save() {
        val content = binding.contentView.text?.toString() ?: return
        Coroutine.async {
            val book = ReadBook.book ?: return@async
            val chapter = appDb.bookChapterDao
                .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                ?: return@async
            BookHelp.saveText(book, chapter, content)
            ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
        }
    }

    class ContentEditViewModel(application: Application) : BaseViewModel(application) {
        val loadStateLiveData = MutableLiveData<Boolean>()
        var content: String? = null

        fun initContent(reset: Boolean = false, success: (String) -> Unit) {
            execute {
                val book = ReadBook.book ?: return@execute null
                val chapter = appDb.bookChapterDao
                    .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                    ?: return@execute null
                if (reset) {
                    content = null
                    BookHelp.delContent(book, chapter)
                    if (!book.isLocal) ReadBook.bookSource?.let { bookSource ->
                        WebBook.getContentAwait(bookSource, book, chapter)
                    }
                }
                return@execute content ?: let {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    val content = BookHelp.getContent(book, chapter) ?: return@let null
                    contentProcessor.getContent(book, chapter, content, includeTitle = false)
                        .toString()
                }
            }.onStart {
                loadStateLiveData.postValue(true)
            }.onSuccess {
                content = it
                success.invoke(it ?: "")
            }.onFinally {
                loadStateLiveData.postValue(false)
            }
        }

    }

}