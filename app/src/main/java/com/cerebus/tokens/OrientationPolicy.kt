package com.cerebus.tokens

import android.content.pm.ActivityInfo
import android.content.res.Configuration

internal fun tokenBoardRequestedOrientation(
    isTokenBoard: Boolean,
    smallestScreenWidthDp: Int,
    isInMultiWindowMode: Boolean,
): Int {
    val isPhoneSize = smallestScreenWidthDp != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED &&
        smallestScreenWidthDp < LARGE_SCREEN_SMALLEST_WIDTH_DP
    return if (isTokenBoard && isPhoneSize && !isInMultiWindowMode) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

private const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600
