package presentation

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.di.tokensFeatureModule
import domain.models.Token
import domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import presentation.settings_screen.SETTINGS_COUNT_TAG
import presentation.settings_screen.SETTINGS_COLOR_TAG
import presentation.settings_screen.COLOR_CONFIRM_TAG
import presentation.settings_screen.COLOR_PICKER_TAG
import presentation.settings_screen.COLOR_PREVIEW_TAG
import presentation.settings_screen.SelectColorDialogFragment
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertNotEquals
import presentation.tokens_screen.COUNT_VALUE_TAG
import presentation.tokens_screen.SelectTokenNumberAlert
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import com.cerebus.tokens.core.ui.R as CoreR
import com.cerebus.tokens.feature.tokens_feature.test.R as TestR

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val board = TestBoardRepository()

    @Before fun before() {
        startKoin {
            androidContext(context)
            modules(tokensFeatureModule, module {
                single<TokenBoardRepository> { board }
                single<WinEffectsRepository> { TestEffectsRepository() }
                single<ReinforcementRepository> { TestReinforcementRepository() }
            })
        }
    }
    @After fun after() { stopKoin() }

    private fun launch() = ActivityScenario.launch<SettingsTestActivity>(
        Intent(InstrumentationRegistry.getInstrumentation().context, SettingsTestActivity::class.java),
    )

    private fun openCount() {
        compose.onNodeWithText(context.getString(CoreR.string.change)).performScrollTo().performClick()
        compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(INITIAL_COUNT.toString())
    }

    @Test fun draftSurvivesActivityRecreationAndSavedCountUpdatesSettings() {
        launch().use { scenario ->
            openCount()
            compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).performClick()
            compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(CHANGED_COUNT.toString())
            assertEquals(INITIAL_COUNT, board.board.value.count)
            scenario.recreate()
            compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(CHANGED_COUNT.toString())
            compose.onNodeWithText(context.getString(CoreR.string.OK)).performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertTextEquals(CHANGED_COUNT.toString())
            assertEquals(CHANGED_COUNT, board.board.value.count)
            assertEquals(SINGLE_WRITE, board.writes.get())
        }
    }

    @Test fun cancelDiscardsDraftAndReopeningUsesSavedCount() {
        launch().use {
            openCount()
            compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).performClick()
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertTextEquals(INITIAL_COUNT.toString())
            openCount()
            assertEquals(NO_WRITES, board.writes.get())
        }
    }

    @Test fun composeSettingsOpensColorDialogAndCancelDiscardsDraft() {
        launch().use { scenario ->
            compose.onNodeWithText(context.getString(R.string.select_button_text)).performScrollTo().performClick()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(TestR.id.feature_test_host) as NavHostFragment
                assertEquals(R.id.selectColorDialogFragment, host.navController.currentDestination?.id)
            }
            changeColor()
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).performScrollTo().performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertTextEquals(INITIAL_COUNT.toString())
            assertEquals(NO_WRITES, board.writes.get())
            openColor()
            assertEquals(TOKEN_COLOR, previewColor())
        }
    }

    private fun openColor() {
        compose.onNodeWithText(context.getString(R.string.select_button_text)).performScrollTo().performClick()
        compose.onNodeWithTag(COLOR_PREVIEW_TAG).assertExists()
    }

    private fun previewColor(): Int {
        val node = compose.onNodeWithTag(COLOR_PREVIEW_TAG).performScrollTo().fetchSemanticsNode()
        val windowOrigin = IntArray(POINT_DIMENSIONS)
        onView(isRoot()).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            view.getLocationOnScreen(windowOrigin)
        }
        // captureToImage falls back to the Activity window for a ComposeView in DialogFragment.
        // Inspect the actual dialog pixels in the full display screenshot instead.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Native window fade animations are not controlled by Compose's test clock.
        automation.waitForIdle(WINDOW_IDLE_MS, SCREENSHOT_TIMEOUT_MS)
        val bitmap = automation.takeScreenshot()
        return try {
            bitmap.getPixel(
                windowOrigin[X_AXIS] + node.boundsInWindow.center.x.toInt(),
                windowOrigin[Y_AXIS] + node.boundsInWindow.center.y.toInt(),
            )
        } finally { bitmap.recycle() }
    }

    private fun settingsColor(): Int {
        val pixels = compose.onNodeWithTag(SETTINGS_COLOR_TAG).performScrollTo().captureToImage().toPixelMap()
        return pixels[pixels.width / HALF_DIVISOR, pixels.height / HALF_DIVISOR].toArgb()
    }

    private fun changeColor(): Int {
        compose.onNodeWithTag(COLOR_PICKER_TAG).performScrollTo().performTouchInput {
            click(Offset(width * PICKER_X_FRACTION, height * PICKER_Y_FRACTION))
        }
        val selected = previewColor()
        assertNotEquals(TOKEN_COLOR, selected)
        return selected
    }

    @Test fun colorDraftSurvivesRecreationAndConfirmationWritesOnce() {
        launch().use { scenario ->
            openColor()
            val selected = changeColor()
            assertEquals(NO_WRITES, board.writes.get())
            scenario.recreate()
            assertEquals(selected, previewColor())
            compose.onNodeWithTag(COLOR_CONFIRM_TAG).performScrollTo().performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertExists()
            assertEquals(selected, board.board.value.color)
            assertEquals(selected, settingsColor())
            assertEquals(SINGLE_WRITE, board.writes.get())
            openColor()
            assertEquals(selected, previewColor())
        }
    }

    @Test fun colorSavingBlocksBackAcrossRecreationAndFailureCanRetry() {
        val gate = CompletableDeferred<Unit>()
        board.beforeWrite = { gate.await() }
        board.failWrite = true
        launch().use { scenario ->
            openColor()
            val selected = changeColor()
            compose.onNodeWithTag(COLOR_CONFIRM_TAG).performScrollTo().performClick()
            compose.onNodeWithTag(COLOR_CONFIRM_TAG).assertIsNotEnabled()
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).assertIsNotEnabled()
            scenario.recreate()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(TestR.id.feature_test_host) as NavHostFragment
                assertFalse(host.childFragmentManager.fragments.filterIsInstance<SelectColorDialogFragment>().single().isCancelable)
            }
            onView(isRoot()).inRoot(isDialog()).perform(pressBack())
            assertEquals(selected, previewColor())
            compose.runOnIdle { gate.complete(Unit) }
            compose.onNodeWithText(context.getString(CoreR.string.storage_save_error)).performScrollTo().assertIsDisplayed()
            assertEquals(selected, previewColor())
            assertEquals(TOKEN_COLOR, board.board.value.color)
            compose.runOnIdle { board.failWrite = false }
            compose.onNodeWithTag(COLOR_CONFIRM_TAG).performScrollTo().performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertExists()
            assertEquals(selected, board.board.value.color)
            assertEquals(RETRY_WRITES, board.writes.get())
        }
    }

    @Test fun openingAndConfirmingDarkColorDoesNotChangeIt() {
        board.board.update { it.copy(color = DARK_COLOR) }
        launch().use { scenario ->
            openColor()
            assertEquals(DARK_COLOR, previewColor())
            scenario.recreate()
            assertEquals(DARK_COLOR, previewColor())
            compose.onNodeWithTag(COLOR_CONFIRM_TAG).performScrollTo().performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertExists()
            assertEquals(DARK_COLOR, board.board.value.color)
        }
    }

    @Test fun savingSurvivesRecreationBlocksBackAndFailureCanRetry() {
        val gate = CompletableDeferred<Unit>()
        board.beforeWrite = { gate.await() }
        board.failWrite = true
        launch().use { scenario ->
            openCount()
            compose.onNodeWithContentDescription(context.getString(R.string.increase_tokens_count)).performClick()
            compose.onNodeWithText(context.getString(CoreR.string.OK)).performClick()
            compose.onNodeWithText(context.getString(CoreR.string.OK)).assertIsNotEnabled()
            compose.onNodeWithText(context.getString(CoreR.string.cancel)).assertIsNotEnabled()
            scenario.recreate()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(TestR.id.feature_test_host) as NavHostFragment
                val dialog = host.childFragmentManager.fragments.filterIsInstance<SelectTokenNumberAlert>().single()
                assertFalse(dialog.isCancelable)
            }
            onView(isRoot()).inRoot(isDialog()).perform(pressBack())
            compose.onNodeWithTag(COUNT_VALUE_TAG).assertTextEquals(CHANGED_COUNT.toString())
            compose.runOnIdle { gate.complete(Unit) }
            compose.onNodeWithText(context.getString(CoreR.string.storage_save_error)).assertIsDisplayed()
            assertEquals(INITIAL_COUNT, board.board.value.count)
            compose.runOnIdle { board.failWrite = false }
            compose.onNodeWithText(context.getString(CoreR.string.OK)).performClick()
            compose.onNodeWithTag(SETTINGS_COUNT_TAG).assertTextEquals(CHANGED_COUNT.toString())
            assertEquals(RETRY_WRITES, board.writes.get())
        }
    }

    private class TestBoardRepository : TokenBoardRepository {
        override val board = MutableStateFlow(TokenBoard(
            List(INITIAL_COUNT) { Token(false, TOKEN_COLOR, "test-token-$it") }, TOKEN_COLOR, INITIAL_BOARD_REVISION,
        ))
        val writes = AtomicInteger()
        var beforeWrite: suspend () -> Unit = {}
        var failWrite = false
        override suspend fun resize(count: Int) {
            writes.incrementAndGet()
            beforeWrite()
            if (failWrite) throw IOException("Expected test failure")
            board.update { old -> old.copy(tokens = List(count) { index ->
                old.tokens.getOrNull(index) ?: Token(false, TOKEN_COLOR, "test-token-$index")
            }) }
        }
        override suspend fun setColor(color: Int) {
            writes.incrementAndGet()
            beforeWrite()
            if (failWrite) throw IOException("Expected test failure")
            board.update { it.copy(color = color, tokens = it.tokens.map { token -> token.copy(checkedColor = color) }) }
        }
        override suspend fun toggle(id: String): TokenChange = error("Not used in settings tests")
        override suspend fun setChecked(id: String, checked: Boolean): TokenChange = error("Not used in settings tests")
        override suspend fun clear() = error("Not used in settings tests")
    }

    private class TestEffectsRepository : WinEffectsRepository {
        override val settings = MutableStateFlow(EffectsSettings())
        override suspend fun setAnimation(enabled: Boolean) { settings.update { it.copy(animation = enabled) } }
        override suspend fun setSound(enabled: Boolean) { settings.update { it.copy(sound = enabled) } }
    }

    private class TestReinforcementRepository : ReinforcementRepository {
        override val settings = MutableStateFlow(ReinforcementSettings())
        override suspend fun setEnabled(enabled: Boolean) { settings.update { it.copy(enabled = enabled) } }
        override suspend fun setPhotoUri(uri: String) { settings.update { it.copy(photoUri = uri) } }
    }

    private companion object {
        const val INITIAL_COUNT = 5
        const val CHANGED_COUNT = 6
        const val TOKEN_COLOR = -65536
        const val NO_WRITES = 0
        const val SINGLE_WRITE = 1
        const val RETRY_WRITES = 2
        const val HALF_DIVISOR = 2
        const val PICKER_X_FRACTION = 0.3f
        const val PICKER_Y_FRACTION = 0.3f
        const val DARK_COLOR = -14663584
        const val POINT_DIMENSIONS = 2
        const val X_AXIS = 0
        const val Y_AXIS = 1
        const val WINDOW_IDLE_MS = 500L
        const val SCREENSHOT_TIMEOUT_MS = 5_000L
    }
}
