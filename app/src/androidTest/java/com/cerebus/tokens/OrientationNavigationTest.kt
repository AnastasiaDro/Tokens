package com.cerebus.tokens

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.core.ui.R as CoreR
import com.cerebus.tokens.feature.tokens_feature.R as TokensR
import com.cerebus.tokens.feature.tokens_feature.api.TokensBoard
import com.cerebus.tokens.feature.tokens_feature.api.TokensEntry
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator
import com.cerebus.tokens.feature.tokens_feature.api.TokensSettings
import com.cerebus.tokens.reinforcement_photo.api.PhotoDestination
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class OrientationNavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val koin get() = GlobalContext.get()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun boardAndSettingsUsePhoneLandscape() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForRoute<TokensBoard>(scenario)
            val boardOrientation = scenario.expectedBoardOrientation()
            waitForRequestedOrientation(scenario, boardOrientation)
            if (boardOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                waitForConfigurationOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE)
            }

            scenario.onActivity {
                koin.get<TokensMediator>().open(it.navController, TokensEntry.SETTINGS)
            }
            waitForRoute<TokensSettings>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)

            scenario.onActivity { assertTrue(it.navController.popBackStack()) }
            waitForRoute<TokensBoard>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)
        }
    }

    @Test
    fun photoDialogInheritsOrientationFromUnderlyingScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForRoute<TokensBoard>(scenario)
            val boardOrientation = scenario.expectedBoardOrientation()
            waitForRequestedOrientation(scenario, boardOrientation)

            openPhoto(scenario)
            waitForRoute<PhotoDestination>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)

            scenario.onActivity {
                assertTrue(it.navController.popBackStack())
                koin.get<TokensMediator>().open(it.navController, TokensEntry.SETTINGS)
            }
            waitForRoute<TokensSettings>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)

            openPhoto(scenario)
            waitForRoute<PhotoDestination>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)
        }
    }

    @Test
    fun featureDialogsKeepUnderlyingOrientationPolicy() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForRoute<TokensBoard>(scenario)
            val boardOrientation = scenario.expectedBoardOrientation()
            compose.onNodeWithContentDescription(context.getString(TokensR.string.tokens_menu)).performClick()
            compose.onNodeWithText(context.getString(TokensR.string.changeChips)).performClick()
            compose.onNodeWithText(context.getString(TokensR.string.quick_count_selection))
                .fetchSemanticsNode()
            waitForRequestedOrientation(scenario, boardOrientation)
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).performClick()

            scenario.onActivity {
                koin.get<TokensMediator>().open(it.navController, TokensEntry.SETTINGS)
            }
            waitForRoute<TokensSettings>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)

            compose.onNodeWithText(context.getString(CoreR.string.change)).performScrollTo().performClick()
            compose.onNodeWithText(context.getString(TokensR.string.quick_count_selection))
                .fetchSemanticsNode()
            waitForRequestedOrientation(scenario, boardOrientation)
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).performClick()

            compose.onNodeWithText(context.getString(TokensR.string.select_button_text))
                .performScrollTo().performClick()
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).performScrollTo().fetchSemanticsNode()
            waitForRequestedOrientation(scenario, boardOrientation)
        }
    }

    @Test
    fun recreationKeepsDestinationAndOrientationPolicy() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForRoute<TokensBoard>(scenario)
            val boardOrientation = scenario.expectedBoardOrientation()
            scenario.recreate()
            waitForRoute<TokensBoard>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)

            scenario.onActivity {
                koin.get<TokensMediator>().open(it.navController, TokensEntry.SETTINGS)
            }
            waitForRoute<TokensSettings>(scenario)
            scenario.recreate()
            waitForRoute<TokensSettings>(scenario)
            waitForRequestedOrientation(scenario, boardOrientation)
        }
    }

    private fun openPhoto(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity {
            koin.get<ReinforcementPhotoMediator>().open(it.navController)
        }
    }

    private inline fun <reified T : Any> waitForRoute(scenario: ActivityScenario<MainActivity>) {
        compose.waitUntil(TIMEOUT_MS) {
            var hasExpectedRoute = false
            scenario.onActivity {
                hasExpectedRoute = it.navController.currentDestination?.hasRoute<T>() == true
            }
            hasExpectedRoute
        }
    }

    private fun waitForRequestedOrientation(
        scenario: ActivityScenario<MainActivity>,
        expectedOrientation: Int,
    ) {
        compose.waitUntil(TIMEOUT_MS) {
            var requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            scenario.onActivity { requestedOrientation = it.requestedOrientation }
            requestedOrientation == expectedOrientation
        }
    }

    private fun waitForConfigurationOrientation(
        scenario: ActivityScenario<MainActivity>,
        expectedOrientation: Int,
    ) {
        compose.waitUntil(TIMEOUT_MS) {
            var orientation = Configuration.ORIENTATION_UNDEFINED
            scenario.onActivity { orientation = it.resources.configuration.orientation }
            orientation == expectedOrientation
        }
    }

    private fun ActivityScenario<MainActivity>.expectedBoardOrientation(): Int {
        var expectedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onActivity {
            val smallestScreenWidthDp = it.resources.configuration.smallestScreenWidthDp
            val isPhoneFullScreen = smallestScreenWidthDp !=
                Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED &&
                smallestScreenWidthDp < LARGE_SCREEN_SMALLEST_WIDTH_DP &&
                !it.isInMultiWindowMode
            if (isPhoneFullScreen) {
                expectedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        return expectedOrientation
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600
    }
}
