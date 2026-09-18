package com.cerebus.tokens.core.ui.theme

import androidx.compose.ui.unit.dp

/** Shared spacing; token sizing and adaptive board rules belong to tokens_feature. */
object TokensDimensions {
    val SmallSpacing = SMALL_SPACING_DP.dp
    val MediumSpacing = MEDIUM_SPACING_DP.dp
    val ContentPadding = CONTENT_PADDING_DP.dp
    val CornerRadius = CORNER_RADIUS_DP.dp
    val MinimumTouchTarget = MINIMUM_TOUCH_TARGET_DP.dp

    private const val SMALL_SPACING_DP = 8
    private const val MEDIUM_SPACING_DP = 16
    private const val CONTENT_PADDING_DP = 20
    private const val CORNER_RADIUS_DP = 16
    private const val MINIMUM_TOUCH_TARGET_DP = 48
}
