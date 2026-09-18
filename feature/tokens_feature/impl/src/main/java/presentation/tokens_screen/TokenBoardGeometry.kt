package presentation.tokens_screen

internal data class TokenBoardGeometry(val columns: Int, val rows: Int, val diameter: Int, val gap: Int) {
    fun rowWidth(items: Int): Int = items * diameter + (items - SINGLE_ITEM).coerceAtLeast(NO_SIZE) * gap
    val height: Int get() = rows * diameter + (rows - SINGLE_ITEM).coerceAtLeast(NO_SIZE) * gap
}

internal data class BoardContentGeometry(
    val boardWidth: Int,
    val boardHeight: Int,
    val tokens: TokenBoardGeometry,
    val photoSize: Int,
    val photoBelow: Boolean,
)

/** All dimensions are pixels. Prefer the requested grid; use another only when touch targets become too small. */
internal fun tokenBoardGeometry(
    width: Int,
    height: Int,
    count: Int,
    preferredDiameter: Int,
    preferredGap: Int,
    minimumDiameter: Int,
    wideLayout: Boolean = false,
): TokenBoardGeometry {
    if (count <= NO_SIZE) return TokenBoardGeometry(SINGLE_ITEM, NO_SIZE, NO_SIZE, NO_SIZE)
    val preferredColumns = minOf(count, if (wideLayout) WIDE_BOARD_COLUMNS else PHONE_BOARD_COLUMNS)
    val preferred = geometryForColumns(width, height, count, preferredColumns, preferredDiameter, preferredGap, minimumDiameter)
    if (preferred.diameter >= minimumDiameter) return preferred

    // Space for tokens takes priority over gaps and a fixed row count in a constrained window.
    val best = (SINGLE_ITEM..count).map { columns ->
        geometryForColumns(width, height, count, columns, preferredDiameter, NO_SIZE, minimumDiameter)
    }.maxWith(compareBy<TokenBoardGeometry> { it.diameter }
        .thenBy { -kotlin.math.abs(it.columns - preferredColumns) })
    return geometryForColumns(width, height, count, best.columns, preferredDiameter, preferredGap, best.diameter)
}

internal fun boardContentGeometry(
    width: Int,
    height: Int,
    count: Int,
    preferredDiameter: Int,
    preferredGap: Int,
    minimumDiameter: Int,
    wideLayout: Boolean,
    showPhoto: Boolean,
    preferredPhotoSize: Int,
): BoardContentGeometry {
    val base = tokenBoardGeometry(width, height, count, preferredDiameter, preferredGap, minimumDiameter, wideLayout)
    val below = height > width
    if (!showPhoto) return BoardContentGeometry(width, height, base, NO_SIZE, below)
    // Reserve tokens first, allowing their gaps to shrink before reducing their diameter.
    val freeSpace = if (below) height - base.rows * base.diameter else width - base.columns * base.diameter
    val desiredPhoto = minOf(preferredPhotoSize,
        width / (if (below) PHOTO_CROSS_AXIS_DIVISOR else PHOTO_MAIN_AXIS_DIVISOR),
        height / (if (below) PHOTO_MAIN_AXIS_DIVISOR else PHOTO_CROSS_AXIS_DIVISOR))
    var photoGap = minOf(preferredGap, freeSpace).coerceAtLeast(NO_SIZE)
    var photo = minOf(desiredPhoto, freeSpace - photoGap).coerceAtLeast(NO_SIZE)
    if (photo < minimumDiameter) {
        photoGap = NO_SIZE
        photo = minOf(desiredPhoto, freeSpace).coerceAtLeast(NO_SIZE)
    }
    // The photo action remains in the menu when its preview cannot have a usable size.
    if (photo < minimumDiameter) return BoardContentGeometry(width, height, base, NO_SIZE, below)
    val boardWidth = if (below) width else width - photo - photoGap
    val boardHeight = if (below) height - photo - photoGap else height
    val tokens = geometryForColumns(boardWidth, boardHeight, count, base.columns, preferredDiameter, preferredGap, base.diameter)
    return BoardContentGeometry(boardWidth, boardHeight, tokens, photo, below)
}

private fun geometryForColumns(
    width: Int, height: Int, count: Int, columns: Int,
    preferredDiameter: Int, preferredGap: Int, minimumDiameter: Int,
): TokenBoardGeometry {
    if (count <= NO_SIZE) return TokenBoardGeometry(SINGLE_ITEM, NO_SIZE, NO_SIZE, NO_SIZE)
    val rows = ((count + columns - SINGLE_ITEM) / columns).coerceAtLeast(SINGLE_ITEM)
    val rawDiameter = minOf(width / columns, height / rows, preferredDiameter).coerceAtLeast(NO_SIZE)
    val floor = minOf(minimumDiameter, rawDiameter)
    val horizontalGap = if (columns > SINGLE_ITEM) (width - columns * floor) / (columns - SINGLE_ITEM) else preferredGap
    val verticalGap = if (rows > SINGLE_ITEM) (height - rows * floor) / (rows - SINGLE_ITEM) else preferredGap
    val gap = minOf(preferredGap, horizontalGap, verticalGap).coerceAtLeast(NO_SIZE)
    val diameter = minOf(
        (width - (columns - SINGLE_ITEM) * gap) / columns,
        (height - (rows - SINGLE_ITEM) * gap) / rows,
        preferredDiameter,
    ).coerceAtLeast(NO_SIZE)
    return TokenBoardGeometry(columns, rows, diameter, gap)
}

internal const val PHONE_BOARD_COLUMNS = 5
internal const val WIDE_BOARD_COLUMNS = 10
private const val SINGLE_ITEM = 1
private const val NO_SIZE = 0
private const val PHOTO_MAIN_AXIS_DIVISOR = 4
private const val PHOTO_CROSS_AXIS_DIVISOR = 2
