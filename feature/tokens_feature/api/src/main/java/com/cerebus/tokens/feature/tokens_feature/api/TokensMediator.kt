package com.cerebus.tokens.feature.tokens_feature.api

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable

enum class TokensEntry { BOARD, SETTINGS }

@Serializable data object TokensGraph
@Serializable data object TokensBoard
@Serializable data object TokensSettings

/** Entry points owned by the tokens feature; graph registration belongs to app composition. */
interface TokensMediator {
    fun registerGraph(builder: NavGraphBuilder, navController: NavHostController, startEntry: TokensEntry = TokensEntry.BOARD)
    fun open(navController: NavController, entry: TokensEntry = TokensEntry.BOARD)
}
