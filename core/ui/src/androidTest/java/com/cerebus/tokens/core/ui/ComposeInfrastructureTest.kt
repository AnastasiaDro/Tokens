package com.cerebus.tokens.core.ui

import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cerebus.tokens.core.ui.theme.TokensColors
import com.cerebus.tokens.core.ui.theme.TokensTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeInfrastructureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun themeProvidesApplicationPaletteAndUpdatesContent() {
        val text = mutableStateOf(INITIAL_TEXT)
        compose.setContent {
            TokensTheme {
                assertEquals(TokensColors.Action, MaterialTheme.colorScheme.primary)
                assertEquals(TokensColors.PrimaryText, MaterialTheme.colorScheme.onSurface)
                assertEquals(TokensColors.SecondaryText, MaterialTheme.colorScheme.onSurfaceVariant)
                assertEquals(TokensColors.Background, MaterialTheme.colorScheme.background)
                assertEquals(TokensColors.Divider, MaterialTheme.colorScheme.outlineVariant)
                Text(text.value)
            }
        }
        compose.onNodeWithText(INITIAL_TEXT).assertIsDisplayed()
        compose.runOnIdle { text.value = UPDATED_TEXT }
        compose.onNodeWithText(UPDATED_TEXT).assertIsDisplayed()
    }

    @Test
    fun hostDisposesCompositionWhenViewLifecycleEndsEvenWhileAttached() {
        lateinit var owner: ViewLifecycleOwner
        lateinit var host: ComposeView
        var entered = false
        var disposed = false
        compose.runOnUiThread {
            owner = ViewLifecycleOwner()
            owner.registry.currentState = Lifecycle.State.RESUMED
            host = ComposeView(compose.activity).apply {
                id = android.R.id.content
                setViewTreeLifecycleOwner(owner)
                setTokensContent {
                    assertEquals(TokensColors.Action, MaterialTheme.colorScheme.primary)
                    DisposableEffect(Unit) {
                        entered = true
                        onDispose { disposed = true }
                    }
                    Text(INITIAL_TEXT)
                }
            }
            compose.activity.setContentView(FrameLayout(compose.activity).apply { addView(host) })
        }
        compose.onNodeWithText(INITIAL_TEXT).assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(entered)
            assertFalse(disposed)
            owner.registry.currentState = Lifecycle.State.DESTROYED
        }
        compose.runOnIdle {
            assertTrue(host.isAttachedToWindow)
            assertTrue(disposed)
        }
    }

    private class ViewLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        const val INITIAL_TEXT = "Настройки"
        const val UPDATED_TEXT = "Жетоны"
    }
}
