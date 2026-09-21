package com.cerebus.tokens

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import androidx.navigation.FloatingWindow
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cerebus.tokens.feature.tokens_feature.api.TokensBoard
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.feature.tokens_feature.api.TokensGraph
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator
import com.cerebus.tokens.feature.tokens_feature.api.TokensSettings
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val tokensMediator: TokensMediator by inject()
    private val photoMediator: ReinforcementPhotoMediator by inject()
    internal lateinit var navController: NavHostController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TokensTheme stays light even when the device uses a dark theme.
        val systemBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = systemBarStyle, navigationBarStyle = systemBarStyle)
        setContent {
            TokensTheme {
                val controller = rememberNavController()
                navController = controller
                NavHost(controller, startDestination = TokensGraph,
                    enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    tokensMediator.registerGraph(this, controller)
                    photoMediator.registerGraph(this, controller)
                }
                TokensOrientationEffect(controller)
            }
        }
    }

    @Composable
    private fun TokensOrientationEffect(controller: NavHostController) {
        val configuration = LocalConfiguration.current
        val visibleEntries by controller.visibleEntries.collectAsState()
        var isInMultiWindowMode by remember(this@MainActivity) {
            mutableStateOf(this@MainActivity.isInMultiWindowMode)
        }

        DisposableEffect(this@MainActivity) {
            val listener = Consumer<MultiWindowModeChangedInfo> { info ->
                isInMultiWindowMode = info.isInMultiWindowMode
            }
            addOnMultiWindowModeChangedListener(listener)
            onDispose { removeOnMultiWindowModeChangedListener(listener) }
        }

        val baseDestination = visibleEntries.asReversed().firstOrNull { entry ->
            entry.destination !is FloatingWindow && entry.destination !is NavGraph
        }?.destination
        if (baseDestination != null) {
            val requestedOrientation = tokensRequestedOrientation(
                isTokenBoard = baseDestination.hasRoute<TokensBoard>(),
                isSettings = baseDestination.hasRoute<TokensSettings>(),
                smallestScreenWidthDp = configuration.smallestScreenWidthDp,
                isInMultiWindowMode = isInMultiWindowMode,
            )
            LaunchedEffect(this@MainActivity, requestedOrientation) {
                if (this@MainActivity.requestedOrientation != requestedOrientation) {
                    this@MainActivity.requestedOrientation = requestedOrientation
                }
            }
        }
    }
}
