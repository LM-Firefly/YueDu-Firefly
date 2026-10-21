package io.YueDu.ui.dict

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import io.YueDu.R
import io.YueDu.base.BaseDialogFragment
import io.YueDu.data.entities.DictRule
import io.YueDu.databinding.DialogDictBinding
import io.YueDu.lib.theme.accentColor
import io.YueDu.lib.theme.backgroundColor
import io.YueDu.utils.setHtml
import io.YueDu.utils.setLayout
import io.YueDu.utils.toastOnUi
import io.YueDu.utils.viewbindingdelegate.viewBinding

/**
 * 词典
 */
class DictDialog() : BaseDialogFragment(R.layout.dialog_dict) {

    constructor(word: String) : this() {
        arguments = Bundle().apply {
            putString("word", word)
        }
    }

    private val viewModel by viewModels<DictViewModel>()
    private val binding by viewBinding(DialogDictBinding::bind)

    private var word: String? = null

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvDict.movementMethod = LinkMovementMethod()
        word = arguments?.getString("word")
        if (word.isNullOrEmpty()) {
            toastOnUi(R.string.cannot_empty)
            dismiss()
            return
        }
        binding.tabLayout.setBackgroundColor(backgroundColor)
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                val dictRule = tab.tag as DictRule
                binding.rotateLoading.visible()
                viewModel.dict(dictRule, word!!) {
                    binding.rotateLoading.inVisible()
                    binding.tvDict.setHtml(it)
                }
            }
        })
        viewModel.initData {
            it.forEach {
                binding.tabLayout.addTab(binding.tabLayout.newTab().apply {
                    text = it.name
                    tag = it
                })
            }
            
            setupTabLayoutMode(it.size)
        }
    }

    //根据已启用词典数动态选取布局
    private fun setupTabLayoutMode(dictCount: Int) {
        if (dictCount <= 4) {
            binding.tabLayout.tabMode = TabLayout.MODE_FIXED
            binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        } else {
            binding.tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
            binding.tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        }
    }
}
