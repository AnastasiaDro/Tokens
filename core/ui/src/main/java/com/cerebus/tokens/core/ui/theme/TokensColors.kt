package com.cerebus.tokens.core.ui.theme

import androidx.compose.ui.graphics.Color

/** Shared legacy palette. Feature-specific token and switch colors stay in their feature. */
object TokensColors {
    val Accent = Color(ACCENT_ARGB)
    val Link = Color(LINK_ARGB)
    val Text = Color(TEXT_ARGB)
    val SecondaryText = Color(SECONDARY_TEXT_ARGB)
    val BackgroundStart = Color(BACKGROUND_START_ARGB)
    val BackgroundEnd = Color(BACKGROUND_END_ARGB)
    val Surface = Color.White

    private const val ACCENT_ARGB = 0xFF03DAC5
    private const val LINK_ARGB = 0xFF008B8B
    private const val TEXT_ARGB = 0xFF444444
    private const val SECONDARY_TEXT_ARGB = 0xFF777777
    private const val BACKGROUND_START_ARGB = 0xFFA9F0A9
    private const val BACKGROUND_END_ARGB = 0xFFADD8E6
}
