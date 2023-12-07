package tokens_screen

import androidx.navigation.NavDirections
import settings_screen.SettingsFragment

/**
 * [TokensNumberListener] a listener for changing number of tokens
 *
 * @see TokensFragment
 * @see SettingsFragment
 *
 * @author Anastasia Drogunova
 * @since 25.05.2023
 */
interface TokensNumberListener {

    fun subscribeToNavigationResultLiveData()

    fun getTokensNumberAlertNavAction(
        minTokensNum: Int,
        maxTokensNum: Int,
        currentTokensNum: Int
    ): NavDirections
}