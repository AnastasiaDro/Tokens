package presentation.tokens_screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.cerebus.tokens.core.ui.theme.TokensDimensions
import com.cerebus.tokens.core.ui.theme.TokensTheme
import com.cerebus.tokens.feature.tokens_feature.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import presentation.state.TokenShape
import presentation.state.TokenState

/**
 * Stateless token. The board owns key(state.id) and dispatches clicks by that ID.
 * Parent constraints take precedence over the preferred size, including in a small window.
 */
@Composable
internal fun Token(
    state: TokenState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(DEFAULT_SCALE) }
    LaunchedEffect(interactionSource) {
        var increase: Job? = null
        var decrease: Job? = null
        // Collect events directly so a press and release within one frame still produce a pulse.
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    decrease?.cancel()
                    increase?.cancel()
                    increase = launch {
                        scale.animateTo(
                            PRESSED_SCALE,
                            tween(PRESS_ANIMATION_DURATION_MILLIS, easing = LinearOutSlowInEasing),
                        )
                    }
                }
                is PressInteraction.Release -> {
                    val pendingIncrease = increase
                    decrease = launch {
                        // A short tap completes the increase before returning to the resting size.
                        pendingIncrease?.join()
                        scale.animateTo(DEFAULT_SCALE, tween(RELEASE_ANIMATION_DURATION_MILLIS))
                    }
                }
                is PressInteraction.Cancel -> {
                    increase?.cancel()
                    decrease?.cancel()
                    decrease = launch {
                        scale.animateTo(DEFAULT_SCALE, tween(RELEASE_ANIMATION_DURATION_MILLIS))
                    }
                }
            }
        }
    }
    val description = stringResource(R.string.token_description)
    val checkedDescription = stringResource(
        if (state.checked) R.string.token_checked else R.string.token_unchecked,
    )
    val fillColor = if (state.checked) Color(state.color) else colorResource(R.color.baseColor)

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = TokensDimensions.MinimumTouchTarget,
                minHeight = TokensDimensions.MinimumTouchTarget,
            )
            .semantics {
                contentDescription = description
                stateDescription = checkedDescription
            }
            .toggleable(
                value = state.checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onClick() },
            ),
        propagateMinConstraints = true,
    ) {
        Canvas(
            Modifier
                .size(dimensionResource(R.dimen.token_width))
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        ) {
            when (state.shape) {
                TokenShape.CIRCLE -> drawCircle(color = fillColor)
            }
        }
    }
}

private const val DEFAULT_SCALE = 1f
private const val PRESSED_SCALE = 1.08f
private const val PRESS_ANIMATION_DURATION_MILLIS = 100
private const val RELEASE_ANIMATION_DURATION_MILLIS = 180

@Preview(showBackground = true)
@Composable
private fun TokenPreview() {
    TokensTheme {
        Surface {
            Row(
                modifier = Modifier.padding(TokensDimensions.ContentPadding),
                horizontalArrangement = Arrangement.spacedBy(TokensDimensions.SmallSpacing),
            ) {
                val state = TokenState(
                    id = "preview-token",
                    shape = TokenShape.CIRCLE,
                    color = colorResource(R.color.checkedColor).toArgb(),
                    checked = false,
                )
                Token(state = state, onClick = {})
                Token(state = state.copy(id = "preview-checked-token", checked = true), onClick = {})
            }
        }
    }
}
