package com.cerebus.tokens.core.ui.theme

import androidx.compose.ui.graphics.Color

/** Shared application palette. The selected token color remains feature state. */
object TokensColors {
    val Background = Color(BACKGROUND_ARGB)
    val PrimaryText = Color(PRIMARY_TEXT_ARGB)
    val SecondaryText = Color(SECONDARY_TEXT_ARGB)
    val Action = Color(ACTION_ARGB)
    val PressedAction = Color(PRESSED_ACTION_ARGB)
    val SwitchCheckedTrack = Color(SWITCH_CHECKED_TRACK_ARGB)
    val SwitchUncheckedTrack = Color(SWITCH_UNCHECKED_TRACK_ARGB)
    val SwitchThumb = Color(SWITCH_THUMB_ARGB)
    val Divider = Color(DIVIDER_ARGB)
    val MenuSurface = Color(MENU_SURFACE_ARGB)
    val MenuPressed = Color(MENU_PRESSED_ARGB)

    // Compatibility aliases for existing consumers of the shared theme tokens.
    val Accent = SecondaryText
    val Link = Action
    val Text = PrimaryText
    val Surface = Background
    val BackgroundStart = Background
    val BackgroundEnd = Background

    private const val BACKGROUND_ARGB = 0xFFFFF8F4
    private const val PRIMARY_TEXT_ARGB = 0xFF1A1A1A
    private const val SECONDARY_TEXT_ARGB = 0xFF6B7280
    private const val ACTION_ARGB = 0xFFD9480F
    private const val PRESSED_ACTION_ARGB = 0xFFB9380A
    private const val SWITCH_CHECKED_TRACK_ARGB = 0xFF22C55E
    private const val SWITCH_UNCHECKED_TRACK_ARGB = 0xFFD1D5DB
    private const val SWITCH_THUMB_ARGB = 0xFFFFFFFF
    private const val DIVIDER_ARGB = 0xFFE5E7EB
    private const val MENU_SURFACE_ARGB = 0xFFFFFFFF
    private const val MENU_PRESSED_ARGB = 0xFFFCE8DF
}
