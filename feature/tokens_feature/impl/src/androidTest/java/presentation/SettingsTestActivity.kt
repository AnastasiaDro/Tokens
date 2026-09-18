package presentation

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.cerebus.tokens.feature.tokens_feature.R
import com.cerebus.tokens.feature.tokens_feature.test.R as TestR

/** Test-only host: exercises the real feature graph without the application's saved data. */
class SettingsTestActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = TestR.id.feature_test_host })
        val host = if (savedInstanceState == null) {
            NavHostFragment().also { host ->
                supportFragmentManager.beginTransaction().replace(TestR.id.feature_test_host, host)
                    .setPrimaryNavigationFragment(host).commitNow()
            }
        } else {
            supportFragmentManager.findFragmentById(TestR.id.feature_test_host) as NavHostFragment
        }
        // Like MainActivity, restore the programmatically assembled graph before applying saved navigation.
        host.navController.graph = host.navController.navInflater.inflate(R.navigation.tokens_nav_graph).apply {
            setStartDestination(if (intent.getBooleanExtra(EXTRA_START_BOARD, false)) R.id.tokensFragment else R.id.settingsFragment)
        }
    }

    companion object { const val EXTRA_START_BOARD = "test-start-board" }
}
