package presentation.settings_screen

import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import domain.repository.EffectsSettings
import presentation.state.*

data class TokenSettingsState(val count: Int, val color: Int)
data class SettingsUiState(
    val tokens: TokenSettingsState? = null,
    val effects: EffectsSettings? = null,
    val reinforcement: ReinforcementSettings? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val readFailure: Boolean = false,
    val writeFailure: Boolean = false,
) {
    val editable: Boolean
        get() = !loading && !saving && tokens != null && !readFailure

    val error: StorageFailure?
        get() = when {
            readFailure -> StorageFailure.READ
            writeFailure -> StorageFailure.WRITE
            else -> null
        }
}

sealed interface SettingsAction {
    data object SelectCountClicked : SettingsAction
    data object SelectColorClicked : SettingsAction
    data class AnimationChanged(val enabled: Boolean) : SettingsAction
    data class SoundChanged(val enabled: Boolean) : SettingsAction
    data class ReinforcementChanged(val enabled: Boolean) : SettingsAction
    data object RetryClicked : SettingsAction
    data object YoutubeClicked : SettingsAction
    data object OtherAppsClicked : SettingsAction
    data class Observed(val source: SettingsSnapshotState) : SettingsAction
    data object Loading : SettingsAction
    data class Loaded(val snapshot: SettingsSnapshot) : SettingsAction
    data object ReadFailed : SettingsAction
    data object WriteStarted : SettingsAction
    data object WriteSucceeded : SettingsAction
    data object WriteFailed : SettingsAction
}
