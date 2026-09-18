package presentation.tokens_screen

import presentation.SelectTokensNumberAlertData
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.SwipeParser
import com.cerebus.tokens.core.ui.SwipeParserImpl
import com.cerebus.tokens.core.ui.setPhotoImage
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.databinding.FragmentTokensBinding
import com.cerebus.tokens.logger.api.LoggerFactory
import presentation.state.TokenState
import presentation.state.StorageFailure
import domain.repository.MIN_TOKEN_COUNT
import domain.repository.MAX_TOKEN_COUNT
import android.content.pm.PackageManager
import com.cerebus.tokens.data.reinforcement.ReinforcementSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * [TokensFragment] - a fragment for tokens displaying
 * It draws as two rows
 * Max tokens number is ten
 *
 * @see TokenView
 *
 * @author Anastasia Drogunova
 * @since 23.05.2023
 */
class TokensFragment : Fragment(R.layout.fragment_tokens) {

    private val viewModel: TokensViewModel by viewModel<TokensViewModel>()
    private val photoMediator: ReinforcementPhotoMediator by inject()
    private val viewBinding: FragmentTokensBinding by viewBinding()
    private var viewArray: List<TokenView> = listOf()

    private var soundPlayer: WinSoundPlayer? = null
    private var animationJob: Job? = null
    private var lastCelebrationId = WinEffectsState.NO_CELEBRATION_ID
    private var renderedReinforcement: ReinforcementSettings? = null
    private val swipeParser: SwipeParser = SwipeParserImpl(this::class.java.simpleName)
    private val loggerFactory: LoggerFactory by inject()
    private val logger = loggerFactory.createLogger(this::class.java.simpleName)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        soundPlayer = WinSoundPlayer(requireContext(), logger)
        initOptionsMenu()


        viewBinding.reinforcementImageCardView.setOnClickListener {
            goToImageSelecting()
        }
        viewArray = getTokenViewsList()
        subscribeToViewModel(viewArray)

        view.setOnTouchListener { v, event ->
            if (swipeParser.onSwipeHorizontal(v, event)) viewModel.clearTokens()
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            return@setOnTouchListener true
        }
        logger.d("Views were initialized")
    }

    private fun initOptionsMenu() {
        viewBinding.tokensToolbar.inflateMenu(R.menu.fragment_tokens_options_menu)
        viewBinding.tokensToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        logger.d("Menu is initialized")
    }

    private fun goToImageSelecting() {
        photoMediator.open(findNavController())
    }

    private fun getTokenViewsList(): List<TokenView> = with(viewBinding) {
        listOf(
            firstRow.tokenButton1,
            firstRow.tokenButton2,
            firstRow.tokenButton3,
            firstRow.tokenButton4,
            firstRow.tokenButton5,

            secondRow.tokenButton1,
            secondRow.tokenButton2,
            secondRow.tokenButton3,
            secondRow.tokenButton4,
            secondRow.tokenButton5
        )
    }

    private fun showTokens(viewList: List<TokenView>, tokensList: List<TokenState>) {
        var i = FIRST_TOKEN_INDEX
        for (index in tokensList.indices) {
            viewList[index].visibility = View.VISIBLE
            if (viewList[index].getCheckedColor() != tokensList[index].color)
                viewList[index].setCheckedColor(tokensList[index].color)
            if (tokensList[index].checked)
                viewList[index].setChecked()
            else
                viewList[index].setUnchecked()
            viewList[index].setOnClickListener { viewModel.onTokenClicked(tokensList[index].id) }
            i = index + NEXT_TOKEN_OFFSET
        }
        for (t in i until viewList.size)
            viewList[t].isVisible = false
        logger.d("All tokens were initialized")
    }

    private fun subscribeToViewModel(viewList: List<TokenView>) {
        viewBinding.storageStatus.setOnClickListener { viewModel.retry() }
        subscribeToHotFlow(Lifecycle.State.STARTED, viewModel.state) { state ->
            showTokens(viewList, state.board?.tokens.orEmpty())
            val ready = !state.loading && state.board != null && state.error != StorageFailure.READ
            viewList.forEach { it.isEnabled = ready }
            viewBinding.tokensToolbar.menu.findItem(R.id.changeChipsNum).isEnabled = ready
            viewBinding.tokensToolbar.menu.findItem(R.id.clearTokens).isEnabled = ready
            val effectsState = state.effects
            if (effectsState.celebrationId != lastCelebrationId) {
                lastCelebrationId = effectsState.celebrationId
                if (effectsState.isSoundPlaying) soundPlayer?.play()
                if (effectsState.isAnimationRunning) playAnimation()
            }
            if (!effectsState.isSoundPlaying) soundPlayer?.stop()
            if (!effectsState.isAnimationRunning) pauseAnimation()
            viewBinding.reinforcementImageCardView.isVisible = state.reinforcement?.enabled == true &&
                requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            if (renderedReinforcement != state.reinforcement) {
                renderedReinforcement = state.reinforcement
                viewBinding.reinforcementImage.setPhotoImage(state.reinforcement?.photoUri?.toUri(),
                    com.cerebus.tokens.core.ui.R.drawable.baseline_add_a_photo_24)
            }
            with(viewBinding.storageStatus) {
                isVisible = state.loading || state.error != null
                isEnabled = state.error != null
                setText(when (state.error) {
                    StorageFailure.READ -> com.cerebus.tokens.core.ui.R.string.storage_read_error
                    StorageFailure.WRITE -> com.cerebus.tokens.core.ui.R.string.storage_write_error
                    null -> com.cerebus.tokens.core.ui.R.string.storage_loading
                })
            }
        }
    }

    private fun playAnimation() = with(viewBinding) {
        pauseAnimation()
        logger.d("animation started")
        animationJob = viewLifecycleOwner.lifecycleScope.launch {
            animationViewLeft.isVisible = true
            animationViewLeft.playAnimation()
            delay(ANIMATION_FIRST_DELAY)
            animationViewRight.isVisible = true
            animationViewRight.playAnimation()
            delay(ANIMATION_SECOND_DELAY)
            animationViewCenter.isVisible = true
            animationViewCenter.playAnimation()
        }
    }

    private fun pauseAnimation() = with(viewBinding) {
        animationJob?.cancel()
        animationJob = null
        listOf(animationViewLeft, animationViewRight, animationViewCenter).forEach {
            it.cancelAnimation()
            it.progress = INITIAL_ANIMATION_PROGRESS
            it.isVisible = false
        }
    }

    private fun onMenuItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.changeChipsNum -> {
                val count = viewModel.state.value.board?.count ?: return true
                findNavController().navigate(TokensFragmentDirections.actionTokensFragmentToSelectTokenNumberAlert(
                    SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, count)))
            }
            R.id.clearTokens -> viewModel.clearTokens()
            R.id.appSettings -> findNavController().navigate(R.id.action_tokensFragment_to_settingsFragment)
        }
        logger.d("${item.title} onMenuItem was clicked")
        return true
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        soundPlayer?.stop()
        pauseAnimation()
        super.onStop()
    }

    override fun onDestroyView() {
        animationJob?.cancel()
        animationJob = null
        soundPlayer?.stop()
        soundPlayer = null
        viewArray = emptyList()
        renderedReinforcement = null
        super.onDestroyView()
    }

    companion object {
        private const val FIRST_TOKEN_INDEX = 0
        private const val NEXT_TOKEN_OFFSET = 1
        private const val INITIAL_ANIMATION_PROGRESS = 0f
        const val ANIMATION_FIRST_DELAY = 500L
        const val ANIMATION_SECOND_DELAY = 300L

    }
}
