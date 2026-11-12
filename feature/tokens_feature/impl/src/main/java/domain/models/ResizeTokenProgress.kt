package domain.models

/** Resizes a board without completing an unfinished board as a side effect. */
internal fun resizeTokenProgress(current: List<Boolean>, newCount: Int): List<Boolean> {
    require(newCount >= MIN_TOKEN_COUNT)
    if (newCount >= current.size) {
        return current + List(newCount - current.size) { false }
    }

    val checkedCount = current.count { it }
    val targetChecked = if (checkedCount == current.size) newCount
        else minOf(checkedCount, newCount - MIN_UNCHECKED_TOKENS)
    val result = current.take(newCount).toMutableList()
    var remaining = targetChecked - result.count { it }
    // Preserve surviving positions, moving removed marks into the first free slots.
    for (index in result.indices) {
        if (remaining > NO_PENDING_MARKS && !result[index]) {
            result[index] = true
            remaining--
        }
    }
    for (index in result.indices.reversed()) {
        if (remaining < NO_PENDING_MARKS && result[index]) {
            result[index] = false
            remaining++
        }
    }
    return result
}

private const val MIN_TOKEN_COUNT = 1
private const val MIN_UNCHECKED_TOKENS = 1
private const val NO_PENDING_MARKS = 0
