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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
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
import com.cerebus.tokens.core.ui.theme.TokensColors
import com.cerebus.tokens.core.ui.theme.TokensDimensions
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

    @Test fun overflowMenuUsesCompactThemeMetrics() {
        compose.setContent { TokensTheme { TokensScreen(ready(), {}, {}, {}, {}, {}, {}) } }

        openMenu()

        val popup = compose.onNodeWithTag(TOKEN_MENU_POPUP_TAG)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(TokensDimensions.MenuWidth)
        val popupBounds = popup.fetchSemanticsNode().boundsInRoot
        val count = compose.onNodeWithText(context.getString(R.string.changeChips))
            .assertHeightIsAtLeast(TokensDimensions.MenuItemMinHeight)
            .fetchSemanticsNode().boundsInRoot
        val clear = compose.onNodeWithText(context.getString(R.string.clearChecked))
            .assertHeightIsAtLeast(TokensDimensions.MenuItemMinHeight)
            .fetchSemanticsNode().boundsInRoot
        val settings = compose.onNodeWithText(context.getString(R.string.settings))
            .assertHeightIsAtLeast(TokensDimensions.MenuItemMinHeight)
            .fetchSemanticsNode().boundsInRoot
        val divider = compose.onNodeWithTag(TOKEN_MENU_DIVIDER_TAG)
            .assertHeightIsEqualTo(TokensDimensions.MenuDividerThickness)
            .fetchSemanticsNode().boundsInRoot
        val verticalPaddingPx = with(compose.density) { TokensDimensions.MenuVerticalPadding.toPx() }

        assertEquals(verticalPaddingPx, count.top - popupBounds.top, PIXEL_TOLERANCE)
        assertEquals(verticalPaddingPx, popupBounds.bottom - settings.bottom, PIXEL_TOLERANCE)
        assertTrue(clear.bottom <= divider.top && divider.bottom <= settings.top)

        val popupPixels = popup.captureToImage().toPixelMap()
        assertEquals(
            TokensColors.MenuSurface.toArgb(),
            popupPixels[popupPixels.width / CENTER_DIVISOR, MENU_SURFACE_SAMPLE_Y].toArgb(),
        )
    }

    @Test fun photoLeavesRoomForAllTokensAndFollowsOnlySavedSetting() {
        val enabled = mutableStateOf(true)
        val photos = mutableListOf<Unit>()
        compose.setContent {
            // Keep the intended content size independent of the host's density and system bars.
            // The tight-window test separately verifies hiding the photo when space is insufficient.
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size((BOARD_WIDTH + DESIGN_PADDING + MENU_VISIBLE_END_PADDING).dp, SCREEN_HEIGHT.dp)
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

    @Test fun portraitTabletPhotoIsAboveTokensWithoutChangingTheirCenterOrSize() {
        val showPhoto = mutableStateOf(false)
        val count = mutableStateOf(PHONE_COLUMNS)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(PORTRAIT_TABLET_WINDOW).testTag(WINDOW_TAG)
                        .consumeWindowInsets(WindowInsets.safeDrawing)) {
                        TokensScreen(ready().copy(
                            board = BoardState(tokens().take(count.value), COLOR, REVISION),
                            reinforcement = ReinforcementSettings(enabled = showPhoto.value)),
                            {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        listOf(PHONE_COLUMNS, MAX_TOKEN_COUNT).forEach { tokenCount ->
            compose.runOnIdle { count.value = tokenCount; showPhoto.value = false }
            val before = assertTokensFit(tokenCount)
            compose.runOnIdle { showPhoto.value = true }
            val after = assertTokensFit(tokenCount)
            assertEquals(before, after)
            val viewport = compose.onNodeWithTag(WINDOW_TAG).fetchSemanticsNode().boundsInRoot
            val photo = compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val top = after.minOf { it.top }
            val board = compose.onNodeWithTag(TOKEN_BOARD_TAG).fetchSemanticsNode().boundsInRoot
            // The photo adapts to the padded width and the free height above the centered grid.
            val expectedPhotoSize = minOf(TABLET_PHOTO_SIZE.toFloat(), board.width / CENTER_DIVISOR,
                top - viewport.top - TABLET_PHOTO_GAP)
            assertEquals(expectedPhotoSize, photo.width, PIXEL_TOLERANCE)
            assertEquals(photo.width, photo.height, PIXEL_TOLERANCE)
            assertEquals(viewport.center.y, (top + after.maxOf { it.bottom }) / CENTER_DIVISOR, PIXEL_TOLERANCE)
            assertEquals(viewport.center.x, photo.center.x, PIXEL_TOLERANCE)
            assertEquals(TABLET_PHOTO_GAP, top - photo.bottom, PIXEL_TOLERANCE)
            assertTrue(photo.top >= viewport.top)
            after.forEach { assertFalse(photo.overlaps(it)) }
        }
    }

    @Test fun landscapePhotoAlignsWithVisibleMenuDotsAcrossWidthsAndSafeInsets() {
        val window = mutableStateOf(LANDSCAPE_WINDOW)
        val safeEdges = mutableStateOf(NO_SAFE_INSET to NO_SAFE_INSET)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(window.value).testTag(WINDOW_TAG)) {
                        TokensScreen(ready().copy(reinforcement = ReinforcementSettings(enabled = true)),
                            {}, {}, {}, {}, {}, {}, safeDrawingInsets = WindowInsets(
                                left = safeEdges.value.first.dp, right = safeEdges.value.second.dp,
                                top = TOP_SAFE_INSET.dp, bottom = BOTTOM_SAFE_INSET.dp))
                    }
                }
            }
        }
        listOf(LANDSCAPE_WINDOW, WIDE_PHONE_WINDOW).forEach { size ->
            var originalTokenSize: Float? = null
            var originalPhotoSize: Float? = null
            listOf(
                NO_SAFE_INSET to NO_SAFE_INSET,
                SMALL_SAFE_INSET to SMALL_SAFE_INSET,
                DESIGN_PADDING to DESIGN_PADDING,
                LARGE_SAFE_INSET to LARGE_SAFE_INSET,
                SMALL_SAFE_INSET to LARGE_SAFE_INSET,
                LARGE_SAFE_INSET to SMALL_SAFE_INSET,
            ).forEach { edges ->
                compose.runOnIdle { window.value = size; safeEdges.value = edges }
                val viewport = compose.onNodeWithTag(WINDOW_TAG).fetchSemanticsNode().boundsInRoot
                val board = compose.onNodeWithTag(TOKEN_BOARD_TAG).fetchSemanticsNode().boundsInRoot
                val photo = compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed()
                    .fetchSemanticsNode().boundsInRoot
                val tokens = assertTokensFit(MAX_TOKEN_COUNT)
                val leftPadding = maxOf(DESIGN_PADDING, edges.first).toFloat()
                assertEquals(leftPadding, board.left - viewport.left, PIXEL_TOLERANCE)
                assertPhotoAlignedWithMenu(photo)
                val menu = compose.onNodeWithContentDescription(context.getString(R.string.tokens_menu))
                    .fetchSemanticsNode().boundsInRoot
                assertEquals(edges.second.toFloat(), viewport.right - menu.right, PIXEL_TOLERANCE)
                assertTrue(viewport.right - photo.right >= maxOf(DESIGN_PADDING, edges.second))
                assertTrue(tokens.minOf { it.left } >= viewport.left + leftPadding)
                assertEquals(TOP_SAFE_INSET.toFloat(), board.top - viewport.top, PIXEL_TOLERANCE)
                assertEquals(BOTTOM_SAFE_INSET.toFloat(), viewport.bottom - board.bottom, PIXEL_TOLERANCE)
                assertEquals(board.center.y, photo.center.y, PIXEL_TOLERANCE)
                assertEquals(board.center.y,
                    (tokens.minOf { it.top } + tokens.maxOf { it.bottom }) / CENTER_DIVISOR, PIXEL_TOLERANCE)
                assertEquals(board.center.x,
                    (tokens.minOf { it.left } + tokens.maxOf { it.right }) / CENTER_DIVISOR, PIXEL_TOLERANCE)
                tokens.forEach { assertFalse(photo.overlaps(it)) }
                if (originalTokenSize == null) {
                    originalTokenSize = tokens.first().width
                    originalPhotoSize = photo.width
                }
                assertEquals(originalTokenSize!!, tokens.first().width, PIXEL_TOLERANCE)
                assertEquals(originalPhotoSize!!, photo.width, PIXEL_TOLERANCE)
            }
        }
    }

    @Test fun smallerLandscapeWindowShrinksElementsWhileKeepingOuterPaddingAndAllTokensVisible() {
        val window = mutableStateOf(LANDSCAPE_WINDOW)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(window.value).testTag(WINDOW_TAG)) {
                        TokensScreen(ready().copy(reinforcement = ReinforcementSettings(enabled = true)),
                            {}, {}, {}, {}, {}, {}, safeDrawingInsets = WindowInsets(NO_SAFE_INSET))
                    }
                }
            }
        }
        val originalTokens = assertTokensFit(MAX_TOKEN_COUNT)
        val originalPhoto = compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { window.value = COMPACT_LANDSCAPE_WINDOW }
        val tokens = assertTokensFit(MAX_TOKEN_COUNT)
        val photo = compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag(WINDOW_TAG).fetchSemanticsNode().boundsInRoot
        val board = compose.onNodeWithTag(TOKEN_BOARD_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(tokens.first().width < originalTokens.first().width)
        assertTrue(photo.width < originalPhoto.width)
        assertEquals(DESIGN_PADDING.toFloat(), board.left - viewport.left, PIXEL_TOLERANCE)
        assertPhotoAlignedWithMenu(photo)
        assertEquals(viewport.center.y, photo.center.y, PIXEL_TOLERANCE)
        assertEquals(viewport.center.y,
            (tokens.minOf { it.top } + tokens.maxOf { it.bottom }) / CENTER_DIVISOR, PIXEL_TOLERANCE)
        tokens.forEach { assertFalse(photo.overlaps(it)) }
    }

    @Test fun lowLandscapePhotoStaysAlignedWithMenuWithoutOverlappingItsTouchTarget() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(TEST_DENSITY)) {
                TokensTheme {
                    Box(Modifier.size(LOW_LANDSCAPE_WINDOW).testTag(WINDOW_TAG)) {
                        TokensScreen(ready().copy(
                            board = BoardState(tokens().take(SINGLE_SIZE), COLOR, REVISION),
                            reinforcement = ReinforcementSettings(enabled = true)),
                            {}, {}, {}, {}, {}, {}, safeDrawingInsets = WindowInsets(NO_SAFE_INSET))
                    }
                }
            }
        }
        val photo = compose.onNodeWithTag(REINFORCEMENT_TAG).assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag(WINDOW_TAG).fetchSemanticsNode().boundsInRoot
        assertPhotoAlignedWithMenu(photo)
        assertEquals(viewport.center.y, photo.center.y, PIXEL_TOLERANCE)
        assertTokensFit(SINGLE_SIZE).forEach { assertFalse(photo.overlaps(it)) }
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

    private fun assertPhotoAlignedWithMenu(photo: androidx.compose.ui.geometry.Rect) {
        val icon = compose.onNodeWithTag(TOKEN_MENU_ICON_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val menu = compose.onNodeWithContentDescription(context.getString(R.string.tokens_menu))
            .fetchSemanticsNode().boundsInRoot
        // The vector's visible dots occupy x=10..14 inside its 24 dp viewport.
        assertEquals(icon.center.x + MENU_DOT_RADIUS, photo.right, PIXEL_TOLERANCE)
        assertFalse("Photo $photo overlaps menu touch target $menu", photo.overlaps(menu))
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
        const val COMPACT_WINDOW_HEIGHT = 180
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
        const val MENU_DOT_RADIUS = 2f
        const val MENU_VISIBLE_END_PADDING = 22
        const val MENU_LOWER_TOUCH_FRACTION = 0.75f
        const val MENU_SURFACE_SAMPLE_Y = 2
        const val TABLET_PHOTO_SIZE = 300
        const val TABLET_PHOTO_GAP = 16f
        const val NO_SAFE_INSET = 0
        const val SMALL_SAFE_INSET = 8
        const val DESIGN_PADDING = 16
        const val LARGE_SAFE_INSET = 28
        const val TOP_SAFE_INSET = 24
        const val BOTTOM_SAFE_INSET = 12
        val PORTRAIT_TABLET_WINDOW = DpSize(600.dp, 1000.dp)
        val PHONE_WINDOW = DpSize(BOARD_WIDTH.dp, 600.dp)
        val LANDSCAPE_WINDOW = DpSize(TABLET_HEIGHT.dp, BOARD_WIDTH.dp)
        val WIDE_PHONE_WINDOW = DpSize(840.dp, BOARD_WIDTH.dp)
        val COMPACT_LANDSCAPE_WINDOW = DpSize(420.dp, SCREEN_HEIGHT.dp)
        val LOW_LANDSCAPE_WINDOW = DpSize(TABLET_HEIGHT.dp, COMPACT_WINDOW_HEIGHT.dp)
        val TABLET_WINDOW = DpSize(1000.dp, TABLET_HEIGHT.dp)
        val TIGHT_WINDOW = DpSize(220.dp, COMPACT_WINDOW_HEIGHT.dp)
        val WINDOWS = listOf(PHONE_WINDOW, TABLET_WINDOW, TIGHT_WINDOW, DpSize(TABLET_HEIGHT.dp, BOARD_WIDTH.dp))
    }
}
