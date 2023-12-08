package presentation.tokens_screen

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.cerebus.tokens.feature.tokens_feature.R

/**
 * [TokenView] - a class for tokens. It draws imageView and circle inside it
 *
 * @see TokensFragment for example
 *
 * @author Anastasia Drogunova
 * @since 23.05.2023
 */
class TokenView : AppCompatImageButton {

    private var notCheckedColor = context.getColor(R.color.baseColor)
    private var checkedColor = Color.RED

    private var isChecked = false
    private var isAnimated = false
    private var width = 0f
    private var height = 0f

    private val paint = Paint().apply { color = notCheckedColor }

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val attrsMap = mapOf(
            R.attr.tokenHeight to
                    fun(a: TypedArray, index: Int) {
                        height = a.getDimension(index, 40f)
                    },
            R.attr.tokenWidth to
                    fun(a: TypedArray, index: Int) {
                        width = a.getDimension(index, 40f)
                    },
            R.attr.notSelectedColor to
                    fun(a: TypedArray, index: Int) {
                        notCheckedColor = ContextCompat.getColor(
                            context,
                            a.getResourceId(index, android.R.color.darker_gray)
                        )
                    },
            R.attr.selectedColor to
                    fun(a: TypedArray, index: Int) {
                        checkedColor = ContextCompat.getColor(
                            context,
                            a.getResourceId(index, android.R.color.holo_red_light)
                        )
                    },
            R.attr.isChecked to
                    fun(a: TypedArray, index: Int) {
                        isChecked = a.getBoolean(index, false)
                    },
            R.attr.isAnimated to
                    fun (a: TypedArray, index: Int) {
                        isAnimated = a.getBoolean(index, false)
                    },
        )
        setupAttrs(context, attrs, attrsMap)
    }

    fun setChecked() {
        isChecked = true
        paint.color = checkedColor
        invalidate()
    }

    fun setUnchecked() {
        isChecked = false
        paint.color = notCheckedColor
        invalidate()
    }

    fun getIsChecked() = isChecked

    fun setUncheckedColor(@ColorInt newColorRes: Int) {
        notCheckedColor = newColorRes
    }

    fun setCheckedColor(@ColorInt newColorRes: Int) {
        checkedColor = newColorRes
    }

    fun getCheckedColor() = checkedColor

    fun setIsAnimated(isAnimate: Boolean) {
        isAnimated = isAnimate
    }

    fun getIsAnimated() = isAnimated

    /**
     * Attributes setting
     */
    private fun setupAttrs(context: Context, attrsSet: AttributeSet?, attrsMap: Map<Int, (TypedArray, Int) -> Unit>){
        val sortedMap = attrsMap.toSortedMap()
        val set = sortedMap.keys.toIntArray().apply { sort() }

        /** extract out attribute from your layout xml **/
        val attrs = context.obtainStyledAttributes(attrsSet, set)

        (0  until attrs.indexCount)
            .map(attrs::getIndex)
            .forEach {
                sortedMap[set[it]]?.invoke(attrs, it)
            }
        attrs.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(height/2, width/2, height/2, paint)
    }
}
