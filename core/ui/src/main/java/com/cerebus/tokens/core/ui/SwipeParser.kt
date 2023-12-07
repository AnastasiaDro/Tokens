package com.cerebus.tokens.core.ui

import android.view.MotionEvent
import android.view.View

/**
 * [SwipeParser] - an interface for parsing swipes cases
 * in different places
 *
 * @see SwipeParserImpl
 *
 * @author Anastasia Drogunova
 * @since 14.07.2023
 */
interface SwipeParser {
    fun onSwipeHorizontal(view: View, event: MotionEvent): Boolean
}