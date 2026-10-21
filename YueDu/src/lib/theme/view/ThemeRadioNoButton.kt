package io.YueDu.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.TooltipCompat
import io.YueDu.R
import io.YueDu.lib.theme.Selector
import io.YueDu.lib.theme.accentColor
import io.YueDu.lib.theme.bottomBackground
import io.YueDu.lib.theme.getPrimaryTextColor
import io.YueDu.utils.ColorUtils
import io.YueDu.utils.dpToPx
import io.YueDu.utils.getCompatColor

class ThemeRadioNoButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    private val isBottomBackground: Boolean

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThemeRadioNoButton)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.ThemeRadioNoButton_isBottomBackground, false)
        typedArray.recycle()
        initTheme()
        TooltipCompat.setTooltipText(this, text)
    }

    private fun initTheme() {
        when {
            isInEditMode -> Unit
            isBottomBackground -> {
                val accentColor = context.accentColor
                val isLight = ColorUtils.isColorLight(context.bottomBackground)
                val textColor = context.getPrimaryTextColor(isLight)
                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dpToPx())
                    .setStrokeWidth(2.dpToPx())
                    .setCheckedBgColor(accentColor)
                    .setCheckedStrokeColor(accentColor)
                    .setDefaultStrokeColor(textColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(textColor)
                        .setCheckedColor(checkedTextColor)
                        .create()
                )
            }
            else -> {
                val accentColor = context.accentColor
                val defaultTextColor = context.getCompatColor(R.color.primaryText)
                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dpToPx())
                    .setStrokeWidth(2.dpToPx())
                    .setCheckedBgColor(accentColor)
                    .setCheckedStrokeColor(accentColor)
                    .setDefaultStrokeColor(defaultTextColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(defaultTextColor)
                        .setCheckedColor(checkedTextColor)
                        .create()
                )
            }
        }

    }

}
