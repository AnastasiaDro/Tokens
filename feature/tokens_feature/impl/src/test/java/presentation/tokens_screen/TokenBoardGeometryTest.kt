package presentation.tokens_screen

import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import org.junit.Assert.*
import org.junit.Test

class TokenBoardGeometryTest {
    @Test fun everySupportedCountFitsBothDimensionsIncludingTinyWindows() {
        for (width in listOf(EMPTY_SIZE, TINY_SIZE, NARROW_SIZE, WIDE_SIZE)) {
            for (height in listOf(EMPTY_SIZE, TINY_SIZE, NARROW_SIZE)) {
                for (count in MIN_TOKEN_COUNT..MAX_TOKEN_COUNT) {
                    val result = tokenBoardGeometry(width, height, count, PREFERRED_DIAMETER, PREFERRED_GAP)
                    assertTrue(result.diameter >= EMPTY_SIZE)
                    assertTrue(result.gap >= EMPTY_SIZE)
                    assertTrue(result.rowWidth(result.columns) <= width)
                    assertTrue(result.height <= height)
                    assertTrue(result.diameter <= PREFERRED_DIAMETER)
                }
            }
        }
    }

    @Test fun tenTokensKeepTwoRowsAndFiveColumns() {
        val result = tokenBoardGeometry(WIDE_SIZE, NARROW_SIZE, MAX_TOKEN_COUNT, PREFERRED_DIAMETER, PREFERRED_GAP)
        assertEquals(BOARD_COLUMNS, result.columns)
        assertEquals(TWO_ROWS, result.rows)
        assertEquals(PREFERRED_DIAMETER, result.diameter)
    }

    @Test fun heightCanBeTheLimitingDimension() {
        val result = tokenBoardGeometry(WIDE_SIZE, TINY_SIZE, MAX_TOKEN_COUNT, PREFERRED_DIAMETER, PREFERRED_GAP)
        assertTrue(result.diameter < PREFERRED_DIAMETER)
        assertTrue(result.height <= TINY_SIZE)
    }

    @Test fun emptyBoardHasNoDimensions() {
        val result = tokenBoardGeometry(WIDE_SIZE, NARROW_SIZE, EMPTY_SIZE, PREFERRED_DIAMETER, PREFERRED_GAP)
        assertEquals(EMPTY_SIZE, result.height)
        assertEquals(EMPTY_SIZE, result.diameter)
    }

    private companion object {
        const val EMPTY_SIZE = 0
        const val TINY_SIZE = 4
        const val NARROW_SIZE = 240
        const val WIDE_SIZE = 1000
        const val PREFERRED_DIAMETER = 80
        const val PREFERRED_GAP = 8
        const val TWO_ROWS = 2
    }
}
