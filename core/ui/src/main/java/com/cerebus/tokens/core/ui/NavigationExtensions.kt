package com.cerebus.tokens.core.ui

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Extension functions for getting result from next backstack entry
 * in navigation component
 *
 * @author Anastasia Drogunova
 * @since 29.11.2023
 */
fun <T> Fragment.getNavigationResultLiveData(key: String) =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)

fun <T> Fragment.setNavigationResult(result: T, key: String) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}
