package domain.models

data class WinEffects(
    val isWinAnimationOn: Boolean,
    val isWinSoundOn: Boolean,
    val duration: Long,
    val animationRepeatTimes: Int
)
