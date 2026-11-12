package com.cerebus.tokens.reinforcement_photo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import androidx.lifecycle.ViewModelProvider
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.core.ui.NAVIGATION_DIALOG_TAG
import androidx.compose.ui.geometry.Offset
import com.cerebus.tokens.reinforcement_photo.api.*
import kotlinx.serialization.Serializable
import org.koin.core.context.GlobalContext
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import com.cerebus.tokens.reinforcement_photo.di.reinforcementModule
import com.cerebus.tokens.reinforcement_photo.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.IOException

@Serializable private data object PhotoTestHome

/** Real Compose Navigation + ActivityResultRegistry; external apps are intercepted. */
class PhotoTestActivity : ComponentActivity() {
    lateinit var navController: NavHostController
        private set
    var launches = NO_LAUNCHES
    var lastIntent: Intent? = null
    var failCamera = false
    private var requestCode = NO_REQUEST

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.createConfigurationContext(Configuration(newBase.resources.configuration).apply {
            fontScale = LARGE_FONT
        }))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCode = savedInstanceState?.getInt(REQUEST_KEY) ?: NO_REQUEST
        val mediator = GlobalContext.get().get<ReinforcementPhotoMediator>()
        setContent {
            TokensTheme {
                val controller = rememberNavController()
                navController = controller
                NavHost(controller, startDestination = PhotoTestHome,
                    enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    composable<PhotoTestHome> { }
                    mediator.registerGraph(this, controller)
                }
                LaunchedEffect(controller) { if (savedInstanceState == null) mediator.open(controller) }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(REQUEST_KEY, requestCode)
        super.onSaveInstanceState(outState)
    }
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        if (failCamera && intent.action == MediaStore.ACTION_IMAGE_CAPTURE) throw ActivityNotFoundException()
        launches++
        lastIntent = intent
        this.requestCode = requestCode
    }
    @Suppress("DEPRECATION")
    fun completeResult(result: Int, data: Intent? = null) { onActivityResult(requestCode, result, data) }
    companion object {
        const val LARGE_FONT = 2f
        const val NO_LAUNCHES = 0
        private const val NO_REQUEST = -1
        private const val REQUEST_KEY = "test-request"
    }
}

