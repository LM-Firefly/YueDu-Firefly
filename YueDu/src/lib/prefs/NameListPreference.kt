package io.YueDu.lib.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import io.YueDu.R
import io.YueDu.lib.theme.bottomBackground
import io.YueDu.lib.theme.getPrimaryTextColor
import io.YueDu.utils.ColorUtils


class NameListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    private val isBottomBackground: Boolean

    init {
        layoutResource = R.layout.view_preference
        widgetLayoutResource = R.layout.item_fillet_text
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference)
        isBottomBackground = typedArray.getBoolean(R.styleable.Preference_isBottomBackground, false)
        typedArray.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val v = Preference.bindView<TextView>(
            context, holder, icon, title, summary, widgetLayoutResource,
            R.id.text_view, isBottomBackground = isBottomBackground
        )
        if (v is TextView) {
            v.text = entry
            if (isBottomBackground) {
                val bgColor = context.bottomBackground
                val pTextColor = context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
                v.setTextColor(pTextColor)
            }
        }
        super.onBindViewHolder(holder)
    }

}