package presentation.tokens_screen

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.cerebus.tokens.feature.tokens_feature.R
import kotlinx.coroutines.delay

@Composable
internal fun WinCelebration(state: WinEffectsState, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var retiredId by remember(lifecycle) { mutableLongStateOf(WinEffectsState.NO_CELEBRATION_ID) }
    val currentId by rememberUpdatedState(state.celebrationId)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) retiredId = currentId
            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (started && state.isAnimationRunning && state.celebrationId != retiredId) {
        key(state.celebrationId) {
            Box(modifier.testTag(CELEBRATION_TAG)) {
                Firework(R.raw.firework_left, FIRST_FIREWORK_DELAY_MS, Modifier.fillMaxSize(), Alignment.BottomStart, LEFT_FIREWORK_TAG)
                Firework(R.raw.firework_right, SECOND_FIREWORK_DELAY_MS, Modifier.fillMaxSize(), Alignment.BottomEnd, RIGHT_FIREWORK_TAG)
                Firework(R.raw.firework_center, THIRD_FIREWORK_DELAY_MS, Modifier.fillMaxSize(), Alignment.Center, CENTER_FIREWORK_TAG)
            }
        }
    }
}

@Composable
private fun Firework(@RawRes resource: Int, delayMillis: Long, modifier: Modifier, alignment: Alignment, tag: String) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resource))
    var visible by remember { mutableStateOf(false) }
    // Only the presentation stagger lives here. The single victory-expiry timer stays in the ViewModel.
    LaunchedEffect(Unit) { delay(delayMillis); visible = true }
    if (visible) LottieAnimation(composition, modifier.testTag(tag), alignment = alignment)
}

internal const val CELEBRATION_TAG = "win-celebration"
internal const val LEFT_FIREWORK_TAG = "win-left"
internal const val RIGHT_FIREWORK_TAG = "win-right"
internal const val CENTER_FIREWORK_TAG = "win-center"
internal const val FIRST_FIREWORK_DELAY_MS = 0L
internal const val SECOND_FIREWORK_DELAY_MS = 500L
internal const val THIRD_FIREWORK_DELAY_MS = 800L
