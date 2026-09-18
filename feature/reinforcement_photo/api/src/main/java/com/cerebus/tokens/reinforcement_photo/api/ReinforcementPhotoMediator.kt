package com.cerebus.tokens.reinforcement_photo.api

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavInflater

/** Photo selection entry. Saved selections are observed through ReinforcementRepository. */
interface ReinforcementPhotoMediator {
    fun createGraph(inflater: NavInflater): NavGraph
    fun open(navController: NavController)
}
