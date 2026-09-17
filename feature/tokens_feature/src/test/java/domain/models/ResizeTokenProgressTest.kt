package domain.models

import org.junit.Assert.*
import org.junit.Test

class ResizeTokenProgressTest {
    @Test fun `all board patterns and sizes preserve the resize invariants`() {
        for (oldSize in 1..10) {
            for (mask in 0 until (1 shl oldSize)) {
                val original = List(oldSize) { mask and (1 shl it) != 0 }
                val checked = original.count { it }
                for (newSize in 1..10) {
                    val result = resizeTokenProgress(original, newSize)
                    val expected = when {
                        newSize >= oldSize -> checked
                        checked == oldSize -> newSize
                        else -> minOf(checked, newSize - 1)
                    }
                    assertEquals(newSize, result.size)
                    assertEquals("$original -> $newSize", expected, result.count { it })
                    if (newSize >= oldSize) assertEquals(original, result.take(oldSize))
                    assertEquals(original, List(oldSize) { mask and (1 shl it) != 0 })
                }
            }
        }
    }

    @Test fun `excess marks are removed from the right`() {
        assertEquals(listOf(true, true, false), resizeTokenProgress(listOf(true, true, true, true, false), 3))
    }
}
