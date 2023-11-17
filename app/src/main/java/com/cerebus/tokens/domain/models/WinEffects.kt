package com.cerebus.tokens.domain.models

import java.time.Duration

class WinEffects(
    var isWinAnimationOn: Boolean,
    var isWinSoundOn: Boolean,
    var duration: Long,
    var animationRepeatTimes: Int

)