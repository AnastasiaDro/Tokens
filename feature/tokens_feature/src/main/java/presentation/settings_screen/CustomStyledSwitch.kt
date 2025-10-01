package presentation.settings_screen


import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.cerebus.tokens.feature.tokens_feature.R

class CustomStyledSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.switchStyle
) : SwitchCompat(context, attrs, defStyleAttr) {

    init {
        // Цвет трека (track) при включении и выключении
        val trackColor = ContextCompat.getColorStateList(context, R.color.switch_track_color)
        trackTintList = trackColor

        // Цвет ползунка (thumb) при включении и выключении
        val thumbColor = ContextCompat.getColorStateList(context, R.color.switch_thumb_color)
        thumbTintList = thumbColor
    }
}
