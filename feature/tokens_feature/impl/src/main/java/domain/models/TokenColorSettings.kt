package domain.models

const val MAX_RECENT_COLORS = 6

data class TokenColorSettings(val color: Int, val recentColors: List<Int> = emptyList())

/** Only confirmed choices belong to history; newest first, without duplicates. */
fun recentColorsAfterConfirmation(recentColors: List<Int>, color: Int): List<Int> =
    (listOf(color) + recentColors).distinct().take(MAX_RECENT_COLORS)
