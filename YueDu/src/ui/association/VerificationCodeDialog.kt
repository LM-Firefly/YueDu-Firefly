package io.YueDu.ui.association

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.YueDu.R
import io.YueDu.base.BaseDialogFragment
import io.YueDu.databinding.DialogVerificationCodeViewBinding
import io.YueDu.help.glide.ImageLoader
import io.YueDu.help.glide.OkHttpModelLoader
import io.YueDu.help.source.SourceVerificationHelp
import io.YueDu.lib.dialogs.alert
import io.YueDu.lib.theme.primaryColor
import io.YueDu.model.ImageProvider
import io.YueDu.ui.widget.dialog.PhotoDialog
import io.YueDu.utils.applyTint
import io.YueDu.utils.setLayout
import io.YueDu.utils.showDialogFragment
import io.YueDu.utils.viewbindingdelegate.viewBinding

/**
 * 图片验证码对话框
 * 结果保存在内存中
 * val key = "${sourceOrigin ?: ""}_verificationResult"
 * CacheManager.get(key)
 */
class VerificationCodeDialog() : BaseDialogFragment(R.layout.dialog_verification_code_view),
    Toolbar.OnMenuItemClickListener {

    constructor(
        imageUrl: String,
        sourceOrigin: String? = null,
        sourceName: String? = null,
        sourceType: Int
    ) : this() {
        arguments = Bundle().apply {
            putString("imageUrl", imageUrl)
            putString("sourceOrigin", sourceOrigin)
            putString("sourceName", sourceName)
            putInt("sourceType", sourceType)
        }
    }

    val binding by viewBinding(DialogVerificationCodeViewBinding::bind)
    val viewModel by viewModels<VerificationCodeViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private var sourceOrigin: String? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?): Unit = binding.run {
        initMenu()
        val arguments = arguments ?: return@run
        viewModel.initData(arguments)
        toolBar.setBackgroundColor(primaryColor)
        toolBar.subtitle = arguments.getString("sourceName")
        sourceOrigin = arguments.getString("sourceOrigin")
        val imageUrl = arguments.getString("imageUrl") ?: return@run
        loadImage(imageUrl, sourceOrigin)
        verificationCodeImageView.setOnClickListener {
            showDialogFragment(PhotoDialog(imageUrl, sourceOrigin))
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.verification_code)
        binding.toolBar.menu.applyTint(requireContext())
    }

    @SuppressLint("CheckResult")
    private fun loadImage(url: String, sourceUrl: String?) {
        ImageProvider.remove(url)
        ImageLoader.loadBitmap(requireContext(), url).apply {
            sourceUrl?.let {
                apply(RequestOptions().set(OkHttpModelLoader.sourceOriginOption, it))
            }
        }.error(R.drawable.image_loading_error)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap?>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: Target<Bitmap?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    val bitmap = resource.copy(resource.config!!, true)
                    ImageProvider.put(url, bitmap) // 传给 PhotoDialog
                    return false
                }
            })
            .into(binding.verificationCodeImageView)
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_ok -> {
                val verificationCode = binding.verificationCode.text.toString()
                SourceVerificationHelp.setResult(sourceOrigin!!, verificationCode)
                dismiss()
            }

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
        SourceVerificationHelp.checkResult(sourceOrigin!!)
        super.onDestroy()
        activity?.finish()
    }

}
