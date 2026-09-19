package presentation.tokens_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.core.ui.theme.TokensColors
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.feature.tokens_feature.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import presentation.state.TokenShape
import presentation.state.TokenState

@RunWith(AndroidJUnit4::class)
class TokenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun checkedStateControlsSemanticsAndFillInBothDirections() {
        val state = mutableStateOf(token())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            TokensTheme { Token(state.value, onClick = {}, modifier = Modifier.testTag(TOKEN_TAG)) }
        }
        val node = compose.onNodeWithTag(TOKEN_TAG)
        node.assertIsOff()
            .assertContentDescriptionEquals(context.getString(R.string.token_description))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription, context.getString(R.string.token_unchecked),
            ))
        node.assertCenterColor(TokensColors.SecondaryText.toArgb())

        compose.runOnIdle { state.value = state.value.copy(checked = true) }
        node.assertIsOn().assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, context.getString(R.string.token_checked),
        ))
        node.assertCenterColor(INITIAL_COLOR)

        compose.runOnIdle { state.value = state.value.copy(checked = false) }
        node.assertIsOff()
        node.assertCenterColor(TokensColors.SecondaryText.toArgb())
    }

    @Test
    fun colorChangesAreRenderedWithoutKeepingPreviousColor() {
        val state = mutableStateOf(token(checked = true))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            TokensTheme { Token(state.value, onClick = {}, modifier = Modifier.testTag(TOKEN_TAG)) }
        }
        val node = compose.onNodeWithTag(TOKEN_TAG)
        node.assertCenterColor(INITIAL_COLOR)
        compose.runOnIdle { state.value = state.value.copy(color = UPDATED_COLOR) }
        node.assertIsOn()
        node.assertCenterColor(UPDATED_COLOR)
        compose.runOnIdle { state.value = state.value.copy(checked = false, color = INITIAL_COLOR) }
        node.assertCenterColor(TokensColors.SecondaryText.toArgb())
        compose.runOnIdle { state.value = state.value.copy(checked = true) }
        node.assertCenterColor(INITIAL_COLOR)
    }

    @Test
    fun clickOnlyCallsParentAndDoesNotToggleLocalState() {
        val clicks = mutableListOf<String>()
        val state = token()
        compose.setContent {
            TokensTheme {
                Token(state, onClick = { clicks += state.id }, modifier = Modifier.testTag(TOKEN_TAG))
            }
        }
        val node = compose.onNodeWithTag(TOKEN_TAG)
        node.performClick().assertIsOff()
        compose.runOnIdle { assertEquals(listOf(TOKEN_ID), clicks) }
        node.performClick().assertIsOff()
        compose.runOnIdle { assertEquals(listOf(TOKEN_ID, TOKEN_ID), clicks) }
    }

    @Test
    fun pressScalesTokenWithoutRippleAndReturnsAfterRelease() {
        val clicks = mutableListOf<Unit>()
        val node = setPressContent(onClick = { clicks += Unit })
        val initialPixelCount = node.coloredPixelCount(INITIAL_COLOR)

        node.performTouchInput { down(center) }
        assertTrue(clicks.isEmpty())
        compose.mainClock.advanceTimeBy(PRESS_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS)
        val pressedPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        assertTrue(pressedPixelCount > initialPixelCount)
        assertCornersStayBackground(node)
        node.assertWidthIsEqualTo(TOKEN_SIZE).assertHeightIsEqualTo(TOKEN_SIZE)

        compose.mainClock.advanceTimeBy(HOLD_DURATION_MILLIS)
        assertEquals(pressedPixelCount, node.coloredPixelCount(INITIAL_COLOR))

        node.performTouchInput { up() }
        assertEquals(listOf(Unit), clicks)
        compose.mainClock.advanceTimeBy(RELEASE_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
    }

    @Test
    fun quickTapStillGrowsAfterReleaseAndCallsParentImmediately() {
        val clicks = mutableListOf<Unit>()
        val node = setPressContent(onClick = { clicks += Unit })
        val initialPixelCount = node.coloredPixelCount(INITIAL_COLOR)

        // Both events arrive before the next Compose frame, as with a very short tap.
        node.performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(Unit), clicks) }
        compose.mainClock.advanceTimeBy(EARLY_ANIMATION_MILLIS)
        val earlyPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        assertTrue(earlyPixelCount > initialPixelCount)
        compose.mainClock.advanceTimeBy(EARLY_ANIMATION_MILLIS)
        assertTrue(node.coloredPixelCount(INITIAL_COLOR) > earlyPixelCount)
        assertCornersStayBackground(node)

        compose.mainClock.advanceTimeBy(FULL_PULSE_SETTLE_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
        node.assertIsOn()
    }

    @Test
    fun newPressDuringReturnGrowsAgainAndDoesNotQueuePulses() {
        val clicks = mutableListOf<Unit>()
        val node = setPressContent(onClick = { clicks += Unit })
        val initialPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        node.performTouchInput { click() }
        compose.mainClock.advanceTimeBy(PRESS_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS)
        val returningPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        assertTrue(returningPixelCount > initialPixelCount)

        node.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(PRESS_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS)
        val heldPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        assertTrue(heldPixelCount > returningPixelCount)
        compose.mainClock.advanceTimeBy(FULL_PULSE_SETTLE_MILLIS)
        assertEquals(heldPixelCount, node.coloredPixelCount(INITIAL_COLOR))

        node.performTouchInput { up() }
        compose.runOnIdle { assertEquals(listOf(Unit, Unit), clicks) }
        compose.mainClock.advanceTimeBy(FULL_PULSE_SETTLE_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
    }

    @Test
    fun cancelledPressReturnsWithoutClick() {
        val clicks = mutableListOf<Unit>()
        val node = setPressContent(onClick = { clicks += Unit })
        val initialPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        node.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(EARLY_ANIMATION_MILLIS)
        assertTrue(node.coloredPixelCount(INITIAL_COLOR) > initialPixelCount)

        node.performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(RELEASE_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
        assertTrue(clicks.isEmpty())
    }

    @Test
    fun disabledTokenDoesNotAnimateOnHoldOrTap() {
        val clicks = mutableListOf<Unit>()
        val node = setPressContent(enabled = false, onClick = { clicks += Unit })
        val initialPixelCount = node.coloredPixelCount(INITIAL_COLOR)
        node.assertIsNotEnabled().performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(PRESS_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
        node.performTouchInput { up(); click() }
        compose.mainClock.advanceTimeBy(EARLY_ANIMATION_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
        compose.mainClock.advanceTimeBy(FULL_PULSE_SETTLE_MILLIS)
        assertEquals(initialPixelCount, node.coloredPixelCount(INITIAL_COLOR))
        assertTrue(clicks.isEmpty())
    }

    @Test
    fun keyedReorderKeepsStateAndClickBoundToTokenId() {
        val first = token()
        val second = token(checked = true).copy(id = OTHER_TOKEN_ID, color = UPDATED_COLOR)
        val tokens = mutableStateOf(listOf(first, second))
        val clicks = mutableListOf<String>()
        compose.setContent {
            TokensTheme {
                Row {
                    tokens.value.forEach { state ->
                        key(state.id) {
                            Token(state, onClick = { clicks += state.id }, modifier = Modifier.testTag(state.id))
                        }
                    }
                }
            }
        }
        compose.runOnIdle { tokens.value = listOf(second, first) }
        compose.onNodeWithTag(OTHER_TOKEN_ID).assertIsOn().performClick()
        compose.onNodeWithTag(TOKEN_ID).assertIsOff().performClick()
        compose.runOnIdle { assertEquals(listOf(OTHER_TOKEN_ID, TOKEN_ID), clicks) }
    }

    @Test
    fun disabledTokenIgnoresTouchAndCanBeEnabledByParent() {
        val enabled = mutableStateOf(false)
        val clicks = mutableListOf<String>()
        compose.setContent {
            TokensTheme {
                Token(token(), onClick = { clicks += TOKEN_ID }, enabled = enabled.value,
                    modifier = Modifier.testTag(TOKEN_TAG))
            }
        }
        val node = compose.onNodeWithTag(TOKEN_TAG)
        node.assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle {
            assertTrue(clicks.isEmpty())
            enabled.value = true
        }
        node.performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(TOKEN_ID), clicks) }
    }

    @Test
    fun circleScalesToParentSizeWithoutFillingItsCorners() {
        compose.setContent {
            TokensTheme {
                Box(Modifier.background(Color.White)) {
                    Token(token(checked = true), onClick = {},
                        modifier = Modifier.size(LARGE_SIZE).testTag(TOKEN_TAG))
                }
            }
        }
        val node = compose.onNodeWithTag(TOKEN_TAG)
        node.assertWidthIsEqualTo(LARGE_SIZE).assertHeightIsEqualTo(LARGE_SIZE)
        node.assertCenterColor(INITIAL_COLOR)
        val pixels = node.captureToImage().toPixelMap()
        assertEquals(Color.White.toArgb(), pixels[CORNER_OFFSET, CORNER_OFFSET].toArgb())
        assertEquals(INITIAL_COLOR, pixels[pixels.width / NEAR_EDGE_DIVISOR, pixels.height / CENTER_DIVISOR].toArgb())
    }

    @Test
    fun smallParentConstraintsDoNotCauseVisualOverflow() {
        compose.setContent {
            TokensTheme {
                Box(Modifier.size(SMALL_SIZE)) {
                    Token(token(checked = true), onClick = {}, modifier = Modifier.testTag(TOKEN_TAG))
                }
            }
        }
        val node = compose.onNodeWithTag(TOKEN_TAG)
        node.assertWidthIsEqualTo(SMALL_SIZE).assertHeightIsEqualTo(SMALL_SIZE)
        node.assertCenterColor(INITIAL_COLOR)
    }

    private fun setPressContent(
        enabled: Boolean = true,
        onClick: () -> Unit,
    ): SemanticsNodeInteraction {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TokensTheme {
                Box(Modifier.background(Color.White)) {
                    Token(
                        token(checked = true),
                        onClick = onClick,
                        enabled = enabled,
                        modifier = Modifier.size(TOKEN_SIZE).testTag(TOKEN_TAG),
                    )
                }
            }
        }
        compose.waitForIdle()
        return compose.onNodeWithTag(TOKEN_TAG)
    }

    private fun SemanticsNodeInteraction.assertCenterColor(expected: Int) {
        val pixels = captureToImage().toPixelMap()
        assertEquals(expected, pixels[pixels.width / CENTER_DIVISOR, pixels.height / CENTER_DIVISOR].toArgb())
    }

    private fun SemanticsNodeInteraction.coloredPixelCount(expected: Int): Int {
        val pixels = captureToImage().toPixelMap()
        var count = FIRST_PIXEL
        for (x in FIRST_PIXEL until pixels.width) {
            for (y in FIRST_PIXEL until pixels.height) {
                if (pixels[x, y].toArgb() == expected) {
                    count += PIXEL_STEP
                }
            }
        }
        assertTrue(count > FIRST_PIXEL)
        return count
    }

    private fun assertCornersStayBackground(node: SemanticsNodeInteraction) {
        val pixels = node.captureToImage().toPixelMap()
        val background = Color.White.toArgb()
        val lastPixel = pixels.width - PIXEL_STEP
        assertEquals(background, pixels[FIRST_PIXEL, FIRST_PIXEL].toArgb())
        assertEquals(background, pixels[lastPixel, FIRST_PIXEL].toArgb())
        assertEquals(background, pixels[FIRST_PIXEL, lastPixel].toArgb())
        assertEquals(background, pixels[lastPixel, lastPixel].toArgb())
    }

    private fun token(checked: Boolean = false) = TokenState(
        id = TOKEN_ID,
        shape = TokenShape.CIRCLE,
        color = INITIAL_COLOR,
        checked = checked,
    )

    private companion object {
        const val TOKEN_TAG = "token"
        const val TOKEN_ID = "first-token"
        const val OTHER_TOKEN_ID = "second-token"
        const val CENTER_DIVISOR = 2
        const val NEAR_EDGE_DIVISOR = 10
        const val CORNER_OFFSET = 1
        const val FIRST_PIXEL = 0
        const val PIXEL_STEP = 1
        const val PRESS_ANIMATION_DURATION_MILLIS = 100L
        const val RELEASE_ANIMATION_DURATION_MILLIS = 180L
        const val EARLY_ANIMATION_MILLIS = 48L
        const val ANIMATION_SETTLE_MILLIS = 100L
        const val FULL_PULSE_SETTLE_MILLIS =
            PRESS_ANIMATION_DURATION_MILLIS + RELEASE_ANIMATION_DURATION_MILLIS + ANIMATION_SETTLE_MILLIS
        const val HOLD_DURATION_MILLIS = 100L
        val INITIAL_COLOR = Color.Red.toArgb()
        val UPDATED_COLOR = Color.Blue.toArgb()
        val LARGE_SIZE = 96.dp
        val SMALL_SIZE = 32.dp
        val TOKEN_SIZE = 68.dp
    }
}
