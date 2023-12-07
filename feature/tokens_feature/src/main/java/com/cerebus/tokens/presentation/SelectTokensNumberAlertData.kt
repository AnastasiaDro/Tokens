package com.cerebus.tokens.presentation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.cerebus.tokens.presentation.tokens_screen.SelectTokenNumberAlert

/**
 * [SelectTokensNumberAlertData] - a class with data for
 * setting initial values for [SelectTokenNumberAlert] by
 * navComponent action args
 *
 * @see SelectTokenNumberAlert
 *
 * @author Anastasia Drogunova
 * @since 29.11.2023
 */
@Parcelize
data class SelectTokensNumberAlertData(
    val minTokensNum: Int,
    val maxTokensNum: Int,
    val currentTokensNum: Int
): Parcelable {
    companion object {
        const val CURRENT_TOKENS_NUMBER_RESULT_KEY = "CurrentTokensNumber"
    }
}

