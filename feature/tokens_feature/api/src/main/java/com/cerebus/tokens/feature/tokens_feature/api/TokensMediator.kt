package com.cerebus.tokens.feature.tokens_feature.api

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavInflater

enum class TokensEntry { BOARD, SETTINGS }

/** Entry points owned by the tokens feature; graph registration belongs to app composition. */
interface TokensMediator {
    fun createGraph(inflater: NavInflater): NavGraph
    fun open(navController: NavController, entry: TokensEntry = TokensEntry.BOARD)
}
