package io.YueDu.ui.login

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.script.rhino.runScriptWithContext
import io.YueDu.R
import io.YueDu.base.BaseDialogFragment
import io.YueDu.constant.AppLog
import io.YueDu.data.entities.BaseSource
import io.YueDu.data.entities.rule.RowUi
import io.YueDu.databinding.DialogLoginBinding
import io.YueDu.databinding.ItemFilletTextBinding
import io.YueDu.databinding.ItemSourceEditBinding
import io.YueDu.lib.dialogs.alert
import io.YueDu.lib.theme.primaryColor
import io.YueDu.ui.about.AppLogDialog
import io.YueDu.utils.GSON
import io.YueDu.utils.applyTint
import io.YueDu.utils.dpToPx
import io.YueDu.utils.isAbsUrl
import io.YueDu.utils.openUrl
import io.YueDu.utils.printOnDebug
import io.YueDu.utils.sendToClip
import io.YueDu.utils.setLayout
import io.YueDu.utils.showDialogFragment
import io.YueDu.utils.toastOnUi
import io.YueDu.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import splitties.views.onClick


class SourceLoginDialog : BaseDialogFragment(R.layout.dialog_login, true) {

    private val binding by viewBinding(DialogLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val source = viewModel.source ?: return
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = getString(R.string.login_source, source.getTag())
        val loginInfo = source.getLoginInfoMap()
        val loginUi = source.loginUi()
        try {
            loginUi?.forEachIndexed { index, rowUi ->
                when (rowUi.type) {
                    RowUi.Type.text -> ItemSourceEditBinding.inflate(
                        layoutInflater,
                        binding.root,
                        false
                    ).let {
                        binding.flexbox.addView(it.root)
                        it.root.id = index + 1000
                        it.textInputLayout.hint = rowUi.name
                        it.editText.setText(loginInfo?.get(rowUi.name))
                    }

                    RowUi.Type.password -> ItemSourceEditBinding.inflate(
                        layoutInflater,
                        binding.root,
                        false
                    ).let {
                        binding.flexbox.addView(it.root)
                        it.root.id = index + 1000
                        it.textInputLayout.hint = rowUi.name
                        it.editText.inputType =
                            InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                        it.editText.setText(loginInfo?.get(rowUi.name))
                    }

                    RowUi.Type.button -> ItemFilletTextBinding.inflate(
                        layoutInflater,
                        binding.root,
                        false
                    ).let {
                        binding.flexbox.addView(it.root)
                        rowUi.style().apply(it.root)
                        it.root.id = index + 1000
                        it.textView.text = rowUi.name
                        it.textView.setPadding(16.dpToPx())
                        it.root.onClick {
                            handleButtonClick(source, rowUi, loginUi)
                        }
                    }
                }
            }
        } catch (e: NullPointerException) {
            AppLog.put("登录UI JSON 数据错误", e, true)
        }
        binding.toolBar.inflateMenu(R.menu.source_login)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_ok -> {
                    val loginData = getLoginData(loginUi)
                    login(source, loginData)
                }

                R.id.menu_show_login_header -> alert {
                    setTitle(R.string.login_header)
                    source.getLoginHeader()?.let { loginHeader ->
                        setMessage(loginHeader)
                        positiveButton(R.string.copy_text) {
                            appCtx.sendToClip(loginHeader)
                        }
                    }
                }

                R.id.menu_del_login_header -> source.removeLoginHeader()
                R.id.menu_log -> showDialogFragment<AppLogDialog>()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun handleButtonClick(source: BaseSource, rowUi: RowUi, loginUi: List<RowUi>) {
        lifecycleScope.launch(IO) {
            if (rowUi.action.isAbsUrl()) {
                context?.openUrl(rowUi.action!!)
            } else if (rowUi.action != null) {
                // JavaScript
                val buttonFunctionJS = rowUi.action!!
                val loginJS = source.getLoginJs() ?: return@launch
                kotlin.runCatching {
                    runScriptWithContext {
                        source.evalJS("$loginJS\n$buttonFunctionJS") {
                            put("result", getLoginData(loginUi))
                        }
                    }
                }.onFailure { e ->
                    ensureActive()
                    AppLog.put("LoginUI Button ${rowUi.name} JavaScript error", e)
                }
            }
        }
    }

    private fun getLoginData(loginUi: List<RowUi>?): HashMap<String, String> {
        val loginData = hashMapOf<String, String>()
        loginUi?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                "text", "password" -> {
                    val rowView = binding.root.findViewById<View>(index + 1000)
                    ItemSourceEditBinding.bind(rowView).editText.text?.let {
                        loginData[rowUi.name] = it.toString()
                    }
                }
            }
        }
        return loginData
    }

    private fun login(source: BaseSource, loginData: HashMap<String, String>) {
        lifecycleScope.launch(IO) {
            if (loginData.isEmpty()) {
                source.removeLoginInfo()
                withContext(Main) {
                    dismiss()
                }
            } else if (source.putLoginInfo(GSON.toJson(loginData))) {
                try {
                    runScriptWithContext {
                        source.login()
                    }
                    context?.toastOnUi(R.string.success)
                    withContext(Main) {
                        dismiss()
                    }
                } catch (e: Exception) {
                    AppLog.put("登录出错\n${e.localizedMessage}", e)
                    context?.toastOnUi("登录出错\n${e.localizedMessage}")
                    e.printOnDebug()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

}
