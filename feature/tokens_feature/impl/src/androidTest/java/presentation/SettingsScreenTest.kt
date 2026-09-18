package presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import com.cerebus.tokens.feature.tokens_feature.R
import domain.repository.EffectsSettings
import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import presentation.settings_screen.SettingsScreen
import presentation.settings_screen.SettingsUiState
import presentation.settings_screen.TokenSettingsState
import presentation.state.SaveState
import presentation.tokens_screen.COUNT_VALUE_TAG
import presentation.tokens_screen.SelectTokensNumberScreen
import presentation.tokens_screen.SelectTokensNumberUiState
import com.cerebus.tokens.core.ui.R as CoreR

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun switchRendersStateWithoutWritingAndClickOnlyDelegates() {
        val state = mutableStateOf(ready())
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            TokensTheme {
                SettingsScreen(state.value, onSelectCount = {}, onSelectColor = {},
                    onAnimationChanged = {}, onSoundChanged = { changes += it }, onReinforcementChanged = {},
                    onRetry = {}, onYoutube = {}, onDonate = {})
            }
        }
        val sound = compose.onNodeWithText(context.getString(R.string.settings_sound))
        sound.performScrollTo().assertIsOn().performClick().assertIsOn()
        compose.runOnIdle {
            assertEquals(listOf(false), changes)
            state.value = state.value.copy(effects = EffectsSettings(sound = false))
        }
        sound.assertIsOff()
        compose.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test fun loadingReadFailureAndSavingDisableChangesAndRetryIsExplicit() {
        val state = mutableStateOf(SettingsUiState())
        val retries = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme {
                SettingsScreen(state.value, onSelectCount = {}, onSelectColor = {},
                    onAnimationChanged = {}, onSoundChanged = {}, onReinforcementChanged = {},
                    onRetry = { retries += Unit }, onYoutube = {}, onDonate = {})
            }
        }
        val change = compose.onNodeWithText(context.getString(CoreR.string.change))
        change.performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state.value = ready().copy(readFailure = true, writeFailure = true) }
        change.assertIsNotEnabled()
        compose.onNodeWithText(context.getString(CoreR.string.storage_read_error)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf(Unit), retries)
            state.value = ready().copy(saving = true)
        }
        change.performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state.value = ready().copy(writeFailure = true) }
        compose.onNodeWithText(context.getString(CoreR.string.storage_write_error)).performScrollTo().assertIsEnabled()
        change.assertIsEnabled()
    }

    @Test fun reinforcementSwitchUsesStoredSettingWithoutCameraCapability() {
        val changes = mutableListOf<Boolean>()
        compose.setContent { TokensTheme {
            SettingsScreen(ready().copy(reinforcement = ReinforcementSettings(enabled = true)),
                onSelectCount = {}, onSelectColor = {}, onAnimationChanged = {}, onSoundChanged = {},
                onReinforcementChanged = { changes += it }, onRetry = {}, onYoutube = {}, onDonate = {})
        } }
        compose.onNodeWithText(context.getString(R.string.reinforcement_image)).performScrollTo()
            .assertIsOn().assertIsEnabled().performClick().assertIsOn()
        compose.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test fun narrowLayoutWithLargeFontKeepsAboutLinksReachable() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, LARGE_FONT_SCALE)) {
                TokensTheme {
                    Box(Modifier.width(NARROW_WIDTH_DP.dp)) {
                        SettingsScreen(ready(), onSelectCount = {}, onSelectColor = {},
                            onAnimationChanged = {}, onSoundChanged = {}, onReinforcementChanged = {}, onRetry = {},
                            onYoutube = { clicks += YOUTUBE }, onDonate = { clicks += DONATE })
                    }
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.youtube_link)).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(context.getString(R.string.donate_link)).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf(YOUTUBE, DONATE), clicks) }
    }

    @Test fun countControlsRespectBoundsAndDelegateDraftWithoutSaving() {
        val state = mutableStateOf(SelectTokensNumberUiState(count = MIN_TOKEN_COUNT))
        val selections = mutableListOf<Int>()
        val confirmations = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme {
                SelectTokensNumberScreen(state.value, { selections += it }, { confirmations += Unit }, {})
            }
        }
        val decrease = compose.onNodeWithContentDescription(context.getString(R.string.decrease_tokens_count))
        val increase = compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count))
        decrease.assertIsNotEnabled()
        increase.performClick()
        compose.runOnIdle {
            assertEquals(listOf(MIN_TOKEN_COUNT + COUNT_STEP), selections)
            assertTrue(confirmations.isEmpty())
            state.value = state.value.copy(count = MAX_TOKEN_COUNT)
        }
        increase.assertIsNotEnabled()
        decrease.assertIsEnabled()
        compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(MAX_TOKEN_COUNT.toString())
    }

    @Test fun countSaveBlocksAllActionsAndErrorKeepsSelection() {
        val state = mutableStateOf(SelectTokensNumberUiState(count = SELECTED_COUNT, save = SaveState.SAVING))
        val confirmations = mutableListOf<Unit>()
        compose.setContent {
            TokensTheme { SelectTokensNumberScreen(state.value, {}, { confirmations += Unit }, {}) }
        }
        compose.onNodeWithText(context.getString(CoreR.string.OK)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(CoreR.string.cancel)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.decrease_tokens_count)).assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(save = SaveState.ERROR) }
        compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(SELECTED_COUNT.toString())
        compose.onNodeWithText(context.getString(CoreR.string.storage_save_error)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(CoreR.string.OK)).performClick()
        compose.runOnIdle { assertEquals(listOf(Unit), confirmations) }
    }

    private fun ready() = SettingsUiState(
        tokens = TokenSettingsState(SELECTED_COUNT, TOKEN_COLOR), effects = EffectsSettings(),
        reinforcement = ReinforcementSettings(), loading = false,
    )

    private companion object {
        const val SELECTED_COUNT = 5
        const val TOKEN_COLOR = -65536
        const val COUNT_STEP = 1
        const val LARGE_FONT_SCALE = 2f
        const val NARROW_WIDTH_DP = 320
        const val YOUTUBE = "youtube"
        const val DONATE = "donate"
    }
}
