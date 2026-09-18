package presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
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
        compose.onNodeWithTag(COLOR_PICKER_TAG).performScrollTo().performTouchInput {
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
        compose.onNodeWithTag(COLOR_PICKER_TAG).assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.ERROR) }
        compose.onNodeWithTag(COLOR_PICKER_TAG).performScrollTo().assertIsDisplayed()
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
        val pixels = compose.onNodeWithTag(COLOR_PREVIEW_TAG).performScrollTo().captureToImage().toPixelMap()
        assertEquals(state.value.color, pixels[pixels.width / HALF_DIVISOR, pixels.height / HALF_DIVISOR].toArgb())
    }

    @Test fun loadingReadFailureAndSavingDoNotMountPickerOrAllowConfirmation() {
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
        compose.onNodeWithTag(COLOR_PICKER_TAG).assertDoesNotExist()
        compose.onNodeWithText(context.getString(CoreR.string.cancel)).assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.ERROR) }
        compose.onNodeWithTag(COLOR_PICKER_TAG).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(COLOR_CONFIRM_TAG).performScrollTo().assertIsEnabled()
    }

    private companion object {
        const val DARK_COLOR = -14663584
        const val RED_COLOR = -65536
        const val PICKER_X_FRACTION = 0.3f
        const val PICKER_Y_FRACTION = 0.3f
        const val DARK_BRIGHTNESS = 96f / 255f
        const val CHANNEL_TOLERANCE = 0.01f
        const val HSV_COMPONENT_COUNT = 3
        const val BRIGHTNESS_INDEX = 2
        const val HALF_DIVISOR = 2
    }
}
