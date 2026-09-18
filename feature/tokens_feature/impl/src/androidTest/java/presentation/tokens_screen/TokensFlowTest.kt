package presentation.tokens_screen

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavInflater
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.di.tokensFeatureModule
import com.cerebus.tokens.logger.api.Logger
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import domain.models.Token
import domain.models.resizeTokenProgress
import domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import presentation.SettingsTestActivity
import java.io.IOException
import com.cerebus.tokens.core.ui.R as CoreR
import com.cerebus.tokens.feature.tokens_feature.test.R as TestR

@RunWith(AndroidJUnit4::class)
class TokensFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val board = BoardRepository()
    private val effects = EffectsRepository()
    private val reinforcement = PhotoRepository()
    private val sound = SoundOutput()
    private val photo = PhotoMediator()

    @Before fun before() {
        startKoin {
            androidContext(context)
            modules(tokensFeatureModule, module {
                single<TokenBoardRepository> { board }
                single<WinEffectsRepository> { effects }
                single<ReinforcementRepository> { reinforcement }
                factory<WinSoundOutput> { sound }
                single<ReinforcementPhotoMediator> { photo }
            })
        }
    }
    @After fun after() { stopKoin() }

    private fun launch() = ActivityScenario.launch<SettingsTestActivity>(
        Intent(InstrumentationRegistry.getInstrumentation().context, SettingsTestActivity::class.java)
            .putExtra(SettingsTestActivity.EXTRA_START_BOARD, true),
    )
    private fun openMenu() = compose.onNodeWithContentDescription(context.getString(R.string.tokens_menu)).performClick()
    private fun click(id: String) = compose.onNodeWithTag(tokenTag(id)).performClick()
    private fun win() { click(FIRST_ID); click(LAST_ID) }

    @Test fun soundOnlyVictoryPlaysOnceAndRecreationDoesNotReplayIt() {
        launch().use { scenario ->
            win()
            compose.runOnIdle { assertEquals(listOf(Unit), sound.plays); assertTrue(sound.playing) }
            compose.onNodeWithTag(CELEBRATION_TAG).assertDoesNotExist()
            compose.runOnIdle { reinforcement.settings.value = ReinforcementSettings(photoUri = "unused") }
            compose.runOnIdle { assertEquals(listOf(Unit), sound.plays) }
            scenario.recreate()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOn()
            compose.runOnIdle { assertEquals(listOf(Unit), sound.plays); assertFalse(sound.playing) }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.runOnIdle { assertEquals(listOf(Unit), sound.plays); assertFalse(sound.playing) }
        }
    }

    @Test fun pendingVictoryCannotPlayAfterLeavingAndReturning() {
        val gate = CompletableDeferred<Unit>()
        launch().use { scenario ->
            click(FIRST_ID)
            compose.onNodeWithTag(tokenTag(FIRST_ID)).assertIsOn()
            compose.runOnIdle { board.beforeWrite = { gate.await() } }
            click(LAST_ID)
            openMenu()
            compose.onNodeWithText(context.getString(R.string.settings)).performClick()
            compose.runOnIdle { gate.complete(Unit) }
            scenario.onActivity {
                val host = it.supportFragmentManager.findFragmentById(TestR.id.feature_test_host) as NavHostFragment
                assertTrue(host.navController.popBackStack())
            }
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOn()
            compose.runOnIdle { assertTrue(sound.plays.isEmpty()) }
        }
    }

    @Test fun clearSwipeStopsSoundAndNewWinCanPlayAgain() {
        launch().use {
            win()
            compose.runOnIdle { assertTrue(sound.playing) }
            compose.onNodeWithTag(TOKEN_BOARD_TAG).performTouchInput { swipeLeft() }
            compose.onNodeWithTag(tokenTag(FIRST_ID)).assertIsOff()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOff()
            compose.runOnIdle { assertFalse(sound.playing) }
            win()
            compose.runOnIdle { assertEquals(listOf(Unit, Unit), sound.plays) }
            openMenu()
            compose.onNodeWithText(context.getString(R.string.clearChecked)).performClick()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOff()
        }
    }

    @Test fun writeFailureDoesNotWinAndRetryCompletesOnce() {
        launch().use {
            click(FIRST_ID)
            compose.runOnIdle { board.failWrite = true }
            click(LAST_ID)
            compose.onNodeWithText(context.getString(CoreR.string.storage_write_error)).assertIsDisplayed()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOff()
            compose.runOnIdle { assertTrue(sound.plays.isEmpty()); board.failWrite = false }
            compose.onNodeWithText(context.getString(CoreR.string.storage_write_error)).performClick()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOn()
            compose.runOnIdle { assertEquals(listOf(Unit), sound.plays) }
        }
    }

    @Test fun countDialogUpdatesBoardThroughFlowWithoutVictory() {
        launch().use {
            click(FIRST_ID)
            openMenu()
            compose.onNodeWithText(context.getString(R.string.changeChips)).performClick()
            compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).performClick()
            compose.onNodeWithText(context.getString(CoreR.string.OK)).performClick()
            compose.onAllNodesWithContentDescription(context.getString(R.string.token_description)).assertCountEquals(RESIZED_COUNT)
            compose.onNodeWithTag(tokenTag(FIRST_ID)).assertIsOn()
            compose.runOnIdle { assertTrue(sound.plays.isEmpty()) }
        }
    }

    @Test fun animationOnlyVictoryHasNoSoundAndStopsOnBackground() {
        effects.settings.value = EffectsSettings(animation = true, sound = false)
        launch().use { scenario ->
            win()
            compose.onNodeWithTag(CELEBRATION_TAG).assertExists()
            compose.runOnIdle { assertTrue(sound.plays.isEmpty()) }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithTag(CELEBRATION_TAG).assertDoesNotExist()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOn()
        }
    }

    @Test fun twentyTokenSelectionSurvivesRotationAndRecreationWithoutVictory() {
        launch().use { scenario ->
            click(FIRST_ID)
            openMenu()
            compose.onNodeWithText(context.getString(R.string.changeChips)).performClick()
            repeat(MAX_TOKEN_COUNT - board.board.value.count) {
                compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).performClick()
            }
            compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).assertIsNotEnabled()
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            compose.waitUntil(ROTATION_TIMEOUT_MS) {
                context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            }
            compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(MAX_TOKEN_COUNT.toString())
            compose.onNodeWithText(context.getString(CoreR.string.OK)).performClick()
            listOf(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT).forEach { orientation ->
                scenario.onActivity { it.requestedOrientation = orientation }
                val expected = if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
                compose.waitUntil(ROTATION_TIMEOUT_MS) { context.resources.configuration.orientation == expected }
                compose.onAllNodesWithContentDescription(context.getString(R.string.token_description)).assertCountEquals(MAX_TOKEN_COUNT)
                board.board.value.tokens.forEach { compose.onNodeWithTag(tokenTag(it.id)).assertIsDisplayed() }
                compose.onNodeWithTag(tokenTag(FIRST_ID)).assertIsOn()
            }
            scenario.recreate()
            compose.onAllNodesWithContentDescription(context.getString(R.string.token_description)).assertCountEquals(MAX_TOKEN_COUNT)
            compose.onNodeWithTag(tokenTag(FIRST_ID)).assertIsOn()
            compose.runOnIdle { assertTrue(sound.plays.isEmpty()) }
        }
    }

    @Test fun androidPlayerCanPrepareStartStopAndReleaseAgain() {
        val messages = mutableListOf<String>()
        val logger = object : Logger {
            override fun d(message: String): Int { messages += message; return LOG_RESULT }
            override fun i(message: String) = LOG_RESULT
            override fun w(message: String): Int { messages += message; return LOG_RESULT }
            override fun e(message: String): Int { messages += message; return LOG_RESULT }
        }
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val player = WinSoundPlayer(activity, logger)
                try {
                    player.play()
                    player.stop()
                    player.stop()
                    player.play()
                } finally { player.stop() }
            }
            assertEquals(EXPECTED_NATIVE_STARTS, messages.count { it.startsWith("Fanfare: started") })
            assertFalse(messages.any { it.contains("error", ignoreCase = true) || it.contains("denied") })
        }
    }

    @Test fun rotationStopsVictoryAndPreservesTokenProgress() {
        effects.settings.value = EffectsSettings(animation = true, sound = true)
        launch().use { scenario ->
            win()
            compose.runOnIdle { assertEquals(listOf(Unit), sound.plays) }
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            compose.waitUntil(ROTATION_TIMEOUT_MS) {
                context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            }
            compose.onNodeWithTag(tokenTag(FIRST_ID)).assertIsOn()
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOn()
            compose.onNodeWithTag(CELEBRATION_TAG).assertDoesNotExist()
            compose.runOnIdle { assertFalse(sound.playing); assertEquals(listOf(Unit), sound.plays) }
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(ROTATION_TIMEOUT_MS) {
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            compose.onNodeWithTag(tokenTag(LAST_ID)).assertIsOn()
        }
    }

    private class SoundOutput : WinSoundOutput {
        val plays = mutableListOf<Unit>()
        var playing = false
        override fun play() { plays += Unit; playing = true }
        override fun stop() { playing = false }
    }
    private class PhotoMediator : ReinforcementPhotoMediator {
        override fun createGraph(inflater: NavInflater): NavGraph = error("No photo graph in isolated board tests")
        override fun open(navController: NavController) = Unit
    }
    private class EffectsRepository : WinEffectsRepository {
        override val settings = MutableStateFlow(EffectsSettings(animation = false, sound = true))
        override suspend fun setAnimation(enabled: Boolean) { settings.update { it.copy(animation = enabled) } }
        override suspend fun setSound(enabled: Boolean) { settings.update { it.copy(sound = enabled) } }
    }
    private class PhotoRepository : ReinforcementRepository {
        override val settings = MutableStateFlow(ReinforcementSettings())
        override suspend fun setEnabled(enabled: Boolean) { settings.update { it.copy(enabled = enabled) } }
        override suspend fun setPhotoUri(uri: String) { settings.update { it.copy(photoUri = uri) } }
    }
    private class BoardRepository : TokenBoardRepository {
        override val board = MutableStateFlow(TokenBoard(
            listOf(Token(false, COLOR, FIRST_ID), Token(false, COLOR, LAST_ID)), COLOR, INITIAL_BOARD_REVISION,
        ))
        var beforeWrite: suspend () -> Unit = {}
        var failWrite = false
        private suspend fun write() { beforeWrite(); if (failWrite) throw IOException("Expected write failure") }
        override suspend fun toggle(id: String): TokenChange {
            write()
            val old = board.value
            val tokens = old.tokens.map { if (it.id == id) it.copy(isChecked = !it.isChecked) else it }
            val changed = tokens != old.tokens
            val next = if (changed) old.copy(tokens = tokens, revision = old.revision + REVISION_STEP) else old
            board.value = next
            return TokenChange(next, changed, changed && !old.completed && next.completed)
        }
        override suspend fun clear() {
            write()
            board.update { it.copy(tokens = it.tokens.map { token -> token.copy(isChecked = false) }, revision = it.revision + REVISION_STEP) }
        }
        override suspend fun resize(count: Int) {
            write()
            board.update { old ->
                val marks = resizeTokenProgress(old.tokens.map { it.isChecked }, count)
                old.copy(tokens = marks.mapIndexed { index, checked ->
                    old.tokens.getOrNull(index)?.copy(isChecked = checked) ?: Token(checked, old.color, "new-$index")
                }, revision = old.revision + REVISION_STEP)
            }
        }
        override suspend fun setColor(color: Int) { write(); board.update { it.copy(color = color, revision = it.revision + REVISION_STEP) } }
        override suspend fun setChecked(id: String, checked: Boolean): TokenChange = error("Not used")
    }
    private companion object {
        const val FIRST_ID = "first-token"
        const val LAST_ID = "last-token"
        const val COLOR = -65536
        const val REVISION_STEP = 1L
        const val RESIZED_COUNT = 3
        const val LOG_RESULT = 0
        const val EXPECTED_NATIVE_STARTS = 2
        const val ROTATION_TIMEOUT_MS = 5_000L
    }
}
