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
    private val storage = FakeTokensStorage(BOARD_SIZE)
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
        viewModel.sendEvent(CheckTokenEvent(FIRST_TOKEN_INDEX))
        viewModel.sendEvent(CheckTokenEvent(LAST_TOKEN_INDEX))
    }

    @Test fun `first victory is available even before UI subscribes`() = runTest(dispatcher) {
        win()
        assertTrue(viewModel.winEffectsFlow.value.isSoundPlaying)
        assertTrue(viewModel.winEffectsFlow.value.isAnimationRunning)
        assertTrue(viewModel.tokensStateFlow.value.tokens.all { it.isChecked })
        runCurrent()
        advanceTimeBy(EFFECTS_DURATION_MS)
        runCurrent()
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        assertFalse(viewModel.winEffectsFlow.value.isAnimationRunning)
    }

    @Test fun `duplicate check never restarts a victory`() = runTest(dispatcher) {
        win()
        val first = viewModel.winEffectsFlow.value
        viewModel.sendEvent(CheckTokenEvent(LAST_TOKEN_INDEX))
        assertEquals(first, viewModel.winEffectsFlow.value)
        assertEquals(BOARD_SIZE, repository.getCheckedTokensNumber())
    }

    @Test fun `next victory has its own full duration and cancels previous timer`() = runTest(dispatcher) {
        win()
        runCurrent()
        val firstId = viewModel.winEffectsFlow.value.celebrationId
        advanceTimeBy(RESTART_DELAY_MS)
        viewModel.sendEvent(UncheckTokenEvent(LAST_TOKEN_INDEX))
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        viewModel.sendEvent(CheckTokenEvent(LAST_TOKEN_INDEX))
        runCurrent()
        assertTrue(viewModel.winEffectsFlow.value.celebrationId > firstId)
        advanceTimeBy(EFFECTS_DURATION_MS - RESTART_DELAY_MS)
        runCurrent()
        assertTrue(viewModel.winEffectsFlow.value.isSoundPlaying)
        advanceTimeBy(RESTART_DELAY_MS)
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
        repository.resizeTokens(SHRUNK_BOARD_SIZE)
        viewModel.updateTokensNum()
        assertEquals(SHRUNK_BOARD_SIZE, viewModel.tokensStateFlow.value.tokens.size)
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
        viewModel.clearTokens()
        assertFalse(viewModel.tokensStateFlow.value.tokens.single().isChecked)
        advanceUntilIdle()
        assertFalse(viewModel.winEffectsFlow.value.isAnimationRunning)
    }

    @Test fun `rapid clicks use current model rather than stale rendered state`() = runTest(dispatcher) {
        viewModel.onTokenClicked(FIRST_TOKEN_INDEX)
        viewModel.onTokenClicked(FIRST_TOKEN_INDEX)
        viewModel.onTokenClicked(OUT_OF_RANGE_TOKEN_INDEX)
        assertEquals(FakeTokensStorage.NO_CHECKED_TOKENS, repository.getCheckedTokensNumber())
        assertFalse(viewModel.winEffectsFlow.value.isSoundPlaying)
    }

    private class FakeEffectsRepository : EffectsRepository {
        var animation = true
        var sound = true
        override fun plugWinAnimationOn() { animation = true }
        override fun plugWinAnimationOff() { animation = false }
        override fun plugWinSoundOn() { sound = true }
        override fun plugWinSoundOff() { sound = false }
        override fun getWinEffects() = WinEffects(animation, sound, EFFECTS_DURATION_MS, ANIMATION_SPEED)
    }

    private companion object {
        const val BOARD_SIZE = 2
        const val FIRST_TOKEN_INDEX = 0
        const val LAST_TOKEN_INDEX = 1
        const val SHRUNK_BOARD_SIZE = 1
        const val EFFECTS_DURATION_MS = 5000L
        const val RESTART_DELAY_MS = 1000L
        const val ANIMATION_SPEED = 1
        const val OUT_OF_RANGE_TOKEN_INDEX = 99
    }
}
