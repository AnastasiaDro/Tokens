package presentation.tokens_screen

import SelectTokensNumberAlertData
import SelectTokensNumberAlertData.Companion.CURRENT_TOKENS_NUMBER_RESULT_KEY
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.core.ui.SwipeParser
import com.cerebus.tokens.core.ui.SwipeParserImpl
import com.cerebus.tokens.core.ui.getNavigationResultLiveData
import com.cerebus.tokens.core.ui.setPhotoImage
import com.cerebus.tokens.core.ui.subscribeToHotFlow
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.databinding.FragmentTokensBinding
import com.cerebus.tokens.logger.api.LoggerFactory
import domain.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.tokens_screen.mvi_contracts.InitEvent
import presentation.tokens_screen.mvi_contracts.reinforcement_image_mvi_contract.GetReinforcementStateEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.CheckTokenEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.ClearTokensEvent
import presentation.tokens_screen.mvi_contracts.tokens_mvi_contract.UncheckTokenEvent

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
class TokensFragment : Fragment(R.layout.fragment_tokens), TokensNumberListener {

    private val viewModel: TokensViewModel by viewModel<TokensViewModel>()
    private val viewBinding: FragmentTokensBinding by viewBinding()
    private var viewArray: List<TokenView> = listOf()

    private var soundPlayer: MediaPlayer? = null
    private val swipeParser: SwipeParser = SwipeParserImpl(this::class.java.simpleName)
    private val loggerFactory: LoggerFactory by inject()
    private val logger = loggerFactory.createLogger(this::class.java.simpleName)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        soundPlayer = MediaPlayer.create(requireActivity(), R.raw.fanfare)
        initOptionsMenu()

        subscribeToNavigationResultLiveData()

        viewBinding.reinforcementImageCardView.setOnClickListener {
            goToImageSelecting()
        }
        viewArray = getTokenViewsList()
        subscribeToViewModel(viewArray)
        viewModel.sendEvent(InitEvent())

        view.setOnTouchListener { v, event ->
            if (swipeParser.onSwipeHorizontal(v, event)) viewModel.sendEvent(ClearTokensEvent())
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
        val request = NavDeepLinkRequest.Builder
            .fromUri("android-app://com.cerebus.tokens.reinforcement_photo.presentation.dialog".toUri())
            .build()
        findNavController().navigate(request)
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

    private fun showTokens(viewList: List<TokenView>, tokensList: List<Token>) {
        var i = 0
        for (index in tokensList.indices) {
            viewList[index].visibility = View.VISIBLE
            if (viewList[index].getCheckedColor() != tokensList[index].checkedColor)
                viewList[index].setCheckedColor(tokensList[index].checkedColor)
            if (tokensList[index].isChecked)
                viewList[index].setChecked()
            else
                viewList[index].setUnchecked()
            viewList[index].setOnClickListener { onTokenClick(index) }
            i = index + 1
        }
        for (t in i until viewList.size)
            viewList[t].isVisible = false
        logger.d("All tokens were initialized")
    }

    override fun subscribeToNavigationResultLiveData() {
        val result = getNavigationResultLiveData<Int>(CURRENT_TOKENS_NUMBER_RESULT_KEY)
        result?.observe(viewLifecycleOwner) { newNum ->
            viewModel.updateTokensNum()
            logger.d("$CURRENT_TOKENS_NUMBER_RESULT_KEY navigation result is taken NEW NUMBER = $newNum")
        }
        val imageResult = getNavigationResultLiveData<Boolean>(IS_IMAGE_SET_RESULT)
        imageResult?.observe(viewLifecycleOwner) { res ->
            if (res) viewModel.sendEvent(GetReinforcementStateEvent())
        }
    }

    private fun subscribeToViewModel(viewList: List<TokenView>) {
        with(viewModel) {
           subscribeToHotFlow(Lifecycle.State.CREATED, tokensStateFlow) { tokensState ->
               showTokens(viewList, tokensState.tokens)
           }

            subscribeToHotFlow(Lifecycle.State.STARTED, winEffectsFlow) { effectsState ->
                if (effectsState.isSoundPlaying) soundPlayer?.start()
                if (effectsState.isAnimationRunning) playAnimation() else pauseAnimation()
            }

            subscribeToHotFlow(Lifecycle.State.STARTED, navigateToSettingsFlow) {
                findNavController().navigate(R.id.action_tokensFragment_to_settingsFragment)
            }

            subscribeToHotFlow(Lifecycle.State.STARTED, reinforcementStateFlow) { state ->
                viewBinding.reinforcementImageCardView.isVisible = state.isReinforcementShow
                viewBinding.reinforcementImage.setPhotoImage(state.reinforcementImageUri, com.cerebus.tokens.core.ui.R.drawable.baseline_add_a_photo_24)
            }
        }
        logger.d("subscribed to viewModel")
    }

    private fun playAnimation() = with(viewBinding) {
        logger.d("animation started")
        lifecycleScope.launch {
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
        logger.d("animation paused")
        lifecycleScope.launch {
            animationViewLeft.pauseAnimation()
            animationViewLeft.isVisible = false
            delay(ANIMATION_FIRST_DELAY)
            animationViewRight.pauseAnimation()
            animationViewRight.isVisible = false
            delay(ANIMATION_SECOND_DELAY)
            animationViewCenter.pauseAnimation()
            animationViewCenter.isVisible = false
        }
    }

    private fun onMenuItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.changeChipsNum -> {
                findNavController().navigate(
                    getTokensNumberAlertNavAction(
                        minTokensNum = viewModel.getMinTokensNum(),
                        maxTokensNum = viewModel.getMaxTokensNum(),
                        currentTokensNum = viewModel.getTokensNum()
                    )
                )
            }
            R.id.clearTokens -> viewModel.clearTokens()
            R.id.appSettings -> viewModel.onSettingsPressed()
        }
        logger.d("${item.title} onMenuItem was clicked")
        return true
    }

    private fun onTokenClick(index: Int) {
        logger.d("token $index was clicked")
        if (viewArray[index].getIsChecked())
            viewModel.sendEvent(UncheckTokenEvent(index))
        else
            viewModel.sendEvent(CheckTokenEvent(index))
    }

    override fun getTokensNumberAlertNavAction(
        minTokensNum: Int,
        maxTokensNum: Int,
        currentTokensNum: Int
    ): NavDirections {
        return TokensFragmentDirections.actionTokensFragmentToSelectTokenNumberAlert(
            SelectTokensNumberAlertData(minTokensNum, maxTokensNum, currentTokensNum)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundPlayer?.release()
    }

    companion object {
        const val ANIMATION_FIRST_DELAY = 500L
        const val ANIMATION_SECOND_DELAY = 300L

        const val IS_IMAGE_SET_RESULT = "ImageUri"
    }
}