package presentation.tokens_screen

import data.tokens.FakeTokensStorage
import data.tokens.TokensRepositoryImpl
import domain.models.WinEffects
import domain.repository.EffectsRepository
import domain.repository.ReinforcementSettingsRepository
import domain.usecases.effects.*
import domain.usecases.reinforcement.*
import domain.usecases.tokens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import presentation.tokens_screen.mvi_contracts.InitEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.CheckTokenEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.UncheckTokenEvent

@OptIn(ExperimentalCoroutinesApi::class)
class TokensViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val storage = FakeTokensStorage(2)
    private val repository = TokensRepositoryImpl(storage)
    private val effects = FakeEffectsRepository()
    private lateinit var viewModel: TokensViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        val reinforcement = object : ReinforcementSettingsRepository {
            override fun getIsReinforcementShown() = false
            override fun setIsReinforcementShown(isShow: Boolean) = Unit
            override fun getReinforcementPhotoPathString(): String? = null
        }
        viewModel = TokensViewModel(
            ClearAllTokensUseCase(repository), CheckTokenUseCase(repository), UncheckTokenUseCase(repository),
            GetTokensNumberUseCase(repository), CheckTokensAreGrappedUseCase(repository), GetAllTokensUseCase(repository),
            GetMinTokensNumberUseCase(repository), GetMaxTokensNumberUseCase(repository),
            IsWinAnimationOnUseCase(effects), IsWinSoundOnUseCase(effects), GetEffectsDurationUseCase(effects),
            GetIsReinforcementShowUseCase(reinforcement), GetReinforcementUriStringUseCase(reinforcement),
        )
    }

    @After fun tearDown() {
        viewModel.stopWinEffects()
        Dispatchers.resetMain()
    }

    private fun win() {
        viewModel.sendEvent(CheckTokenEvent(0))
        viewModel.sendEvent(CheckTokenEvent(1))
    }

    @Test fun `first victory is available even before UI subscribes`() = runTest(dispatcher) {
        win()
        assertTrue(viewModel.winEffectsFlow.value.isSoundPlaying)
        assertTrue(viewModel.winEffectsFlow.value.isAnimationRunning)
        assertTrue(viewModel.tokensStateFlow.value.tokens.all { it.isChecked })
        runCurrent()
        advanceTimeBy(5000)
        runCurrent()
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        assertFalse(viewModel.winEffectsFlow.value.isAnimationRunning)
    }

    @Test fun `duplicate check never restarts a victory`() = runTest(dispatcher) {
        win()
        val first = viewModel.winEffectsFlow.value
        viewModel.sendEvent(CheckTokenEvent(1))
        assertEquals(first, viewModel.winEffectsFlow.value)
        assertEquals(2, repository.getCheckedTokensNumber())
    }

    @Test fun `next victory has its own full duration and cancels previous timer`() = runTest(dispatcher) {
        win()
        runCurrent()
        val firstId = viewModel.winEffectsFlow.value.celebrationId
        advanceTimeBy(1000)
        viewModel.sendEvent(UncheckTokenEvent(1))
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        viewModel.sendEvent(CheckTokenEvent(1))
        runCurrent()
        assertTrue(viewModel.winEffectsFlow.value.celebrationId > firstId)
        advanceTimeBy(4000)
        runCurrent()
        assertTrue(viewModel.winEffectsFlow.value.isSoundPlaying)
        advanceTimeBy(1000)
        runCurrent()
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
    }

    @Test fun `animation and sound respect independent settings`() = runTest(dispatcher) {
        for (animation in listOf(false, true)) for (sound in listOf(false, true)) {
            viewModel.clearTokens()
            effects.animation = animation
            effects.sound = sound
            win()
            assertEquals(animation, viewModel.winEffectsFlow.value.isAnimationRunning)
            assertEquals(sound, viewModel.winEffectsFlow.value.isSoundPlaying)
        }
    }

    @Test fun `leaving screen stops effects and init does not replay victory`() = runTest(dispatcher) {
        win()
        viewModel.stopWinEffects()
        viewModel.sendEvent(InitEvent())
        advanceUntilIdle()
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        assertFalse(viewModel.winEffectsFlow.value.isAnimationRunning)
        assertTrue(viewModel.tokensStateFlow.value.tokens.all { it.isChecked })
    }

    @Test fun `clear and resize stop effects without triggering a new victory`() = runTest(dispatcher) {
        win()
        repository.resizeTokens(1)
        viewModel.updateTokensNum()
        assertEquals(1, viewModel.tokensStateFlow.value.tokens.size)
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        viewModel.clearTokens()
        assertFalse(viewModel.tokensStateFlow.value.tokens.single().isChecked)
        advanceUntilIdle()
        assertFalse(viewModel.winEffectsFlow.value.isAnimationRunning)
    }

    @Test fun `rapid clicks use current model rather than stale rendered state`() = runTest(dispatcher) {
        viewModel.onTokenClicked(0)
        viewModel.onTokenClicked(0)
        viewModel.onTokenClicked(99)
        assertEquals(0, repository.getCheckedTokensNumber())
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
    }

    private class FakeEffectsRepository : EffectsRepository {
        var animation = true
        var sound = true
        override fun plugWinAnimationOn() { animation = true }
        override fun plugWinAnimationOff() { animation = false }
        override fun plugWinSoundOn() { sound = true }
        override fun plugWinSoundOff() { sound = false }
        override fun getWinEffects() = WinEffects(animation, sound, 5000L, 1)
    }
}
