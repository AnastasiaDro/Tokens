package com.cerebus.tokens.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogProperties
import com.cerebus.tokens.core.ui.theme.TokensDimensions

// The navigator must not pop automatically: dismissal consults current VM state, even before a new frame.
val GuardedDialogProperties = DialogProperties(
    dismissOnBackPress = false, dismissOnClickOutside = false,
    usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
)

@Composable
fun NavigationDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    BackHandler { dismiss() }
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { dismiss() } }
        .systemBarsPadding().testTag(NAVIGATION_DIALOG_TAG).padding(TokensDimensions.MediumSpacing), contentAlignment = Alignment.Center) {
        Box(Modifier.pointerInput(Unit) { detectTapGestures { /* Inside the dialog is not outside dismissal. */ } }) {
            content()
        }
    }
}

const val NAVIGATION_DIALOG_TAG = "navigation-dialog"
