package presentation.settings_screen

import org.junit.Assert.*
import org.junit.Test

class SettingsNavigatorTest {
    @Test fun navigationIsSynchronousAndNotReplayedAfterBindingAgain() {
        val navigator = SettingsNavigator()
        val destinations = mutableListOf<SettingsNavigator.Destination>()
        val binding = navigator.bind { destinations += it }
        navigator.selectCount(TOKEN_COUNT)
        navigator.selectColor()
        navigator.youtube()
        navigator.otherApps()
        assertEquals(listOf(SettingsNavigator.Destination.SelectCount(TOKEN_COUNT),
            SettingsNavigator.Destination.SelectColor, SettingsNavigator.Destination.Youtube,
            SettingsNavigator.Destination.OtherApps), destinations)
        binding.close()
        navigator.youtube()
        val next = mutableListOf<SettingsNavigator.Destination>()
        navigator.bind { next += it }.use {
            assertTrue(next.isEmpty())
            navigator.selectColor()
            assertEquals(listOf(SettingsNavigator.Destination.SelectColor), next)
        }
    }

    @Test fun disposingOldBindingDoesNotClearNewNavigationEnvironment() {
        val navigator = SettingsNavigator()
        val oldDestinations = mutableListOf<SettingsNavigator.Destination>()
        val destinations = mutableListOf<SettingsNavigator.Destination>()
        val old = navigator.bind { oldDestinations += it }
        val current = navigator.bind { destinations += it }
        old.close()
        navigator.youtube()
        assertTrue(oldDestinations.isEmpty())
        assertEquals(listOf(SettingsNavigator.Destination.Youtube), destinations)
        current.close()
        navigator.selectColor()
        assertEquals(listOf(SettingsNavigator.Destination.Youtube), destinations)
    }

    @Test fun differentNavigatorInstancesDoNotShareBindings() {
        val first = SettingsNavigator()
        val second = SettingsNavigator()
        val destinations = mutableListOf<SettingsNavigator.Destination>()
        first.bind { destinations += it }.use {
            second.selectColor()
            assertTrue(destinations.isEmpty())
            first.selectColor()
            assertEquals(listOf(SettingsNavigator.Destination.SelectColor), destinations)
        }
    }

    private companion object { const val TOKEN_COUNT = 5 }
}
