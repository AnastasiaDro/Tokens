package com.cerebus.tokens.feature.tokens_feature

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavInflater
import androidx.navigation.NavDeepLinkRequest
import androidx.core.net.toUri
import com.cerebus.tokens.feature.tokens_feature.api.TokensEntry
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator

internal class TokensMediatorImpl : TokensMediator {
    override fun createGraph(inflater: NavInflater): NavGraph =
        inflater.inflate(R.navigation.tokens_nav_graph)

    override fun open(navController: NavController, entry: TokensEntry) {
        when (entry) {
            TokensEntry.BOARD -> navController.navigate(R.id.tokens_nav_graph)
            TokensEntry.SETTINGS -> navController.navigate(
                NavDeepLinkRequest.Builder.fromUri(SETTINGS_URI.toUri()).build()
            )
        }
    }

    private companion object {
        const val SETTINGS_URI = "android-app://com.cerebus.tokens.tokens/settings"
    }
}
