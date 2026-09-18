package com.cerebus.tokens.reinforcement_photo

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.compose.dialog
import com.cerebus.tokens.core.ui.GuardedDialogProperties
import com.cerebus.tokens.core.ui.navigationViewModel
import com.cerebus.tokens.core.ui.popEntryIfCurrent
import com.cerebus.tokens.reinforcement_photo.api.PhotoDestination
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import com.cerebus.tokens.reinforcement_photo.presentation.ChangePhotoViewModel
import com.cerebus.tokens.reinforcement_photo.presentation.PhotoDestinationContent

internal class ReinforcementPhotoMediatorImpl : ReinforcementPhotoMediator {
    override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController) {
        builder.dialog<PhotoDestination>(dialogProperties = GuardedDialogProperties,
            deepLinks = listOf(navDeepLink { uriPattern = PHOTO_DEEP_LINK })) { entry ->
            PhotoDestinationContent(navigationViewModel<ChangePhotoViewModel>(entry)) { navController.popEntryIfCurrent(entry) }
        }
    }
    override fun open(navController: NavController) {
        navController.navigate(PhotoDestination) { launchSingleTop = true }
    }
    private companion object { const val PHOTO_DEEP_LINK = "android-app://com.cerebus.tokens.reinforcement_photo.presentation.dialog" }
}
