package presentation.tokens_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import com.cerebus.tokens.feature.tokens_feature.R
import domain.repository.MAX_TOKEN_COUNT
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import presentation.state.BoardState
import presentation.state.TokenShape
import presentation.state.TokenState
import com.cerebus.tokens.core.ui.R as CoreR

@RunWith(AndroidJUnit4::class)
class TokensScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun allTenTokensFitSmallBoardAndClicksUseStableIdsAfterReorder() {
        val tokens = mutableStateOf(tokens())
        val clicks = mutableListOf<String>()
        compose.setContent { TokensTheme {
            TokenBoard(tokens.value, true, { clicks += it }, {}, Modifier.size(BOARD_WIDTH.dp, BOARD_HEIGHT.dp))
        } }
        val bounds = compose.onNodeWithTag(TOKEN_BOARD_TAG).fetchSemanticsNode().boundsInRoot
        val sizes = tokens.value.map { token ->
            val item = compose.onNodeWithTag(tokenTag(token.id)).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(item.left >= bounds.left && item.right <= bounds.right)
            assertTrue(item.top >= bounds.top && item.bottom <= bounds.bottom)
            item.size
        }
        assertEquals(SINGLE_SIZE, sizes.distinct().size)
        val selected = tokens.value.last()
        compose.runOnIdle { tokens.value = tokens.value.reversed() }
        compose.onNodeWithTag(tokenTag(selected.id)).performClick().assertIsOff()
        compose.runOnIdle { assertEquals(listOf(selected.id), clicks) }
    }

    @Test fun onlyLongRightToLeftSwipeClearsAndDoesNotToggleToken() {
        val clears = mutableListOf<Unit>()
        val clicks = mutableListOf<String>()
        val enabled = mutableStateOf(true)
        compose.setContent { TokensTheme {
            TokenBoard(tokens(), enabled.value, { clicks += it }, { clears += Unit },
                Modifier.size(BOARD_WIDTH.dp, BOARD_HEIGHT.dp))
        } }
        val board = compose.onNodeWithTag(TOKEN_BOARD_TAG)
        board.performTouchInput { swipeRight() }
        board.performTouchInput { swipe(Offset(width * SWIPE_START, centerY), Offset(width * SHORT_END, centerY)) }
        compose.runOnIdle { assertTrue(clears.isEmpty()); assertTrue(clicks.isEmpty()) }
        board.performTouchInput { swipeLeft() }
        compose.runOnIdle {
            assertEquals(listOf(Unit), clears)
            assertTrue(clicks.isEmpty())
            enabled.value = false
        }
        board.performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals(listOf(Unit), clears) }
    }

    @Test fun loadingAndReadFailureDisableTokensCountAndClearButNotSettings() {
        val state = mutableStateOf(TokensUiState())
        val settings = mutableListOf<Unit>()
        val retries = mutableListOf<Unit>()
        compose.setContent { TokensTheme {
            TokensScreen(state.value, true, {}, {}, { retries += Unit }, {}, { settings += Unit }, {})
        } }
        compose.onNodeWithText(context.getString(CoreR.string.storage_loading)).assertIsDisplayed()
        openMenu()
        compose.onNodeWithText(context.getString(R.string.changeChips)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.clearChecked)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.settings)).performClick()
        compose.runOnIdle { state.value = ready().copy(readFailure = true, writeFailure = true) }
        compose.onNodeWithTag(tokenTag(tokens().first().id)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(CoreR.string.storage_read_error)).performClick()
        compose.runOnIdle { assertEquals(listOf(Unit), settings); assertEquals(listOf(Unit), retries) }
    }

    @Test fun menuDelegatesCountClearAndSettingsWithoutLocalMutation() {
        val actions = mutableListOf<String>()
        compose.setContent { TokensTheme {
            TokensScreen(ready(), true, {}, { actions += CLEAR }, {}, { assertEquals(MAX_TOKEN_COUNT, it); actions += COUNT },
                { actions += SETTINGS }, {})
        } }
        openMenu()
        compose.onNodeWithText(context.getString(R.string.changeChips)).performClick()
        openMenu()
        compose.onNodeWithText(context.getString(R.string.clearChecked)).performClick()
        openMenu()
        compose.onNodeWithText(context.getString(R.string.settings)).performClick()
        compose.runOnIdle { assertEquals(listOf(COUNT, CLEAR, SETTINGS), actions) }
    }

    @Test fun photoLeavesRoomForAllTokensAndUsesExistingCameraGate() {
        val hasCamera = mutableStateOf(true)
        val photos = mutableListOf<Unit>()
        compose.setContent { TokensTheme {
            Box(Modifier.size(BOARD_WIDTH.dp, SCREEN_HEIGHT.dp)) {
                TokensScreen(ready().copy(reinforcement = ReinforcementSettings(enabled = true)), hasCamera.value,
                    {}, {}, {}, {}, {}, { photos += Unit })
            }
        } }
        compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed().performClick()
        tokens().forEach { compose.onNodeWithTag(tokenTag(it.id)).assertIsDisplayed() }
        compose.runOnIdle { assertEquals(listOf(Unit), photos); hasCamera.value = false }
        compose.onNodeWithTag(REINFORCEMENT_TAG).assertDoesNotExist()
    }

    private fun openMenu() = compose.onNodeWithContentDescription(context.getString(R.string.tokens_menu)).performClick()
    private fun tokens() = List(MAX_TOKEN_COUNT) { TokenState("token-$it", TokenShape.CIRCLE, COLOR, false) }
    private fun ready() = TokensUiState(board = BoardState(tokens(), COLOR, REVISION), loading = false)

    private companion object {
        const val BOARD_WIDTH = 320
        const val BOARD_HEIGHT = 140
        const val SCREEN_HEIGHT = 240
        const val COLOR = -65536
        const val REVISION = 0L
        const val SINGLE_SIZE = 1
        const val SWIPE_START = 0.6f
        const val SHORT_END = 0.5f
        const val CLEAR = "clear"
        const val COUNT = "count"
        const val SETTINGS = "settings"
    }
}
