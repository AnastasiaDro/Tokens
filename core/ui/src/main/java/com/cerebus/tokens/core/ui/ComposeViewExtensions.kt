package com.cerebus.tokens.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.cerebus.tokens.core.ui.theme.TokensTheme

/**
 * Transitional host for Fragment/ DialogFragment views. Call before attaching the view.
 * Assign a stable resource ID to the ComposeView so rememberSaveable can be restored.
 * ViewModels and navigation remain owned by the caller; this only installs theme and disposal.
 */
fun ComposeView.setTokensContent(content: @Composable () -> Unit) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        TokensTheme(content)
    }
}
