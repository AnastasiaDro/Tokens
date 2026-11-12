package com.cerebus.tokens.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

/** Existing Koin definitions, but ownership and SavedStateHandle belong to this destination. */
@Composable
inline fun <reified T : ViewModel> navigationViewModel(entry: NavBackStackEntry): T = remember(entry) {
    val factory = viewModelFactory {
        initializer { GlobalContext.get().get<T> { parametersOf(createSavedStateHandle()) } }
    }
    ViewModelProvider.create(entry, factory,
        entry.defaultViewModelCreationExtras)[T::class]
}

/** Late/repeated completion must never pop the screen underneath an already closed dialog. */
fun NavController.popEntryIfCurrent(entry: NavBackStackEntry): Boolean =
    currentBackStackEntry?.id == entry.id && popBackStack()
