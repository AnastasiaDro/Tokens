package presentation.tokens_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.cerebus.tokens.core.ui.setTokensContent
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.SelectTokensNumberAlertData

class TokensFragment : Fragment() {
    private val viewModel: TokensViewModel by viewModel()
    private val photoMediator: ReinforcementPhotoMediator by inject()
    private val soundPlayer: WinSoundOutput by inject()
    private var lastCelebrationId = WinEffectsState.NO_CELEBRATION_ID

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.tokens_compose_view
            setTokensContent {
                TokensRoute(
                    viewModel = viewModel,
                    onSelectCount = { count ->
                        findNavController().navigate(TokensFragmentDirections.actionTokensFragmentToSelectTokenNumberAlert(
                            SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, count)))
                    },
                    onSettings = { findNavController().navigate(R.id.action_tokensFragment_to_settingsFragment) },
                    onPhoto = { photoMediator.open(findNavController()) },
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state) { state ->
            val effects = state.effects
            if (effects.celebrationId != lastCelebrationId) {
                lastCelebrationId = effects.celebrationId
                if (effects.isSoundPlaying) soundPlayer.play()
            }
            if (!effects.isSoundPlaying) soundPlayer.stop()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        soundPlayer.stop()
        super.onStop()
    }

    override fun onDestroyView() {
        soundPlayer.stop()
        super.onDestroyView()
    }
}
