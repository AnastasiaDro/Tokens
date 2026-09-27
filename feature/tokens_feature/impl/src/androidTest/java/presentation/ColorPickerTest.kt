package presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cerebus.tokens.feature.tokens_feature.R
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.core.ui.theme.TokensTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import presentation.settings_screen.*
import presentation.state.SaveState
import com.cerebus.tokens.core.ui.R as CoreR

@RunWith(AndroidJUnit4::class)
class ColorPickerTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun initialDarkColorAndRecompositionDoNotEmitAndFirstTouchKeepsBrightness() {
        val state = mutableStateOf(ColorUiState(color = DARK_COLOR, loading = false))
        val selections = mutableListOf<Int>()
        compose.setContent {
            TokensTheme {
                SelectColorScreen(state.value, {
                    selections += it
                    state.value = state.value.copy(color = it)
                }, {}, {}, {})
            }
        }
        compose.runOnIdle {
            assertTrue(selections.isEmpty())
            state.value = state.value.copy(save = SaveState.ERROR)
        }
        compose.runOnIdle { assertTrue(selections.isEmpty()) }
        reveal(COLOR_PICKER_TAG).performTouchInput {
            click(Offset(width * PICKER_X_FRACTION, height * PICKER_Y_FRACTION))
        }
        compose.runOnIdle {
            assertFalse(selections.isEmpty())
            assertNotEquals(DARK_COLOR, state.value.color)
            val hsv = FloatArray(HSV_COMPONENT_COUNT)
            android.graphics.Color.colorToHSV(state.value.color!!, hsv)
            assertEquals(DARK_BRIGHTNESS, hsv[BRIGHTNESS_INDEX], CHANNEL_TOLERANCE)
        }
        val selected = state.value.color
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.SAVING) }
        reveal(COLOR_PICKER_TAG).assertIsDisplayed().assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.ERROR) }
        reveal(COLOR_PICKER_TAG).assertIsDisplayed()
        compose.runOnIdle { assertEquals(selected, state.value.color) }
    }

    @Test fun brightnessChangesDraftAndPreviewWithoutConfirmation() {
        val state = mutableStateOf(ColorUiState(color = RED_COLOR, loading = false))
        val confirmations = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme { SelectColorScreen(state.value, { state.value = state.value.copy(color = it) },
                { confirmations += Unit }, {}, {}) }
        }
        compose.onNodeWithTag(COLOR_BRIGHTNESS_TAG).performScrollTo().performTouchInput { click(center) }
        compose.runOnIdle {
            assertNotEquals(RED_COLOR, state.value.color)
            assertTrue(confirmations.isEmpty())
        }
        assertEquals(state.value.color, previewColor())
    }

    @Test fun quickColorChangesDraftAndMarksSelectedSwatchWithoutConfirmation() {
        val state = mutableStateOf(ColorUiState(color = RED_COLOR, loading = false))
        val confirmations = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme { SelectColorScreen(state.value, { state.value = state.value.copy(color = it) },
                { confirmations += Unit }, {}, {}) }
        }
        compose.onNodeWithTag(quickColorTag(ORANGE_SWATCH_INDEX)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(ORANGE_COLOR, state.value.color)
            assertTrue(confirmations.isEmpty())
        }
        compose.onNodeWithTag(quickColorTag(ORANGE_SWATCH_INDEX)).assertIsSelected()
    }

    @Test fun loadingAndReadFailureHidePickerWhileSavingKeepsItDisabled() {
        val state = mutableStateOf(ColorUiState())
        val retries = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme { SelectColorScreen(state.value, {}, {}, {}, { retries += Unit }) }
        }
        compose.onNodeWithTag(COLOR_PICKER_TAG).assertDoesNotExist()
        compose.onNodeWithTag(COLOR_CONFIRM_TAG).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(CoreR.string.cancel)).assertIsEnabled()
        compose.runOnIdle { state.value = ColorUiState(color = RED_COLOR, loading = false, readError = true, save = SaveState.ERROR) }
        compose.onNodeWithText(context.getString(CoreR.string.storage_read_error)).performClick()
        compose.onNodeWithText(context.getString(CoreR.string.storage_save_error)).assertDoesNotExist()
        compose.onNodeWithTag(COLOR_CONFIRM_TAG).assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf(Unit), retries)
            state.value = state.value.copy(readError = false, save = SaveState.SAVING)
        }
        reveal(COLOR_PICKER_TAG).assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText(context.getString(CoreR.string.cancel)).assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.ERROR) }
        reveal(COLOR_PICKER_TAG).assertIsDisplayed()
        compose.onNodeWithTag(COLOR_CONFIRM_TAG).assertIsEnabled()
    }


    @Test fun recentColorsAreUniqueLimitedAndSelectableAndEmptyHistoryStaysVisible() {
        val state = mutableStateOf(ColorUiState(color = RED_COLOR, loading = false))
        compose.setContent {
            TokensTheme { SelectColorScreen(state.value, { state.value = state.value.copy(color = it) }, {}, {}, {}) }
        }
        compose.onNodeWithText(context.getString(R.string.no_recent_colors)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(QUICK_COLOR_SWATCHES_TAG).onChildren().assertCountEquals(SWATCH_COUNT)
        compose.runOnIdle { state.value = state.value.copy(recentColors = listOf(ORANGE_COLOR) + TEST_COLORS + TEST_COLORS + DARK_COLOR) }
        compose.onNodeWithText(context.getString(R.string.no_recent_colors)).assertDoesNotExist()
        compose.onNodeWithTag(RECENT_COLOR_SWATCHES_TAG).onChildren().assertCountEquals(SWATCH_COUNT)
        compose.onNodeWithTag(recentColorTag(FIRST_SWATCH)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ORANGE_COLOR, state.value.color) }
        compose.onNodeWithTag(recentColorTag(FIRST_SWATCH)).assertIsSelected()
    }

    @Test fun savingBlocksBrightnessWheelSwatchesAndCloseWithoutMovingActions() {
        val state = mutableStateOf(ColorUiState(color = DARK_COLOR, loading = false))
        val selections = mutableListOf<Int>()
        val dismissals = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme {
                SelectColorScreen(state.value, { selections += it; state.value = state.value.copy(color = it) },
                    {}, { dismissals += Unit }, {})
            }
        }
        val actionsBefore = compose.onNodeWithTag(COLOR_ACTIONS_TAG).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.SAVING) }
        compose.onNodeWithTag(COLOR_BRIGHTNESS_TAG).performScrollTo().assertIsNotEnabled().performTouchInput { click(center) }
        reveal(COLOR_PICKER_TAG).performTouchInput { click(center) }
        compose.onNodeWithTag(quickColorTag(ORANGE_SWATCH_INDEX)).performScrollTo().assertIsNotEnabled().performTouchInput { click(center) }
        compose.onNodeWithTag(COLOR_CLOSE_TAG).assertIsNotEnabled().performTouchInput { click(center) }
        compose.runOnIdle {
            assertTrue(selections.isEmpty())
            assertTrue(dismissals.isEmpty())
        }
        assertEquals(actionsBefore, compose.onNodeWithTag(COLOR_ACTIONS_TAG).fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.ERROR) }
        reveal(COLOR_PICKER_TAG).performTouchInput {
            click(Offset(width * PICKER_X_FRACTION, height * PICKER_Y_FRACTION))
        }
        compose.runOnIdle {
            val hsv = FloatArray(HSV_COMPONENT_COUNT)
            android.graphics.Color.colorToHSV(state.value.color!!, hsv)
            assertEquals(DARK_BRIGHTNESS, hsv[BRIGHTNESS_INDEX], CHANNEL_TOLERANCE)
        }
    }

    @Test fun actionsRemainVisibleAcrossLandscapeNarrowInsetsAndLargeFont() {
        val cases = listOf(
            LayoutCase(PHONE_WIDTH, PHONE_HEIGHT),
            LayoutCase(900, NARROW_WIDTH),
            LayoutCase(PHONE_WIDTH, PHONE_HEIGHT, sideInset = 28),
            LayoutCase(NARROW_WIDTH, 560),
            LayoutCase(720, 240, fontScale = 1.8f),
        )
        val layout = mutableStateOf(cases.first())
        compose.setContent {
            val current = layout.value
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY, current.fontScale)) {
                TokensTheme {
                    // Replace device insets with a controlled safe region; no global device settings are changed.
                    Box(Modifier.size(current.width.dp, current.height.dp)
                        .consumeWindowInsets(WindowInsets.safeDrawing)
                        .windowInsetsPadding(WindowInsets(left = current.sideInset, right = current.sideInset))) {
                        Box(Modifier.fillMaxSize().testTag(SAFE_REGION_TAG)) {
                            SelectColorScreen(ColorUiState(color = RED_COLOR, loading = false, recentColors = TEST_COLORS),
                                {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        cases.forEach { case ->
            compose.runOnIdle { layout.value = case }
            val safeBounds = compose.onNodeWithTag(SAFE_REGION_TAG).fetchSemanticsNode().boundsInRoot
            val dialog = compose.onNodeWithTag(COLOR_DIALOG_TAG).fetchSemanticsNode().boundsInRoot
            assertEquals(safeBounds.center.x, dialog.center.x, BOUNDS_TOLERANCE)
            listOf(COLOR_CONFIRM_TAG, COLOR_CLOSE_TAG, COLOR_ACTIONS_TAG).forEach { tag ->
                val node = compose.onNodeWithTag(tag).assertIsDisplayed()
                node.assert(hasAnyAncestor(hasScrollAction()).not())
                val bounds = node.fetchSemanticsNode().boundsInRoot
                assertTrue("$case: $tag outside safe region", bounds.left >= safeBounds.left &&
                    bounds.right <= safeBounds.right && bounds.top >= safeBounds.top && bounds.bottom <= safeBounds.bottom)
                assertTrue(bounds.height > ZERO_SIZE)
            }
            if (case == cases.first()) {
                compose.onNodeWithTag(recentColorTag(SWATCH_COUNT - SINGLE_ITEM)).assertIsDisplayed()
                compose.onNodeWithTag(quickColorTag(SWATCH_COUNT - SINGLE_ITEM)).assertIsDisplayed()
                val palette = compose.onNodeWithTag(COLOR_PICKER_TAG).fetchSemanticsNode().boundsInRoot
                val brightness = compose.onNodeWithTag(COLOR_BRIGHTNESS_TAG).fetchSemanticsNode().boundsInRoot
                assertTrue(palette.right <= brightness.left)
            }
        }
    }

    private fun reveal(tag: String): SemanticsNodeInteraction {
        val node = compose.onNodeWithTag(tag)
        if (compose.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
            node.performScrollTo()
        }
        return node
    }

    @Test fun changingLayoutKeepsPresetBrightnessWhenWheelIsUsedAgain() {
        val state = mutableStateOf(ColorUiState(color = DARK_COLOR, loading = false))
        val layoutWidth = mutableStateOf(NARROW_WIDTH)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(layoutWidth.value.dp, PHONE_HEIGHT.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                        SelectColorScreen(state.value, { state.value = state.value.copy(color = it) }, {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithTag(quickColorTag(ORANGE_SWATCH_INDEX)).performScrollTo().performClick()
        compose.runOnIdle { layoutWidth.value = PHONE_WIDTH }
        reveal(COLOR_PICKER_TAG).performTouchInput {
            click(Offset(width * PICKER_X_FRACTION, height * PICKER_Y_FRACTION))
        }
        compose.runOnIdle {
            val hsv = FloatArray(HSV_COMPONENT_COUNT)
            android.graphics.Color.colorToHSV(state.value.color!!, hsv)
            assertEquals(FULL_BRIGHTNESS, hsv[BRIGHTNESS_INDEX], CHANNEL_TOLERANCE)
        }
    }

    private fun previewColor(): Int = compose.onNodeWithTag(COLOR_PREVIEW_TAG).fetchSemanticsNode()
        .config[SemanticsProperties.StateDescription].removePrefix("#").toLong(HEX_RADIX).toInt()

    private data class LayoutCase(val width: Int, val height: Int, val sideInset: Int = 0, val fontScale: Float = 1f)

    private companion object {
        const val DARK_COLOR = -14663584
        const val RED_COLOR = -65536
        const val ORANGE_COLOR = -26624
        const val ORANGE_SWATCH_INDEX = 1
        const val PICKER_X_FRACTION = 0.3f
        const val PICKER_Y_FRACTION = 0.3f
        const val DARK_BRIGHTNESS = 96f / 255f
        const val CHANNEL_TOLERANCE = 0.01f
        const val HSV_COMPONENT_COUNT = 3
        const val BRIGHTNESS_INDEX = 2
        const val HEX_RADIX = 16
        const val SWATCH_COUNT = 6
        const val FIRST_SWATCH = 0
        const val SINGLE_ITEM = 1
        const val ZERO_SIZE = 0f
        const val TEST_DENSITY = 1f
        const val BOUNDS_TOLERANCE = 1f
        const val SAFE_REGION_TAG = "color-safe-region"
        const val PHONE_WIDTH = 640
        const val PHONE_HEIGHT = 280
        const val NARROW_WIDTH = 320
        const val FULL_BRIGHTNESS = 1f
        val TEST_COLORS = listOf(RED_COLOR, ORANGE_COLOR, -256, -16711936, -16776961, -10011977)
    }
}
