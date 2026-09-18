package domain.repository

import kotlinx.coroutines.flow.Flow

const val WIN_EFFECTS_DURATION_MS = 5000L
data class EffectsSettings(val animation: Boolean = true, val sound: Boolean = true)

interface WinEffectsRepository {
    val settings: Flow<EffectsSettings>
    suspend fun setAnimation(enabled: Boolean)
    suspend fun setSound(enabled: Boolean)
}

