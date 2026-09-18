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
        if (savedInstanceState == null) {
            val host = NavHostFragment()
            supportFragmentManager.beginTransaction().replace(TestR.id.feature_test_host, host)
                .setPrimaryNavigationFragment(host).commitNow()
            host.navController.graph = host.navController.navInflater.inflate(R.navigation.tokens_nav_graph).apply {
                setStartDestination(R.id.settingsFragment)
            }
        }
    }
}
