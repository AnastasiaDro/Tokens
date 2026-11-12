package com.cerebus.tokens.core.ui.theme

import androidx.compose.ui.unit.dp

/** Shared spacing; token sizing and adaptive board rules belong to tokens_feature. */
object TokensDimensions {
    val SmallSpacing = SMALL_SPACING_DP.dp
    val MediumSpacing = MEDIUM_SPACING_DP.dp
    val ContentPadding = CONTENT_PADDING_DP.dp
    val CornerRadius = CORNER_RADIUS_DP.dp
    val MinimumTouchTarget = MINIMUM_TOUCH_TARGET_DP.dp
    val MenuWidth = MENU_WIDTH_DP.dp
    val MenuItemMinHeight = MENU_ITEM_MIN_HEIGHT_DP.dp
    val MenuHorizontalPadding = MENU_HORIZONTAL_PADDING_DP.dp
    val MenuVerticalPadding = MENU_VERTICAL_PADDING_DP.dp
    val MenuCornerRadius = MENU_CORNER_RADIUS_DP.dp
    val MenuElevation = MENU_ELEVATION_DP.dp
    val MenuDividerThickness = MENU_DIVIDER_THICKNESS_DP.dp

    private const val SMALL_SPACING_DP = 8
    private const val MEDIUM_SPACING_DP = 16
    private const val CONTENT_PADDING_DP = 12
    private const val CORNER_RADIUS_DP = 16
    private const val MINIMUM_TOUCH_TARGET_DP = 48
    private const val MENU_WIDTH_DP = 196
    private const val MENU_ITEM_MIN_HEIGHT_DP = 48
    private const val MENU_HORIZONTAL_PADDING_DP = 16
    private const val MENU_VERTICAL_PADDING_DP = 4
    private const val MENU_CORNER_RADIUS_DP = 12
    private const val MENU_ELEVATION_DP = 8
    private const val MENU_DIVIDER_THICKNESS_DP = 1
}
