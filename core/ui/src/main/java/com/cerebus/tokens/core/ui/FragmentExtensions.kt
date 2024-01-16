package com.cerebus.tokens.core.ui

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

fun Fragment.showToast(message: String, duration: ToastDuration = ToastDuration.LONG) {
    Toast.makeText(requireActivity(), message, duration.value).show()
}

enum class ToastDuration(val value: Int) {
    SHORT(Toast.LENGTH_SHORT),
    LONG(Toast.LENGTH_LONG)
}

fun <T> Fragment.subscribeToHotFlow(lifecycleState: Lifecycle.State, observable: SharedFlow<T>, action: (data: T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            observable.collect { data ->
                action.invoke(data)
            }
        }
    }
}


