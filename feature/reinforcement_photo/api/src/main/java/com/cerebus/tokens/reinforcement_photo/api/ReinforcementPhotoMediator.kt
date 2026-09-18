package com.cerebus.tokens.reinforcement_photo.api

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable

@Serializable data object PhotoDestination

/** Photo selection entry. Saved selections are observed through ReinforcementRepository. */
interface ReinforcementPhotoMediator {
    fun registerGraph(builder: NavGraphBuilder, navController: NavHostController)
    fun open(navController: NavController)
}
