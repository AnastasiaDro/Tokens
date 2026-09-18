package presentation.tokens_screen

import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import org.junit.Assert.*
import org.junit.Test

class TokenBoardGeometryTest {
    @Test fun everySupportedCountFitsBothDimensionsIncludingTinyWindows() {
        for (width in listOf(EMPTY_SIZE, TINY_SIZE, NARROW_SIZE, PHONE_WIDTH, WIDE_SIZE)) {
            for (height in listOf(EMPTY_SIZE, TINY_SIZE, SHORT_HEIGHT, NARROW_SIZE, TALL_HEIGHT)) {
                for (count in MIN_TOKEN_COUNT..MAX_TOKEN_COUNT) for (wide in listOf(false, true)) {
                    val result = geometry(width, height, count, wide)
                    assertTrue(result.diameter >= EMPTY_SIZE)
                    assertTrue(result.gap >= EMPTY_SIZE)
                    assertTrue(result.rowWidth(result.columns) <= width)
                    assertTrue(result.height <= height)
                    assertTrue(result.diameter <= PREFERRED_DIAMETER)
                    val plan = boardContentGeometry(width, height, count, PREFERRED_DIAMETER, PREFERRED_GAP,
                        MINIMUM_DIAMETER, wide, true, PHOTO_SIZE)
                    assertTrue(plan.tokens.diameter >= result.diameter)
                    assertTrue(plan.tokens.rowWidth(plan.tokens.columns) <= plan.boardWidth)
                    assertTrue(plan.tokens.height <= plan.boardHeight)
                    assertTrue(plan.boardWidth <= width && plan.boardHeight <= height)
                    if (plan.photoBelow) assertTrue(plan.boardHeight + plan.photoSize <= height)
                    else assertTrue(plan.boardWidth + plan.photoSize <= width)
                }
            }
        }
    }

    @Test fun twentyTokensPreferFourRowsOfFiveOnPhone() {
        for ((width, height) in listOf(PHONE_WIDTH to TALL_HEIGHT, PHONE_LANDSCAPE_WIDTH to PHONE_LANDSCAPE_HEIGHT)) {
            val result = geometry(width, height)
            assertEquals(PHONE_BOARD_COLUMNS, result.columns)
            assertEquals(FOUR_ROWS, result.rows)
            assertTrue(result.diameter >= MINIMUM_DIAMETER)
        }
    }

    @Test fun wideWindowPrefersTwoRowsOfTenIncludingPhoto() {
        val plan = boardContentGeometry(WIDE_SIZE, TALL_HEIGHT, MAX_TOKEN_COUNT, PREFERRED_DIAMETER,
            PREFERRED_GAP, MINIMUM_DIAMETER, true, true, PHOTO_SIZE)
        assertEquals(WIDE_BOARD_COLUMNS, plan.tokens.columns)
        assertEquals(TWO_ROWS, plan.tokens.rows)
        assertEquals(PREFERRED_DIAMETER, plan.tokens.diameter)
        assertEquals(PHOTO_SIZE, plan.photoSize)
        assertFalse(plan.photoBelow)
    }

    @Test fun gapsShrinkBeforeChangingThePhoneGrid() {
        val result = geometry(NARROW_SIZE, MINIMUM_DIAMETER * FOUR_ROWS)
        assertEquals(PHONE_BOARD_COLUMNS, result.columns)
        assertEquals(MINIMUM_DIAMETER, result.diameter)
        assertEquals(EMPTY_SIZE, result.gap)
    }

    @Test fun shortWindowChangesGridToKeepLargerEqualTokens() {
        val result = geometry(PHONE_WIDTH, SHORT_HEIGHT)
        assertNotEquals(PHONE_BOARD_COLUMNS, result.columns)
        assertTrue(result.diameter > SHORT_HEIGHT / FOUR_ROWS)
        assertTrue(result.height <= SHORT_HEIGHT)
    }

    @Test fun photoMovesBelowPortraitBoardAndCanHideWithoutShrinkingTokens() {
        val portrait = boardContentGeometry(PHONE_WIDTH, TALL_HEIGHT, MAX_TOKEN_COUNT, PREFERRED_DIAMETER,
            PREFERRED_GAP, MINIMUM_DIAMETER, false, true, PHOTO_SIZE)
        assertTrue(portrait.photoBelow)
        assertTrue(portrait.photoSize >= MINIMUM_DIAMETER)
        val tight = boardContentGeometry(NARROW_SIZE, SHORT_HEIGHT, MAX_TOKEN_COUNT, PREFERRED_DIAMETER,
            PREFERRED_GAP, MINIMUM_DIAMETER, false, true, PHOTO_SIZE)
        assertEquals(EMPTY_SIZE, tight.photoSize)
        assertEquals(geometry(NARROW_SIZE, SHORT_HEIGHT), tight.tokens)
    }

    @Test fun emptyBoardHasNoDimensions() {
        val result = geometry(WIDE_SIZE, NARROW_SIZE, EMPTY_SIZE)
        assertEquals(EMPTY_SIZE, result.height)
        assertEquals(EMPTY_SIZE, result.diameter)
    }

    private fun geometry(width: Int, height: Int, count: Int = MAX_TOKEN_COUNT, wide: Boolean = false) =
        tokenBoardGeometry(width, height, count, PREFERRED_DIAMETER, PREFERRED_GAP, MINIMUM_DIAMETER, wide)

    private companion object {
        const val EMPTY_SIZE = 0
        const val TINY_SIZE = 4
        const val NARROW_SIZE = 240
        const val PHONE_WIDTH = 320
        const val PHONE_LANDSCAPE_WIDTH = 780
        const val PHONE_LANDSCAPE_HEIGHT = 300
        const val WIDE_SIZE = 1000
        const val TALL_HEIGHT = 600
        const val SHORT_HEIGHT = 120
        const val PREFERRED_DIAMETER = 68
        const val MINIMUM_DIAMETER = 48
        const val PREFERRED_GAP = 8
        const val PHOTO_SIZE = 150
        const val TWO_ROWS = 2
        const val FOUR_ROWS = 4
    }
}
