package com.cerebus.tokens

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.core.ui.popEntryIfCurrent
import com.cerebus.tokens.feature.tokens_feature.api.*
import com.cerebus.tokens.reinforcement_photo.api.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class FeatureNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val koin get() = GlobalContext.get()

    @Test fun boardSettingsAndPhotoKeepTheirDestinationAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            listOf(TokensBoard, TokensSettings, PhotoDestination).forEach { screen ->
                scenario.onActivity { activity ->
                    when (screen) {
                        TokensSettings -> koin.get<TokensMediator>().open(activity.navController, TokensEntry.SETTINGS)
                        PhotoDestination -> koin.get<ReinforcementPhotoMediator>().open(activity.navController)
                    }
                }
                compose.waitForIdle()
                scenario.recreate()
                compose.waitForIdle()
                scenario.onActivity {
                    assertTrue(it.navController.currentDestination!!.hasRoute(screen::class))
                }
            }
        }
    }

    @Test fun settingsEntryRestoresAndReturnsToBoard() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            scenario.onActivity { koin.get<TokensMediator>().open(it.navController, TokensEntry.SETTINGS) }
            compose.waitForIdle()
            scenario.recreate()
            compose.waitForIdle()
            scenario.onActivity {
                assertTrue(it.navController.currentDestination!!.hasRoute<TokensSettings>())
                assertTrue(it.navController.popBackStack())
                assertTrue(it.navController.currentDestination!!.hasRoute<TokensBoard>())
            }
        }
    }

    @Test fun tokensEntriesCanBeOpenedFromPhotoDestination() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            scenario.onActivity {
                koin.get<ReinforcementPhotoMediator>().open(it.navController)
                koin.get<TokensMediator>().open(it.navController, TokensEntry.SETTINGS)
                assertTrue(it.navController.currentDestination!!.hasRoute<TokensSettings>())
                koin.get<TokensMediator>().open(it.navController, TokensEntry.BOARD)
                assertTrue(it.navController.currentDestination!!.hasRoute<TokensBoard>())
            }
        }
    }

    @Test fun photoMediatorRestoresBackStackAndDoesNotDuplicateItsEntry() {
        val mediator = koin.get<ReinforcementPhotoMediator>()
        assertSame(mediator, koin.get<ReinforcementPhotoMediator>())
        assertSame(koin.get<ReinforcementRepository>(), koin.get<ReinforcementRepository>())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            scenario.onActivity {
                mediator.open(it.navController)
                val entry = it.navController.currentBackStackEntry
                mediator.open(it.navController)
                assertEquals(entry?.id, it.navController.currentBackStackEntry?.id)
            }
            compose.waitForIdle()
            scenario.recreate()
            compose.waitForIdle()
            scenario.onActivity {
                assertTrue(it.navController.currentDestination!!.hasRoute<PhotoDestination>())
                assertTrue(it.navController.popBackStack())
                assertTrue(it.navController.currentDestination!!.hasRoute<TokensBoard>())
            }
        }
    }

    @Test fun repeatedCompletionCannotPopAnotherDestination() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            scenario.onActivity {
                val controller = it.navController
                koin.get<ReinforcementPhotoMediator>().open(controller)
                val photoEntry = controller.currentBackStackEntry!!
                assertTrue(controller.popEntryIfCurrent(photoEntry))
                koin.get<TokensMediator>().open(controller, TokensEntry.SETTINGS)
                assertFalse(controller.popEntryIfCurrent(photoEntry))
                assertTrue(controller.currentDestination!!.hasRoute<TokensSettings>())
            }
        }
    }
}
