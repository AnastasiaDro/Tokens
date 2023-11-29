package com.cerebus.tokens.presentation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

fun <T> Fragment.getNavigationResultLiveData(key: String) =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)

fun <T> Fragment.setNavigationResult(result: T, key: String) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}
