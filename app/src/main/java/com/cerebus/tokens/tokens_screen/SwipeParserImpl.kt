package com.cerebus.tokens.tokens_screen

import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 * [SwipeParserImpl] - a realisation of [SwipeParser]
 * is used at the [TokensFragment] for the clearing of tokens
 *
 * @see SwipeParser
 * @see TokensFragment
 *
 * @author Anastasia Drogunova
 * @since 14.07.2023
 */
class SwipeParserImpl(private val screenTag: String): SwipeParser {

    private var xStart = 0f
    private var xEnd = 0f

    override fun onSwipeHorizontal(view: View, event: MotionEvent): Boolean {
        val validDistance = view.width / 3
        when (event.action) {
            MotionEvent.ACTION_DOWN -> xStart = event.x
            MotionEvent.ACTION_UP ->  {
                xEnd = event.x
                if (xStart - xEnd > validDistance) {
                    Log.d(screenTag, "Right to left swipe occurred")
                    return true
                }
            }
        }
        return false
    }
}