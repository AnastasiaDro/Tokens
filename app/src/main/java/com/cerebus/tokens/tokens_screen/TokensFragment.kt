package com.cerebus.tokens.tokens_screen

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.cerebus.tokens.databinding.FragmentTokensBinding
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.MainActivity
import com.cerebus.tokens.utils.PrefsConstants.TOKENS_PREFERENCES
import com.cerebus.tokens.R
import com.cerebus.tokens.navigator.Navigator
import kotlinx.coroutines.delay
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

    private val viewModel: TokensViewModel by viewModels()
    private val viewBinding: FragmentTokensBinding by viewBinding()
    private var viewArray: List<TokenView> = listOf()

    private var navigator: Navigator? = null
    private var soundPlayer: MediaPlayer? = null
    private val swipeParser: SwipeParser = SwipeParserImpl(TAG)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator = (requireActivity() as? MainActivity)?.getNavigator()
        soundPlayer = MediaPlayer.create(requireActivity(), R.raw.fanfare)
        initOptionsMenu()
        viewArray = getTokensList()
        viewModel.initData(requireActivity().getSharedPreferences(TOKENS_PREFERENCES, Context.MODE_PRIVATE))
        subscribeToViewModel(viewArray)

        view.setOnTouchListener { v, event ->
            if (swipeParser.onSwipeHorizontal(v, event)) viewModel.clearTokens()
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            return@setOnTouchListener true
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.getCheckedColor() != viewArray[0].getCheckedColor()) {
            for (token in viewArray) token.setCheckedColor(viewModel.getCheckedColor())
            refreshTokens(viewArray)
        }
    }

    override fun changeTokensNumber(newNumber: Int) {
        viewModel.changeTokensNum(newNumber)
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
        for (i in 0 until viewModel.getTokensNum()) {
            if (i < viewModel.getCheckedTokensNum())
                viewList[i].setChecked()
            else
                viewList[i].setUnchecked()
            viewList[i].isVisible = true
            viewList[i].setOnClickListener { onTokenClick(i) }
        }
        for (i in viewModel.getTokensNum() until viewModel.getMaxTokensNumber())
            viewList[i].isVisible = false
        Log.d(TAG, "Tokens were initialized")
    }

    private fun refreshTokens(viewList: List<TokenView>) {
        when(viewModel.getCheckedTokensNum()) {
            NO_CHECKED_TOKENS ->  viewList.map { it.setUnchecked() }
            viewModel.getTokensNum() -> viewList.map { it.setChecked() }
            else -> {
                for (i in 0 until viewModel.getTokensNum()) {
                    if (i < viewModel.getCheckedTokensNum())
                        viewList[i].setChecked()
                    else
                        viewList[i].setUnchecked()
                }
            }
        }
        Log.d(TAG, "refreshed checking")
    }

    private fun subscribeToViewModel(viewList: List<TokenView>) {
        with(viewModel) {
            prefsLoadedLiveData.observe(viewLifecycleOwner) { if (it == true) showTokens(viewList) }
            selectedLiveData.observe(viewLifecycleOwner) { index -> viewList[index].setChecked() }
            unselectedLiveData.observe(viewLifecycleOwner) { index -> viewList[index].setUnchecked() }
            changedTokensNumLiveData.observe(viewLifecycleOwner) { showTokens(viewList) }
            changeCheckedTokensNumLiveData.observe(viewLifecycleOwner) { refreshTokens(viewList) }
            navigateLiveData.observe(viewLifecycleOwner) { it?.let { navigator?.navigate(it, requireActivity() as? AppCompatActivity)}}
            animationLiveData.observe(viewLifecycleOwner) { animate ->
                if (animate) playAnimation() else pauseAnimation()
            }
            soundLiveData.observe(viewLifecycleOwner) { sound ->
                if (sound) soundPlayer?.start()
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
            R.id.changeChipsNum -> SelectTokenNumberAlert(viewModel.getTokensNum(), viewModel.getMinTokensNumber(), viewModel.getMaxTokensNumber(), this@TokensFragment).show(requireActivity().supportFragmentManager,
                SelectTokenNumberAlert.TAG
            )
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

    companion object {
        const val TAG = "TOKENS_FRAGMENT"
        private const val NO_CHECKED_TOKENS = 0
        const val ANIMATION_FIRST_DELAY = 500L
        const val ANIMATION_SECOND_DELAY = 300L

        fun newInstance() = TokensFragment()
    }
}