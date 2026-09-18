package com.cerebus.tokens.reinforcement_photo

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import com.cerebus.tokens.logger.api.LoggerFactory
import com.cerebus.tokens.logger.impl.LoggerFactoryImpl
import com.cerebus.tokens.reinforcement_photo.di.reinforcementModule
import com.cerebus.tokens.reinforcement_photo.presentation.AskForReinforcementImageDialog
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** Real photo dialog, isolated storage and a large font without changing device settings. */
class PhotoTestActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.createConfigurationContext(Configuration(newBase.resources.configuration).apply {
            fontScale = LARGE_FONT
        }))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) AskForReinforcementImageDialog().showNow(supportFragmentManager, PHOTO_TAG)
    }
    companion object {
        const val PHOTO_TAG = "test-photo"
        const val LARGE_FONT = 2f
    }
}

@RunWith(AndroidJUnit4::class)
class PhotoLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Before fun before() {
        startKoin {
            androidContext(instrumentation.targetContext)
            modules(reinforcementModule, module {
                single<LoggerFactory> { LoggerFactoryImpl() }
                single<ReinforcementRepository> { object : ReinforcementRepository {
                    override val settings = MutableStateFlow(ReinforcementSettings())
                    override suspend fun setEnabled(enabled: Boolean) = error("Layout test must not write")
                    override suspend fun setPhotoUri(uri: String) = error("Layout test must not write")
                } }
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
                val deadline = android.os.SystemClock.uptimeMillis() + ROTATION_TIMEOUT_MS
                var actual = Configuration.ORIENTATION_UNDEFINED
                while (actual != expected && android.os.SystemClock.uptimeMillis() < deadline) {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { actual = it.resources.configuration.orientation }
                }
                assertEquals(expected, actual)
                assertActions()
            }
            scenario.recreate()
            assertActions()
            onView(withId(R.id.cancel_button)).inRoot(isDialog()).perform(scrollTo(), click())
            scenario.onActivity { assertTrue(it.supportFragmentManager.fragments.isEmpty()) }
        }
    }

    @Test fun shortPhotoWindowScrollsToEveryAction() {
        launch().use { scenario ->
            scenario.onActivity {
                val dialog = it.supportFragmentManager.findFragmentByTag(PhotoTestActivity.PHOTO_TAG) as AskForReinforcementImageDialog
                val density = it.resources.displayMetrics.density
                dialog.requireDialog().window!!.setLayout((NARROW_WIDTH * density).toInt(), (SHORT_HEIGHT * density).toInt())
            }
            assertActions()
        }
    }

    private fun assertActions() {
        listOf(R.id.cancel_button, R.id.make_photo_button, R.id.get_from_gallery_button).forEach { id ->
            onView(withId(id)).inRoot(isDialog()).perform(scrollTo()).check(matches(isDisplayed())).check(matches(isEnabled()))
        }
    }
    private companion object {
        const val ROTATION_TIMEOUT_MS = 5_000L
        const val NARROW_WIDTH = 320
        const val SHORT_HEIGHT = 200
    }
}
