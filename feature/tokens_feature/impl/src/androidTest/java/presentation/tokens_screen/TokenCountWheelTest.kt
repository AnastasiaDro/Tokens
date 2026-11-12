package presentation.tokens_screen

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cerebus.tokens.core.ui.theme.TokensTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenCountWheelTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun useLandscape() {
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(ORIENTATION_TIMEOUT_MS) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
    }

    @Test fun releasingSlowDragKeepsHighlightedNumberInLandscape() =
        releaseSlowDrag(INITIAL_COUNT, NEXT_COUNT, -DRAG_ITEM_FRACTION)

    @Test fun releasingReverseDragKeepsHighlightedNumberInLandscape() =
        releaseSlowDrag(NEXT_COUNT, INITIAL_COUNT, DRAG_ITEM_FRACTION)

    @Test fun releasingSmallDragKeepsOriginalNumberInLandscape() =
        releaseSlowDrag(INITIAL_COUNT, INITIAL_COUNT, -SMALL_DRAG_ITEM_FRACTION)

    private fun releaseSlowDrag(initialCount: Int, expectedCount: Int, itemFraction: Float) {
        val state = mutableStateOf(SelectTokensNumberUiState(count = initialCount))
        compose.setContent {
            TokensTheme {
                Box(Modifier.requiredSize(LANDSCAPE_WIDTH_DP.dp, LANDSCAPE_HEIGHT_DP.dp), contentAlignment = Alignment.Center) {
                    SelectTokensNumberScreen(state.value, { state.value = state.value.copy(count = it) }, {}, {})
                }
            }
        }
        val wheel = compose.onNodeWithTag(COUNT_WHEEL_TAG)
        val itemHeight = wheel.fetchSemanticsNode().size.height / VISIBLE_ITEMS
        wheel.performTouchInput {
            val startFraction = if (itemFraction < NO_HORIZONTAL_MOVE) DRAG_START_FRACTION else REVERSE_DRAG_START_FRACTION
            down(Offset(centerX, height * startFraction))
            moveBy(Offset(NO_HORIZONTAL_MOVE, itemHeight * itemFraction), DRAG_DURATION_MS)
            advanceEventTime(RELEASE_PAUSE_MS)
        }
        compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(expectedCount.toString())
        wheel.performTouchInput { up() }
        compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(expectedCount.toString())
        compose.runOnIdle { assertEquals(expectedCount, state.value.count) }
    }

    private companion object {
        const val INITIAL_COUNT = 13
        const val NEXT_COUNT = 14
        const val LANDSCAPE_WIDTH_DP = 600
        const val LANDSCAPE_HEIGHT_DP = 320
        const val VISIBLE_ITEMS = 3f
        const val DRAG_START_FRACTION = 0.9f
        const val REVERSE_DRAG_START_FRACTION = 0.1f
        const val DRAG_ITEM_FRACTION = 1.1f
        const val SMALL_DRAG_ITEM_FRACTION = 0.35f
        const val NO_HORIZONTAL_MOVE = 0f
        const val DRAG_DURATION_MS = 600L
        const val RELEASE_PAUSE_MS = 200L
        const val ORIENTATION_TIMEOUT_MS = 5_000L
    }
}
