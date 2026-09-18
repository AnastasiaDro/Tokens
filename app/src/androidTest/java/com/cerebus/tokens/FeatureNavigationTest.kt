package com.cerebus.tokens

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.feature.tokens_feature.api.TokensEntry
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class FeatureNavigationTest {
    @Test fun boardSettingsAndPhotoKeepTheirDestinationInBothOrientations() {
        val koin = GlobalContext.get()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            listOf(TOKENS_SCREEN, SETTINGS_SCREEN, PHOTO_SCREEN).forEach { screen ->
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                    when (screen) {
                        SETTINGS_SCREEN -> koin.get<TokensMediator>().open(host.navController, TokensEntry.SETTINGS)
                        PHOTO_SCREEN -> koin.get<ReinforcementPhotoMediator>().open(host.navController)
                    }
                }
                listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE).forEach { orientation ->
                    scenario.onActivity { it.requestedOrientation = orientation }
                    val expected = if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
                    val deadline = SystemClock.uptimeMillis() + ROTATION_TIMEOUT_MS
                    var actual = Configuration.ORIENTATION_UNDEFINED
                    while (actual != expected && SystemClock.uptimeMillis() < deadline) {
                        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                        scenario.onActivity { actual = it.resources.configuration.orientation }
                    }
                    assertEquals(expected, actual)
                    scenario.onActivity { activity ->
                        val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                        assertEquals(screen, host.navController.currentDestination?.label)
                    }
                }
            }
        }
    }

    @Test fun settingsEntryRestoresAndReturnsToBoard() {
        val mediator = GlobalContext.get().get<TokensMediator>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                mediator.open(host.navController, TokensEntry.SETTINGS)
                assertEquals(SETTINGS_SCREEN, host.navController.currentDestination?.label)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                assertEquals(SETTINGS_SCREEN, host.navController.currentDestination?.label)
                assertTrue(host.navController.popBackStack())
                assertEquals(TOKENS_SCREEN, host.navController.currentDestination?.label)
            }
        }
    }

    @Test fun tokensEntriesCanBeOpenedFromPhotoGraph() {
        val koin = GlobalContext.get()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                koin.get<ReinforcementPhotoMediator>().open(host.navController)
                koin.get<TokensMediator>().open(host.navController, TokensEntry.SETTINGS)
                assertEquals(SETTINGS_SCREEN, host.navController.currentDestination?.label)
                koin.get<TokensMediator>().open(host.navController, TokensEntry.BOARD)
                assertEquals(TOKENS_SCREEN, host.navController.currentDestination?.label)
            }
        }
    }

    @Test fun photoMediatorOpensDialogAndRestoresBackStackAfterRecreation() {
        val koin = GlobalContext.get()
        val mediator = koin.get<ReinforcementPhotoMediator>()
        assertSame(mediator, koin.get<ReinforcementPhotoMediator>())
        assertSame(koin.get<ReinforcementRepository>(), koin.get<ReinforcementRepository>())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                assertEquals(TOKENS_SCREEN, host.navController.currentDestination?.label)
                mediator.open(host.navController)
                assertEquals(PHOTO_SCREEN, host.navController.currentDestination?.label)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
                assertEquals(PHOTO_SCREEN, host.navController.currentDestination?.label)
                assertTrue(host.navController.popBackStack())
                assertEquals(TOKENS_SCREEN, host.navController.currentDestination?.label)
            }
        }
    }

    private companion object {
        const val TOKENS_SCREEN = "TokensFragment"
        const val PHOTO_SCREEN = "AskForReinforcementImageDialog"
        const val SETTINGS_SCREEN = "SettingsFragment"
        const val ROTATION_TIMEOUT_MS = 5_000L
    }
}
