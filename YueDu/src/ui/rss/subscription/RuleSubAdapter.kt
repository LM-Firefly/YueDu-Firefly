package io.YueDu.ui.rss.subscription

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import io.YueDu.R
import io.YueDu.base.adapter.ItemViewHolder
import io.YueDu.base.adapter.RecyclerAdapter
import io.YueDu.data.entities.RuleSub
import io.YueDu.databinding.ItemRuleSubBinding
import io.YueDu.ui.widget.recycler.ItemTouchCallback


class RuleSubAdapter(context: Context, val callBack: Callback) :
    RecyclerAdapter<RuleSub, ItemRuleSubBinding>(context),
    ItemTouchCallback.Callback {

    private val typeArray = context.resources.getStringArray(R.array.rule_type)

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRuleSubBinding,
        item: RuleSub,
        payloads: MutableList<Any>
    ) {
        binding.tvType.text = typeArray[item.type]
        binding.tvName.text = item.name
        binding.tvUrl.text = item.url
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRuleSubBinding) {
        binding.root.setOnClickListener {
            callBack.openSubscription(getItem(holder.layoutPosition)!!)
        }
        binding.ivEdit.setOnClickListener {
            callBack.editSubscription(getItem(holder.layoutPosition)!!)
        }
        binding.ivMenuMore.setOnClickListener {
            showMenu(binding.ivMenuMore, holder.layoutPosition)
        }
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.source_sub_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_del -> callBack.delSubscription(source)
            }
            true
        }
        popupMenu.show()
    }

    override fun getViewBinding(parent: ViewGroup): ItemRuleSubBinding {
        return ItemRuleSubBinding.inflate(inflater, parent, false)
    }

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.customOrder == targetItem.customOrder) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.customOrder
                srcItem.customOrder = targetItem.customOrder
                targetItem.customOrder = srcOrder
                movedItems.add(srcItem)
                movedItems.add(targetItem)
            }
        }
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<RuleSub>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.updateSourceSub(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    interface Callback {
        fun openSubscription(ruleSub: RuleSub)
        fun editSubscription(ruleSub: RuleSub)
        fun delSubscription(ruleSub: RuleSub)
        fun updateSourceSub(vararg ruleSub: RuleSub)
        fun upOrder()
    }

}