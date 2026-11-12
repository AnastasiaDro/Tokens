package presentation.tokens_screen

import org.junit.Assert.*
import org.junit.Test

class TokensNavigatorTest {
    @Test fun navigationIsSynchronousAndNotReplayedAfterBindingAgain() {
        val navigator = TokensNavigator()
        val destinations = mutableListOf<TokensNavigator.Destination>()
        val binding = navigator.bind { destinations += it }
        navigator.selectCount(TOKEN_COUNT)
        navigator.settings()
        navigator.photo()
        assertEquals(listOf(TokensNavigator.Destination.SelectCount(TOKEN_COUNT),
            TokensNavigator.Destination.Settings, TokensNavigator.Destination.Photo), destinations)
        binding.close()
        navigator.photo()
        val next = mutableListOf<TokensNavigator.Destination>()
        navigator.bind { next += it }.use {
            assertTrue(next.isEmpty())
            navigator.settings()
            assertEquals(listOf(TokensNavigator.Destination.Settings), next)
        }
    }

    @Test fun disposingOldBindingDoesNotClearNewNavigationEnvironment() {
        val navigator = TokensNavigator()
        val oldDestinations = mutableListOf<TokensNavigator.Destination>()
        val destinations = mutableListOf<TokensNavigator.Destination>()
        val old = navigator.bind { oldDestinations += it }
        val current = navigator.bind { destinations += it }
        old.close()
        navigator.photo()
        assertTrue(oldDestinations.isEmpty())
        assertEquals(listOf(TokensNavigator.Destination.Photo), destinations)
        current.close()
        navigator.settings()
        assertEquals(listOf(TokensNavigator.Destination.Photo), destinations)
    }

    @Test fun differentNavigatorInstancesDoNotShareBindings() {
        val first = TokensNavigator()
        val second = TokensNavigator()
        val destinations = mutableListOf<TokensNavigator.Destination>()
        first.bind { destinations += it }.use {
            second.settings()
            assertTrue(destinations.isEmpty())
            first.settings()
            assertEquals(listOf(TokensNavigator.Destination.Settings), destinations)
        }
    }

    private companion object { const val TOKEN_COUNT = 5 }
}
