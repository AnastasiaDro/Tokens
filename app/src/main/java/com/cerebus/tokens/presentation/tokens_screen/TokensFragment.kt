package com.cerebus.tokens.presentation.tokens_screen

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.R
import com.cerebus.tokens.databinding.FragmentTokensBinding
import com.cerebus.tokens.presentation.SelectTokensNumberAlertData
import com.cerebus.tokens.presentation.SelectTokensNumberAlertData.Companion.CURRENT_TOKENS_NUMBER_RESULT_KEY
import com.cerebus.tokens.presentation.getNavigationResultLiveData
import com.cerebus.tokens.presentation.settings_screen.SettingsFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch

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
class TokensFragment: Fragment(R.layout.fragment_tokens), TokensNumberListener {

    private lateinit var viewModel: TokensViewModel
    private val viewBinding: FragmentTokensBinding by viewBinding()
    private var viewArray: List<TokenView> = listOf()

    private var soundPlayer: MediaPlayer? = null
    private val swipeParser: SwipeParser = SwipeParserImpl(TAG)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, TokensViewModelFactory(requireContext())).get(TokensViewModel::class.java)

        soundPlayer = MediaPlayer.create(requireActivity(), R.raw.fanfare)
        initOptionsMenu()
        viewArray = getTokensList()
        viewModel.initData()
        subscribeToNavigationResultLiveData()
        subscribeToViewModel(viewArray)

        view.setOnTouchListener { v, event ->
            if (swipeParser.onSwipeHorizontal(v, event)) viewModel.clearTokens()
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            return@setOnTouchListener true
        }
        Log.d(SettingsFragment.TAG, "Views were initialized")
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.getCheckedColor() != viewArray[0].getCheckedColor()) {
            for (token in viewArray) token.setCheckedColor(viewModel.getCheckedColor())
            refreshTokens(viewArray)
        }
    }

    private fun initOptionsMenu() {
        viewBinding.tokensToolbar.inflateMenu(R.menu.fragment_tokens_options_menu)
        viewBinding.tokensToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
    }

    private fun getTokensList() = with(viewBinding) {
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

    private fun showTokens(viewList: List<TokenView>) {
        lifecycleScope.launch {
            var i = 0
            viewModel.getTokens().collectIndexed { index, token ->
                viewList[index].visibility = View.VISIBLE
                if (token.isChecked)
                    viewList[index].setChecked()
                else
                    viewList[index].setUnchecked()
                viewList[index].setOnClickListener { onTokenClick(index) }
                Log.d(TAG, "Token [$index] were initialized")
                i = index + 1
            }
            for (t in i until viewList.size)
                viewList[t].isVisible = false
        }
        Log.d(TAG, "All tokens were initialized")
    }

    private fun refreshTokens(viewList: List<TokenView>) {
        lifecycleScope.launch {
            viewModel.getTokens().collectIndexed { index, token ->
                if (token.isChecked)
                    viewList[index].setChecked()
                else
                    viewList[index].setUnchecked()
            }
            Log.d(TAG, "refreshed tokens' check")
        }
    }

    override fun subscribeToNavigationResultLiveData() {
        val result = getNavigationResultLiveData<Int>(CURRENT_TOKENS_NUMBER_RESULT_KEY)
        result?.observe(viewLifecycleOwner) {
                newNum -> viewModel.changeTokensNum(newNum)
        }
    }

    private fun subscribeToViewModel(viewList: List<TokenView>) {
        with(viewModel) {
            prefsLoadedLiveData.observe(viewLifecycleOwner) { if (it == true) showTokens(viewList) }
            selectedLiveData.observe(viewLifecycleOwner) { index -> viewList[index].setChecked() }
            unselectedLiveData.observe(viewLifecycleOwner) { index -> viewList[index].setUnchecked() }
            changedTokensNumLiveData.observe(viewLifecycleOwner) { showTokens(viewList) }
            changeCheckedTokensNumLiveData.observe(viewLifecycleOwner) { refreshTokens(viewList) }

            animationLiveData.observe(viewLifecycleOwner) { animate ->
                if (animate) playAnimation() else pauseAnimation()
            }
            soundLiveData.observe(viewLifecycleOwner) { sound ->
                if (sound) soundPlayer?.start()
            }
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.navigateToSettingsFlow.collect {
                        findNavController().navigate(R.id.action_tokensFragment_to_settingsFragment)
                    }
                }
            }
        }
        Log.d(TAG, "subscribed to viewModel")
    }

    private fun playAnimation() = with(viewBinding) {
        Log.d(TAG, "animation started")
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
        Log.d(TAG, "animation paused")
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
        when(item.itemId) {
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
            R.id.appSettings -> viewModel.onAboutAppPressed()
        }
        Log.d(TAG, "onMenuItem was clicked")
        return true
    }

    private fun onTokenClick(index: Int) {
        Log.d(TAG, "token $index was clicked")
        if (viewArray[index].getIsChecked()) viewModel.onTokenUnselected(index) else viewModel.onTokenSelected(index)
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

    companion object {
        const val TAG = "TokensFragment"
        const val ANIMATION_FIRST_DELAY = 500L
        const val ANIMATION_SECOND_DELAY = 300L

        fun newInstance() = TokensFragment()
    }
}