@RunWith(AndroidJUnit4::class)
class PhotoLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val repository = TestRepository()
    private val files = TestFiles()
    @Before fun before() {
        startKoin {
            androidContext(instrumentation.targetContext)
            modules(reinforcementModule, module {
                single<ReinforcementRepository> { repository }
                single<PhotoFiles> { files }
            })
        }
    }
    @After fun after() { stopKoin() }
    private fun launch() = ActivityScenario.launch<PhotoTestActivity>(Intent(instrumentation.context, PhotoTestActivity::class.java))

    @Test fun photoActionsRemainReachableAfterRotationAndRecreationWithLargeFont() {
        launch().use { scenario ->
            listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE).forEach { orientation ->
                scenario.onActivity { it.requestedOrientation = orientation }
                val expected = if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
                compose.waitUntil(TIMEOUT_MS) {
                    var actual = Configuration.ORIENTATION_UNDEFINED
                    scenario.onActivity { actual = it.resources.configuration.orientation }
                    actual == expected
                }
                assertActions()
            }
            scenario.recreate()
            assertActions()
            compose.onNodeWithTag(PHOTO_CLOSE_TAG).assertIsDisplayed().performClick()
            assertClosed(scenario)
            assertEquals(NO_WRITES, repository.writes)
        }
    }

    @Test fun galleryCancellationKeepsPhotoAndDialogOpen() {
        repository.settings.value = ReinforcementSettings(photoUri = OLD_URI)
        launch().use { scenario ->
            click(PHOTO_GALLERY_TAG)
            scenario.onActivity {
                assertEquals(SINGLE_OPERATION, it.launches)
                it.completeResult(Activity.RESULT_CANCELED)
            }
            assertActions()
            assertEquals(OLD_URI, repository.settings.value.photoUri)
            assertEquals(NO_WRITES, repository.writes)
        }
    }

    @Test fun earlyResultAfterRecreationWaitsForComposeLauncherAndSavesOnce() {
        launch().use { scenario ->
            click(PHOTO_GALLERY_TAG)
            scenario.recreate()
            // Delivery may precede the new composition registering its remembered launcher.
            scenario.onActivity { it.completeResult(Activity.RESULT_OK, Intent().setData(SELECTED_URI.toUri())) }
            assertClosed(scenario)
            assertEquals(IMPORTED_URI, repository.settings.value.photoUri)
            assertEquals(SINGLE_OPERATION, repository.writes)
            assertEquals(SINGLE_OPERATION, files.imports)
        }
    }

    @Test fun cameraResultAfterRecreationBlocksDismissalUntilSavedWithoutRelaunch() {
        repository.gate = CompletableDeferred()
        launch().use { scenario ->
            click(PHOTO_CAMERA_TAG)
            scenario.onActivity {
                assertEquals(SINGLE_OPERATION, it.launches)
                assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, it.lastIntent!!.action)
                @Suppress("DEPRECATION")
                assertEquals(CAPTURE_URI.toUri(), it.lastIntent!!.getParcelableExtra(MediaStore.EXTRA_OUTPUT))
            }
            scenario.recreate()
            compose.waitForIdle()
            scenario.onActivity {
                assertEquals(PhotoTestActivity.NO_LAUNCHES, it.launches)
                it.completeResult(Activity.RESULT_OK)
                // Native cancellation must be blocked synchronously, not just after recomposition.
                assertFalse(ViewModelProvider.create(it.navController.currentBackStackEntry!!)[ChangePhotoViewModel::class].state.value.cancellable)
            }
            compose.onNodeWithTag(PHOTO_CLOSE_TAG).assertIsDisplayed().assertIsNotEnabled()
            onView(isRoot()).inRoot(isDialog()).perform(pressBack())
            scenario.onActivity { assertTrue(it.navController.currentDestination!!.hasRoute<PhotoDestination>()) }
            repository.gate!!.complete(Unit)
            assertClosed(scenario)
            assertEquals(CAPTURE_URI, repository.settings.value.photoUri)
            assertEquals(SINGLE_OPERATION, repository.writes)
        }
    }

    @Test fun missingCameraLeavesGalleryAvailableAndSelectionSaves() {
        launch().use { scenario ->
            scenario.onActivity { it.failCamera = true }
            click(PHOTO_CAMERA_TAG)
            compose.onNodeWithTag(PHOTO_RETRY_TAG).performScrollTo().assertIsDisplayed()
            assertEquals(listOf(CAPTURE_URI), files.removed)
            click(PHOTO_GALLERY_TAG)
            scenario.onActivity { it.completeResult(Activity.RESULT_OK, Intent().setData(SELECTED_URI.toUri())) }
            assertClosed(scenario)
            assertEquals(IMPORTED_URI, repository.settings.value.photoUri)
        }
    }

    @Test fun idleDialogCanBeDismissedWithBack() {
        launch().use { scenario ->
            assertActions()
            onView(isRoot()).inRoot(isDialog()).perform(pressBack())
            assertClosed(scenario)
            assertEquals(NO_WRITES, repository.writes)
        }
    }

    @Test fun outsideTapCancelsIdleDialogButCannotInterruptSave() {
        repository.gate = CompletableDeferred()
        launch().use { scenario ->
            click(PHOTO_GALLERY_TAG)
            scenario.onActivity { it.completeResult(Activity.RESULT_OK, Intent().setData(SELECTED_URI.toUri())) }
            compose.onNodeWithTag(PHOTO_CLOSE_TAG).assertIsDisplayed().assertIsNotEnabled()
            outsideTap()
            scenario.onActivity { assertTrue(it.navController.currentDestination!!.hasRoute<PhotoDestination>()) }
            repository.gate!!.complete(Unit)
            assertClosed(scenario)
            scenario.onActivity { GlobalContext.get().get<ReinforcementPhotoMediator>().open(it.navController) }
            assertActions()
            outsideTap()
            assertClosed(scenario)
            assertEquals(SINGLE_OPERATION, repository.writes)
        }
    }

    private fun outsideTap() {
        compose.onNodeWithTag(NAVIGATION_DIALOG_TAG).performTouchInput { click(Offset(OUTSIDE_OFFSET, OUTSIDE_OFFSET)) }
        compose.waitForIdle()
    }

    @Test fun failedSaveCanRetryWithoutRepeatingGalleryImport() {
        repository.fail = true
        launch().use { scenario ->
            click(PHOTO_GALLERY_TAG)
            scenario.onActivity { it.completeResult(Activity.RESULT_OK, Intent().setData(SELECTED_URI.toUri())) }
            compose.onNodeWithTag(PHOTO_RETRY_TAG).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag(PHOTO_GALLERY_TAG).performScrollTo().assertIsNotEnabled()
            assertNull(repository.settings.value.photoUri)
            repository.fail = false
            click(PHOTO_RETRY_TAG)
            assertClosed(scenario)
            assertEquals(SINGLE_OPERATION, files.imports)
            assertEquals(IMPORTED_URI, repository.settings.value.photoUri)
        }
    }

    private fun click(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); compose.waitForIdle() }
    private fun assertActions() {
        compose.onNodeWithTag(PHOTO_CLOSE_TAG).assertIsDisplayed().assertIsEnabled()
        listOf(PHOTO_CAMERA_TAG, PHOTO_GALLERY_TAG).forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertIsEnabled()
        }
    }
    private fun assertClosed(scenario: ActivityScenario<PhotoTestActivity>) {
        compose.waitUntil(TIMEOUT_MS) {
            var closed = false
            scenario.onActivity { closed = it.navController.currentDestination?.hasRoute<PhotoTestHome>() == true }
            closed
        }
    }
    private class TestRepository : ReinforcementRepository {
        override val settings = MutableStateFlow(ReinforcementSettings())
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        var writes = NO_WRITES
        override suspend fun setEnabled(enabled: Boolean) = error("Not expected")
        override suspend fun setPhotoUri(uri: String) {
            gate?.await()
            if (fail) throw IOException("Test failure")
            writes++
            settings.update { it.copy(photoUri = uri) }
        }
    }
    private class TestFiles : PhotoFiles {
        val removed = mutableListOf<String>()
        var imports = NO_WRITES
        override suspend fun createCapture() = CAPTURE_URI
        override suspend fun importGallery(uri: String): String { imports++; return IMPORTED_URI }
        override suspend fun validateCapture(uri: String) = Unit
        override fun discard(uri: String) { removed += uri }
        override fun releaseSource(uri: String) = Unit
    }
    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val OUTSIDE_OFFSET = 4f
        const val NO_WRITES = 0
        const val SINGLE_OPERATION = 1
        const val CAPTURE_URI = "content://test/capture.jpg"
        const val OLD_URI = "content://test/previous.jpg"
        const val SELECTED_URI = "content://test/gallery.jpg"
        const val IMPORTED_URI = "content://test/imported.img"
    }
}
