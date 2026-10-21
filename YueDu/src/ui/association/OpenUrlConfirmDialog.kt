package io.YueDu.ui.association

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import io.YueDu.R
import io.YueDu.base.BaseDialogFragment
import io.YueDu.constant.AppLog
import io.YueDu.databinding.DialogOpenUrlConfirmBinding
import io.YueDu.lib.dialogs.alert
import io.YueDu.lib.theme.primaryColor
import io.YueDu.utils.applyTint
import io.YueDu.utils.setLayout
import io.YueDu.utils.toastOnUi
import io.YueDu.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx

class OpenUrlConfirmDialog() : BaseDialogFragment(R.layout.dialog_open_url_confirm),
    Toolbar.OnMenuItemClickListener {

    constructor(
        uri: String,
        mimeType: String?,
        sourceOrigin: String? = null,
        sourceName: String? = null,
        sourceType: Int
    ) : this() {
        arguments = Bundle().apply {
            putString("uri", uri)
            putString("mimeType", mimeType)
            putString("sourceOrigin", sourceOrigin)
            putString("sourceName", sourceName)
            putInt("sourceType", sourceType)
        }
    }

    val binding by viewBinding(DialogOpenUrlConfirmBinding::bind)
    val viewModel by viewModels<OpenUrlConfirmViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initMenu()
        val arguments = arguments ?: return
        viewModel.initData(arguments)
        if (viewModel.uri.isBlank()) {
            dismiss()
            return
        }
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.subtitle = viewModel.sourceName
        initView()
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.open_url_confirm)
        binding.toolBar.menu.applyTint(requireContext())
    }

    private fun initView() {
        binding.message.text = "${viewModel.sourceName} 正在请求跳转链接/应用，是否跳转？"
        binding.btnNegative.setOnClickListener { dismiss() }
        binding.btnPositive.setOnClickListener {
            openUrl()
            dismiss()
        }
    }

    private fun openUrl() {
        try {
            val uri = viewModel.uri.toUri()
            val mimeType = viewModel.mimeType
            // 创建目标 Intent 并设置类型
            val targetIntent = Intent(Intent.ACTION_VIEW).apply {
                // 同时设置 Data 和 Type
                if (!mimeType.isNullOrBlank()) {
                    setDataAndType(uri, mimeType)
                } else {
                    data = uri
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 验证是否有应用可以处理
            if (targetIntent.resolveActivity(appCtx.packageManager) != null) {
                startActivity(targetIntent)
            } else {
                toastOnUi(R.string.can_not_open)
            }
        } catch (e: Exception) {
            AppLog.put("打开链接失败", e, true)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_disable_source -> {
                viewModel.disableSource {
                    dismiss()
                }
            }

            R.id.menu_delete_source -> {
                alert(R.string.draw) {
                    setMessage(getString(R.string.sure_del) + "\n" + viewModel.sourceName)
                    noButton()
                    yesButton {
                        viewModel.deleteSource {
                            dismiss()
                        }
                    }
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.finish()
    }

}
