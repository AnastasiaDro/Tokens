package com.cerebus.tokens

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationPolicyTest {
    @Test
    fun tokenBoardOnPhoneRequestsLandscape() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            isTokenBoard = true,
            smallestScreenWidthDp = PHONE_SMALLEST_WIDTH_DP,
        )
    }

    @Test
    fun settingsOnPhoneRequestsLandscape() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            isTokenBoard = false,
            isSettings = true,
            smallestScreenWidthDp = PHONE_SMALLEST_WIDTH_DP,
        )
    }

    @Test
    fun unrelatedScreenOnPhoneLeavesOrientationToSystem() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            isTokenBoard = false,
            isSettings = false,
            smallestScreenWidthDp = PHONE_SMALLEST_WIDTH_DP,
        )
    }

    @Test
    fun sizeImmediatelyBelowLargeScreenBoundaryRequestsLandscape() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            isTokenBoard = true,
            smallestScreenWidthDp = LAST_PHONE_SMALLEST_WIDTH_DP,
        )
    }

    @Test
    fun largeScreenBoundaryLeavesOrientationToSystem() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            isTokenBoard = true,
            smallestScreenWidthDp = LARGE_SCREEN_SMALLEST_WIDTH_DP,
        )
    }

    @Test
    fun sizeAboveLargeScreenBoundaryLeavesOrientationToSystem() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            isTokenBoard = true,
            smallestScreenWidthDp = TABLET_SMALLEST_WIDTH_DP,
        )
    }

    @Test
    fun multiWindowLeavesOrientationToSystem() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            isTokenBoard = true,
            smallestScreenWidthDp = PHONE_SMALLEST_WIDTH_DP,
            isInMultiWindowMode = true,
        )
    }

    @Test
    fun undefinedSizeLeavesOrientationToSystem() {
        assertOrientation(
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            isTokenBoard = true,
            smallestScreenWidthDp = Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED,
        )
    }

    private fun assertOrientation(
        expected: Int,
        isTokenBoard: Boolean,
        isSettings: Boolean = false,
        smallestScreenWidthDp: Int,
        isInMultiWindowMode: Boolean = false,
    ) {
        assertEquals(
            expected,
            tokensRequestedOrientation(
                isTokenBoard = isTokenBoard,
                isSettings = isSettings,
                smallestScreenWidthDp = smallestScreenWidthDp,
                isInMultiWindowMode = isInMultiWindowMode,
            ),
        )
    }

    private companion object {
        const val PHONE_SMALLEST_WIDTH_DP = 411
        const val LAST_PHONE_SMALLEST_WIDTH_DP = 599
        const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600
        const val TABLET_SMALLEST_WIDTH_DP = 840
    }
}
