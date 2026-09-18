package com.cerebus.tokens.reinforcement_photo

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavInflater
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator

internal class ReinforcementPhotoMediatorImpl : ReinforcementPhotoMediator {
    override fun createGraph(inflater: NavInflater): NavGraph =
        inflater.inflate(R.navigation.reinforcement_nav_graph)

    override fun open(navController: NavController) {
        navController.navigate(R.id.nav_graph)
    }
}
