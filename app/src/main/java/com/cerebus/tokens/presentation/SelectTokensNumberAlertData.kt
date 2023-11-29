package com.cerebus.tokens.presentation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

