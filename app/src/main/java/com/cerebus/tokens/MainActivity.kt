package com.cerebus.tokens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.feature.tokens_feature.api.TokensGraph
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val tokensMediator: TokensMediator by inject()
    private val photoMediator: ReinforcementPhotoMediator by inject()
    internal lateinit var navController: NavHostController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TokensTheme {
                val controller = rememberNavController()
                navController = controller
                NavHost(controller, startDestination = TokensGraph,
                    enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    tokensMediator.registerGraph(this, controller)
                    photoMediator.registerGraph(this, controller)
                }
            }
        }
    }
}
