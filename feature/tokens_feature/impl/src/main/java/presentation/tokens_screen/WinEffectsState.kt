package presentation.tokens_screen

data class WinEffectsState(
    val isAnimationRunning: Boolean,
    val isSoundPlaying: Boolean,
    val celebrationId: Long = NO_CELEBRATION_ID,
) {
    companion object { const val NO_CELEBRATION_ID = 0L }
}
