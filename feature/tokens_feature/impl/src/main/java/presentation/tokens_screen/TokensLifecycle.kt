package presentation.tokens_screen

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.koin.core.context.GlobalContext

/** Effects belong to this back-stack entry, never to rendering or repository emissions alone. */
@Composable
internal fun TokensLifecycle(viewModel: TokensViewModel) {
    val owner = LocalLifecycleOwner.current
    val sound = remember(viewModel) { GlobalContext.get().get<WinSoundOutput>() }
    var lastCelebration by remember(viewModel) { mutableLongStateOf(WinEffectsState.NO_CELEBRATION_ID) }
    DisposableEffect(owner, viewModel, sound) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> { viewModel.onStop(); sound.stop() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.onStop(); sound.stop() }
    }
    LaunchedEffect(owner, viewModel, sound) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state ->
                val effects = state.effects
                if (effects.celebrationId != lastCelebration) {
                    lastCelebration = effects.celebrationId
                    if (effects.isSoundPlaying) sound.play()
                }
                if (!effects.isSoundPlaying) sound.stop()
            }
        }
    }
}
