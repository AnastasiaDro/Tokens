package com.cerebus.tokens

import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cerebus.tokens.data.reinforcement.ReinforcementRepository
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class FeatureNavigationTest {
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
    }
}
