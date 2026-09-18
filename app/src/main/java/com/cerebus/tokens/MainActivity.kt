package com.cerebus.tokens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.cerebus.tokens.feature.tokens_feature.api.TokensMediator
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val tokensMediator: TokensMediator by inject()
    private val photoMediator: ReinforcementPhotoMediator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val host = supportFragmentManager.findFragmentById(R.id.nav_container) as NavHostFragment
        val controller = host.navController
        val root = controller.navInflater.inflate(R.navigation.root_nav_graph)
        val tokensGraph = tokensMediator.createGraph(controller.navInflater)
        root.addDestination(tokensGraph)
        root.addDestination(photoMediator.createGraph(controller.navInflater))
        root.setStartDestination(tokensGraph.id)
        // NavController applies the saved back stack when the composed graph is assigned.
        controller.graph = root
    }
}
