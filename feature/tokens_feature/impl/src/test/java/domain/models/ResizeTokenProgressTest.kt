package domain.models

import org.junit.Assert.*
import org.junit.Test

class ResizeTokenProgressTest {
    @Test fun `all board patterns and sizes preserve the resize invariants`() {
        for (oldSize in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
            for (mask in EMPTY_MASK until (TOKEN_BIT shl oldSize)) {
                val original = List(oldSize) { mask and (TOKEN_BIT shl it) != EMPTY_MASK }
                val checked = original.count { it }
                for (newSize in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
                    val result = resizeTokenProgress(original, newSize)
                    val expected = when {
                        newSize >= oldSize -> checked
                        checked == oldSize -> newSize
                        else -> minOf(checked, newSize - MIN_UNCHECKED_TOKENS)
                    }
                    assertEquals(newSize, result.size)
                    assertEquals("$original -> $newSize", expected, result.count { it })
                    if (newSize >= oldSize) assertEquals(original, result.take(oldSize))
                    assertEquals(original, List(oldSize) { mask and (TOKEN_BIT shl it) != EMPTY_MASK })
                }
            }
        }
    }

    @Test fun `excess marks are removed from the right`() {
        assertEquals(listOf(true, true, false),
            resizeTokenProgress(listOf(true, true, true, true, false), SHRUNK_BOARD_SIZE))
    }

    private companion object {
        const val MIN_BOARD_SIZE = 1
        const val MAX_BOARD_SIZE = 10
        const val EMPTY_MASK = 0
        const val TOKEN_BIT = 1
        const val MIN_UNCHECKED_TOKENS = 1
        const val SHRUNK_BOARD_SIZE = 3
    }
}
