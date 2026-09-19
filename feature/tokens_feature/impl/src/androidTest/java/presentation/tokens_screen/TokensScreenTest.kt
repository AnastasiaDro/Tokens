package presentation.tokens_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
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

    @Test fun allTwentyTokensFitSmallBoardAndClicksUseStableIdsAfterReorder() {
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
            TokensScreen(state.value, {}, {}, { retries += Unit }, {}, { settings += Unit }, {})
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
            TokensScreen(ready(), {}, { actions += CLEAR }, {}, { assertEquals(MAX_TOKEN_COUNT, it); actions += COUNT },
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

    @Test fun compactMenuIconAndPressTargetShareTheirCenterWithLargeFont() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY, LARGE_FONT)) {
                TokensTheme { TokensScreen(ready(), {}, {}, {}, {}, {}, {}) }
            }
        }
        val menu = compose.onNodeWithContentDescription(context.getString(R.string.tokens_menu))
            .assertWidthIsEqualTo(MENU_TOUCH_SIZE.dp).assertHeightIsEqualTo(MENU_TOUCH_SIZE.dp)
        val icon = compose.onNodeWithTag(TOKEN_MENU_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed().assertWidthIsEqualTo(MENU_ICON_SIZE.dp).assertHeightIsEqualTo(MENU_ICON_SIZE.dp)
        val menuBounds = menu.fetchSemanticsNode().boundsInRoot
        val iconBounds = icon.fetchSemanticsNode().boundsInRoot
        assertEquals(menuBounds.center.x, iconBounds.center.x, PIXEL_TOLERANCE)
        assertEquals(menuBounds.center.y, iconBounds.center.y, PIXEL_TOLERANCE)
        // The complete minimum touch target remains clickable around the centered indication.
        menu.performTouchInput { click(Offset(centerX, height * MENU_LOWER_TOUCH_FRACTION)) }
        compose.onNodeWithText(context.getString(R.string.settings)).assertIsDisplayed()
    }

    @Test fun photoLeavesRoomForAllTokensAndFollowsOnlySavedSetting() {
        val enabled = mutableStateOf(true)
        val photos = mutableListOf<Unit>()
        compose.setContent {
            // Keep the intended content size independent of the host's density and system bars.
            // The tight-window test separately verifies hiding the photo when space is insufficient.
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(BOARD_WIDTH.dp, SCREEN_HEIGHT.dp)
                        .consumeWindowInsets(WindowInsets.systemBars)) {
                        TokensScreen(ready().copy(reinforcement = ReinforcementSettings(enabled = enabled.value)),
                            {}, {}, {}, {}, {}, { photos += Unit })
                    }
                }
            }
        }
        compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed().performClick()
        tokens().forEach { compose.onNodeWithTag(tokenTag(it.id)).assertIsDisplayed() }
        compose.runOnIdle { assertEquals(listOf(Unit), photos); enabled.value = false }
        compose.onNodeWithTag(REINFORCEMENT_TAG).assertDoesNotExist()
    }

    @Test fun adaptiveScreenFitsEveryCountWithPhotoAndLargeFontAcrossWindowSizes() {
        val window = mutableStateOf(WINDOWS.first())
        val count = mutableStateOf(MAX_TOKEN_COUNT)
        val photo = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY, LARGE_FONT)) {
                TokensTheme { Box(Modifier.size(window.value).consumeWindowInsets(WindowInsets.systemBars)) {
                    TokensScreen(ready().copy(board = BoardState(tokens().take(count.value), COLOR, REVISION),
                        reinforcement = ReinforcementSettings(enabled = photo.value)), {}, {}, {}, {}, {}, {})
                } }
            }
        }
        WINDOWS.forEach { size ->
            listOf(false, true).forEach { withPhoto ->
                (SINGLE_SIZE..MAX_TOKEN_COUNT).forEach { tokenCount ->
                    compose.runOnIdle { window.value = size; count.value = tokenCount; photo.value = withPhoto }
                    assertTokensFit(tokenCount)
                }
            }
        }
    }

    @Test fun phoneUsesFourRowsAndWideWindowUsesTwoRows() {
        val window = mutableStateOf(PHONE_WINDOW)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme { Box(Modifier.size(window.value).consumeWindowInsets(WindowInsets.systemBars)) {
                    TokensScreen(ready(), {}, {}, {}, {}, {}, {})
                } }
            }
        }
        assertEquals(PHONE_ROWS, assertTokensFit(MAX_TOKEN_COUNT).map { it.top }.distinct().size)
        compose.runOnIdle { window.value = TABLET_WINDOW }
        assertEquals(TABLET_ROWS, assertTokensFit(MAX_TOKEN_COUNT).map { it.top }.distinct().size)
    }

    @Test fun boardIsVerticallyCenteredAndMenuDoesNotOverlapTokensInBothOrientations() {
        val window = mutableStateOf(PHONE_WINDOW)
        val count = mutableStateOf(MAX_TOKEN_COUNT)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(window.value).testTag(WINDOW_TAG)
                        .consumeWindowInsets(WindowInsets.safeDrawing)) {
                        TokensScreen(ready().copy(board = BoardState(tokens().take(count.value), COLOR, REVISION)),
                            {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        listOf(PHONE_WINDOW, LANDSCAPE_WINDOW, TIGHT_WINDOW).forEach { size ->
            listOf(PHONE_COLUMNS, MAX_TOKEN_COUNT).forEach { tokenCount ->
                compose.runOnIdle { window.value = size; count.value = tokenCount }
                val bounds = assertTokensFit(tokenCount)
                val viewport = compose.onNodeWithTag(WINDOW_TAG).fetchSemanticsNode().boundsInRoot
                val tokensCenter = (bounds.minOf { it.top } + bounds.maxOf { it.bottom }) / CENTER_DIVISOR
                assertEquals(viewport.center.y, tokensCenter, PIXEL_TOLERANCE)
                val menu = compose.onNodeWithContentDescription(context.getString(R.string.tokens_menu))
                    .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                bounds.forEach { assertFalse("Menu $menu overlaps token $it", it.overlaps(menu)) }
            }
        }
    }

    @Test fun tightWindowKeepsTokensVisibleDuringErrorAndPhotoAccessibleFromMenu() {
        val photos = mutableListOf<Unit>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY, LARGE_FONT)) {
                TokensTheme { Box(Modifier.size(TIGHT_WINDOW).consumeWindowInsets(WindowInsets.systemBars)) {
                    TokensScreen(ready().copy(writeFailure = true, reinforcement = ReinforcementSettings(enabled = true)),
                        {}, {}, {}, {}, {}, { photos += Unit })
                } }
            }
        }
        assertTokensFit(MAX_TOKEN_COUNT)
        compose.onNodeWithTag(REINFORCEMENT_TAG).assertDoesNotExist()
        openMenu()
        compose.onNodeWithText(context.getString(R.string.reinforcement_image)).performClick()
        compose.runOnIdle { assertEquals(listOf(Unit), photos) }
    }

    private fun assertTokensFit(count: Int): List<androidx.compose.ui.geometry.Rect> {
        val board = compose.onNodeWithTag(TOKEN_BOARD_TAG).fetchSemanticsNode().boundsInRoot
        val bounds = tokens().take(count).map { token ->
            compose.onNodeWithTag(tokenTag(token.id)).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.also {
                assertTrue("Token must have positive size: $it", it.width > ZERO_SIZE && it.height > ZERO_SIZE)
                assertTrue("Token $it outside $board", it.left >= board.left && it.right <= board.right &&
                    it.top >= board.top && it.bottom <= board.bottom)
            }
        }
        assertEquals(SINGLE_SIZE, bounds.map { it.size }.distinct().size)
        bounds.forEachIndexed { index, item -> bounds.drop(index + SINGLE_SIZE).forEach { other ->
            assertFalse("Overlapping tokens: $item and $other", item.overlaps(other))
        } }
        return bounds
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
        const val TEST_DENSITY = 1f
        const val LARGE_FONT = 2f
        const val ZERO_SIZE = 0f
        const val PHONE_ROWS = 4
        const val TABLET_ROWS = 2
        const val TABLET_HEIGHT = 650
        const val WINDOW_TAG = "test-window"
        const val PHONE_COLUMNS = 5
        const val CENTER_DIVISOR = 2
        const val PIXEL_TOLERANCE = 1f
        const val MENU_TOUCH_SIZE = 48
        const val MENU_ICON_SIZE = 24
        const val MENU_LOWER_TOUCH_FRACTION = 0.75f
        val PHONE_WINDOW = DpSize(BOARD_WIDTH.dp, 600.dp)
        val LANDSCAPE_WINDOW = DpSize(TABLET_HEIGHT.dp, BOARD_WIDTH.dp)
        val TABLET_WINDOW = DpSize(1000.dp, TABLET_HEIGHT.dp)
        val TIGHT_WINDOW = DpSize(220.dp, 180.dp)
        val WINDOWS = listOf(PHONE_WINDOW, TABLET_WINDOW, TIGHT_WINDOW, DpSize(TABLET_HEIGHT.dp, BOARD_WIDTH.dp))
    }
}
