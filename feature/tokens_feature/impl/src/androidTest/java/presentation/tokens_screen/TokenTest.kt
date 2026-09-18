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
        node.assertCenterColor(context.getColor(R.color.baseColor))

        compose.runOnIdle { state.value = state.value.copy(checked = true) }
        node.assertIsOn().assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, context.getString(R.string.token_checked),
        ))
        node.assertCenterColor(INITIAL_COLOR)

        compose.runOnIdle { state.value = state.value.copy(checked = false) }
        node.assertIsOff()
        node.assertCenterColor(context.getColor(R.color.baseColor))
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
        node.assertCenterColor(context.getColor(R.color.baseColor))
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

    private fun SemanticsNodeInteraction.assertCenterColor(expected: Int) {
        val pixels = captureToImage().toPixelMap()
        assertEquals(expected, pixels[pixels.width / CENTER_DIVISOR, pixels.height / CENTER_DIVISOR].toArgb())
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
        val INITIAL_COLOR = Color.Red.toArgb()
        val UPDATED_COLOR = Color.Blue.toArgb()
        val LARGE_SIZE = 96.dp
        val SMALL_SIZE = 32.dp
    }
}
