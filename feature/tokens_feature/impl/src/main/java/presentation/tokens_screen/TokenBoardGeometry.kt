package presentation.tokens_screen

/** The existing five-column board, bounded in both dimensions. General adaptive layouts belong to 4.6. */
internal data class TokenBoardGeometry(val columns: Int, val rows: Int, val diameter: Int, val gap: Int) {
    fun rowWidth(items: Int): Int = items * diameter + (items - SINGLE_ITEM).coerceAtLeast(NO_SIZE) * gap
    val height: Int get() = rows * diameter + (rows - SINGLE_ITEM).coerceAtLeast(NO_SIZE) * gap
}

internal fun tokenBoardGeometry(width: Int, height: Int, count: Int, preferredDiameter: Int, preferredGap: Int): TokenBoardGeometry {
    if (count <= NO_SIZE) return TokenBoardGeometry(SINGLE_ITEM, NO_SIZE, NO_SIZE, NO_SIZE)
    val columns = count.coerceAtMost(BOARD_COLUMNS)
    val rows = (count + columns - SINGLE_ITEM) / columns
    val cellLimit = minOf(width / columns, height / rows).coerceAtLeast(NO_SIZE)
    val gap = minOf(preferredGap, cellLimit / GAP_DIVISOR).coerceAtLeast(NO_SIZE)
    val diameter = minOf(
        (width - (columns - SINGLE_ITEM) * gap) / columns,
        (height - (rows - SINGLE_ITEM) * gap) / rows,
        preferredDiameter,
    ).coerceAtLeast(NO_SIZE)
    return TokenBoardGeometry(columns, rows, diameter, gap)
}

internal const val BOARD_COLUMNS = 5
private const val SINGLE_ITEM = 1
private const val NO_SIZE = 0
private const val GAP_DIVISOR = 4
