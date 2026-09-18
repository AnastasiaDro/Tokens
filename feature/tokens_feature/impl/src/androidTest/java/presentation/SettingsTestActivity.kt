package presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.feature.tokens_feature.api.*
import org.koin.core.context.GlobalContext

/** Real feature graph with isolated test repositories. */
class SettingsTestActivity : ComponentActivity() {
    lateinit var navController: NavHostController
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediator = GlobalContext.get().get<TokensMediator>()
        val start = if (intent.getBooleanExtra(EXTRA_START_BOARD, false)) TokensEntry.BOARD else TokensEntry.SETTINGS
        setContent {
            TokensTheme {
                val controller = rememberNavController()
                navController = controller
                NavHost(controller, startDestination = TokensGraph,
                    enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    mediator.registerGraph(this, controller, start)
                }
            }
        }
    }
    companion object { const val EXTRA_START_BOARD = "test-start-board" }
}
