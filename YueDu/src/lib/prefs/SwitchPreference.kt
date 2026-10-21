package io.YueDu.lib.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import io.YueDu.R
import io.YueDu.lib.theme.accentColor
import io.YueDu.utils.applyTint

class SwitchPreference(context: Context, attrs: AttributeSet) :
    SwitchPreferenceCompat(context, attrs) {

    private val isBottomBackground: Boolean
    private var onLongClick: ((preference: SwitchPreference) -> Boolean)? = null

    init {
        layoutResource = R.layout.view_preference
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference)
        isBottomBackground = typedArray.getBoolean(R.styleable.Preference_isBottomBackground, false)
        typedArray.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val v = Preference.bindView<SwitchCompat>(
            context, holder, icon, title, summary,
            widgetLayoutResource,
            androidx.preference.R.id.switchWidget,
            isBottomBackground = isBottomBackground
        )
        if (v is SwitchCompat && !v.isInEditMode) {
            v.applyTint(context.accentColor)
        }
        super.onBindViewHolder(holder)
        onLongClick?.let { listener ->
            holder.itemView.setOnLongClickListener {
                listener.invoke(this)
            }
        }
    }

    fun onLongClick(listener: (preference: SwitchPreference) -> Boolean) {
        onLongClick = listener
    }

}
