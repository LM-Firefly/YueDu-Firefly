package io.YueDu.ui.rss.favorites


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.YueDu.R
import io.YueDu.base.VMBaseFragment
import io.YueDu.constant.AppLog
import io.YueDu.data.appDb
import io.YueDu.data.entities.RssStar
import io.YueDu.databinding.FragmentRssArticlesBinding
import io.YueDu.lib.dialogs.alert
import io.YueDu.lib.theme.primaryColor
import io.YueDu.ui.rss.read.ReadRssActivity
import io.YueDu.ui.widget.recycler.VerticalDivider
import io.YueDu.utils.applyNavigationBarPadding
import io.YueDu.utils.setEdgeEffectColor
import io.YueDu.utils.startActivity
import io.YueDu.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssFavoritesFragment() : VMBaseFragment<RssFavoritesViewModel>(R.layout.fragment_rss_articles),
    RssFavoritesAdapter.CallBack {

    constructor(group: String) : this() {
        arguments = Bundle().apply {
            putString("group", group)
        }
    }

    private val binding by viewBinding(FragmentRssArticlesBinding::bind)
    override val viewModel by viewModels<RssFavoritesViewModel>()
    private val adapter: RssFavoritesAdapter by lazy {
        RssFavoritesAdapter(requireContext(), this@RssFavoritesFragment)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        loadArticles()
    }

    private fun initView() = binding.run {
        refreshLayout.isEnabled = false
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.layoutManager = run {
            recyclerView.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())
        }
        recyclerView.adapter = adapter
        recyclerView.applyNavigationBarPadding()
    }

    private fun loadArticles() {
        lifecycleScope.launch {
            val group = arguments?.getString("group") ?: "默认分组"
            appDb.rssStarDao.flowByGroup(group).catch {
                AppLog.put("订阅文章界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }

    override fun delStar(rssStar: RssStar) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n<" + rssStar.title + ">")
            noButton()
            yesButton {
                appDb.rssStarDao.delete(rssStar.origin, rssStar.link)
            }
        }
    }
}
