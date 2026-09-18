package com.cerebus.tokens.feature.tokens_feature

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.cerebus.tokens.core.ui.*
import com.cerebus.tokens.feature.tokens_feature.api.*
import com.cerebus.tokens.reinforcement_photo.api.ReinforcementPhotoMediator
import domain.repository.MAX_TOKEN_COUNT
import domain.repository.MIN_TOKEN_COUNT
import kotlinx.serialization.Serializable
import org.koin.core.context.GlobalContext
import presentation.SelectTokensNumberAlertData
import presentation.settings_screen.*
import presentation.tokens_screen.*
import presentation.state.SaveState

@Serializable internal data class CountDestination(val count: Int)
@Serializable internal data object ColorDestination

internal class TokensMediatorImpl : TokensMediator {
    override fun registerGraph(builder: NavGraphBuilder, navController: NavHostController, startEntry: TokensEntry) {
        builder.navigation<TokensGraph>(startDestination = if (startEntry == TokensEntry.BOARD) TokensBoard else TokensSettings) {
            composable<TokensBoard> { entry ->
                val vm = navigationViewModel<TokensViewModel>(entry)
                TokensLifecycle(vm)
                TokensRoute(vm,
                    onSelectCount = { navController.navigate(CountDestination(it)) },
                    onSettings = { vm.onStop(); navController.navigate(TokensSettings) },
                    onPhoto = { GlobalContext.get().get<ReinforcementPhotoMediator>().open(navController) })
            }
            composable<TokensSettings>(deepLinks = listOf(navDeepLink { uriPattern = SETTINGS_DEEP_LINK })) {
                val vm = navigationViewModel<SettingsViewModel>(it)
                val context = LocalContext.current
                SettingsRoute(vm, { count -> navController.navigate(CountDestination(count)) },
                    { navController.navigate(ColorDestination) },
                    { context.openSettingsLink(R.string.youtube_link) },
                    { context.openSettingsLink(R.string.donate_link) })
            }
            dialog<CountDestination>(dialogProperties = GuardedDialogProperties) { entry ->
                val vm = navigationViewModel<SelectTokensNumberViewModel>(entry)
                remember(vm) {
                    vm.initialize(SelectTokensNumberAlertData(MIN_TOKEN_COUNT, MAX_TOKEN_COUNT, entry.toRoute<CountDestination>().count))
                }
                val state by vm.state.collectAsStateWithLifecycle()
                val close = { if (vm.state.value.editable) navController.popEntryIfCurrent(entry); Unit }
                LaunchedEffect(state.save) { if (state.save == SaveState.SAVED) navController.popEntryIfCurrent(entry) }
                NavigationDialog(close) { SelectTokensNumberScreen(state, vm::selectCount, vm::save, close) }
            }
            dialog<ColorDestination>(dialogProperties = GuardedDialogProperties) { entry ->
                val vm = navigationViewModel<SelectColorViewModel>(entry)
                val state by vm.state.collectAsStateWithLifecycle()
                val close = { if (vm.state.value.cancellable) navController.popEntryIfCurrent(entry); Unit }
                LaunchedEffect(state.save) { if (state.save == SaveState.SAVED) navController.popEntryIfCurrent(entry) }
                NavigationDialog(close) { SelectColorRoute(vm, vm::save, close) }
            }
        }
    }
    override fun open(navController: NavController, entry: TokensEntry) {
        when (entry) {
            TokensEntry.BOARD -> navController.navigate(TokensBoard) { launchSingleTop = true }
            TokensEntry.SETTINGS -> navController.navigate(TokensSettings) { launchSingleTop = true }
        }
    }

    private companion object { const val SETTINGS_DEEP_LINK = "android-app://com.cerebus.tokens.tokens/settings" }
}
