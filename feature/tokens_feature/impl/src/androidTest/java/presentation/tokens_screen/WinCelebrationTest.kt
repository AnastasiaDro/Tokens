package presentation.tokens_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.cerebus.tokens.feature.tokens_feature.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WinCelebrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allExistingFireworkResourcesParseWithNewLottie() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (resource in listOf(R.raw.firework_left, R.raw.firework_right, R.raw.firework_center)) {
            val result = LottieCompositionFactory.fromRawResSync(context, resource)
            assertNull(result.exception)
            assertNotNull(result.value)
            assertTrue(result.value!!.duration > NO_DURATION)
        }
    }

    @Test fun animationDoesNotDependOnSoundAndStopsWithoutReplayingRetiredVictory() {
        val owner = Owner()
        val state = mutableStateOf(WinEffectsState(true, false, FIRST_ID))
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                Box(Modifier.fillMaxSize()) { WinCelebration(state.value, Modifier.fillMaxSize()) }
            }
        }
        compose.onNodeWithTag(CELEBRATION_TAG).assertExists()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.onNodeWithTag(CELEBRATION_TAG).assertDoesNotExist()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.onNodeWithTag(CELEBRATION_TAG).assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(celebrationId = SECOND_ID) }
        compose.onNodeWithTag(CELEBRATION_TAG).assertExists()
        compose.runOnIdle { state.value = WinEffectsState(false, true, SECOND_ID) }
        compose.onNodeWithTag(CELEBRATION_TAG).assertDoesNotExist()
    }

    @Test fun staggeredFireworksAreCancelledTogetherWhenVictoryStops() {
        val state = mutableStateOf(WinEffectsState(true, false, FIRST_ID))
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.fillMaxSize()) { WinCelebration(state.value, Modifier.fillMaxSize()) } }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(LEFT_FIREWORK_TAG).assertExists()
        compose.onNodeWithTag(RIGHT_FIREWORK_TAG).assertDoesNotExist()
        compose.onNodeWithTag(CENTER_FIREWORK_TAG).assertDoesNotExist()
        compose.runOnIdle { state.value = WinEffectsState(false, false) }
        compose.mainClock.advanceTimeBy(THIRD_FIREWORK_DELAY_MS + FRAME_MARGIN_MS)
        compose.onNodeWithTag(LEFT_FIREWORK_TAG).assertDoesNotExist()
        compose.onNodeWithTag(RIGHT_FIREWORK_TAG).assertDoesNotExist()
        compose.onNodeWithTag(CENTER_FIREWORK_TAG).assertDoesNotExist()
    }

    @Test fun allFireworksAppearAndANewIdResetsTheirStagger() {
        val state = mutableStateOf(WinEffectsState(true, false, FIRST_ID))
        compose.mainClock.autoAdvance = false
        compose.setContent { Box(Modifier.fillMaxSize()) { WinCelebration(state.value, Modifier.fillMaxSize()) } }
        compose.mainClock.advanceTimeBy(THIRD_FIREWORK_DELAY_MS + FRAME_MARGIN_MS)
        compose.onNodeWithTag(LEFT_FIREWORK_TAG).assertExists()
        compose.onNodeWithTag(RIGHT_FIREWORK_TAG).assertExists()
        compose.onNodeWithTag(CENTER_FIREWORK_TAG).assertExists()
        compose.runOnIdle { state.value = state.value.copy(celebrationId = SECOND_ID) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(RIGHT_FIREWORK_TAG).assertDoesNotExist()
        compose.onNodeWithTag(CENTER_FIREWORK_TAG).assertDoesNotExist()
    }

    @Test fun everyFireworkActuallyDrawsPixelsWithTheComposeRenderer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val composition = mutableStateOf<LottieComposition?>(null)
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color.White).testTag(RENDER_TAG)) {
                LottieAnimation(composition.value, progress = { VISIBLE_PROGRESS }, modifier = Modifier.fillMaxSize())
            }
        }
        for (resource in listOf(R.raw.firework_left, R.raw.firework_right, R.raw.firework_center)) {
            val parsed = LottieCompositionFactory.fromRawResSync(context, resource).value!!
            compose.runOnIdle { composition.value = parsed }
            val pixels = compose.onNodeWithTag(RENDER_TAG).captureToImage().toPixelMap()
            assertTrue((PIXEL_ORIGIN until pixels.width step PIXEL_STRIDE).any { x ->
                (PIXEL_ORIGIN until pixels.height step PIXEL_STRIDE).any { y -> pixels[x, y] != Color.White }
            })
        }
    }

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        const val FIRST_ID = 1L
        const val SECOND_ID = 2L
        const val NO_DURATION = 0f
        const val FRAME_MARGIN_MS = 100L
        const val RENDER_TAG = "firework-render"
        const val VISIBLE_PROGRESS = 0.4f
        const val PIXEL_ORIGIN = 0
        const val PIXEL_STRIDE = 4
    }
}
