package io.YueDu.ui.main.rss

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import io.YueDu.R
import io.YueDu.base.VMBaseFragment
import io.YueDu.constant.AppLog
import io.YueDu.data.AppDatabase
import io.YueDu.data.appDb
import io.YueDu.data.entities.RssSource
import io.YueDu.databinding.FragmentRssBinding
import io.YueDu.databinding.ItemRssBinding
import io.YueDu.lib.dialogs.alert
import io.YueDu.lib.theme.primaryColor
import io.YueDu.lib.theme.primaryTextColor
import io.YueDu.ui.main.MainFragmentInterface
import io.YueDu.ui.rss.article.RssSortActivity
import io.YueDu.ui.rss.favorites.RssFavoritesActivity
import io.YueDu.ui.rss.read.ReadRssActivity
import io.YueDu.ui.rss.source.edit.RssSourceEditActivity
import io.YueDu.ui.rss.source.manage.RssSourceActivity
import io.YueDu.ui.rss.subscription.RuleSubActivity
import io.YueDu.utils.applyTint
import io.YueDu.utils.flowWithLifecycleAndDatabaseChange
import io.YueDu.utils.openUrl
import io.YueDu.utils.setEdgeEffectColor
import io.YueDu.utils.startActivity
import io.YueDu.utils.transaction
import io.YueDu.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch


/**
 * 订阅界面
 */
class RssFragment() : VMBaseFragment<RssViewModel>(R.layout.fragment_rss),
    MainFragmentInterface,
    RssAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentRssBinding::bind)
    override val viewModel by viewModels<RssViewModel>()
    private val adapter by lazy {
        RssAdapter(requireContext(), this, this, viewLifecycleOwner.lifecycle)
    }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var groupsFlowJob: Job? = null
    private var rssFlowJob: Job? = null
    private val groups = linkedSetOf<String>()
    private var groupsMenu: SubMenu? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        upRssFlowJob()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_rss, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_rss_config -> startActivity<RssSourceActivity>()
            R.id.menu_rss_star -> startActivity<RssFavoritesActivity>()
            else -> if (item.groupId == R.id.menu_group_text) {
                searchView.setQuery("group:${item.title}", true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        searchView.clearFocus()
    }

    private fun upGroupsMenu() = groupsMenu?.transaction { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
        }
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.rss)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                upRssFlowJob(newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemRssBinding.inflate(layoutInflater, it, false).apply {
                tvName.setText(R.string.rule_subscription)
                ivIcon.setImageResource(R.drawable.image_legado)
                root.setOnClickListener {
                    startActivity<RuleSubActivity>()
                }
            }
        }
    }

    private fun initGroupData() {
        groupsFlowJob?.cancel()
        groupsFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.rssSourceDao.flowEnabledGroups().catch {
                AppLog.put("订阅界面获取分组数据失败\n${it.localizedMessage}", it)
            }.flowWithLifecycleAndDatabaseChange(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.RSS_SOURCE_TABLE_NAME
            ).conflate().collect {
                groups.clear()
                groups.addAll(it)
                upGroupsMenu()
            }
        }
    }

    private fun upRssFlowJob(searchKey: String? = null) {
        rssFlowJob?.cancel()
        rssFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.rssSourceDao.flowEnabled()
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.rssSourceDao.flowEnabledByGroup(key)
                }

                else -> appDb.rssSourceDao.flowEnabled(searchKey)
            }.flowWithLifecycleAndDatabaseChange(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.RSS_SOURCE_TABLE_NAME
            ).catch {
                AppLog.put("订阅界面更新数据出错", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    override fun openRss(rssSource: RssSource) {
        if (rssSource.singleUrl) {
            viewModel.getSingleUrl(rssSource) { url ->
                if (url.startsWith("http", true)) {
                    startActivity<ReadRssActivity> {
                        putExtra("title", rssSource.sourceName)
                        putExtra("origin", url)
                    }
                } else {
                    context?.openUrl(url)
                }
            }
        } else {
            startActivity<RssSortActivity> {
                putExtra("url", rssSource.sourceUrl)
            }
        }
    }

    override fun toTop(rssSource: RssSource) {
        viewModel.topSource(rssSource)
    }

    override fun edit(rssSource: RssSource) {
        startActivity<RssSourceEditActivity> {
            putExtra("sourceUrl", rssSource.sourceUrl)
        }
    }

    override fun del(rssSource: RssSource) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + rssSource.sourceName)
            noButton()
            yesButton {
                viewModel.del(rssSource)
            }
        }
    }

    override fun disable(rssSource: RssSource) {
        viewModel.disable(rssSource)
    }
}